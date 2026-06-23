package tech.capullo.radio.services

import android.app.ForegroundServiceStartNotAllowedException
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.annotations.Range
import tech.capullo.radio.airplay.AirplayMetadataReader
import tech.capullo.radio.airplay.AirplayProcess
import tech.capullo.radio.airplay.DacpController
import tech.capullo.radio.espoti.AudioFocusManager
import tech.capullo.radio.espoti.EspotiNsdManager
import tech.capullo.radio.espoti.EspotiPlayerManager
import tech.capullo.radio.espoti.EspotiSessionRepository
import tech.capullo.radio.snapcast.ArtData
import tech.capullo.radio.snapcast.ProcessStatus
import tech.capullo.radio.snapcast.SnapcastControlBridge
import tech.capullo.radio.snapcast.SnapclientProcess
import tech.capullo.radio.snapcast.SnapserverNsdManager
import tech.capullo.radio.snapcast.SnapserverProcess
import tech.capullo.radio.snapcast.StreamMetadata
import tech.capullo.radio.snapcast.StreamProperties
import tech.capullo.radio.ui.model.AudioChannel
import xyz.gianlu.librespot.audio.MetadataWrapper
import xyz.gianlu.librespot.core.Session
import xyz.gianlu.librespot.metadata.PlayableId
import xyz.gianlu.librespot.player.Player
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import javax.inject.Inject

@AndroidEntryPoint
class RadioBroadcasterService : Service() {
    @Inject lateinit var audioFocusManager: AudioFocusManager

    @Inject lateinit var espotiSessionRepository: EspotiSessionRepository

    @Inject lateinit var espotiPlayerManager: EspotiPlayerManager

    @Inject lateinit var snapclientProcess: SnapclientProcess

    @Inject lateinit var snapserverProcess: SnapserverProcess

    @Inject lateinit var snapserverNsdManager: SnapserverNsdManager

    @Inject lateinit var airplayProcess: AirplayProcess

    @Inject lateinit var airplayMetadataReader: AirplayMetadataReader

    @Inject lateinit var dacpController: DacpController

    // Spotify Connect zeroconf discovery + login handler. Lives in the service
    // (not the loading screen's ViewModel) so a user who skips Spotify can still
    // connect later while broadcasting; a successful login emits
    // SessionState.Created and brings up the Spotify stack.
    @Inject lateinit var espotiNsdManager: EspotiNsdManager

    private val playbackExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private var player: Player? = null
    private var session: Session? = null
    private val _isPlayerLoadingFlow = MutableStateFlow(true)
    val isPlayerLoadingFlow = _isPlayerLoadingFlow.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var snapserverJob: Job? = null
    private var snapclientJob: Job? = null
    private var airplayJob: Job? = null
    private var airplayMetadataJob: Job? = null
    private var controlBridge: SnapcastControlBridge? = null
    private var airplayControlBridge: SnapcastControlBridge? = null

    // The receiver stack (snapserver/snapclient/AirPlay/NSD) is independent of
    // Spotify and starts with the service; the Spotify stack (player) starts
    // only once a session exists. Flags keep both idempotent across repeated
    // onStartCommand calls and SessionState.Created emissions.
    private var receiverStarted = false
    private var spotifyStarted = false

    // Health of the supervised native processes, surfaced to the broadcaster UI
    private val _airplayStatus = MutableStateFlow(ProcessStatus.STARTING)
    val airplayStatus = _airplayStatus.asStateFlow()
    private val _snapserverStatus = MutableStateFlow(ProcessStatus.STARTING)
    val snapserverStatus = _snapserverStatus.asStateFlow()

    // shairport-sync's in-process mDNS responder (tinysvcmdns) only receives
    // queries while multicast is allowed through the Wi-Fi filter
    private var multicastLock: android.net.wifi.WifiManager.MulticastLock? = null

    private var isPlaying: Boolean = false
    private var isMuted: Boolean = false
    private var currentVolumePercent: Int = 100
    private var currentMetadata: MetadataWrapper? = null
    private var currentArtData: String? = null
    private var currentArtUrl: String? = null

    private var currentAudioChannel = AudioChannel.STEREO

    private val binder = LocalBinder()
    inner class LocalBinder : Binder() {
        fun getIsPlayerLoadingFlow() = this@RadioBroadcasterService.isPlayerLoadingFlow
        fun getAirplayStatusFlow() = this@RadioBroadcasterService.airplayStatus
        fun getSnapserverStatusFlow() = this@RadioBroadcasterService.snapserverStatus
        fun updateAudioChannel(channel: AudioChannel) =
            this@RadioBroadcasterService.updateAudioChannel(channel)
    }

    private fun runOnPlayback(func: () -> Unit): Future<*>? = playbackExecutor.submit(func)

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT,
            )
            val notificationManager =
                getSystemService(NOTIFICATION_SERVICE) as
                    NotificationManager
            notificationManager.createNotificationChannel(serviceChannel)
        }
    }
    private fun startForeground() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                createChannel()
            }
            val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .build()
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                notification,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                } else {
                    0
                },
            )
        } catch (e: Exception) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                e is ForegroundServiceStartNotAllowedException
            ) {
                // App not in a valid state to start foreground service
                Log.d(TAG, "Foreground service not allowed")
            } else {
                Log.d(TAG, "Error starting foreground service")
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground()
        // The receiver stack is independent of Spotify and comes up as soon as
        // the service does. Guarded so repeated starts don't relaunch it.
        startReceiverStack()

        return START_NOT_STICKY
    }

    override fun onCreate() {
        super.onCreate()
        observeSessionState()
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onUnbind(intent: Intent?): Boolean {
        println("RadioBroadcasterService on unbind")
        return super.onUnbind(intent)
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        controlBridge?.stop()
        airplayControlBridge?.stop()
        dacpController.stop()
        snapserverNsdManager.stop()
        releaseMulticastLock()
        scope.cancel()
        player?.close()
        session?.close()
        playbackExecutor.shutdownNow()
        Log.d(TAG, "Stop Service")
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        controlBridge?.stop()
        airplayControlBridge?.stop()
        dacpController.stop()
        snapserverNsdManager.stop()
        releaseMulticastLock()
        scope.cancel()
        player?.close()
        session?.close()
        playbackExecutor.shutdownNow()
        Log.d(TAG, "Destroy Service")
    }

    private fun startLibrespot() {
        audioFocusManager.requestFocus()

        val playerEventsListener = object : Player.EventsListener {
            override fun onContextChanged(player: Player, newUri: String) {
                println("context changed: $newUri")
            }

            override fun onTrackChanged(
                player: Player,
                id: PlayableId,
                metadata: MetadataWrapper?,
                userInitiated: Boolean,
            ) {
                if (metadata != null) {
                    println("track changed: ${metadata.id}")
                    updateMetadata(metadata)
                    isPlaying = true
                    player.play()
                } else {
                    println("track changed: $id")
                    updateMetadata(null)
                }
            }

            override fun onPlaybackEnded(player: Player) {
                println("playback ended")
                isPlaying = false
                pushBridgeProperties()
            }

            override fun onPlaybackPaused(player: Player, trackTime: Long) {
                println("playback paused")
                isPlaying = false
                pushBridgeProperties()
            }

            override fun onPlaybackResumed(player: Player, trackTime: Long) {
                audioFocusManager.requestFocus()
                println("playback resumed")
                isPlaying = true
                pushBridgeProperties()
            }

            override fun onPlaybackFailed(player: Player, e: java.lang.Exception) {
                println("playback failed: ${e.message}")
                isPlaying = false
                pushBridgeProperties()
            }

            override fun onTrackSeeked(player: Player, trackTime: Long) {
                println("track seeked: $trackTime")
                pushBridgeProperties()
            }

            override fun onMetadataAvailable(player: Player, metadata: MetadataWrapper) {
                println("metadata available: ${metadata.id}")
                updateMetadata(metadata)
            }

            override fun onPlaybackHaltStateChanged(
                player: Player,
                halted: Boolean,
                trackTime: Long,
            ) {
                if (halted) {
                    println("playback halted")
                    isPlaying = false
                } else {
                    println("playback resumed")
                    isPlaying = true
                }
                pushBridgeProperties()
            }

            override fun onInactiveSession(player: Player, timeout: Boolean) {
                println("inactive session")
                isPlaying = false
                pushBridgeProperties()
            }

            override fun onVolumeChanged(
                player: Player,
                volume: @Range(
                    from = 0,
                    to = 1,
                ) Float,
            ) {
                println("volume changed: $volume")
                currentVolumePercent = (volume * 100).toInt().coerceIn(0, 100)
                pushBridgeProperties()
            }

            override fun onPanicState(player: Player) {
                println("panic state")
                isPlaying = false
                pushBridgeProperties()
            }

            override fun onStartedLoading(player: Player) {
                _isPlayerLoadingFlow.value = true
                println("started loading")
            }

            override fun onFinishedLoading(player: Player) {
                _isPlayerLoadingFlow.value = false
                println("finished loading")
                isPlaying = true
                pushBridgeProperties()
            }
        }
        runOnPlayback {
            espotiPlayerManager.player.addEventsListener(playerEventsListener)
            _isPlayerLoadingFlow.value = false
        }

        // Wait for Spotify Connect device registration off the playbackExecutor so the
        // executor isn't wedged if waitReady() blocks (the connection_id dealer message
        // can race and be dropped, in which case waitReady() never returns). Timeout
        // bounds the wait; we attempt autoplay either way.
        scope.launch {
            val ready = withTimeoutOrNull(WAIT_READY_TIMEOUT_MS) {
                runInterruptible(Dispatchers.IO) { espotiPlayerManager.player.waitReady() }
                true
            } ?: false
            if (!ready) Log.w(TAG, "waitReady timed out; attempting autoplay anyway")

            runOnPlayback {
                session?.let { ses ->
                    val uri = "spotify:user:${ses.username()}:collection"
                    Log.d(TAG, "Autoplay: loading $uri")
                    espotiPlayerManager.player.load(uri, true, true)
                }
            }
        }
    }

    private fun fetchAlbumArt(url: String) {
        Log.d(TAG, "fetchAlbumArt: starting for url: $url")
        scope.launch {
            try {
                Log.d(TAG, "fetchAlbumArt: launching coroutine for $url")
                val bytes = withContext(Dispatchers.IO) {
                    Log.d(TAG, "fetchAlbumArt: opening connection to $url")
                    val conn = java.net.URL(url).openConnection() as java.net.HttpURLConnection
                    conn.connectTimeout = 5000
                    conn.readTimeout = 5000
                    conn.setRequestProperty(
                        "User-Agent",
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64)",
                    )
                    conn.requestMethod = "GET"
                    conn.doInput = true
                    conn.connect()
                    val responseCode = conn.responseCode
                    Log.d(TAG, "fetchAlbumArt: HTTP response code: $responseCode")
                    if (responseCode == java.net.HttpURLConnection.HTTP_OK) {
                        conn.inputStream.use { it.readBytes() }
                    } else {
                        throw java.io.IOException("HTTP error code: $responseCode")
                    }
                }
                Log.d(TAG, "fetchAlbumArt: read ${bytes.size} bytes")
                val base64 = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                Log.d(TAG, "fetchAlbumArt: encoded base64 length: ${base64.length}")
                currentArtData = base64
                currentArtUrl = url
                pushBridgeProperties()
                Log.d(TAG, "fetchAlbumArt: successfully pushed properties with artData")
            } catch (t: Throwable) {
                Log.e(TAG, "Failed to fetch album art from $url", t)
            }
        }
    }

    private fun updateMetadata(metadata: MetadataWrapper?) {
        currentMetadata = metadata
        val url = metadata?.coverImage?.let { coverGroup ->
            if (coverGroup.imageCount > 0) {
                val fileId = coverGroup.getImage(0).fileId
                val hex = xyz.gianlu.librespot.common.Utils.bytesToHex(fileId)
                    .lowercase(java.util.Locale.ROOT)
                "https://i.scdn.co/image/$hex"
            } else {
                null
            }
        }

        if (url != currentArtUrl) {
            currentArtData = null
            currentArtUrl = url
            if (url != null) {
                fetchAlbumArt(url)
            } else {
                pushBridgeProperties()
            }
        } else {
            pushBridgeProperties()
        }
    }

    private fun observeSessionState() {
        scope.launch {
            espotiSessionRepository.sessionState.collect { sessionState ->
                when (sessionState) {
                    is EspotiSessionRepository.SessionState.Created -> {
                        session = sessionState.session
                        // Only the Spotify-specific pieces wait on a session;
                        // the receiver stack is already running from onStartCommand.
                        startSpotifyStack()
                    }

                    is EspotiSessionRepository.SessionState.Error -> {
                        Log.d(TAG, "Session error: ${sessionState.message}")
                    }

                    else -> {}
                }
            }
        }
    }

    fun startReceiverStack() {
        if (receiverStarted) return
        receiverStarted = true

        // Bind the control-bridge socket before snapserver starts: snapserver
        // spawns libsnapcontrol.so for the Spotify stream regardless of login,
        // and it connects back to this socket. The bridge is null-safe without
        // a player (see buildStreamProperties / bridgePlayerCallbacks).
        if (controlBridge == null) {
            controlBridge = SnapcastControlBridge(player = bridgePlayerCallbacks).apply {
                start()
            }
        }
        // Same contract for the Airplay stream, on its own socket so its
        // libsnapcontrol.so relay doesn't collide with the Spotify one. Phase 1
        // is read-only: metadata flows up from shairport-sync; transport
        // callbacks are no-ops (see airplayBridgeCallbacks).
        if (airplayControlBridge == null) {
            airplayControlBridge = SnapcastControlBridge(
                socketName = SnapserverProcess.AIRPLAY_CONTROL_SOCKET,
                player = airplayBridgeCallbacks,
            ).apply { start() }
        }
        acquireMulticastLock()
        snapserverJob = launchSupervised("snapserver", _snapserverStatus) {
            snapserverProcess.start()
        }
        snapclientJob = launchSupervised("snapclient") {
            snapclientProcess.start(audioChannel = currentAudioChannel.ordinal)
        }
        airplayJob = launchSupervised("airplay", _airplayStatus) {
            airplayProcess.start()
        }
        // Read shairport-sync's metadata pipe (reopened on each AirPlay session)
        // and push every change to the Airplay control bridge.
        airplayMetadataJob = launchSupervised("airplay-metadata") {
            airplayMetadataReader.run()
        }
        scope.launch {
            airplayMetadataReader.properties.collect { props ->
                airplayControlBridge?.pushProperties(props)
            }
        }
        // Drive DACP discovery from the sender's credentials (acre/daid) so the
        // Airplay transport callbacks below can reach the sender.
        scope.launch {
            airplayMetadataReader.credentials.collect { creds ->
                dacpController.updateCredentials(creds)
            }
        }
        scope.launch { snapserverNsdManager.start() }
        // Advertise Spotify Connect + handle logins for the life of the service,
        // so the device is connectable whether or not the user logs in up front.
        scope.launch { espotiNsdManager.start() }
    }

    fun startSpotifyStack() {
        if (spotifyStarted) return
        spotifyStarted = true

        espotiPlayerManager.createPlayer()
        startLibrespot()
    }

    /**
     * Launches [block] (a native-process run loop) and restarts it with
     * exponential backoff if it exits unexpectedly, so a crash or network blip
     * recovers instead of silently killing the source. Cancellation (service
     * teardown, channel change) propagates out and stops the loop cleanly.
     */
    private fun launchSupervised(
        name: String,
        status: MutableStateFlow<ProcessStatus>? = null,
        block: suspend () -> Unit,
    ): Job = scope.launch {
        var backoffMs = INITIAL_BACKOFF_MS
        while (isActive) {
            val startedAt = SystemClock.elapsedRealtime()
            status?.value = ProcessStatus.RUNNING
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "$name failed", e)
            }
            if (!isActive) break
            if (SystemClock.elapsedRealtime() - startedAt > HEALTHY_RUN_MS) {
                backoffMs = INITIAL_BACKOFF_MS
            }
            status?.value = ProcessStatus.RESTARTING
            Log.w(TAG, "$name exited; restarting in ${backoffMs}ms")
            delay(backoffMs)
            backoffMs = (backoffMs * 2).coerceAtMost(MAX_BACKOFF_MS)
        }
        status?.value = ProcessStatus.STOPPED
    }

    private fun acquireMulticastLock() {
        if (multicastLock == null) {
            val wifiManager =
                applicationContext.getSystemService(WIFI_SERVICE) as android.net.wifi.WifiManager
            multicastLock = wifiManager.createMulticastLock("RadioCapulloAirplayMdns").apply {
                setReferenceCounted(false)
                acquire()
            }
        }
    }

    private fun releaseMulticastLock() {
        multicastLock?.takeIf { it.isHeld }?.release()
        multicastLock = null
    }

    private fun buildStreamProperties(): StreamProperties {
        val playerInstance = espotiPlayerManager.playerNullable()
        val durationMs = currentMetadata?.duration() ?: 0
        val positionSec = playerInstance?.let {
            val t = it.time()
            if (t >= 0) t / 1000f else 0.0f
        } ?: 0.0f

        val artistJson = currentMetadata?.let {
            JsonArray(listOf(JsonPrimitive(it.artist)))
        }

        val metadataObj = currentMetadata?.let {
            StreamMetadata(
                album = it.albumName,
                artist = artistJson,
                track = it.id?.toString(),
                title = it.name,
                duration = durationMs / 1000f,
                artUrl = currentArtUrl,
                artData = currentArtData?.let { data -> ArtData(data = data, extension = "png") },
            )
        }

        return StreamProperties(
            playbackStatus = if (isPlaying) "playing" else "paused",
            loopStatus = "none",
            shuffle = false,
            volume = currentVolumePercent,
            mute = isMuted,
            rate = 1.0f,
            position = positionSec,
            canControl = true,
            canGoNext = true,
            canGoPrevious = true,
            canPause = true,
            canPlay = true,
            canSeek = true,
            metadata = metadataObj,
        )
    }

    private fun pushBridgeProperties() {
        val props = buildStreamProperties()
        controlBridge?.pushProperties(props)
    }

    private val bridgePlayerCallbacks = object : SnapcastControlBridge.PlayerCallbacks {
        override fun onPlay() {
            runOnPlayback {
                try {
                    espotiPlayerManager.player.play()
                } catch (e: Exception) {
                    Log.e(TAG, "Error executing play from Snapcast", e)
                }
            }
        }

        override fun onPause() {
            runOnPlayback {
                try {
                    espotiPlayerManager.player.pause()
                } catch (e: Exception) {
                    Log.e(TAG, "Error executing pause from Snapcast", e)
                }
            }
        }

        override fun onPlayPause() {
            runOnPlayback {
                try {
                    espotiPlayerManager.player.playPause()
                } catch (e: Exception) {
                    Log.e(TAG, "Error executing playPause from Snapcast", e)
                }
            }
        }

        override fun onNext() {
            runOnPlayback {
                try {
                    espotiPlayerManager.player.next()
                } catch (e: Exception) {
                    Log.e(TAG, "Error executing next from Snapcast", e)
                }
            }
        }

        override fun onPrevious() {
            runOnPlayback {
                try {
                    espotiPlayerManager.player.previous()
                } catch (e: Exception) {
                    Log.e(TAG, "Error executing previous from Snapcast", e)
                }
            }
        }

        override fun onSeek(offsetUs: Long) {
            runOnPlayback {
                try {
                    val p = espotiPlayerManager.player
                    val currentTimeMs = p.time()
                    if (currentTimeMs >= 0) {
                        val offsetMs = offsetUs / 1000
                        val durationMs = currentMetadata?.duration() ?: 0
                        val targetTimeMs = (currentTimeMs + offsetMs)
                            .coerceIn(0, durationMs.toLong())
                        p.seek(targetTimeMs.toInt())
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error executing seek from Snapcast", e)
                }
            }
        }

        override fun onSetPosition(trackId: String, posUs: Long) {
            runOnPlayback {
                try {
                    val p = espotiPlayerManager.player
                    val durationMs = currentMetadata?.duration() ?: 0
                    val posMs = (posUs / 1000).coerceIn(0, durationMs.toLong())
                    p.seek(posMs.toInt())
                } catch (e: Exception) {
                    Log.e(TAG, "Error executing setPosition from Snapcast", e)
                }
            }
        }

        override fun onSetProperty(name: String, value: kotlinx.serialization.json.JsonElement) {
            runOnPlayback {
                try {
                    val p = espotiPlayerManager.player
                    if (name == "volume") {
                        val snapcastVol = value.jsonPrimitive.double.toInt().coerceIn(0, 100)
                        currentVolumePercent = snapcastVol
                        val librespotVol =
                            (snapcastVol.toFloat() / 100f * Player.VOLUME_MAX).toInt()
                        p.setVolume(librespotVol)
                    } else if (name == "mute") {
                        val mute = value.jsonPrimitive.booleanOrNull ?: false
                        isMuted = mute
                        if (mute) {
                            p.setVolume(0)
                        } else {
                            val librespotVol =
                                (currentVolumePercent.toFloat() / 100f * Player.VOLUME_MAX).toInt()
                            p.setVolume(librespotVol)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error executing setProperty ($name) from Snapcast", e)
                }
            }
        }

        override fun currentProperties(): StreamProperties = buildStreamProperties()
    }

    // AirPlay transport is driven back to the sender over DACP (DacpController).
    // Seek/setPosition stay unsupported (canSeek=false); volume/mute map to the
    // sender's dmcp.volume.
    private val airplayBridgeCallbacks = object : SnapcastControlBridge.PlayerCallbacks {
        override fun onPlay() = dacpController.play()
        override fun onPause() = dacpController.pause()
        override fun onPlayPause() = dacpController.playPause()
        override fun onNext() = dacpController.next()
        override fun onPrevious() = dacpController.previous()

        override fun onSeek(offsetUs: Long) {
            Log.d(TAG, "AirPlay seek unsupported")
        }

        override fun onSetPosition(trackId: String, posUs: Long) {
            Log.d(TAG, "AirPlay setPosition unsupported")
        }

        override fun onSetProperty(name: String, value: kotlinx.serialization.json.JsonElement) {
            when (name) {
                "volume" -> dacpController.setVolume(value.jsonPrimitive.double.toInt())
                "mute" -> dacpController.setMute(value.jsonPrimitive.booleanOrNull ?: false)
                else -> Log.d(TAG, "AirPlay setProperty '$name' unsupported")
            }
        }

        override fun currentProperties(): StreamProperties = airplayMetadataReader.properties.value
    }

    fun updateAudioChannel(channel: AudioChannel) {
        currentAudioChannel = channel
        // Restart snapclient with new channel
        snapclientJob?.cancel()
        snapclientJob = launchSupervised("snapclient") {
            snapclientProcess.start(audioChannel = currentAudioChannel.ordinal)
        }
    }

    companion object {
        const val CHANNEL_ID = "RadioBroadcasterServiceChannel"
        const val CHANNEL_NAME = "Radio Broadcaster Service Channel"
        const val NOTIFICATION_ID = 100
        const val WAIT_READY_TIMEOUT_MS = 10_000L

        // Restart-supervisor backoff bounds for the native receiver processes
        const val INITIAL_BACKOFF_MS = 1_000L
        const val MAX_BACKOFF_MS = 30_000L
        const val HEALTHY_RUN_MS = 10_000L

        val TAG: String = RadioBroadcasterService::class.java.simpleName
    }
}

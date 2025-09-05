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
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.jetbrains.annotations.Range
import tech.capullo.radio.espoti.AudioFocusManager
import tech.capullo.radio.espoti.EspotiPlayerManager
import tech.capullo.radio.espoti.EspotiSessionRepository
import tech.capullo.radio.snapcast.SnapclientProcess
import tech.capullo.radio.snapcast.SnapserverProcess
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

    private val playbackExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private var player: Player? = null
    private var session: Session? = null
    private val _isPlayerLoadingFlow = MutableStateFlow(true)
    val isPlayerLoadingFlow = _isPlayerLoadingFlow.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO + Job())

    private var snapserverJob: Job? = null
    private var snapclientJob: Job? = null

    private var currentAudioChannel = AudioChannel.STEREO

    private val binder = LocalBinder()
    inner class LocalBinder : Binder() {
        fun getIsPlayerLoadingFlow() = this@RadioBroadcasterService.isPlayerLoadingFlow
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
        observeSessionState()

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        scope.cancel()
        player?.close()
        session?.close()
        playbackExecutor.shutdownNow()
        Log.d(TAG, "Stop Service")
        stopSelf()
    }

    private fun startLibrespot() {
        audioFocusManager.requestFocus()

        val playerEventsListener = object : Player.EventsListener {
            override fun onContextChanged(player: Player, newUri: String) {
                println("context changed: $newUri")
                // player.play()
            }

            override fun onTrackChanged(
                player: Player,
                id: PlayableId,
                metadata: MetadataWrapper?,
                userInitiated: Boolean,
            ) {
                if (metadata != null) {
                    println("track changed: ${metadata.id}")
                    player.play()
                } else {
                    println("track changed: $id")
                }
            }

            override fun onPlaybackEnded(player: Player) {
                println("playback ended")
            }

            override fun onPlaybackPaused(player: Player, trackTime: Long) {
                println("playback paused")
            }

            override fun onPlaybackResumed(player: Player, trackTime: Long) {
                audioFocusManager.requestFocus()
                println("playback resumed")
            }

            override fun onPlaybackFailed(player: Player, e: java.lang.Exception) {
                println("playback failed: ${e.message}")
            }

            override fun onTrackSeeked(player: Player, trackTime: Long) {
                println("track seeked: $trackTime")
            }

            override fun onMetadataAvailable(player: Player, metadata: MetadataWrapper) {
                println("metadata available: ${metadata.id}")
            }

            override fun onPlaybackHaltStateChanged(
                player: Player,
                halted: Boolean,
                trackTime: Long,
            ) {
                if (halted) {
                    println("playback halted")
                } else {
                    println("playback resumed")
                }
            }

            override fun onInactiveSession(player: Player, timeout: Boolean) {
                println("inactive session")
            }

            override fun onVolumeChanged(
                player: Player,
                volume: @Range(
                    from = 0,
                    to = 1,
                ) Float,
            ) {
                println("volume changed: $volume")
            }

            override fun onPanicState(player: Player) {
                println("panic state")
            }

            override fun onStartedLoading(player: Player) {
                _isPlayerLoadingFlow.value = true
                println("started loading")
            }

            override fun onFinishedLoading(player: Player) {
                _isPlayerLoadingFlow.value = false
                println("finished loading")
            }
        }
        runOnPlayback {
            espotiPlayerManager.player.addEventsListener(playerEventsListener)
            println("added events listener")
            espotiPlayerManager.player.waitReady()
            println("player ready")
            espotiPlayerManager.player.play()
            session?.let { ses ->
                val uri = "spotify:user:${ses.username()}:collection"
                println("loading uri: $uri")
                espotiPlayerManager.player.load(uri, true, true)
            }
        }
    }

    private fun observeSessionState() {
        scope.launch {
            espotiSessionRepository.sessionState.collect { sessionState ->
                when (sessionState) {
                    is EspotiSessionRepository.SessionState.Created -> {
                        session = sessionState.session
                        // TODO: might need to handle player session thrown errors
                        espotiPlayerManager.createPlayer()
                        startLibrespot()

                        // Start snapcast processes after we have a valid session
                        startSnapcast()
                    }
                    is EspotiSessionRepository.SessionState.Error -> {
                        Log.d(TAG, "Session error: ${sessionState.message}")
                    }
                    else -> {}
                }
            }
        }
    }

    fun startSnapcast() {
        snapserverJob = scope.launch { snapserverProcess.start() }
        snapclientJob = scope.launch { snapclientProcess.start() }
    }

    fun updateAudioChannel(channel: AudioChannel) {
        currentAudioChannel = channel
        // Restart snapclient with new channel
        snapclientJob?.cancel()
        snapclientJob =
            scope.launch { snapclientProcess.start(audioChannel = currentAudioChannel.ordinal) }
    }

    companion object {
        const val CHANNEL_ID = "RadioBroadcasterServiceChannel"
        const val CHANNEL_NAME = "Radio Broadcaster Service Channel"
        const val NOTIFICATION_ID = 100
        val TAG: String = RadioBroadcasterService::class.java.simpleName
    }
}

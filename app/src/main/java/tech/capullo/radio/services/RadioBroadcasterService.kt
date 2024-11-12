package tech.capullo.radio.services

import android.app.ForegroundServiceStartNotAllowedException
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.Process
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import tech.capullo.radio.data.RadioRepository
import tech.capullo.radio.espoti.EspotiZeroconfServer.SessionParams
import xyz.gianlu.librespot.core.Session
import xyz.gianlu.librespot.player.Player
import xyz.gianlu.librespot.player.PlayerConfiguration
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.util.UUID
import java.util.concurrent.Executors
import javax.inject.Inject

@AndroidEntryPoint
class RadioBroadcasterService : Service() {
    @Inject lateinit var repository: RadioRepository

    val executorService = Executors.newSingleThreadExecutor()
    var player: Player? = null
    var session: Session? = null

    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private lateinit var snapserver: Deferred<Unit>
    private lateinit var snapclient: Deferred<Unit>

    private val binder = LocalBinder()
    inner class LocalBinder : Binder() {
        fun getService(): RadioBroadcasterService = this@RadioBroadcasterService
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
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
                100,
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
            } else
                Log.d(TAG, "Error starting foreground service")
        }
    }
    override fun onCreate() {
        super.onCreate()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val pipeFilepath = repository.getPipeFilepath()!!
        startForeground()

        startSnapcast(
            pipeFilepath,
            repository.getCacheDirPath(),
            repository.getNativeLibDirPath(),
            applicationContext.getSystemService(AUDIO_SERVICE) as AudioManager
        )

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return binder
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        scope.cancel()
        player?.close()
        session?.close()
        executorService.shutdownNow()
        Log.d(TAG, "Stop Service")
        stopSelf()
    }

    fun startLibrespot(sessionParams: SessionParams) {
        // TODO: close the session using executor closeable
        //this.session = session
        val pipeFilepath = repository.getPipeFilepath()!!
        executorService.execute(
            SessionChangedRunnable(
                sessionParams,
                pipeFilepath,
                object : SessionChangedCallback {
                    override fun onPlayerReady(callbackPlayer: Player) {
                        Log.d(TAG, "Player ready")
                        player = callbackPlayer
                        player?.next()
                        player?.play()
                    }

                    override fun onPlayerError(e: Exception) {
                        Log.e(TAG, "Error creating player", e)
                    }
                }
            )
        )
    }

    interface SessionChangedCallback {
        fun onPlayerReady(player: Player)
        fun onPlayerError(ex: Exception)
    }

    private class SessionChangedRunnable(
        val sessionParams: SessionParams,
        val pipeFilepath: String,
        val callback: SessionChangedCallback
    ) : Runnable {
        override fun run() {
            // build the session
            val session = Session.Builder(sessionParams.conf)
                .setDeviceId(sessionParams.deviceId)
                .setDeviceName(sessionParams.deviceName)
                .setDeviceType(sessionParams.deviceType)
                .setPreferredLocale(sessionParams.preferredLocale)
                .blob(sessionParams.username, sessionParams.decrypted)
                .create()
            val player = prepareLibrespotPlayer(session)
            try {
                player.waitReady()
                callback.onPlayerReady(player)
            } catch (ex: Exception) {
                Log.e("NSD", "Error waiting for player to be ready", ex)
                callback.onPlayerError(ex)
            }
        }

        private fun prepareLibrespotPlayer(session: Session): Player {
            val configuration = PlayerConfiguration.Builder()
                .setOutput(PlayerConfiguration.AudioOutput.PIPE)
                .setOutputPipe(File(pipeFilepath))
                .build()
            return Player(configuration, session)
        }
    }

    private fun startSnapcast(
        filifoFilepath: String,
        cacheDir: String,
        nativeLibraryDir: String,
        audioManager: AudioManager,
    ) {
        // Android audio configuration
        val androidPlayer = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) "opensl" else "oboe"
        val rate: String? = audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE)
        val fpb: String? = audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER)
        val sampleformat = "$rate:16:*"

        scope.launch {
            snapserver = async {
                snapcastProcess(
                    filifoFilepath,
                    cacheDir,
                    nativeLibraryDir,
                    true,
                    UUID.randomUUID().toString(),
                    androidPlayer,
                    sampleformat,
                    rate,
                    fpb
                )
            }
            snapclient = async {
                snapcastProcess(
                    filifoFilepath,
                    cacheDir,
                    nativeLibraryDir,
                    false,
                    UUID.randomUUID().toString(),
                    androidPlayer,
                    sampleformat,
                    rate,
                    fpb
                )
            }
            awaitAll(snapclient, snapserver)
        }
    }

    private suspend fun snapcastProcess(
        filifoFilepath: String,
        cacheDir: String,
        nativeLibDir: String,
        isSnapserver: Boolean,
        uniqueId: String,
        player: String,
        sampleFormat: String,
        rate: String?,
        fpb: String?
    ) = withContext(Dispatchers.IO) {
        val pb = if (isSnapserver) {
            val streamName = "name=RadioCapullo"
            val pipeMode = "mode=read"
            val dryoutMs = "dryout_ms=2000"
            val librespotSampleFormat = "sampleformat=44100:16:2"
            val pipeArgs = listOf(
                streamName, pipeMode, dryoutMs, librespotSampleFormat
            ).joinToString("&")

            ProcessBuilder()
                .command(
                    "$nativeLibDir/libsnapserver.so",
                    "--server.datadir=$cacheDir",
                    "--stream.source",
                    "pipe://$filifoFilepath?$pipeArgs",
                )
                .redirectErrorStream(true)
        } else {
            ProcessBuilder().command(
                "$nativeLibDir/libsnapclient.so", "-h", "127.0.0.1", "-p", 1704.toString(),
                "--hostID", uniqueId, "--player", player, "--sampleformat", sampleFormat,
                "--logfilter", "*:info,Stats:debug"
            )
        }
        try {
            val env = pb.environment()
            if (rate != null) env["SAMPLE_RATE"] = rate
            if (fpb != null) env["FRAMES_PER_BUFFER"] = fpb

            val process = pb.start()
            val bufferedReader = BufferedReader(
                InputStreamReader(process.inputStream)
            )
            var line: String?
            while (bufferedReader.readLine().also { line = it } != null) {
                ensureActive()
                val processId = Process.myPid()
                val threadName = Thread.currentThread().name
                val tag = if (isSnapserver) "SNAPSERVER" else "SNAPCLIENT"
                Log.d(tag, "Running on: $processId -  $threadName - ${line!!}")
            }
        } catch (e: IOException) {
            Log.e(TAG, "Error starting snapcast process", e)
        } catch (e: CancellationException) {
            Log.e(TAG, "Worker stopped", e)
        }
    }

    companion object {
        const val CHANNEL_ID = "RadioBroadcasterServiceChannel"
        const val CHANNEL_NAME = "Radio Broadcaster Service Channel"
        const val TAG = "RadioBroadcasterService"
    }
}

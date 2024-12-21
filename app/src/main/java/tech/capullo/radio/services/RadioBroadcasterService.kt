package tech.capullo.radio.services

import android.annotation.SuppressLint
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
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.guava.future
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import tech.capullo.radio.data.RadioRepository
import tech.capullo.radio.espoti.AudioFocusManager
import tech.capullo.radio.espoti.EspotiPlayerManager
import tech.capullo.radio.espoti.EspotiSessionManager
import xyz.gianlu.librespot.core.Session
import xyz.gianlu.librespot.player.Player
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.util.UUID
import java.util.concurrent.Executors
import javax.inject.Inject

@AndroidEntryPoint
class RadioBroadcasterService : Service() {
    @Inject lateinit var repository: RadioRepository
    @Inject lateinit var audioFocusManager: AudioFocusManager
    @Inject lateinit var espotiSessionManager: EspotiSessionManager
    @Inject lateinit var espotiPlayerManager: EspotiPlayerManager

    val playbackExecutor = Executors.newSingleThreadExecutor()
    private val playbackScope =
        CoroutineScope(playbackExecutor.asCoroutineDispatcher() + SupervisorJob())
    var player: Player? = null
    var session: Session? = null

    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private lateinit var snapserver: Deferred<Unit>
    private lateinit var snapclient: Deferred<Unit>

    private val binder = LocalBinder()
    inner class LocalBinder : Binder() {
        fun getService(): RadioBroadcasterService = this@RadioBroadcasterService
    }

    fun runOnPlayback(func: () -> Unit) = playbackExecutor.submit(func)

    @SuppressLint("RestrictedApi")
    private fun createListenableFuture(action: suspend () -> Unit) = playbackScope.future {
        return@future try {
            action()
        } catch (e: Exception) {
            e.printStackTrace()
        }
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
        playbackExecutor.shutdownNow()
        Log.d(TAG, "Stop Service")
        stopSelf()
    }

    fun startLibrespot() {
        audioFocusManager.requestFocus()
        runOnPlayback {
            espotiPlayerManager.player().waitReady()
        }
        /*
        createListenableFuture {
            Log.d(TAG, "Playing")
            espotiPlayerManager.player().play()
        }
         */
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

package tech.capullo.radio

import android.app.ForegroundServiceStartNotAllowedException
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioManager
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
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.util.UUID
import javax.inject.Inject

@AndroidEntryPoint
class RadioBroadcasterService : Service() {
    @Inject lateinit var repository: RadioRepository

    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private lateinit var snapserver: Deferred<Unit>
    private lateinit var snapclient: Deferred<Unit>

    private fun createChannel() {
        // Create a Notification channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "My Foreground Service Channel",
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
                // Create the notification to display while the service is running
                .build()
            ServiceCompat.startForeground(
                /* service = */ this,
                /* id = */ 100, // Cannot be 0
                /* notification = */ notification,
                /* foregroundServiceType = */
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
                // (e.g. started from bg)
                Log.d("CAPULLOWORKER", "Foreground service not allowed")
            } else
                Log.d("CAPULLOWORKER", "Error starting foreground service")
            // ...
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
            applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        )

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        // TODO: properly stop the service
        // TODO: stop the snapcast processes
        Log.d("CAPULLOWORKER", "Task removed")
        //snapserver.cancel()
        //snapclient.cancel()
        scope.cancel()
        stopSelf()
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
            throw RuntimeException(e)
        } catch (e: CancellationException) {
            Log.d("CAPULLOWORKER", "Worker stopped")
            throw RuntimeException(e)
        }
    }

    companion object {
        const val CHANNEL_ID = "RadioBroadcasterServiceChannel"
    }
}

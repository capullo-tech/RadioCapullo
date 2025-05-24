package tech.capullo.radio.snapcast

import android.app.Service.AUDIO_SERVICE
import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.os.Process
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import tech.capullo.radio.compose.AudioChannel
import tech.capullo.radio.data.RadioRepository
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.UUID
import javax.inject.Inject

class SnapclientProcess @Inject constructor(
    @ApplicationContext private val applicationContext: Context,
    radioRepository: RadioRepository,
) {

    private val nativeLibDir = radioRepository.getNativeLibDirPath()
    private val androidPlayer = if (Build.VERSION.SDK_INT <
        Build.VERSION_CODES.O
    ) {
        "opensl"
    } else {
        "oboe"
    }

    private val audioManager = applicationContext.getSystemService(AUDIO_SERVICE) as AudioManager
    private val rate: String? = audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE)
    private val fpb: String? = audioManager.getProperty(
        AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER,
    )
    private val sampleFormat = "$rate:16:*"

    suspend fun start(
        hostId: String = UUID.randomUUID().toString(),
        snapserverAddress: String = "localhost",
        snapserverPort: Int = 1704,
        audioChannel: Int = AudioChannel.entries.size / 2,
    ) = coroutineScope {
        val audioChannel = AudioChannel.entries[audioChannel].label.lowercase()
        val pb = ProcessBuilder().command(
            "$nativeLibDir/libsnapclient.so",
            "-h", snapserverAddress, "-p", snapserverPort.toString(),
            "--hostID", hostId, "--player", androidPlayer, "--sampleformat", sampleFormat,
            "--logfilter", "*:info,Stats:debug",
            "--channel", audioChannel,
        )

        val env = pb.environment()
        if (rate != null) env["SAMPLE_RATE"] = rate
        if (fpb != null) env["FRAMES_PER_BUFFER"] = fpb

        val process = pb.start()
        try {
            val bufferedReader = BufferedReader(
                InputStreamReader(process.inputStream),
            )
            var line: String?
            while (bufferedReader.readLine().also { line = it } != null) {
                ensureActive()
                val processId = Process.myPid()
                val threadName = Thread.currentThread().name
                println("Running on: $processId -  $threadName - ${line!!}")
            }
        } catch (e: CancellationException) {
            println("Snapclient process cancelled")
            process.destroy()
        } catch (e: Exception) {
            Log.e(TAG, "Error starting snapcast process", e)
        }
    }

    companion object {
        private val TAG = SnapclientProcess::class.java.simpleName
    }
}

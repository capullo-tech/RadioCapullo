package tech.capullo.radio.snapcast

import android.os.Process
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import tech.capullo.radio.data.RadioRepository
import java.io.BufferedReader
import java.io.InputStreamReader
import javax.inject.Inject

class SnapserverProcess @Inject constructor(radioRepository: RadioRepository) {

    private val nativeLibDir = radioRepository.getNativeLibDirPath()
    private val cacheDir = radioRepository.getCacheDirPath()
    private val pipeFilepath = radioRepository.getPipeFilepath()!!

    companion object {
        private const val STREAM_NAME: String = "name=RadioCapullo"
        private const val PIPE_MODE: String = "mode=read"
        private const val DRYOUT_MS: String = "dryout_ms=2000"
        private const val SAMPLE_FORMAT: String = "sampleformat=44100:16:2"

        private val pipeArgs = listOf(
            STREAM_NAME,
            PIPE_MODE,
            DRYOUT_MS,
            SAMPLE_FORMAT,
        ).joinToString("&")

        private val TAG = SnapclientProcess::class.java.simpleName
    }

    suspend fun start() = coroutineScope {
        val pb = ProcessBuilder()
            .command(
                "$nativeLibDir/libsnapserver.so",
                "--server.datadir=$cacheDir",
                "--stream.source",
                "pipe://$pipeFilepath?$pipeArgs",
            )
            .redirectErrorStream(true)

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
            println("Snapserver process cancelled")
            process.destroy()
        } catch (e: Exception) {
            Log.e(TAG, "Error starting snapcast process", e)
        }
    }
}

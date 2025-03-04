package tech.capullo.radio.snapcast

import android.os.Process
import android.util.Log
import tech.capullo.radio.data.RadioRepository
import tech.capullo.radio.services.RadioBroadcasterService.Companion.TAG
import java.io.BufferedReader
import java.io.InputStreamReader
import javax.inject.Inject

class SnapserverProcess @Inject constructor(
    radioRepository: RadioRepository,
    streamName: String = "name=RadioCapullo",
    pipeMode: String = "mode=read",
    dryoutMs: String = "dryout_ms=2000",
    sampleFormat: String = "sampleformat=44100:16:2",
) {

    val nativeLibDir = radioRepository.getNativeLibDirPath()
    val cacheDir = radioRepository.getCacheDirPath()
    val pipeFilepath = radioRepository.getPipeFilepath()!!

    private val pipeArgs = listOf(
        streamName,
        pipeMode,
        dryoutMs,
        sampleFormat,
    ).joinToString("&")

    fun start() {
        val pb = ProcessBuilder()
            .command(
                "$nativeLibDir/libsnapserver.so",
                "--server.datadir=$cacheDir",
                "--stream.source",
                "pipe://$pipeFilepath?$pipeArgs",
            )
            .redirectErrorStream(true)

        try {
            val process = pb.start()
            val bufferedReader = BufferedReader(
                InputStreamReader(process.inputStream),
            )
            var line: String?
            while (bufferedReader.readLine().also { line = it } != null) {
                val processId = Process.myPid()
                val threadName = Thread.currentThread().name
                println("Running on: $processId -  $threadName - ${line!!}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting snapcast process", e)
        }
    }

    companion object {
        private val TAG = SnapclientProcess::class.java.simpleName
    }
}

package tech.capullo.radio.snapcast

import android.os.Process
import android.util.Log
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import tech.capullo.radio.data.RadioRepository
import java.io.BufferedReader
import java.io.InputStreamReader
import javax.inject.Inject

class SnapserverProcess @Inject constructor(radioRepository: RadioRepository) {

    private val nativeLibDir = radioRepository.getNativeLibDirPath()
    private val cacheDir = radioRepository.getCacheDir()
    private val confFile = radioRepository.getSnapserverConfPath()
    private val pipeFilepath = radioRepository.getPipeFilepath()!!
    private val airplayPipeFilepath = radioRepository.getAirplayPipeFilepath()

    companion object {
        private const val PIPE_MODE: String = "mode=read"
        private const val DRYOUT_MS: String = "dryout_ms=2000"
        private const val SAMPLE_FORMAT: String = "sampleformat=44100:16:2"

        // codec=null hides the raw inputs from clients, so the meta stream
        // below is the default stream that new client groups attach to
        // (StreamManager::getDefaultStream skips null-codec streams).
        private val pipeArgs = listOf(
            PIPE_MODE,
            DRYOUT_MS,
            SAMPLE_FORMAT,
            "codec=null",
        ).joinToString("&")

        private val TAG = SnapserverProcess::class.java.simpleName
    }

    suspend fun start() = coroutineScope {
        val streamSources = mutableListOf(
            "--stream.source",
            "pipe://$pipeFilepath?name=Spotify&$pipeArgs" +
                "&controlscript=$nativeLibDir/libsnapcontrol.so",
        )
        if (airplayPipeFilepath != null) {
            streamSources += listOf(
                "--stream.source",
                "pipe://$airplayPipeFilepath?name=Airplay&$pipeArgs",
            )
        }
        // The meta stream activates whichever input is playing (earlier names
        // win on conflict) and must be declared after the streams it combines.
        val metaPath = if (airplayPipeFilepath != null) "Spotify/Airplay" else "Spotify"
        streamSources += listOf(
            "--stream.source",
            "meta:///$metaPath?name=RadioCapullo&$SAMPLE_FORMAT",
        )

        val pb = ProcessBuilder()
            .command(
                listOf(
                    "$nativeLibDir/libsnapserver.so",
                    "--config",
                    confFile,
                    "--server.datadir=$cacheDir",
                ) + streamSources,
            )
            .redirectErrorStream(true)

        val process = pb.start()
        // Cleanup lives in finally so cancellation propagates to the
        // supervisor (which distinguishes a clean stop from a crash); a normal
        // native exit returns and lets the supervisor restart.
        try {
            val bufferedReader = BufferedReader(
                InputStreamReader(process.inputStream),
            )
            var line: String?
            while (bufferedReader.readLine().also { line = it } != null) {
                ensureActive()
                val processId = Process.myPid()
                val threadName = Thread.currentThread().name
                // Log.d(TAG, "Running on: $processId -  $threadName - ${line!!}")
            }
        } finally {
            process.destroy()
            process.waitFor()
        }
    }
}

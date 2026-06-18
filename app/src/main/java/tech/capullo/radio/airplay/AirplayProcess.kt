package tech.capullo.radio.airplay

import android.util.Log
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import tech.capullo.radio.data.RadioRepository
import java.io.BufferedReader
import java.io.InputStreamReader
import javax.inject.Inject

/**
 * Runs shairport-sync (packaged by lib-shairport-android as an exec-able
 * libshairport.so) as a classic AirPlay receiver writing 44100:16:2 PCM to
 * the AirPlay FIFO, where snapserver picks it up as a stream source.
 */
class AirplayProcess @Inject constructor(private val radioRepository: RadioRepository) {

    private val nativeLibDir = radioRepository.getNativeLibDirPath()

    suspend fun start() = coroutineScope {
        val pipeFilepath = radioRepository.getAirplayPipeFilepath()
        if (pipeFilepath == null) {
            Log.e(TAG, "Could not create the AirPlay PIPE, not starting shairport-sync")
            return@coroutineScope
        }
        val confFile = radioRepository.getShairportConfPath(pipeFilepath)

        val pb = ProcessBuilder()
            .command(
                "$nativeLibDir/libshairport.so",
                "-c",
                confFile,
            )
            .redirectErrorStream(true)
        // Android hides MAC addresses from apps; shairport-sync's Android port
        // derives its AirPlay device ID from this variable instead.
        pb.environment()["SPS_DEVICE_ID"] = radioRepository.getAirplayDeviceId()

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
                Log.d(TAG, "shairport-sync: ${line!!}")
            }
        } finally {
            process.destroy()
            process.waitFor()
        }
    }

    companion object {
        private val TAG = AirplayProcess::class.java.simpleName
    }
}

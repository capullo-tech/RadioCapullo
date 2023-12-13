package tech.capullo.radio

import android.content.Context
import android.content.Context.AUDIO_SERVICE
import android.media.AudioManager
import android.os.Build
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.util.UUID

class RadioCapulloCoroutineWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val PREF_UNIQUE_ID = "PREF_UNIQUE_ID"

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val fili = "$applicationContext.cacheDir/filifo"
        Log.i("SESSION", "sessionChanged!: filifo:$fili")
        val cacheDir: String = applicationContext.cacheDir.toString()
        val nativeLibraryDir = applicationContext.applicationInfo.nativeLibraryDir

        val androidPlayer = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) "opensl" else "oboe"
        val rate: String?
        val fpb: String?
        val audioManager = applicationContext.getSystemService(AUDIO_SERVICE) as AudioManager
        rate =
            audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE)
        fpb =
            audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER)
        val sampleformat = "$rate:16:*"

        val deferred = async {
            snapcastRunnable(
                cacheDir,
                fili,
                nativeLibraryDir,
                true,
                getUniqueId(applicationContext), androidPlayer, sampleformat, rate, fpb
            ) {
                // Log.i("SERVICE", "SnapcastRunnable")
                Log.i("SNAPCAST", "Snapserver ")
            }
        }
        val deferred2 = async {
            snapcastRunnable(
                cacheDir, fili, nativeLibraryDir, false, getUniqueId(applicationContext),
                androidPlayer, sampleformat, rate, fpb
            ) {
                Log.i("SNAPCAST", "Snapclient ")
            }
        }
        Log.d("SNAPCAST", "before await")
        deferred.await()
        deferred2.await()
        Log.d("SNAPCAST", "after await")
        // Communicate with the main thread to update UI
        /*
        withContext(Dispatchers.Main) {
            // Update the Compose UI by updating the state variable
            (appContext as? Application)?.let {
                MyComposeViewModel.getInstance(it).updateDownloadedData(downloadedData)
            }
        }
         */

        Result.success()
    }

    private fun snapcastRunnable(
        cacheDir: String,
        filifoFile: String,
        nativeLibDir: String,
        isSnapserver: Boolean,
        uniqueId: String,
        player: String,
        sampleFormat: String,
        rate: String?,
        fpb: String?,
        callback: () -> Unit
    ) {

        val pb = if (isSnapserver) {
            ProcessBuilder()
                .command(
                    "$nativeLibDir/libsnapserver.so",
                    "--server.datadir=$cacheDir", "--stream.source",
                    "pipe://$cacheDir/filifo?name=fil&mode=create&dryout_ms=2000"
                )
                .redirectErrorStream(true)
        } else {
            ProcessBuilder().command(
                "$nativeLibDir/libsnapclient.so", "-h", "127.0.0.1", "-p",
                1704.toString(), "--hostID", uniqueId, "--player", player, "--sampleformat",
                sampleFormat, "--logfilter", "*:info,Stats:debug"
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
                Log.d("SNAPCAST${if (isSnapserver) "SERVER" else "CLIENT"}", line!!)
            }
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
        // handler.post { callback() }
    }

    @Synchronized
    fun getUniqueId(context: Context): String {
        val sharedPrefs = context.getSharedPreferences(
            PREF_UNIQUE_ID, ComponentActivity.MODE_PRIVATE
        )
        var uniqueID = sharedPrefs.getString(PREF_UNIQUE_ID, null)
        if (uniqueID == null) {
            uniqueID = UUID.randomUUID().toString()
            val editor = sharedPrefs.edit()
            editor.putString(PREF_UNIQUE_ID, uniqueID)
            editor.apply()
        }
        return uniqueID
    }
}

package tech.capullo.radio.viewmodels

import android.app.Notification
import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import tech.capullo.radio.R
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.util.UUID

class SnapcastCoroutineWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    @Synchronized
    fun getUniqueId(context: Context): String {
        val PREF_UNIQUE_ID = "PREF_UNIQUE_ID"
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

    override suspend fun doWork(): Result {
        val ip = inputData.getString("ip")
        val libDir = inputData.getString("libDir")

        // Use the parameters
        // ...
        val CHANNEL_ID = "snapcast_channel"
        val notification: Notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle("WorkManager is running in the foreground")
            .setTicker("WorkManager is running in the foreground")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .build()

        val NOTIFICATION_ID = 0
        val foregroundInfo = ForegroundInfo(NOTIFICATION_ID, notification)
        setForeground(foregroundInfo)

        // Simulate long running task
        withContext(Dispatchers.Default) {

            val audioManager = applicationContext
                .getSystemService(ComponentActivity.AUDIO_SERVICE) as AudioManager

            val player = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) "opensl" else "oboe"
            val rate: String? = audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE)
            val fpb: String? = audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER)
            val sampleFormat = "$rate:16:*"

            val uniqueId = getUniqueId(applicationContext)
            val pb =
                ProcessBuilder().command(
                    "$libDir/libsnapclient.so", "-h", ip, "-p", 1704.toString(),
                    "--hostID", uniqueId, "--player", player, "--sampleformat", sampleFormat,
                    "--logfilter", "*:info,Stats:debug"
                )


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
                        Log.d("SNAPCLIENT", "${line!!} ${Thread.currentThread().name}")
                }
            } catch (e: IOException) {
                throw RuntimeException(e)
            }
        }

        // Return result
        return Result.success()
    }
}
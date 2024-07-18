package tech.capullo.radio

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
import android.media.AudioManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import java.io.BufferedReader
import java.util.UUID

class SnapcastProcessWorker(context: Context, parameters: WorkerParameters) :
    CoroutineWorker(context, parameters) {
    @Synchronized
    fun getUniqueId(context: Context): String {
        val PREF_UNIQUE_ID = "PREF_UNIQUE_ID"

        val sharedPrefs = context.getSharedPreferences(
            PREF_UNIQUE_ID, Context.MODE_PRIVATE
        )
        var uniqueID: String? = sharedPrefs.getString(PREF_UNIQUE_ID, null)
        if (uniqueID == null) {
            uniqueID = UUID.randomUUID().toString()
            val editor = sharedPrefs.edit()
            editor.putString(PREF_UNIQUE_ID, uniqueID)
            editor.apply()
        }
        return uniqueID
    }
    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as
            NotificationManager

    override suspend fun doWork(): Result {
        // Mark the Worker as important
        setForeground(createForegroundInfo())

        val ip = inputData.getString(KEY_IP)
            ?: return Result.failure()

        val nativeLibDir = applicationContext.applicationInfo.nativeLibraryDir

        val audioManager =
            applicationContext
                .getSystemService(Context.AUDIO_SERVICE) as AudioManager

        val androidPlayer = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) "opensl" else "oboe"
        val rate: String? = audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE)
        val sampleFormat = "$rate:16:*"

        val snapclient = CoroutineScope(Dispatchers.IO).async {
            val processBuilder =
                ProcessBuilder().command(
                    "$nativeLibDir/libsnapclient.so", "-h", ip, "-p", 1704.toString(),
                    "--hostID", getUniqueId(applicationContext), "--player", androidPlayer,
                    "--sampleformat", sampleFormat,
                    "--logfilter", "*:info,Stats:debug"
                )

            val process = processBuilder.start()

            val reader = BufferedReader(process.inputStream.bufferedReader())
            reader.forEachLine { line ->
                if (isStopped) {
                    process.destroy()
                    return@forEachLine
                }
                Log.d("SNAPCLIENT", "$line ${Thread.currentThread().name}")
            }
        }
        snapclient.await()
        if (isStopped) {
            Log.d("SNAPCLIENT", "Worker stopped")
            return Result.failure()
        }
        return Result.success()
    }

    // Creates an instance of ForegroundInfo which can be used to update the
    // ongoing notification.
    private fun createForegroundInfo(): ForegroundInfo {
        // val id = applicationContext.getString(R.string.notification_channel_id)
        val title = "Radio capullo, radio capullo"
        val progress = "Una vuelta mas..."
        val cancel = "Cancel"
        // This PendingIntent can be used to cancel the worker
        val intent = WorkManager.getInstance(applicationContext)
            .createCancelPendingIntent(getId())

        // Create a Notification channel if necessary
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createChannel()
        }
        val notification = NotificationCompat.Builder(applicationContext, "MyServiceChannel")
            .setContentTitle(title)
            .setTicker(title)
            .setContentText(progress)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            // Add the cancel action to the notification which can
            // be used to cancel the worker
            .addAction(android.R.drawable.ic_delete, cancel, intent)
            .build()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(1234, notification, FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            ForegroundInfo(1234, notification)
        }
    }

    private fun createChannel() {
        // Create a Notification channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                "MyServiceChannel",
                "My Foreground Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(serviceChannel)
        }
    }

    companion object {
        const val KEY_IP = "KEY_IP"
    }
}

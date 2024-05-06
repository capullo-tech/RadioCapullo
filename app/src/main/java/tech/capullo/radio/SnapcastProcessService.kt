package tech.capullo.radio

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.util.UUID

class SnapcastProcessService : Service() {

    val coroutineScope = CoroutineScope(Dispatchers.IO)
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

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        createNotificationChannel()
        val notification: Notification = NotificationCompat.Builder(this, "MyServiceChannel")
            .setContentTitle("Radio capullo, radio capullo")
            .setContentText("Una vuelta mas...")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .build()

        startForeground(1, notification)

        // Your process here
        val ip = intent.getStringExtra("ip")

        val nativeLibDir = applicationContext.applicationInfo.nativeLibraryDir
        val audioManager =
            applicationContext
                .getSystemService(ComponentActivity.AUDIO_SERVICE) as AudioManager
        val uniqueId = getUniqueId(applicationContext)

        val androidPlayer = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) "opensl" else "oboe"
        val rate: String? = audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE)
        val fpb: String? = audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER)
        val sampleFormat = "$rate:16:*"

        coroutineScope.launch {
            val processBuilder =
                ProcessBuilder().command(
                    "$nativeLibDir/libsnapclient.so", "-h", ip, "-p", 1704.toString(),
                    "--hostID", uniqueId, "--player", androidPlayer, "--sampleformat", sampleFormat,
                    "--logfilter", "*:info,Stats:debug"
                )

            val process = processBuilder.start()

            val reader = BufferedReader(process.inputStream.bufferedReader())
            reader.forEachLine { line ->
                Log.d("SNAPCLIENT", "$line ${Thread.currentThread().name}")
            }
        }
        return START_NOT_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                "MyServiceChannel",
                "My Foreground Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )

            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.cancel()
    }
}

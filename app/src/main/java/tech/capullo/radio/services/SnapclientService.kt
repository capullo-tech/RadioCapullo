package tech.capullo.radio.services

import android.app.ForegroundServiceStartNotAllowedException
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import tech.capullo.radio.snapcast.SnapclientProcess
import javax.inject.Inject

/**
 * Worker that starts a Snapcast client process
 */
@AndroidEntryPoint
class SnapclientService : Service() {
    @Inject lateinit var snapclientProcess: SnapclientProcess

    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private var snapclientJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val snapserverIp = intent?.getStringExtra(KEY_IP) ?: return START_NOT_STICKY

        startForeground()
        startSnapclient(snapserverIp)

        return START_NOT_STICKY
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT,
            )
            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(serviceChannel)
        }
    }

    private fun startForeground() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                createChannel()
            }
            val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .build()
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                notification,
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
                Log.d(TAG, "Foreground service not allowed")
            } else {
                Log.d(TAG, "Error starting foreground service")
            }
        }
    }

    private fun startSnapclient(snapserverIp: String) {
        snapclientJob = scope.launch {
            snapclientProcess.start(snapserverIp)
        }
    }

    companion object {
        const val KEY_IP = "KEY_IP"
        const val CHANNEL_ID = "SnapclientServiceChannel"
        const val CHANNEL_NAME = "Snapclient Service Channel"
        const val NOTIFICATION_ID = 101
        val TAG: String = SnapclientService::class.java.simpleName
    }
}

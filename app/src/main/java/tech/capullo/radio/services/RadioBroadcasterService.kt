package tech.capullo.radio.services

import android.app.ForegroundServiceStartNotAllowedException
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import tech.capullo.radio.espoti.AudioFocusManager
import tech.capullo.radio.espoti.EspotiPlayerManager
import tech.capullo.radio.espoti.EspotiSessionRepository
import tech.capullo.radio.snapcast.SnapclientProcess
import tech.capullo.radio.snapcast.SnapserverProcess
import xyz.gianlu.librespot.core.Session
import xyz.gianlu.librespot.player.Player
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import javax.inject.Inject

@AndroidEntryPoint
class RadioBroadcasterService : Service() {
    @Inject lateinit var audioFocusManager: AudioFocusManager

    @Inject lateinit var espotiSessionRepository: EspotiSessionRepository

    @Inject lateinit var espotiPlayerManager: EspotiPlayerManager

    @Inject lateinit var snapclientProcess: SnapclientProcess

    @Inject lateinit var snapserverProcess: SnapserverProcess

    private val playbackExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private var player: Player? = null
    private var session: Session? = null

    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private lateinit var snapserverJob: Deferred<Unit>
    private lateinit var snapclientJob: Deferred<Unit>

    private val binder = LocalBinder()
    inner class LocalBinder : Binder() {
        fun getService(): RadioBroadcasterService = this@RadioBroadcasterService
    }

    private fun runOnPlayback(func: () -> Unit): Future<*>? = playbackExecutor.submit(func)

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT,
            )
            val notificationManager =
                getSystemService(NOTIFICATION_SERVICE) as
                    NotificationManager
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

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground()
        observeSessionState()

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        scope.cancel()
        player?.close()
        session?.close()
        playbackExecutor.shutdownNow()
        Log.d(TAG, "Stop Service")
        stopSelf()
    }

    private fun startLibrespot() {
        audioFocusManager.requestFocus()
        runOnPlayback { espotiPlayerManager.player.waitReady() }
    }

    private fun observeSessionState() {
        scope.launch {
            espotiSessionRepository.sessionStateFlow.collect { sessionState ->
                when (sessionState) {
                    is EspotiSessionRepository.SessionState.Created -> {
                        session = sessionState.session
                        // TODO: might need to handle player session thrown errors
                        espotiPlayerManager.createPlayer()
                        startLibrespot()

                        // Start snapcast processes after we have a valid session
                        startSnapcast()
                    }
                    is EspotiSessionRepository.SessionState.Error -> {
                        Log.d(TAG, "Session error: ${sessionState.message}")
                    }
                    else -> {}
                }
            }
        }
    }

    fun startSnapcast() {
        scope.launch {
            snapserverJob = async { snapserverProcess.start() }
            snapclientJob = async { snapclientProcess.start() }
            awaitAll(snapclientJob, snapserverJob)
        }
    }

    companion object {
        const val CHANNEL_ID = "RadioBroadcasterServiceChannel"
        const val CHANNEL_NAME = "Radio Broadcaster Service Channel"
        const val NOTIFICATION_ID = 100
        val TAG: String = RadioBroadcasterService::class.java.simpleName
    }
}

package tech.capullo.radio

import android.content.Context
import android.os.Process
import android.util.Log
import androidx.work.WorkerParameters
import androidx.work.multiprocess.RemoteCoroutineWorker
import com.powerbling.librespot_android_zeroconf_server.AndroidZeroconfServer
import com.spotify.connectstate.Connect
import xyz.gianlu.librespot.core.Session
import xyz.gianlu.librespot.player.Player
import xyz.gianlu.librespot.player.PlayerConfiguration
import java.io.File
import java.util.Locale
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class LibrespotPlayerWorker(
    context: Context,
    parameters: WorkerParameters
) : RemoteCoroutineWorker(context, parameters) {

    override suspend fun doRemoteWork(): Result {
        val processId = Process.myPid()
        val threadId = Thread.currentThread().id
        val threadName = Thread.currentThread().name

        Log.d(
            TAG,
            "Starting LibrespotPlayerWorker - " +
                "Process ID: $processId, Thread ID: $threadId, Thread Name: $threadName"
        )

        startAdvertisingSession()

        Log.d(
            TAG,
            "Session finished - " +
                "Process ID: $processId, Thread ID: $threadId, Thread Name: $threadName"
        )
        return Result.success()
    }

    private suspend fun startAdvertisingSession() {
        val executorService = Executors.newSingleThreadExecutor()

        suspendCoroutine { continuation ->
            val advertisingName = inputData.getString(DEVICE_NAME) ?: "Radio Capullo"
            val pipeFilepath = inputData.getString(PIPE_FILE_PATH) ?: ""

            val sessionListener = object : AndroidZeroconfServer.SessionListener {
                var boundPlayer: Player? = null

                override fun sessionChanged(session: Session) {
                    val callback = object : SessionChangedCallback {
                        override fun onPlayerReady(player: Player) {
                            Log.d(TAG, "Player ready")
                            boundPlayer = player
                        }

                        override fun onPlayerError(ex: Exception) {
                            Log.e(TAG, "Error preparing player", ex)
                        }
                    }
                    executorService.execute(
                        SessionChangedRunnable(
                            session,
                            pipeFilepath,
                            callback
                        )
                    )
                }

                override fun sessionClosing(session: Session) {
                    session.close()
                    boundPlayer?.close()
                    continuation.resume(Unit)
                }
            }

            executorService.execute(
                ZeroconfServerRunnable(
                    advertisingName,
                    sessionListener,
                    applicationContext
                )
            )
        }
    }

    private class ZeroconfServerRunnable(
        val advertisingName: String,
        val sessionListener: AndroidZeroconfServer.SessionListener,
        val applicationContext: Context
    ) : Runnable {
        override fun run() {
            val server = prepareLibrespotSession(advertisingName)
            server.addSessionListener(sessionListener)

            Runtime.getRuntime().addShutdownHook(
                Thread {
                    try {
                        server.closeSession()
                        server.close()
                    } catch (ex: Exception) {
                        Log.e(TAG, "Error closing Zeroconf server", ex)
                    }
                }
            )
        }

        private fun prepareLibrespotSession(advertisingName: String): AndroidZeroconfServer {
            // Configure the Spotify advertising session
            val conf = Session.Configuration.Builder()
                .setStoreCredentials(false)
                .setCacheEnabled(false)
                .build()
            val builder = AndroidZeroconfServer.Builder(applicationContext, conf)
                .setPreferredLocale(Locale.getDefault().language)
                .setDeviceType(Connect.DeviceType.SPEAKER)
                .setDeviceId(null)
                .setDeviceName(advertisingName)
            return builder.create()
        }
    }

    interface SessionChangedCallback {
        fun onPlayerReady(player: Player)
        fun onPlayerError(ex: Exception)
    }

    private class SessionChangedRunnable(
        val session: Session,
        val pipeFilepath: String,
        val callback: SessionChangedCallback
    ) : Runnable {
        override fun run() {
            val player = prepareLibrespotPlayer(session, pipeFilepath)
            try {
                player.waitReady()
                callback.onPlayerReady(player)
            } catch (ex: Exception) {
                Log.e(TAG, "Error waiting for player to be ready", ex)
                callback.onPlayerError(ex)
            }
        }

        private fun prepareLibrespotPlayer(session: Session, pipeFilepath: String): Player {
            val configuration = PlayerConfiguration.Builder()
                .setOutput(PlayerConfiguration.AudioOutput.PIPE)
                .setOutputPipe(File(pipeFilepath))
                .build()
            return Player(configuration, session)
        }
    }

    companion object {
        private const val TAG = "CAPULLOWORKER"
        private const val DEVICE_NAME = "DEVICE_NAME"
        private const val PIPE_FILE_PATH = "PIPE_FILE_PATH"
    }
}

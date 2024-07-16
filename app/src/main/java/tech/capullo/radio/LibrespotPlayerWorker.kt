package tech.capullo.radio

import android.content.Context
import android.os.Build
import android.os.Process
import android.util.Log
import androidx.work.WorkerParameters
import androidx.work.multiprocess.RemoteCoroutineWorker
import com.powerbling.librespot_android_zeroconf_server.AndroidZeroconfServer
import com.spotify.connectstate.Connect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import xyz.gianlu.librespot.android.sink.AndroidSinkOutput
import xyz.gianlu.librespot.core.Session
import xyz.gianlu.librespot.player.Player
import xyz.gianlu.librespot.player.PlayerConfiguration
import java.util.Locale

class LibrespotPlayerWorker(
    context: Context,
    parameters: WorkerParameters
) : RemoteCoroutineWorker(context, parameters) {
    override suspend fun doRemoteWork(): Result = withContext(Dispatchers.IO) {
        var processId = Process.myPid()
        var threadId = Thread.currentThread().id
        var threadName = Thread.currentThread().name

        Log.d(
            TAG,
            "Starting LibrespotPlayerWorker - " +
                "Process ID: $processId, Thread ID: $threadId, Thread Name: $threadName"
        )

        // Create a session listener to handle the session changes
        val sessionListener = object : AndroidZeroconfServer.SessionListener {
            override fun sessionClosing(session: Session) {
                TODO("Not yet implemented")
            }

            override fun sessionChanged(session: Session) {
                val player = prepareLibrespotPlayer(session)
                player?.let {
                    launch {
                        withContext(Dispatchers.Main) {
                            processId = Process.myPid()
                            threadId = Thread.currentThread().id
                            threadName = Thread.currentThread().name
                            Log.d(
                                TAG,
                                "Inside LibrespotPlayerWorker onSessionChanged - " +
                                    "Process ID: $processId, " +
                                    "Thread ID: $threadId, " +
                                    "Thread Name: $threadName"
                            )
                            player.play()
                        }
                    }
                }
            }
        }

        val server = prepareLibrespotSession()
        server.addSessionListener(sessionListener)

        return@withContext Result.success()
    }

    private fun prepareLibrespotSession(): AndroidZeroconfServer {
        val conf = Session.Configuration.Builder()
            .setDoCacheCleanUp(true)
            .setStoreCredentials(false)
            .setCacheEnabled(false)
            .build()
        val builder = AndroidZeroconfServer.Builder(applicationContext, conf)
            .setPreferredLocale(Locale.getDefault().language)
            .setDeviceType(Connect.DeviceType.SMARTPHONE)
            .setDeviceId(null)
            .setDeviceName(
                "Radio Capullo"
            )
        return builder.create()
    }

    private fun prepareLibrespotPlayer(session: Session): Player? {
        val configuration = PlayerConfiguration.Builder()
            .setOutput(PlayerConfiguration.AudioOutput.CUSTOM)
            .setOutputClass(AndroidSinkOutput::class.java.name)
            .build()
        val player = Player(configuration, session)

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.M) {
            while (!player.isReady) {
                try {
                    Thread.sleep(100)
                } catch (ex: InterruptedException) {
                    return null
                }
            }
        } else {
            try {
                player.waitReady()
            } catch (ex: InterruptedException) {
                return null
            }
        }
        return player
    }

    companion object {
        private const val TAG = "CAPULLOWORKER"
    }
}

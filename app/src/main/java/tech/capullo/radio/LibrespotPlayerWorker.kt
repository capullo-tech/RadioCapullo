package tech.capullo.radio

import android.content.Context
import android.os.Process
import android.util.Log
import androidx.work.WorkerParameters
import androidx.work.multiprocess.RemoteCoroutineWorker
import com.powerbling.librespot_android_zeroconf_server.AndroidZeroconfServer
import com.spotify.connectstate.Connect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import xyz.gianlu.librespot.core.Session
import xyz.gianlu.librespot.player.Player
import xyz.gianlu.librespot.player.PlayerConfiguration
import java.io.File
import java.util.Locale
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

    private suspend fun startAdvertisingSession() = withContext(Dispatchers.IO) {
        suspendCoroutine { continuation ->
            val advertisingName = inputData.getString(DEVICE_NAME) ?: "Radio Capullo"
            val pipeFilepath = inputData.getString(PIPE_FILE_PATH) ?: ""
            val server = prepareLibrespotSession(advertisingName)
            server.addSessionListener(object : AndroidZeroconfServer.SessionListener {
                lateinit var player: Player

                override fun sessionChanged(session: Session) {
                    player = prepareLibrespotPlayer(session, pipeFilepath)
                    Log.d(TAG, "Player got created successfully")
                }

                override fun sessionClosing(session: Session) {
                    session.close()
                    player.close()
                    continuation.resume(Unit)
                }
            })
        }
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

    private fun prepareLibrespotPlayer(session: Session, pipeFilepath: String): Player {
        val configuration = PlayerConfiguration.Builder()
            .setOutput(PlayerConfiguration.AudioOutput.PIPE)
            .setOutputPipe(File(pipeFilepath))
            .build()
        return Player(configuration, session)
    }

    companion object {
        private const val TAG = "CAPULLOWORKER"
        private const val DEVICE_NAME = "DEVICE_NAME"
        private const val PIPE_FILE_PATH = "PIPE_FILE_PATH"
    }
}

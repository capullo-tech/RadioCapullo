package tech.capullo.radio.snapcast

import android.app.Service.AUDIO_SERVICE
import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.os.Process
import android.util.Log
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import tech.capullo.radio.data.RadioRepository
import tech.capullo.radio.ui.model.AudioChannel
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.UUID
import javax.inject.Inject

class SnapclientProcess @Inject constructor(
    @ApplicationContext private val applicationContext: Context,
    radioRepository: RadioRepository,
) {

    private val nativeLibDir = radioRepository.getNativeLibDirPath()
    private val androidPlayer = if (Build.VERSION.SDK_INT <
        Build.VERSION_CODES.O
    ) {
        "opensl"
    } else {
        "oboe"
    }

    private val audioManager = applicationContext.getSystemService(AUDIO_SERVICE) as AudioManager
    private val rate: String? = audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE)
    private val fpb: String? = audioManager.getProperty(
        AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER,
    )
    private val sampleFormat = "$rate:16:*"

    enum class ConnectionState { STARTING, CONNECTED, ERROR }

    private val _connectionState = MutableStateFlow(ConnectionState.STARTING)
    val connectionState = _connectionState.asStateFlow()

    fun loadHostId(): String {
        val sharedPreferences = applicationContext.getSharedPreferences(
            "SNAPCAST_CLIENT_HOST_ID",
            Context.MODE_PRIVATE,
        )

        var hostId = sharedPreferences.getString(
            "SNAPCAST_CLIENT_HOST_ID_PREFERENCE",
            null,
        )

        if (hostId == null) {
            // Generate a new hostId
            hostId = UUID.randomUUID().toString()

            // Save it for future use
            sharedPreferences.edit {
                putString(
                    "SNAPCAST_CLIENT_HOST_ID_PREFERENCE",
                    hostId,
                )
            }

            Log.d(TAG, "Generating hostID for the first time: $hostId")
        }

        return hostId
    }

    suspend fun start(
        snapserverAddress: String = "localhost",
        snapserverPort: Int = 1704,
        audioChannel: Int = AudioChannel.STEREO.ordinal,
    ) = coroutineScope {
        val hostId = loadHostId()
        val audioChannel = AudioChannel.entries[audioChannel].label.lowercase()

        val pb = ProcessBuilder().command(
            "$nativeLibDir/libsnapclient.so",
            "--hostID", hostId,
            "--player", androidPlayer,
            "--sampleformat", sampleFormat,
            "--logfilter", "*:info,Stats:debug",
            "tcp://$snapserverAddress:$snapserverPort",
            "--channel", audioChannel,
        )

        val env = pb.environment()
        if (rate != null) env["SAMPLE_RATE"] = rate
        if (fpb != null) env["FRAMES_PER_BUFFER"] = fpb

        val process = pb.start()
        try {
            val bufferedReader = BufferedReader(
                InputStreamReader(process.inputStream),
            )
            var line: String?
            while (bufferedReader.readLine().also { line = it } != null) {
                ensureActive()

                // Example logs from snapclient process stdout

                // typing a host that doesn't exist - tries to resolve but fails
                // [Error] (Connection) Failed to resolve host 'srttrs', error: Host not found (authoritative)
                // [Error] (Controller) Error: Host not found (authoritative)
                // [Info] (Controller) Reconnecting
                // [Info] (Connection) Resolving host IP for: srttrs

                // typing a host that exists but doesn't have the port open or is refusing
                // [Info] (Connection) Connecting to host: 127.0.0.1:1704, port: 1704, protocol: tcp
                // [Error] (Connection) Failed to connect to host 'localhost', error: Connection refused
                // [Error] (Connection) Error in socket shutdown: Transport endpoint is not connected
                // [Error] (Controller) Error: Connection refused
                // [Info] (Controller) Reconnecting
                // [Info] (Connection) Resolving host IP for: localhost
                // [Info] (Connection) Connecting to host: 127.0.0.1:1704, port: 1704, protocol: tcp

                // connection got established but cancelled later on
                // [Error] (Connection) Error reading message header of length 0: End of file
                // [Error] (Controller) Error receiving next message: asio.misc:2

                // connection got established successfully
                // [Info] (Connection) Resolving host IP for: localhost
                // [Info] (Connection) Connecting to host: 127.0.0.1:1704, port: 1704, protocol: tcp
                // [Notice] (Connection) Connected to localhost

                line?.let { processStdout ->
                    if (processStdout.contains("[Error] (Connection)")) {
                        _connectionState.update { ConnectionState.ERROR }
                    }
                    if (processStdout.contains("[Notice] (Connection) Connected to")) {
                        _connectionState.update { ConnectionState.CONNECTED }
                    }
                }

                val processId = Process.myPid()
                val threadName = Thread.currentThread().name
                Log.d(TAG, "Running on: $processId -  $threadName - ${line!!}")
            }
        } catch (_: CancellationException) {
            Log.d(TAG, "Snapclient process cancelled")
            process.destroy()
            process.waitFor()
            Log.d(TAG, "Snapclient process destroyed")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting snapcast process", e)
        }
    }

    companion object {
        private val TAG = SnapclientProcess::class.java.simpleName
    }
}

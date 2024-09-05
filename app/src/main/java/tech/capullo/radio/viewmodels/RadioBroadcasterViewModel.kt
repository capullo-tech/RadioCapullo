package tech.capullo.radio.viewmodels

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.compose.runtime.toMutableStateList
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spotify.connectstate.Connect
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import tech.capullo.radio.data.AndroidZeroconfServer
import xyz.gianlu.librespot.android.sink.AndroidSinkOutput
import xyz.gianlu.librespot.core.Session
import xyz.gianlu.librespot.player.Player
import xyz.gianlu.librespot.player.PlayerConfiguration
import java.io.IOException
import java.net.NetworkInterface
import java.net.ServerSocket
import java.util.Collections
import java.util.Locale
import java.util.concurrent.Executors
import javax.inject.Inject

@HiltViewModel
class RadioBroadcasterViewModel @Inject constructor(
    @ApplicationContext private val applicationContext: Context,
): ViewModel() {
    private val server = prepareLibrespotSession(getDeviceName())
    private val _hostAddresses = getInetAddresses().toMutableStateList()
    private lateinit var nsdManager: NsdManager
    private var serverSocket: ServerSocket? = null

    val hostAddresses: List<String>
        get() = _hostAddresses

    private fun getInetAddresses(): List<String> =
        Collections.list(NetworkInterface.getNetworkInterfaces()).flatMap { networkInterface ->
            Collections.list(networkInterface.inetAddresses).filter { inetAddress ->
                inetAddress.hostAddress != null && inetAddress.hostAddress?.takeIf {
                    it.indexOf(":") < 0 && !inetAddress.isLoopbackAddress
                }?.let { true } ?: false
            }.map { it.hostAddress!! }
        }

    fun getDeviceName(): String =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            val deviceName = Settings.Global.getString(
                applicationContext.contentResolver,
                Settings.Global.DEVICE_NAME
            )
            if (deviceName == Build.MODEL) Build.MODEL else "$deviceName (${Build.MODEL})"
        } else {
            Build.MODEL
        }

    private fun prepareLibrespotSession(advertisingName: String): AndroidZeroconfServer {
        // Configure the Spotify advertising session
        val conf = Session.Configuration.Builder()
            .setStoreCredentials(false)
            .setCacheEnabled(false)
            .build()
        val builder = AndroidZeroconfServer.Builder(conf)
            .setPreferredLocale(Locale.getDefault().language)
            .setDeviceType(Connect.DeviceType.SMARTPHONE)
            .setDeviceId(null)
            .setDeviceName(advertisingName)
        return builder.create()
    }

    private fun startServerSocket(): Int {
        serverSocket = ServerSocket(0)
        return serverSocket?.localPort ?: -1
    }

    fun startNsdService() {
        server.addSessionListener(object : AndroidZeroconfServer.SessionListener {
            val executorService = Executors.newSingleThreadExecutor()
            override fun sessionClosing(session: Session) {
                session.close()
            }

            override fun sessionChanged(session: Session) {
                Log.d("NSD", "Session changed on thread: ${Thread.currentThread().name}")
                // start the execution service from the main thread
                executorService.execute(
                    SessionChangedRunnable(
                        session,
                        object : SessionChangedCallback {
                            override fun onPlayerReady(player: Player) {
                                Log.d("NSD", "Player ready")
                            }

                            override fun onPlayerError(ex: Exception) {
                                Log.e("NSD", "Error creating player", ex)
                            }
                        }
                    )
                )
            }
        })
        nsdManager = applicationContext.getSystemService(Context.NSD_SERVICE) as NsdManager

        val serviceInfo = NsdServiceInfo().apply {
            serviceName = "RadioCapullo"
            serviceType = "_spotify-connect._tcp"
            port = startServerSocket()
            Log.d("NSD", "Service port: $port")
        }

        startListening()
        nsdManager.registerService(
            serviceInfo,
            NsdManager.PROTOCOL_DNS_SD,
            registrationListener
        )
    }

    private val registrationListener = object : NsdManager.RegistrationListener {

        override fun onServiceRegistered(NsdServiceInfo: NsdServiceInfo) {
            // Save the service name. Android may have changed it in order to
            // resolve a conflict, so update the name you initially requested
            // with the name Android actually used.
            Log.d("NSD", "Service registered")
        }

        override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
            // Registration failed! Put debugging code here to determine why.
            Log.d("NSD", "Registration failed")
        }

        override fun onServiceUnregistered(arg0: NsdServiceInfo) {
            // Service has been unregistered. This only happens when you call
            // NsdManager.unregisterService() and pass in this listener.
            Log.d("NSD", "Service unregistered")
        }

        override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
            // Unregistration failed. Put debugging code here to determine why.
            Log.d("NSD", "Unregistration failed")
        }
    }

    private fun startListening() {
        serverSocket?.let { socket ->
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    while (true) {
                        Log.d("NSD", "Listening for clients on port: ${socket.localPort} thread: ${Thread.currentThread().name}")
                        val client = socket.accept()
                        Log.d("NSD", "Client connected: ${client.inetAddress}")
                        launch {
                            server.handle(client)
                        }
                    }
                } catch (e: IOException) {
                    Log.e("NSD", "Error accepting client connection", e)
                }
            }
        }
    }

    fun stopNsdService() {
        nsdManager.unregisterService(registrationListener)
    }

    interface SessionChangedCallback {
        fun onPlayerReady(player: Player)
        fun onPlayerError(ex: Exception)
    }

    private class SessionChangedRunnable(
        val session: Session,
        val callback: SessionChangedCallback
    ) : Runnable {
        override fun run() {
            val player = prepareLibrespotPlayer(session)
            try {
                player.waitReady()
                callback.onPlayerReady(player)
            } catch (ex: Exception) {
                Log.e("NSD", "Error waiting for player to be ready", ex)
                callback.onPlayerError(ex)
            }
        }

        private fun prepareLibrespotPlayer(session: Session): Player {
            val configuration = PlayerConfiguration.Builder()
                .setOutput(PlayerConfiguration.AudioOutput.CUSTOM)
                .setOutputClass(AndroidSinkOutput::class.java.name)
                .build()
            return Player(configuration, session)
        }
    }
    override fun onCleared() {
        super.onCleared()
        Log.d("ViewModel", "RadioViewModel destroyed")
        serverSocket?.close()
        nsdManager.unregisterService(registrationListener)
    }
}
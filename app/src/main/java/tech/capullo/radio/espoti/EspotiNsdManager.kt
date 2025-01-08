package tech.capullo.radio.espoti

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import com.spotify.connectstate.Connect
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class EspotiNsdManager @Inject constructor(
    @ApplicationContext private val applicationContext: Context,
) {
    private lateinit var server: EspotiZeroconfServer

    fun start(advertisingName: String, sessionListener: EspotiZeroconfServer.SessionListener) {
        // the server initializes the runnable inside the executor
        server =
            EspotiZeroconfServer(
                deviceType = Connect.DeviceType.SPEAKER,
                deviceName = advertisingName,
            )
        server.addSessionListener(sessionListener)

        val nsdManager = applicationContext.getSystemService(Context.NSD_SERVICE) as NsdManager

        val serviceInfo = NsdServiceInfo().apply {
            serviceName = "RadioCapullo"
            serviceType = "_spotify-connect._tcp"
            port = server.listenPort
            Log.d(TAG, "Service port: $port")
        }

        nsdManager.registerService(
            serviceInfo,
            NsdManager.PROTOCOL_DNS_SD,
            registrationListener,
        )
    }

    private val registrationListener = object : NsdManager.RegistrationListener {

        override fun onServiceRegistered(nsdServiceInfo: NsdServiceInfo) {
            // Save the service name. Android may have changed it in order to
            // resolve a conflict, so update the name you initially requested
            // with the name Android actually used.
            Log.d(TAG, "Service registered")
        }

        override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
            // Registration failed! Put debugging code here to determine why.
            Log.d(TAG, "Registration failed")
        }

        override fun onServiceUnregistered(arg0: NsdServiceInfo) {
            // Service has been unregistered. This only happens when you call
            // NsdManager.unregisterService() and pass in this listener.
            Log.d(TAG, "Service unregistered")
        }

        override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
            // Unregistration failed. Put debugging code here to determine why.
            Log.d(TAG, "Unregistration failed")
        }
    }

    // close both the nsdManager and the server
    fun stop() {
        server.close()
        val nsdManager = applicationContext.getSystemService(Context.NSD_SERVICE) as NsdManager
        nsdManager.unregisterService(registrationListener)
    }

    companion object {
        private val TAG = EspotiNsdManager::class.java.simpleName
    }
}

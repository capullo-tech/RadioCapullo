package tech.capullo.radio.espoti

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import com.spotify.connectstate.Connect
import dagger.hilt.android.qualifiers.ApplicationContext
import xyz.gianlu.librespot.core.Session
import javax.inject.Inject

class EspotiNsdManager @Inject constructor(
    @ApplicationContext private val applicationContext: Context,
) {

    fun start(
        advertisingName: String,
        sessionListener: EspotiZeroconfServer.SessionListener,
    ) {
        // the server initializes the runnable inside the executor
        val server = prepareLibrespotSession(advertisingName)
        server.addSessionListener(sessionListener)

        val nsdManager = applicationContext.getSystemService(Context.NSD_SERVICE) as NsdManager

        val serviceInfo = NsdServiceInfo().apply {
            serviceName = "RadioCapullo"
            serviceType = "_spotify-connect._tcp"
            port = server.listenPort
            Log.d("NSD", "Service port: $port")
        }

        nsdManager.registerService(
            serviceInfo,
            NsdManager.PROTOCOL_DNS_SD,
            registrationListener
        )
    }

    private val registrationListener = object : NsdManager.RegistrationListener {

        override fun onServiceRegistered(nsdServiceInfo: NsdServiceInfo) {
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

    private fun prepareLibrespotSession(advertisingName: String): EspotiZeroconfServer {
        // Configure the Spotify advertising session
        val conf = Session.Configuration.Builder()
            .setStoreCredentials(false)
            .setCacheEnabled(false)
            .build()
        return EspotiZeroconfServer(
            deviceType = Connect.DeviceType.SPEAKER,
            deviceName = advertisingName,
            conf = conf
        )
    }
}

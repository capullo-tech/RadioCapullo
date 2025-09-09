package tech.capullo.radio.snapcast

import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import javax.inject.Inject

class SnapserverNsdManager @Inject constructor(private val nsdManager: NsdManager) {

    private val registeredListeners = mutableListOf<NsdManager.RegistrationListener>()

    fun start() {
        val controlServiceInfo = NsdServiceInfo().apply {
            serviceName = SERVICE_NAME
            serviceType = SERVICE_TYPE
            port = SERVICE_PORT
        }

        val streamServiceInfo = NsdServiceInfo().apply {
            serviceName = SERVICE_NAME
            serviceType = STREAM_SERVICE_TYPE
            port = STREAM_SERVICE_PORT
        }

        registerNsdService(controlServiceInfo)
        registerNsdService(streamServiceInfo)
    }

    fun stop() {
        registeredListeners.forEach { listener ->
            try {
                nsdManager.unregisterService(listener)
            } catch (e: Exception) {
                Log.e(TAG, "Error unregistering service", e)
            }
        }
        registeredListeners.clear()
    }

    private fun registerNsdService(serviceInfo: NsdServiceInfo) {
        val registrationListener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(nsdServiceInfo: NsdServiceInfo) {
                Log.d(TAG, "Service registered: ${nsdServiceInfo.serviceType}")
            }

            override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Log.e(TAG, "Registration failed for ${serviceInfo.serviceType}: $errorCode")
            }

            override fun onServiceUnregistered(arg0: NsdServiceInfo) {
                Log.d(TAG, "Service unregistered: ${arg0.serviceType}")
            }

            override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Log.e(TAG, "Unregistration failed for ${serviceInfo.serviceType}: $errorCode")
            }
        }

        registeredListeners.add(registrationListener)

        nsdManager.registerService(
            serviceInfo,
            NsdManager.PROTOCOL_DNS_SD,
            registrationListener,
        )
    }

    companion object {
        private val TAG = SnapserverNsdManager::class.java.simpleName
        const val SERVICE_NAME = "Snapcast"
        const val SERVICE_TYPE = "_snapcast._tcp"
        const val SERVICE_PORT = 1704
        const val STREAM_SERVICE_TYPE = "_snapcast-stream._tcp"
        const val STREAM_SERVICE_PORT = 1705
    }
}

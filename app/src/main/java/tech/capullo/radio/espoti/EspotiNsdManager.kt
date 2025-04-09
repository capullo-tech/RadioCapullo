package tech.capullo.radio.espoti

import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class EspotiNsdManager @Inject constructor(
    private val nsdManager: NsdManager,
    val server: EspotiZeroconfServer,
) {

    suspend fun start() = coroutineScope {
        val listeningPort = server.initAndGetPort()

        val serviceInfo = NsdServiceInfo().apply {
            serviceName = "RadioCapullo"
            serviceType = "_spotify-connect._tcp"
            port = listeningPort
        }

        try {
            registerNsdService(serviceInfo)
            server.listen()
        } catch (e: Exception) {
            Log.e(TAG, "Error starting server", e)
        }
    }

    suspend fun registerNsdService(serviceInfo: NsdServiceInfo) =
        suspendCancellableCoroutine { continuation ->
            val registrationListener = object : NsdManager.RegistrationListener {
                override fun onServiceRegistered(nsdServiceInfo: NsdServiceInfo) {
                    continuation.resume(Unit)
                }

                override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                    continuation.resumeWithException(Exception("Registration failed: $errorCode"))
                }

                override fun onServiceUnregistered(arg0: NsdServiceInfo) {
                }

                override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                }
            }

            nsdManager.registerService(
                serviceInfo,
                NsdManager.PROTOCOL_DNS_SD,
                registrationListener,
            )

            continuation.invokeOnCancellation {
                nsdManager.unregisterService(registrationListener)
            }
        }

    companion object {
        private val TAG = EspotiNsdManager::class.java.simpleName
    }
}

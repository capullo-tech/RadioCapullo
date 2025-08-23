package tech.capullo.radio.snapcast

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SnapcastDiscoveryManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val tag = "SnapcastDiscoveryManager"

    private val nsdManager: NsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager

    private val _discoveredServers = MutableStateFlow<List<SnapcastServer>>(emptyList())
    val discoveredServers: StateFlow<List<SnapcastServer>> = _discoveredServers.asStateFlow()

    private val discoveredServices = mutableMapOf<String, SnapcastServer>()

    private val discoveryListener = object : NsdManager.DiscoveryListener {
        override fun onDiscoveryStarted(regType: String) {
            Log.d(tag, "Service discovery started")
        }

        override fun onServiceFound(service: NsdServiceInfo) {
            Log.d(tag, "Service found: ${service.serviceName}")
            when {
                service.serviceName.contains("snapcast", ignoreCase = true) ||
                    service.serviceType.contains("_snapcast._tcp") -> {
                    nsdManager.resolveService(service, resolveListener)
                }
            }
        }

        override fun onServiceLost(service: NsdServiceInfo) {
            Log.d(tag, "Service lost: ${service.serviceName}")
            discoveredServices.remove(service.serviceName)?.let {
                updateDiscoveredServers()
            }
        }

        override fun onDiscoveryStopped(serviceType: String) {
            Log.d(tag, "Discovery stopped: $serviceType")
        }

        override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
            Log.e(tag, "Discovery failed: Error code: $errorCode")
            nsdManager.stopServiceDiscovery(this)
        }

        override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
            Log.e(tag, "Stop discovery failed: Error code: $errorCode")
        }
    }

    private val resolveListener = object : NsdManager.ResolveListener {
        override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
            Log.e(tag, "Resolve failed: $errorCode")
        }

        override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
            Log.d(
                tag,
                "Service resolved: ${serviceInfo.serviceName} at ${serviceInfo.host}:${serviceInfo.port}",
            )

            val server = SnapcastServer(
                serviceName = serviceInfo.serviceName,
                host = serviceInfo.host?.hostAddress ?: return,
                port = serviceInfo.port,
            )

            discoveredServices[serviceInfo.serviceName] = server
            updateDiscoveredServers()
        }
    }

    fun startDiscovery() {
        try {
            nsdManager.discoverServices(
                "_snapcast._tcp",
                NsdManager.PROTOCOL_DNS_SD,
                discoveryListener,
            )
        } catch (e: Exception) {
            Log.e(tag, "Failed to start discovery", e)
        }
    }

    fun stopDiscovery() {
        try {
            nsdManager.stopServiceDiscovery(discoveryListener)
            discoveredServices.clear()
            _discoveredServers.value = emptyList()
        } catch (e: Exception) {
            Log.e(tag, "Failed to stop discovery", e)
        }
    }

    private fun updateDiscoveredServers() {
        _discoveredServers.value = discoveredServices.values.toList()
    }
}

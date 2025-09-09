package tech.capullo.radio.snapcast

import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Build
import android.os.ext.SdkExtensions
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

data class DiscoveredSnapserver(
    val serviceName: String,
    val serviceType: String,
    val hostAddress: String,
    val port: Int,
)

@Singleton
class SnapserverDiscoveryManager @Inject constructor(private val nsdManager: NsdManager) {

    private val _discoveredServices =
        MutableStateFlow<List<DiscoveredSnapserver>>(emptyList())
    val discoveredServices: StateFlow<List<DiscoveredSnapserver>> =
        _discoveredServices.asStateFlow()

    // this would be the data source
    val availableServers = mutableMapOf<String, Pair<NsdServiceInfo?, NsdServiceInfo?>>()

    private val discoveryListeners = mutableListOf<NsdManager.DiscoveryListener>()
    private val resolveListeners = mutableMapOf<String, NsdManager.ResolveListener>()

    private val discoveredServiceInfos = mutableMapOf<String, NsdServiceInfo>()

    fun startDiscovery() {
        startDiscoveryForServiceType(SnapserverNsdManager.SERVICE_TYPE)
        startDiscoveryForServiceType(SnapserverNsdManager.STREAM_SERVICE_TYPE)
    }

    fun stopDiscovery() {
        discoveryListeners.forEach { listener ->
            try {
                nsdManager.stopServiceDiscovery(listener)
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping service discovery", e)
            }
        }
        discoveryListeners.clear()

        resolveListeners.values.forEach { listener ->
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
                    SdkExtensions.getExtensionVersion(
                        Build.VERSION_CODES.TIRAMISU,
                    ) >= 7
                ) {
                    nsdManager.stopServiceResolution(listener)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping service resolution", e)
            }
        }
        resolveListeners.clear()

        discoveredServiceInfos.clear()
        _discoveredServices.value = emptyList()
    }

    private fun startDiscoveryForServiceType(serviceType: String) {
        val discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(regType: String) {
                Log.d(TAG, "Service discovery started for $regType")
            }

            override fun onServiceFound(service: NsdServiceInfo) {
                Log.d(
                    TAG,
                    "Service found: " +
                        "${service.serviceName} (${service.serviceType})",
                )
                discoveredServiceInfos[service.serviceName] = service
                resolveService(service)
            }

            override fun onServiceLost(service: NsdServiceInfo) {
                Log.d(TAG, "Service lost: ${service.serviceName}")
                discoveredServiceInfos.remove(service.serviceName)
                updateDiscoveredServices()
            }

            override fun onDiscoveryStopped(serviceType: String) {
                Log.d(TAG, "Service discovery stopped for $serviceType")
            }

            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e(TAG, "Start discovery failed for $serviceType: $errorCode")
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e(TAG, "Stop discovery failed for $serviceType: $errorCode")
            }
        }

        discoveryListeners.add(discoveryListener)

        nsdManager.discoverServices(
            serviceType,
            NsdManager.PROTOCOL_DNS_SD,
            discoveryListener,
        )
    }

    private fun resolveService(serviceInfo: NsdServiceInfo) {
        val resolveListener = object : NsdManager.ResolveListener {
            override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Log.e(TAG, "Resolve failed for ${serviceInfo.serviceName}: $errorCode")
                resolveListeners.remove(serviceInfo.serviceName)
            }

            override fun onServiceResolved(resolvedServiceInfo: NsdServiceInfo) {
                Log.d(
                    TAG,
                    "Service resolved: " +
                        "${resolvedServiceInfo.serviceName} at " +
                        "${resolvedServiceInfo.host?.hostAddress}:${resolvedServiceInfo.port}",
                )

                // Update the available servers map
                val resolvedHost = resolvedServiceInfo.host?.hostAddress ?: ""
                val existingServices = availableServers.getOrPut(
                    resolvedHost,
                ) { Pair(null, null) }
                when {
                    resolvedServiceInfo.serviceType.contains(
                        SnapserverNsdManager.SERVICE_TYPE,
                    ) -> {
                        availableServers[resolvedHost] =
                            Pair(resolvedServiceInfo, existingServices.second)
                        println(
                            "putting in pair: ${Pair(
                                resolvedServiceInfo,
                                existingServices.second,
                            )}",
                        )
                    }
                    resolvedServiceInfo.serviceType.contains(
                        SnapserverNsdManager.STREAM_SERVICE_TYPE,
                    ) -> {
                        availableServers[resolvedHost] =
                            Pair(existingServices.first, resolvedServiceInfo)
                        println(
                            "putting in pair: ${Pair(existingServices.first, resolvedServiceInfo)}",
                        )
                    }
                }

                // Update the list of discovered services
                _discoveredServices.value =
                    availableServers.filter { it.value.first != null || it.value.second != null }
                        .map {
                            val serviceInfo = when {
                                it.value.first != null -> it.value.first!!
                                else -> it.value.second!!
                            }
                            DiscoveredSnapserver(
                                serviceName = serviceInfo.serviceName,
                                serviceType = serviceInfo.serviceType,
                                hostAddress = serviceInfo.host?.hostAddress ?: "",
                                port = serviceInfo.port,
                            )
                        }

                resolveListeners.remove(resolvedServiceInfo.serviceName)
            }
        }

        resolveListeners[serviceInfo.serviceName] = resolveListener

        nsdManager.resolveService(serviceInfo, resolveListener)
    }

    // TODO: use the map instead
    private fun updateDiscoveredServices() {
        val currentServices = discoveredServiceInfos.values.mapNotNull { serviceInfo ->
            val hostAddress = serviceInfo.host?.hostAddress
            if (hostAddress != null) {
                DiscoveredSnapserver(
                    serviceName = serviceInfo.serviceName,
                    serviceType = serviceInfo.serviceType,
                    hostAddress = hostAddress,
                    port = serviceInfo.port,
                )
            } else {
                null
            }
        }
        _discoveredServices.value = currentServices
    }

    companion object {
        private val TAG = SnapserverDiscoveryManager::class.java.simpleName
    }
}

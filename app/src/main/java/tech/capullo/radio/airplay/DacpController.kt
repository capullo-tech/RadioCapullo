package tech.capullo.radio.airplay

import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Build
import android.os.ext.SdkExtensions
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject

/**
 * Sends DACP remote-control commands to the connected AirPlay sender so Snapcast
 * clients can drive transport/volume on the iPhone/Mac/iTunes.
 *
 * The sender advertises a `_dacp._tcp` service named `iTunes_Ctrl_<DACP-ID>`; we
 * resolve it to host:port via [NsdManager] (mirrors [tech.capullo.radio.snapcast
 * .SnapserverDiscoveryManager]) and issue `GET /ctrl-int/1/<command>` with the
 * `Active-Remote` header (shairport's own protocol — see dacp.c). Credentials
 * come from [AirplayMetadataReader] over shairport's metadata pipe.
 */
class DacpController @Inject constructor(private val nsdManager: NsdManager) {

    private data class DacpServer(val host: String, val port: Int)

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    @Volatile private var server: DacpServer? = null

    @Volatile private var activeRemote: String? = null
    private var targetDacpId: String? = null
    private var lastVolumePercent: Int = 100

    private var discoveryListener: NsdManager.DiscoveryListener? = null
    private val resolveListeners = mutableMapOf<String, NsdManager.ResolveListener>()

    /**
     * React to credential changes from the metadata pipe: (re)start `_dacp._tcp`
     * discovery for a new DACP-ID, refresh the Active-Remote token, or tear down
     * when the sender disconnects.
     */
    @Synchronized
    fun updateCredentials(credentials: DacpCredentials?) {
        if (credentials == null) {
            stopDiscovery()
            server = null
            activeRemote = null
            targetDacpId = null
            return
        }
        activeRemote = credentials.activeRemote
        if (credentials.dacpId != targetDacpId) {
            targetDacpId = credentials.dacpId
            server = null
            stopDiscovery()
            startDiscovery()
        }
    }

    @Synchronized
    fun stop() {
        stopDiscovery()
        server = null
        activeRemote = null
        targetDacpId = null
        scope.cancel()
    }

    // Transport / volume — mapped to DACP commands (dacp.c send_simple_dacp_command).
    fun play() = send("play")
    fun pause() = send("pause")
    fun playPause() = send("playpause")
    fun next() = send("nextitem")
    fun previous() = send("previtem")

    fun setVolume(percent: Int) {
        lastVolumePercent = percent.coerceIn(0, 100)
        send("setproperty?dmcp.volume=$lastVolumePercent")
    }

    fun setMute(muted: Boolean) {
        send("setproperty?dmcp.volume=${if (muted) 0 else lastVolumePercent}")
    }

    private fun send(command: String) {
        scope.launch { sendCommand(command) }
    }

    private suspend fun sendCommand(command: String) {
        val target = server
        val token = activeRemote
        if (target == null || token == null) {
            Log.d(TAG, "Dropping DACP '$command' — sender not resolved yet")
            return
        }
        withContext(Dispatchers.IO) {
            val host = if (target.host.contains(':')) "[${target.host}]" else target.host
            val conn = URL("http://$host:${target.port}/ctrl-int/1/$command")
                .openConnection() as HttpURLConnection
            try {
                conn.requestMethod = "GET"
                conn.connectTimeout = TIMEOUT_MS
                conn.readTimeout = TIMEOUT_MS
                conn.setRequestProperty("Active-Remote", token)
                val code = conn.responseCode
                if (code !in 200..299) Log.w(TAG, "DACP '$command' -> HTTP $code")
            } catch (e: Exception) {
                Log.e(TAG, "DACP '$command' failed", e)
            } finally {
                conn.disconnect()
            }
        }
    }

    private fun startDiscovery() {
        val listener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(serviceType: String) {}

            override fun onServiceFound(service: NsdServiceInfo) {
                if (matchesTarget(service.serviceName)) resolveService(service)
            }

            override fun onServiceLost(service: NsdServiceInfo) {
                if (matchesTarget(service.serviceName)) server = null
            }

            override fun onDiscoveryStopped(serviceType: String) {}

            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e(TAG, "DACP discovery start failed: $errorCode")
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e(TAG, "DACP discovery stop failed: $errorCode")
            }
        }
        discoveryListener = listener
        nsdManager.discoverServices(DACP_SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, listener)
    }

    private fun stopDiscovery() {
        discoveryListener?.let {
            try {
                nsdManager.stopServiceDiscovery(it)
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping DACP discovery", e)
            }
        }
        discoveryListener = null
        resolveListeners.values.forEach { stopResolution(it) }
        resolveListeners.clear()
    }

    private fun resolveService(serviceInfo: NsdServiceInfo) {
        if (resolveListeners.containsKey(serviceInfo.serviceName)) return
        val listener = object : NsdManager.ResolveListener {
            override fun onResolveFailed(info: NsdServiceInfo, errorCode: Int) {
                Log.e(TAG, "DACP resolve failed for ${info.serviceName}: $errorCode")
                resolveListeners.remove(info.serviceName)
            }

            override fun onServiceResolved(info: NsdServiceInfo) {
                val host = info.host?.hostAddress
                if (host != null) {
                    server = DacpServer(host, info.port)
                    Log.i(TAG, "DACP sender resolved: $host:${info.port}")
                }
                resolveListeners.remove(info.serviceName)
            }
        }
        resolveListeners[serviceInfo.serviceName] = listener
        nsdManager.resolveService(serviceInfo, listener)
    }

    private fun stopResolution(listener: NsdManager.ResolveListener) {
        // stopServiceResolution needs API 34 / T-extensions 7 (as in
        // SnapserverDiscoveryManager); on older devices the resolve just lapses.
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
                SdkExtensions.getExtensionVersion(Build.VERSION_CODES.TIRAMISU) >= 7
            ) {
                nsdManager.stopServiceResolution(listener)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping DACP resolution", e)
        }
    }

    // The sender advertises `iTunes_Ctrl_<DACP-ID>`; match the suffix against the
    // target DACP-ID, leading-zero/case-insensitive (mirrors mdns_avahi.c).
    private fun matchesTarget(serviceName: String): Boolean {
        val id = targetDacpId ?: return false
        if (!serviceName.startsWith(DACP_NAME_PREFIX)) return false
        val found = serviceName.removePrefix(DACP_NAME_PREFIX)
        return normalizeId(found).equals(normalizeId(id), ignoreCase = true)
    }

    private fun normalizeId(id: String): String = id.trimStart('0').ifEmpty { "0" }

    companion object {
        private val TAG = DacpController::class.java.simpleName
        private const val DACP_SERVICE_TYPE = "_dacp._tcp"
        private const val DACP_NAME_PREFIX = "iTunes_Ctrl_"
        private const val TIMEOUT_MS = 5000
    }
}

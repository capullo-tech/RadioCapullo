package tech.capullo.radio.snapcast

import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Process
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.suspendCancellableCoroutine
import tech.capullo.radio.data.RadioRepository
import java.io.BufferedReader
import java.io.InputStreamReader
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class SnapserverProcess @Inject constructor(
    private val nsdManager: NsdManager,
    radioRepository: RadioRepository,
) {

    private val nativeLibDir = radioRepository.getNativeLibDirPath()
    private val cacheDir = radioRepository.getCacheDirPath()
    private val pipeFilepath = radioRepository.getPipeFilepath()!!

    companion object {
        private const val STREAM_NAME: String = "name=RadioCapullo"
        private const val PIPE_MODE: String = "mode=read"
        private const val DRYOUT_MS: String = "dryout_ms=2000"
        private const val SAMPLE_FORMAT: String = "sampleformat=44100:16:2"
        private const val SERVICE_TYPE: String = "_snapcast._tcp"
        private const val STREAM_SERVICE_TYPE: String = "_snapcast-stream._tcp"
        private const val SERVICE_NAME: String = "Snapcast"

        private val pipeArgs = listOf(
            STREAM_NAME,
            PIPE_MODE,
            DRYOUT_MS,
            SAMPLE_FORMAT,
        ).joinToString("&")

        private val TAG = SnapserverProcess::class.java.simpleName
    }

    suspend fun start() = coroutineScope {
        val pb = ProcessBuilder()
            .command(
                "$nativeLibDir/libsnapserver.so",
                "--server.datadir=$cacheDir",
                "--stream.source",
                "pipe://$pipeFilepath?$pipeArgs",
            )
            .redirectErrorStream(true)

        val process = pb.start()
        // Register NSD service for Snapcast in its own try-catch block
        try {
            val serviceInfo = NsdServiceInfo().apply {
                serviceName = SERVICE_NAME
                serviceType = SERVICE_TYPE
                port = 1704 // Default Snapcast port
            }
            registerNsdService(serviceInfo)
            val streamServiceInfo = NsdServiceInfo().apply {
                serviceName = SERVICE_NAME
                serviceType = STREAM_SERVICE_TYPE
                port = 1705 // Default Snapcast port
            }
            registerNsdService(streamServiceInfo)
        } catch (e: Exception) {
            Log.e(TAG, "Error registering NSD service", e)
        }

        try {
            val bufferedReader = BufferedReader(
                InputStreamReader(process.inputStream),
            )
            var line: String?
            while (bufferedReader.readLine().also { line = it } != null) {
                ensureActive()
                val processId = Process.myPid()
                val threadName = Thread.currentThread().name
                println("Running on: $processId -  $threadName - ${line!!}")
            }
        } catch (e: CancellationException) {
            println("Snapserver process cancelled")
            process.destroy()
        } catch (e: Exception) {
            Log.e(TAG, "Error starting snapcast process", e)
        }
    }

    private suspend fun registerNsdService(serviceInfo: NsdServiceInfo) =
        suspendCancellableCoroutine { continuation ->
            val registrationListener = object : NsdManager.RegistrationListener {
                override fun onServiceRegistered(nsdServiceInfo: NsdServiceInfo) {
                    Log.d(TAG, "Snapcast service registered: ${nsdServiceInfo.serviceName}")
                    continuation.resume(Unit)
                }

                override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                    Log.e(TAG, "Snapcast service registration failed: $errorCode")
                    continuation.resumeWithException(Exception("Registration failed: $errorCode"))
                }

                override fun onServiceUnregistered(arg0: NsdServiceInfo) {
                    Log.d(TAG, "Snapcast service unregistered")
                }

                override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                    Log.e(TAG, "Snapcast service unregistration failed: $errorCode")
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
}

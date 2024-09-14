package tech.capullo.radio.viewmodels

import android.content.Context
import android.media.AudioManager
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Build
import android.os.Process
import android.util.Log
import androidx.compose.runtime.toMutableStateList
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spotify.connectstate.Connect
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import tech.capullo.radio.data.AndroidZeroconfServer
import tech.capullo.radio.data.RadioRepository
import xyz.gianlu.librespot.android.sink.AndroidSinkOutput
import xyz.gianlu.librespot.core.Session
import xyz.gianlu.librespot.player.Player
import xyz.gianlu.librespot.player.PlayerConfiguration
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.inject.Inject

@HiltViewModel
class RadioBroadcasterViewModel @Inject constructor(
    @ApplicationContext private val applicationContext: Context,
    private val repository: RadioRepository
): ViewModel() {
    private val _hostAddresses = repository.getInetAddresses().toMutableStateList()
    val executorService: ExecutorService = Executors.newSingleThreadExecutor()

    val hostAddresses: List<String>
        get() = _hostAddresses

    fun getDeviceName(): String = repository.getDeviceName()

    fun startNsdService() {
        val pipeFilepath = repository.getPipeFilepath()
        if (pipeFilepath == null) {
            Log.e("CAPULLOWORKER", "Error creating FIFO file")
            return
        }

        val sessionListener = object : AndroidZeroconfServer.SessionListener {
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
                        pipeFilepath,
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
        }

        executorService.execute(
            ZeroconfServerRunnable(
                getDeviceName(),
                sessionListener,
                applicationContext
            )
        )

        startSnapcast(
            pipeFilepath,
            repository.getCacheDirPath(),
            repository.getNativeLibDirPath(),
            applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        )
    }

    fun stopNsdService() {
        //nsdManager.unregisterService(registrationListener)
    }

    private class ZeroconfServerRunnable(
        val advertisingName: String,
        val sessionListener: AndroidZeroconfServer.SessionListener,
        val applicationContext: Context
    ) : Runnable {
        override fun run() {
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

            Runtime.getRuntime().addShutdownHook(
                Thread {
                    try {
                        server.closeSession()
                        server.close()
                    } catch (ex: Exception) {
                        Log.e("CAPULLO", "Error closing Zeroconf server", ex)
                    }
                }
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
        private fun prepareLibrespotSession(advertisingName: String): AndroidZeroconfServer {
            // Configure the Spotify advertising session
            val conf = Session.Configuration.Builder()
                .setStoreCredentials(false)
                .setCacheEnabled(false)
                .build()
            val builder = AndroidZeroconfServer.Builder(applicationContext, conf)
                .setPreferredLocale(Locale.getDefault().language)
                .setDeviceType(Connect.DeviceType.SPEAKER)
                .setDeviceId(null)
                .setDeviceName(advertisingName)
            return builder.create()
        }
    }

    interface SessionChangedCallback {
        fun onPlayerReady(player: Player)
        fun onPlayerError(ex: Exception)
    }

    private class SessionChangedRunnable(
        val session: Session,
        val pipeFilepath: String,
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
                //.setOutput(PlayerConfiguration.AudioOutput.CUSTOM)
                //.setOutputClass(AndroidSinkOutput::class.java.name)
                .setOutput(PlayerConfiguration.AudioOutput.PIPE)
                .setOutputPipe(File(pipeFilepath))
                .build()
            return Player(configuration, session)
        }
    }

    private fun startSnapcast(
        filifoFilepath: String,
        cacheDir: String,
        nativeLibraryDir: String,
        audioManager: AudioManager,
    ) {
        // Android audio configuration
        val androidPlayer = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) "opensl" else "oboe"
        val rate: String? = audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE)
        val fpb: String? = audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER)
        val sampleformat = "$rate:16:*"

        viewModelScope.launch {
            val snapserver = async {
                snapcastProcess(
                    filifoFilepath,
                    cacheDir,
                    nativeLibraryDir,
                    true,
                    UUID.randomUUID().toString(),
                    androidPlayer,
                    sampleformat,
                    rate,
                    fpb
                )
            }
            val snapclient = async {
                snapcastProcess(
                    filifoFilepath,
                    cacheDir,
                    nativeLibraryDir,
                    false,
                    UUID.randomUUID().toString(),
                    androidPlayer,
                    sampleformat,
                    rate,
                    fpb
                )
            }
            awaitAll(snapclient, snapserver)
        }
    }

    private suspend fun snapcastProcess(
        filifoFilepath: String,
        cacheDir: String,
        nativeLibDir: String,
        isSnapserver: Boolean,
        uniqueId: String,
        player: String,
        sampleFormat: String,
        rate: String?,
        fpb: String?
    ) = withContext(Dispatchers.IO) {
        val pb = if (isSnapserver) {
            val streamName = "name=RadioCapullo"
            val pipeMode = "mode=read"
            val dryoutMs = "dryout_ms=2000"
            val librespotSampleFormat = "sampleformat=44100:16:2"
            val pipeArgs = listOf(
                streamName, pipeMode, dryoutMs, librespotSampleFormat
            ).joinToString("&")

            ProcessBuilder()
                .command(
                    "$nativeLibDir/libsnapserver.so",
                    "--server.datadir=$cacheDir",
                    "--stream.source",
                    "pipe://$filifoFilepath?$pipeArgs",
                )
                .redirectErrorStream(true)
        } else {
            ProcessBuilder().command(
                "$nativeLibDir/libsnapclient.so", "-h", "127.0.0.1", "-p", 1704.toString(),
                "--hostID", uniqueId, "--player", player, "--sampleformat", sampleFormat,
                "--logfilter", "*:info,Stats:debug"
            )
        }
        try {
            val env = pb.environment()
            if (rate != null) env["SAMPLE_RATE"] = rate
            if (fpb != null) env["FRAMES_PER_BUFFER"] = fpb

            val process = pb.start()
            val bufferedReader = BufferedReader(
                InputStreamReader(process.inputStream)
            )
            var line: String?
            while (bufferedReader.readLine().also { line = it } != null) {
                val processId = Process.myPid()
                val threadName = Thread.currentThread().name
                val tag = if (isSnapserver) "SNAPSERVER" else "SNAPCLIENT"
                Log.d(tag, "Running on: $processId -  $threadName - ${line!!}")
            }
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }
    override fun onCleared() {
        super.onCleared()
        Log.d("ViewModel", "RadioViewModel destroyed")
    }
}
package tech.capullo.radio.viewmodels

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.AudioManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.pgreze.process.process
import com.powerbling.librespot_android_zeroconf_server.AndroidZeroconfServer
import com.spotify.connectstate.Connect
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import tech.capullo.radio.SnapcastProcessService
import xyz.gianlu.librespot.core.Session
import xyz.gianlu.librespot.player.Player
import xyz.gianlu.librespot.player.PlayerConfiguration
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.lang.Thread.sleep
import java.net.NetworkInterface
import java.util.Collections
import java.util.Locale
import java.util.UUID
import java.util.concurrent.Executors
import javax.inject.Inject

@HiltViewModel
class RadioViewModel @Inject constructor(
    @ApplicationContext private val applicationContext: Context
) : ViewModel() {
    private val _hostAddresses = getInetAddresses().toMutableStateList()
    private val PREF_UNIQUE_ID = "PREF_UNIQUE_ID"

    private val _snapclientsList = mutableListOf<String>().toMutableStateList()
    //@SuppressLint("MutableCollectionMutableState")
    //private val _snapserverServerStatus = mutableListOf<ServerStatus>().toMutableStateList()

    val hostAddresses: List<String>
        get() = _hostAddresses

    //val snapClientsList: SnapshotStateList<ServerStatus> get() = _snapserverServerStatus

    init {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                delay(10000)
                Log.d("THREAD", "10 delay done, on Thread: ${Thread.currentThread().name}")
                val sessionListener: AndroidZeroconfServer.SessionListener =
                    object : AndroidZeroconfServer.SessionListener {
                        override fun sessionClosing(p0: Session) {
                        }

                        override fun sessionChanged(session: Session) {
                            Log.d("THREAD", "spotify session:[$session], on Thread: ${Thread.currentThread().name}")
                            viewModelScope.launch(Dispatchers.Main) {
                                Log.d("THREAD", "calling snapcast, on Thread: ${Thread.currentThread().name}")
                                startSnapcast(
                                    applicationContext.cacheDir.toString(),
                                    applicationContext.applicationInfo.nativeLibraryDir,
                                    applicationContext
                                        .getSystemService(ComponentActivity.AUDIO_SERVICE) as AudioManager,
                                    session
                                )
                            }
                        }
                    }
                val conf = Session.Configuration.Builder()
                    .setDoCacheCleanUp(true)
                    .setStoreCredentials(false)
                    .setCacheEnabled(false)
                    .build()
                val builder = AndroidZeroconfServer.Builder(applicationContext, conf)
                    .setPreferredLocale(Locale.getDefault().language)
                    .setDeviceType(Connect.DeviceType.SPEAKER)
                    // .setDeviceId(null)
                    .setDeviceName( // Set name as set in preferences
                        // "Radio Capullo"
                        getDeviceName()
                    )
                val server = builder.create()
                server.addSessionListener(sessionListener)
            }
        }
    }

    fun getSharedPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences("MyApp", Context.MODE_PRIVATE)
    }

    fun saveText(text: String) {
        val editor = getSharedPreferences(applicationContext).edit()
        editor.putString("my_text", text)
        editor.apply()
    }

    fun getText(): String {
        return getSharedPreferences(applicationContext).getString("my_text", "") ?: ""
    }
    private fun isServiceRunning(serviceClass: Class<*>, context: Context): Boolean {
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                return true
            }
        }
        return false
    }
    fun initiateWorker(ip: String) {
        if (isServiceRunning(SnapcastProcessService::class.java, applicationContext)) {
            Log.d("SESSION", "Service already running")
            val stopIntent = Intent(applicationContext, SnapcastProcessService::class.java)
            applicationContext.stopService(stopIntent)
        } else {
            Log.d("SESSION", "Service not running")
        }

        val serviceIntent = Intent(applicationContext, SnapcastProcessService::class.java)
        serviceIntent.putExtra("ip", ip)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            applicationContext
                .startForegroundService(serviceIntent)
        } else {
            applicationContext
                .startService(serviceIntent)
        }
    }

    fun startSnapcast(
        cacheDir: String,
        nativelibraryDir: String,
        audioManager: AudioManager,
        session: Session
    ) {
        viewModelScope.launch {
            startListener(
                cacheDir, nativelibraryDir, audioManager, getUniqueId(applicationContext),
                session
            )
        }
    }
    @Synchronized
    fun getUniqueId(context: Context): String {
        val sharedPrefs = context.getSharedPreferences(
            PREF_UNIQUE_ID, ComponentActivity.MODE_PRIVATE
        )
        var uniqueID = sharedPrefs.getString(PREF_UNIQUE_ID, null)
        if (uniqueID == null) {
            uniqueID = UUID.randomUUID().toString()
            val editor = sharedPrefs.edit()
            editor.putString(PREF_UNIQUE_ID, uniqueID)
            editor.apply()
        }
        return uniqueID
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

    private suspend fun startListener(
        cacheDir: String,
        nativeLibraryDir: String,
        audioManager: AudioManager,
        uuid: String,
        session: Session,
    ) = withContext(Dispatchers.IO) {
        val executorService = Executors.newCachedThreadPool()

        val filifoFile = File("$cacheDir/filifo")
        if (filifoFile.exists()) {
            filifoFile.delete()
        }
        filifoFile.createNewFile()

        val androidPlayer = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) "opensl" else "oboe"
        val rate: String? = audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE)
        val fpb: String? = audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER)
        val sampleformat = "$rate:16:*"
        val snapserver = async {
            process(
                "$nativeLibraryDir/libsnapserver.so",
                "--server.datadir=$cacheDir", "--stream.source",
                "pipe://$cacheDir/filifo?name=fil&dryout_ms=2000"
            )
            Log.d("THREAD", "snapserver started, on Thread: ${Thread.currentThread().name}")
        }
        val player: Player
        val configuration = PlayerConfiguration.Builder()
            .setOutput(PlayerConfiguration.AudioOutput.PIPE)
            .setOutputPipe(filifoFile)
            .build()
        player = Player(configuration, session)
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.M) {
            while (!player.isReady) {
                try {
                    Log.d("THREAD", "waiting for librespot player, on Thread: ${Thread.currentThread().name}")
                    sleep(100)
                } catch (ex: InterruptedException) {
                    Log.d("THREAD", "waiting for librespot player exception: [$ex], on Thread: ${Thread.currentThread().name}")
                }
            }
        } else {
            try {
                player.waitReady()
            } catch (ex: InterruptedException) {
                Log.d("THREAD", "waiting for librespot player exception: [$ex], on Thread: ${Thread.currentThread().name}")
            }
        }
        player.play()

        val snapclient = async {
            snapcastRunnable(
                cacheDir, nativeLibraryDir, false, uuid, androidPlayer, sampleformat, rate, fpb
            )
            Log.d("THREAD", "snapclient started, on Thread: ${Thread.currentThread().name}")
        }
        awaitAll(snapclient, snapserver)
    }
}
private fun getInetAddresses(): List<String> =
    Collections.list(NetworkInterface.getNetworkInterfaces()).flatMap { networkInterface ->
        Collections.list(networkInterface.inetAddresses).filter { inetAddress ->
            inetAddress.hostAddress != null && inetAddress.hostAddress?.takeIf {
                it.indexOf(":") < 0 && !inetAddress.isLoopbackAddress
            }?.let { true } ?: false
        }.map { it.hostAddress!! }
    }
fun snapcastRunnable(
    cacheDir: String,
    nativeLibDir: String,
    isSnapserver: Boolean,
    uniqueId: String,
    player: String,
    sampleFormat: String,
    rate: String?,
    fpb: String?
) {
    val pb = if (isSnapserver) {
        ProcessBuilder()
            .command(
                "$nativeLibDir/libsnapserver.so",
                "--server.datadir=$cacheDir", "--stream.source",
                "pipe://$cacheDir/filifo?name=fil&mode=create&dryout_ms=2000"
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
            if (isSnapserver) {
                Log.d("SNAPSERVER", "${line!!} ${Thread.currentThread().name}")
            } else {
                Log.d("SNAPCLIENT", "${line!!} ${Thread.currentThread().name}")
            }
        }
    } catch (e: IOException) {
        throw RuntimeException(e)
    }
}

package tech.capullo.radio.viewmodels

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.core.content.ContextCompat.startForegroundService
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.powerbling.librespot_android_zeroconf_server.AndroidZeroconfServer
import com.spotify.connectstate.Connect
import control.RemoteControl
import control.json.Client
import control.json.Group
import control.json.ServerStatus
import control.json.Stream
import control.json.Volume
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
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
    @SuppressLint("MutableCollectionMutableState")
    private val _snapserverServerStatus = mutableListOf<ServerStatus>().toMutableStateList()

    val hostAddresses: List<String>
        get() = _hostAddresses

    val snapClientsList: SnapshotStateList<ServerStatus> get() = _snapserverServerStatus

    init {
        viewModelScope.launch {
            Log.d("SESSION", "about to start ${Thread.currentThread().name}")
            delay(10000)
            Log.d("SESSION", "onCreate! ${Thread.currentThread().name}")
            val executorService = Executors.newCachedThreadPool()

            val sessionListener: AndroidZeroconfServer.SessionListener =
                object : AndroidZeroconfServer.SessionListener {
                    override fun sessionClosing(p0: Session) {
                    }

                    override fun sessionChanged(session: Session) {
                        Log.d("SESSION", "onSessionChanged! ${Thread.currentThread().name}")
                        startSnapcast(
                            applicationContext.cacheDir.toString(),
                            applicationContext.applicationInfo.nativeLibraryDir,
                            applicationContext
                                .getSystemService(ComponentActivity.AUDIO_SERVICE) as AudioManager,
                            session
                        )
                    }
                }
            executorService.execute(
                SetupRunnable(
                    applicationContext,
                    getDeviceName(), sessionListener
                )
            )
        }
    }

    fun initiateWorker(ip: String) {
        val serviceIntent = Intent(applicationContext, SnapcastProcessService::class.java)
        serviceIntent.putExtra("ip", ip)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            applicationContext.
            startForegroundService(serviceIntent)
        } else {
            applicationContext.
            startService(serviceIntent)
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
    ) = withContext(Dispatchers.Default) {
        val executorService = Executors.newCachedThreadPool()

        val fili = "$cacheDir/filifo"
        Log.i("SESSION", "sessionChanged!: filifo:$fili ${Thread.currentThread().name}")

        val androidPlayer = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) "opensl" else "oboe"
        val rate: String? = audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE)
        val fpb: String? = audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER)
        val sampleformat = "$rate:16:*"
        executorService.execute(
            SnapcastRunnable(
                cacheDir,
                nativeLibraryDir,
                true,
                uuid,
                androidPlayer,
                sampleformat,
                rate,
                fpb
            ) {
                Log.i("SERVICE", "SnapcastRunnable")
            }
        )

        val filifofile = File(fili)
        executorService.execute(
            SessionChangedRunnable(
                session,
                filifofile,
                object : SessionChangedCallback {
                    override fun playerReady(player: Player, username: String) {
                        Log.i("SESSION", "playerReady! ")
                        executorService.execute(
                            SnapcastRunnable(
                                cacheDir,
                                nativeLibraryDir,
                                false,
                                uuid,
                                androidPlayer,
                                sampleformat,
                                rate,
                                fpb
                            ) {
                                Log.i("SERVICE", "SnapcastRunnable")
                            }
                        )
                    }

                    override fun failedGettingReady(ex: Exception) {
                        Log.i("SESSION", "playerFailedGettingReady!")
                    }
                }
            )
        )
        val remoteControl =
            RemoteControl(object : RemoteControl.RemoteControlListener {
                override fun onUpdate(server: ServerStatus?) {
                    Log.d("SESSION", "onUpdate: server:$server")
                    server?.let {
                        _snapserverServerStatus.add(it)
                    }
                }

                override fun onUpdate(streamId: String?, stream: Stream?) {
                }

                override fun onUpdate(group: Group?) {
                }

                override fun onUpdate(client: Client?) {
                }

                override fun onMute(
                    event: RemoteControl.RPCEvent?,
                    groupId: String?,
                    mute: Boolean
                ) {
                }

                override fun onStreamChanged(
                    event: RemoteControl.RPCEvent?,
                    groupId: String?,
                    streamId: String?
                ) {
                    Log.d("SESSION", "onLatencyChanged: ${Thread.currentThread().name}")
                }

                override fun onConnect(client: Client?) {
                    Log.d("SESSION", "onConnect: client:${client?.host}")
                    _snapclientsList.add(client?.host.toString())
                }

                override fun onDisconnect(clientId: String?) {
                    Log.d("SESSION", "onDisconnect: ${Thread.currentThread().name}")
                }

                override fun onVolumeChanged(
                    event: RemoteControl.RPCEvent?,
                    clientId: String?,
                    volume: Volume?
                ) {
                    val serverStatusss = ServerStatus(_snapserverServerStatus.first().toJson())
                    Log.d(
                        "SESSION",
                        "${Thread.currentThread().name} onVolumeChanged: $volume clientID:" +
                            " $clientId serverStatus: ${serverStatusss.toJson()}"
                    )

                    val serverStatusJson = serverStatusss.toJson()
                    if (serverStatusJson.has("groups")) {
                        val groups = serverStatusJson.getJSONArray("groups")
                        (0 until groups.length()).forEach { i ->
                            val element = groups.getJSONObject(i)
                            if (element.has("clients")) {

                                val clients = element.getJSONArray("clients")
                                (0 until clients.length()).forEach { j ->
                                    val client = clients.getJSONObject(j)
                                    if (client.getString("id").equals(clientId)) {
                                        val volumee =
                                            client
                                                .getJSONObject("config")
                                                .getJSONObject("volume")
                                                .get("percent")
                                        Log.d(
                                            "SESSION",
                                            "${Thread.currentThread().name} " +
                                                "updating volume status before: $serverStatusJson"
                                        )
                                        val jjj =
                                            JSONObject(
                                                serverStatusJson.toString().replaceFirst(
                                                    oldValue = volumee.toString(),
                                                    newValue = volume?.percent.toString()
                                                )
                                            )
                                        Log.d(
                                            "SESSION",
                                            "${Thread.currentThread().name} " +
                                                "updating volume status after: " +
                                                "${ServerStatus(jjj)}"
                                        )
                                        _snapserverServerStatus.clear()
                                        _snapserverServerStatus.add(ServerStatus(jjj))
                                    }
                                    Log.i(
                                        "SESSION",
                                        "each :${client::class.java} -- " +
                                            "$client"
                                    )

                                    // client.getJSONObject("config").getJSONObject("volume").
                                }
                            }
                        }
                        Log.i("SESSION", "serverStatus key:${groups::class.java} $groups")
                    }
                    volume?.percent
                }

                override fun onLatencyChanged(
                    event: RemoteControl.RPCEvent?,
                    clientId: String?,
                    latency: Long
                ) {
                    Log.d("SESSION", "onLatencyChanged: ${Thread.currentThread().name}")
                }

                override fun onNameChanged(
                    event: RemoteControl.RPCEvent?,
                    clientId: String?,
                    name: String?
                ) {
                    Log.d("SESSION", "onNameChanged: ${Thread.currentThread().name}")
                }

                override fun onConnected(remoteControl: RemoteControl?) {
                    Log.d("SESSION", "onConnected: $remoteControl")
                    remoteControl?.getServerStatus()
                }

                override fun onConnecting(remoteControl: RemoteControl?) {
                    Log.d("SESSION", "onConnecting: ${remoteControl?.host}")
                }

                override fun onDisconnected(
                    remoteControl: RemoteControl?,
                    e: java.lang.Exception?
                ) {
                    Log.d("SESSION", "onDisconnected: ${remoteControl?.host} $e")
                }

                override fun onBatchStart() {
                    Log.d("SESSION", "onBatchStart: ${Thread.currentThread().name}")
                }

                override fun onBatchEnd() {
                    Log.d("SESSION", "onBatchEnd: ${Thread.currentThread().name}")
                }
            })
        remoteControl.connect(getInetAddresses().first(), 1705)
        remoteControl.getServerStatus()
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
private class SnapcastRunnable(
    private val cacheDir: String,
    private val nativeLibDir: String,
    private val isSnapserver: Boolean,
    private val uniqueId: String,
    private val player: String,
    private val sampleFormat: String,
    private val rate: String?,
    private val fpb: String?,
    private var callback: () -> Unit
) :
    Runnable {

    private val handler = Handler(Looper.getMainLooper())

    override fun run() {
        Log.d("SERVICE", "$nativeLibDir -- $cacheDir")
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
        handler.post { callback() }
    }
}

private interface SessionChangedCallback {
    fun playerReady(player: Player, username: String)
    fun failedGettingReady(ex: Exception)
}
private class SessionChangedRunnable(
    private val session: Session,
    private val filifoFile: File,
    private val callback: SessionChangedCallback
) : Runnable {
    private val handler = Handler(Looper.getMainLooper())
    override fun run() {
        Log.i("SESSION", "Connected to: " + session.username())
        val player: Player
        val configuration = PlayerConfiguration.Builder()
            .setOutput(PlayerConfiguration.AudioOutput.PIPE)
            .setOutputPipe(filifoFile)
            .build()
        player = Player(configuration, session)
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.M) {
            while (!player.isReady) {
                try {
                    Log.i("SESSION", "waiting for player...")
                    Thread.sleep(100)
                } catch (ex: InterruptedException) {
                    handler.post { callback.failedGettingReady(ex) }
                    return
                }
            }
        } else {
            try {
                Log.i("SESSION", "waiting for player...")
                player.waitReady()
            } catch (ex: InterruptedException) {
                handler.post { callback.failedGettingReady(ex) }
                return
            }
        }
        player.play()
        handler.post { callback.playerReady(player, session.username()) }
    }
}
private class SetupRunnable(
    private val context: Context,
    private val deviceName: String,
    private val sessionListener: AndroidZeroconfServer.SessionListener,
) : Runnable {
    override fun run() {
        Log.i("SESSION", "SetupRunnable run")
        try {
            val conf = Session.Configuration.Builder()
                .setStoreCredentials(false)
                .setCacheEnabled(false)
                .build()
            val builder = AndroidZeroconfServer.Builder(context, conf)
                .setPreferredLocale(Locale.getDefault().language)
                .setDeviceType(Connect.DeviceType.SPEAKER)
                .setDeviceId(null)
                .setDeviceName( // Set name as set in preferences
                    // "Radio Capullo"
                    deviceName
                )
            val server = builder.create()
            server.addSessionListener(sessionListener)
            // LibrespotHolder.set(server);
            Log.i("SESSION", "SetupRunnable created and added listener")
            Runtime.getRuntime().addShutdownHook(
                Thread {
                    try {
                        server.closeSession()
                        server.close()
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
            )
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}

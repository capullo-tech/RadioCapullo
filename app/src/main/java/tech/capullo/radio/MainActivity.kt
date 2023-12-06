package tech.capullo.radio

import android.content.ComponentName
import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.work.Data
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.multiprocess.RemoteListenableWorker.ARGUMENT_CLASS_NAME
import androidx.work.multiprocess.RemoteListenableWorker.ARGUMENT_PACKAGE_NAME
import androidx.work.multiprocess.RemoteWorkerService
import com.powerbling.librespot_android_zeroconf_server.AndroidZeroconfServer
import com.spotify.connectstate.Connect
import tech.capullo.radio.ui.theme.RadioTheme
import xyz.gianlu.librespot.core.Session
import xyz.gianlu.librespot.player.Player
import xyz.gianlu.librespot.player.PlayerConfiguration
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.net.NetworkInterface
import java.util.Collections
import java.util.Locale
import java.util.UUID
import java.util.concurrent.Executors


class MainActivity : ComponentActivity() {

    private val PACKAGE_NAME = "tech.capullo.radio"
    private val PREF_UNIQUE_ID = "PREF_UNIQUE_ID"
    private var workManager: WorkManager? = null
    //private val executorService = Executors.newSingleThreadExecutor()
    private val executorService = Executors.newCachedThreadPool()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        workManager = WorkManager.getInstance(this@MainActivity)
        val filifoFile = File(applicationContext.getExternalFilesDir(null), "filifo.out")
        try {
            if (filifoFile.exists()) filifoFile.delete()
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }

        val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
        val hostAddresses = mutableListOf<String>()
        interfaces.forEach { intf ->
            val addrs = Collections.list(intf.inetAddresses)
            addrs.forEach { addr ->
                if (!addr.isLoopbackAddress) {
                    //addr.hostAddress?.let { hostAddresses.add(it) }
                    addr.hostAddress?.takeIf { it.indexOf(":") < 0 }?.let { hostAddresses.add(it) }
                }
            }
        }

        setContent {
            RadioTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    //modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column {
                        Greeting("Radio Capullo")
                        Text("Discoverable as: ${getDeviceName(applicationContext)}")
                        LazyColumn(modifier = Modifier.padding(vertical = 4.dp)) {
                            items(items = hostAddresses) { name ->
                                Greeting(name = name)
                            }
                        }
                    }
                    //Button(onClick = { startService() }) {
                        //Text(text = "start service")
                    //}
                }
            }
        }

        // Setup session Listener
        val sessionListener: AndroidZeroconfServer.SessionListener = object : AndroidZeroconfServer.SessionListener {
            override fun sessionClosing(p0: Session) {
            }

            override fun sessionChanged(session: Session) {

                val fili = "$cacheDir/filifo"
                Log.i("SESSION", "sessionChanged!: filifo:$fili")
                val cacheDir: String = cacheDir.toString()
                val nativeLibraryDir = applicationInfo.nativeLibraryDir

                val androidPlayer = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) "opensl" else "oboe"
                val rate: String?
                val fpb: String?
                val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
                rate =
                    audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE)
                fpb =
                    audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER)
                val sampleformat: String = "$rate:16:*"
                executorService.execute(SnapcastRunnable(cacheDir, fili, nativeLibraryDir, true, getUniqueId(applicationContext), androidPlayer, sampleformat, rate, fpb) {
                    Log.i("SERVICE", "SnapcastRunnable")
                })

                val filifofile = File(fili)
                executorService.execute(SessionChangedRunnable(session, filifofile, object : SessionChangedCallback {
                    override fun playerReady(player: Player, username: String) {
                        Log.i("SESSION", "playerReady!")
                        executorService.execute(SnapcastRunnable(cacheDir, filifoFile.absolutePath, nativeLibraryDir, false, getUniqueId(applicationContext), androidPlayer, sampleformat, rate, fpb) {
                            Log.i("SERVICE", "SnapcastRunnable")
                        })
                    }

                    override fun failedGettingReady(ex: Exception) {
                        Log.i("SESSION", "playerFailedGettingReady!")
                    }

                }))
                /*
                executorService.execute(SessionChangedRunnable(session, filifofile, object : SessionChangedCallback {
                    override fun playerReady(player: Player, username: String) {
                        Log.i("SESSION", "playerReady!")

                        // TODO implement as a service or coroutineWorker because I'm player here

                        val cacheDir: String = cacheDir.toString()
                        val nativeLibraryDir = applicationInfo.nativeLibraryDir;

                        val androidPlayer = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) "opensl" else "oboe"
                        var rate: String? = null
                        var fpb: String? = null
                        var sampleformat = "*:16:*"
                        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
                        // boolean bta2dp = audioManager.isBluetoothA2dpOn();
                        // boolean bta2dp = audioManager.isBluetoothA2dpOn();
                        rate =
                            audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE)
                        fpb =
                            audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER)
                        sampleformat = "$rate:16:*"
                        executorService.execute(SnapcastRunnable(cacheDir, filifoFile.absolutePath, nativeLibraryDir, true, getUniqueId(applicationContext), androidPlayer, sampleformat) {
                            Log.i("SERVICE", "SnapcastRunnable")
                        })
                        executorService.execute(SnapcastRunnable(cacheDir, filifoFile.absolutePath, nativeLibraryDir, false, getUniqueId(applicationContext), androidPlayer, sampleformat) {
                            Log.i("SERVICE", "SnapcastRunnable")
                        })
                    }

                    override fun failedGettingReady(ex: Exception) {
                        Log.i("SESSION", "playerFailedGettingReady!")
                    }

                }))
                 */
            }
        }

        Log.i("SESSION", "about to start listener${getDeviceName(applicationContext)}")
        getUniqueId(applicationContext)
        executorService.execute(SetupRunnable(applicationContext, getDeviceName(applicationContext),sessionListener))
    }

    fun getDeviceName(appContext: Context): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            val deviceName = Settings.Global.getString(appContext.contentResolver, Settings.Global.DEVICE_NAME)
            if (deviceName == Build.MODEL) Build.MODEL else "$deviceName (${Build.MODEL})"
        } else {
            Build.MODEL
        }
    }
    @Synchronized
    fun getUniqueId(context: Context): String {
        val sharedPrefs = context.getSharedPreferences(
            PREF_UNIQUE_ID, MODE_PRIVATE
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
    private fun startService() {
        val serviceName = RemoteWorkerService::class.java.name
        val componentName = ComponentName(PACKAGE_NAME, serviceName)

        val data: Data = Data.Builder()
            .putString(ARGUMENT_PACKAGE_NAME, componentName.packageName)
            .putString(ARGUMENT_CLASS_NAME, componentName.className)
            .build()

        val oneTimeWorkRequest = OneTimeWorkRequest.Builder(RadioCapulloCoroutineWorker::class.java)
            .setInputData(data)
            .build()

        workManager?.enqueue(oneTimeWorkRequest)
    }
    private class SessionChangedRunnable internal constructor(
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



    private interface SessionChangedCallback {
        fun playerReady(player: Player, username: String)
        fun failedGettingReady(ex: Exception)
    }

    private class SetupRunnable constructor(
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
                        //"Radio Capullo"
                        deviceName
                    )
                val server = builder.create()
                server.addSessionListener(sessionListener)
                //LibrespotHolder.set(server);
                Log.i("SESSION", "SetupRunnable created and added listener")
                Runtime.getRuntime().addShutdownHook(Thread {
                    try {
                        server.closeSession()
                        server.close()
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                })
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    private class SnapcastRunnable internal constructor(
        private val cacheDir: String,
        private val filifoFile: String,
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
            Log.d("SERVICE", "$nativeLibDir -- $cacheDir -- ${filifoFile}")
            val pb = if (isSnapserver) {
                ProcessBuilder() //.command(this.getApplicationInfo().nativeLibraryDir + "/libsnapclient.so", "-h", host, "-p", Integer.toString(port), "--hostID", getUniqueId(this.getApplicationContext()), "--player", player, "--sampleformat", sampleformat, "--logfilter", "*:info,Stats:debug")
                    //.command(this.getApplicationInfo().nativeLibraryDir + "/libsnapserver.so", "--server.datadir="+cacheDir, "--stream.source", "tcp://127.0.0.1?name=android")
                    .command(
                        "$nativeLibDir/libsnapserver.so",
                        "--server.datadir=$cacheDir", "--stream.source",
                        "pipe://$cacheDir/filifo?name=fil&mode=create&dryout_ms=2000"
                    )
                    .redirectErrorStream(true)
            } else {
                ProcessBuilder().command("$nativeLibDir/libsnapclient.so", "-h", "127.0.0.1", "-p",
                    1704.toString(), "--hostID", uniqueId, "--player", player, "--sampleformat", sampleFormat, "--logfilter", "*:info,Stats:debug")
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
                    Log.d("SNAPCAST", line!!)
                }
            } catch (e: IOException) {
                throw RuntimeException(e)
            }
            handler.post { callback() }
        }
    }

}

@Composable
fun Greeting(
    name: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )

}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    RadioTheme {
        Greeting("Android")
    }
}
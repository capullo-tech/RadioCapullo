package tech.capullo.radio.viewmodels

import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import android.media.AudioManager
import android.os.Build
import android.os.Process
import android.provider.Settings
import android.util.Log
import androidx.compose.runtime.toMutableStateList
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.multiprocess.RemoteListenableWorker.ARGUMENT_CLASS_NAME
import androidx.work.multiprocess.RemoteListenableWorker.ARGUMENT_PACKAGE_NAME
import androidx.work.multiprocess.RemoteWorkerService
import androidx.work.workDataOf
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import tech.capullo.radio.LibrespotPlayerWorker
import tech.capullo.radio.SnapcastProcessWorker
import tech.capullo.radio.data.RadioRepository
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.NetworkInterface
import java.util.Collections
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class RadioViewModel @Inject constructor(
    @ApplicationContext private val applicationContext: Context,
    val radioRepository: RadioRepository,
) : ViewModel() {
    private val _hostAddresses = getInetAddresses().toMutableStateList()
    private val PREF_UNIQUE_ID = "PREF_UNIQUE_ID"

    val hostAddresses: List<String>
        get() = _hostAddresses

    private fun getSharedPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences("MyApp", Context.MODE_PRIVATE)
    }

    fun saveLastServerText(text: String) {
        val editor = getSharedPreferences(applicationContext).edit()
        editor.putString("my_text", text)
        editor.apply()
    }

    fun getLastServerText(): String {
        return getSharedPreferences(applicationContext).getString("my_text", "") ?: ""
    }

    @Synchronized
    fun getUniqueId(context: Context): String {
        val sharedPrefs = context.getSharedPreferences(
            PREF_UNIQUE_ID, Context.MODE_PRIVATE
        )

        var uniqueID: String? = sharedPrefs.getString(PREF_UNIQUE_ID, null)
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

    private fun getInetAddresses(): List<String> =
        Collections.list(NetworkInterface.getNetworkInterfaces()).flatMap { networkInterface ->
            Collections.list(networkInterface.inetAddresses).filter { inetAddress ->
                inetAddress.hostAddress != null && inetAddress.hostAddress?.takeIf {
                    it.indexOf(":") < 0 && !inetAddress.isLoopbackAddress
                }?.let { true } ?: false
            }.map { it.hostAddress!! }
        }

    fun startSpotifyBroadcasting() {
        val processId = Process.myPid()
        val threadId = Thread.currentThread().id
        val threadName = Thread.currentThread().name

        Log.d(
            "CAPULLOWORKER",
            "Starting from main process - " +
                "Process ID: $processId, " +
                "Thread ID: $threadId, " +
                "Thread Name: $threadName"
        )

        val pipeFilepath = radioRepository.getPipeFilepath()
        if (pipeFilepath == null) {
            Log.e("CAPULLOWORKER", "Error creating FIFO file")
            return
        }

        // Setup and initialize the librespot worker as a separate process
        val PACKAGE_NAME = "tech.capullo.radio"

        val serviceName = RemoteWorkerService::class.java.name
        val componentName = ComponentName(PACKAGE_NAME, serviceName)

        val data: Data = Data.Builder()
            .putString(ARGUMENT_PACKAGE_NAME, componentName.packageName)
            .putString(ARGUMENT_CLASS_NAME, componentName.className)
            .putString("DEVICE_NAME", getDeviceName())
            .putString("PIPE_FILE_PATH", pipeFilepath)
            .build()

        val oneTimeWorkRequest = OneTimeWorkRequest.Builder(LibrespotPlayerWorker::class.java)
            .setInputData(data)
            .build()

        WorkManager
            .getInstance(applicationContext)
            .enqueueUniqueWork(
                "librespotPlayerWorker",
                ExistingWorkPolicy.REPLACE,
                oneTimeWorkRequest
            )

        startSnapcast(
            filifoFilepath = pipeFilepath,
            cacheDir = radioRepository.getCacheDirPath(),
            nativeLibraryDir = radioRepository.getNativeLibDirPath(),
            audioManager =
            applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager,
        )
    }

    fun initiateWorker(ip: String) {
        val uploadWorkRequest: WorkRequest =
            OneTimeWorkRequestBuilder<SnapcastProcessWorker>()
                .setInputData(
                    workDataOf(
                        "KEY_IP" to ip
                    )
                )
                .build()

        WorkManager
            .getInstance(applicationContext)
            .enqueue(uploadWorkRequest)
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
                    getUniqueId(applicationContext),
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
                    getUniqueId(applicationContext),
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
}

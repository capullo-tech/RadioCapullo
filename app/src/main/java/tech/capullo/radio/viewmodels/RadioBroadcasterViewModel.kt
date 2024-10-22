package tech.capullo.radio.viewmodels

import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.os.Process
import android.util.Log
import androidx.compose.runtime.toMutableStateList
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import tech.capullo.radio.data.RadioRepository
import tech.capullo.radio.data.sp.SpNsdManager
import tech.capullo.radio.data.sp.SpZeroconfServer
import xyz.gianlu.librespot.core.Session
import xyz.gianlu.librespot.player.Player
import xyz.gianlu.librespot.player.PlayerConfiguration
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.util.UUID
import java.util.concurrent.Executors
import javax.inject.Inject

@HiltViewModel
class RadioBroadcasterViewModel @Inject constructor(
    @ApplicationContext private val applicationContext: Context,
    private val repository: RadioRepository,
    private val spNsdManager: SpNsdManager
) : ViewModel() {
    private val _hostAddresses = repository.getInetAddresses().toMutableStateList()

    val hostAddresses: List<String>
        get() = _hostAddresses

    fun getDeviceName(): String = repository.getDeviceName()

    fun startNsdService() {
        val pipeFilepath = repository.getPipeFilepath() ?: run {
            Log.e("CAPULLOWORKER", "Error creating FIFO file")
            return
        }

        val sessionListener = object : SpZeroconfServer.SessionListener {
            val executorService = Executors.newSingleThreadExecutor()
            override fun sessionClosing(session: Session) {
                session.close()
            }

            override fun sessionChanged(session: Session) {
                Log.d("NSD", "Session changed on thread: ${Thread.currentThread().name}")
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

        spNsdManager.start(getDeviceName(), sessionListener)

        startSnapcast(
            pipeFilepath,
            repository.getCacheDirPath(),
            repository.getNativeLibDirPath(),
            applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        )
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

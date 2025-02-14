package tech.capullo.radio

import android.app.Service.AUDIO_SERVICE
import android.media.AudioManager
import android.os.Build
import android.os.Process
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.ktor.network.selector.SelectorManager
import io.ktor.network.sockets.aSocket
import io.ktor.network.sockets.openReadChannel
import io.ktor.network.sockets.openWriteChannel
import io.ktor.utils.io.readUTF8Line
import io.ktor.utils.io.writeStringUtf8
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import tech.capullo.radio.data.PipeFileDataSource
import tech.capullo.radio.data.RadioAdvertisingDataSource
import tech.capullo.radio.data.RadioRepository
import tech.capullo.radio.data.SnapcastControlClient
import tech.capullo.radio.services.RadioBroadcasterService.Companion.TAG
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class SnapcastControlClientInstrumentedTest {
    @Test
    fun useAppContext() = runBlocking {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("tech.capullo.radio", appContext.packageName)

        val pipeFileDataSource = PipeFileDataSource(appContext)
        val radioAdvertisingDataSource = RadioAdvertisingDataSource(appContext)
        val radioRepository = RadioRepository(pipeFileDataSource, radioAdvertisingDataSource)

        val pipeFilepath = radioRepository.getPipeFilepath()!!

        val nativeLibDir = radioRepository.getNativeLibDirPath()
        val cacheDir = radioRepository.getCacheDirPath()

        /* SNAPSERVER */
        val streamName = "name=RadioCapullo"
        val pipeMode = "mode=read"
        val dryoutMs = "dryout_ms=2000"
        val librespotSampleFormat = "sampleformat=44100:16:2"
        val pipeArgs = listOf(
            streamName,
            pipeMode,
            dryoutMs,
            librespotSampleFormat,
        ).joinToString("&")

        val pb = ProcessBuilder()
            .command(
                "$nativeLibDir/libsnapserver.so",
                "--server.datadir=$cacheDir",
                "--stream.source",
                "pipe://$pipeFilepath?$pipeArgs",
            )
            .redirectErrorStream(true)

        launch(Dispatchers.IO) {
            try {
                val process = pb.start()
                val bufferedReader = BufferedReader(
                    InputStreamReader(process.inputStream),
                )
                var line: String?
                while (bufferedReader.readLine().also { line = it } != null) {
                    val processId = Process.myPid()
                    val threadName = Thread.currentThread().name
                    // Log.d("hihi", "Running on: $processId -  $threadName - ${line!!}")
                    println("Running on: $processId -  $threadName - ${line!!}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error starting snapcast process", e)
            }
        }

        /* SNAPCLIENT */
        // Android audio configuration
        val audioManager =
            appContext.getSystemService(AUDIO_SERVICE) as AudioManager
        val androidPlayer = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) "opensl" else "oboe"
        val rate: String? = audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE)
        val fpb: String? = audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER)
        val sampleFormat = "$rate:16:*"
        val snapcastPb = ProcessBuilder().command(
            "$nativeLibDir/libsnapclient.so", "-h", "127.0.0.1", "-p", 1704.toString(),
            "--hostID", "fromTest", "--player", androidPlayer, "--sampleformat", sampleFormat,
            "--logfilter", "*:info,Stats:debug",
        )
        launch(Dispatchers.IO) {
            try {
                val env = pb.environment()
                if (rate != null) env["SAMPLE_RATE"] = rate
                if (fpb != null) env["FRAMES_PER_BUFFER"] = fpb

                val process = snapcastPb.start()
                val bufferedReader = BufferedReader(
                    InputStreamReader(process.inputStream),
                )
                var line: String?
                while (bufferedReader.readLine().also { line = it } != null) {
                    val processId = Process.myPid()
                    val threadName = Thread.currentThread().name
                    // Log.d("hihi", "Running on: $processId -  $threadName - ${line!!}")
                    // println("Running on: $processId -  $threadName - ${line!!}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error starting snapcast process", e)
            }
        }

        val snapcastControlClient = SnapcastControlClient("127.0.0.1")
        /*
        launch {
            val processId = Process.myPid()
            val threadName = Thread.currentThread().name
            snapcastControlClient.initSocket()
            println("snapcastControlClient.rr() done on: $processId -  $threadName")
        }

        val processId = Process.myPid()
        val threadName = Thread.currentThread().name
        snapcastControlClient.sendCommand("Server.GetStatus")
        println("sendCommand done on: $processId -  $threadName")
         */

        launch {
            snapcastControlClient.initWebsocket()
        }
        delay(60_000)
    }

    private suspend fun client(hostname: String, port: Int) = coroutineScope {
        val selectorManager = SelectorManager(Dispatchers.IO)
        val socket = aSocket(selectorManager).tcp().connect(hostname, port)

        val receiveChannel = socket.openReadChannel()
        val sendChannel = socket.openWriteChannel(autoFlush = true)

        launch(Dispatchers.IO) {
            while (true) {
                val greeting = receiveChannel.readUTF8Line()
                if (greeting != null) {
                    println(greeting)
                } else {
                    println("Server closed a connection")
                    socket.close()
                    selectorManager.close()
                }
            }
        }

        val myMessage = "Hello, server"
        sendChannel.writeStringUtf8("$myMessage\n")
    }
}

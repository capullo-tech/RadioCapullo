package tech.capullo.radio

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import tech.capullo.radio.data.PipeFileDataSource
import tech.capullo.radio.data.RadioAdvertisingDataSource
import tech.capullo.radio.data.RadioRepository
import tech.capullo.radio.snapcast.SnapcastControlClient
import tech.capullo.radio.snapcast.SnapclientProcess
import tech.capullo.radio.snapcast.SnapserverProcess

@RunWith(AndroidJUnit4::class)
class SnapcastControlClientInstrumentedTest {

    private lateinit var radioRepository: RadioRepository
    private lateinit var appContext: android.content.Context

    @Before
    fun setup() {
        appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val pipeFileDataSource = PipeFileDataSource(appContext)
        val radioAdvertisingDataSource = RadioAdvertisingDataSource(appContext)
        radioRepository = RadioRepository(pipeFileDataSource, radioAdvertisingDataSource)
    }

    @Test
    fun testSnapcastControlClientConnection() = runBlocking {
        // Start a snapserver process
        val serverJob = launch(Dispatchers.IO) {
            val snapserverProcess = SnapserverProcess(radioRepository)
            snapserverProcess.start()
        }

        // Give server time to start
        delay(2000)

        // Start a snapclient process
        val clientJob = launch(Dispatchers.IO) {
            val snapclientProcess = SnapclientProcess(appContext, radioRepository)
            snapclientProcess.start()
        }

        // Give client time to connect
        delay(2000)

        // Create and test the control client
        val snapcastControlClient = SnapcastControlClient("127.0.0.1")

        // Start listening for server status updates
        val statusJob = launch {
            println("Listening for server status updates")
            val result = withTimeoutOrNull(5000) {
                snapcastControlClient.serverStatus.first()
            }
            println("Received server status: $result")
            assertNotNull("Should receive server status", result)

            println("Checking server status")
            result?.let {
                // Verify some basic structure of the response
                assertNotNull(it.result)
                assertNotNull(it.result.server)
                assertNotNull(it.result.server.groups)
                // assertTrue("Server should have a version", it.result.server.version.isNotEmpty())
            }
            println("Server status check complete")
        }

        // Initialize the websocket connection
        val controlClientJob = launch(Dispatchers.IO) {
            snapcastControlClient.initWebsocket()
        }

        // Wait for status job to complete
        println("Waiting for status job to complete")
        statusJob.join()
        println("Status job complete")

        // Clean up
        println("Cleaning up")
        clientJob.cancel()
        serverJob.cancel()
        controlClientJob.cancel()
        println("Cleanup complete")
    }
}

package tech.capullo.radio

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import tech.capullo.radio.data.ConfFileDataSource
import tech.capullo.radio.data.RadioAdvertisingDataSource
import tech.capullo.radio.data.RadioRepository
import tech.capullo.radio.snapcast.SnapcastControlClient
import tech.capullo.radio.snapcast.SnapclientProcess
import tech.capullo.radio.snapcast.SnapserverProcess

@RunWith(AndroidJUnit4::class)
class SnapclientProcessTest {

    private lateinit var radioRepository: RadioRepository
    private lateinit var appContext: android.content.Context

    @Before
    fun setup() {
        appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val confFileDataSource = ConfFileDataSource(appContext)
        val radioAdvertisingDataSource = RadioAdvertisingDataSource(appContext)
        radioRepository = RadioRepository(confFileDataSource, radioAdvertisingDataSource)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun snapclientSuccessfulFirstTryConnection() = runTest {
        // Setup
        backgroundScope.launch(Dispatchers.IO) { SnapserverProcess(radioRepository).start() }

        val snapclientProcess = SnapclientProcess(appContext, radioRepository)
        backgroundScope.launch(Dispatchers.IO) { snapclientProcess.start() }

        // Succesfull Connection on the first try
        val snapclientProcessConnectionState = snapclientProcess.connectionState.drop(1).first()
        assert(snapclientProcessConnectionState == SnapclientProcess.ConnectionState.CONNECTED)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun snapclientHandlesInvalidHostAddress() = runTest {
        val snapclientProcess =
            SnapclientProcess(appContext, radioRepository)

        backgroundScope.launch(Dispatchers.IO) {
            snapclientProcess.start(
                snapserverAddress = "someHostAddressThatFailsToResolve",
            )
        }
        val snapclientProcessConnectionState = snapclientProcess.connectionState.take(2).toList()
        assert(snapclientProcessConnectionState[0] == SnapclientProcess.ConnectionState.STARTING)
        assert(snapclientProcessConnectionState[1] == SnapclientProcess.ConnectionState.ERROR)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun snapclientRetryAndReconnectBehavior() = runTest {
        // Setup
        val snapclientProcess = SnapclientProcess(appContext, radioRepository)
        backgroundScope.launch(Dispatchers.IO) { snapclientProcess.start() }

        // connecting to a valid host (localhost) but no server is online yet
        var snapclientProcessConnectionState = snapclientProcess.connectionState.drop(1).first()
        assert(snapclientProcessConnectionState == SnapclientProcess.ConnectionState.ERROR)

        // bring the snapserver online...
        val serverJob = backgroundScope.launch(Dispatchers.IO) {
            SnapserverProcess(radioRepository).start()
        }
        // ... snapclient process has retry mechanism
        snapclientProcessConnectionState = snapclientProcess.connectionState.drop(1).first()
        assert(snapclientProcessConnectionState == SnapclientProcess.ConnectionState.CONNECTED)

        // plug the server off in the middle of a session
        serverJob.cancel()

        // trick to make the snapserver process stdout to print something
        // causing the readLine() function call [SnapserverProcess.kt:45] to unblock
        // and ensureActive to recognize the process has been canceled
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            // run it in a background scope since the control client has an auto retry mechanism
            try {
                val snapcastControlClient = SnapcastControlClient(
                    "127.0.0.1",
                    ioDispatcher = StandardTestDispatcher(testScheduler),
                )
                snapcastControlClient.initialize()
            } catch (_: Exception) {}
        }

        serverJob.join()

        snapclientProcessConnectionState = snapclientProcess.connectionState.first()
        assert(snapclientProcessConnectionState == SnapclientProcess.ConnectionState.ERROR)

        // make the server come back online, simulating connected -> disconected -> then reconnected
        // automatically
        backgroundScope.launch(Dispatchers.IO) { SnapserverProcess(radioRepository).start() }
        snapclientProcessConnectionState = snapclientProcess.connectionState.drop(1).first()
        assert(snapclientProcessConnectionState == SnapclientProcess.ConnectionState.CONNECTED)
    }
}

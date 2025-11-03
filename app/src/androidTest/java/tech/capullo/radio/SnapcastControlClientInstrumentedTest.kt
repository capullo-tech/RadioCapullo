package tech.capullo.radio

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import tech.capullo.radio.data.ConfFileDataSource
import tech.capullo.radio.data.RadioAdvertisingDataSource
import tech.capullo.radio.data.RadioRepository
import tech.capullo.radio.snapcast.ServerGetStatusResponse
import tech.capullo.radio.snapcast.ServerOnUpdate
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
        val confFileDataSource = ConfFileDataSource(appContext)
        val radioAdvertisingDataSource = RadioAdvertisingDataSource(appContext)
        radioRepository = RadioRepository(confFileDataSource, radioAdvertisingDataSource)
    }

    @Test
    fun serverGetStatusAfterOneClientConnection() = runTest {
        // Setup
        backgroundScope.launch(Dispatchers.IO) {
            SnapserverProcess(radioRepository).start()
        }

        val snapcastControlClient = SnapcastControlClient(
            "127.0.0.1",
            ioDispatcher = StandardTestDispatcher(testScheduler),
        )
        snapcastControlClient.initialize()

        // no client connections yet, querying for the status should return us an empty list
        snapcastControlClient.sendGetStatus()

        var notification = snapcastControlClient.notifications.first()

        assert(notification is ServerGetStatusResponse)
        (notification as ServerGetStatusResponse).also { serverGetStatusResponse ->
            assert(serverGetStatusResponse.result.server.groups.isEmpty())
        }

        // expecting a serverOnUpdate right after a snapclient has connected
        backgroundScope.launch(Dispatchers.IO) {
            SnapclientProcess(appContext, radioRepository).start()
        }

        notification = snapcastControlClient.notifications.first()

        assert(notification is ServerOnUpdate)
        (notification as ServerOnUpdate).also { serverOnUpdate ->
            assert(serverOnUpdate.params.server.groups.size == 1)
        }

        // querying again for the server.getStatus should be consistent
        snapcastControlClient.sendGetStatus()
        notification = snapcastControlClient.notifications.first()

        assert(notification is ServerGetStatusResponse)
        (notification as ServerGetStatusResponse).also { serverGetStatusResponse ->
            assert(serverGetStatusResponse.result.server.groups.size == 1)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun notificationCollectionDuringSnapserverShutdown() = runTest {
        // Setup
        val serverJob = backgroundScope.launch(Dispatchers.IO) {
            SnapserverProcess(radioRepository).start()
        }

        val snapcastControlClient = SnapcastControlClient(
            "127.0.0.1",
            ioDispatcher = StandardTestDispatcher(testScheduler),
        )
        snapcastControlClient.initialize()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            snapcastControlClient.notifications
                // If it wasn't for this catch, the collection would fail...
                .catch { exception ->
                    println("caught: $exception")
                }
                .collect {
                    println("collected: $it")
                }
        }

        snapcastControlClient.sendGetStatus()

        val clientJob = backgroundScope.launch(Dispatchers.IO) {
            SnapclientProcess(appContext, radioRepository).start()
        }

        serverJob.cancelAndJoin()
        clientJob.cancelAndJoin()
        assert(serverJob.isCancelled)

        // ...therefore we count this test being able to finish as "passing"
    }
}

package tech.capullo.radio

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import tech.capullo.radio.data.ConfFileDataSource
import tech.capullo.radio.data.RadioAdvertisingDataSource
import tech.capullo.radio.data.RadioRepository
import tech.capullo.radio.snapcast.ClientOnConnect
import tech.capullo.radio.snapcast.ClientOnDisconnect
import tech.capullo.radio.snapcast.ServerGetStatusResponse
import tech.capullo.radio.snapcast.ServerOnUpdate
import tech.capullo.radio.snapcast.SnapcastControlClient
import tech.capullo.radio.snapcast.SnapclientProcess
import tech.capullo.radio.snapcast.SnapserverProcess

@RunWith(AndroidJUnit4::class)
class SnapcastControlClientTest {

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
    fun serverClientOnDisconnectOnConnect() = runTest {
        // Setup
        backgroundScope.launch(Dispatchers.IO) {
            SnapserverProcess(radioRepository).start()
        }

        // A client's first ever connection will trigger a ServerOnUpdate notification...
        val clientJob = backgroundScope.launch(Dispatchers.IO) {
            SnapclientProcess(appContext, radioRepository).start()
        }

        val snapcastControlClient = SnapcastControlClient(
            "127.0.0.1",
            ioDispatcher = StandardTestDispatcher(testScheduler),
        )
        // the control client will do a status query as part of its init routine
        snapcastControlClient.initialize()

        clientJob.cancelAndJoin()

        // skip the initial getServerStatus and serverOnUpdate
        var notification = snapcastControlClient.notifications.drop(2).first()
        assert(notification is ClientOnDisconnect)

        // ...subsequent connections of a particular client will trigger ClientOnConnect
        backgroundScope.launch(Dispatchers.IO) {
            SnapclientProcess(appContext, radioRepository).start()
        }
        notification = snapcastControlClient.notifications.first()
        assert(notification is ClientOnConnect)
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
        // the control client will do a status query as part of its init routine
        snapcastControlClient.initialize()

        // no client connections yet, the initial status query should return us an empty list
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

    @Test
    fun serverOnLatencyChanged() = runTest {
        // Setup
        backgroundScope.launch(Dispatchers.IO) {
            SnapserverProcess(radioRepository).start()
        }

        backgroundScope.launch(Dispatchers.IO) {
            SnapclientProcess(appContext, radioRepository).start()
        }

        val snapcastControlClient = SnapcastControlClient(
            "127.0.0.1",
            ioDispatcher = StandardTestDispatcher(testScheduler),
        )

        // the control client will do a status query as part of its init routine
        snapcastControlClient.initialize()

        var notification = snapcastControlClient.notifications.first()
        println("notification: $notification")
        // Asserting the first notification we receive is from the control client's init routine
        assert(notification is ServerGetStatusResponse)

        // Second notification we receive is from the client connection, get the id
        notification = snapcastControlClient.notifications.first()
        println("notification: $notification")
        assert(notification is ServerOnUpdate)
        val client = (notification as ServerOnUpdate)
            .params.server.groups.first().clients.first()
        val clientId = client.id
        val currLatency = client.config.latency

        snapcastControlClient.sendSetLatency(clientId, currLatency + 25)
        notification = snapcastControlClient.notifications.first()
        println("notification: $notification")
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

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun snapcastControlClientReconnect() {
        runBlocking {
            // Setup: snapclient, control client, and notifications flow will all retry in a loop
            // until the specified server ("localhost" in this case) becomes available
            val clientJob = launch(Dispatchers.IO) {
                SnapclientProcess(appContext, radioRepository).start()
            }
            val snapcastControlClient = SnapcastControlClient(
                "127.0.0.1",
                ioDispatcher = Dispatchers.IO,
            )
            launch {
                snapcastControlClient.notifications.collect { println("collected: $it") }
            }
            launch {
                snapcastControlClient.initialize() // loops continuously until it connects
                println("finished init")
            }

            // confirm that snapcastControlClient.initialize() unblocks
            delay(5000)
            val serverJob = launch(Dispatchers.IO) {
                SnapserverProcess(radioRepository).start()
            }

            // cancelling the server will make the notification flow reconnect mechanism to trigger
            delay(2000)
            serverJob.cancel()
            delay(2000)
            clientJob.cancel()

            // bringing the server back online will make the notifications flow to start emmitting again
            delay(7000)
            launch(Dispatchers.IO) {
                SnapserverProcess(radioRepository).start()
            }
        }
    }
}

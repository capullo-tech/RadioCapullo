package tech.capullo.radio.snapcast

import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.sendSerialized
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.http.HttpMethod
import io.ktor.serialization.kotlinx.KotlinxWebsocketSerializationConverter
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.util.Collections
import java.util.concurrent.TimeUnit

/**
 * Snapcast Control Client for managing WebSocket connections to Snapserver.
 *
 * Initialization Flow:
 * 1. initialize() is called
 * 2. WebSocket connection attempt is made
 * 3. If successful:
 *    - Session object is initialized
 *    - get status request is sent
 *    - Connection is ready for use
 * 4. If failed:
 *    - Log error and retry after 1 second delay
 *    - Loop continues until connection succeeds
 *
 * =================================================================================================
 *
 * Notifications Flow:
 *
 * The notifications flow runs continuously to receive server responses:
 *
 * 1. Flow starts with session object null (not initialized)
 * 2. Since session is null, delays for 1 second and tries again
 * 3. Once connection is established via initialize():
 *    - Session object is created
 *    - Flow blocks waiting for incoming frames
 * 4. Each frame is decoded to SnapcastJSONRPCResponse and emitted
 * 5. If server shuts down (EOFException):
 *    - Calls initialize() to reconnect
 *    - initialize() sends get status request
 *    - This makes another frame available for the next flow iteration
 *
 * This provides automatic reconnection and continuous message handling.
 *
 *
 * =================================================================================================
 *
 * Caller can use this code as follow:
 *
 *  val snapcastControlClient = SnapcastControlClient("127.0.0.1")
 *
 *  init {
 *      viewModelScope.launch {
 *          snapcastControlClient.initialize() // <--- will suspend until connected
 *
 *          // at least one notification frame is guaranteed since initialize calls getStatusRequest
 *
 *          snapcastControlClient.notifications.collect { notification ->
 *              ...
 *          } // <--- in case the server shuts down, this flow will automatically reconnect
 *      }
 *  }
 */
class SnapcastControlClient(
    private val snapserverHostAddress: String,
    private val websocketPort: Int = 1780,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    val client = HttpClient(OkHttp) {
        engine {
            config {
                pingInterval(20, TimeUnit.SECONDS)
            }
        }

        install(WebSockets) {
            contentConverter = KotlinxWebsocketSerializationConverter(json)
        }
    }

    private var session: DefaultClientWebSocketSession? = null

    private var requestIdCounter: Int = 1
    private val pendingRequests = Collections.synchronizedMap(mutableMapOf<Int, String>())

    enum class ConnectionState { STARTING, CONNECTED, ERROR }

    private val _connectionState = MutableStateFlow(ConnectionState.STARTING)
    val connectionState = _connectionState.asStateFlow()

    suspend fun initialize() = withContext(ioDispatcher) {
        while (true) {
            Log.d(TAG, "Attempting to create a websocket session with: $snapserverHostAddress")

            try {
                session = client.webSocketSession(
                    method = HttpMethod.Get,
                    host = snapserverHostAddress,
                    port = websocketPort,
                    path = "/jsonrpc",
                )

                Log.d(TAG, "Connection to websocket successful: $session")
                _connectionState.update { ConnectionState.CONNECTED }

                // so there is a new incoming frame - makes the `notification` flow unblock
                sendGetStatus()

                // do not keep retrying
                break
            } catch (e: Exception) {
                Log.d(TAG, "Failed to create websocket session: $e")
                _connectionState.update { ConnectionState.ERROR }

                // retrying...
                delay(1000)
            }
        }
    }

    val notifications: Flow<SnapcastJSONRPCResponse?> = flow {
        while (true) {
            val frame = withContext(ioDispatcher) {
                try {
                    return@withContext session?.incoming?.receive() as? Frame.Text
                } catch (e: Exception) { // possible EOFException if the server shuts down
                    Log.d(TAG, "Error reading incoming frame: $e")
                    delay(1000)

                    if (e is java.io.EOFException) {
                        Log.d(TAG, "Server shutdown")
                        _connectionState.update { ConnectionState.ERROR }

                        initialize() // loops trying to reconnect

                        // after finishing this suspend initialize(), inside there is a
                        // `sendGetStatus` call, this makes next while loop inside this flow have
                        // some new frames giving the appearance that we auto-reconnected
                    }
                }
                return@withContext null
            }

            frame?.readText()?.also { jsonString ->
                Log.d(TAG, "[RAW] Notification from server: $jsonString")

                val response = try {
                    json.decodeFromString(SnapcastJSONRPCResponseSerializer, jsonString)
                } catch (e: Exception) {
                    Log.d(TAG, "Error decoding response: $e")
                    null
                }

                if (response is RequestResponse) {
                    val requestMethod = pendingRequests.remove(response.id)

                    when (response) {
                        is JsonRpcErrorResponse -> {
                            Log.e(
                                TAG,
                                "JSON-RPC error for ${requestMethod ?: "unknown request"}: ${response.error}",
                            )
                        }

                        is GenericResultResponse -> {
                            Log.d(
                                TAG,
                                "Ack for ${requestMethod ?: "unknown request"}: ${response.result}",
                            )
                        }

                        else -> Unit
                    }
                }
                emit(response)
            } ?: run {
                // frame == null -> session == null (session not initialized)
                delay(1000)
                Log.d(TAG, "Incoming frame == null (Possibly because session is not initialized)")
            }
        }
    }

    suspend fun sendGetStatus() {
        val requestId = nextRequestId("Server.GetStatus")
        val getStatusRequest = ServerGetStatusRequest(id = requestId)

        Log.d(TAG, "sendGetStatus: $getStatusRequest")
        session?.sendSerialized(getStatusRequest)
    }

    suspend fun sendSetVolume(clientId: String, muted: Boolean, percent: Int) {
        val volume = Volume(muted, percent)
        val requestId = nextRequestId("Client.SetVolume")
        val setVolume = ClientSetVolumeRequest(
            id = requestId,
            params = VolumeParams(clientId, volume),
        )

        Log.d(TAG, "sendSetVolume: $setVolume")
        session?.sendSerialized(setVolume)
    }

    suspend fun sendSetLatency(clientId: String, latency: Int) {
        val requestId = nextRequestId("Client.SetLatency")
        val setLatency = ClientSetLatencyRequest(
            id = requestId,
            params = LatencyParams(clientId, latency),
        )

        Log.d(TAG, "sendSetLatency: $setLatency")
        session?.sendSerialized(setLatency)
        sendGetStatus()
    }

    suspend fun sendStreamControl(streamId: String, command: String) {
        val requestId = nextRequestId("Stream.Control")
        val streamControl = StreamControlRequest(
            id = requestId,
            params = StreamControlParams(id = streamId, command = command),
        )

        Log.d(TAG, "sendStreamControl: $streamControl")
        session?.sendSerialized(streamControl)
    }

    private fun nextRequestId(method: String): Int {
        val requestId = requestIdCounter++
        pendingRequests[requestId] = method
        return requestId
    }

    companion object {
        private val TAG = SnapcastControlClient::class.simpleName
    }
}

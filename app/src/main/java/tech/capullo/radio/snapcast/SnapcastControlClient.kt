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
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.util.concurrent.TimeUnit

class SnapcastControlClient(
    private val snapserverHostAddress: String,
    private val tag: Int = 1,
    private val websocketPort: Int = 1780,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    val client = HttpClient(OkHttp) {
        engine {
            config {
                pingInterval(20, TimeUnit.SECONDS)
            }
        }

        install(WebSockets) {
            contentConverter = KotlinxWebsocketSerializationConverter(Json)
        }
    }

    private var session: DefaultClientWebSocketSession? = null

    private var requestIdCounter: Int = 1

    suspend fun initialize() = withContext(ioDispatcher) {
        session = client.webSocketSession(
            method = HttpMethod.Get,
            host = snapserverHostAddress,
            port = websocketPort,
            path = "/jsonrpc",
        )
    }

    val notifications: Flow<SnapcastJSONRPCResponse?> = flow {
        while (true) {
            val frame = withContext(ioDispatcher) {
                session?.incoming?.receive() as? Frame.Text
            }
            frame?.readText()?.also { jsonString ->
                val response = try {
                    Json.decodeFromString(SnapcastJSONRPCResponseSerializer, jsonString)
                } catch (e: Exception) {
                    Log.d(TAG, "Error decoding response: $e")
                    null
                }
                emit(response)
            }
        }
    }

    suspend fun sendGetStatus() {
        val getStatusRequest = ServerGetStatusRequest(id = requestIdCounter++)
        session?.sendSerialized(getStatusRequest)
    }

    suspend fun sendSetVolume(clientId: String, muted: Boolean, percent: Int) {
        val volume = Volume(
            muted = muted,
            percent = percent,
        )
        val setVolume = ClientSetVolumeRequest(
            id = requestIdCounter++,
            params = VolumeParams(
                clientId = clientId,
                volume = volume,
            ),
        )

        session?.sendSerialized(setVolume)
    }

    companion object {
        private val TAG = SnapcastControlClient::class.simpleName
    }
}

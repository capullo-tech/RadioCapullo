package tech.capullo.radio.data

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.sendSerialized
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.http.HttpMethod
import io.ktor.serialization.kotlinx.KotlinxWebsocketSerializationConverter
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.concurrent.TimeUnit

class SnapcastControlClient(
    private val snapserverHostAddress: String,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + Job()),
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

    private val _serverStatus = MutableStateFlow<SnapcastServerStatus?>(null)
    val serverStatus = _serverStatus

    suspend fun initWebsocket() = coroutineScope {
        val session = client.webSocketSession(
            method = HttpMethod.Get,
            host = snapserverHostAddress,
            port = DEFAULT_WS_PORT,
            path = "/jsonrpc",
        )

        launch {
            while (true) {
                val frame = session.incoming.receive() as? Frame.Text
                println("Received frame: ${frame?.readText()}")

                val jsonString = frame?.readText()
                jsonString?.let {
                    val response = Json.decodeFromString<SnapcastServerStatus>(jsonString)
                    println(response)
                    _serverStatus.value = response
                }
            }
        }

        println("sending frame")
        // session.send(Frame.Text("{\"id\":1,\"jsonrpc\":\"2.0\",\"method\":\"Server.GetStatus\"}"))
        val getStatus = SnapcastGetStatusRequest(1, "2.0", "Server.GetStatus")
        session.sendSerialized(getStatus)
    }

    @Serializable
    data class SnapcastGetStatusRequest(val id: Int, val jsonrpc: String, val method: String)

    companion object {
        private const val TAG = "SnapcastControlClient"
        private const val DEFAULT_PORT = 1705
        private const val DEFAULT_WS_PORT = 1780
    }
}

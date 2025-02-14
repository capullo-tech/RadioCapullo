package tech.capullo.radio.data

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.http.HttpMethod
import io.ktor.network.selector.SelectorManager
import io.ktor.network.sockets.aSocket
import io.ktor.network.sockets.openReadChannel
import io.ktor.network.sockets.openWriteChannel
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.readUTF8Line
import io.ktor.utils.io.writeStringUtf8
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class SnapcastControlClient(
    private val snapserverHostAddress: String,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + Job()),
) {
    private var sendChannel: ByteWriteChannel? = null

    suspend fun initSocket() = coroutineScope {
        delay(10000)
        println("in SnapcastControlClient.rr()")
        val selectorManager = SelectorManager(ioDispatcher)
        println("selectorManager: $selectorManager")
        val socket = aSocket(selectorManager).tcp().connect(snapserverHostAddress, DEFAULT_PORT)
        println("socket: $socket")

        val receiveChannel = socket.openReadChannel()
        sendChannel = socket.openWriteChannel(autoFlush = true)

        launch(ioDispatcher) {
            while (true) {
                println("waiting for snapserver socket greeting")
                val greeting = receiveChannel.readUTF8Line()
                if (greeting != null) {
                    println("from snapserver socket: $greeting")
                } else {
                    println("Server closed a connection")
                    socket.close()
                    selectorManager.close()
                }
            }
        }
    }

    val client = HttpClient(OkHttp) {
        engine {
            config {
                pingInterval(20, TimeUnit.SECONDS)
            }
        }

        install(WebSockets)
    }

    suspend fun initWebsocket() = coroutineScope {
        val session =
        client.webSocketSession(
            method = HttpMethod.Get,
            host = snapserverHostAddress,
            port = DEFAULT_WS_PORT,
            path = "/jsonrpc",
        )

        launch {
            while (true) {
                val frame = session.incoming.receive() as? Frame.Text
                println("Received frame: ${frame?.readText()}")
            }
        }

        session.send(Frame.Text("{\"id\":1,\"jsonrpc\":\"2.0\",\"method\":\"Server.GetStatus\"}"))
        /*
        {
            println("Connected to snapserver websocket")
            launch { sendCommand("Server.GetStatus") }
            while (true) {
                val frame = incoming.receive() as? Frame.Text
                println("Received frame: ${frame?.readText()}")
            }
        }
         */
    }


    suspend fun sendCommand(command: String) = withContext(ioDispatcher) {
        delay(20000)
        val commandString = "{\"id\":1,\"jsonrpc\":\"2.0\",\"method\":\"$command\"}\n"

        println("aboud to send command: $commandString")
        sendChannel?.writeStringUtf8(commandString)
        println("command sent: $commandString")
    }

    companion object {
        private const val TAG = "SnapcastControlClient"
        private const val DEFAULT_PORT = 1705
        private const val DEFAULT_WS_PORT = 1780
    }
}

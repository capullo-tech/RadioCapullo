package tech.capullo.radio.espoti

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import tech.capullo.radio.espoti.EspotiConnectHandlerImpl.SessionParams
import java.io.IOException
import java.net.ServerSocket
import java.net.Socket

class EspotiZeroconfServer(
    val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    val espotiConnectHandler: EspotiConnectHandler = EspotiConnectHandlerImpl(),
) {

    interface EspotiConnectHandler {
        @Throws(Exception::class)
        suspend fun onConnect(socket: Socket): SessionParams?
    }

    private lateinit var serverSocket: ServerSocket

    @Throws(IOException::class)
    fun initAndGetPort(): Int = ServerSocket(0).also { serverSocket = it }.localPort

    private suspend fun serverSocketAccept() = withContext(dispatcher) { serverSocket.accept() }

    suspend fun listen(): SessionParams? = coroutineScope {
        println("Server listening at ${serverSocket.inetAddress}")
        while (true) {
            println("[${Thread.currentThread().name}] Awaiting connection")
            val socket = serverSocketAccept()
            println("[${Thread.currentThread().name}] Serving ${socket.port}")
            try {
                espotiConnectHandler.onConnect(socket)?.let { sessionParams ->
                    println("Emitting Session params: $sessionParams")
                    socket.close()
                    return@coroutineScope sessionParams
                }
            } catch (e: Exception) {
                println("Error serving ${socket.port}")
                e.printStackTrace()
            } finally {
                socket.close()
            }
        }
        return@coroutineScope null
    }
}

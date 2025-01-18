package tech.capullo.radio.espoti

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.ServerSocket
import java.net.Socket

interface EspotiConnectHandler {
    @Throws(Exception::class)
    suspend fun onConnect(socket: Socket)
}

class EspotiZeroconf(
    val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    val espotiConnectHandler: EspotiConnectHandler = EspotiConnectHandlerImpl(),
) {
    private lateinit var serverSocket: ServerSocket

    @Throws(IOException::class)
    fun initAndGetPort(): Int = ServerSocket(0).also { serverSocket = it }.localPort

    private suspend fun serverSocketAccept() = withContext(dispatcher) { serverSocket.accept() }

    suspend fun listen() = coroutineScope {
        println("Server listening at ${serverSocket.inetAddress}")
        while (true) {
            println("[${Thread.currentThread().name}] Awaiting connection")
            val socket = serverSocketAccept()
            launch(dispatcher) {
                println("[${Thread.currentThread().name}] Serving ${socket.port}")
                espotiConnectHandler.onConnect(socket)
            }
        }
    }
}

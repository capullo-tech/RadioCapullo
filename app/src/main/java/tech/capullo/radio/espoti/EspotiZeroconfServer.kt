package tech.capullo.radio.espoti

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.ServerSocket
import javax.inject.Inject

class EspotiZeroconfServer @Inject constructor(val espotiConnectHandler: EspotiConnectHandler) {

    val dispatcher: CoroutineDispatcher = Dispatchers.IO
    private lateinit var serverSocket: ServerSocket

    @Throws(IOException::class)
    fun initAndGetPort(): Int = ServerSocket(0).also { serverSocket = it }.localPort

    private suspend fun serverSocketAccept() = withContext(dispatcher) { serverSocket.accept() }

    suspend fun listen() = coroutineScope {
        while (true) {
            val socket = serverSocketAccept()
            try {
                val sessionConnected = espotiConnectHandler.onConnect(socket)
                if (sessionConnected) {
                    socket.close()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                socket.close()
            }
        }
    }
}

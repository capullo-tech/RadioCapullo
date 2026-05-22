package tech.capullo.radio.snapcast

import android.net.LocalServerSocket
import android.net.LocalSocket
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

class SnapcastControlBridge(
    private val socketName: String = "snapcontrol",
    private val player: PlayerCallbacks,
) {
    interface PlayerCallbacks {
        fun onPlay()
        fun onPause()
        fun onPlayPause()
        fun onNext()
        fun onPrevious()
        fun onSeek(offsetUs: Long)
        fun onSetPosition(trackId: String, posUs: Long)
        fun onSetProperty(name: String, value: JsonElement)
        fun currentProperties(): StreamProperties
    }

    companion object {
        private const val TAG = "SnapcastControlBridge"
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var serverSocket: LocalServerSocket? = null
    private var activeSocket: LocalSocket? = null
    private val writeMutex = Mutex()
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun start() {
        Log.i(TAG, "Starting SnapcastControlBridge on abstract socket: $socketName")
        try {
            serverSocket = LocalServerSocket(socketName)
        } catch (e: IOException) {
            Log.e(TAG, "Failed to bind LocalServerSocket", e)
            return
        }

        scope.launch {
            while (isActive) {
                try {
                    val socket = serverSocket?.accept() ?: break
                    Log.i(TAG, "New connection accepted from libsnapcontrol")
                    handleClient(socket)
                } catch (e: IOException) {
                    if (!isActive) break
                    Log.e(TAG, "Error accepting socket connection", e)
                }
            }
        }
    }

    fun stop() {
        Log.i(TAG, "Stopping SnapcastControlBridge")
        scope.cancel()
        closeActiveSocket()
        try {
            serverSocket?.close()
        } catch (e: IOException) {
            Log.e(TAG, "Error closing server socket", e)
        }
        serverSocket = null
    }

    private fun closeActiveSocket() {
        activeSocket?.let { socket ->
            try {
                socket.close()
            } catch (e: IOException) {
                Log.e(TAG, "Error closing client socket", e)
            }
        }
        activeSocket = null
    }

    private fun handleClient(socket: LocalSocket) {
        closeActiveSocket()
        activeSocket = socket

        scope.launch {
            // Once connected, perform the required handshake: send Plugin.Stream.Ready
            sendNotification("Plugin.Stream.Ready")

            try {
                val reader = BufferedReader(InputStreamReader(socket.inputStream))
                var line: String? = null
                while (isActive && reader.readLine().also { line = it } != null) {
                    val currentLine = line ?: break
                    if (currentLine.isNotBlank()) {
                        try {
                            handleRequest(currentLine)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error handling request: $currentLine", e)
                        }
                    }
                }
            } catch (e: IOException) {
                Log.e(TAG, "Connection lost or error reading from socket", e)
            } finally {
                if (activeSocket == socket) {
                    closeActiveSocket()
                    Log.i(TAG, "Client socket closed")
                }
            }
        }
    }

    private fun handleRequest(line: String) {
        val root = json.parseToJsonElement(line).jsonObject
        val id = root["id"]
        val method = root["method"]?.jsonPrimitive?.content

        Log.d(TAG, "Received message: $line")

        when (method) {
            "Plugin.Stream.Player.GetProperties" -> {
                val props = player.currentProperties()
                val result = json.encodeToJsonElement(StreamProperties.serializer(), props)
                respond(id, result)
            }

            "Plugin.Stream.Player.SetProperty" -> {
                val params = root["params"]?.jsonObject ?: return
                for ((key, value) in params) {
                    player.onSetProperty(key, value)
                }
                respond(id, JsonPrimitive("ok"))
            }

            "Plugin.Stream.Player.Control" -> {
                val params = root["params"]?.jsonObject ?: return
                val command = params["command"]?.jsonPrimitive?.content
                val cmdParams = params["params"]?.jsonObject
                when (command) {
                    "play" -> player.onPlay()

                    "pause" -> player.onPause()

                    "playPause" -> player.onPlayPause()

                    "next" -> player.onNext()

                    "previous" -> player.onPrevious()

                    "seek" -> {
                        val offsetSeconds = cmdParams?.get("offset")?.jsonPrimitive?.double ?: 0.0
                        player.onSeek((offsetSeconds * 1_000_000).toLong())
                    }

                    "setPosition" -> {
                        val posSeconds = cmdParams?.get("position")?.jsonPrimitive?.double ?: 0.0
                        val trackId = cmdParams?.get("trackId")?.jsonPrimitive?.content ?: ""
                        player.onSetPosition(trackId, (posSeconds * 1_000_000).toLong())
                    }

                    else -> {
                        Log.w(TAG, "Unknown control command: $command")
                    }
                }
                respond(id, JsonPrimitive("ok"))
            }

            else -> {
                if (id != null) {
                    respond(id, JsonNull)
                }
            }
        }
    }

    private fun respond(id: JsonElement?, result: JsonElement) {
        if (id == null) return
        val responseObj = buildJsonObject {
            put("jsonrpc", JsonPrimitive("2.0"))
            put("id", id)
            put("result", result)
        }
        sendRaw(responseObj)
    }

    private fun sendNotification(method: String, params: JsonElement? = null) {
        val notificationObj = buildJsonObject {
            put("jsonrpc", JsonPrimitive("2.0"))
            put("method", JsonPrimitive(method))
            if (params != null) {
                put("params", params)
            }
        }
        sendRaw(notificationObj)
    }

    fun pushProperties(properties: StreamProperties) {
        val params = json.encodeToJsonElement(StreamProperties.serializer(), properties)
        sendNotification("Plugin.Stream.Player.Properties", params)
    }

    private fun sendRaw(obj: JsonObject) {
        val socket = activeSocket ?: return
        scope.launch {
            writeMutex.withLock {
                try {
                    val rawData = (obj.toString() + "\n").toByteArray(Charsets.UTF_8)
                    socket.outputStream.write(rawData)
                    socket.outputStream.flush()
                } catch (e: IOException) {
                    Log.e(TAG, "Error writing to socket", e)
                }
            }
        }
    }
}

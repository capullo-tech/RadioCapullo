package tech.capullo.radio.data.sp

import android.util.Log
import com.google.gson.JsonObject
import com.spotify.connectstate.Connect
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import okhttp3.HttpUrl.Companion.toHttpUrl
import xyz.gianlu.librespot.common.NameThreadFactory
import xyz.gianlu.librespot.common.Utils
import xyz.gianlu.librespot.core.Session
import xyz.gianlu.librespot.core.Session.SpotifyAuthenticationException
import xyz.gianlu.librespot.crypto.DiffieHellman
import xyz.gianlu.librespot.mercury.MercuryClient.MercuryException
import java.io.Closeable
import java.io.DataInputStream
import java.io.IOException
import java.io.OutputStream
import java.lang.IllegalStateException
import java.net.ServerSocket
import java.net.Socket
import java.net.URLDecoder
import java.security.GeneralSecurityException
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.ArrayList
import java.util.HashMap
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.function.Function
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.concurrent.Volatile

class SpZeroconfServer(
    val deviceType: Connect.DeviceType,
    val deviceName: String,
    val deviceId: String = Utils.randomHexString(SecureRandom(), 40).lowercase(Locale.getDefault()),
    val preferredLocale: String = Locale.getDefault().language,
    val conf: Session.Configuration,
) : Closeable {
    private var runner: HttpRunner
    private val keys: DiffieHellman = DiffieHellman(SecureRandom())
    private val sessionListeners: MutableList<SessionListener> = ArrayList()
    private val connectionLock = Any()

    @Volatile
    private var session: Session? = null
    private var connectingUsername: String? = null

    var listenPort: Int = SecureRandom().nextInt((MAX_PORT - MIN_PORT) + 1) + MIN_PORT

    init {
        this.runner = HttpRunner(listenPort).apply { startListening() }
    }

    @Throws(IOException::class)
    override fun close() {
        runner.close()
    }

    @Throws(IOException::class)
    fun closeSession() {
        if (session == null) return

        for (l in sessionListeners) {
            l.sessionClosing(session!!)
        }
        session!!.close()
        session = null
    }

    private fun hasValidSession(): Boolean {
        try {
            val valid = session != null && session!!.isValid()
            if (!valid) session = null
            return valid
        } catch (_: IllegalStateException) {
            session = null
            return false
        }
    }

    @Throws(IOException::class)
    private fun handleGetInfo(out: OutputStream, httpVersion: String) {
        val info: JsonObject = DEFAULT_GET_INFO_FIELDS.deepCopy()
        info.addProperty("deviceID", deviceId)
        info.addProperty("remoteName", deviceName)
        info.addProperty("publicKey", Utils.toBase64(keys.publicKeyArray()))
        info.addProperty("deviceType", deviceType.name.uppercase(Locale.getDefault()))

        synchronized(connectionLock) {
            info.addProperty(
                "activeUser",
                if (connectingUsername != null) {
                    connectingUsername
                } else {
                    (if (hasValidSession()) session!!.username() else "")
                },
            )
        }

        out.write(httpVersion.toByteArray())
        out.write(" 200 OK".toByteArray())
        out.write(EOL)
        out.flush()

        out.write("Content-Type: application/json".toByteArray())
        out.write(EOL)
        out.flush()

        out.write(EOL)
        out.write(info.toString().toByteArray())
        out.flush()
    }

    @Throws(GeneralSecurityException::class, IOException::class)
    private fun handleAddUser(
        out: OutputStream,
        params: MutableMap<String?, String?>,
        httpVersion: String,
    ) {
        val username = params["userName"]
        if (username == null || username.isEmpty()) {
            Log.d(TAG, "Missing userName!")
            return
        }

        val blobStr = params["blob"]
        if (blobStr == null || blobStr.isEmpty()) {
            Log.d(TAG, "Missing blob!")
            return
        }

        val clientKeyStr = params["clientKey"]
        if (clientKeyStr == null || clientKeyStr.isEmpty()) {
            Log.d(TAG, "Missing clientKey!")
            return
        }

        synchronized(connectionLock) {
            if (username == connectingUsername) {
                Log.d(TAG, "$username is already trying to connect.")

                out.write(httpVersion.toByteArray())
                out.write(" 403 Forbidden".toByteArray()) // I don't think this is the Spotify way
                out.write(EOL)
                out.write(EOL)
                out.flush()
                return
            }
        }

        val sharedKey = Utils.toByteArray(keys.computeSharedKey(Utils.fromBase64(clientKeyStr)))
        val blobBytes = Utils.fromBase64(blobStr)
        val iv = blobBytes.copyOfRange(0, 16)
        val encrypted = blobBytes.copyOfRange(16, blobBytes.size - 20)
        val checksum = blobBytes.copyOfRange(blobBytes.size - 20, blobBytes.size)

        val sha1 = MessageDigest.getInstance("SHA-1")
        sha1.update(sharedKey)
        val baseKey = sha1.digest().copyOfRange(0, 16)

        val hmac = Mac.getInstance("HmacSHA1")
        hmac.init(SecretKeySpec(baseKey, "HmacSHA1"))
        hmac.update("checksum".toByteArray())
        val checksumKey = hmac.doFinal()

        hmac.init(SecretKeySpec(baseKey, "HmacSHA1"))
        hmac.update("encryption".toByteArray())
        val encryptionKey = hmac.doFinal()

        hmac.init(SecretKeySpec(checksumKey, "HmacSHA1"))
        hmac.update(encrypted)
        val mac = hmac.doFinal()

        if (!mac.contentEquals(checksum)) {
            Log.d(TAG, "Mac and checksum don't match!")

            out.write(httpVersion.toByteArray())
            out.write(" 400 Bad Request".toByteArray()) // I don't think this is the Spotify way
            out.write(EOL)
            out.write(EOL)
            out.flush()
            return
        }

        val aes = Cipher.getInstance("AES/CTR/NoPadding")
        aes.init(
            Cipher.DECRYPT_MODE,
            SecretKeySpec(encryptionKey.copyOfRange(0, 16), "AES"),
            IvParameterSpec(iv),
        )
        val decrypted = aes.doFinal(encrypted)

        try {
            closeSession()
        } catch (ex: IOException) {
            Log.d(TAG, "Failed closing previous session.", ex)
        }

        try {
            synchronized(connectionLock) {
                connectingUsername = username
            }

            Log.d(
                TAG,
                "Accepted new user from " +
                    params["deviceName"] + ". {deviceId: " +
                    deviceId + "}",
            )

            // Sending response
            val resp = DEFAULT_SUCCESSFUL_ADD_USER.toString()
            out.write(httpVersion.toByteArray())
            out.write(" 200 OK".toByteArray())
            out.write(EOL)
            out.write("Content-Length: ".toByteArray())
            out.write(resp.length.toString().toByteArray())
            out.write(EOL)
            out.flush()

            out.write(EOL)
            out.write(resp.toByteArray())
            out.flush()

            session = Session.Builder(conf)
                .setDeviceId(deviceId)
                .setDeviceName(deviceName)
                .setDeviceType(deviceType)
                .setPreferredLocale(preferredLocale)
                .blob(username, decrypted)
                .create()

            synchronized(connectionLock) {
                connectingUsername = null
            }

            for (l in sessionListeners) {
                l.sessionChanged(session!!)
            }
        } catch (ex: SpotifyAuthenticationException) {
            Log.d(TAG, "Couldn't establish a new session. $ex")

            synchronized(connectionLock) {
                connectingUsername = null
            }

            out.write(httpVersion.toByteArray())
            // I don't think this is the Spotify way
            out.write(" 500 Internal Server Error".toByteArray())
            out.write(EOL)
            out.write(EOL)
            out.flush()
        } catch (ex: MercuryException) {
            Log.d(TAG, "Couldn't establish a new session.$ex")

            synchronized(connectionLock) {
                connectingUsername = null
            }

            out.write(httpVersion.toByteArray())
            out.write(" 500 Internal Server Error".toByteArray())
            out.write(EOL)
            out.write(EOL)
            out.flush()
        } catch (ex: IOException) {
            Log.d(TAG, "Couldn't establish a new session.$ex")

            synchronized(connectionLock) {
                connectingUsername = null
            }

            out.write(httpVersion.toByteArray())
            out.write(" 500 Internal Server Error".toByteArray())
            out.write(EOL)
            out.write(EOL)
            out.flush()
        } catch (ex: GeneralSecurityException) {
            Log.d(TAG, "Couldn't establish a new session.$ex")

            synchronized(connectionLock) {
                connectingUsername = null
            }

            out.write(httpVersion.toByteArray())
            out.write(" 500 Internal Server Error".toByteArray())
            out.write(EOL)
            out.write(EOL)
            out.flush()
        }
    }

    fun addSessionListener(listener: SessionListener) {
        sessionListeners.add(listener)
    }

    interface SessionListener {
        /**
         * The session instance is going to be closed after this call.
         *
         * @param session The old [Session]
         */
        fun sessionClosing(session: Session)

        /**
         * The session instance changed. [.sessionClosing] has been already called.
         *
         * @param session The new [Session]
         */
        fun sessionChanged(session: Session)
    }

    private inner class HttpRunner(port: Int) : Closeable {
        private val serverSocket: ServerSocket = ServerSocket(port)
        private val scope = CoroutineScope(Dispatchers.IO + Job())
        private val executorService: ExecutorService =
            Executors.newCachedThreadPool(
                NameThreadFactory(Function { r: Runnable? -> "zeroconf-client-" + r.hashCode() }),
            )

        @Volatile
        private var shouldStop = false

        init {
            Log.d(TAG, "Zeroconf HTTP server started successfully on port $port!")
        }

        fun startListening() {
            scope.launch {
                while (!shouldStop) {
                    try {
                        val socket = serverSocket.accept()
                        executorService.execute(
                            Runnable {
                                try {
                                    Log.d(
                                        TAG,
                                        "Handling request!" +
                                            " on thread: ${Thread.currentThread().name}",
                                    )
                                    handle(socket)
                                    socket.close()
                                } catch (ex: IOException) {
                                    Log.d(TAG, "Failed handling request!: $ex")
                                }
                            },
                        )
                    } catch (ex: IOException) {
                        Log.d(TAG, "Failed handling connection!: $ex")
                    }
                }
            }
        }

        fun handleRequest(
            out: OutputStream,
            httpVersion: String,
            action: String,
            params: MutableMap<String?, String?>?,
        ) {
            if (action == "addUser") {
                requireNotNull(params)

                try {
                    Log.d(TAG, "Handling addUser! $params $httpVersion")
                    handleAddUser(out, params, httpVersion)
                } catch (ex: GeneralSecurityException) {
                    Log.d(TAG, "Failed handling addUser!", ex)
                } catch (ex: IOException) {
                    Log.d(TAG, "Failed handling addUser!", ex)
                }
            } else if (action == "getInfo") {
                try {
                    Log.d(TAG, "Handling getInfo! $httpVersion")
                    handleGetInfo(out, httpVersion)
                } catch (ex: IOException) {
                    Log.d(TAG, "Failed handling getInfo!", ex)
                }
            } else {
                Log.d(TAG, "Unknown action: $action")
            }
        }

        @Throws(IOException::class)
        fun handle(socket: Socket) {
            val `in` = DataInputStream(socket.getInputStream())
            val out = socket.getOutputStream()

            val requestLine = Utils.split(Utils.readLine(`in`), ' ')
            if (requestLine.size != 3) {
                Log.d(TAG, "Unexpected request line: " + requestLine.contentToString())
                return
            }

            val method: String? = requestLine[0]
            val path = requestLine[1]
            val httpVersion = requestLine[2]

            val headers: MutableMap<String?, String?> = HashMap<String?, String?>()
            var header: String?
            while (!(Utils.readLine(`in`).also { header = it }).isEmpty()) {
                val split = Utils.split(header!!, ':')
                headers.put(split[0], split[1].trim { it <= ' ' })
            }

            if (!hasValidSession()) {
                Log.d(
                    TAG,
                    "Handling request: $method $path $httpVersion, headers: $headers",
                )
            }

            var params: MutableMap<String?, String?>?
            if (method == "POST") {
                val contentType = headers["Content-Type"]
                if (contentType != "application/x-www-form-urlencoded") {
                    Log.d(TAG, "Bad Content-Type: $contentType")
                    return
                }

                val contentLengthStr = headers["Content-Length"]
                if (contentLengthStr == null) {
                    Log.d(TAG, "Missing Content-Length header!")
                    return
                }

                val contentLength = contentLengthStr.toInt()
                val body = ByteArray(contentLength)
                `in`.readFully(body)
                val bodyStr = String(body)

                val pairs = Utils.split(bodyStr, '&')
                params = HashMap<String?, String?>(pairs.size)
                for (pair in pairs) {
                    val split = Utils.split(pair, '=')
                    params.put(
                        URLDecoder.decode(split[0], "UTF-8"),
                        URLDecoder.decode(split[1], "UTF-8"),
                    )
                }
            } else {
                params = parsePath(path)
            }

            val action = params["action"]
            if (action == null) {
                Log.d(TAG, "Request is missing action.")
                return
            }

            handleRequest(out, httpVersion, action, params)
        }

        @Throws(IOException::class)
        override fun close() {
            shouldStop = true
            serverSocket.close()
            executorService.shutdown()
            scope.cancel()
        }
    }

    companion object {
        private const val MAX_PORT = 65536
        private const val MIN_PORT = 1024
        private val TAG = SpZeroconfServer::class.java.simpleName
        private val EOL = byteArrayOf('\r'.code.toByte(), '\n'.code.toByte())
        private val DEFAULT_GET_INFO_FIELDS = JsonObject()
        private val DEFAULT_SUCCESSFUL_ADD_USER = JsonObject()

        init {
            DEFAULT_GET_INFO_FIELDS.addProperty("status", 101)
            DEFAULT_GET_INFO_FIELDS.addProperty("statusString", "OK")
            DEFAULT_GET_INFO_FIELDS.addProperty("spotifyError", 0)
            DEFAULT_GET_INFO_FIELDS.addProperty("version", "2.7.1")
            DEFAULT_GET_INFO_FIELDS.addProperty("libraryVersion", "?.?.?")
            DEFAULT_GET_INFO_FIELDS.addProperty("accountReq", "PREMIUM")
            DEFAULT_GET_INFO_FIELDS.addProperty("brandDisplayName", "librespot-org")
            DEFAULT_GET_INFO_FIELDS.addProperty("modelDisplayName", "librespot-android")
            DEFAULT_GET_INFO_FIELDS.addProperty("voiceSupport", "NO")
            DEFAULT_GET_INFO_FIELDS.addProperty("availability", "")
            DEFAULT_GET_INFO_FIELDS.addProperty("productID", 0)
            DEFAULT_GET_INFO_FIELDS.addProperty("tokenType", "default")
            DEFAULT_GET_INFO_FIELDS.addProperty("groupStatus", "NONE")
            DEFAULT_GET_INFO_FIELDS.addProperty("resolverVersion", "0")
            DEFAULT_GET_INFO_FIELDS.addProperty("scope", "streaming,client-authorization-universal")

            DEFAULT_SUCCESSFUL_ADD_USER.addProperty("status", 101)
            DEFAULT_SUCCESSFUL_ADD_USER.addProperty("spotifyError", 0)
            DEFAULT_SUCCESSFUL_ADD_USER.addProperty("statusString", "OK")

            Utils.removeCryptographyRestrictions()
        }

        private fun parsePath(path: String): MutableMap<String?, String?> {
            val url = "http://host$path".toHttpUrl()
            val map: MutableMap<String?, String?> = HashMap<String?, String?>()
            for (name in url.queryParameterNames) map.put(name, url.queryParameter(name))
            return map
        }
    }
}

package tech.capullo.radio.espoti

import android.util.Log
import com.google.gson.JsonObject
import com.spotify.connectstate.Connect
import kotlinx.coroutines.coroutineScope
import okhttp3.HttpUrl.Companion.toHttpUrl
import xyz.gianlu.librespot.common.Utils
import xyz.gianlu.librespot.crypto.DiffieHellman
import java.io.DataInputStream
import java.io.IOException
import java.io.OutputStream
import java.net.Socket
import java.net.URLDecoder
import java.security.GeneralSecurityException
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.HashMap
import java.util.Locale
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject

class EspotiConnectHandler @Inject constructor(sessionManager: EspotiSessionManager) {

    val deviceType: Connect.DeviceType = sessionManager.espotiDeviceType
    val deviceName: String = sessionManager.espotiDeviceName
    val deviceId: String = sessionManager.espotiDeviceId
    val keys: DiffieHellman = DiffieHellman(SecureRandom())
    data class SessionParams(
        val username: String,
        val decrypted: ByteArray,
    )

    suspend fun onConnect(socket: Socket): SessionParams? = coroutineScope {
        val inputStream = DataInputStream(socket.getInputStream())
        val outputStream = socket.getOutputStream()

        println("about to read the line")
        val requestLine = Utils.split(Utils.readLine(inputStream), ' ')
        println("read the line: $requestLine")
        if (requestLine.size != 3) {
            return@coroutineScope null
        }

        val method: String = requestLine[0]
        val path = requestLine[1]
        val httpVersion = requestLine[2]

        val headers: MutableMap<String?, String?> = HashMap<String?, String?>()
        var header: String?
        println("about to read headers")
        while ((Utils.readLine(inputStream).also { header = it }).isNotEmpty()) {
            println("header: $header")
            val split = Utils.split(header!!, ':')
            headers[split[0]] = split[1].trim { it <= ' ' }
        }
        println("headers: $headers")
        val params: MutableMap<String?, String?>?
        if (method == "POST") {
            val contentType = headers["Content-Type"]
            if (contentType != "application/x-www-form-urlencoded") {
                return@coroutineScope null
            }

            val contentLengthStr = headers["Content-Length"] ?: return@coroutineScope null

            val contentLength = contentLengthStr.toInt()
            val body = ByteArray(contentLength)
            inputStream.readFully(body)
            val bodyStr = String(body)

            val pairs = Utils.split(bodyStr, '&')
            params = HashMap<String?, String?>(pairs.size)
            for (pair in pairs) {
                val split = Utils.split(pair, '=')
                params[URLDecoder.decode(split[0], "UTF-8")] = URLDecoder.decode(split[1], "UTF-8")
            }
        } else {
            params = parsePath(path)
        }

        val action = params["action"] ?: return@coroutineScope null

        return@coroutineScope handleRequest(outputStream, httpVersion, action, params)
    }

    private fun handleRequest(
        out: OutputStream,
        httpVersion: String,
        action: String,
        params: MutableMap<String?, String?>?,
    ): SessionParams? {
        if (action == "addUser") {
            requireNotNull(params)

            try {
                return handleAddUser(out, params, httpVersion)
            } catch (e: Exception) {
                println(e)
            }
        } else if (action == "getInfo") {
            try {
                handleGetInfo(out, httpVersion)
            } catch (e: Exception) {
                println(e)
            }
        }

        return null
    }

    @Throws(IOException::class)
    fun handleGetInfo(out: OutputStream, httpVersion: String) {
        val info: JsonObject = DEFAULT_GET_INFO_FIELDS.deepCopy()
        info.addProperty("deviceID", deviceId)
        info.addProperty("remoteName", deviceName)
        info.addProperty("publicKey", Utils.toBase64(keys.publicKeyArray()))
        info.addProperty("deviceType", deviceType.name.uppercase(Locale.getDefault()))

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
    fun handleAddUser(
        out: OutputStream,
        params: MutableMap<String?, String?>,
        httpVersion: String,
    ): SessionParams? {
        println("handleAddUser")
        val username = params["userName"]
        if (username.isNullOrEmpty()) {
            Log.d(TAG, "Missing userName!")
            return null
        }

        val blobStr = params["blob"]
        if (blobStr.isNullOrEmpty()) {
            Log.d(TAG, "Missing blob!")
            return null
        }

        val clientKeyStr = params["clientKey"]
        if (clientKeyStr.isNullOrEmpty()) {
            Log.d(TAG, "Missing clientKey!")
            return null
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

        println("mac: ${mac.contentToString()}")

        if (!mac.contentEquals(checksum)) {
            Log.d(TAG, "Mac and checksum don't match!")

            out.write(httpVersion.toByteArray())
            out.write(" 400 Bad Request".toByteArray()) // I don't think this is the Spotify way
            out.write(EOL)
            out.write(EOL)
            out.flush()
            return null
        }

        val aes = Cipher.getInstance("AES/CTR/NoPadding")
        aes.init(
            Cipher.DECRYPT_MODE,
            SecretKeySpec(encryptionKey.copyOfRange(0, 16), "AES"),
            IvParameterSpec(iv),
        )
        val decrypted = aes.doFinal(encrypted)
        println("decrypted: ${decrypted.contentToString()}")

        try {
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

            println("username: $username")

            return SessionParams(
                username = username,
                decrypted = decrypted,
            )
        } catch (_: Exception) {
            out.write(httpVersion.toByteArray())
            out.write(" 500 Internal Server Error".toByteArray())
            out.write(EOL)
            out.write(EOL)
            out.flush()

            return null
        }
    }

    private fun parsePath(path: String): MutableMap<String?, String?> {
        val url = "http://host$path".toHttpUrl()
        val map: MutableMap<String?, String?> = HashMap<String?, String?>()
        for (name in url.queryParameterNames) map[name] = url.queryParameter(name)
        return map
    }

    companion object {
        private val TAG = EspotiConnectHandler::class.java.simpleName
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
    }
}

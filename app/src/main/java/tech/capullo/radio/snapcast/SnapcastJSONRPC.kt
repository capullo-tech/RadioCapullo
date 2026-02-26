package tech.capullo.radio.snapcast

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

// Requests
// https://github.com/badaix/snapcast/blob/develop/doc/json_rpc_api/control.md#requests-1
@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class ServerGetStatusRequest(
    val id: Int,
    @EncodeDefault
    val jsonrpc: String = "2.0",
    @EncodeDefault
    val method: String = "Server.GetStatus",
)

@Serializable
data class ServerGetStatusResponse(
    override val id: Int,
    override val jsonrpc: String,
    val result: ServerStatusResult,
) : RequestResponse()

@Serializable
data class ServerStatusResult(val server: ServerInfo)

@Serializable
data class ServerInfo(val groups: List<Group>, val server: Server, val streams: List<Stream>)

@Serializable
data class Group(
    val clients: List<Client>,
    val id: String,
    val muted: Boolean,
    val name: String,
    @SerialName("stream_id")
    val streamId: String,
)

@Serializable
data class Client(
    val config: ClientConfig,
    val connected: Boolean,
    val host: Host,
    val id: String,
    val lastSeen: LastSeen,
    val snapclient: SnapClient,
)

@Serializable
data class ClientConfig(val instance: Int, val latency: Int, val name: String, val volume: Volume)

@Serializable
data class ClientParams(val client: Client, val id: String)

@Serializable
data class Volume(val muted: Boolean, val percent: Int)

@Serializable
data class Host(val arch: String, val ip: String, val mac: String, val name: String, val os: String)

@Serializable
data class LastSeen(val sec: Long, val usec: Long)

@Serializable
data class SnapClient(val name: String, val protocolVersion: Int, val version: String)

@Serializable
data class Server(val host: Host, val snapserver: SnapServer)

@Serializable
data class SnapServer(
    val controlProtocolVersion: Int,
    val name: String,
    val protocolVersion: Int,
    val version: String,
)

@Serializable
data class Stream(
    val id: String,
    val properties: StreamProperties,
    val status: String,
    val uri: StreamUri,
)

@Serializable
data class StreamProperties(
    val canControl: Boolean,
    val canGoNext: Boolean,
    val canGoPrevious: Boolean,
    val canPause: Boolean,
    val canPlay: Boolean,
    val canSeek: Boolean,
)

@Serializable
data class StreamUri(
    val fragment: String,
    val host: String,
    val path: String,
    val query: StreamQuery,
    val raw: String,
    val scheme: String,
)

@Serializable
data class StreamQuery(
    @SerialName("chunk_ms")
    val chunkMs: String,
    val codec: String,
    @SerialName("dryout_ms")
    val dryoutMs: String,
    val mode: String,
    val name: String,
    val sampleformat: String,
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class ClientSetVolumeRequest(
    val id: Int,
    @EncodeDefault
    val jsonrpc: String = "2.0",
    @EncodeDefault
    val method: String = "Client.SetVolume",
    val params: VolumeParams,
)

@Serializable
data class VolumeParams(
    @SerialName("id")
    val clientId: String,
    val volume: Volume,
)

// Notifications
// https://github.com/badaix/snapcast/blob/develop/doc/json_rpc_api/control.md#notifications
@Serializable
abstract class Notification : SnapcastJSONRPCResponse() {
    abstract override val jsonrpc: String
    abstract val method: String
}

@Serializable
data class GenericNotification(
    override val jsonrpc: String,
    override val method: String,
    val params: JsonObject,
) : Notification()

@Serializable
data class ClientOnVolumeChanged(
    override val jsonrpc: String,
    override val method: String,
    val params: VolumeParams,
) : Notification()

@Serializable
data class ClientOnDisconnect(
    override val jsonrpc: String,
    override val method: String,
    val params: ClientParams,
) : Notification()

@Serializable
data class ClientOnConnect(
    override val jsonrpc: String,
    override val method: String,
    val params: ClientParams,
) : Notification()

@Serializable
data class ServerOnUpdate(
    override val jsonrpc: String,
    override val method: String,
    val params: ServerStatusResult,
) : Notification()

object NotificationSerializer : JsonContentPolymorphicSerializer<Notification>(
    Notification::class,
) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<Notification> {
        val method = element.jsonObject["method"]
        return when (method.toString()) {
            "\"Server.OnUpdate\"" -> ServerOnUpdate.serializer()
            "\"Client.OnVolumeChanged\"" -> ClientOnVolumeChanged.serializer()
            "\"Client.OnDisconnect\"" -> ClientOnDisconnect.serializer()
            "\"Client.OnConnect\"" -> ClientOnConnect.serializer()
            else -> GenericNotification.serializer()
        }
    }
}

// Custom serializers for polymorphism
@Serializable
abstract class RequestResponse : SnapcastJSONRPCResponse() {
    abstract val id: Int
    abstract override val jsonrpc: String
}

object RequestResponseSerializer : JsonContentPolymorphicSerializer<RequestResponse>(
    RequestResponse::class,
) {
    override fun selectDeserializer(
        element: JsonElement,
    ): DeserializationStrategy<RequestResponse> {
        // TODO: add serializers for all kinds of responses
        return ServerGetStatusResponse.serializer()
    }
}

@Serializable
abstract class SnapcastJSONRPCResponse {
    abstract val jsonrpc: String
}

object SnapcastJSONRPCResponseSerializer :
    JsonContentPolymorphicSerializer<SnapcastJSONRPCResponse>(
        SnapcastJSONRPCResponse::class,
    ) {
    override fun selectDeserializer(
        element: JsonElement,
    ): DeserializationStrategy<SnapcastJSONRPCResponse> = if ("method" in element.jsonObject) {
        NotificationSerializer
    } else {
        RequestResponseSerializer
    }
}

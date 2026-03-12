package tech.capullo.radio

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.capullo.radio.snapcast.ClientOnConnect
import tech.capullo.radio.snapcast.ClientOnDisconnect
import tech.capullo.radio.snapcast.ClientOnLatencyChanged
import tech.capullo.radio.snapcast.ClientOnVolumeChanged
import tech.capullo.radio.snapcast.ClientSetLatencyRequest
import tech.capullo.radio.snapcast.GenericNotification
import tech.capullo.radio.snapcast.GenericResultResponse
import tech.capullo.radio.snapcast.LatencyParams
import tech.capullo.radio.snapcast.ServerGetStatusResponse
import tech.capullo.radio.snapcast.ServerOnUpdate
import tech.capullo.radio.snapcast.SnapcastJSONRPCResponseSerializer

class SnapcastControlClientTest {

    @Test
    fun clientSetLatencySerializeTest() {
        val request = ClientSetLatencyRequest(
            id = 7,
            params = LatencyParams(
                clientId = "client-1",
                latency = 125,
            ),
        )

        val serialized = Json.encodeToString(request)

        assertTrue(serialized.contains("\"method\":\"Client.SetLatency\""))
        assertTrue(serialized.contains("\"id\":\"client-1\""))
        assertTrue(serialized.contains("\"latency\":125"))
    }

    @Test
    fun serverGetStatusResponseDeserializeTest() {
        val serverGetStatusString = """
            {
              "id": 2,
              "jsonrpc": "2.0",
              "result": {
                "server":  {
                  "groups": [
                    {
                      "clients": [
                        {
                          "config": {
                            "instance": 1,
                            "latency": 0,
                            "name": "",
                            "volume": {
                              "muted": false,
                              "percent": 100
                            }
                          },
                          "connected": true,
                          "host": {
                            "arch": "x86_64",
                            "ip": "::ffff:127.0.0.1",
                            "mac": "00:00:00:00:00:00",
                            "name": "sdk_gphone64_x86_64",
                            "os": "Android 16"
                          },
                          "id": "031a7306-6a7d-4a52-ab7d-3c08051b1d84",
                          "lastSeen": {
                            "sec": 1762503830,
                            "usec": 240402
                          },
                          "snapclient": {
                            "name": "Snapclient",
                            "protocolVersion": 2,
                            "version": "0.34.0"
                          }
                        }
                      ],
                      "id": "5447d658-940b-a745-a122-b0c213b9b400",
                      "muted": false,
                      "name": "",
                      "stream_id": "RadioCapullo"
                    }
                  ],
                  "server": {
                    "host": {
                      "arch": "x86_64",
                      "ip": "",
                      "mac": "",
                      "name": "sdk_gphone64_x86_64",
                      "os": "Android 16"
                    },
                    "snapserver": {
                      "controlProtocolVersion": 1,
                      "name": "Snapserver",
                      "protocolVersion": 1,
                      "version": "0.34.0"
                    }
                  },
                  "streams": [
                    {
                      "id": "RadioCapullo",
                      "properties": {
                        "canControl": false,
                        "canGoNext": false,
                        "canGoPrevious": false,
                        "canPause": false,
                        "canPlay": false,
                        "canSeek": false
                      },
                      "status": "idle",
                      "uri": {
                        "fragment": "",
                        "host": "",
                        "path": "/data/user/0/tech.capullo.radio/cache/filifo",
                        "query": {
                          "chunk_ms": "20",
                          "codec": "flac",
                          "dryout_ms": "2000",
                          "mode": "read",
                          "name": "RadioCapullo",
                          "sampleformat": "44100:16:2"
                        },
                        "raw": "pipe:///data/user/0/tech.capullo.radio/cache/filifo?chunk_ms=20&codec=flac&dryout_ms=2000&mode=read&name=RadioCapullo&sampleformat=44100%3A16%3A2",
                        "scheme": "pipe"
                      }
                    }
                  ]
                }
              }
            }
        """.trimIndent()
        val response =
            Json.decodeFromString(SnapcastJSONRPCResponseSerializer, serverGetStatusString)

        assert(response is ServerGetStatusResponse)
    }

    @Test
    fun clientOnVolumeChangedDeserializeTest() {
        val clientId = "82c9349e-da57-401a-aefa-2eb68b358ef3"
        val muted = true
        val percent = 20
        val onVolumeChangedString = """
        {
          "jsonrpc": "2.0",
          "method": "Client.OnVolumeChanged",
          "params": {
            "id": "$clientId",
            "volume": {
              "muted": $muted,
              "percent": $percent
            }
          }
        }
    """

        val response =
            Json.decodeFromString(SnapcastJSONRPCResponseSerializer, onVolumeChangedString)

        assert(response is ClientOnVolumeChanged)
        (response as ClientOnVolumeChanged).let {
            assert(it.params.clientId == clientId)
            assert(it.params.volume.muted == muted)
            assert(it.params.volume.percent == percent)
        }
    }

    @Test
    fun clientOnLatencyChangedDeserializeTest() {
        val clientId = "dabb677e-18ca-429d-a710-1740e400a40a"
        val latency = 5
        val onLatencyChangedString = """
        {
          "jsonrpc": "2.0",
          "method": "Client.OnLatencyChanged",
          "params": {
            "id": "$clientId",
            "latency": $latency
          }
        }
    """

        val response =
            Json.decodeFromString(SnapcastJSONRPCResponseSerializer, onLatencyChangedString)

        assertTrue(response is ClientOnLatencyChanged)
        (response as ClientOnLatencyChanged).let {
            assertEquals(clientId, it.params.clientId)
            assertEquals(latency, it.params.latency)
        }
    }

    @Test
    fun serverOnUpdateDeserializeTest() {
        val onUpdateString = """
            {
              "jsonrpc": "2.0",
              "method": "Server.OnUpdate",
              "params": {
                "server": {
                  "groups": [
                    {
                      "clients": [
                        {
                          "config": {
                            "instance": 1,
                            "latency": 0,
                            "name": "",
                            "volume": {
                              "muted": false,
                              "percent": 100
                            }
                          },
                          "connected": true,
                          "host": {
                            "arch": "x86_64",
                            "ip": "::ffff:127.0.0.1",
                            "mac": "00:00:00:00:00:00",
                            "name": "sdk_gphone64_x86_64",
                            "os": "Android 16"
                          },
                          "id": "e694ba4d-3b37-4303-ab0c-c0aa41165385",
                          "lastSeen": {
                            "sec": 1762426399,
                            "usec": 509162
                          },
                          "snapclient": {
                            "name": "Snapclient",
                            "protocolVersion": 2,
                            "version": "0.34.0"
                          }
                        }
                      ],
                      "id": "3f80e383-6603-682c-ebc7-c8b1d4314640",
                      "muted": false,
                      "name": "",
                      "stream_id": "RadioCapullo"
                    }
                  ],
                  "server": {
                    "host": {
                      "arch": "x86_64",
                      "ip": "",
                      "mac": "",
                      "name": "sdk_gphone64_x86_64",
                      "os": "Android 16"
                    },
                    "snapserver": {
                      "controlProtocolVersion": 1,
                      "name": "Snapserver",
                      "protocolVersion": 1,
                      "version": "0.34.0"
                    }
                  },
                  "streams": [
                    {
                      "id": "RadioCapullo",
                      "properties": {
                        "canControl": false,
                        "canGoNext": false,
                        "canGoPrevious": false,
                        "canPause": false,
                        "canPlay": false,
                        "canSeek": false
                      },
                      "status": "idle",
                      "uri": {
                        "fragment": "",
                        "host": "",
                        "path": "/data/user/0/tech.capullo.radio/cache/filifo",
                        "query": {
                          "chunk_ms": "20",
                          "codec": "flac",
                          "dryout_ms": "2000",
                          "mode": "read",
                          "name": "RadioCapullo",
                          "sampleformat": "44100:16:2"
                        },
                        "raw": "pipe:///data/user/0/tech.capullo.radio/cache/filifo?chunk_ms=20&codec=flac&dryout_ms=2000&mode=read&name=RadioCapullo&sampleformat=44100%3A16%3A2",
                        "scheme": "pipe"
                      }
                    }
                  ]
                }
              }
            }
        """.trimIndent()

        val response =
            Json.decodeFromString(SnapcastJSONRPCResponseSerializer, onUpdateString)
        assert(response is ServerOnUpdate)
    }

    @Test
    fun serverClientOnDisconnectDeserializeTest() {
        val clientId = "c2b30a57-e3b3-4157-a30d-38b10513b4be"
        val hostName = "sdk_gphone64_x86_64"
        val onDisconnectString = """
            {
              "jsonrpc": "2.0",
              "method": "Client.OnDisconnect",
              "params": {
                "client": {
                  "config": {
                    "instance": 1,
                    "latency": 0,
                    "name": "",
                    "volume": {
                      "muted": false,
                      "percent": 100
                    }
                  },
                  "connected": false,
                  "host": {
                    "arch": "x86_64",
                    "ip": "::ffff:127.0.0.1",
                    "mac": "00:00:00:00:00:00",
                    "name": "$hostName",
                    "os": "Android 16"
                  },
                  "id": "$clientId",
                  "lastSeen": {
                    "sec": 1772428020,
                    "usec": 692807
                  },
                  "snapclient": {
                    "name": "Snapclient",
                    "protocolVersion": 2,
                    "version": "0.34.0"
                  }
                },
                "id": "$clientId"
              }
            }
        """.trimIndent()

        val response =
            Json.decodeFromString(SnapcastJSONRPCResponseSerializer, onDisconnectString)

        assert(response is ClientOnDisconnect)
        (response as ClientOnDisconnect).let {
            assert(it.params.client.id == clientId)
            assert(it.params.id == clientId)
            assert(it.params.client.host.name == hostName)
        }
    }

    @Test
    fun serverClientOnConnectDeserializeTest() {
        val clientId = "11b01211-ae95-41f6-876c-48c3ea4310da"
        val hostName = "sdk_gphone64_x86_64"
        val onConnectString = """
            {
              "jsonrpc": "2.0",
              "method": "Client.OnConnect",
              "params": {
                "client": {
                  "config": {
                    "instance": 1,
                    "latency": 0,
                    "name": "",
                    "volume": {
                      "muted": false,
                      "percent": 100
                    }
                  },
                  "connected": true,
                  "host": {
                    "arch": "x86_64",
                    "ip": "::ffff:127.0.0.1",
                    "mac": "00:00:00:00:00:00",
                    "name": "$hostName",
                    "os": "Android 16"
                  },
                  "id": "$clientId",
                  "lastSeen": {
                    "sec": 1772457507,
                    "usec": 730325
                  },
                  "snapclient": {
                    "name": "Snapclient",
                    "protocolVersion": 2,
                    "version": "0.34.0"
                  }
                },
                "id": "$clientId"
              }
            }
        """.trimIndent()

        val response =
            Json.decodeFromString(SnapcastJSONRPCResponseSerializer, onConnectString)

        assert(response is ClientOnConnect)
        (response as ClientOnConnect).let {
            assert(it.params.client.id == clientId)
            assert(it.params.id == clientId)
            assert(it.params.client.host.name == hostName)
        }
    }

    @Test
    fun genericResultResponseDeserializeTest() {
        val ackString = """
            {
              "id": 9,
              "jsonrpc": "2.0",
              "result": {}
            }
        """.trimIndent()

        val response = Json.decodeFromString(SnapcastJSONRPCResponseSerializer, ackString)

        assertTrue(response is GenericResultResponse)
        assertEquals(9, (response as GenericResultResponse).id)
    }

    @Test
    fun genericNotificationDeserializeTest() {
        val genericNotificationString = """
            {
              "jsonrpc": "2.0",
              "method": "Client.OnSomethingElse",
              "params": {
                "id": "client-1",
                "latency": 42
              }
            }
        """.trimIndent()

        val response =
            Json.decodeFromString(SnapcastJSONRPCResponseSerializer, genericNotificationString)

        assertTrue(response is GenericNotification)
    }
}

package tech.capullo.radio

import kotlinx.serialization.json.Json
import org.junit.Test
import tech.capullo.radio.snapcast.ClientOnVolumeChanged
import tech.capullo.radio.snapcast.ServerGetStatusResponse
import tech.capullo.radio.snapcast.ServerOnUpdate
import tech.capullo.radio.snapcast.SnapcastJSONRPCResponseSerializer

class SnapcastControlClientTest {

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
}

package tech.capullo.radio.ui

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import tech.capullo.radio.R
import tech.capullo.radio.snapcast.Client
import tech.capullo.radio.snapcast.ClientConfig
import tech.capullo.radio.snapcast.Group
import tech.capullo.radio.snapcast.Host
import tech.capullo.radio.snapcast.LastSeen
import tech.capullo.radio.snapcast.SnapClient
import tech.capullo.radio.snapcast.Volume
import tech.capullo.radio.ui.theme.RadioTheme
import tech.capullo.radio.ui.theme.SchemeChoice
import kotlin.math.round

@Composable
fun SnapserverGroups(
    modifier: Modifier = Modifier,
    groups: List<Group>,
    onClientVolumeChange: (String, Boolean, Int) -> Unit,
) {
    LazyColumn(modifier = modifier.padding(vertical = 4.dp)) {
        groups.forEach { group ->
            stickyHeader {
                Row(
                    modifier = Modifier.fillMaxWidth().background(
                        MaterialTheme.colorScheme.surfaceVariant,
                    ).padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Text(group.name.ifEmpty { group.streamId.ifEmpty { group.id } })
                }
            }

            items(
                items = group.clients.filter { it.connected },
                key = { client -> client.id },
            ) { client ->
                SnapcastClientCard(
                    name = client.host.name,
                    muted = client.config.volume.muted,
                    onMutedChange = { muted ->
                        // pass over new muted state, keep the same volume value
                        onClientVolumeChange(
                            client.id,
                            muted,
                            client.config.volume.percent,
                        )
                    },
                    volume = client.config.volume.percent.toFloat() / 100f,
                    onVolumeChange = { volume ->
                        // pass over new volume value, keep the same muted state
                        onClientVolumeChange(
                            client.id,
                            client.config.volume.muted,
                            volume,
                        )
                    },
                )
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun SnapcastClientCard(
    name: String,
    muted: Boolean,
    onMutedChange: (Boolean) -> Unit,
    volume: Float,
    onVolumeChange: (Int) -> Unit,
) {
    // Context: When sending a ClientSetVolumeRequest, we DO NOT process the request's response
    // therefore, we manage the slider state locally for UI interactions
    // Incoming ClientOnVolumeChanged notifications are being reflected with LaunchedEffect
    var mutedState by remember { mutableStateOf(muted) }
    var volumeState by remember { mutableFloatStateOf(volume) }

    LaunchedEffect(muted) {
        mutedState = muted
    }
    LaunchedEffect(volume) {
        volumeState = volume
    }

    Card(
        modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp),
    ) {
        Column {
            Text(
                modifier = Modifier.padding(start = 24.dp, top = 24.dp, bottom = 12.dp),
                text = name,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                ),
            )
            Row(
                modifier = Modifier.padding(start = 12.dp, end = 24.dp, bottom = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = {
                        mutedState = !mutedState
                        onMutedChange(mutedState)
                    },
                ) {
                    Icon(
                        painter = if (mutedState) {
                            painterResource(id = R.drawable.volume_off_24px)
                        } else {
                            painterResource(id = R.drawable.volume_up_24px)
                        },
                        contentDescription = if (mutedState) {
                            "Unmute"
                        } else {
                            "Mute"
                        },

                    )
                }
                Slider(
                    value = volumeState,
                    onValueChange = {
                        volumeState = it
                        onVolumeChange(round(volumeState * 100).toInt())
                    },
                    steps = 99,
                    valueRange = 0f..1f,
                    modifier = Modifier.weight(2f),
                    track = { sliderState ->
                        SliderDefaults.Track(
                            sliderState = sliderState,
                            colors = SliderDefaults.colors(
                                activeTickColor = Color.Transparent,
                                inactiveTickColor = Color.Transparent,
                            ),
                        )
                    },
                )
            }
        }
    }
}

@Preview(
    showBackground = true,
    widthDp = 320,
    uiMode = UI_MODE_NIGHT_YES,
    name = "DefaultPreviewDark",
)
@Preview(showBackground = true, widthDp = 320)
@Composable
fun DefaultPreview() {
    val groups = listOf(
        Group(
            clients = listOf(
                Client(
                    config = ClientConfig(1, 10, "OnePlus", Volume(false, 40)),
                    connected = true,
                    host = Host("", "", "", "LEFT Device 1", ""),
                    id = "LEFT Device 1",
                    lastSeen = LastSeen(0, 0),
                    snapclient = SnapClient("Snapclient", 2, "0.34.0"),
                ),
                Client(
                    config = ClientConfig(1, 10, "OnePlus", Volume(false, 40)),
                    connected = true,
                    host = Host("", "", "", "LEFT Device 2", ""),
                    id = "LEFT Device 2",
                    lastSeen = LastSeen(0, 0),
                    snapclient = SnapClient("Snapclient", 2, "0.34.0"),
                ),
                Client(
                    config = ClientConfig(1, 10, "OnePlus", Volume(false, 40)),
                    connected = true,
                    host = Host("", "", "", "LEFT Device 3", ""),
                    id = "LEFT Device 3",
                    lastSeen = LastSeen(0, 0),
                    snapclient = SnapClient("Snapclient", 2, "0.34.0"),
                ),
                Client(
                    config = ClientConfig(1, 10, "OnePlus", Volume(false, 40)),
                    connected = true,
                    host = Host("", "", "", "LEFT Device 4", ""),
                    id = "LEFT Device 4",
                    lastSeen = LastSeen(0, 0),
                    snapclient = SnapClient("Snapclient", 2, "0.34.0"),
                ),
                Client(
                    config = ClientConfig(1, 10, "OnePlus", Volume(false, 40)),
                    connected = true,
                    host = Host("", "", "", "LEFT Device 5", ""),
                    id = "LEFT Device 5",
                    lastSeen = LastSeen(0, 0),
                    snapclient = SnapClient("Snapclient", 2, "0.34.0"),
                ),
            ),
            id = "left_group",
            muted = false,
            name = "LEFT",
            streamId = "left_stream",
        ),
        Group(
            clients = listOf(
                Client(
                    config = ClientConfig(1, 10, "OnePlus", Volume(false, 40)),
                    connected = true,
                    host = Host("", "", "", "RIGHT Device 1", ""),
                    id = "RIGHT Device 1",
                    lastSeen = LastSeen(0, 0),
                    snapclient = SnapClient("Snapclient", 2, "0.34.0"),
                ),
                Client(
                    config = ClientConfig(1, 10, "OnePlus", Volume(false, 40)),
                    connected = true,
                    host = Host("", "", "", "RIGHT Device 2", ""),
                    id = "RIGHT Device 2",
                    lastSeen = LastSeen(0, 0),
                    snapclient = SnapClient("Snapclient", 2, "0.34.0"),
                ),
                Client(
                    config = ClientConfig(1, 10, "OnePlus", Volume(false, 40)),
                    connected = true,
                    host = Host("", "", "", "RIGHT Device 3", ""),
                    id = "RIGHT Device 3",
                    lastSeen = LastSeen(0, 0),
                    snapclient = SnapClient("Snapclient", 2, "0.34.0"),
                ),
                Client(
                    config = ClientConfig(1, 10, "OnePlus", Volume(false, 40)),
                    connected = true,
                    host = Host("", "", "", "RIGHT Device 4", ""),
                    id = "RIGHT Device 4",
                    lastSeen = LastSeen(0, 0),
                    snapclient = SnapClient("Snapclient", 2, "0.34.0"),
                ),
                Client(
                    config = ClientConfig(1, 10, "OnePlus", Volume(false, 40)),
                    connected = true,
                    host = Host("", "", "", "RIGHT Device 5", ""),
                    id = "RIGHT Device 5",
                    lastSeen = LastSeen(0, 0),
                    snapclient = SnapClient("Snapclient", 2, "0.34.0"),
                ),
            ),
            id = "right_group",
            muted = false,
            name = "RIGHT",
            streamId = "right_stream",
        ),
    )
    RadioTheme(
        schemeChoice = SchemeChoice.ORANGE,
    ) {
        SnapserverGroups(
            groups = groups,
            onClientVolumeChange = { _, _, _ -> },
        )
    }
}

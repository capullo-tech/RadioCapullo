package tech.capullo.radio.ui

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import tech.capullo.radio.R
import tech.capullo.radio.snapcast.Client
import tech.capullo.radio.snapcast.ClientConfig
import tech.capullo.radio.snapcast.Host
import tech.capullo.radio.snapcast.LastSeen
import tech.capullo.radio.snapcast.SnapClient
import tech.capullo.radio.snapcast.Volume
import tech.capullo.radio.ui.theme.RadioTheme
import tech.capullo.radio.viewmodels.GroupUIState

@Composable
fun SnapserverGroups(
    modifier: Modifier = Modifier,
    clients: List<Client>,
    onClientVolumeChange: (String, Boolean, Int) -> Unit,
) {
    LazyColumn(modifier = modifier.padding(vertical = 4.dp)) {
        items(
            items = clients,
            key = { client -> client.id },
        ) { client ->
            SnapcastClientCard(
                name = client.host.name,
                muted = client.config.volume.muted,
                volume = client.config.volume.percent.toFloat(),
                onVolumeChange = { muted, volume ->
                    onClientVolumeChange(
                        client.id,
                        muted,
                        volume,
                    )
                },
            )
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun SnapcastClientCard(
    name: String,
    muted: Boolean,
    volume: Float,
    onVolumeChange: (Boolean, Int) -> Unit,
) {
    Card(
        modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp),
    ) {
        var mutedState by remember { mutableStateOf(muted) }
        var volumeState by remember { mutableFloatStateOf(volume) }

        LaunchedEffect(muted) {
            mutedState = muted
        }
        LaunchedEffect(volume) {
            volumeState = volume
        }

        Row(
            modifier = Modifier
                .padding(12.dp)
                .animateContentSize(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow,
                    ),
                ),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(12.dp),
            ) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                    ),
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { mutedState = !mutedState },
                    ) {
                        Icon(
                            painter = if (mutedState) {
                                painterResource(id = R.drawable.volume_off_24px)
                            } else {
                                painterResource(id = R.drawable.volume_up_24px)
                            },
                            contentDescription = if (mutedState) {
                                stringResource(R.string.app_name)
                            } else {
                                stringResource(R.string.app_name)
                            },

                        )
                    }
                    Slider(
                        value = volumeState,
                        onValueChange = {
                            volumeState = it
                            onVolumeChange(mutedState, volumeState.toInt())
                        },
                        valueRange = 0f..100f,
                        modifier = Modifier.weight(2f),
                    )
                }
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
    val clients = listOf(
        Client(
            config =
            ClientConfig(1, 10, "OnePlus", Volume(false, 40)),
            connected = true,
            host = Host("", "", "", "LEFT Device 1", ""),
            id = "LEFT Device 1",
            lastSeen = LastSeen(0, 0),
            snapclient = SnapClient("Snapclient", 2, "0.34.0"),
        ),
        Client(
            config =
            ClientConfig(1, 10, "OnePlus", Volume(false, 40)),
            connected = true,
            host = Host("", "", "", "LEFT Device 2", ""),
            id = "LEFT Device 2",
            lastSeen = LastSeen(0, 0),
            snapclient = SnapClient("Snapclient", 2, "0.34.0"),
        ),
        Client(
            config =
            ClientConfig(1, 10, "OnePlus", Volume(false, 40)),
            connected = true,
            host = Host("", "", "", "LEFT Device 3", ""),
            id = "LEFT Device 3",
            lastSeen = LastSeen(0, 0),
            snapclient = SnapClient("Snapclient", 2, "0.34.0"),
        ),
        Client(
            config =
            ClientConfig(1, 10, "OnePlus", Volume(false, 40)),
            connected = true,
            host = Host("", "", "", "LEFT Device 4", ""),
            id = "LEFT Device 4",
            lastSeen = LastSeen(0, 0),
            snapclient = SnapClient("Snapclient", 2, "0.34.0"),
        ),
        Client(
            config =
            ClientConfig(1, 10, "OnePlus", Volume(false, 40)),
            connected = true,
            host = Host("", "", "", "LEFT Device 5", ""),
            id = "LEFT Device 5",
            lastSeen = LastSeen(0, 0),
            snapclient = SnapClient("Snapclient", 2, "0.34.0"),
        ),
        Client(
            config =
            ClientConfig(1, 10, "OnePlus", Volume(false, 40)),
            connected = true,
            host = Host("", "", "", "RIGHT Device 1", ""),
            id = "RIGHT Device 1",
            lastSeen = LastSeen(0, 0),
            snapclient = SnapClient("Snapclient", 2, "0.34.0"),
        ),
        Client(
            config =
            ClientConfig(1, 10, "OnePlus", Volume(false, 40)),
            connected = true,
            host = Host("", "", "", "RIGHT Device 2", ""),
            id = "RIGHT Device 2",
            lastSeen = LastSeen(0, 0),
            snapclient = SnapClient("Snapclient", 2, "0.34.0"),
        ),
        Client(
            config =
            ClientConfig(1, 10, "OnePlus", Volume(false, 40)),
            connected = true,
            host = Host("", "", "", "RIGHT Device 3", ""),
            id = "RIGHT Device 3",
            lastSeen = LastSeen(0, 0),
            snapclient = SnapClient("Snapclient", 2, "0.34.0"),
        ),
        Client(
            config =
            ClientConfig(1, 10, "OnePlus", Volume(false, 40)),
            connected = true,
            host = Host("", "", "", "RIGHT Device 4", ""),
            id = "RIGHT Device 4",
            lastSeen = LastSeen(0, 0),
            snapclient = SnapClient("Snapclient", 2, "0.34.0"),
        ),
        Client(
            config =
            ClientConfig(1, 10, "OnePlus", Volume(false, 40)),
            connected = true,
            host = Host("", "", "", "RIGHT Device 5", ""),
            id = "RIGHT Device 5",
            lastSeen = LastSeen(0, 0),
            snapclient = SnapClient("Snapclient", 2, "0.34.0"),
        ),
    )
    RadioTheme {
        SnapserverGroups(
            clients = clients,
            onClientVolumeChange = { _, _, _ -> },
        )
    }
}

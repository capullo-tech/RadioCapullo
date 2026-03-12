package tech.capullo.radio.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import tech.capullo.radio.snapcast.Group
import tech.capullo.radio.snapcast.SnapcastControlClient
import tech.capullo.radio.snapcast.SnapclientProcess
import tech.capullo.radio.ui.model.AudioChannel
import tech.capullo.radio.ui.theme.RadioTheme
import tech.capullo.radio.ui.theme.SchemeChoice
import tech.capullo.radio.ui.theme.Typography
import tech.capullo.radio.viewmodels.NowPlayingUiState
import tech.capullo.radio.viewmodels.NowPlayingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingScreen(viewModel: NowPlayingViewModel = hiltViewModel()) {
    val uiState by viewModel.nowPlayingUiState.collectAsStateWithLifecycle()
    NowPlayingScreenContent(
        uiState = uiState,
        groups = viewModel.groups,
        onClientVolumeChange = viewModel::onClientVolumeChange,
        onClientLatencyChange = viewModel::onClientLatencyChange,
        onUpdateAudioChannel = viewModel::updateAudioChannel,
        onStreamControl = viewModel::onStreamControl,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingScreenContent(
    uiState: NowPlayingUiState,
    groups: List<Group>,
    onClientVolumeChange: (String, Boolean, Int) -> Unit,
    onClientLatencyChange: (String, Int) -> Unit,
    onUpdateAudioChannel: (AudioChannel) -> Unit,
    onStreamControl: (String) -> Unit,
) {
    var showChannelDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                actions = {
                    IconButton(onClick = { showChannelDialog = true }) {
                        Icon(Icons.Filled.Menu, contentDescription = "Menu")
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Card(
                modifier = Modifier
                    .padding(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                shape = MaterialTheme.shapes.medium,
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(
                        text = when (uiState.snapclientProcessConnectionState) {
                            SnapclientProcess.ConnectionState.STARTING -> "Connecting to Server..."
                            SnapclientProcess.ConnectionState.CONNECTED -> "Connected to Server"
                            SnapclientProcess.ConnectionState.ERROR -> "Failed to Connect to Server"
                        },
                        style = Typography.headlineMedium,
                    )

                    Text(
                        text = "Server: ${uiState.serverIp}",
                        style = Typography.bodyLarge,
                    )

                    if (uiState.snapclientProcessConnectionState ==
                        SnapclientProcess.ConnectionState.CONNECTED
                    ) {
                        Text(
                            text = "Channel: ${uiState.audioChannelState.label}",
                            style = Typography.bodyLarge,
                        )

                        Text(
                            text = "Playing music via Snapclient",
                            style = Typography.bodyMedium,
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Metadata display
                        uiState.metadata?.let { metadata ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = metadata.title ?: "Unknown Title",
                                    style = Typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    text = when (val artist = metadata.artist) {
                                        is JsonPrimitive -> artist.content

                                        is JsonArray -> {
                                            artist.joinToString(", ") {
                                                it.toString().removeSurrounding("\"")
                                            }
                                        }

                                        else -> "Unknown Artist"
                                    },
                                    style = Typography.bodyLarge,
                                )
                                metadata.album?.let {
                                    Text(
                                        text = it,
                                        style = Typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Playback controls
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(24.dp),
                        ) {
                            IconButton(onClick = { onStreamControl("previous") }) {
                                Icon(
                                    imageVector = Icons.Filled.SkipPrevious,
                                    contentDescription = "Previous",
                                    modifier = Modifier.size(36.dp),
                                )
                            }

                            IconButton(
                                onClick = { onStreamControl("playPause") },
                                modifier = Modifier.size(48.dp),
                            ) {
                                Icon(
                                    imageVector = if (uiState.playbackStatus == "playing") {
                                        Icons.Filled.Pause
                                    } else {
                                        Icons.Filled.PlayArrow
                                    },
                                    contentDescription = if (uiState.playbackStatus == "playing") {
                                        "Pause"
                                    } else {
                                        "Play"
                                    },
                                    modifier = Modifier.size(48.dp),
                                )
                            }

                            IconButton(onClick = { onStreamControl("next") }) {
                                Icon(
                                    imageVector = Icons.Filled.SkipNext,
                                    contentDescription = "Next",
                                    modifier = Modifier.size(36.dp),
                                )
                            }
                        }
                    }
                }
            }
            // Show SnapserverGroups only when snapcastControlClient is connected
            if (uiState.snapcastControlClientConnectionState ==
                SnapcastControlClient.ConnectionState.CONNECTED
            ) {
                SnapserverGroups(
                    groups = groups,
                    onClientVolumeChange = onClientVolumeChange,
                    onClientLatencyChange = onClientLatencyChange,
                )
            } else {
                Card(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = when (uiState.snapcastControlClientConnectionState) {
                                SnapcastControlClient.ConnectionState.STARTING -> {
                                    "Connecting to Snapserver Control..."
                                }

                                SnapcastControlClient.ConnectionState.CONNECTED -> {
                                    "Connected to Snapserver Control"
                                }

                                // This case is handled above
                                SnapcastControlClient.ConnectionState.ERROR -> {
                                    "Failed to Connect to Snapserver Control"
                                }
                            },
                            style = Typography.headlineSmall,
                        )

                        if (uiState.snapcastControlClientConnectionState ==
                            SnapcastControlClient.ConnectionState.ERROR
                        ) {
                            Text(
                                text = "Retrying connection...",
                                style = Typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }

        if (showChannelDialog) {
            AudioSettingsDialog(
                onDismissRequest = { showChannelDialog = false },
                selectedChannel = uiState.audioChannelState,
                onCheckedChanged = { isChecked: Boolean, audioChannel: AudioChannel ->
                    if (isChecked && audioChannel != uiState.audioChannelState) {
                        onUpdateAudioChannel(audioChannel)
                    }
                },
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun NowPlayingScreenContentPreview() {
    val uiState = NowPlayingUiState(
        audioChannelState = AudioChannel.STEREO,
        serverIp = "192.168.1.100",
        snapclientProcessConnectionState = SnapclientProcess.ConnectionState.CONNECTED,
        snapcastControlClientConnectionState = SnapcastControlClient.ConnectionState.CONNECTED,
        playbackStatus = "playing",
        metadata = tech.capullo.radio.snapcast.StreamMetadata(
            title = "Bohemian Rhapsody",
            artist = JsonPrimitive("Queen"),
            album = "A Night at the Opera",
        ),
    )
    RadioTheme(schemeChoice = SchemeChoice.ORANGE) {
        NowPlayingScreenContent(
            uiState = uiState,
            groups = mockSnapcastGroups,
            onClientVolumeChange = { _, _, _ -> },
            onClientLatencyChange = { _, _ -> },
            onUpdateAudioChannel = {},
            onStreamControl = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
fun NowPlayingScreenContentErrorPreview() {
    val uiState = NowPlayingUiState(
        audioChannelState = AudioChannel.STEREO,
        serverIp = "192.168.1.100",
        snapclientProcessConnectionState = SnapclientProcess.ConnectionState.CONNECTED,
        snapcastControlClientConnectionState = SnapcastControlClient.ConnectionState.ERROR,
    )
    RadioTheme(schemeChoice = SchemeChoice.ORANGE) {
        NowPlayingScreenContent(
            uiState = uiState,
            groups = emptyList(), // No groups when control client is not connected
            onClientVolumeChange = { _, _, _ -> },
            onClientLatencyChange = { _, _ -> },
            onUpdateAudioChannel = {},
            onStreamControl = {},
        )
    }
}

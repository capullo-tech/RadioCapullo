package tech.capullo.radio.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import tech.capullo.radio.snapcast.Client
import tech.capullo.radio.snapcast.ClientConfig
import tech.capullo.radio.snapcast.Group
import tech.capullo.radio.snapcast.Host
import tech.capullo.radio.snapcast.LastSeen
import tech.capullo.radio.snapcast.SnapClient
import tech.capullo.radio.snapcast.Volume
import tech.capullo.radio.ui.model.AudioChannel
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
        onUpdateAudioChannel = viewModel::updateAudioChannel,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingScreenContent(
    uiState: NowPlayingUiState,
    groups: List<Group>,
    onClientVolumeChange: (String, Boolean, Int) -> Unit,
    onUpdateAudioChannel: (AudioChannel) -> Unit,
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
                        text = "Connected to Server",
                        style = Typography.headlineMedium,
                    )

                    Text(
                        text = "Server: ${uiState.serverIp}",
                        style = Typography.bodyLarge,
                    )

                    Text(
                        text = "Channel: ${uiState.audioChannel.label}",
                        style = Typography.bodyLarge,
                    )

                    Text(
                        text = "Playing music via Snapclient",
                        style = Typography.bodyMedium,
                    )
                }
            }
            SnapserverGroups(
                groups = groups,
                onClientVolumeChange = onClientVolumeChange,
            )
        }

        if (showChannelDialog) {
            AudioSettingsDialog(
                onDismissRequest = { showChannelDialog = false },
                selectedChannel = uiState.audioChannel,
                onCheckedChanged = { isChecked: Boolean, audioChannel: AudioChannel ->
                    if (isChecked && audioChannel != uiState.audioChannel) {
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
    val mockGroup = Group(
        clients = listOf(
            Client(
                config = ClientConfig(
                    instance = 1,
                    latency = 0,
                    name = "Living Room",
                    volume = Volume(muted = false, percent = 50),
                ),
                connected = true,
                host = Host(
                    arch = "x86_64",
                    ip = "192.168.1.101",
                    mac = "00:11:22:33:44:55",
                    name = "LivingRoomPC",
                    os = "Linux",
                ),
                id = "client1",
                lastSeen = LastSeen(sec = 0, usec = 0),
                snapclient = SnapClient(
                    name = "Snapclient",
                    protocolVersion = 1,
                    version = "0.26.0",
                ),
            ),
            Client(
                config = ClientConfig(
                    instance = 1,
                    latency = 0,
                    name = "Kitchen",
                    volume = Volume(muted = false, percent = 75),
                ),
                connected = true,
                host = Host(
                    arch = "arm64",
                    ip = "192.168.1.102",
                    mac = "AA:BB:CC:DD:EE:FF",
                    name = "KitchenPi",
                    os = "Linux",
                ),
                id = "client2",
                lastSeen = LastSeen(sec = 0, usec = 0),
                snapclient = SnapClient(
                    name = "Snapclient",
                    protocolVersion = 1,
                    version = "0.26.0",
                ),
            ),
        ),
        id = "group1",
        muted = false,
        name = "Default",
        streamId = "stream1",
    )

    NowPlayingScreenContent(
        uiState = NowPlayingUiState(
            audioChannel = AudioChannel.STEREO,
            serverIp = "192.168.1.100",
        ),
        groups = listOf(mockGroup),
        onClientVolumeChange = { _, _, _ -> },
        onUpdateAudioChannel = {},
    )
}

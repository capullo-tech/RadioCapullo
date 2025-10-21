package tech.capullo.radio.ui

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import tech.capullo.radio.R
import tech.capullo.radio.data.RadioRepository.IPv4AddressesResult
import tech.capullo.radio.snapcast.Client
import tech.capullo.radio.snapcast.ClientConfig
import tech.capullo.radio.snapcast.Host
import tech.capullo.radio.snapcast.LastSeen
import tech.capullo.radio.snapcast.SnapClient
import tech.capullo.radio.snapcast.Volume
import tech.capullo.radio.ui.model.AudioChannel
import tech.capullo.radio.ui.theme.RadioTheme
import tech.capullo.radio.ui.theme.SchemeChoice
import tech.capullo.radio.ui.theme.Typography
import tech.capullo.radio.viewmodels.BroadcasterUiState
import tech.capullo.radio.viewmodels.BroadcasterViewModel

@Composable
fun BroadcasterScreen(viewModel: BroadcasterViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    BroadcasterScreenContent(
        uiState = uiState,
        onAudioChannelChange = viewModel::updateAudioChannel,
        onRefreshHostAddresses = viewModel::refreshIPv4Addresses,
    )
}

@Composable
fun BroadcasterScreenContent(
    uiState: BroadcasterUiState,
    onAudioChannelChange: (AudioChannel) -> Unit,
    onRefreshHostAddresses: () -> Unit,
) {
    when (uiState) {
        is BroadcasterUiState.EspotiPlayerReady -> {
            BroadcasterPlayback(
                ipv4AddressesResult = uiState.ipv4AddressesResult,
                snapcastClients = uiState.snapcastClients,
                audioChannel = uiState.audioChannel,
                onRefreshHostAddresses = onRefreshHostAddresses,
                onAudioChannelChange = onAudioChannelChange,
            )
        }

        is BroadcasterUiState.EspotiConnect -> {
            if (uiState.isLoading) {
                LoadingSessionScreen()
            } else {
                BroadcasterEspotiConnect(
                    deviceName = uiState.deviceName,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun BroadcasterPlayback(
    ipv4AddressesResult: IPv4AddressesResult,
    snapcastClients: List<Client> = emptyList(),
    audioChannel: AudioChannel,
    onRefreshHostAddresses: () -> Unit,
    onAudioChannelChange: (AudioChannel) -> Unit,
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
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            IPv4AddressesCard(ipv4AddressesResult, onRefreshHostAddresses)

            SnapclientList(snapcastClients)
        }

        if (showChannelDialog) {
            AudioSettingsDialog(
                onDismissRequest = { showChannelDialog = false },
                selectedChannel = audioChannel,
                onCheckedChanged = { isChecked: Boolean, channel: AudioChannel ->
                    if (isChecked && audioChannel != channel) {
                        onAudioChannelChange(channel)
                    }
                },
            )
        }
    }
}

@Composable
fun IPv4AddressesCard(
    ipv4AddressesResult: IPv4AddressesResult,
    onRefreshHostAddresses: () -> Unit,
) {
    Card(
        modifier = Modifier
            .padding(vertical = 4.dp, horizontal = 8.dp)
            .fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Host Addresses:",
                    style = Typography.titleLarge,
                )
                IconButton(
                    onClick = {
                        onRefreshHostAddresses()
                    },
                    enabled = ipv4AddressesResult !is IPv4AddressesResult.Loading,
                ) {
                    if (ipv4AddressesResult is IPv4AddressesResult.Loading) {
                        @OptIn(ExperimentalMaterial3ExpressiveApi::class)
                        // LoadingIndicator(modifier = Modifier.size(32.dp))
                        CircularWavyProgressIndicator()
                    } else {
                        Icon(
                            Icons.Filled.Refresh,
                            contentDescription = "Refresh ip addresses",
                            modifier = Modifier
                                .size(32.dp),
                        )
                    }
                }
            }

            when (ipv4AddressesResult) {
                is IPv4AddressesResult.Success -> {
                    LazyColumn(modifier = Modifier.padding(vertical = 4.dp)) {
                        items(items = ipv4AddressesResult.addresses) { address ->
                            Text(
                                text = address,
                                style = Typography.displaySmall,
                            )
                        }
                    }
                }
                is IPv4AddressesResult.Error -> {
                    Text(
                        text =
                        ipv4AddressesResult.message
                            ?: "Error loading network interfaces",
                        style = Typography.displaySmall,
                    )
                }
                else -> Unit
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LoadingSessionScreen() {
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            LoadingIndicator(
                modifier = Modifier.size(64.dp),
            )

            Spacer(modifier = Modifier.padding(16.dp))

            Text(
                text = "Checking for previous playback session...",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun BroadcasterEspotiConnect(deviceName: String) {
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = """
                Connect to this device as a speaker on Spotify
                """.trimIndent(),
                style = Typography.headlineMedium,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.padding(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.google_home_devices_24px),
                    contentDescription = "Espoti Connect Speaker Device",
                )
                Text(
                    text = deviceName,
                )
            }
            Spacer(modifier = Modifier.padding(8.dp))
            LinearWavyProgressIndicator()
        }
    }
}

@Preview(
    showBackground = true,
    uiMode = UI_MODE_NIGHT_YES,
    name = "PreviewBroadcasterEspotiConnectDark",
)
@Preview(showBackground = true)
@Composable
fun PreviewBroadcasterEspotiConnect() {
    val deviceName = "Samsung Galaxy S21 Ultra Max"
    RadioTheme(schemeChoice = SchemeChoice.GREEN) {
        BroadcasterEspotiConnect(deviceName = deviceName)
    }
}

@Preview(
    showBackground = true,
    uiMode = UI_MODE_NIGHT_YES,
    name = "PreviewLoadingIPv4AddressesCard",
)
@Preview(showBackground = true)
@Composable
fun PreviewLoadingIPv4AddressesCard() {
    RadioTheme(schemeChoice = SchemeChoice.GREEN) {
        IPv4AddressesCard(
            ipv4AddressesResult = IPv4AddressesResult.Loading,
            onRefreshHostAddresses = {},
        )
    }
}

@Preview(
    showBackground = true,
    uiMode = UI_MODE_NIGHT_YES,
    name = "PreviewLoadingIndicatorDark",
)
@Preview(showBackground = true)
@Composable
fun PreviewLoadingIndicator() {
    RadioTheme(schemeChoice = SchemeChoice.GREEN) {
        LoadingSessionScreen()
    }
}

@Preview(
    showBackground = true,
    uiMode = UI_MODE_NIGHT_YES,
    name = "PreviewBroadcasterPlaybackDark",
    showSystemUi = true,
)
@Preview(
    showBackground = true,
    showSystemUi = true,
)
@Composable
fun PreviewBroadcasterPlayback() {
    val ipv4AddressesResult = IPv4AddressesResult.Success(
        listOf("192.168.0.1", "0.0.0.0", "100.10.14.7"),
    )

    val sampleClients = listOf(
        Client(
            config = ClientConfig(
                instance = 1,
                latency = 0,
                name = "Living Room",
                volume = Volume(muted = false, percent = 85),
            ),
            connected = true,
            host = Host(
                arch = "x86_64",
                ip = "192.168.1.100",
                mac = "00:00:00:00:00:00",
                name = "Nacatambucho",
                os = "Android 15",
            ),
            id = "client1",
            lastSeen = LastSeen(sec = 1740010683, usec = 710695),
            snapclient = SnapClient(
                name = "Snapclient",
                protocolVersion = 2,
                version = "0.29.0",
            ),
        ),
        Client(
            config = ClientConfig(
                instance = 2,
                latency = 0,
                name = "Kitchen",
                volume = Volume(muted = true, percent = 50),
            ),
            connected = true,
            host = Host(
                arch = "x86_64",
                ip = "192.168.1.101",
                mac = "00:00:00:00:00:01",
                name = "Pixel 3a",
                os = "Android 15",
            ),
            id = "client2",
            lastSeen = LastSeen(sec = 1740010683, usec = 710695),
            snapclient = SnapClient(
                name = "Snapclient",
                protocolVersion = 2,
                version = "0.29.0",
            ),
        ),
        Client(
            config = ClientConfig(
                instance = 2,
                latency = 0,
                name = "Kitchen",
                volume = Volume(muted = true, percent = 100),
            ),
            connected = true,
            host = Host(
                arch = "x86_64",
                ip = "192.168.1.101",
                mac = "00:00:00:00:00:01",
                name = "¡OnePlus 3T!",
                os = "Android 15",
            ),
            id = "client2",
            lastSeen = LastSeen(sec = 1740010683, usec = 710695),
            snapclient = SnapClient(
                name = "Snapclient",
                protocolVersion = 2,
                version = "0.29.0",
            ),
        ),
    )

    RadioTheme(schemeChoice = SchemeChoice.GREEN) {
        BroadcasterPlayback(
            ipv4AddressesResult = ipv4AddressesResult,
            snapcastClients = sampleClients,
            audioChannel = AudioChannel.STEREO,
            onAudioChannelChange = { _: AudioChannel -> },
            onRefreshHostAddresses = {},
        )
    }
}

package tech.capullo.radio.ui

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import kotlinx.serialization.json.JsonPrimitive
import tech.capullo.radio.data.RadioRepository.IPv4AddressesResult
import tech.capullo.radio.snapcast.Client
import tech.capullo.radio.snapcast.ClientConfig
import tech.capullo.radio.snapcast.Group
import tech.capullo.radio.snapcast.Host
import tech.capullo.radio.snapcast.LastSeen
import tech.capullo.radio.snapcast.SnapClient
import tech.capullo.radio.snapcast.SnapcastControlClient
import tech.capullo.radio.snapcast.StreamMetadata
import tech.capullo.radio.snapcast.Volume
import tech.capullo.radio.ui.model.AudioChannel
import tech.capullo.radio.ui.theme.RadioTheme
import tech.capullo.radio.ui.theme.SchemeChoice
import tech.capullo.radio.ui.theme.Typography
import tech.capullo.radio.viewmodels.BroadcasterUiState
import tech.capullo.radio.viewmodels.BroadcasterViewModel
import tech.capullo.radio.viewmodels.StreamCommand

@Composable
fun BroadcasterScreen(viewModel: BroadcasterViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    LaunchedEffect(Unit) {
        viewModel.initialize()
    }

    BroadcasterScreenContent(
        uiState = uiState,
        onAudioChannelChange = viewModel::updateAudioChannel,
        onRefreshHostAddresses = viewModel::refreshIPv4Addresses,
        groups = viewModel.groups,
        onClientVolumeChange = viewModel::onClientVolumeChange,
        onClientLatencyChange = viewModel::onClientLatencyChange,
        onStreamControl = viewModel::onStreamControl,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BroadcasterScreenContent(
    uiState: BroadcasterUiState,
    onAudioChannelChange: (AudioChannel) -> Unit,
    onRefreshHostAddresses: () -> Unit,
    groups: List<Group>,
    onClientVolumeChange: (String, Boolean, Int) -> Unit,
    onClientLatencyChange: (String, Int) -> Unit,
    onStreamControl: (StreamCommand) -> Unit,
) {
    var showChannelDialog by remember { mutableStateOf(false) }
    var isExpanded by remember { mutableStateOf(false) }

    if (isExpanded) {
        BackHandler { isExpanded = false }
    }

    val controlConnected =
        uiState.snapcastControlClientConnectionState ==
            SnapcastControlClient.ConnectionState.CONNECTED

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                LargeTopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "Radio Capullo",
                                style = MaterialTheme.typography.displaySmall,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = "Broadcasting",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    },
                    actions = {
                        ConnectionStatusChip(uiState.snapcastControlClientConnectionState)
                        IconButton(onClick = { showChannelDialog = true }) {
                            Icon(Icons.Filled.Menu, contentDescription = "Menu")
                        }
                    },
                )
            },
            bottomBar = {
                if (controlConnected) {
                    PlaybackMiniBar(
                        metadata = uiState.metadata,
                        artistDisplay = uiState.artistDisplay,
                        playbackStatus = uiState.playbackStatus,
                        canPlay = uiState.canPlay,
                        canPause = uiState.canPause,
                        canGoNext = uiState.canGoNext,
                        canGoPrevious = uiState.canGoPrevious,
                        onStreamControl = onStreamControl,
                        onExpand = { isExpanded = true },
                    )
                }
            },
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                IPv4AddressesCard(uiState.ipv4AddressesResult, onRefreshHostAddresses)

                if (controlConnected) {
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
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(
                                alpha = 0.3f,
                            ),
                        ),
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = when (uiState.snapcastControlClientConnectionState) {
                                    SnapcastControlClient.ConnectionState.STARTING ->
                                        "Starting Broadcaster..."

                                    SnapcastControlClient.ConnectionState.ERROR ->
                                        "Broadcaster Connection Error"

                                    SnapcastControlClient.ConnectionState.CONNECTED -> ""
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )

                            if (uiState.snapcastControlClientConnectionState ==
                                SnapcastControlClient.ConnectionState.ERROR
                            ) {
                                Text(
                                    text = "Check if Snapserver is running locally",
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }
                }
            }

            if (showChannelDialog) {
                AudioSettingsDialog(
                    onDismissRequest = { showChannelDialog = false },
                    selectedChannel = uiState.audioChannel,
                    onCheckedChanged = { isChecked: Boolean, channel: AudioChannel ->
                        if (isChecked && uiState.audioChannel != channel) {
                            onAudioChannelChange(channel)
                        }
                    },
                )
            }
        }

        AnimatedVisibility(
            visible = isExpanded,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
            ) + fadeIn(animationSpec = tween(durationMillis = 300)),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
            ) + fadeOut(animationSpec = tween(durationMillis = 300)),
        ) {
            ExpandedPlayerScreen(
                metadata = uiState.metadata,
                artistDisplay = uiState.artistDisplay,
                playbackStatus = uiState.playbackStatus,
                canPlay = uiState.canPlay,
                canPause = uiState.canPause,
                canGoNext = uiState.canGoNext,
                canGoPrevious = uiState.canGoPrevious,
                onStreamControl = onStreamControl,
                onCollapse = { isExpanded = false },
                onShowChannelDialog = { showChannelDialog = true },
            )
        }
    }
}

@Composable
private fun ConnectionStatusChip(state: SnapcastControlClient.ConnectionState) {
    Surface(
        shape = CircleShape,
        color = when (state) {
            SnapcastControlClient.ConnectionState.CONNECTED ->
                MaterialTheme.colorScheme.primaryContainer

            SnapcastControlClient.ConnectionState.ERROR ->
                MaterialTheme.colorScheme.errorContainer

            else -> MaterialTheme.colorScheme.surfaceVariant
        }.copy(alpha = 0.7f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(
                        color = when (state) {
                            SnapcastControlClient.ConnectionState.CONNECTED ->
                                MaterialTheme.colorScheme.primary

                            SnapcastControlClient.ConnectionState.ERROR ->
                                MaterialTheme.colorScheme.error

                            else -> MaterialTheme.colorScheme.outline
                        },
                        shape = CircleShape,
                    ),
            )
            Text(
                text = when (state) {
                    SnapcastControlClient.ConnectionState.STARTING -> "Starting"
                    SnapcastControlClient.ConnectionState.CONNECTED -> "Live"
                    SnapcastControlClient.ConnectionState.ERROR -> "Offline"
                },
                style = MaterialTheme.typography.labelSmall,
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
    name = "PreviewBroadcasterScreenContentDark",
    showSystemUi = true,
)
@Preview(
    showBackground = true,
    showSystemUi = true,
)
@Composable
fun PreviewBroadcasterScreenContent() {
    val ipv4AddressesResult = IPv4AddressesResult.Success(
        listOf("192.168.0.1", "0.0.0.0", "100.10.14.7"),
    )

    val uiState = BroadcasterUiState(
        ipv4AddressesResult = ipv4AddressesResult,
        audioChannel = AudioChannel.STEREO,
        snapcastControlClientConnectionState = SnapcastControlClient.ConnectionState.CONNECTED,
        playbackStatus = "playing",
        metadata = StreamMetadata(
            title = "Bohemian Rhapsody",
            artist = JsonPrimitive("Queen"),
            album = "A Night at the Opera",
        ),
        canPlay = true,
        canPause = true,
        canGoNext = true,
        canGoPrevious = true,
        artistDisplay = "Queen",
    )

    RadioTheme(schemeChoice = SchemeChoice.GREEN) {
        BroadcasterScreenContent(
            uiState,
            onAudioChannelChange = { },
            onRefreshHostAddresses = { },
            groups = listOf(
                Group(
                    clients = listOf(
                        Client(
                            config =
                            ClientConfig(
                                instance = 1,
                                latency = 10,
                                name = "OnePlus",
                                volume = Volume(muted = false, percent = 40),
                            ),
                            connected = true,
                            host = Host(
                                arch = "",
                                ip = "",
                                mac = "",
                                name = "OnePlus",
                                os = "",
                            ),
                            id = "xxxxx",
                            lastSeen = LastSeen(
                                sec = 0,
                                usec = 0,
                            ),
                            snapclient = SnapClient(
                                name = "Snapclient",
                                protocolVersion = 2,
                                version = "0.34.0",
                            ),
                        ),
                    ),
                    id = "group1",
                    muted = false,
                    name = "Group 1",
                    streamId = "stream1",
                ),
            ),
            onClientVolumeChange = { _, _, _ -> },
            onClientLatencyChange = { _, _ -> },
            onStreamControl = { },
        )
    }
}

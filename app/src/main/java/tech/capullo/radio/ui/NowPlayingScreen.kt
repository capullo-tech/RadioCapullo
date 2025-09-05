package tech.capullo.radio.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import tech.capullo.radio.ui.theme.Typography
import tech.capullo.radio.viewmodels.RadioTuneInModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingScreen(radioTuneInModel: RadioTuneInModel = hiltViewModel()) {
    val connectionState by radioTuneInModel.connectionState.collectAsState()
    var showChannelDialog by remember { mutableStateOf(false) }
    var selectedChannel by remember { mutableStateOf(AudioChannel.STEREO) }

    LaunchedEffect(connectionState.channel) {
        selectedChannel = connectionState.channel
    }

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
            verticalArrangement = Arrangement.Center,
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
                        text = "Server: ${connectionState.serverIp}",
                        style = Typography.bodyLarge,
                    )

                    Text(
                        text = "Channel: ${connectionState.channel.label}",
                        style = Typography.bodyLarge,
                    )

                    Text(
                        text = "Playing music via Snapclient",
                        style = Typography.bodyMedium,
                    )

                    if (connectionState.isConnected) {
                        Text(
                            text = "✓ Service is running",
                            style = Typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    } else {
                        Text(
                            text = "⏳ Connecting...",
                            style = Typography.bodyMedium,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    }
                }
            }
        }

        if (showChannelDialog) {
            AudioSettingsDialog(
                onDismissRequest = { showChannelDialog = false },
                selectedChannel = selectedChannel,
                onCheckedChanged = { isChecked, audioChannel ->
                    if (isChecked && selectedChannel != audioChannel) {
                        selectedChannel = audioChannel
                        radioTuneInModel.updateAudioChannel(audioChannel)
                    }
                },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AudioSettingsDialog(
    onDismissRequest: () -> Unit,
    selectedChannel: AudioChannel,
    onCheckedChanged: (Boolean, AudioChannel) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Audio Channel Settings") },
        text = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(
                    ButtonGroupDefaults.ConnectedSpaceBetween,
                ),
            ) {
                AudioChannel.entries.forEach { channel ->
                    ToggleButton(
                        checked = selectedChannel == channel,
                        onCheckedChange = { isChecked -> onCheckedChanged(isChecked, channel) },
                        modifier = Modifier.weight(channel.modifierWeight),
                        shapes =
                        when (channel) {
                            AudioChannel.LEFT -> {
                                ButtonGroupDefaults.connectedLeadingButtonShapes()
                            }
                            AudioChannel.RIGHT -> {
                                ButtonGroupDefaults.connectedTrailingButtonShapes()
                            }
                            AudioChannel.STEREO -> {
                                ButtonGroupDefaults.connectedMiddleButtonShapes()
                            }
                        },
                        contentPadding = PaddingValues(0.dp),
                    ) {
                        Icon(
                            if (selectedChannel == channel) {
                                channel.selectedIcon
                            } else {
                                channel.unselectedIcon
                            },
                            contentDescription = channel.label,
                        )
                        Spacer(Modifier.size(ToggleButtonDefaults.IconSpacing))
                        Text(
                            text = channel.label,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Dismiss")
            }
        },
    )
}

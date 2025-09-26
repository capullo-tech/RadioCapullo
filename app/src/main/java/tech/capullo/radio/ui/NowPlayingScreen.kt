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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import tech.capullo.radio.ui.model.AudioChannel
import tech.capullo.radio.ui.theme.Typography
import tech.capullo.radio.viewmodels.TuneInModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingScreen(viewModel: TuneInModel = hiltViewModel()) {
    val uiState by viewModel.tuneInState.collectAsState()
    var showChannelDialog by remember { mutableStateOf(false) }
    var selectedChannel by remember { mutableStateOf(AudioChannel.STEREO) }

    LaunchedEffect(uiState.audioChannel) {
        selectedChannel = uiState.audioChannel
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

                    if (uiState.isTunedIn) {
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
                onCheckedChanged = { isChecked: Boolean, audioChannel: AudioChannel ->
                    if (isChecked && selectedChannel != audioChannel) {
                        selectedChannel = audioChannel
                        viewModel.updateAudioChannel(audioChannel)
                    }
                },
            )
        }
    }
}

package tech.capullo.radio.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import tech.capullo.radio.R
import tech.capullo.radio.snapcast.Client
import tech.capullo.radio.snapcast.ClientConfig
import tech.capullo.radio.snapcast.Host
import tech.capullo.radio.snapcast.LastSeen
import tech.capullo.radio.snapcast.SnapClient
import tech.capullo.radio.snapcast.Volume
import tech.capullo.radio.ui.theme.Typography
import tech.capullo.radio.ui.theme.onSecondaryLight
import tech.capullo.radio.ui.theme.primaryGreen
import tech.capullo.radio.ui.theme.surfaceLight
import tech.capullo.radio.viewmodels.RadioBroadcasterUiState
import tech.capullo.radio.viewmodels.RadioBroadcasterViewModel

@Composable
fun RadioBroadcasterScreen(viewModel: RadioBroadcasterViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    when (uiState) {
        is RadioBroadcasterUiState.EspotiPlayerReady -> {
            val state = uiState as RadioBroadcasterUiState.EspotiPlayerReady
            RadioBroadcasterPlayback(
                hostAddresses = state.hostAddresses,
                snapcastClients = state.snapcastClients,
            )
        }
        else -> {
            val state = uiState as RadioBroadcasterUiState.EspotiConnect
            if (state.loadingStoredCredentials) {
                LoadingIndicator()
            } else {
                RadioBroadcasterEspotiConnect(
                    deviceName = state.deviceName,
                )
            }
        }
    }
}

@Composable
fun LoadingIndicator() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.width(64.dp),
            color = MaterialTheme.colorScheme.secondary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
    }
}

@Composable
fun RadioBroadcasterEspotiConnect(deviceName: String) {
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
                    contentDescription = "trs",
                )
                Text(
                    text = deviceName,
                )
            }
        }
    }
}

@Composable fun RadioBroadcasterPlayback(
    hostAddresses: List<String>,
    snapcastClients: List<Client> = emptyList(),
) {
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(surfaceLight),
        ) {
            Card(
                modifier = Modifier
                    .padding(vertical = 4.dp, horizontal = 8.dp)
                    .fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = primaryGreen),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Host Addresses:",
                        style = Typography.bodyMedium,
                        color = Color.Black,
                    )
                    LazyColumn(modifier = Modifier.padding(vertical = 4.dp)) {
                        items(items = hostAddresses) { name ->
                            Text(
                                text = name,
                                style = Typography.titleLarge,
                                color = onSecondaryLight,
                            )
                        }
                    }
                }
            }

            SnapclientList(snapcastClients)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewRadioBroadcasterEspotiConnect() {
    val deviceName = "Samsung Galaxy S21 Ultra Max"
    RadioBroadcasterEspotiConnect(deviceName = deviceName)
}

@Preview(showBackground = true)
@Composable
fun PreviewLoadingIndicator() {
    LoadingIndicator()
}

@Preview(showBackground = true)
@Composable
fun PreviewRadioBroadcasterPlayback() {
    val hostAddresses = listOf("192.168.0.1", "0.0.0.0", "100.10.14.7")

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
                name = "Karin's nacatambucho",
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
                name = "mrG's oneplus 3T",
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

    RadioBroadcasterPlayback(
        hostAddresses = hostAddresses,
        snapcastClients = sampleClients,
    )
}

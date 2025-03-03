package tech.capullo.radio.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import tech.capullo.radio.data.Client
import tech.capullo.radio.data.ClientConfig
import tech.capullo.radio.data.Host
import tech.capullo.radio.data.LastSeen
import tech.capullo.radio.data.SnapClient
import tech.capullo.radio.data.Volume
import tech.capullo.radio.ui.theme.Typography
import tech.capullo.radio.ui.theme.primaryBlack
import tech.capullo.radio.ui.theme.secondaryOrange
import tech.capullo.radio.viewmodels.RadioBroadcasterViewModel

@Composable
fun RadioBroadcasterScreen(
    viewModel: RadioBroadcasterViewModel = hiltViewModel(),
    useDarkTheme: Boolean = false,
) {
    val colorScheme = if (useDarkTheme) darkColorScheme() else lightColorScheme()
    val snapcastClients by viewModel.snapcastClients.collectAsState()

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
    ) {
        RadioBroadcasterContent(
            hostAddresses = viewModel.hostAddresses,
            deviceName = viewModel.getDeviceName(),
            snapcastClients,
        )
    }
}

@Composable
fun RadioBroadcasterContent(
    hostAddresses: List<String>,
    deviceName: String,
    snapcastClients: List<Client> = emptyList(),
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.DarkGray),
    ) {
        Card(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(containerColor = primaryBlack),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Discoverable on Spotify as:",
                    style = Typography.bodyMedium,
                    color = Color.White,
                )
                Text(
                    text = deviceName,
                    style = Typography.titleLarge,
                    color = secondaryOrange,
                )
                Text(
                    text = "Host Addresses:",
                    style = Typography.bodyMedium,
                    color = Color.White,
                )
                LazyColumn(modifier = Modifier.padding(vertical = 4.dp)) {
                    items(items = hostAddresses) { name ->
                        Text(
                            text = name,
                            style = Typography.titleLarge,
                            color = secondaryOrange,
                        )
                    }
                }
            }
        }

        SnapclientList(snapcastClients)
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewRadioBroadcasterContent() {
    val hostAddresses = listOf("192.168.0.1", "0.0.0.0", "100.10.14.7")
    val deviceName = "Samsung Galaxy S21 Ultra Max"
    RadioBroadcasterContent(hostAddresses, deviceName)
}

@Preview(showBackground = true)
@Composable
fun PreviewRadioBroadcasterContentWithClients() {
    val hostAddresses = listOf("192.168.0.1", "0.0.0.0", "100.10.14.7")
    val deviceName = "Samsung Galaxy S21 Ultra Max"

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

    RadioBroadcasterContent(
        hostAddresses = hostAddresses,
        deviceName = deviceName,
        snapcastClients = sampleClients,
    )
}

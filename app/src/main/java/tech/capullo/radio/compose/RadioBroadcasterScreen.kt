package tech.capullo.radio.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
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

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
    ) {
        RadioBroadcasterContent(
            hostAddresses = viewModel.hostAddresses,
            deviceName = viewModel.getDeviceName(),
        )
    }
}

@Composable
fun RadioBroadcasterContent(hostAddresses: List<String>, deviceName: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.DarkGray),
        contentAlignment = Alignment.TopCenter,
    ) {
        Card(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
                .fillMaxHeight(0.5f),
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
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewRadioBroadcasterContent() {
    val hostAddresses = listOf("192.168.0.1", "0.0.0.0", "100.10.14.7")
    val deviceName = "Samsung Galaxy S21 Ultra Max"
    RadioBroadcasterContent(hostAddresses, deviceName)
}

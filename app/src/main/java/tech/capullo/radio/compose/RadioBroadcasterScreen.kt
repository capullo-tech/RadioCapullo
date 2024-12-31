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
//import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import tech.capullo.radio.ui.theme.*  // Import the typography and color scheme
import tech.capullo.radio.viewmodels.RadioBroadcasterViewModel
//import androidx.lifecycle.ViewModel

@Composable
fun RadioBroadcasterScreen(
    viewModel: RadioBroadcasterViewModel = hiltViewModel(),
    useDarkTheme: Boolean = false // Option to toggle between light and dark themes
) {
    // Choose the color scheme based on the theme preference
    val colorScheme = if (useDarkTheme) darkColorScheme() else lightColorScheme()

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.DarkGray),
            contentAlignment = Alignment.TopCenter
        ) {
            Card(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
                    .fillMaxHeight(0.5f),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp), // Adjust the elevation for the shadow effect
                shape = MaterialTheme.shapes.medium, // Optional: You can change the shape of the card (rounded corners)
                colors = CardDefaults.cardColors(containerColor = secondaryBlack)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Discoverable on Spotify as:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White
                    )
                    Text(
                        text = viewModel.getDeviceName(),
                        style = MaterialTheme.typography.titleLarge,
                        color = tertiaryOrange
                    )
                    Text(
                        text = "Host Addresses:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White
                    )
                    LazyColumn(modifier = Modifier.padding(vertical = 4.dp)) {
                        items(items = viewModel.hostAddresses) { name ->
                            Text(
                                text = name,
                                style = MaterialTheme.typography.titleLarge,
                                color = tertiaryOrange
                            )
                        }
                    }
                }
            }
        }
    }
}

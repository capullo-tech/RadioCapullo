package tech.capullo.radio.compose

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import tech.capullo.radio.viewmodels.RadioViewModel

@Composable
fun RadioBroadcasterScreen(
    radioViewModel: RadioViewModel = hiltViewModel(),
) {
    radioViewModel.startSpotifyBroadcasting()

    Column {
        Text("Discoverable on Spotify as: ${radioViewModel.getDeviceName()}")
        Text("Host Addresses:")
        LazyColumn(modifier = Modifier.padding(vertical = 4.dp)) {
            items(items = radioViewModel.hostAddresses) { name ->
                Text(name)
            }
        }
    }
}

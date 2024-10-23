package tech.capullo.radio.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import tech.capullo.radio.viewmodels.RadioBroadcasterViewModel

@Composable
fun RadioBroadcasterScreen(
    viewModel: RadioBroadcasterViewModel = hiltViewModel(),
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column {
            Text("Discoverable on Spotify as: ${viewModel.getDeviceName()}")
            Text("Host Addresses:")
            LazyColumn(modifier = Modifier.padding(vertical = 4.dp)) {
                items(items = viewModel.hostAddresses) { name ->
                    Text(name)
                }
            }
        }
    }
}

package tech.capullo.radio.compose

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import tech.capullo.radio.viewmodels.RadioBroadcasterViewModel

@Composable
fun RadioBroadcasterScreen(
    viewModel: RadioBroadcasterViewModel = hiltViewModel(),
) {
    var isServiceRunning by remember { mutableStateOf(false) }

    Column {
        if (isServiceRunning) {
            Text("Discoverable on Spotify as: ${viewModel.getDeviceName()}")
            Text("Host Addresses:")
            LazyColumn(modifier = Modifier.padding(vertical = 4.dp)) {
                items(items = viewModel.hostAddresses) { name ->
                    Text(name)
                }
            }
        }
        else {
            Button(
                onClick = {
                    viewModel.startNsdService()
                    isServiceRunning = !isServiceRunning
                }
            ) {
                Text("Start NSD Service")
            }
        }
    }
}

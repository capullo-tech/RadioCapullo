package tech.capullo.radio.compose

import android.content.res.Configuration
import android.os.Build
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import tech.capullo.radio.ui.theme.RadioTheme
import tech.capullo.radio.viewmodels.RadioViewModel

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun RadioApp(
    modifier: Modifier = Modifier,
    radioViewModel: RadioViewModel = viewModel()
) {
    Surface(
        modifier, color = MaterialTheme.colorScheme.background
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val multiplePermissionsState =
                rememberMultiplePermissionsState(
                    permissions = listOf(
                        android.Manifest.permission.NEARBY_WIFI_DEVICES,
                        android.Manifest.permission.POST_NOTIFICATIONS
                    )
                )
            if (multiplePermissionsState.allPermissionsGranted) {
                RadioMainScreen(
                    deviceName = radioViewModel.getDeviceName(),
                    hostAddresses = radioViewModel.hostAddresses,
                    snapclientsList = emptyList<String>(), // radioViewModel.snapClientsList,
                    startBroadcast = { radioViewModel.startSpotifyBroadcasting() },
                    lastServer = radioViewModel.getText(),
                    saveServer = { ip -> radioViewModel.saveText(ip) },
                    startWorker = { ip -> radioViewModel.initiateWorker(ip) }
                )
            } else {
                RadioPermissionHandler(multiplePermissionsState = multiplePermissionsState)
            }
        } else {
            RadioMainScreen(
                deviceName = radioViewModel.getDeviceName(),
                hostAddresses = radioViewModel.hostAddresses,
                snapclientsList = emptyList(), // radioViewModel.snapClientsList,
                startBroadcast = { radioViewModel.startSpotifyBroadcasting() },
                lastServer = radioViewModel.getText(),
                saveServer = { ip -> radioViewModel.saveText(ip) },
                startWorker = { ip -> radioViewModel.initiateWorker(ip) }
            )
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun RadioPermissionHandler(multiplePermissionsState: MultiplePermissionsState) {
    val textToShow =
        getTextToShowGivenPermissions(
            multiplePermissionsState.revokedPermissions,
            multiplePermissionsState.shouldShowRationale
        )
    RadioPermissionScreen(textToShow = textToShow) {
        multiplePermissionsState.launchMultiplePermissionRequest()
    }
}

@Composable
fun RadioPermissionScreen(textToShow: String, onPermissionRequest: () -> Unit) {
    Column {
        Text(
            textToShow
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onPermissionRequest) {
            Text("Request permissions")
        }
    }
}
@Preview
@Composable
fun RadioPermissionScreenPreview() {
    RadioTheme {
        RadioPermissionScreen(
            textToShow = """The INTERNET, ACCESS_NETWORK_STATE, and ACCESS_WIFI_STATE 
                permissions are important. Please grant all of them for the app to function 
                properly."""
        ) { }
    }
}
@Composable
fun RadioMainScreen(
    deviceName: String,
    hostAddresses: List<String>,
    startBroadcast: () -> Unit,
    snapclientsList: List<String>,
    lastServer: String,
    saveServer: (String) -> Unit,
    startWorker: (String) -> Unit
) {
    var text by remember { mutableStateOf(lastServer) }
    var buttonText by remember { mutableStateOf("Tune in") }
    var isBroadcasting by remember { mutableStateOf(false) } // Add this line


    Column {
        Text("Radio Capullo")
        if (isBroadcasting) {
            Text("Discoverable on Spotify as: $deviceName")
            Text("Host Addresses:")
            LazyColumn(modifier = Modifier.padding(vertical = 4.dp)) {
                items(items = hostAddresses) { name ->
                    Text(name)
                }
            }
            SnapclientList(snapclientList = snapclientsList)
        }
            Button(
            onClick = {
                startBroadcast()
                isBroadcasting = true
            },
            enabled = !isBroadcasting
        ) {
            Text(if (isBroadcasting) "Broadcasting" else "Broadcast") // Modify this line
        }
        Row {
            TextField(
                value = text,
                onValueChange = { newText ->
                    text = newText
                    saveServer(newText)
                },
                label = { Text("Host Address") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    startWorker(text)
                    buttonText = "Restart"
                }
            ) {

                Text(buttonText)
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
private fun getTextToShowGivenPermissions(
    permissions: List<PermissionState>,
    shouldShowRationale: Boolean
): String {
    val revokedPermissionsSize = permissions.size
    if (revokedPermissionsSize == 0) return ""

    val textToShow = StringBuilder().apply {
        append("The ")
    }

    for (i in permissions.indices) {
        textToShow.append(permissions[i].permission)
        when {
            revokedPermissionsSize > 1 && i == revokedPermissionsSize - 2 -> {
                textToShow.append(", and ")
            }
            i == revokedPermissionsSize - 1 -> {
                textToShow.append(" ")
            }
            else -> {
                textToShow.append(", ")
            }
        }
    }
    textToShow.append(if (revokedPermissionsSize == 1) "permission is" else "permissions are")
    textToShow.append(
        if (shouldShowRationale) {
            " important. Please grant all of them for the app to function properly."
        } else {
            " denied. The app cannot function without them."
        }
    )
    return textToShow.toString()
}

@Preview(
    showBackground = true,
    widthDp = 320,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    name = "DefaultPreviewDark"
)
@Preview(showBackground = true, widthDp = 320)
@Composable
fun RadioAppPreview() {
    RadioTheme {
        RadioMainScreen(
            deviceName = "Pixel 3a API 28",
            hostAddresses = listOf("192.168.0.109", "100.17.17.4"),
            snapclientsList = listOf(),
            startBroadcast = { println("Broadcasting") },
            lastServer = "",
            saveServer = { ip -> println(ip) },
            startWorker = { ip -> println(ip) }
        )
    }
}

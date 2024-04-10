package tech.capullo.radio.compose

import android.content.res.Configuration
import android.os.Build
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import control.json.ServerStatus
import tech.capullo.radio.ui.theme.RadioTheme
import tech.capullo.radio.viewmodels.RadioViewModel

@Composable
fun RadioApp(
    modifier: Modifier = Modifier,
    radioViewModel: RadioViewModel = viewModel()
) {
    Surface(
        modifier, color = MaterialTheme.colorScheme.background
    ) {
        RadioMainScreen(
            deviceName = radioViewModel.getDeviceName(),
            hostAddresses = radioViewModel.hostAddresses,
            snapclientsList = radioViewModel.snapClientsList
        )
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun RadioMainScreen(
    deviceName: String,
    hostAddresses: List<String>,
    snapclientsList: List<ServerStatus>
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val multiplePermissionsState =
            rememberMultiplePermissionsState(
                listOf(
                    android.Manifest.permission.INTERNET,
                    android.Manifest.permission.ACCESS_NETWORK_STATE,
                    android.Manifest.permission.ACCESS_WIFI_STATE,
                    android.Manifest.permission.NEARBY_WIFI_DEVICES,
                    android.Manifest.permission.BLUETOOTH_SCAN,
                    android.Manifest.permission.BLUETOOTH_ADVERTISE,
                    android.Manifest.permission.BLUETOOTH_CONNECT
                )
            )
        if (multiplePermissionsState.allPermissionsGranted) {
            Text("Nearby permission Granted!")
        } else {
            Text(
                getTextToShowGivenPermissions(
                    multiplePermissionsState.revokedPermissions,
                    multiplePermissionsState.shouldShowRationale
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = { multiplePermissionsState.launchMultiplePermissionRequest() }) {
                Text("Request permissions")
            }
            Column {
                Text("Radio Capullo")
                Text("Discoverable as: $deviceName")
                LazyColumn(modifier = Modifier.padding(vertical = 4.dp)) {
                    items(items = hostAddresses) { name ->
                        Text(name)
                    }
                }
                SnapclientList(snapclientList = snapclientsList)
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
            snapclientsList = listOf()
        )
    }
}

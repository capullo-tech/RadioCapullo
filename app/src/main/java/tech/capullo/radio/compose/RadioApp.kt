package tech.capullo.radio.compose

import android.content.res.Configuration
import android.os.Build
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import control.json.ServerStatus
import tech.capullo.radio.ui.theme.RadioTheme
import tech.capullo.radio.viewmodels.RadioViewModel

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun RadioApp(
    modifier: Modifier = Modifier,
    radioViewModel: RadioViewModel = viewModel()
) {
    val permissionList = mutableListOf(
        android.Manifest.permission.INTERNET,
        android.Manifest.permission.ACCESS_NETWORK_STATE,
        android.Manifest.permission.ACCESS_WIFI_STATE,
    )
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        permissionList.addAll(
            listOf(
                android.Manifest.permission.BLUETOOTH_SCAN,
                android.Manifest.permission.BLUETOOTH_ADVERTISE,
                android.Manifest.permission.BLUETOOTH_CONNECT
            )
        )
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        permissionList.add(android.Manifest.permission.NEARBY_WIFI_DEVICES)
    }

    Surface(
        modifier, color = MaterialTheme.colorScheme.background
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val multiplePermissionsState =
                rememberMultiplePermissionsState(permissions = permissionList)
            if (multiplePermissionsState.allPermissionsGranted) {
                RadioMainScreen(
                    deviceName = radioViewModel.getDeviceName(),
                    hostAddresses = radioViewModel.hostAddresses,
                    snapclientsList = radioViewModel.snapClientsList
                )
            } else {
                RadioPermission(multiplePermissionsState = multiplePermissionsState)
            }
        } else {
            RadioMainScreen(
                deviceName = radioViewModel.getDeviceName(),
                hostAddresses = radioViewModel.hostAddresses,
                snapclientsList = radioViewModel.snapClientsList
            )
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun RadioPermission(multiplePermissionsState: MultiplePermissionsState) {
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
@OptIn(ExperimentalPermissionsApi::class)
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
    snapclientsList: List<ServerStatus>
) {
    var text by remember { mutableStateOf("") }

    Column {
        Text("Radio Capullo")
        Text("Discoverable as: $deviceName")
        LazyColumn(modifier = Modifier.padding(vertical = 4.dp)) {
            items(items = hostAddresses) { name ->
                Text(name)
            }
        }
        SnapclientList(snapclientList = snapclientsList)
        Row {
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

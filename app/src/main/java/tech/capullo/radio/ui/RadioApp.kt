package tech.capullo.radio.ui

import android.Manifest
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import tech.capullo.radio.ui.theme.RadioTheme

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun RadioApp(onStartBroadcastingClicked: () -> Unit, onTuneInClicked: () -> Unit) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val multiplePermissionsState =
            rememberMultiplePermissionsState(
                permissions = listOf(
                    Manifest.permission.NEARBY_WIFI_DEVICES,
                    Manifest.permission.POST_NOTIFICATIONS,
                ),
            )
        if (multiplePermissionsState.allPermissionsGranted) {
            RadioMainScreen(
                onStartBroadcastingClicked = onStartBroadcastingClicked,
                onTuneInClicked = onTuneInClicked,
            )
        } else {
            // Launch the permission request
            LaunchedEffect(multiplePermissionsState) {
                multiplePermissionsState.launchMultiplePermissionRequest()
            }
        }
    } else {
        // For devices below TIRAMISU, show the main screen directly
        RadioMainScreen(
            onStartBroadcastingClicked = onStartBroadcastingClicked,
            onTuneInClicked = onTuneInClicked,
        )
    }
}

@Composable
fun RadioMainScreen(onStartBroadcastingClicked: () -> Unit, onTuneInClicked: () -> Unit) {
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            verticalArrangement = Arrangement.Center,
        ) {
            Button(
                onClick = onStartBroadcastingClicked,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                elevation = ButtonDefaults.buttonElevation(8.dp),
            ) {
                Text(
                    "RADIO-ON",
                    style = MaterialTheme.typography.displayLarge,
                    modifier = Modifier.padding(vertical = 24.dp),
                )
            }

            Spacer(modifier = Modifier.height(64.dp))
            Button(
                onClick = onTuneInClicked,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                elevation = ButtonDefaults.buttonElevation(8.dp),
            ) {
                Text(
                    "TUNE-IN",
                    style = MaterialTheme.typography.displayLarge,
                    modifier = Modifier.padding(vertical = 24.dp),
                )
            }
        }
    }
}

@Preview(
    showBackground = true,
    uiMode = UI_MODE_NIGHT_YES,
    name = "PreviewRadioAppDark",
    showSystemUi = true,
)
@Preview(
    showBackground = true,
    showSystemUi = true,
)
@Composable
fun RadioAppPreview() {
    RadioTheme {
        RadioMainScreen(
            onStartBroadcastingClicked = {},
            onTuneInClicked = {},
        )
    }
}

package tech.capullo.radio.compose

import android.content.res.Configuration
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import tech.capullo.radio.ui.theme.RadioTheme


@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun RadioApp(
    modifier: Modifier = Modifier,
    onStartBroadcastingClicked: () -> Unit,
    onTuneInClicked: () -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxSize(),
//        color = Color.DarkGray,
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val multiplePermissionsState =
                rememberMultiplePermissionsState(
                    permissions = listOf(
                        android.Manifest.permission.NEARBY_WIFI_DEVICES,
                        android.Manifest.permission.POST_NOTIFICATIONS,
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
}

@Composable
fun RadioMainScreen(onStartBroadcastingClicked: () -> Unit, onTuneInClicked: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Button(
                onClick = onStartBroadcastingClicked,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 8.dp),
                elevation = ButtonDefaults.buttonElevation(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Text("RADIO-ON", style = MaterialTheme.typography.displayLarge)
            }

            // Tune in Button with secondary color and fill all available space
            Button(
                onClick = onTuneInClicked,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 8.dp),
                elevation = ButtonDefaults.buttonElevation(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary,
                ),
            ) {
                Text("TUNE-IN", style = MaterialTheme.typography.displayLarge)
            }
        }
    }
}

@Preview(showBackground = true, name = "Light Mode")
@Composable
fun RadioAppPreview() {
    RadioTheme(darkTheme = false) {
        RadioMainScreen(
            onStartBroadcastingClicked = {},
            onTuneInClicked = {},
        )
    }
}

@Preview(
    showBackground = true,
    name = "Dark Mode",
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
fun RadioAppDarkPreview() {
    RadioTheme(darkTheme = true) {
        RadioMainScreen(
            onStartBroadcastingClicked = {},
            onTuneInClicked = {},
        )
    }
}
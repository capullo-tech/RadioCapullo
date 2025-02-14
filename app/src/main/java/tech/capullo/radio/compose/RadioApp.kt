package tech.capullo.radio.compose

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import tech.capullo.radio.ui.theme.primaryBlack
import tech.capullo.radio.ui.theme.secondaryOrange

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun RadioApp(
    modifier: Modifier = Modifier,
    onStartBroadcastingClicked: () -> Unit,
    onTuneInClicked: () -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = Color.DarkGray,
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
            .background(Color.DarkGray),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp), // Add padding to all sides of the column
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Start Broadcasting Button with primary color and fill all available space
            Button(
                onClick = onStartBroadcastingClicked,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f) // height equal space in the column
                    .padding(vertical = 8.dp), // vertical space between buttons
                elevation = ButtonDefaults.buttonElevation(8.dp), // Apply elevation for shadow
                colors = ButtonDefaults.buttonColors(
                    containerColor = primaryBlack,
                    contentColor = MaterialTheme.colorScheme.onSecondary,
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
                elevation = ButtonDefaults.buttonElevation(8.dp), // Apply elevation for shadow
                colors = ButtonDefaults.buttonColors(
                    containerColor = secondaryOrange,
                    contentColor = Color.Black,
                ),
            ) {
                Text("TUNE-IN", style = MaterialTheme.typography.displayLarge)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun RadioAppPreview() {
    RadioMainScreen(
        onStartBroadcastingClicked = {},
        onTuneInClicked = {},
    )
}
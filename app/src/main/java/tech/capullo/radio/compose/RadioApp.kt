package tech.capullo.radio.compose

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import tech.capullo.radio.ui.theme.*

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun RadioApp(
    modifier: Modifier = Modifier,
    onStartBroadcastingClicked: () -> Unit,
    onTuneInClicked: () -> Unit
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = Color.DarkGray
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
                    onStartBroadcastingClicked = onStartBroadcastingClicked,
                    onTuneInClicked = onTuneInClicked
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
                onTuneInClicked = onTuneInClicked
            )
        }
    }
}

@Composable
fun RadioMainScreen(
    onStartBroadcastingClicked: () -> Unit,
    onTuneInClicked: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.DarkGray),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp), // Add padding to all sides of the column
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Start Broadcasting Button with primary color and fill all available space
            Button(
                onClick = onStartBroadcastingClicked,
                modifier = Modifier
                    .fillMaxWidth() // Set button width to fill the screen
                    .weight(1f) // Set button height to take equal space in the column
                    .padding(vertical = 8.dp), // Add vertical padding to create space between buttons
                elevation = ButtonDefaults.buttonElevation(8.dp), // Apply elevation for shadow
                colors = ButtonDefaults.buttonColors(
                    containerColor = secondaryBlack, // Button background color (secondary color)
                    contentColor = MaterialTheme.colorScheme.onSecondary // Text color (onSecondary color)
                )
            ) {
                Text("RADIO-ON", style = MaterialTheme.typography.displayLarge)
            }

            // Tune in Button with secondary color and fill all available space
            Button(
                onClick = onTuneInClicked,
                modifier = Modifier
                    .fillMaxWidth() // Set button width to fill the screen
                    .weight(1f) // Set button height to take equal space in the column
                    .padding(vertical = 8.dp), // Add vertical padding to create space between buttons
                elevation = ButtonDefaults.buttonElevation(8.dp), // Apply elevation for shadow
                colors = ButtonDefaults.buttonColors(
                    containerColor = tertiaryOrange, // Button background color (tertiary color)
                    contentColor = Color.Black // Text color (onTertiary color)
                )
            ) {
                Text("TUNE-IN", style = MaterialTheme.typography.displayLarge)
            }
        }
    }
}

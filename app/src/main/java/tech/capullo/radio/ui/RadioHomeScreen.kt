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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons.Filled
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RichTooltip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import kotlinx.coroutines.launch
import tech.capullo.radio.ui.theme.RadioTheme
import tech.capullo.radio.ui.theme.SchemeChoice

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun RadioHomeScreen(onStartBroadcastingClicked: () -> Unit, onTuneInClicked: () -> Unit) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val multiplePermissionsState =
            rememberMultiplePermissionsState(
                permissions = listOf(
                    Manifest.permission.NEARBY_WIFI_DEVICES,
                    Manifest.permission.POST_NOTIFICATIONS,
                ),
            )
        if (multiplePermissionsState.allPermissionsGranted) {
            RadioHomeScreenContent(
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
        RadioHomeScreenContent(
            onStartBroadcastingClicked = onStartBroadcastingClicked,
            onTuneInClicked = onTuneInClicked,
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun RadioHomeScreenContent(onStartBroadcastingClicked: () -> Unit, onTuneInClicked: () -> Unit) {
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            Text(
                "Radio Capullo",
                style = MaterialTheme.typography.displayMediumEmphasized,
                modifier = Modifier.padding(start = 24.dp),
            )
            Text(
                "Broadcast music to other phones",
                fontSize = 28.sp,
                modifier = Modifier.padding(start = 24.dp),
            )

            Spacer(modifier = Modifier.weight(1f))

            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth(),
            ) {
                RadioTheme(
                    schemeChoice = SchemeChoice.GREEN,
                ) {
                    Button(
                        onClick = onStartBroadcastingClicked,
                        modifier = Modifier.width(320.dp),
                        elevation = ButtonDefaults.buttonElevation(2.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        ),
                    ) {
                        Text(
                            "RADIO-ON",
                            style = MaterialTheme.typography.displayLarge,
                            modifier = Modifier.padding(vertical = 24.dp),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(64.dp))

                Column(
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier.width(320.dp),
                ) {
                    RadioTheme(
                        schemeChoice = SchemeChoice.ORANGE,
                    ) {
                        Button(
                            onClick = onTuneInClicked,
                            modifier = Modifier.fillMaxWidth(),
                            elevation = ButtonDefaults.buttonElevation(4.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            ),
                        ) {
                            Text(
                                "TUNE-IN",
                                style = MaterialTheme.typography.displayLarge,
                                modifier = Modifier.padding(vertical = 24.dp),
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    HelpTooltip()
                }
            }
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpTooltip() {
    val scope = rememberCoroutineScope()
    val tooltipState = rememberTooltipState(isPersistent = true)
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
            positioning = TooltipAnchorPosition.Above,
            spacingBetweenTooltipAndAnchor = 8.dp,
        ),
        tooltip = {
            RichTooltip(
                title = {
                    Text("How to Use Radio Capullo")
                },
                action = {
                    TextButton(
                        onClick = {
                            scope.launch {
                                tooltipState.dismiss()
                            }
                        },
                    ) {
                        Text("Got it")
                    }
                },
            ) {
                Column {
                    Text("• Start broadcasting: Tap RADIO-ON to share your music")
                    Text("• Listen to broadcasts: Tap TUNE-IN to discover broadcasts from others")
                    Text("• Network: Ensure devices are on the same network")
                }
            }
        },
        state = tooltipState,
    ) {
        IconButton(
            onClick = { scope.launch { tooltipState.show() } },
            modifier = Modifier.size(56.dp),
        ) {
            Icon(
                imageVector = Filled.Info,
                contentDescription = "Help information",
                modifier = Modifier.size(32.dp),
            )
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
fun RadioHomeScreenPreview() {
    RadioTheme {
        RadioHomeScreenContent(
            onStartBroadcastingClicked = {},
            onTuneInClicked = {},
        )
    }
}

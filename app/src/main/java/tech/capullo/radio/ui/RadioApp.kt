package tech.capullo.radio.ui

import android.Manifest
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.Icons.Filled
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.twotone.MailOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.RichTooltip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import kotlinx.coroutines.launch
import tech.capullo.radio.R
import tech.capullo.radio.ui.theme.RadioTheme
import tech.capullo.radio.ui.theme.SchemeChoice

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
            //PlainTooltipExample()
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlainTooltipExample(
    modifier: Modifier = Modifier,
    plainTooltipText: String = "Add to favorites"
) {
    val tooltipState = rememberTooltipState()
    val scope = rememberCoroutineScope()
    Scaffold { innerPadding ->
        Column(
                    modifier = Modifier
                    .fillMaxSize()
                .padding(innerPadding),
        ) {
            Text(plainTooltipText)
            TooltipBox(
                modifier = modifier,
                positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                tooltip = {
                    PlainTooltip { Text(plainTooltipText) }
                },
                state = tooltipState
            ) {
                IconButton(onClick = { /* Do something... */ }) {
                    Icon(
                        imageVector = Icons.Filled.Favorite,
                        contentDescription = "Add to favorites"
                    )
                }
            }
            Spacer(Modifier.requiredHeight(30.dp))
            OutlinedButton(
                onClick = { scope.launch { tooltipState.show() } }
            ) {
                Text("Display tooltip")
            }



            val tooltipState2 = rememberTooltipState(
                isPersistent = true,
            )
            TooltipBox(
                positionProvider = TooltipDefaults.rememberRichTooltipPositionProvider(),
                tooltip = {
                    RichTooltip(
                        title = { Text(plainTooltipText) },
                        action = {
                            TextButton(
                                onClick = {
                                    scope.launch {
                                        tooltipState2.dismiss()
                                    }
                                }
                            ) { Text(plainTooltipText) }
                        }
                    ) { Text(plainTooltipText) }
                },
                state = tooltipState2
            ) {
                Icon(
                    imageVector = Icons.Filled.Info,
                    contentDescription = "Localized Description"
                )
            }
            Spacer(Modifier.requiredHeight(30.dp))
            OutlinedButton(
                onClick = { scope.launch { tooltipState2.show() } }
            ) {
                Text("Display tooltip")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun RadioMainScreen(onStartBroadcastingClicked: () -> Unit, onTuneInClicked: () -> Unit) {
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            //verticalArrangement = Arrangement.Center,
            //horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                //modifier = Modifier.weight(1f)
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
            }

            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Button(
                    onClick = {},
                    //contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
                ) {
                    Icon(
                        imageVector = Filled.Settings,
                        contentDescription = "somm",
                        modifier = Modifier.size(ButtonDefaults.IconSize)
                    )
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text(
                        "RADIO-ON",
                    )
                }

                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Button(
                        onClick = {},
                        contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = "somm",
                            modifier = Modifier.size(ButtonDefaults.IconSize)
                        )
                        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                        Text(
                            "Radio-on",
                        )
                    }

                    val scope = rememberCoroutineScope()
                    val plainTooltipText = "how to use Radio Capullo"
                    val tooltipState2 = rememberTooltipState(
                        isPersistent = true,
                    )
                    TooltipBox(
                        positionProvider = TooltipDefaults.rememberRichTooltipPositionProvider(),
                        tooltip = {
                            RichTooltip(
                                title = { Text(plainTooltipText) },
                                action = {
                                    TextButton(
                                        onClick = {
                                            scope.launch {
                                                tooltipState2.dismiss()
                                            }
                                        }
                                    ) { Text(plainTooltipText) }
                                }
                            ) { Text(plainTooltipText) }
                        },
                        state = tooltipState2
                    ) {
                        IconButton(
                            onClick = { scope.launch { tooltipState2.show() } },
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Info,
                                contentDescription = "Localized Description",
                            )
                        }
                    }
                }
            }
            /*
            RadioTheme(
                schemeChoice = SchemeChoice.GREEN,
            ) {
                Button(
                    onClick = onStartBroadcastingClicked,
                    modifier = Modifier.width(320.dp),
                    elevation = ButtonDefaults.buttonElevation(8.dp),
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
            RadioTheme(
                schemeChoice = SchemeChoice.ORANGE,
            ) {
                Button(
                    onClick = onTuneInClicked,
                    modifier = Modifier.width(320.dp),
                    elevation = ButtonDefaults.buttonElevation(8.dp),
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
             */
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

@Preview
@Composable
fun PP() {
    RadioTheme { PlainTooltipExample() }
}

package tech.capullo.radio

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import tech.capullo.radio.ui.RadioApp
import tech.capullo.radio.ui.RadioBroadcasterScreen
import tech.capullo.radio.ui.RadioTuneInScreen
import tech.capullo.radio.ui.theme.RadioTheme
import tech.capullo.radio.ui.theme.SchemeChoice
import tech.capullo.radio.viewmodels.AudioSettingsViewModel

@Composable
fun RadioCapulloNavHost(
    navController: NavHostController = rememberNavController(),
    startDestination: String = RadioDestinations.HOME_ROUTE,
) {
    val viewModel: AudioSettingsViewModel = hiltViewModel()
    var audioSettings by remember { mutableStateOf(viewModel.audioSettings) }

    NavHost(
        navController = navController,
        startDestination = startDestination,
    ) {
        composable(RadioDestinations.HOME_ROUTE) {
            val context = LocalContext.current
            RadioApp(
                audioSettings = audioSettings,
                onAudioSettingsChanged = { newSettings ->
                    val oldChannel = audioSettings.audioChannel
                    audioSettings = newSettings
                    viewModel.saveAudioSettings(newSettings)

                    // Restart snapclient if audio channel changed
                    if (oldChannel != newSettings.audioChannel) {
                        val intent = Intent(
                            context,
                            tech.capullo.radio.services.SnapclientService::class.java,
                        ).apply {
                            action = "RESTART_SNAPCLIENT"
                            putExtra("NEW_AUDIO_CHANNEL", newSettings.audioChannel.ordinal)
                        }
                        context.startService(intent)
                    }
                },
                onStartBroadcastingClicked = {
                    navController.navigate(RadioDestinations.BROADCAST_ROUTE)
                },
                onTuneInClicked = {
                    navController.navigate(RadioDestinations.TUNE_IN_ROUTE)
                },
            )
        }
        composable(RadioDestinations.BROADCAST_ROUTE) {
            val context = LocalContext.current
            RadioTheme(
                schemeChoice = SchemeChoice.GREEN,
            ) {
                RadioBroadcasterScreen(
                    audioSettings = audioSettings,
                    onAudioSettingsChanged = { newSettings ->
                        val oldChannel = audioSettings.audioChannel
                        audioSettings = newSettings
                        viewModel.saveAudioSettings(newSettings)

                        // Restart snapclient if audio channel changed
                        if (oldChannel != newSettings.audioChannel) {
                            val intent = Intent(
                                context,
                                tech.capullo.radio.services.SnapclientService::class.java,
                            ).apply {
                                action = "RESTART_SNAPCLIENT"
                                putExtra("NEW_AUDIO_CHANNEL", newSettings.audioChannel.ordinal)
                            }
                            context.startService(intent)
                        }
                    },
                )
            }
        }
        composable(RadioDestinations.TUNE_IN_ROUTE) {
            val context = LocalContext.current
            RadioTheme(
                schemeChoice = SchemeChoice.ORANGE,
            ) {
                RadioTuneInScreen(
                    audioSettings = audioSettings,
                    onAudioSettingsChanged = { newSettings ->
                        val oldChannel = audioSettings.audioChannel
                        audioSettings = newSettings
                        viewModel.saveAudioSettings(newSettings)

                        // Restart snapclient if audio channel changed
                        if (oldChannel != newSettings.audioChannel) {
                            val intent = Intent(
                                context,
                                tech.capullo.radio.services.SnapclientService::class.java,
                            ).apply {
                                action = "RESTART_SNAPCLIENT"
                                putExtra("NEW_AUDIO_CHANNEL", newSettings.audioChannel.ordinal)
                            }
                            context.startService(intent)
                        }
                    },
                )
            }
        }
    }
}

object RadioDestinations {
    const val HOME_ROUTE = "home"
    const val BROADCAST_ROUTE = "broadcast"
    const val TUNE_IN_ROUTE = "tune_in"
}

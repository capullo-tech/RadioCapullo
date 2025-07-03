package tech.capullo.radio

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import tech.capullo.radio.ui.RadioApp
import tech.capullo.radio.ui.RadioBroadcasterScreen
import tech.capullo.radio.ui.RadioTuneInScreen

@Composable
fun RadioCapulloNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    startDestination: String = RadioDestinations.HOME_ROUTE,
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
    ) {
        composable(RadioDestinations.HOME_ROUTE) {
            RadioApp(
                onStartBroadcastingClicked = {
                    navController.navigate(RadioDestinations.BROADCAST_ROUTE)
                },
                onTuneInClicked = {
                    navController.navigate(RadioDestinations.TUNE_IN_ROUTE)
                },
            )
        }
        composable(RadioDestinations.BROADCAST_ROUTE) {
            RadioBroadcasterScreen()
        }
        composable(RadioDestinations.TUNE_IN_ROUTE) {
            RadioTuneInScreen()
        }
    }
}

object RadioDestinations {
    const val HOME_ROUTE = "home"
    const val BROADCAST_ROUTE = "broadcast"
    const val TUNE_IN_ROUTE = "tune_in"
}

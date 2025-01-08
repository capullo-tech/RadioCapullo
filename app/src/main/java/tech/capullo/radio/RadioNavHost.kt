package tech.capullo.radio

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import tech.capullo.radio.compose.RadioApp
import tech.capullo.radio.compose.RadioBroadcasterScreen
import tech.capullo.radio.compose.RadioTuneInScreen

@Composable
fun RadioCapulloNavHost(
    navController: NavHostController = rememberNavController(),
    startDestination: String = RadioDestinations.HOME_ROUTE,
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
    ) {
        composable(RadioDestinations.HOME_ROUTE) {
            RadioApp(
                onStartBroadcastingClicked = {
                    navController.navigate(RadioDestinations.BROADCAST_ROUTE)
                },
                onTuneInClicked = {
                    navController.navigate(RadioDestinations.TUNEIN_ROUTE)
                },
            )
        }
        composable(RadioDestinations.BROADCAST_ROUTE) {
            RadioBroadcasterScreen()
        }
        composable(RadioDestinations.TUNEIN_ROUTE) {
            RadioTuneInScreen()
        }
    }
}

object RadioDestinations {
    const val HOME_ROUTE = "home"
    const val BROADCAST_ROUTE = "broadcast"
    const val TUNEIN_ROUTE = "tunein"
}

package tech.capullo.radio

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import tech.capullo.radio.ui.RadioApp
import tech.capullo.radio.ui.RadioBroadcasterScreen
import tech.capullo.radio.ui.RadioTuneInScreen
import tech.capullo.radio.ui.theme.RadioTheme
import tech.capullo.radio.ui.theme.SchemeChoice

data object Home
data object Broadcast
data object TuneIn

@Composable
fun RadioCapulloNavHost() {
    val backStack = remember { mutableStateListOf<Any>(Home) }

    NavDisplay(
        backStack = backStack,
        entryProvider = { key ->
            when (key) {
                is Home -> NavEntry(key) {
                    RadioApp(
                        onStartBroadcastingClicked = { backStack.add(Broadcast) },
                        onTuneInClicked = { backStack.add(TuneIn) },
                    )
                }

                is Broadcast -> NavEntry(key) {
                    RadioTheme(
                        schemeChoice = SchemeChoice.GREEN,
                    ) {
                        RadioBroadcasterScreen()
                    }
                }

                is TuneIn -> NavEntry(key) {
                    RadioTheme(
                        schemeChoice = SchemeChoice.ORANGE,
                    ) {
                        RadioTuneInScreen()
                    }
                }

                else -> NavEntry(Unit) {}
            }
        },
    )
}

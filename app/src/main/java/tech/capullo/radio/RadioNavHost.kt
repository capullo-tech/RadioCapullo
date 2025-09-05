package tech.capullo.radio

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import tech.capullo.radio.ui.BroadcasterScreen
import tech.capullo.radio.ui.NowPlayingScreen
import tech.capullo.radio.ui.RadioHomeScreen
import tech.capullo.radio.ui.TuneInScreen
import tech.capullo.radio.ui.theme.RadioTheme
import tech.capullo.radio.ui.theme.SchemeChoice

data object Home
data object Broadcast
data object TuneIn
data object NowPlaying

@Composable
fun RadioCapulloNavHost() {
    val backStack = remember { mutableStateListOf<Any>(Home) }

    NavDisplay(
        backStack = backStack,
        entryProvider = { key ->
            when (key) {
                is Home -> NavEntry(key) {
                    RadioHomeScreen(
                        onStartBroadcastingClicked = { backStack.add(Broadcast) },
                        onTuneInClicked = { backStack.add(TuneIn) },
                    )
                }

                is Broadcast -> NavEntry(key) {
                    RadioTheme(
                        schemeChoice = SchemeChoice.GREEN,
                    ) {
                        BroadcasterScreen()
                    }
                }

                is TuneIn -> NavEntry(key) {
                    RadioTheme(
                        schemeChoice = SchemeChoice.ORANGE,
                    ) {
                        TuneInScreen(
                            onConnected = { backStack.add(NowPlaying) },
                        )
                    }
                }

                is NowPlaying -> NavEntry(key) {
                    RadioTheme(
                        schemeChoice = SchemeChoice.ORANGE,
                    ) {
                        NowPlayingScreen()
                    }
                }

                else -> NavEntry(Unit) {}
            }
        },
    )
}

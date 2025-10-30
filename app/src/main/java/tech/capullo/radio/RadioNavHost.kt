package tech.capullo.radio

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import kotlinx.serialization.Serializable
import tech.capullo.radio.ui.BroadcasterScreen
import tech.capullo.radio.ui.EspotiSessionLoadingScreen
import tech.capullo.radio.ui.NowPlayingScreen
import tech.capullo.radio.ui.RadioHomeScreen
import tech.capullo.radio.ui.TuneInScreen
import tech.capullo.radio.ui.theme.RadioTheme
import tech.capullo.radio.ui.theme.SchemeChoice

@Serializable
private data object Home : NavKey

@Serializable
private data object EspotiSessionLoading : NavKey

@Serializable
private data object Broadcast : NavKey

@Serializable
private data object TuneIn : NavKey

@Serializable
private data object NowPlaying : NavKey

@Composable
fun RadioCapulloNavHost() {
    val backStack = rememberNavBackStack(Home)

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = entryProvider {
            entry<Home> {
                RadioHomeScreen(
                    onStartBroadcastingClicked = { backStack.add(EspotiSessionLoading) },
                    onTuneInClicked = { backStack.add(TuneIn) },
                )
            }

            entry<EspotiSessionLoading> {
                RadioTheme(
                    schemeChoice = SchemeChoice.GREEN,
                ) {
                    EspotiSessionLoadingScreen(
                        onPlayerReady = {
                            backStack.add(Broadcast)
                        },
                    )
                }
            }

            entry<Broadcast> {
                RadioTheme(
                    schemeChoice = SchemeChoice.GREEN,
                ) {
                    BroadcasterScreen()
                }
            }

            entry<TuneIn> {
                RadioTheme(
                    schemeChoice = SchemeChoice.ORANGE,
                ) {
                    TuneInScreen(
                        onConnected = { backStack.add(NowPlaying) },
                    )
                }
            }

            entry<NowPlaying> {
                RadioTheme(
                    schemeChoice = SchemeChoice.ORANGE,
                ) {
                    NowPlayingScreen()
                }
            }
        },
    )
}

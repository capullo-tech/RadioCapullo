package tech.capullo.radio

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.printToLog
import org.junit.Rule
import org.junit.Test
import tech.capullo.radio.snapcast.Client
import tech.capullo.radio.ui.BroadcasterScreenContent
import tech.capullo.radio.ui.model.AudioChannel
import tech.capullo.radio.viewmodels.BroadcasterUiState

class RadioBroadcasterEspotiConnectTest {

    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun whenEspotiConnectState_showsEspotiConnectScreen() {
        // Given: UI state is EspotiConnect
        val uiState = BroadcasterUiState.EspotiConnect(
            isLoading = false,
            deviceName = "Test Device",
        )

        // When: RadioBroadcasterScreen is displayed
        composeTestRule.setContent {
            BroadcasterScreenContent(
                uiState,
                onAudioChannelChange = { },
            )
        }
        composeTestRule.onRoot().printToLog("TAG")

        // Then: The EspotiConnect screen is displayed
        composeTestRule.onNodeWithText(
            "Connect to this device as a speaker on Spotify",
        ).assertIsDisplayed()

        val espotiConnectIcon = SemanticsMatcher.expectValue(
            SemanticsProperties.Role,
            Role.Image,
        ) and hasContentDescription("Espoti Connect Speaker Device")
        composeTestRule.onNode(espotiConnectIcon).assertIsDisplayed()

        composeTestRule.onNodeWithText("Test Device").assertIsDisplayed()
    }

    @Test
    fun whenLoadingPreviousPlaybackSessionState_showsLoadingIndicator() {
        // Given: UI state is EspotiConnect with loadingStoredCredentials = true
        val uiState = BroadcasterUiState.EspotiConnect(
            isLoading = true,
            deviceName = "Test Device",
        )

        // When: RadioBroadcasterScreen is displayed
        composeTestRule.setContent {
            BroadcasterScreenContent(
                uiState,
                onAudioChannelChange = { },
            )
        }
        composeTestRule.onRoot().printToLog("TAG")

        // Then: The loading indicator is displayed
        composeTestRule.onNodeWithText(
            "Checking for previous playback session...",
        ).assertIsDisplayed()
    }

    @Test
    fun whenPlayerReadyState_showsRabioBroadcasterPlaybackScreen() {
        // Given: UI state is EspotiPlayerReady
        val hostAddresses = listOf("192.168.0.1", "10.0.0.2")
        val mockClients = emptyList<Client>()

        val uiState = BroadcasterUiState.EspotiPlayerReady(
            hostAddresses = hostAddresses,
            snapcastClients = mockClients,
            audioChannel = AudioChannel.STEREO,
        )

        // When: Composable is displayed
        composeTestRule.setContent {
            BroadcasterScreenContent(
                uiState,
                onAudioChannelChange = { },
            )
        }

        // Then: Broadcaster host addresses are displayed
        composeTestRule.onNodeWithText("Host Addresses:").assertIsDisplayed()
        composeTestRule.onNodeWithText("192.168.0.1").assertIsDisplayed()
        composeTestRule.onNodeWithText("10.0.0.2").assertIsDisplayed()
    }
}

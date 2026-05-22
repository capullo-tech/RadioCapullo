package tech.capullo.radio

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.printToLog
import org.junit.Rule
import org.junit.Test
import tech.capullo.radio.data.RadioRepository.IPv4AddressesResult
import tech.capullo.radio.ui.BroadcasterScreenContent
import tech.capullo.radio.ui.EspotiSessionLoadingScreenContent
import tech.capullo.radio.ui.model.AudioChannel
import tech.capullo.radio.viewmodels.BroadcasterUiState
import tech.capullo.radio.viewmodels.EspotiSessionLoadingUiState

class RadioBroadcasterEspotiConnectTest {

    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun whenEspotiConnectState_showsEspotiConnectScreen() {
        // Given: Finished loading the stored session, but the player didn't load
        val uiState = EspotiSessionLoadingUiState(
            isLoading = false,
            isPlayerReady = false,
            deviceName = "Test Device",
        )

        // When: EspotiSessionLoadingScreen is displayed
        composeTestRule.setContent { EspotiSessionLoadingScreenContent(uiState) }
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
        // Given: UI state is loading the stored credentials
        val uiState = EspotiSessionLoadingUiState(
            isLoading = true,
            isPlayerReady = false,
            deviceName = "Test Device",
        )

        // When: EspotiSessionLoadingScreen is displayed
        composeTestRule.setContent { EspotiSessionLoadingScreenContent(uiState) }
        composeTestRule.onRoot().printToLog("TAG")

        // Then: The loading indicator is displayed
        composeTestRule.onNodeWithText(
            text = "Connecting to music source...",
        ).assertIsDisplayed()
    }

    @Test
    fun whenPlayerReadyState_showsRadioBroadcasterPlaybackScreen() {
        // Given: UI state is EspotiPlayerReady
        val ipv4AddressesResult: IPv4AddressesResult = IPv4AddressesResult.Success(
            listOf("192.168.0.1", "10.0.0.2"),
        )

        val uiState = BroadcasterUiState(
            ipv4AddressesResult = ipv4AddressesResult,
            audioChannel = AudioChannel.STEREO,
        )

        // When: Composable is displayed
        composeTestRule.setContent {
            BroadcasterScreenContent(
                uiState,
                onAudioChannelChange = { },
                onRefreshHostAddresses = { },
                groups = listOf(),
                onClientVolumeChange = { _, _, _ -> },
                onClientLatencyChange = { _, _ -> },
                onStreamControl = { },
            )
        }

        // Then: Broadcaster host addresses are displayed
        composeTestRule.onNodeWithText("Host Addresses:").assertIsDisplayed()
        composeTestRule.onNodeWithText("192.168.0.1").assertIsDisplayed()
        composeTestRule.onNodeWithText("10.0.0.2").assertIsDisplayed()

        val refreshIpAdressesButton = SemanticsMatcher.expectValue(
            SemanticsProperties.Role,
            Role.Button,
        ) and hasContentDescription("Refresh ip addresses")
        composeTestRule.onNode(refreshIpAdressesButton).assertIsEnabled()
    }
}

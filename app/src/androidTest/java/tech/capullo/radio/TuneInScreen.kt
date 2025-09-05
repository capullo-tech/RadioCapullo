package tech.capullo.radio

import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.printToLog
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import tech.capullo.radio.ui.TuneInScreenContent
import tech.capullo.radio.ui.model.AudioChannel

@RunWith(AndroidJUnit4::class)
class TuneInScreen {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testDefaultChannelSelection() {
        val lastServerText = "192.168.0.1"
        composeTestRule.setContent {
            TuneInScreenContent(
                lastServerText = lastServerText,
                onTextChange = {},
                onTuneInClick = {},
                isButtonEnabled = true,
                selectedChannel = AudioChannel.STEREO,
                onChannelChange = {},
            )
        }

        composeTestRule.onRoot().printToLog("TAG")

        // Assert Stereo channel is selected by default
        composeTestRule
            .onNodeWithText(AudioChannel.STEREO.label)
            .assertIsOn()
        composeTestRule
            .onNodeWithText(AudioChannel.LEFT.label)
            .assertIsOff()
        composeTestRule
            .onNodeWithText(AudioChannel.RIGHT.label)
            .assertIsOff()
    }

    @Test
    fun testSameChannelSelection() {
        val lastServerText = "192.168.0.1"
        composeTestRule.setContent {
            TuneInScreenContent(
                lastServerText = lastServerText,
                onTextChange = {},
                onTuneInClick = {},
                isButtonEnabled = false,
                selectedChannel = AudioChannel.STEREO,
                onChannelChange = { },
            )
        }

        AudioChannel.entries.forEach { channel ->
            // [LEFT, STEREO, RIGHT]
            // i.e. Press LEFT 10 times
            repeat(10) {
                composeTestRule
                    .onNodeWithText(channel.label)
                    .performClick()
                    .assertIsOn()
            }
            //  i.e assert STEREO and RIGHT remain unpressed
            AudioChannel.entries.filter { it != channel }.forEach { otherChannel ->
                composeTestRule
                    .onNodeWithText(otherChannel.label)
                    .assertIsOff()
            }
        }
    }

    @Test
    fun testChannelSelectionRestoration() {
        val lastServerText = "192.168.0.1"

        val restorationTester = StateRestorationTester(composeTestRule)
        restorationTester.setContent {
            TuneInScreenContent(
                lastServerText = lastServerText,
                onTextChange = {},
                onTuneInClick = {},
                isButtonEnabled = false,
                selectedChannel = AudioChannel.RIGHT,
                onChannelChange = { },
            )
        }

        repeat(10) {
            composeTestRule
                .onNodeWithText(AudioChannel.RIGHT.label)
                .performClick()
                .assertIsOn()
        }

        // Right should remain selected after recomposition
        restorationTester.emulateSavedInstanceStateRestore()
        composeTestRule
            .onNodeWithText(AudioChannel.RIGHT.label)
            .assertIsOn()
        composeTestRule
            .onNodeWithText(AudioChannel.LEFT.label)
            .assertIsOff()
        composeTestRule
            .onNodeWithText(AudioChannel.STEREO.label)
            .assertIsOff()
    }
}

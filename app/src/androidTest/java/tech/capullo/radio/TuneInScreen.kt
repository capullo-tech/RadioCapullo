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
import tech.capullo.radio.ui.AudioChannel
import tech.capullo.radio.ui.RadioTuneInScreenContent

@RunWith(AndroidJUnit4::class)
class TuneInScreen {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testDefaultChannelSelection() {
        val lastServerText = "192.168.0.1"
        val isTunedIn = false
        composeTestRule.setContent {
            RadioTuneInScreenContent(
                lastServerText = lastServerText,
                isTunedIn = isTunedIn,
                onTextChange = {},
                onTuneInClick = {},
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
        val isTunedIn = false
        composeTestRule.setContent {
            RadioTuneInScreenContent(
                lastServerText = lastServerText,
                isTunedIn = isTunedIn,
                onTextChange = {},
                onTuneInClick = {},
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
        val isTunedIn = false

        val restorationTester = StateRestorationTester(composeTestRule)
        restorationTester.setContent {
            RadioTuneInScreenContent(
                lastServerText = lastServerText,
                isTunedIn = isTunedIn,
                onTextChange = {},
                onTuneInClick = {},
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

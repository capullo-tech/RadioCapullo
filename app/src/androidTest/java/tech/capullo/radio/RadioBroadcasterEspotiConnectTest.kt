package tech.capullo.radio

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.printToLog
import org.junit.Rule
import org.junit.Test
import tech.capullo.radio.compose.RadioBroadcasterEspotiConnect

class RadioBroadcasterEspotiConnectTest {

    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun testRadioBroadcasterEspotiConnect() {
        composeTestRule.setContent {
            RadioBroadcasterEspotiConnect(
                deviceName = "Test Device",
            )
        }

        composeTestRule.onRoot().printToLog("TAG")
        composeTestRule.onNodeWithText("Test Device").assertExists()

        val espotiConnectIcon = SemanticsMatcher.expectValue(
            SemanticsProperties.Role,
            Role.Image,
        ) and hasContentDescription("Espoti Connect Speaker Device")
        composeTestRule.onNode(espotiConnectIcon).assertExists()
    }
}

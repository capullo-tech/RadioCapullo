package tech.capullo.radio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import dagger.hilt.android.AndroidEntryPoint
import tech.capullo.radio.compose.RadioApp
import tech.capullo.radio.ui.theme.RadioTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RadioTheme {
                RadioApp(modifier = Modifier.fillMaxSize())
            }
        }
    }
}

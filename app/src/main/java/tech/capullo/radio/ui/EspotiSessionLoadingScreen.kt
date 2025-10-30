package tech.capullo.radio.ui

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import tech.capullo.radio.R
import tech.capullo.radio.ui.theme.RadioTheme
import tech.capullo.radio.ui.theme.SchemeChoice
import tech.capullo.radio.ui.theme.Typography
import tech.capullo.radio.viewmodels.EspotiSessionLoadingUiState
import tech.capullo.radio.viewmodels.EspotiSessionLoadingViewModel

@Composable
fun EspotiSessionLoadingScreen(
    viewModel: EspotiSessionLoadingViewModel = hiltViewModel(),
    onPlayerReady: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) {
        viewModel.initialize()
    }

    LaunchedEffect(uiState.isPlayerReady) {
        if (uiState.isPlayerReady) {
            onPlayerReady()
        }
    }

    EspotiSessionLoadingScreenContent(uiState)
}

@Composable
fun EspotiSessionLoadingScreenContent(uiState: EspotiSessionLoadingUiState) {
    if (uiState.isLoading) {
        LoadingStoredSessionScreen()
    } else {
        EspotiConnectScreen(
            deviceName = uiState.deviceName,
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LoadingStoredSessionScreen() {
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            LoadingIndicator(
                modifier = Modifier.size(64.dp),
            )

            Spacer(modifier = Modifier.padding(16.dp))

            Text(
                text = "Connecting to music source...",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun EspotiConnectScreen(deviceName: String) {
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = """
                Connect to this device as a speaker on Spotify
                """.trimIndent(),
                style = Typography.headlineMedium,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.padding(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.google_home_devices_24px),
                    contentDescription = "Espoti Connect Speaker Device",
                )
                Text(
                    text = deviceName,
                )
            }
            Spacer(modifier = Modifier.padding(8.dp))
            LinearWavyProgressIndicator()
        }
    }
}

@Preview(
    showBackground = true,
    uiMode = UI_MODE_NIGHT_YES,
    name = "PreviewLoadingStoredSessionScreenDark",
    showSystemUi = true,
)
@Preview(
    showBackground = true,
    showSystemUi = true,
)
@Composable
fun PreviewLoadingStoredSessionScreen() {
    RadioTheme(schemeChoice = SchemeChoice.GREEN) {
        LoadingStoredSessionScreen()
    }
}

@Preview(
    showBackground = true,
    uiMode = UI_MODE_NIGHT_YES,
    name = "PreviewEspotiConnectScreenDark",
    showSystemUi = true,
)
@Preview(
    showBackground = true,
    showSystemUi = true,
)
@Composable
fun PreviewEspotiConnectScreen() {
    val deviceName = "Samsung Galaxy S21 Ultra Max"
    RadioTheme(schemeChoice = SchemeChoice.GREEN) {
        EspotiConnectScreen(deviceName)
    }
}

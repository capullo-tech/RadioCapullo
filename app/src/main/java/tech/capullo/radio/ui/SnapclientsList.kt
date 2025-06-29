package tech.capullo.radio.ui

import android.annotation.SuppressLint
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons.Filled
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import tech.capullo.radio.R
import tech.capullo.radio.snapcast.Client
import tech.capullo.radio.ui.theme.RadioTheme

@SuppressLint("SuspiciousIndentation")
@Composable
fun SnapclientList(snapclientList: List<Client>) {
    LazyColumn {
        items(snapclientList) { client ->
            SnapcastClientCard(
                name = client.host.name,
                volume = client.config.volume.percent.toFloat(),
            )
        }
    }
}

@Composable
private fun Greetings(
    modifier: Modifier = Modifier,
    // TODO a list of custom data object
    names: List<Pair<String, Float>> = listOf(Pair("localhost", 100f), Pair("Pixel 3a", 85f)),
) {
    LazyColumn(modifier = modifier.padding(vertical = 4.dp)) {
        items(items = names) { name ->
            name.first
            SnapcastClientCard(name = name.first, volume = name.second)
        }
    }
}

@Composable
private fun SnapcastClientCard(name: String, volume: Float) {
    Card(
        modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp),
    ) {
        SnapcastClientContent(name, volume)
    }
}

@Composable
private fun SnapcastClientContent(name: String, volume: Float) {
    val expanded by remember { mutableStateOf(true) }
    var progress by remember { mutableFloatStateOf(volume) }

    Row(
        modifier = Modifier
            .padding(12.dp)
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow,
                ),
            ),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(12.dp),
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                ),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = if (expanded) {
                        painterResource(id = R.drawable.volume_up_24px)
                    } else {
                        painterResource(id = R.drawable.volume_off_24px)
                    },
                    contentDescription = if (expanded) {
                        stringResource(R.string.app_name)
                    } else {
                        stringResource(R.string.app_name)
                    },
                )
                Slider(
                    value = progress,
                    onValueChange = {
                        progress = it
                    },
                    valueRange = 0f..100f,
                    // steps = 100,
                    modifier = Modifier.weight(2f),
                )
                Spacer(modifier = Modifier.height(16.dp))
                IconButton(onClick = { /*expanded = !expanded */ }) {
                    Icon(
                        imageVector = if (expanded) Filled.Settings else Filled.Settings,
                        contentDescription = if (expanded) {
                            stringResource(R.string.app_name)
                        } else {
                            stringResource(R.string.app_name)
                        },
                    )
                }
            }
        }
    }
}

@Preview(
    showBackground = true,
    widthDp = 320,
    uiMode = UI_MODE_NIGHT_YES,
    name = "DefaultPreviewDark",
)
@Preview(showBackground = true, widthDp = 320)
@Composable
fun DefaultPreview() {
    RadioTheme {
        Greetings()
    }
}

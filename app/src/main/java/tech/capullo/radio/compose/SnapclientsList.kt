package tech.capullo.radio.compose

import android.annotation.SuppressLint
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.util.Log
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.Icons.Filled
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.json.JSONObject
import tech.capullo.radio.R
import tech.capullo.radio.ui.theme.RadioTheme

@SuppressLint("SuspiciousIndentation")
@Composable
fun SnapclientList(
    snapclientList: List<String>,
    // modifier: Modifier = Modifier,
    // radioViewModel: RadioViewModel = hiltViewModel()
) {
    if (snapclientList.isNotEmpty()) {
        val serverStatus = JSONObject() // snapclientList.first().toJson()
        Log.i("SESSION", "serverStatus updated:$serverStatus")

        if (serverStatus.has("groups")) {
            val groups = serverStatus.getJSONArray("groups")
            Log.i("SESSION", "serverStatus key:${groups::class.java}")
            (0 until groups.length()).forEach { i ->

                // try {
                // Access each element in the array
                val element = groups.getJSONObject(i)
                Log.i("SESSION", "group :${element::class.java} -- $element")
                // Do something with the element
                val listOfsnapclients = mutableListOf<Pair<String, Float>>()
                if (element.has("clients")) {

                    val clients = element.getJSONArray("clients")
                    Log.i("SESSION", "client :${clients::class.java} -- $clients")
                    (0 until clients.length()).forEach { j ->
                        val client = clients.getJSONObject(j)
                        Log.i("SESSION", "each :${client::class.java} -- $client")
                        val volume =
                            client
                                .getJSONObject("config")
                                .getJSONObject("volume").get("percent")
                        Log.i("SESSION", "each :${volume::class.java} -- $volume")
                        // TODO: all this filtering
                        val name = client.getJSONObject("host").getString("name")
                        val vol =
                            client
                                .getJSONObject("config")
                                .getJSONObject("volume").getInt("percent")

                        listOfsnapclients.add(Pair(name, vol.toFloat()))
                        // Text(text = client.getJSONObject("host").getString("name"))
                    }
                    println(element.toString())
                }
                Greetings(names = listOfsnapclients)
                // } catch (e: JSONException) {
                // e.printStackTrace()
                // }
            }
        }
    }
}

@Composable
private fun Greetings(
    modifier: Modifier = Modifier,
    // TODO a list of custom data object
    names: List<Pair<String, Float>> = listOf(Pair("localhost", 100f), Pair("Pixel 3a", 85f))
) {
    LazyColumn(modifier = modifier.padding(vertical = 4.dp)) {
        items(items = names) { name ->
            name.first
            Greeting(name = name.first, volume = name.second)
        }
    }
}

@Composable
private fun Greeting(name: String, volume: Float) {
    Card(
        modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp)
    ) {
        CardContent(name, volume)
    }
}
@Composable
private fun CardContent(name: String, volume: Float) {
    val expanded by remember { mutableStateOf(true) }
    var progress by remember { mutableFloatStateOf(volume) }

    Row(
        modifier = Modifier
            .padding(12.dp)
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(12.dp)
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold
                )
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (expanded) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                    contentDescription = if (expanded) {
                        stringResource(R.string.app_name)
                    } else {
                        stringResource(R.string.app_name)
                    }
                )
                Slider(
                    value = progress,
                    onValueChange = {
                        progress = it
                    },
                    valueRange = 0f..100f,
                    // steps = 100,
                    modifier = Modifier.weight(2f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                IconButton(onClick = { /*expanded = !expanded */ }) {
                    Icon(
                        imageVector = if (expanded) Filled.Settings else Filled.Settings,
                        contentDescription = if (expanded) {
                            stringResource(R.string.app_name)
                        } else {
                            stringResource(R.string.app_name)
                        }
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
    name = "DefaultPreviewDark"
)
@Preview(showBackground = true, widthDp = 320)
@Composable
fun DefaultPreview() {
    RadioTheme {
        Greetings()
    }
}

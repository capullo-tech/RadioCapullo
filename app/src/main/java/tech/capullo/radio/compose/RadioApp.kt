package tech.capullo.radio.compose

import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import tech.capullo.radio.viewmodels.RadioViewModel

@Composable
fun RadioApp(
    modifier: Modifier = Modifier,
    radioViewModel: RadioViewModel = hiltViewModel()
) {
    Surface(
        modifier, color = MaterialTheme.colorScheme.background
    ) {
        Column {
            Text("Radio Capullo")
            Text("Discoverable as: ${getDeviceName()}")
            LazyColumn(modifier = Modifier.padding(vertical = 4.dp)) {
                items(items = radioViewModel.hostAddresses) { name ->
                    Text(name)
                }
            }
            SnapclientList(snapclientList = radioViewModel.snapClientsList)
        }
    }
}

fun getDeviceName(): String {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
        val deviceName = Settings.Global.DEVICE_NAME//Settings.Global.getString(appContext.contentResolver, Settings.Global.DEVICE_NAME)
        if (deviceName == Build.MODEL) Build.MODEL else "$deviceName (${Build.MODEL})"
    } else {
        Build.MODEL
    }
}

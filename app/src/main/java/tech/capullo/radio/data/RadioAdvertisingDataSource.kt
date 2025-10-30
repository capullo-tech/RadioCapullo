package tech.capullo.radio.data

import android.content.Context
import android.os.Build
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.NetworkInterface
import javax.inject.Inject

class RadioAdvertisingDataSource @Inject constructor(
    @ApplicationContext private val appContext: Context,
) {
    // TODO: injected dispatcher
    suspend fun getIPv4Addresses(): List<String> = withContext(Dispatchers.Default) {
        NetworkInterface.getNetworkInterfaces().toList().flatMap { networkInterface ->
            networkInterface.inetAddresses.toList().filter { inetAddress ->
                inetAddress.address.size == 4 && !inetAddress.isLoopbackAddress
            }.map { it.hostAddress }
        }
    }

    fun getDeviceName(): String = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
        val deviceName = Settings.Global.getString(
            appContext.contentResolver,
            Settings.Global.DEVICE_NAME,
        )
        if (deviceName == Build.MODEL) Build.MODEL else "$deviceName (${Build.MODEL})"
    } else {
        Build.MODEL
    }
}

package tech.capullo.radio.data

import android.content.Context
import android.os.Build
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import java.net.NetworkInterface
import java.util.Collections
import javax.inject.Inject

class RadioAdvertisingDataSource @Inject constructor(
    @ApplicationContext private val applicationContext: Context,
)
{
    fun getInetAddresses(): List<String> =
        Collections.list(NetworkInterface.getNetworkInterfaces()).flatMap { networkInterface ->
            Collections.list(networkInterface.inetAddresses).filter { inetAddress ->
                inetAddress.hostAddress != null && inetAddress.hostAddress?.takeIf {
                    it.indexOf(":") < 0 && !inetAddress.isLoopbackAddress
                }?.let { true } ?: false
            }.map { it.hostAddress!! }
        }

    fun getDeviceName(): String =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            val deviceName = Settings.Global.getString(
                applicationContext.contentResolver,
                Settings.Global.DEVICE_NAME
            )
            if (deviceName == Build.MODEL) Build.MODEL else "$deviceName (${Build.MODEL})"
        } else {
            Build.MODEL
        }
}
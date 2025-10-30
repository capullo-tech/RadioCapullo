package tech.capullo.radio

import android.content.Context
import android.net.nsd.NsdManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import tech.capullo.radio.data.ConfFileDataSource
import tech.capullo.radio.data.RadioAdvertisingDataSource
import tech.capullo.radio.data.RadioRepository
import tech.capullo.radio.snapcast.SnapserverDiscoveryManager
import tech.capullo.radio.snapcast.SnapserverNsdManager

@RunWith(AndroidJUnit4::class)
class SnapserverDiscoveryTest {

    @Test
    fun testDiscoversSnapserverServices() = runBlocking {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val confFileDataSource = ConfFileDataSource(appContext)
        val radioAdvertisingDataSource = RadioAdvertisingDataSource(appContext)
        val repository = RadioRepository(confFileDataSource, radioAdvertisingDataSource)
        val nsdManager = appContext.getSystemService(Context.NSD_SERVICE) as NsdManager

        launch {
            SnapserverNsdManager(nsdManager, repository).start()
        }

        val snapserverDiscoveryManager = SnapserverDiscoveryManager(nsdManager)
        snapserverDiscoveryManager.startDiscovery()

        delay(5000)
        println(snapserverDiscoveryManager.discoveredServices.value)
        assert(snapserverDiscoveryManager.discoveredServices.value.isNotEmpty())
    }
}

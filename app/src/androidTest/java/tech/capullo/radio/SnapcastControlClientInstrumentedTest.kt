package tech.capullo.radio

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import tech.capullo.radio.data.PipeFileDataSource
import tech.capullo.radio.data.RadioAdvertisingDataSource
import tech.capullo.radio.data.RadioRepository
import tech.capullo.radio.data.SnapcastControlClient
import tech.capullo.radio.snapcast.SnapclientProcess
import tech.capullo.radio.snapcast.SnapserverProcess

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class SnapcastControlClientInstrumentedTest {
    @Test
    fun useAppContext() = runBlocking {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("tech.capullo.radio", appContext.packageName)

        val pipeFileDataSource = PipeFileDataSource(appContext)
        val radioAdvertisingDataSource = RadioAdvertisingDataSource(appContext)
        val radioRepository = RadioRepository(pipeFileDataSource, radioAdvertisingDataSource)

        launch(Dispatchers.IO) {
            val snapserverProcess = SnapserverProcess(radioRepository)
            snapserverProcess.start()
        }

        launch(Dispatchers.IO) {
            val snapclientProcess = SnapclientProcess(appContext, radioRepository)
            snapclientProcess.start()
        }

        launch {
            val snapcastControlClient = SnapcastControlClient("127.0.0.1")
            snapcastControlClient.initWebsocket()
        }
        delay(10_000)
    }
}

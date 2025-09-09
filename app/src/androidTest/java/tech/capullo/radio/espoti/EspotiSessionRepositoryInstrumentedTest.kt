package tech.capullo.radio.espoti

import android.content.Context
import android.net.nsd.NsdManager
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import tech.capullo.radio.data.PipeFileDataSource
import tech.capullo.radio.data.RadioAdvertisingDataSource
import tech.capullo.radio.data.RadioRepository
import tech.capullo.radio.viewmodels.BroadcasterUiState
import tech.capullo.radio.viewmodels.BroadcasterViewModel
import java.io.File
import kotlin.system.measureTimeMillis

@RunWith(AndroidJUnit4::class)
class EspotiSessionRepositoryInstrumentedTest {

    companion object {
        const val TAG = "espotiSessionTest"
    }

    @Test
    fun testViewModelInitLoadingState() = runTest {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext

        // Repository init
        val pipeFileDataSource = PipeFileDataSource(appContext)
        val radioAdvertisingDataSource = RadioAdvertisingDataSource(appContext)
        val radioRepository = RadioRepository(
            pipeFileDataSource,
            radioAdvertisingDataSource,
        )
        val sessionRepository = EspotiSessionRepository(
            appContext,
            RadioRepository(pipeFileDataSource, radioAdvertisingDataSource),
        )

        // NSDManager init
        val nsdManager = appContext.getSystemService(Context.NSD_SERVICE) as NsdManager
        val espotiConnectHandler = EspotiConnectHandler(sessionRepository)
        val espotiZeroconfServer = EspotiZeroconfServer(espotiConnectHandler)
        val espotiNsdManager = EspotiNsdManager(nsdManager, espotiZeroconfServer)
        val viewModel = BroadcasterViewModel(
            appContext,
            radioRepository,
            espotiNsdManager,
            sessionRepository,
        )
        var resultState = viewModel.uiState.take(2).toList()
        assertEquals(
            BroadcasterUiState.EspotiConnect(
                isLoading = true,
                deviceName = radioAdvertisingDataSource.getDeviceName(),
            ),
            resultState[0],
        )

        val credentialsFile = File(appContext.filesDir, "espoti_creds")

        if (!credentialsFile.exists()) {
            assertEquals(
                BroadcasterUiState.EspotiConnect(
                    isLoading = false,
                    deviceName = radioAdvertisingDataSource.getDeviceName(),
                ),
                resultState[1],
            )
        }
    }

    @Test
    fun testPreviousSessionCreationPerformance() = runTest {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val pipeFileDataSource = PipeFileDataSource(appContext)
        val radioAdvertisingDataSource = RadioAdvertisingDataSource(appContext)
        val sessionRepository = EspotiSessionRepository(
            appContext,
            RadioRepository(pipeFileDataSource, radioAdvertisingDataSource),
        )

        // Ensure credentials file exists before running the test
        val credentialsFile = File(appContext.filesDir, "espoti_creds")

        if (!credentialsFile.exists()) {
            Log.w(TAG, "No stored credentials found. Test will likely fail.")
        }

        var resultState: EspotiSessionRepository.SessionState? = null

        val executionTime = measureTimeMillis {
            // Execute the method we want to measure
            sessionRepository.createSessionWithStoredCredentials()

            // Wait for and capture the resulting state
            resultState = sessionRepository.sessionState.first { it != null }
        }

        // Log the results
        Log.i(TAG, "Session creation took $executionTime ms")

        when (resultState) {
            is EspotiSessionRepository.SessionState.Created -> {
                Log.i(TAG, "Session created successfully")
            }

            is EspotiSessionRepository.SessionState.Error -> {
                Log.w(TAG, "Session creation failed: ${resultState.message}")
            }

            null -> {
                Log.e(TAG, "No state received - this shouldn't happen")
            }
        }
    }
}

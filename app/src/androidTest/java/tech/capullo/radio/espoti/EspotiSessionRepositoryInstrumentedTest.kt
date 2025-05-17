package tech.capullo.radio.espoti

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import tech.capullo.radio.data.PipeFileDataSource
import tech.capullo.radio.data.RadioAdvertisingDataSource
import tech.capullo.radio.data.RadioRepository
import java.io.File
import kotlin.system.measureTimeMillis

@RunWith(AndroidJUnit4::class)
class EspotiSessionRepositoryInstrumentedTest {

    companion object {
        const val TAG = "espotiSessionTest"
    }

    @Test
    fun testPreviousSessionCreationPerformance() = runTest {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val pipeFileDataSource = PipeFileDataSource(appContext)
        val radioAdvertisingDataSource = RadioAdvertisingDataSource(appContext)
        val repository = EspotiSessionRepository(
            appContext,
            RadioRepository(pipeFileDataSource, radioAdvertisingDataSource),
        )

        // Ensure credentials file exists before running the test
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val credentialsFile = File(context.filesDir, "espoti_creds")

        if (!credentialsFile.exists()) {
            Log.w(TAG, "No stored credentials found. Test will likely fail.")
        }

        var resultState: EspotiSessionRepository.SessionState? = null

        val executionTime = measureTimeMillis {
            // Execute the method we want to measure
            repository.createSessionWithStoredCredentials()

            // Wait for and capture the resulting state
            resultState = repository.sessionState.first { it != null }
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

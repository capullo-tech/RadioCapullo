package tech.capullo.radio

import android.content.Context
import android.net.nsd.NsdManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import tech.capullo.radio.data.ConfFileDataSource
import tech.capullo.radio.data.RadioAdvertisingDataSource
import tech.capullo.radio.data.RadioRepository
import tech.capullo.radio.espoti.EspotiConnectHandler
import tech.capullo.radio.espoti.EspotiNsdManager
import tech.capullo.radio.espoti.EspotiSessionRepository
import tech.capullo.radio.espoti.EspotiZeroconfServer
import tech.capullo.radio.viewmodels.EspotiSessionLoadingViewModel

@RunWith(AndroidJUnit4::class)
class SessionRepositoryTest {

    private lateinit var appContext: Context

    @Before
    fun setUp() {
        appContext = ApplicationProvider.getApplicationContext<MainApplication>()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testStopLoadingWhenStoredSessionFails() = runTest {
        // In this test, the sessionRepository won't contain a valid stored session file
        val dispatcher = StandardTestDispatcher(testScheduler)

        val confFileDataSource = ConfFileDataSource(appContext)
        val radioAdvertisingDataSource = RadioAdvertisingDataSource(appContext)
        val radioRepository = RadioRepository(confFileDataSource, radioAdvertisingDataSource)
        val espotiSessionRepository = EspotiSessionRepository(
            appContext,
            dispatcher,
            radioRepository,
        )

        val nsdManager = appContext.getSystemService(Context.NSD_SERVICE) as NsdManager
        val espotiConnectHandler = EspotiConnectHandler(espotiSessionRepository)
        val espotiZeroconfServer = EspotiZeroconfServer(espotiConnectHandler)
        val espotiNsdManager = EspotiNsdManager(
            nsdManager,
            espotiZeroconfServer,
        )
        val viewModel = EspotiSessionLoadingViewModel(
            appContext,
            espotiSessionRepository,
            espotiNsdManager = espotiNsdManager,
            ioDispatcher = dispatcher,
        )

        assert(viewModel.uiState.value.isLoading)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { }
        }

        viewModel.initialize()
        advanceUntilIdle()
        assert(!viewModel.uiState.value.isLoading)
    }
}

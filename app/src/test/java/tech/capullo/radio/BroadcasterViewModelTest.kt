package tech.capullo.radio

import android.content.Context
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import tech.capullo.radio.data.ConfFileDataSource
import tech.capullo.radio.data.RadioAdvertisingDataSource
import tech.capullo.radio.data.RadioRepository
import java.net.SocketException

class BroadcasterViewModelTest {

    @Test
    fun testGetIPv4AddressesSuccess() = runTest {
        val mockContext = mockk<Context>()

        val radioAdvertisingDataSource = RadioAdvertisingDataSource(mockContext)
        val confFileDataSource = mockk<ConfFileDataSource>()
        val radioRepository = RadioRepository(confFileDataSource, radioAdvertisingDataSource)

        val iPv4AddressesResult = radioRepository.getIPv4Addresses()

        assert(iPv4AddressesResult is RadioRepository.IPv4AddressesResult.Success)

        // If the cast were to fail (i.e. iPv4AddressesResult is not .Success)
        // the test would fail here as well
        (iPv4AddressesResult as RadioRepository.IPv4AddressesResult.Success).run {
            assert(addresses.isNotEmpty())
        }
    }

    @Test
    fun testGetIPv4AddressesError() = runTest {
        val socketExceptionTestMessage = "socketExceptionTestMessage "

        val radioAdvertisingDataSource = mockk<RadioAdvertisingDataSource>()
        coEvery { radioAdvertisingDataSource.getIPv4Addresses() } throws
            SocketException(socketExceptionTestMessage)

        val confFileDataSource = mockk<ConfFileDataSource>()
        val radioRepository = RadioRepository(confFileDataSource, radioAdvertisingDataSource)

        val iPv4AddressesResult = radioRepository.getIPv4Addresses()

        assert(iPv4AddressesResult is RadioRepository.IPv4AddressesResult.Error)
        assert(
            (iPv4AddressesResult as RadioRepository.IPv4AddressesResult.Error).message ==
                socketExceptionTestMessage,
        )
    }
}

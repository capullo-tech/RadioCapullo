package tech.capullo.radio.data

import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RadioRepository @Inject constructor(
    private val confFileDataSource: ConfFileDataSource,
    private val radioAdvertisingDataSource: RadioAdvertisingDataSource,
) {
    fun getPipeFilepath(): String? = confFileDataSource.getPipeFilepath()

    fun getAirplayPipeFilepath(): String? = confFileDataSource.getAirplayPipeFilepath()

    fun getAirplayMetadataPipeFilepath(): String? =
        confFileDataSource.getAirplayMetadataPipeFilepath()

    fun getShairportConfPath(pipeFilepath: String, metadataPipeFilepath: String? = null): String =
        confFileDataSource.getShairportConfPath(getDeviceName(), pipeFilepath, metadataPipeFilepath)

    fun getAirplayDeviceId(): String = radioAdvertisingDataSource.getAirplayDeviceId()

    fun getNativeLibDirPath(): String = confFileDataSource.getNativeLibDirPath()

    fun getCacheDir(): File = confFileDataSource.getCacheDir()

    fun getFilesDir(): File = confFileDataSource.getFilesDir()

    sealed class IPv4AddressesResult {
        object Loading : IPv4AddressesResult()
        data class Success(val addresses: List<String>) : IPv4AddressesResult()
        data class Error(val message: String?) : IPv4AddressesResult()
    }

    suspend fun getIPv4Addresses(): IPv4AddressesResult = try {
        IPv4AddressesResult.Success(radioAdvertisingDataSource.getIPv4Addresses())
    } catch (e: Exception) {
        IPv4AddressesResult.Error(e.message)
    }

    fun getDeviceName(): String = radioAdvertisingDataSource.getDeviceName()

    fun getSnapserverConfPath(): String = confFileDataSource.getSnapserverConfPath()
}

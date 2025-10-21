package tech.capullo.radio.data

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RadioRepository @Inject constructor(
    private val pipeFileDataSource: PipeFileDataSource,
    private val radioAdvertisingDataSource: RadioAdvertisingDataSource,
) {
    fun getPipeFilepath(): String? = pipeFileDataSource.getPipeFilepath()

    fun getNativeLibDirPath(): String = pipeFileDataSource.getNativeLibDirPath()

    fun getCacheDirPath(): String = pipeFileDataSource.getCacheDirPath()

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

    fun getSnapserverConfPath(): String = pipeFileDataSource.getSnapserverConfPath()
}

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

    fun getInetAddresses(): List<String> = radioAdvertisingDataSource.getInetAddresses()

    fun getDeviceName(): String = radioAdvertisingDataSource.getDeviceName()
}

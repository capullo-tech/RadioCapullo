package tech.capullo.radio.data

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RadioRepository @Inject constructor(
    private val pipeFileDataSource: PipeFileDataSource,
    private val radioAdvertisingDataSource: RadioAdvertisingDataSource,
) {
    fun getPipeFilepath(): String? {
        return pipeFileDataSource.getPipeFilepath()
    }

    fun getNativeLibDirPath(): String {
        return pipeFileDataSource.getNativeLibDirPath()
    }

    fun getCacheDirPath(): String {
        return pipeFileDataSource.getCacheDirPath()
    }

    fun getInetAddresses(): List<String> {
        return radioAdvertisingDataSource.getInetAddresses()
    }

    fun getDeviceName(): String {
        return radioAdvertisingDataSource.getDeviceName()
    }
}

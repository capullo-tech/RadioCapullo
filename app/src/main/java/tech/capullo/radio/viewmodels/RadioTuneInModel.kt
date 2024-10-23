package tech.capullo.radio.viewmodels

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.workDataOf
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import tech.capullo.radio.SnapcastProcessWorker
import javax.inject.Inject

@HiltViewModel
class RadioTuneInModel @Inject constructor(
    @ApplicationContext private val applicationContext: Context,
) : ViewModel() {

    private fun getSharedPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences("MyApp", Context.MODE_PRIVATE)
    }

    fun saveLastServerText(text: String) {
        val editor = getSharedPreferences(applicationContext).edit()
        editor.putString("my_text", text)
        editor.apply()
    }

    fun getLastServerText(): String {
        return getSharedPreferences(applicationContext).getString("my_text", "") ?: ""
    }

    fun initiateWorker(ip: String) {
        val uploadWorkRequest: WorkRequest =
            OneTimeWorkRequestBuilder<SnapcastProcessWorker>()
                .setInputData(
                    workDataOf(
                        "KEY_IP" to ip
                    )
                )
                .build()

        WorkManager
            .getInstance(applicationContext)
            .enqueue(uploadWorkRequest)
    }
}

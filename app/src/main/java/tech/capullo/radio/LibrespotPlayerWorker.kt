package tech.capullo.radio

import android.content.Context
import android.os.Process
import android.util.Log
import androidx.work.WorkerParameters
import androidx.work.multiprocess.RemoteCoroutineWorker

class LibrespotPlayerWorker(
    context: Context,
    parameters: WorkerParameters
) : RemoteCoroutineWorker(context, parameters) {
    override suspend fun doRemoteWork(): Result {
        val processId = Process.myPid()
        val threadId = Thread.currentThread().id
        val threadName = Thread.currentThread().name

        Log.d(TAG, "Starting ExampleRemoteCoroutineWorker - Process ID: $processId, Thread ID: $threadId, Thread Name: $threadName")

        // Do some work here

        return Result.success()
    }

    companion object {
        private const val TAG = "CAPULLOWORKER"
    }
}
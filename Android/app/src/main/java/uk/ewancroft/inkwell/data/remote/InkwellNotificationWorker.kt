package uk.ewancroft.inkwell.data.remote

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class InkwellNotificationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val notificationManager: InkwellNotificationManager,
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = try {
        notificationManager.pollForNewDocuments()
        Result.success()
    } catch (e: Exception) {
        Result.retry()
    }
}

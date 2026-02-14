package it.rfmariano.nstates.notifications

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import kotlin.math.max

object NextIssueNotificationScheduler {
    private const val UNIQUE_WORK_NAME = "next_issue_notification"

    fun schedule(context: Context, nextIssueTimeSeconds: Long) {
        if (nextIssueTimeSeconds <= 0L) return

        val nowSeconds = System.currentTimeMillis() / 1000
        val delaySeconds = max(0L, nextIssueTimeSeconds - nowSeconds)
        val request = OneTimeWorkRequestBuilder<NextIssueNotificationWorker>()
            .setInitialDelay(delaySeconds, TimeUnit.SECONDS)
            .addTag(UNIQUE_WORK_NAME)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(UNIQUE_WORK_NAME, ExistingWorkPolicy.REPLACE, request)
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK_NAME)
    }
}

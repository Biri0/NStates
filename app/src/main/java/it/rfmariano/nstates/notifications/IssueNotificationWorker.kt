package it.rfmariano.nstates.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import it.rfmariano.nstates.R
import it.rfmariano.nstates.data.local.AuthLocalDataSource
import it.rfmariano.nstates.data.local.SettingsDataSource
import it.rfmariano.nstates.data.repository.NationRepository
import kotlinx.coroutines.flow.first

@HiltWorker
class IssueNotificationWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val repository: NationRepository,
    private val settings: SettingsDataSource,
    private val authLocal: AuthLocalDataSource
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        if (!settings.issueNotificationsEnabled.first()) {
            return Result.success()
        }

        val enabledAccounts = settings.issueNotificationAccounts.first()
        if (enabledAccounts.isEmpty()) {
            return Result.success()
        }

        val accounts = authLocal.getAccounts()
        val accountsToCheck = accounts.filter { account ->
            enabledAccounts.contains(SettingsDataSource.normalizeNationKey(account.nationName))
        }

        if (accountsToCheck.isEmpty()) {
            return Result.success()
        }

        accountsToCheck.forEach { account ->
            val issuesResult = repository.fetchIssuesForAccount(
                nationName = account.nationName,
                pin = account.pin,
                autologin = account.autologin
            )
            issuesResult.onSuccess { data ->
                val lastCount = settings.getLastIssueCount(account.nationName)
                    ?: run {
                        settings.setLastIssueCount(account.nationName, data.issues.size)
                        return@onSuccess
                    }
                if (data.issues.size > lastCount) {
                    val newCount = data.issues.size - lastCount
                    postNotification(account.nationName, newCount)
                }
                settings.setLastIssueCount(account.nationName, data.issues.size)
            }
        }

        return Result.success()
    }

    private fun postNotification(nationName: String, newCount: Int) {
        ensureChannel()
        val title = "New issues for $nationName"
        val content = if (newCount == 1) {
            "1 new issue available"
        } else {
            "$newCount new issues available"
        }

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(applicationContext)
            .notify(notificationIdForNation(nationName), notification)
    }

    private fun notificationIdForNation(nationName: String): Int {
        return SettingsDataSource.normalizeNationKey(nationName).hashCode()
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE)
            as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT
        )
        manager.createNotificationChannel(channel)
    }

    companion object {
        const val CHANNEL_ID = "issues"
        const val CHANNEL_NAME = "Issue notifications"
    }
}

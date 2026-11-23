package pl.sekula.myvideofonlite.workers

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.flow.first
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import pl.sekula.myvideofonlite.domain.repository.DoorbellRepository
import pl.sekula.myvideofonlite.domain.repository.SettingsRepository
import pl.sekula.myvideofonlite.widget.WidgetUpdater
import java.util.concurrent.TimeUnit

/** Okresowe (min. 15 min – limit WorkManagera) odświeżenie statusu dla widgetu. Bez częstego pollingu. */
class StatusRefreshWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params), KoinComponent {

    private val repository: DoorbellRepository by inject()
    private val settings: SettingsRepository by inject()
    private val widgetUpdater: WidgetUpdater by inject()

    override suspend fun doWork(): Result {
        if (settings.settings.first().selectedDeviceId != null) {
            repository.refreshStatus()
        }
        widgetUpdater.update()
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "status-refresh"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<StatusRefreshWorker>(15, TimeUnit.MINUTES)
                .setConstraints(
                    Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build(),
                )
                .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}

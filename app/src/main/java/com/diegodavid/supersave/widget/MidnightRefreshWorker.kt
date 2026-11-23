package com.diegodavid.supersave.widget

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

/**
 * Redraws the widgets once a day, just after midnight. (WorkManager plumbing by Claude.)
 *
 * The widgets only redraw when data changes, so without this the summary would still
 * show last week's "left" on Monday morning until the app was opened. Redrawing also
 * runs the usual calculation: it freezes the new week (D3) and records payments that
 * came due (D30). WorkManager keeps the schedule after a reboot; Android may run it a
 * little late to save battery.
 */
class MidnightRefreshWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        refreshWidget(applicationContext)
        return Result.success()
    }

    companion object {
        private const val NAME = "midnight-widget-refresh"

        /** Called on every app start. KEEP: if it's already scheduled, nothing changes. */
        fun schedule(context: Context) {
            val now = LocalDateTime.now()
            val firstRun = now.toLocalDate().plusDays(1).atStartOfDay().plusMinutes(1)
            val request = PeriodicWorkRequestBuilder<MidnightRefreshWorker>(1, TimeUnit.DAYS)
                .setInitialDelay(Duration.between(now, firstRun).toMinutes(), TimeUnit.MINUTES)
                .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}

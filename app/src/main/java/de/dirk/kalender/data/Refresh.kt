package de.dirk.kalender.data

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import de.dirk.kalender.widget.AgendaWidget
import de.dirk.kalender.widget.ComboWidget
import de.dirk.kalender.widget.DateWidget
import de.dirk.kalender.widget.MonthWidget
import de.dirk.kalender.widget.WidgetKeys
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.TimeUnit

object WidgetUpdater {
    private val widgets: List<GlanceAppWidget> get() = listOf(MonthWidget(), AgendaWidget(), ComboWidget(), DateWidget())

    /** Explicitly restyle every placed widget with the current defaults. */
    suspend fun applyStyleToAll(ctx: Context) {
        val prefs = Prefs(ctx)
        val mgr = GlanceAppWidgetManager(ctx)
        for (w in widgets) {
            for (id in mgr.getGlanceIds(w.javaClass)) {
                updateAppWidgetState(ctx, id) { p ->
                    p[WidgetKeys.STYLE_MODE] = prefs.theme
                    p[WidgetKeys.STYLE_GLASS] = prefs.glass
                    p[WidgetKeys.STYLE_SET] = true
                }
                w.update(ctx, id)
            }
        }
    }

    /** Bumps the refresh token of every placed widget so it reloads its data. */
    suspend fun updateAll(ctx: Context, resetMonth: Boolean = false) {
        val mgr = GlanceAppWidgetManager(ctx)
        for (w in widgets) {
            for (id in mgr.getGlanceIds(w.javaClass)) {
                updateAppWidgetState(ctx, id) { p ->
                    p[WidgetKeys.TOKEN] = System.nanoTime()
                    if (resetMonth) p[WidgetKeys.MONTH_OFFSET] = 0
                }
                w.update(ctx, id)
            }
        }
    }
}

class RefreshWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        WeatherRepo.refresh(applicationContext)
        WidgetUpdater.updateAll(applicationContext, inputData.getBoolean("reset", false))
        return Result.success()
    }
}

/** Fires whenever the calendar provider changes, then re-arms itself. */
class CalendarChangeWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        WidgetUpdater.updateAll(applicationContext)
        Scheduler.armCalendarTrigger(applicationContext)
        return Result.success()
    }
}

object Scheduler {
    private const val MIDNIGHT = "de.dirk.kalender.MIDNIGHT"
    const val EVENT_END = "de.dirk.kalender.EVENT_END"

    /** One pending alarm (same request code) for the end of the next running/upcoming event. */
    fun armEventEnd(ctx: Context, at: Long) {
        val pi = PendingIntent.getBroadcast(
            ctx, 2, Intent(EVENT_END).setClass(ctx, SystemEventsReceiver::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        ctx.getSystemService(AlarmManager::class.java).setAndAllowWhileIdle(AlarmManager.RTC, at + 5_000, pi)
    }

    fun scheduleAll(ctx: Context) {
        val wm = WorkManager.getInstance(ctx)
        wm.enqueueUniquePeriodicWork(
            "refresh", ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<RefreshWorker>(30, TimeUnit.MINUTES).build()
        )
        armCalendarTrigger(ctx)
        armMidnight(ctx)
    }

    fun refreshNow(ctx: Context, resetMonth: Boolean = false) {
        WorkManager.getInstance(ctx).enqueueUniqueWork(
            "refresh_now", ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<RefreshWorker>().setInputData(workDataOf("reset" to resetMonth)).build()
        )
    }

    fun armCalendarTrigger(ctx: Context) {
        if (!CalendarRepo.hasPermission(ctx)) return
        val c = Constraints.Builder()
            .addContentUriTrigger(CalendarContract.CONTENT_URI, true)
            .setTriggerContentUpdateDelay(3, TimeUnit.SECONDS)
            .setTriggerContentMaxDelay(15, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(ctx).enqueueUniqueWork(
            "calendar_change", ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<CalendarChangeWorker>().setConstraints(c).build()
        )
    }

    fun armMidnight(ctx: Context) {
        val zone = ZoneId.systemDefault()
        val at = LocalDate.now(zone).plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() + 30_000
        val pi = PendingIntent.getBroadcast(
            ctx, 1, Intent(MIDNIGHT).setClass(ctx, SystemEventsReceiver::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        ctx.getSystemService(AlarmManager::class.java).setAndAllowWhileIdle(AlarmManager.RTC, at, pi)
    }
}

class SystemEventsReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        if (intent.action == Scheduler.EVENT_END) {
            Scheduler.refreshNow(ctx)
            return
        }
        Scheduler.scheduleAll(ctx)
        Scheduler.refreshNow(ctx, resetMonth = true)
    }
}

package de.dirk.kalender.widget

import android.content.Context
import de.dirk.kalender.data.CalendarRepo
import de.dirk.kalender.data.Ev
import de.dirk.kalender.data.Weather
import de.dirk.kalender.data.WeatherRepo
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters

/** [hadEvents]: the day had timed events that are already over (and therefore hidden). */
data class AgendaDay(val date: LocalDate, val events: List<Ev>, val hadEvents: Boolean = false)

data class Snapshot(
    val permission: Boolean,
    val today: LocalDate,
    val month: YearMonth,
    val gridStart: LocalDate,
    val counts: Map<LocalDate, Int>,
    val agenda: List<AgendaDay>,
    val weather: Weather?,
    val theme: Int = 0,
    val glass: Boolean = false,
) {
    companion object {
        const val GRID_DAYS = 42
        private const val AGENDA_DAYS = 8L

        fun load(ctx: Context, monthOffset: Int): Snapshot {
            val today = LocalDate.now()
            val month = YearMonth.from(today).plusMonths(monthOffset.toLong())
            val gridStart = month.atDay(1).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            val gridEnd = gridStart.plusDays(GRID_DAYS.toLong())
            val agendaEnd = today.plusDays(AGENDA_DAYS)

            val from = minOf(gridStart, today)
            val to = maxOf(gridEnd, agendaEnd)
            val events = CalendarRepo.events(ctx, from, to)

            val counts = HashMap<LocalDate, Int>()
            for (e in events) {
                var d = maxOf(e.startDate, from)
                while (!d.isAfter(e.lastDate) && d.isBefore(to)) {
                    counts[d] = (counts[d] ?: 0) + 1
                    d = d.plusDays(1)
                }
            }

            // Finished timed events are hidden; all-day events stay for the whole day.
            val now = System.currentTimeMillis()
            val agenda = (0 until AGENDA_DAYS).map { today.plusDays(it) }.map { day ->
                val all = events.filter { it.coversDate(day) }
                val open = all.filter { it.allDay || it.end > now }
                AgendaDay(day, open.sortedWith(compareBy({ !it.allDay }, { it.begin })), hadEvents = open.size < all.size)
            }.filterIndexed { i, d -> i < 2 || d.events.isNotEmpty() } // today + tomorrow always

            // Refresh the widgets right when the next visible event ends.
            events.filter { !it.allDay && it.end > now }.minOfOrNull { it.end }
                ?.let { de.dirk.kalender.data.Scheduler.armEventEnd(ctx, it) }

            return Snapshot(
                permission = CalendarRepo.hasPermission(ctx),
                today = today, month = month, gridStart = gridStart,
                counts = counts, agenda = agenda,
                weather = WeatherRepo.cached(ctx),
            )
        }
    }
}

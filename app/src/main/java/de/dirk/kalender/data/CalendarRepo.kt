package de.dirk.kalender.data

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract.Attendees
import android.provider.CalendarContract.Calendars
import android.provider.CalendarContract.Instances
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

data class CalInfo(val id: Long, val name: String, val account: String, val color: Int)

data class Ev(
    val eventId: Long,
    val title: String,
    val begin: Long,
    val end: Long,
    val allDay: Boolean,
    val color: Int,
    val location: String,
    val startDate: LocalDate,
    val lastDate: LocalDate, // inclusive
) {
    fun coversDate(d: LocalDate) = !d.isBefore(startDate) && !d.isAfter(lastDate)
}

object CalendarRepo {

    private val DEFAULT_NAMES = setOf("dirk", "arbeit")

    fun hasPermission(ctx: Context) =
        ctx.checkSelfPermission(Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED

    fun calendars(ctx: Context): List<CalInfo> {
        if (!hasPermission(ctx)) return emptyList()
        val out = mutableListOf<CalInfo>()
        ctx.contentResolver.query(
            Calendars.CONTENT_URI,
            arrayOf(Calendars._ID, Calendars.CALENDAR_DISPLAY_NAME, Calendars.ACCOUNT_NAME, Calendars.CALENDAR_COLOR),
            "${Calendars.VISIBLE} = 1", null,
            "${Calendars.ACCOUNT_NAME}, ${Calendars.CALENDAR_DISPLAY_NAME}"
        )?.use { c ->
            while (c.moveToNext()) {
                out += CalInfo(c.getLong(0), c.getString(1) ?: "?", c.getString(2) ?: "", c.getInt(3))
            }
        }
        return out
    }

    /** Selected calendar ids; applies defaults ("Dirk", "Arbeit", else all) on first use. */
    fun selectedIds(ctx: Context): Set<Long> {
        val prefs = Prefs(ctx)
        prefs.selectedCalendars?.let { return it }
        val all = calendars(ctx)
        if (all.isEmpty()) return emptySet()
        val defaults = all.filter { it.name.trim().lowercase() in DEFAULT_NAMES }.map { it.id }.toSet()
        val chosen = defaults.ifEmpty { all.map { it.id }.toSet() }
        prefs.selectedCalendars = chosen
        return chosen
    }

    /** All event instances overlapping [from, toExclusive) in local time. */
    fun events(ctx: Context, from: LocalDate, toExclusive: LocalDate): List<Ev> {
        if (!hasPermission(ctx)) return emptyList()
        val ids = selectedIds(ctx)
        if (ids.isEmpty()) return emptyList()
        val zone = ZoneId.systemDefault()
        // Widen the window by a day on both sides so all-day events (stored in UTC) are not cut off.
        val begin = from.minusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val end = toExclusive.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val uri = Instances.CONTENT_URI.buildUpon().also {
            ContentUris.appendId(it, begin); ContentUris.appendId(it, end)
        }.build()

        val out = mutableListOf<Ev>()
        ctx.contentResolver.query(
            uri,
            arrayOf(
                Instances.EVENT_ID, Instances.TITLE, Instances.BEGIN, Instances.END,
                Instances.ALL_DAY, Instances.DISPLAY_COLOR, Instances.EVENT_LOCATION,
                Instances.SELF_ATTENDEE_STATUS
            ),
            "${Instances.CALENDAR_ID} IN (${ids.joinToString(",")}) AND ${Instances.VISIBLE} = 1",
            null,
            "${Instances.ALL_DAY} DESC, ${Instances.BEGIN} ASC"
        )?.use { c ->
            while (c.moveToNext()) {
                if (c.getInt(7) == Attendees.ATTENDEE_STATUS_DECLINED) continue
                val b = c.getLong(2)
                val e = c.getLong(3)
                val allDay = c.getInt(4) == 1
                val (sd, ld) = if (allDay) {
                    val s = Instant.ofEpochMilli(b).atZone(ZoneOffset.UTC).toLocalDate()
                    val x = Instant.ofEpochMilli(e).atZone(ZoneOffset.UTC).toLocalDate()
                    s to (if (x.isAfter(s)) x.minusDays(1) else s)
                } else {
                    val s = Instant.ofEpochMilli(b).atZone(zone).toLocalDate()
                    val x = Instant.ofEpochMilli(maxOf(b, e - 1)).atZone(zone).toLocalDate()
                    s to x
                }
                if (ld.isBefore(from) || !sd.isBefore(toExclusive)) continue
                out += Ev(
                    eventId = c.getLong(0),
                    title = c.getString(1)?.takeIf { it.isNotBlank() } ?: "(Ohne Titel)",
                    begin = b, end = e, allDay = allDay,
                    color = c.getInt(5),
                    location = c.getString(6) ?: "",
                    startDate = sd, lastDate = ld,
                )
            }
        }
        return out
    }
}

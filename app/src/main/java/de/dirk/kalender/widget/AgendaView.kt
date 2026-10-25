package de.dirk.kalender.widget

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import de.dirk.kalender.data.Ev
import de.dirk.kalender.data.WeatherRepo
import java.time.LocalDate

/**
 * Agenda: today and tomorrow always, following days only when they have events.
 * Fills the available height (estimated per line) and ends with "+N weitere".
 */
private class Plan(val days: List<Pair<AgendaDay, List<Ev>>>, val hidden: Int)

private fun plan(s: Snapshot, height: Float, compact: Boolean): Plan {
    val headerH = 26f; val pillH = 24f; val rowH = if (compact) 24f else 42f; val emptyH = 18f
    var budget = height
    val out = mutableListOf<Pair<AgendaDay, List<Ev>>>()
    var hidden = 0
    for ((i, day) in s.agenda.withIndex()) {
        if (out.size >= 6 || budget < headerH + minOf(rowH, pillH)) { hidden += day.events.size; continue }
        val shown = mutableListOf<Ev>()
        // Keep room for tomorrow's header + first line while filling today.
        val reserve = if (i == 0 && s.agenda.size > 1) headerH + (if (s.agenda[1].events.isEmpty()) emptyH else rowH) else 0f
        var b = budget - headerH - reserve - (if (day.events.isEmpty()) emptyH else 0f)
        for (e in day.events) {
            val need = if (e.allDay) pillH else rowH
            if (b >= need && shown.size < 8) { shown += e; b -= need } else hidden++
        }
        if (i >= 2 && shown.isEmpty()) continue
        out += day to shown
        budget = b + reserve
    }
    return Plan(out, hidden)
}

/**
 * Agenda: today and tomorrow always, following days only when they have events.
 * Fills the available height; switches to one-line rows when space is tight.
 * Footer row holds "+N weitere" and the + button.
 */
@Composable
fun Agenda(s: Snapshot, w: Dp, h: Dp, forceCompact: Boolean) {
    val wide = w >= 260.dp
    val footer = 34.dp
    val avail = (h - footer).value
    var compact = forceCompact
    var p = plan(s, avail, compact)
    if (!compact && p.hidden > 0) { compact = true; p = plan(s, avail, true) }

    Column(modifier = GlanceModifier.width(w).height(h)) {
        Column(modifier = GlanceModifier.fillMaxWidth().defaultWeight()) {
            p.days.forEach { (day, events) ->
                Column(modifier = GlanceModifier.fillMaxWidth()) {
                    DayHeader(s, day.date, s.agenda.indexOf(day), wide, w)
                    if (day.events.isEmpty()) Text(
                        if (day.hadEvents) "Keine weiteren Termine" else "Keine Termine",
                        modifier = GlanceModifier.padding(start = 2.dp, bottom = 2.dp),
                        style = TextStyle(color = C.muted, fontSize = 12.sp)
                    )
                    events.forEach { e -> if (e.allDay) AllDayPill(e) else TimedRow(e, day.date, compact, false) }
                }
            }
        }
        Row(modifier = GlanceModifier.fillMaxWidth().height(footer), verticalAlignment = Alignment.Bottom) {
            if (p.hidden > 0) Text(
                "+${p.hidden} weitere",
                modifier = GlanceModifier.padding(bottom = 6.dp).clickable(actionStartActivity(Intents.day(s.today))),
                style = TextStyle(color = C.muted, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            )
            Spacer(GlanceModifier.defaultWeight())
            AddButton()
        }
    }
}

@Composable
private fun DayHeader(s: Snapshot, day: LocalDate, index: Int, wide: Boolean, w: Dp) {
    val (label, color) = when (index) {
        0 -> "HEUTE" to C.blue
        1 -> "MORGEN" to C.text
        else -> Fmt.weekdayShort(day.dayOfWeek).uppercase() to C.text
    }
    val sub = when {
        index >= 2 -> Fmt.dayMonth(day)
        wide -> "${Fmt.weekdayShort(day.dayOfWeek)} ${Fmt.dayMonth(day)}"
        else -> null
    }
    Row(
        modifier = GlanceModifier.fillMaxWidth().padding(top = if (index == 0) 0.dp else 6.dp, bottom = 3.dp)
            .clickable(actionStartActivity(Intents.day(day))),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = TextStyle(color = color, fontSize = 14.sp, fontWeight = FontWeight.Bold))
        if (sub != null) {
            Spacer(GlanceModifier.width(6.dp))
            Text(sub, style = TextStyle(color = C.muted, fontSize = 13.sp, fontWeight = FontWeight.Medium))
        }
        Spacer(GlanceModifier.defaultWeight())
        val weather = s.weather
        val wx = weather?.days?.get(day)
        if (weather != null && wx != null && w >= 150.dp) {
            Text("${wx.max}°/${wx.min}°", style = TextStyle(color = C.text, fontSize = 13.sp, fontWeight = FontWeight.Bold))
            Spacer(GlanceModifier.width(5.dp))
            // Today after sunset: show the night variant (moon) like the original.
            val dayIcon = index != 0 || weather.isDay
            Image(ImageProvider(WeatherRepo.icon(wx.code, dayIcon)), "Wetter", GlanceModifier.size(18.dp))
        }
    }
}

@Composable
private fun AllDayPill(e: Ev) {
    Box(modifier = GlanceModifier.fillMaxWidth().padding(bottom = 3.dp)) {
        Text(
            e.title,
            maxLines = 1,
            modifier = GlanceModifier.background(C.of(e.color)).cornerRadius(6.dp)
                .padding(horizontal = 8.dp, vertical = 3.dp)
                .clickable(actionStartActivity(Intents.event(e))),
            style = TextStyle(color = C.of(onColor(e.color)), fontSize = 13.sp, fontWeight = FontWeight.Medium)
        )
    }
}

@Composable
private fun TimedRow(e: Ev, day: LocalDate, compact: Boolean, past: Boolean) {
    val main = if (past) C.muted else C.text
    Row(
        modifier = GlanceModifier.fillMaxWidth().padding(vertical = 2.dp).clickable(actionStartActivity(Intents.event(e))),
        verticalAlignment = if (compact) Alignment.CenterVertically else Alignment.Top
    ) {
        Box(modifier = GlanceModifier.padding(top = if (compact) 0.dp else 5.dp)) {
            Box(modifier = GlanceModifier.size(9.dp).background(C.of(e.color)).cornerRadius(5.dp)) {}
        }
        Spacer(GlanceModifier.width(7.dp))
        if (compact) {
            Text(Fmt.start(e, day), style = TextStyle(color = C.muted, fontSize = 12.sp, fontWeight = FontWeight.Medium))
            Spacer(GlanceModifier.width(6.dp))
            Text(e.title, maxLines = 1, style = TextStyle(color = main, fontSize = 13.sp))
        } else {
            Column(modifier = GlanceModifier.defaultWeight()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(Fmt.timeRange(e, day), maxLines = 1, style = TextStyle(color = main, fontSize = 12.sp, fontWeight = FontWeight.Medium))
                    if (e.location.isNotBlank()) {
                        Spacer(GlanceModifier.width(6.dp))
                        Text(e.location, maxLines = 1, style = TextStyle(color = C.muted, fontSize = 12.sp))
                    }
                }
                Text(e.title, maxLines = 1, style = TextStyle(color = main, fontSize = 14.sp))
            }
        }
    }
}

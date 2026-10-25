package de.dirk.kalender.widget

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.coerceIn
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
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
import androidx.glance.layout.RowScope
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontFamily
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import de.dirk.kalender.R
import de.dirk.kalender.data.WeatherRepo
import java.time.LocalDate
import java.time.YearMonth

/** Month grid with weekday labels; optional "‹ Okt 2026 ›" header with navigation. */
@Composable
fun MonthGrid(s: Snapshot, w: Dp, h: Dp, kind: String, header: Boolean) {
    val headerH = if (header) (h * 0.13f).coerceIn(24.dp, 34.dp) else 0.dp
    val labelH = (h * 0.085f).coerceIn(14.dp, 24.dp)
    val cellH = (h - headerH - labelH) / 6
    val fs = (minOf(cellH.value * 0.44f, w.value / 7 * 0.40f)).coerceIn(9f, 20f).sp
    val radius = (cellH * 0.28f).coerceIn(4.dp, 10.dp)

    Column(modifier = GlanceModifier.width(w).height(h)) {
        if (header) MonthHeader(s, kind, headerH, fs)
        Row(modifier = GlanceModifier.fillMaxWidth().height(labelH), verticalAlignment = Alignment.CenterVertically) {
            Fmt.weekdays.forEachIndexed { i, wd ->
                val isTodayCol = s.today.dayOfWeek.value - 1 == i && YearMonth.from(s.today) == s.month
                Text(
                    wd, modifier = GlanceModifier.defaultWeight(),
                    style = TextStyle(
                        color = if (isTodayCol) C.blue else C.muted,
                        fontSize = (fs.value * 0.62f).sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center
                    )
                )
            }
        }
        for (week in 0 until 6) {
            val start = s.gridStart.plusDays(week * 7L)
            val current = !s.today.isBefore(start) && s.today.isBefore(start.plusDays(7))
            val rowMod = GlanceModifier.fillMaxWidth().height(cellH)
            Row(
                modifier = if (current) rowMod.background(C.week).cornerRadius(radius) else rowMod,
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (d in 0 until 7) DayCell(start.plusDays(d.toLong()), s, fs, radius)
            }
        }
    }
}

@Composable
private fun RowScope.DayCell(date: LocalDate, s: Snapshot, fs: TextUnit, radius: Dp) {
    val inMonth = YearMonth.from(date) == s.month
    val isToday = date == s.today
    val bg = if (isToday) C.blue else C.heat(s.counts[date] ?: 0)
    val fg = when {
        isToday -> C.white
        !inMonth -> C.faint
        else -> C.text
    }
    Box(modifier = GlanceModifier.defaultWeight().fillMaxHeight().padding(1.5.dp)) {
        val inner = GlanceModifier.fillMaxSize().clickable(actionStartActivity(Intents.day(date)))
        Box(
            modifier = if (bg != null) inner.background(bg).cornerRadius(radius) else inner,
            contentAlignment = Alignment.Center
        ) {
            Text(
                date.dayOfMonth.toString(),
                style = TextStyle(
                    color = fg, fontSize = fs, textAlign = TextAlign.Center,
                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
                )
            )
        }
    }
}

@Composable
private fun MonthHeader(s: Snapshot, kind: String, h: Dp, fs: TextUnit) {
    val title = (fs.value * 0.95f).coerceIn(12f, 18f).sp
    val icon = (h * 0.62f).coerceIn(14.dp, 20.dp)
    Row(modifier = GlanceModifier.fillMaxWidth().height(h), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = GlanceModifier.size(h).clickable(navAction(kind, -1)), contentAlignment = Alignment.Center) {
            Image(ImageProvider(R.drawable.ic_chevron_left), "Vorheriger Monat", GlanceModifier.size(icon), colorFilter = ColorFilter.tint(C.text))
        }
        Row(
            modifier = GlanceModifier.defaultWeight().fillMaxHeight().clickable(navAction(kind, 0)),
            horizontalAlignment = Alignment.CenterHorizontally, verticalAlignment = Alignment.CenterVertically
        ) {
            Text(Fmt.monthShort(s.month) + " ", style = TextStyle(color = C.text, fontSize = title, fontWeight = FontWeight.Bold))
            Text(s.month.year.toString(), style = TextStyle(color = C.red, fontSize = title, fontWeight = FontWeight.Bold))
        }
        Box(modifier = GlanceModifier.size(h).clickable(navAction(kind, 1)), contentAlignment = Alignment.Center) {
            Image(ImageProvider(R.drawable.ic_chevron_right), "Nächster Monat", GlanceModifier.size(icon), colorFilter = ColorFilter.tint(C.text))
        }
    }
}

/** Big "23 | OKTOBER / Freitag | 14° ☀" header of the large month widget. */
@Composable
fun DateHeader(s: Snapshot, w: Dp, h: Dp) {
    val big = (h.value * 0.78f).coerceIn(36f, 84f).sp
    val small = (h.value * 0.2f).coerceIn(12f, 17f).sp
    val day = (h.value * 0.3f).coerceIn(17f, 28f).sp
    Row(
        modifier = GlanceModifier.width(w).height(h).clickable(actionStartActivity(Intents.day(s.today))),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            s.today.dayOfMonth.toString(),
            style = TextStyle(color = C.text, fontSize = big, fontFamily = FontFamily("sans-serif-light"))
        )
        Spacer(GlanceModifier.width(14.dp))
        Column {
            Text(Fmt.monthLong(s.today).uppercase(), style = TextStyle(color = C.muted, fontSize = small, fontWeight = FontWeight.Medium))
            Text(Fmt.weekdayLong(s.today), style = TextStyle(color = C.red, fontSize = day, fontWeight = FontWeight.Bold))
        }
        Spacer(GlanceModifier.defaultWeight())
        s.weather?.let { wx ->
            Text("${wx.now}°", style = TextStyle(color = C.text, fontSize = day, fontWeight = FontWeight.Medium))
            Spacer(GlanceModifier.width(6.dp))
            Image(
                ImageProvider(WeatherRepo.icon(wx.nowCode, wx.isDay)), "Wetter",
                GlanceModifier.size((h * 0.42f).coerceIn(22.dp, 36.dp))
            )
        }
    }
}

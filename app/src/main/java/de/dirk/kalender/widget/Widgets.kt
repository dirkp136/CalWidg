package de.dirk.kalender.widget

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.coerceIn
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.layout.size
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.text.FontFamily
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import java.time.YearMonth
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width

private val PAD = 12.dp

class MonthWidget : BaseWidget() {
    override val kind = "month"

    @Composable
    override fun Content(s: Snapshot) {
        if (!s.permission) return NoPermission()
        val size = LocalSize.current
        val w = size.width - PAD * 2
        val h = size.height - PAD * 2
        val large = size.height >= 250.dp && size.width >= 220.dp
        Box(modifier = rootModifier.padding(PAD)) {
            if (large) {
                val headH = (h * 0.24f).coerceIn(56.dp, 110.dp)
                Column {
                    DateHeader(s, w, headH)
                    Spacer(GlanceModifier.height(6.dp))
                    MonthGrid(s, w, h - headH - 6.dp, kind, header = false)
                }
            } else {
                MonthGrid(s, w, h, kind, header = true)
            }
        }
    }
}

class AgendaWidget : BaseWidget() {
    override val kind = "agenda"

    @Composable
    override fun Content(s: Snapshot) {
        if (!s.permission) return NoPermission()
        val size = LocalSize.current
        val pad = 14.dp
        AgendaBox(s, size.width - pad * 2, size.height - pad * 2, pad)
    }
}

class ComboWidget : BaseWidget() {
    override val kind = "combo"

    @Composable
    override fun Content(s: Snapshot) {
        if (!s.permission) return NoPermission()
        val size = LocalSize.current
        val w = size.width - PAD * 2
        val h = size.height - PAD * 2
        val monthW = (w * 0.48f).coerceIn(140.dp, 360.dp)
        Box(modifier = rootModifier.padding(PAD)) {
            Row(modifier = GlanceModifier.fillMaxSize()) {
                MonthGrid(s, monthW, h, kind, header = true)
                Spacer(GlanceModifier.width(14.dp))
                Agenda(s, w - monthW - 14.dp, h, forceCompact = false)
            }
        }
    }
}

@Composable
private fun AgendaBox(s: Snapshot, w: Dp, h: Dp, pad: Dp) {
    Box(modifier = rootModifier.padding(pad)) {
        Agenda(s, w, h, forceCompact = false)
    }
}

class MonthWidgetReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget = MonthWidget() }
class AgendaWidgetReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget = AgendaWidget() }
class ComboWidgetReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget = ComboWidget() }

/** App-icon style date tile: "So Okt" over a large day number. Needs no calendar permission. */
class DateWidget : BaseWidget() {
    override val kind = "date"

    @Composable
    override fun Content(s: Snapshot) {
        val size = LocalSize.current
        val side = minOf(size.width, size.height)
        val head = (side.value * 0.17f).coerceIn(10f, 34f).sp
        Box(
            modifier = rootModifier.clickable(actionStartActivity(Intents.day(s.today))),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(Fmt.weekdayShort(s.today.dayOfWeek), style = TextStyle(color = C.red, fontSize = head, fontWeight = FontWeight.Bold))
                    Spacer(GlanceModifier.width((side.value * 0.035f).dp))
                    Text(Fmt.monthShort(YearMonth.from(s.today)), style = TextStyle(color = C.muted, fontSize = head, fontWeight = FontWeight.Medium))
                }
                val ctx = LocalContext.current
                val density = ctx.resources.displayMetrics.density
                val glyphDp = side.value * 0.44f
                val bmp = BigText.render(ctx, s.today.dayOfMonth.toString(), (glyphDp * density).toInt())
                Spacer(GlanceModifier.height((side.value * 0.04f).dp))
                Image(
                    ImageProvider(bmp), contentDescription = s.today.dayOfMonth.toString(),
                    modifier = GlanceModifier.size((bmp.width / density).dp, (bmp.height / density).dp),
                    colorFilter = ColorFilter.tint(C.text)
                )
            }
        }
    }
}

class DateWidgetReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget = DateWidget() }

package de.dirk.kalender.widget

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.graphics.Color as AColor
import android.provider.CalendarContract
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.size
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import de.dirk.kalender.R
import de.dirk.kalender.data.Ev
import de.dirk.kalender.ui.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.time.format.TextStyle as JTextStyle

/** Shared plumbing: data loading, refresh token, month offset. */
abstract class BaseWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact
    override val stateDefinition = PreferencesGlanceStateDefinition
    abstract val kind: String

    companion object {
        /** Test hook for rendering previews off-device. */
        var snapshotOverride: ((Int) -> Snapshot)? = null
        fun load(ctx: Context, offset: Int) = snapshotOverride?.invoke(offset) ?: Snapshot.load(ctx, offset)
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // First composition of a newly placed widget: freeze the current default appearance.
        if (snapshotOverride == null) {
            val cur = getAppWidgetState(context, PreferencesGlanceStateDefinition, id)
            if (cur[WidgetKeys.STYLE_SET] != true) {
                val prefs = de.dirk.kalender.data.Prefs(context)
                updateAppWidgetState(context, id) { p ->
                    p[WidgetKeys.STYLE_MODE] = prefs.theme
                    p[WidgetKeys.STYLE_GLASS] = prefs.glass
                    p[WidgetKeys.STYLE_SET] = true
                }
            }
        }
        val st = getAppWidgetState(context, PreferencesGlanceStateDefinition, id)
        val initial = withContext(Dispatchers.IO) { load(context, st[WidgetKeys.MONTH_OFFSET] ?: 0) }
        provideContent {
            val p = currentState<Preferences>()
            val offset = p[WidgetKeys.MONTH_OFFSET] ?: 0
            val token = p[WidgetKeys.TOKEN] ?: 0L
            var snap by remember { mutableStateOf(initial) }
            LaunchedEffect(offset, token) {
                snap = withContext(Dispatchers.IO) { load(context, offset) }
            }
            val style = if (snapshotOverride != null) Style(snap.theme, snap.glass)
            else Style(p[WidgetKeys.STYLE_MODE] ?: 0, p[WidgetKeys.STYLE_GLASS] ?: false)
            CompositionLocalProvider(LocalStyle provides style) { Content(snap) }
        }
    }

    @Composable
    abstract fun Content(s: Snapshot)
}

/** Month navigation from the widget header. delta 0 = back to the current month. */
class MonthNav : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val delta = parameters[DELTA] ?: 0
        updateAppWidgetState(context, glanceId) { p ->
            p[WidgetKeys.MONTH_OFFSET] = if (delta == 0) 0 else (p[WidgetKeys.MONTH_OFFSET] ?: 0) + delta
        }
        when (parameters[KIND]) {
            "combo" -> ComboWidget().update(context, glanceId)
            else -> MonthWidget().update(context, glanceId)
        }
    }

    companion object {
        val DELTA = ActionParameters.Key<Int>("delta")
        val KIND = ActionParameters.Key<String>("kind")
        fun params(kind: String, delta: Int) = actionParametersOf(KIND to kind, DELTA to delta)
    }
}

object Fmt {
    private val de = Locale.GERMAN
    private val hm = DateTimeFormatter.ofPattern("HH:mm")
    private val dm = DateTimeFormatter.ofPattern("d.M.")
    val weekdays = listOf("MO", "DI", "MI", "DO", "FR", "SA", "SO")
    private val monthShort = listOf("Jan", "Feb", "März", "Apr", "Mai", "Juni", "Juli", "Aug", "Sep", "Okt", "Nov", "Dez")

    fun monthShort(m: YearMonth) = monthShort[m.monthValue - 1]
    fun monthLong(d: LocalDate): String = d.month.getDisplayName(JTextStyle.FULL_STANDALONE, de)
    fun weekdayLong(d: LocalDate): String = d.dayOfWeek.getDisplayName(JTextStyle.FULL, de)
    fun weekdayShort(d: DayOfWeek): String = d.getDisplayName(JTextStyle.SHORT, de).removeSuffix(".")
    fun dayMonth(d: LocalDate): String = d.format(dm)

    private fun time(ms: Long) = Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).format(hm)

    /** "09:00 – 10:00", or "ab"/"bis" for events spanning midnight. */
    fun timeRange(e: Ev, day: LocalDate): String {
        val s = if (e.startDate == day) time(e.begin) else null
        val t = if (e.lastDate == day) time(e.end) else null
        return when {
            s != null && t != null -> if (e.begin == e.end) s else "$s – $t"
            s != null -> "ab $s"
            t != null -> "bis $t"
            else -> "ganztägig"
        }
    }

    fun start(e: Ev, day: LocalDate): String = if (e.startDate == day) time(e.begin) else "…"
}

object Intents {
    fun day(d: LocalDate): Intent {
        val ms = d.atTime(9, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val uri = CalendarContract.CONTENT_URI.buildUpon().appendPath("time").appendPath(ms.toString()).build()
        return Intent(Intent.ACTION_VIEW, uri)
    }

    fun event(e: Ev) = Intent(Intent.ACTION_VIEW, ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, e.eventId))
        .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, e.begin)
        .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, e.end)

    fun newEvent() = Intent(Intent.ACTION_INSERT, CalendarContract.Events.CONTENT_URI)

    fun settings(ctx: Context) = Intent(ctx, MainActivity::class.java)
}

/** Readable text on a colored all-day pill. */
fun onColor(argb: Int): Int {
    val r = AColor.red(argb) / 255.0; val g = AColor.green(argb) / 255.0; val b = AColor.blue(argb) / 255.0
    return if (0.299 * r + 0.587 * g + 0.114 * b > 0.72) 0xFF1A1A1A.toInt() else AColor.WHITE
}

val rootModifier: GlanceModifier
    @Composable get() = GlanceModifier.fillMaxSize()
        .appWidgetBackground()
        .let { m -> if (C.isGlass) m.background(ImageProvider(C.glassBg)) else m.background(C.card) }
        .cornerRadius(android.R.dimen.system_app_widget_background_radius)

@Composable
fun AddButton() {
    Box(
        modifier = GlanceModifier.size(32.dp).background(C.chip).cornerRadius(16.dp)
            .clickable(actionStartActivity(Intents.newEvent())),
        contentAlignment = Alignment.Center
    ) {
        Image(ImageProvider(R.drawable.ic_plus), contentDescription = "Neuer Termin", modifier = GlanceModifier.size(16.dp))
    }
}

@Composable
fun NoPermission() {
    val ctx = LocalContext.current
    Box(
        modifier = rootModifier.clickable(actionStartActivity(Intents.settings(ctx))),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "Kalenderzugriff fehlt.\nTippen zum Einrichten.",
            style = TextStyle(color = C.muted, fontSize = 13.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
        )
    }
}

fun navAction(kind: String, delta: Int) = actionRunCallback<MonthNav>(MonthNav.params(kind, delta))

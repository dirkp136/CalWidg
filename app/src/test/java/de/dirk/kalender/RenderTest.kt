package de.dirk.kalender

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.view.View
import android.widget.FrameLayout
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.ExperimentalGlanceApi
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.compose
import androidx.test.core.app.ApplicationProvider
import de.dirk.kalender.data.DayWx
import de.dirk.kalender.data.Ev
import de.dirk.kalender.data.Weather
import de.dirk.kalender.widget.AgendaDay
import de.dirk.kalender.widget.AgendaWidget
import de.dirk.kalender.widget.BaseWidget
import de.dirk.kalender.widget.ComboWidget
import de.dirk.kalender.widget.MonthWidget
import de.dirk.kalender.widget.Snapshot
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

/** Renders the widgets to PNGs (off-device sanity check of the layout). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], application = Application::class, qualifiers = "de-w411dp-h891dp-xxhdpi")
class RenderTest {

    private val today = LocalDate.of(2026, 10, 23)
    private var forcedTheme = 0
    private var glass = false

    private fun ev(id: Long, title: String, day: LocalDate, from: String?, to: String?, color: Long, loc: String = ""): Ev {
        val z = ZoneId.systemDefault()
        return if (from == null) Ev(id, title, 0, 0, true, color.toInt(), loc, day, day)
        else Ev(
            id, title,
            day.atTime(java.time.LocalTime.parse(from)).atZone(z).toInstant().toEpochMilli(),
            day.atTime(java.time.LocalTime.parse(to!!)).atZone(z).toInstant().toEpochMilli(),
            false, color.toInt(), loc, day, day
        )
    }

    private fun snapshot(offset: Int): Snapshot {
        val month = YearMonth.from(today).plusMonths(offset.toLong())
        val gridStart = month.atDay(1).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val counts = mapOf(
            1 to 1, 5 to 1, 6 to 2, 7 to 2, 8 to 1, 9 to 3, 10 to 1, 12 to 1, 13 to 1, 14 to 4, 15 to 1,
            18 to 1, 20 to 3, 22 to 1, 23 to 3, 24 to 2, 26 to 2, 28 to 1, 29 to 1, 31 to 1
        ).mapKeys { month.atDay(minOf(it.key, month.lengthOfMonth())) } + mapOf(gridStart.plusDays(39) to 1)
        val tomorrow = today.plusDays(1)
        val agenda = listOf(
            AgendaDay(today, listOf(
                ev(1, "Geburtstag Sabine", today, null, null, 0xFF33B679),
                ev(3, "Budgetrunde IT", today, "13:30", "15:00", 0xFFE67C73, "Raum 4.12"),
                ev(4, "Abendessen", today, "19:30", "22:00", 0xFF33B679, "Bei Marco"),
            ), hadEvents = true),
            AgendaDay(tomorrow, listOf(ev(5, "Formel 1 – Qualifying", tomorrow, "22:00", "23:00", 0xFFF6BF26))),
            AgendaDay(today.plusDays(2), listOf(ev(6, "HSV – Heimspiel", today.plusDays(2), "15:30", "17:30", 0xFF3F51B5, "Volksparkstadion"))),
        )
        val wx = Weather(9, 1, true, mapOf(
            today to DayWx(14, 6, 1), tomorrow to DayWx(12, 5, 61), today.plusDays(2) to DayWx(11, 4, 3)
        ))
        return Snapshot(true, today, month, gridStart, counts, agenda, wx, forcedTheme, glass)
    }

    @OptIn(ExperimentalGlanceApi::class)
    private fun render(ctx: Context, w: GlanceAppWidget, size: DpSize, name: String) = runBlocking {
        val rv = w.compose(ctx, size = size)
        val d = ctx.resources.displayMetrics.density
        val wPx = (size.width.value * d).toInt(); val hPx = (size.height.value * d).toInt()
        val host = FrameLayout(ctx)
        val v = rv.apply(ctx, host)
        host.addView(v, FrameLayout.LayoutParams(wPx, hPx))
        host.measure(View.MeasureSpec.makeMeasureSpec(wPx, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(hPx, View.MeasureSpec.EXACTLY))
        host.layout(0, 0, wPx, hPx)
        val store = name.startsWith("store_") // transparent, no margin: composited later
        val m = if (store) 0 else (24 * d).toInt()
        val bmp = Bitmap.createBitmap(wPx + 2 * m, hPx + 2 * m, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        if (!store) c.drawPaint(Paint().apply {
            shader = LinearGradient(0f, 0f, 0f, bmp.height.toFloat(), 0xFF8E2DE2.toInt(), 0xFFE5338F.toInt(), Shader.TileMode.CLAMP)
        })
        if (!store && name.contains("glass")) {
            // Wallpaper-like backdrop with soft color blobs to judge translucency.
            val blobs = listOf(Triple(0.15f, 0.2f, 0xFFFFB347), Triple(0.8f, 0.35f, 0xFF36D1DC), Triple(0.45f, 0.95f, 0xFF5B86E5))
            for ((fx, fy, col) in blobs) {
                val cx = bmp.width * fx; val cy = bmp.height * fy; val r = bmp.width * 0.45f
                c.drawCircle(cx, cy, r, Paint().apply {
                    shader = android.graphics.RadialGradient(cx, cy, r, col.toInt(), 0x00000000, Shader.TileMode.CLAMP)
                })
            }
        }
        c.translate(m.toFloat(), m.toFloat())
        host.draw(c)
        val dir = File(System.getProperty("render.out") ?: "renders").apply { mkdirs() }
        File(dir, "$name.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun all(suffix: String) {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        BaseWidget.snapshotOverride = { snapshot(it) }
        render(ctx, de.dirk.kalender.widget.DateWidget(), DpSize(170.dp, 170.dp), "date$suffix")
        render(ctx, de.dirk.kalender.widget.DateWidget(), DpSize(76.dp, 76.dp), "date_1x1$suffix")
        render(ctx, MonthWidget(), DpSize(170.dp, 170.dp), "month_small$suffix")
        render(ctx, MonthWidget(), DpSize(360.dp, 360.dp), "month_large$suffix")
        render(ctx, AgendaWidget(), DpSize(360.dp, 180.dp), "agenda$suffix")
        render(ctx, ComboWidget(), DpSize(360.dp, 180.dp), "combo$suffix")
        render(ctx, ComboWidget(), DpSize(360.dp, 240.dp), "combo_tall$suffix")
    }

    @Test fun light() = all("")

    /** Transparent renders for store screenshots. */
    private fun store(suffix: String) {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        BaseWidget.snapshotOverride = { snapshot(it) }
        render(ctx, ComboWidget(), DpSize(380.dp, 200.dp), "store_combo$suffix")
        render(ctx, MonthWidget(), DpSize(380.dp, 390.dp), "store_month$suffix")
        render(ctx, AgendaWidget(), DpSize(380.dp, 200.dp), "store_agenda$suffix")
        render(ctx, de.dirk.kalender.widget.DateWidget(), DpSize(180.dp, 180.dp), "store_date$suffix")
        render(ctx, MonthWidget(), DpSize(180.dp, 180.dp), "store_msmall$suffix")
    }
    @Test fun storeLight() { forcedTheme = 1; store("_light") }
    @Test fun storeDark() { forcedTheme = 2; store("_dark") }
    @Test fun storeGlassLight() { glass = true; forcedTheme = 1; store("_glight") }
    @Test fun storeGlassDark() { glass = true; forcedTheme = 2; store("_gdark") }

    /** System is light, widget forced to dark (and the reverse below). */
    @Test fun glassLight() { glass = true; forcedTheme = 1; all("_glass_light") }
    @Test fun glassDark() { glass = true; forcedTheme = 2; all("_glass_dark") }

    @Test fun forcedDark() { forcedTheme = 2; all("_forced_dark") }

    @Test @Config(qualifiers = "de-w411dp-h891dp-night-xxhdpi")
    fun forcedLight() { forcedTheme = 1; all("_forced_light") }

    @Test @Config(qualifiers = "de-w411dp-h891dp-night-xxhdpi")
    fun dark() = all("_dark")
}

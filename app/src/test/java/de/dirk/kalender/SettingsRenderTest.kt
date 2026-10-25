package de.dirk.kalender

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import de.dirk.kalender.ui.MainActivity
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Renders the settings screen (Widgets tab) to a PNG. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], application = Application::class, qualifiers = "de-w411dp-h891dp-xxhdpi")
class SettingsRenderTest {
    @Test fun settings() = shot(null, "settings")
    @Test fun calendars() = shot("Kalender", "settings_kalender")
    @Test fun guide() = shot("Anleitung", "settings_anleitung")

    private fun shot(tabLabel: String?, name: String) {
        val ctrl = Robolectric.buildActivity(MainActivity::class.java).setup()
        val looper = shadowOf(android.os.Looper.getMainLooper())
        looper.idle()
        val root = ctrl.get().window.decorView
        if (tabLabel != null) {
            fun find(v: android.view.View): android.view.View? {
                if (v.contentDescription == null && v is android.view.ViewGroup) {
                    for (i in 0 until v.childCount) find(v.getChildAt(i))?.let { return it }
                }
                return null
            }
            // Compose has no View per item: click the bottom bar position instead.
            val w = root.width; val h = root.height
            val idx = if (tabLabel == "Kalender") 1 else 2
            val x = w * (idx * 2 + 1) / 6f; val y = h - 120f
            val t = android.os.SystemClock.uptimeMillis()
            root.dispatchTouchEvent(android.view.MotionEvent.obtain(t, t, android.view.MotionEvent.ACTION_DOWN, x, y, 0))
            root.dispatchTouchEvent(android.view.MotionEvent.obtain(t, t + 50, android.view.MotionEvent.ACTION_UP, x, y, 0))
            looper.idle()
        }
        val v = root
        val bmp = Bitmap.createBitmap(v.width, v.height, Bitmap.Config.ARGB_8888)
        v.draw(Canvas(bmp))
        val dir = File(System.getProperty("render.out") ?: "renders").apply { mkdirs() }
        File(dir, "$name.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}

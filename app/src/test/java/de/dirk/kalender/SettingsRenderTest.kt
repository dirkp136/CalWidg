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
    @Test fun settings() {
        val ctrl = Robolectric.buildActivity(MainActivity::class.java).setup()
        shadowOf(android.os.Looper.getMainLooper()).idle()
        val v = ctrl.get().window.decorView
        val bmp = Bitmap.createBitmap(v.width, v.height, Bitmap.Config.ARGB_8888)
        v.draw(Canvas(bmp))
        val dir = File(System.getProperty("render.out") ?: "renders").apply { mkdirs() }
        File(dir, "settings.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}

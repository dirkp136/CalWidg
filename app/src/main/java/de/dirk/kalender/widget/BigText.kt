package de.dirk.kalender.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface

/**
 * Renders text with the bundled Inter Black face as a white alpha mask.
 * Widgets cannot use custom fonts directly, so the date number is drawn as an image
 * and tinted per theme via ColorFilter (keeps light/dark switching working).
 */
object BigText {
    @Volatile private var face: Typeface? = null

    private fun typeface(ctx: Context): Typeface =
        face ?: runCatching { Typeface.createFromAsset(ctx.assets, "fonts/Inter-Black.ttf") }
            .getOrDefault(Typeface.create(Typeface.DEFAULT, Typeface.BOLD)).also { face = it }

    /** @param glyphHeightPx height of the digits (cap height), bitmap is cropped tightly around them. */
    fun render(ctx: Context, text: String, glyphHeightPx: Int): Bitmap {
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = typeface(ctx); color = Color.WHITE; textSize = 100f
            letterSpacing = -0.02f
        }
        val r = Rect()
        p.getTextBounds(text, 0, text.length, r)
        p.textSize = 100f * glyphHeightPx / r.height().coerceAtLeast(1)
        p.getTextBounds(text, 0, text.length, r)
        val pad = (glyphHeightPx * 0.03f).toInt() + 1
        val bmp = Bitmap.createBitmap(r.width() + 2 * pad, r.height() + 2 * pad, Bitmap.Config.ALPHA_8)
        Canvas(bmp).drawText(text, (pad - r.left).toFloat(), (pad - r.top).toFloat(), p)
        return bmp
    }
}

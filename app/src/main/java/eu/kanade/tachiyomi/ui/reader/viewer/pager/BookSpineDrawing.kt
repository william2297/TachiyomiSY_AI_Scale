package eu.kanade.tachiyomi.ui.reader.viewer.pager

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Shader

/**
 * The dip of an open book: paper darkens towards the spine, a thin crease runs down the middle and
 * the outer edges are slightly shaded. Gradients multiply, so they darken white paper but leave
 * black ink at full contrast and speech bubbles near the spine stay readable.
 */
internal class BookSpineDrawing {
    private val multiply = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.MULTIPLY)
    }
    private val line = Paint(Paint.ANTI_ALIAS_FLAG)

    /** Static two-page view: dips on both pages, crease, outer edge shade. */
    fun drawSpread(canvas: Canvas, pictures: List<BookPageBounds>, light: Boolean) {
        if (pictures.size != 2) return
        val left = if (pictures[0].left <= pictures[1].left) pictures[0] else pictures[1]
        val right = if (left === pictures[0]) pictures[1] else pictures[0]
        drawDip(canvas, left, spineAtStart = false, light = light)
        drawDip(canvas, right, spineAtStart = true, light = light)
        drawOuterEdge(canvas, left, outerAtStart = true)
        drawOuterEdge(canvas, right, outerAtStart = false)
        val spine = (left.right + right.left) / 2f
        drawCrease(canvas, spine, minOf(left.top, right.top), maxOf(left.bottom, right.bottom), light, 1f)
    }

    /**
     * Darkens [page] towards its spine edge. [spineAtStart] means the spine is the page's left edge.
     * [strength] scales the effect (0 to 1) for leaves that are turning.
     */
    fun drawDip(canvas: Canvas, page: BookPageBounds, spineAtStart: Boolean, light: Boolean, strength: Float = 1f) {
        if (strength <= 0.01f || page.width <= 1f) return
        val width = page.width
        val reach = width * 0.09f
        val spine = if (spineAtStart) page.left else page.right
        val end = if (spineAtStart) spine + reach else spine - reach
        val colors = if (light) LIGHT_DIP else DARK_DIP
        val stops = if (light) LIGHT_STOPS else DARK_STOPS
        multiply.shader = LinearGradient(spine, 0f, end, 0f, colors, stops, Shader.TileMode.CLAMP)
        multiply.alpha = (strength.coerceIn(0f, 1f) * 255).toInt()
        canvas.drawRect(minOf(spine, end), page.top, maxOf(spine, end), page.bottom, multiply)
        multiply.shader = null
        multiply.alpha = 255
    }

    private fun drawOuterEdge(canvas: Canvas, page: BookPageBounds, outerAtStart: Boolean) {
        val reach = page.width * 0.02f
        val edge = if (outerAtStart) page.left else page.right
        val end = if (outerAtStart) edge + reach else edge - reach
        multiply.shader = LinearGradient(edge, 0f, end, 0f, OUTER_EDGE, Color.WHITE, Shader.TileMode.CLAMP)
        canvas.drawRect(minOf(edge, end), page.top, maxOf(edge, end), page.bottom, multiply)
        multiply.shader = null
    }

    /** The fold line at [spine]; [strength] dips while a leaf stands up during a turn. */
    fun drawCrease(canvas: Canvas, spine: Float, top: Float, bottom: Float, light: Boolean, strength: Float) {
        val creaseAlpha = (if (light) 0.20f else 0.30f) * strength
        line.color = Color.argb((creaseAlpha * 255).toInt(), 0, 0, 0)
        line.strokeWidth = 2f
        canvas.drawLine(spine, top, spine, bottom, line)
        line.color = Color.argb((0.08f * strength * 255).toInt(), 255, 255, 255)
        line.strokeWidth = 1f
        canvas.drawLine(spine - 3f, top, spine - 3f, bottom, line)
        canvas.drawLine(spine + 3f, top, spine + 3f, bottom, line)
    }

    private companion object {
        val DARK_DIP = intArrayOf(0xFFB4B4B4.toInt(), 0xFFCFCFCF.toInt(), 0xFFE8E8E8.toInt(), 0xFFF8F8F8.toInt(), Color.WHITE)
        val DARK_STOPS = floatArrayOf(0f, 0.012f / 0.09f, 0.035f / 0.09f, 0.06f / 0.09f, 1f)
        val LIGHT_DIP = intArrayOf(0xFFC6C6C6.toInt(), 0xFFDCDCDC.toInt(), 0xFFF0F0F0.toInt(), Color.WHITE)
        val LIGHT_STOPS = floatArrayOf(0f, 0.012f / 0.09f, 0.035f / 0.09f, 1f)
        val OUTER_EDGE = 0xFFEDEDED.toInt()
    }
}

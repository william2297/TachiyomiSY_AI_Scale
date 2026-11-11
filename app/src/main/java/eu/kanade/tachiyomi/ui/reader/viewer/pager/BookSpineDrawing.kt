package eu.kanade.tachiyomi.ui.reader.viewer.pager

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Shader
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * The dip of an open book: paper darkens towards the spine, a thin crease runs down the middle and
 * the outer edges are slightly shaded. Gradients multiply, so they darken white paper but leave
 * black ink at full contrast and speech bubbles near the spine stay readable.
 */
internal class BookSpineDrawing {
    private val multiply = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        // The separable multiply keeps the page opaque when the effect is drawn at partial strength.
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            blendMode = android.graphics.BlendMode.MULTIPLY
        } else {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.MULTIPLY)
        }
    }
    private val line = Paint(Paint.ANTI_ALIAS_FLAG)

    /**
     * Static two-page view with the paper bending into the spine: outside a band next to the spine
     * the pages are drawn as they are; inside it the picture is drawn in strips that are squeezed
     * towards the spine and shortened a little, so the top and bottom edges dip into the gutter.
     */
    fun drawCurvedSpread(
        canvas: Canvas,
        viewHeight: Float,
        pictures: List<BookPageBounds>,
        light: Boolean,
        drawWhole: (Canvas) -> Unit,
        drawBackground: (Canvas) -> Unit,
        content: (Canvas) -> Unit,
        stackLeft: Float? = null,
    ) {
        val left = if (pictures[0].left <= pictures[1].left) pictures[0] else pictures[1]
        val right = if (left === pictures[0]) pictures[1] else pictures[0]
        val leftBand = left.width * CURVE_BAND
        val rightBand = right.width * CURVE_BAND
        val bandStart = left.right - leftBand
        val bandEnd = right.left + rightBand

        var save = canvas.save()
        canvas.clipOutRect(bandStart, 0f, bandEnd, viewHeight)
        drawWhole(canvas)
        canvas.restoreToCount(save)

        save = canvas.save()
        canvas.clipRect(bandStart, 0f, bandEnd, viewHeight)
        drawBackground(canvas)
        canvas.restoreToCount(save)

        drawCurvedBand(canvas, left, left.right, -1f, leftBand, viewHeight, content)
        drawCurvedBand(canvas, right, right.left, 1f, rightBand, viewHeight, content)
        drawSpread(canvas, pictures, light)
        if (stackLeft != null) drawPaperStack(canvas, left, right, stackLeft, light)
    }

    /**
     * One page with its bend into the spine, for the pages lying flat while a leaf turns, so the gutter does not
     * flatten for the length of the turn.
     */
    fun drawPageCurved(canvas: Canvas, page: BookPageBounds, spineAtStart: Boolean, content: (Canvas) -> Unit) {
        if (page.width <= 1f) return
        val band = page.width * CURVE_BAND
        val spine = if (spineAtStart) page.left else page.right
        val bandEdge = if (spineAtStart) spine + band else spine - band
        val save = canvas.save()
        if (spineAtStart) {
            canvas.clipRect(bandEdge, page.top, page.right, page.bottom)
        } else {
            canvas.clipRect(page.left, page.top, bandEdge, page.bottom)
        }
        content(canvas)
        canvas.restoreToCount(save)
        drawCurvedBand(canvas, page, spine, if (spineAtStart) 1f else -1f, band, page.bottom + 1f, content)
    }

    /**
     * Edges of the pages under each side, outside the outer page edges: more lines on the side that holds more
     * of the chapter, so the stack moves as the chapter is read. [leftShare] is the part of the chapter on the
     * left side (0 to 1).
     */
    fun drawPaperStack(canvas: Canvas, left: BookPageBounds, right: BookPageBounds, leftShare: Float, light: Boolean) {
        val colors = if (light) LIGHT_STACK else DARK_STACK
        val share = leftShare.coerceIn(0f, 1f)
        fun lines(page: BookPageBounds, count: Int, outward: Float) {
            val edge = if (outward < 0f) page.left else page.right
            for (k in 0 until count) {
                val x = edge + outward * STACK_PITCH * (k + 1)
                // Lower pages show a little less, like a stack seen from above.
                val inset = STACK_PITCH * (k + 1) * 0.5f
                line.color = colors[k.coerceAtMost(colors.lastIndex)]
                line.strokeWidth = 1f
                canvas.drawLine(x, page.top + inset, x, page.bottom - inset, line)
            }
        }
        lines(left, (1 + 4 * share).roundToInt(), -1f)
        lines(right, (1 + 4 * (1f - share)).roundToInt(), 1f)
    }

    private fun drawCurvedBand(
        canvas: Canvas,
        page: BookPageBounds,
        spine: Float,
        direction: Float,
        band: Float,
        viewHeight: Float,
        content: (Canvas) -> Unit,
    ) {
        if (band < 2f) return
        val centerY = (page.top + page.bottom) / 2f
        // Position across the band: squeezed at the spine, slightly stretched midway, unchanged at
        // the band edge, so it joins the flat part of the page without a seam.
        fun bent(u: Float): Float {
            val rest = 1f - u / band
            return u - CURVE_SQUEEZE * u * rest * rest
        }
        for (i in 0 until CURVE_STRIPS) {
            val u0 = band * i / CURVE_STRIPS
            val u1 = band * (i + 1) / CURVE_STRIPS
            val x0 = spine + direction * bent(u0)
            val x1 = spine + direction * bent(u1)
            val source0 = spine + direction * u0
            val source1 = spine + direction * u1
            if (abs(x1 - x0) < 0.05f) continue
            val rest = 1f - (u0 + u1) / 2f / band
            // Ease-out: the dip of the top and bottom edges is concentrated next to the spine.
            val vertical = 1f - CURVE_SINK * rest * rest * rest
            val save = canvas.save()
            canvas.clipRect(min(x0, x1) - 0.5f, 0f, max(x0, x1) + 0.5f, viewHeight)
            canvas.scale(1f, vertical, 0f, centerY)
            canvas.translate(x0, 0f)
            canvas.scale((x1 - x0) / (source1 - source0), 1f)
            canvas.translate(-source0, 0f)
            content(canvas)
            canvas.restoreToCount(save)
        }
    }

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
        val reach = page.width * 0.015f
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
        val OUTER_EDGE = 0xFFF0F0F0.toInt()
        val DARK_STACK = intArrayOf(
            0xFFD8D8D8.toInt(), 0xFFBDBDBD.toInt(), 0xFF9E9E9E.toInt(), 0xFF868686.toInt(), 0xFF707070.toInt(),
        )
        val LIGHT_STACK = intArrayOf(
            0xFFE4E4E4.toInt(), 0xFFCFCFCF.toInt(), 0xFFB8B8B8.toInt(), 0xFFA4A4A4.toInt(), 0xFF909090.toInt(),
        )

        /** Distance between the lines of the paper stack, in pixels. */
        const val STACK_PITCH = 2f

        /** Share of each page width that bends into the spine. */
        const val CURVE_BAND = 0.07f

        /** How much the picture is squeezed at the spine (0.32 = 0.68 x width at the crease). */
        const val CURVE_SQUEEZE = 0.32f

        /** How much shorter the page is at the spine, so its top and bottom edges dip. */
        const val CURVE_SINK = 0.02f
        const val CURVE_STRIPS = 14
    }
}

package eu.kanade.tachiyomi.ui.reader.viewer.pager

import android.graphics.Camera
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

internal class BookPageTurnDrawing {
    private val camera = Camera()
    private val matrix = Matrix()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val bounds = RectF()

    fun draw(canvas: Canvas, width: Int, height: Int, turn: BookPageTurn, content: (Canvas) -> Unit) {
        if (turn.curl && turn.spread && !turn.vertical && drawCurl(canvas, turn, content)) return
        val count = if (turn.spread) 2 else 1
        // Draw the fixed leaf first. The other spread supplies the turning leaf's reverse face.
        val leaves = (0 until count).sortedBy { turn.leaf(it).turning }
        for (index in leaves) {
            val leaf = turn.leaf(index)
            if (!leaf.visible) continue
            bounds.set(0f, 0f, width.toFloat(), height.toFloat())
            turn.bounds?.let { bounds.set(it.left, it.top, it.right, it.bottom) }
            val imageBounds = turn.imageBounds?.getOrNull(index)
            if (imageBounds != null) {
                bounds.set(imageBounds.left, imageBounds.top, imageBounds.right, imageBounds.bottom)
            } else if (turn.spread) {
                val left = bounds.left
                val half = bounds.width() / 2f
                bounds.left = left + half * index
                bounds.right = left + half * (index + 1)
            }
            val extent = if (turn.vertical) bounds.height() else bounds.width()
            val hinge = if (leaf.hingeAtStart) {
                if (turn.vertical) bounds.top else bounds.left
            } else {
                if (turn.vertical) bounds.bottom else bounds.right
            }
            val save = canvas.save()
            val fraction = if (turn.current) turn.progress else 1f - turn.progress
            val scale = 1f + (turn.targetScale - 1f) * fraction
            if (turn.layoutTransition) {
                val shift = if (turn.current) turn.progress else turn.progress - 1f
                canvas.translate(turn.hingeShift * shift, turn.crossShift * shift)
                canvas.scale(scale, scale, hinge, bounds.centerY())
            }
            if (leaf.turning) {
                camera.save()
                camera.setLocation(0f, 0f, -maxOf(1600f, extent * 4f) / (72f * scale))
                if (turn.vertical) camera.rotateX(-leaf.degrees) else camera.rotateY(leaf.degrees)
                camera.getMatrix(matrix)
                camera.restore()
                val pivotX = if (turn.vertical) bounds.centerX() else hinge
                val pivotY = if (turn.vertical) hinge else bounds.centerY()
                matrix.preTranslate(-pivotX, -pivotY)
                matrix.postTranslate(pivotX, pivotY)
                canvas.concat(matrix)
            }
            canvas.clipRect(bounds)
            content(canvas)
            val shade = sin(Math.PI * turn.progress).toFloat().coerceAtLeast(0f)
            if (shade > 0f) {
                val shadowSize = extent * if (turn.spread) 0.09f else 0.18f
                val edge = hinge + if (leaf.hingeAtStart) shadowSize else -shadowSize
                val color = Color.argb((shade * if (leaf.turning) 90 else 45).toInt(), 0, 0, 0)
                paint.alpha = 255
                paint.shader = if (turn.vertical) {
                    LinearGradient(0f, hinge, 0f, edge, color, Color.TRANSPARENT, Shader.TileMode.CLAMP)
                } else {
                    LinearGradient(hinge, 0f, edge, 0f, color, Color.TRANSPARENT, Shader.TileMode.CLAMP)
                }
                canvas.drawRect(bounds, paint)
                paint.shader = null
                if (leaf.turning && abs(leaf.degrees) > 65f) {
                    paint.color = Color.argb((shade * 100).toInt(), 240, 235, 224)
                    paint.strokeWidth = 1.5f
                    if (turn.vertical) {
                        canvas.drawLine(bounds.left, hinge, bounds.right, hinge, paint)
                    } else {
                        canvas.drawLine(hinge, bounds.top, hinge, bounds.bottom, paint)
                    }
                }
            }
            canvas.restoreToCount(save)
        }
    }

    /**
     * Page curl for a spread turning into the next spread. The turning leaf wraps around a cylinder
     * whose axis runs parallel to the spine and travels from the outer edge to the spine.
     *
     * The outgoing spread draws its fixed leaf and the front of the curling leaf. The incoming spread
     * lies on top and draws the part of its own page that the curl uncovers, the shadows, and the
     * back of the curling leaf, which is the incoming spread's other page.
     *
     * The picture is drawn as vertical strips; each strip maps its slice of the page linearly, which
     * approximates the cylinder closely enough at this strip count.
     */
    private fun drawCurl(canvas: Canvas, turn: BookPageTurn, content: (Canvas) -> Unit): Boolean {
        val own = turn.imageBounds?.takeIf { it.size == 2 } ?: return false
        val peer = turn.peerImageBounds?.takeIf { it.size == 2 } ?: own
        // Index 0 is the left picture on screen. The outgoing spread turns its leaf on this side.
        val turningSide = if (turn.step > 0) 1 else 0
        val otherSide = 1 - turningSide
        val leaf = if (turn.current) own[turningSide] else peer[turningSide]
        val direction = if (turningSide == 1) 1f else -1f
        val spine = if (turningSide == 1) leaf.left else leaf.right
        val leafWidth = leaf.width
        if (leafWidth <= 1f) return false

        val progress = turn.progress.coerceIn(0f, 1f)
        val radius = CURL_RADIUS * (1f - progress) + 0.002f
        val axis = (1f - progress) - progress * (PI.toFloat() * radius / 2f)
        val frontEnd = axis + PI.toFloat() * radius / 2f
        val backEnd = axis + PI.toFloat() * radius

        fun curlPosition(s: Float): Float {
            val distance = s - axis
            if (distance <= 0f) return s
            val arc = PI.toFloat() * radius
            return if (distance <= arc) axis + radius * sin(distance / radius) else axis - (distance - arc)
        }
        fun screenX(s: Float): Float = spine + direction * curlPosition(s) * leafWidth
        fun angle(s: Float): Float = ((s - axis) / radius).coerceIn(0f, PI.toFloat())

        val shade = sin(PI * progress).toFloat().coerceAtLeast(0f)
        if (turn.current) {
            // Fixed leaf.
            val fixed = own[otherSide]
            drawSlice(canvas, fixed.left, fixed.right, fixed, content)
            // Flat part of the turning leaf.
            val flatEnd = axis.coerceIn(0f, 1f)
            if (flatEnd > 0f) {
                val edge = spine + direction * flatEnd * leafWidth
                drawSlice(canvas, min(spine, edge), max(spine, edge), leaf, content)
            }
            // Front of the cylinder. Mostly hidden under the back face, but it fills the curl when
            // the incoming spread is not drawn yet.
            val strips = drawStrips(
                canvas = canvas,
                clip = leaf,
                from = max(0f, axis),
                to = min(1f, frontEnd),
                content = content,
                screenX = ::screenX,
                sourceX = { s -> spine + direction * s * leafWidth },
            )
            strips.forEach { strip ->
                shadeStrip(canvas, strip.left, strip.right, leaf, 0.42f * (1f - cos(angle(strip.middle))))
            }
            return true
        }

        // Incoming spread: its page under the curling leaf, visible beyond the curl.
        val under = own[turningSide]
        val maxReach = if (frontEnd <= 1f) axis + radius else curlPosition(1f)
        val reach = spine + direction * maxReach * leafWidth
        if (direction > 0f) {
            drawSlice(canvas, max(under.left, reach), under.right, under, content)
        } else {
            drawSlice(canvas, under.left, min(under.right, reach), under, content)
        }
        // Shadow the curl casts on that page.
        if (shade > 0f && maxReach < 1.2f) {
            val shadowEnd = reach + direction * leafWidth * 0.08f
            drawShadow(canvas, reach, shadowEnd, under, (shade * 110).toInt())
        }

        // Back of the curling leaf: the incoming spread's other page, read from its spine outwards.
        val back = own[otherSide]
        val backSpine = if (otherSide == 0) back.right else back.left
        val backDirection = -direction
        val backWidth = back.width
        if (backWidth <= 1f) return true
        fun backSource(s: Float): Float = backSpine + backDirection * s * backWidth

        val flatBackStart = max(0f, backEnd)
        if (flatBackStart < 1f && shade > 0f) {
            // Shadow of the folded part on the page below it.
            val edge = screenX(1f)
            drawShadow(canvas, edge, edge - direction * leafWidth * 0.05f, back, (shade * 70).toInt())
        }
        val backStrips = drawStrips(
            canvas = canvas,
            clip = back,
            from = max(0f, frontEnd),
            to = min(1f, backEnd),
            content = content,
            screenX = ::screenX,
            sourceX = ::backSource,
        )
        backStrips.forEach { strip ->
            shadeStrip(canvas, strip.left, strip.right, back, 0.42f * (1f - abs(cos(angle(strip.middle)))))
        }
        if (flatBackStart < 1f) {
            drawMapped(
                canvas,
                back,
                screenX(flatBackStart),
                screenX(1f),
                backSource(flatBackStart),
                backSource(1f),
                content,
            )
        }
        return true
    }

    private class Strip(val left: Float, val right: Float, val middle: Float)

    /** Draws [from]..[to] of a leaf as strips; returns each strip's screen span and middle position. */
    private fun drawStrips(
        canvas: Canvas,
        clip: BookPageBounds,
        from: Float,
        to: Float,
        content: (Canvas) -> Unit,
        screenX: (Float) -> Float,
        sourceX: (Float) -> Float,
    ): List<Strip> {
        if (to <= from) return emptyList()
        val strips = ArrayList<Strip>(CURL_STRIPS)
        for (i in 0 until CURL_STRIPS) {
            val s0 = from + (to - from) * i / CURL_STRIPS
            val s1 = from + (to - from) * (i + 1) / CURL_STRIPS
            val x0 = screenX(s0)
            val x1 = screenX(s1)
            if (abs(x1 - x0) < 0.1f) continue
            drawMapped(canvas, clip, x0, x1, sourceX(s0), sourceX(s1), content)
            strips += Strip(min(x0, x1), max(x0, x1), (s0 + s1) / 2f)
        }
        return strips
    }

    /** Draws the picture so that source x [source0]..[source1] lands on screen x [x0]..[x1]. */
    private fun drawMapped(
        canvas: Canvas,
        clip: BookPageBounds,
        x0: Float,
        x1: Float,
        source0: Float,
        source1: Float,
        content: (Canvas) -> Unit,
    ) {
        if (abs(source1 - source0) < 0.01f) return
        val save = canvas.save()
        // Slight overlap hides hairline seams between neighbouring strips.
        canvas.clipRect(min(x0, x1) - 0.75f, clip.top, max(x0, x1) + 0.75f, clip.bottom)
        canvas.translate(x0, 0f)
        canvas.scale((x1 - x0) / (source1 - source0), 1f)
        canvas.translate(-source0, 0f)
        content(canvas)
        canvas.restoreToCount(save)
    }

    private fun drawSlice(canvas: Canvas, left: Float, right: Float, clip: BookPageBounds, content: (Canvas) -> Unit) {
        if (right - left < 0.5f) return
        val save = canvas.save()
        canvas.clipRect(left, clip.top, right, clip.bottom)
        content(canvas)
        canvas.restoreToCount(save)
    }

    private fun shadeStrip(canvas: Canvas, left: Float, right: Float, clip: BookPageBounds, amount: Float) {
        val alpha = (amount.coerceIn(0f, 1f) * 255).toInt()
        if (alpha <= 0) return
        paint.shader = null
        paint.color = Color.argb(alpha, 0, 0, 0)
        bounds.set(left - 0.75f, clip.top, right + 0.75f, clip.bottom)
        canvas.drawRect(bounds, paint)
    }

    private fun drawShadow(canvas: Canvas, start: Float, end: Float, clip: BookPageBounds, alpha: Int) {
        if (alpha <= 0 || abs(end - start) < 0.5f) return
        val color = Color.argb(alpha.coerceIn(0, 255), 0, 0, 0)
        paint.color = Color.BLACK
        paint.shader = LinearGradient(start, 0f, end, 0f, color, Color.TRANSPARENT, Shader.TileMode.CLAMP)
        bounds.set(min(start, end), clip.top, max(start, end), clip.bottom)
        canvas.drawRect(bounds, paint)
        paint.shader = null
    }

    private companion object {
        /** Curl radius at the start of a turn, as a fraction of the page width. */
        const val CURL_RADIUS = 0.16f
        const val CURL_STRIPS = 10
    }
}

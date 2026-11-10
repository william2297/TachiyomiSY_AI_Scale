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
import kotlin.math.pow
import kotlin.math.sin

internal class BookPageTurnDrawing {
    private val camera = Camera()
    private val matrix = Matrix()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val bounds = RectF()
    private val spineDrawing = BookSpineDrawing()
    private val flipMatrix = Matrix()

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
     * Book page turn for a spread turning into the next spread: the turning leaf swings around the
     * spine in 3D, bending a little towards its outer edge, and is drawn in perspective so it grows
     * as it lifts towards the reader. Strips that face away show the back of the leaf, which is the
     * incoming spread's other page.
     *
     * Until halfway the outgoing spread lies on top and draws its fixed page and the front of the
     * leaf; the incoming spread underneath draws the page being uncovered. After halfway the
     * incoming spread lies on top and draws the back of the leaf over the outgoing fixed page.
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
        val wave = sin(PI * progress).toFloat().coerceAtLeast(0f)
        val light = turn.lightBook
        val shadowScale = if (light) 0.7f else 1f
        val centerY = (leaf.top + leaf.bottom) / 2f
        val focal = leafWidth * FLIP_FOCAL

        // Leaf shape: tangent angle along the leaf, the outer part leading the turn.
        val base = PI.toFloat() * progress
        val count = FLIP_STRIPS
        val xs = FloatArray(count + 1)
        val zs = FloatArray(count + 1)
        val angles = FloatArray(count + 1)
        for (i in 0..count) {
            val s = i.toFloat() / count
            angles[i] = (base + FLIP_BEND * wave * s * s).coerceAtMost(PI.toFloat())
        }
        for (i in 1..count) {
            val mid = (angles[i - 1] + angles[i]) / 2f
            xs[i] = xs[i - 1] + cos(mid) / count
            zs[i] = zs[i - 1] + sin(mid) / count
        }
        // Perspective: points lifted towards the reader (z > 0) are drawn larger.
        fun scaleAt(i: Int): Float = focal / (focal - zs[i] * leafWidth)
        fun screenXAt(i: Int): Float = spine + direction * xs[i] * leafWidth * scaleAt(i)
        val outerX = screenXAt(count)

        fun drawLeafStrips(back: Boolean) {
            val picture = if (back) own[otherSide] else leaf
            val pictureSpine = if (back) {
                if (otherSide == 0) picture.right else picture.left
            } else {
                spine
            }
            val pictureDirection = if (back) -direction else direction
            val src = FloatArray(8)
            val dst = FloatArray(8)
            for (i in 0 until count) {
                val facing = (angles[i] + angles[i + 1]) / 2f
                val showsFront = facing < PI.toFloat() / 2f
                if (showsFront == back) continue
                val source0 = pictureSpine + pictureDirection * (i.toFloat() / count) * picture.width
                val source1 = pictureSpine + pictureDirection * ((i + 1).toFloat() / count) * picture.width
                val k0 = scaleAt(i)
                val k1 = scaleAt(i + 1)
                val x0 = screenXAt(i)
                val x1 = screenXAt(i + 1)
                if (abs(x1 - x0) < 0.2f) continue
                src[0] = source0; src[1] = picture.top
                src[2] = source1; src[3] = picture.top
                src[4] = source1; src[5] = picture.bottom
                src[6] = source0; src[7] = picture.bottom
                dst[0] = x0; dst[1] = centerY + (picture.top - centerY) * k0
                dst[2] = x1; dst[3] = centerY + (picture.top - centerY) * k1
                dst[4] = x1; dst[5] = centerY + (picture.bottom - centerY) * k1
                dst[6] = x0; dst[7] = centerY + (picture.bottom - centerY) * k0
                if (!flipMatrix.setPolyToPoly(src, 0, dst, 0, 4)) continue
                val save = canvas.save()
                canvas.concat(flipMatrix)
                // A sliver of overlap hides seams between neighbouring strips.
                val pad = abs(source1 - source0) * 0.08f + 0.5f
                canvas.clipRect(min(source0, source1) - pad, picture.top, max(source0, source1) + pad, picture.bottom)
                content(canvas)
                // Shading follows the angle continuously across strips, so strips never show.
                val shade0 = leafShade(angles[i], back) * shadowScale
                val shade1 = leafShade(angles[i + 1], back) * shadowScale
                if (shade0 > 0.004f || shade1 > 0.004f) {
                    paint.color = Color.BLACK
                    paint.shader = LinearGradient(
                        source0, 0f, source1, 0f,
                        Color.argb((shade0 * 255).toInt(), 0, 0, 0),
                        Color.argb((shade1 * 255).toInt(), 0, 0, 0),
                        Shader.TileMode.CLAMP,
                    )
                    bounds.set(min(source0, source1) - pad, picture.top, max(source0, source1) + pad, picture.bottom)
                    canvas.drawRect(bounds, paint)
                    paint.shader = null
                }
                canvas.restoreToCount(save)
            }
        }

        val creaseStrength = 1f - 0.4f * wave
        if (turn.current) {
            // Fixed page with its dip, and the shadow of the leaf once it lies over this page.
            val fixed = own[otherSide]
            drawSlice(canvas, fixed.left, fixed.right, fixed, content)
            spineDrawing.drawDip(canvas, fixed, spineAtStart = otherSide == 1, light = light)
            if (progress > 0.5f && progress < 0.98f) {
                drawLeafShadow(canvas, fixed, outerX, -direction, leafWidth, wave, shadowScale)
            }
            drawLeafStrips(back = false)
            if (progress < 0.5f) spineDrawing.drawCrease(canvas, spine, leaf.top, leaf.bottom, light, creaseStrength)
            return true
        }

        // Incoming spread: the page being uncovered, its shadow, then the back of the leaf.
        val under = own[turningSide]
        drawSlice(canvas, under.left, under.right, under, content)
        spineDrawing.drawDip(canvas, under, spineAtStart = turningSide == 1, light = light)
        if (progress > 0.02f && progress < 0.5f) {
            drawLeafShadow(canvas, under, outerX, direction, leafWidth, wave, shadowScale)
        }
        drawLeafStrips(back = true)
        if (progress >= 0.5f) spineDrawing.drawCrease(canvas, spine, leaf.top, leaf.bottom, light, creaseStrength)
        return true
    }

    /** Black alpha for a leaf strip at [angle]: darker as it turns edge-on to the reader. */
    private fun leafShade(angle: Float, back: Boolean): Float {
        val facing = abs(cos(angle))
        return if (back) 0.38f * (1f - facing) else 0.30f * (1f - facing)
    }

    /** Soft shadow the lifted leaf casts on [page], next to the leaf's outer edge. */
    private fun drawLeafShadow(
        canvas: Canvas,
        page: BookPageBounds,
        edge: Float,
        outward: Float,
        leafWidth: Float,
        wave: Float,
        shadowScale: Float,
    ) {
        val alpha = 0.30f * wave * shadowScale
        val width = leafWidth * (0.03f + 0.08f * wave)
        val save = canvas.save()
        canvas.clipRect(page.left, page.top, page.right, page.bottom)
        drawGradient(
            canvas, page, edge, edge + outward * width,
            intArrayOf(
                Color.argb((alpha * 255).toInt(), 0, 0, 0),
                Color.argb((alpha * 0.4f * 255).toInt(), 0, 0, 0),
                Color.TRANSPARENT,
            ),
            floatArrayOf(0f, 0.35f, 1f),
        )
        canvas.restoreToCount(save)
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
        // A 1 px overlap hides hairline seams between neighbouring strips.
        canvas.clipRect(min(x0, x1) - 1f, clip.top, max(x0, x1) + 1f, clip.bottom)
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

    /** A horizontal gradient from [start] to [end], limited to [clip] vertically. */
    private fun drawGradient(
        canvas: Canvas,
        clip: BookPageBounds,
        start: Float,
        end: Float,
        colors: IntArray,
        stops: FloatArray,
    ) {
        if (abs(end - start) < 0.5f) return
        paint.color = Color.BLACK
        paint.shader = LinearGradient(start, 0f, end, 0f, colors, stops, Shader.TileMode.CLAMP)
        bounds.set(min(start, end), clip.top, max(start, end), clip.bottom)
        canvas.drawRect(bounds, paint)
        paint.shader = null
    }

    private companion object {
        const val CURL_STRIPS = 16

        /** Strips the turning leaf is drawn in. */
        const val FLIP_STRIPS = 24

        /** Extra turn of the outer edge mid-turn, in radians. */
        const val FLIP_BEND = 0.6f

        /** Camera distance as a multiple of the page width; smaller is stronger perspective. */
        const val FLIP_FOCAL = 5f
    }
}

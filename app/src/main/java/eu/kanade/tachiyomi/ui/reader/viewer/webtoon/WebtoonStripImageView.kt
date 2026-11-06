package eu.kanade.tachiyomi.ui.reader.viewer.webtoon

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.view.MotionEvent
import android.view.View
import eu.kanade.tachiyomi.data.coil.ImageDecoder
import kotlin.math.roundToInt

/**
 * Draws a webtoon page as a stack of strips, so a very tall page never becomes one huge GPU texture.
 *
 * Ignores all touch events, because the webtoon viewer handles all the gestures.
 */
class WebtoonStripImageView(
    context: Context,
    private val largePageBytes: Long = LARGE_PAGE_BYTES,
) : View(context) {

    private var strips: List<Bitmap> = emptyList()
    private var imageWidth = 0
    private var imageHeight = 0

    private val paint = Paint(Paint.FILTER_BITMAP_FLAG)
    private val clipRect = Rect()
    private val dstRect = Rect()

    /** Decodes the page again after its strips were freed off screen. */
    var onReload: (() -> Unit)? = null

    private var isReleasedOffScreen = false

    private var blankSinceNanos = 0L

    fun setImage(image: ImageDecoder.StripsImage) {
        endBlankEpisode()
        releaseStrips()
        isReleasedOffScreen = false
        strips = image.strips
        reloadAttempts = 0
        imageWidth = image.width
        imageHeight = image.height
        requestLayout()
        invalidate()
    }

    fun recycle() {
        removeCallbacks(reloadAgain)
        reloadAttempts = 0
        releaseStrips()
        isReleasedOffScreen = false
        onReload = null
        imageWidth = 0
        imageHeight = 0
        requestLayout()
    }

    // The viewer keeps a few pages off screen without recycling them, which adds up for huge pages.
    // The size stays, so the layout doesn't move while the page decodes again.
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        if (strips.sumOf { it.allocationByteCount.toLong() } > largePageBytes) {
            releaseStrips()
            isReleasedOffScreen = true
            StripStats.freed()
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (isReleasedOffScreen) {
            isReleasedOffScreen = false
            StripStats.reloaded()
            onReload?.invoke()
        }
    }

    // A page's strips can be hundreds of MB, which would otherwise wait for a garbage collection to be freed
    private fun releaseStrips() {
        val released = strips
        strips = emptyList()
        released.forEach(Bitmap::recycle)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = if (imageWidth > 0) (width.toLong() * imageHeight / imageWidth).toInt() else 0
        setMeasuredDimension(width, height)
    }

    private var reloadAttempts = 0

    private val reloadAgain = Runnable {
        if (isAttachedToWindow && imageWidth > 0 && strips.isEmpty()) onReload?.invoke()
    }

    /**
     * A page that has its size but no pixels, while it is on screen, is a reload that did not arrive (the strips were
     * freed off screen, or the request ended without a result). Ask again a few times instead of leaving a blank page
     * that only comes back after scrolling away and back.
     */
    private fun endBlankEpisode() {
        if (blankSinceNanos == 0L) return
        StripStats.blankEnded((System.nanoTime() - blankSinceNanos) / 1_000_000L)
        blankSinceNanos = 0L
    }

    private fun reloadIfBlank() {
        if (imageWidth <= 0 || strips.isNotEmpty() || onReload == null) return
        if (blankSinceNanos == 0L) {
            blankSinceNanos = System.nanoTime()
            StripStats.blankStarted()
        }
        if (reloadAttempts >= MAX_RELOAD_ATTEMPTS) return
        reloadAttempts++
        removeCallbacks(reloadAgain)
        postDelayed(reloadAgain, RELOAD_DELAY_MILLIS)
    }

    override fun onDraw(canvas: Canvas) {
        if (imageWidth <= 0) return
        reloadIfBlank()
        canvas.getClipBounds(clipRect)

        val scale = width.toFloat() / imageWidth
        var srcTop = 0
        for (strip in strips) {
            val top = (srcTop * scale).roundToInt()
            srcTop += strip.height
            val bottom = (srcTop * scale).roundToInt()
            if (bottom <= clipRect.top) continue
            if (top >= clipRect.bottom) break
            dstRect.set(0, top, width, bottom)
            canvas.drawBitmap(strip, null, dstRect, paint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean = false
}

private const val LARGE_PAGE_BYTES = 64L * 1024 * 1024
private const val MAX_RELOAD_ATTEMPTS = 3
private const val RELOAD_DELAY_MILLIS = 1_500L

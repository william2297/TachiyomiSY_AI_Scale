package eu.kanade.tachiyomi.ui.reader.viewer.webtoon

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView

/**
 * A 2x2 pixel, almost transparent surface that asks the display for an exact refresh rate. A surface's own frame
 * rate is a separate vote to the screen compositor, apart from the window's preferred mode, which the manufacturer's
 * refresh rate policy may revise. It keeps drawing one empty pixel a few times a second, because a surface that
 * stops updating loses its vote. Only used while auto scroll runs.
 */
class FrameRateVoteView(context: Context) : SurfaceView(context), SurfaceHolder.Callback {

    private var hz = 0f
    private var running = false

    /** How many times the vote was sent, for the diagnostics. */
    var votesSent = 0
        private set

    private val tick = object : Runnable {
        override fun run() {
            if (!running) return
            drawOnePixel()
            sendVote()
            postDelayed(this, TICK_MILLIS)
        }
    }

    init {
        setZOrderOnTop(true)
        holder.setFormat(PixelFormat.TRANSLUCENT)
        holder.addCallback(this)
    }

    fun start(hz: Float) {
        this.hz = hz
        running = true
        removeCallbacks(tick)
        post(tick)
    }

    fun stop() {
        running = false
        removeCallbacks(tick)
    }

    private fun drawOnePixel() {
        val surface = holder.surface
        if (!surface.isValid) return
        runCatching {
            val canvas = holder.lockCanvas() ?: return
            canvas.drawColor(Color.argb(NEARLY_TRANSPARENT_ALPHA, 0, 0, 0))
            holder.unlockCanvasAndPost(canvas)
        }
    }

    private fun sendVote() {
        val surface = holder.surface
        if (!surface.isValid || hz <= 0f) return
        runCatching {
            if (Build.VERSION.SDK_INT >= 31) {
                surface.setFrameRate(hz, Surface.FRAME_RATE_COMPATIBILITY_DEFAULT, Surface.CHANGE_FRAME_RATE_ALWAYS)
            } else if (Build.VERSION.SDK_INT >= 30) {
                surface.setFrameRate(hz, Surface.FRAME_RATE_COMPATIBILITY_DEFAULT)
            }
            votesSent++
        }
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        if (running) sendVote()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) = Unit

    override fun surfaceDestroyed(holder: SurfaceHolder) = Unit

    private companion object {
        const val TICK_MILLIS = 250L
        const val NEARLY_TRANSPARENT_ALPHA = 2
    }
}

package eu.kanade.tachiyomi.util.system

import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.view.FrameMetrics
import android.view.Window
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Measures how smooth the screen is while auto scroll runs, so a stutter can be traced to a cause instead of guessed.
 * For every frame it reads the system's frame timings: how long layout, drawing, the sync with the render thread and
 * the GPU took, and how many screen refreshes were skipped between two frames. Totals are shown in the enhancement
 * settings page.
 */
object FrameJankMonitor {

    data class Stats(
        val frames: Int = 0,
        val seconds: Float = 0f,
        /** Screen refreshes with no new frame (a drop from 120 to 60 per second skips every other one). */
        val skippedRefreshes: Int = 0,
        /** Frames that took longer than one refresh to produce. */
        val slowFrames: Int = 0,
        /** Average milliseconds per phase over the slow frames only. */
        val layoutMs: Float = 0f,
        val drawMs: Float = 0f,
        val syncMs: Float = 0f,
        val gpuMs: Float = 0f,
        /** Slow frames that drew new content for the first time (a page or tile that just arrived). */
        val slowFirstDraws: Int = 0,
    ) {
        val fps: Float get() = if (seconds > 0f) frames / seconds else 0f
    }

    private val mutableStats = MutableStateFlow(Stats())
    val stats: StateFlow<Stats> = mutableStats.asStateFlow()

    private var thread: HandlerThread? = null
    private var attachedWindow: Window? = null
    private var listener: Window.OnFrameMetricsAvailableListener? = null

    // Accumulators, touched only from the listener's thread
    private var frames = 0
    private var skipped = 0
    private var slow = 0
    private var slowFirst = 0
    private var layoutNanos = 0L
    private var drawNanos = 0L
    private var syncNanos = 0L
    private var gpuNanos = 0L
    private var firstVsync = 0L
    private var lastVsync = 0L
    private var refreshNanos = 8_333_333L

    @Synchronized
    fun start(window: Window, refreshRate: Float) {
        stop()
        refreshNanos = (1_000_000_000f / refreshRate.coerceAtLeast(30f)).toLong()
        val handlerThread = HandlerThread("FrameJankMonitor").also { it.start() }
        val frameListener = Window.OnFrameMetricsAvailableListener { _, metrics, _ -> record(metrics) }
        runCatching { window.addOnFrameMetricsAvailableListener(frameListener, Handler(handlerThread.looper)) }
            .onFailure {
                handlerThread.quitSafely()
                return
            }
        thread = handlerThread
        attachedWindow = window
        listener = frameListener
    }

    @Synchronized
    fun stop() {
        val frameListener = listener
        val window = attachedWindow
        if (frameListener != null && window != null) {
            runCatching { window.removeOnFrameMetricsAvailableListener(frameListener) }
        }
        thread?.quitSafely()
        thread = null
        attachedWindow = null
        listener = null
        publish()
    }

    @Synchronized
    fun reset() {
        frames = 0
        skipped = 0
        slow = 0
        slowFirst = 0
        layoutNanos = 0L
        drawNanos = 0L
        syncNanos = 0L
        gpuNanos = 0L
        firstVsync = 0L
        lastVsync = 0L
        mutableStats.value = Stats()
    }

    @Synchronized
    private fun record(metrics: FrameMetrics) {
        val vsync = metrics.getMetric(FrameMetrics.VSYNC_TIMESTAMP)
        if (lastVsync != 0L) {
            val missed = ((vsync - lastVsync) / refreshNanos.toFloat() + 0.25f).toInt() - 1
            if (missed > 0) skipped += missed
        } else {
            firstVsync = vsync
        }
        lastVsync = vsync
        frames++

        val total = metrics.getMetric(FrameMetrics.TOTAL_DURATION)
        if (total > refreshNanos) {
            slow++
            layoutNanos += metrics.getMetric(FrameMetrics.LAYOUT_MEASURE_DURATION)
            drawNanos += metrics.getMetric(FrameMetrics.DRAW_DURATION)
            syncNanos += metrics.getMetric(FrameMetrics.SYNC_DURATION)
            if (Build.VERSION.SDK_INT >= 31) gpuNanos += metrics.getMetric(FrameMetrics.GPU_DURATION)
            if (metrics.getMetric(FrameMetrics.FIRST_DRAW_FRAME) == 1L) slowFirst++
        }
        if (frames % PUBLISH_EVERY == 0) publish()
    }

    @Synchronized
    private fun publish() {
        val divisor = slow.coerceAtLeast(1) * 1_000_000f
        mutableStats.value = Stats(
            frames = frames,
            seconds = if (lastVsync > firstVsync) (lastVsync - firstVsync) / 1_000_000_000f else 0f,
            skippedRefreshes = skipped,
            slowFrames = slow,
            layoutMs = layoutNanos / divisor,
            drawMs = drawNanos / divisor,
            syncMs = syncNanos / divisor,
            gpuMs = gpuNanos / divisor,
            slowFirstDraws = slowFirst,
        )
    }

    private const val PUBLISH_EVERY = 120
}

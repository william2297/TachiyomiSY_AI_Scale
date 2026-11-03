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
        /** Seconds spent scrolling: gaps of more than [IDLE_REFRESHES] refreshes (nothing moving) are left out. */
        val activeSeconds: Float = 0f,
        /** Frames that came one refresh after the previous one (smooth). */
        val smooth: Int = 0,
        /** Frames that came two refreshes after the previous one (half rate, for example 60 on a 120 Hz screen). */
        val halfRate: Int = 0,
        /** Frames that came three to [IDLE_REFRESHES] refreshes after the previous one (a visible hitch). */
        val hitches: Int = 0,
        /** Frames that took longer than one refresh to produce, and where that time went (average ms). */
        val slowFrames: Int = 0,
        val inputMs: Float = 0f,
        val animationMs: Float = 0f,
        val layoutMs: Float = 0f,
        val drawMs: Float = 0f,
        val syncMs: Float = 0f,
        val commandMs: Float = 0f,
        val swapMs: Float = 0f,
        val gpuMs: Float = 0f,
        val otherDelayMs: Float = 0f,
        /** The shortest gap between two frames that was reached, in ms: the screen's real refresh period. */
        val shortestGapMs: Float = 0f,
        /** What the device reports: Android version, refresh rate and the modes the screen supports. */
        val deviceInfo: String = "",
    ) {
        val fps: Float get() = if (activeSeconds > 0f) frames / activeSeconds else 0f
    }

    private val mutableStats = MutableStateFlow(Stats())
    val stats: StateFlow<Stats> = mutableStats.asStateFlow()

    private var thread: HandlerThread? = null
    private var attachedWindow: Window? = null
    private var listener: Window.OnFrameMetricsAvailableListener? = null

    // Accumulators, touched only from the listener's thread
    private var frames = 0
    private var smooth = 0
    private var half = 0
    private var hitch = 0
    private var slow = 0
    private var activeNanos = 0L
    private val phaseNanos = LongArray(PHASES)
    private var lastVsync = 0L
    private var refreshNanos = 8_333_333L
    private var shortestGapNanos = Long.MAX_VALUE
    private var deviceInfo = ""

    @Synchronized
    fun start(window: Window, refreshRate: Float, deviceInfo: String = "") {
        stop()
        this.deviceInfo = deviceInfo
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
        smooth = 0
        half = 0
        hitch = 0
        slow = 0
        activeNanos = 0L
        phaseNanos.fill(0L)
        lastVsync = 0L
        shortestGapNanos = Long.MAX_VALUE
        mutableStats.value = Stats()
    }

    @Synchronized
    private fun record(metrics: FrameMetrics) {
        val vsync = metrics.getMetric(FrameMetrics.VSYNC_TIMESTAMP)
        if (lastVsync != 0L) {
            val gap = ((vsync - lastVsync) / refreshNanos.toFloat() + 0.25f).toInt()
            // A long gap means nothing was moving (menu open, finger holding, waiting for a page), not a drop
            if (gap in 1..IDLE_REFRESHES) {
                activeNanos += vsync - lastVsync
                if (vsync - lastVsync in 1 until shortestGapNanos) shortestGapNanos = vsync - lastVsync
                when (gap) {
                    1 -> smooth++
                    2 -> half++
                    else -> hitch++
                }
            }
        }
        lastVsync = vsync
        frames++

        if (metrics.getMetric(FrameMetrics.TOTAL_DURATION) > refreshNanos) {
            slow++
            phaseNanos[0] += metrics.getMetric(FrameMetrics.INPUT_HANDLING_DURATION)
            phaseNanos[1] += metrics.getMetric(FrameMetrics.ANIMATION_DURATION)
            phaseNanos[2] += metrics.getMetric(FrameMetrics.LAYOUT_MEASURE_DURATION)
            phaseNanos[3] += metrics.getMetric(FrameMetrics.DRAW_DURATION)
            phaseNanos[4] += metrics.getMetric(FrameMetrics.SYNC_DURATION)
            phaseNanos[5] += metrics.getMetric(FrameMetrics.COMMAND_ISSUE_DURATION)
            phaseNanos[6] += metrics.getMetric(FrameMetrics.SWAP_BUFFERS_DURATION)
            if (Build.VERSION.SDK_INT >= 31) phaseNanos[7] += metrics.getMetric(FrameMetrics.GPU_DURATION)
            phaseNanos[8] += metrics.getMetric(FrameMetrics.UNKNOWN_DELAY_DURATION)
        }
        if (frames % PUBLISH_EVERY == 0) publish()
    }

    @Synchronized
    private fun publish() {
        val divisor = slow.coerceAtLeast(1) * 1_000_000f
        mutableStats.value = Stats(
            frames = frames,
            activeSeconds = activeNanos / 1_000_000_000f,
            smooth = smooth,
            halfRate = half,
            hitches = hitch,
            slowFrames = slow,
            inputMs = phaseNanos[0] / divisor,
            animationMs = phaseNanos[1] / divisor,
            layoutMs = phaseNanos[2] / divisor,
            drawMs = phaseNanos[3] / divisor,
            syncMs = phaseNanos[4] / divisor,
            commandMs = phaseNanos[5] / divisor,
            swapMs = phaseNanos[6] / divisor,
            gpuMs = phaseNanos[7] / divisor,
            otherDelayMs = phaseNanos[8] / divisor,
            shortestGapMs = if (shortestGapNanos == Long.MAX_VALUE) 0f else shortestGapNanos / 1_000_000f,
            deviceInfo = deviceInfo,
        )
    }

    private const val PUBLISH_EVERY = 120
    private const val PHASES = 9

    /** A gap longer than this many refreshes (about 50 ms at 120 Hz) is a pause, not a stutter. */
    private const val IDLE_REFRESHES = 6
}

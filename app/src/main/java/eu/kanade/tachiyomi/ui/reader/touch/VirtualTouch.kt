package eu.kanade.tachiyomi.ui.reader.touch

import android.app.Instrumentation
import android.os.SystemClock
import android.util.Log
import android.view.InputDevice
import android.view.MotionEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs

/**
 * Taps the reader's own window now and then while auto scroll runs. The Oppo Pad Mini gives the reader 120 frames per
 * second only shortly after a touch, so untouched auto scroll is drawn at 60. The reader swallows these taps (see
 * [consume]), they never turn a page or open the menu.
 *
 * Two ways to make one: the app's own instrumentation, which newer Android versions refuse, and [VirtualTouchService],
 * which the user has to enable in the system's accessibility settings.
 */
object VirtualTouch {

    @Volatile
    internal var service: VirtualTouchService? = null

    val serviceEnabled: Boolean get() = service != null

    @Volatile
    private var instrumentationWorks = true

    @Volatile
    private var expectedX = 0f

    @Volatile
    private var expectedY = 0f

    @Volatile
    private var expectedUntil = 0L

    private var swallowedDownTime = -1L

    /** Virtual taps that reached the reader. */
    @Volatile
    var receivedCount = 0
        private set

    /** For the settings page. */
    val status: String
        get() = when {
            serviceEnabled -> "accessibility service"
            instrumentationWorks && receivedCount > 0 -> "from the app"
            instrumentationWorks -> "not tried yet"
            else -> "blocked by Android, turn on the service"
        }

    val available: Boolean get() = instrumentationWorks || serviceEnabled

    /** Sends one tap at screen coordinates; false when there is no way to send it. */
    suspend fun tap(x: Float, y: Float): Boolean {
        expectedX = x
        expectedY = y
        expectedUntil = SystemClock.uptimeMillis() + EXPECT_MILLIS
        service?.let { return it.tap(x, y) }
        if (!instrumentationWorks) return false
        return withContext(Dispatchers.IO) { tapByInstrumentation(x, y) }
    }

    private fun tapByInstrumentation(x: Float, y: Float): Boolean {
        val downTime = SystemClock.uptimeMillis()
        return try {
            val instrumentation = Instrumentation()
            for (action in intArrayOf(MotionEvent.ACTION_DOWN, MotionEvent.ACTION_UP)) {
                val event = MotionEvent.obtain(downTime, SystemClock.uptimeMillis(), action, x, y, 0)
                event.source = InputDevice.SOURCE_TOUCHSCREEN
                instrumentation.sendPointerSync(event)
                event.recycle()
            }
            true
        } catch (e: Throwable) {
            instrumentationWorks = false
            Log.i(TAG, "instrumentation refused: $e")
            false
        }
    }

    /** True for an event of a virtual tap, which the reader must not act on. Main thread only. */
    fun consume(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_DOWN) {
            // A finger comes from a real device, whose id is positive; injected events have 0 or less
            val virtual = event.deviceId <= 0 &&
                SystemClock.uptimeMillis() < expectedUntil &&
                abs(event.rawX - expectedX) < POSITION_TOLERANCE &&
                abs(event.rawY - expectedY) < POSITION_TOLERANCE
            swallowedDownTime = if (virtual) event.downTime else -1L
            if (virtual) receivedCount++
            return virtual
        }
        return swallowedDownTime != -1L && event.downTime == swallowedDownTime
    }

    const val TAG = "VirtualTouch"
    private const val EXPECT_MILLIS = 1_000L
    private const val POSITION_TOLERANCE = 3f
}

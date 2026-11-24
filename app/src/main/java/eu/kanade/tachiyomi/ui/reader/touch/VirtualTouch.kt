package eu.kanade.tachiyomi.ui.reader.touch

import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs

/**
 * Taps the reader's own window now and then while auto scroll runs. ColorOS on the Oppo Pad Mini halves the frame
 * rate 2.5 seconds after the last touch (touch_idle in /my_product/etc/oplus_vrr_config.json, "oti-hw" in the log),
 * whatever an app asks for. Only a touch that comes through the system restarts that timer: one queued in the app's
 * own window reached the reader but the rate stayed at 60. So the taps are sent by [VirtualTouchService], which the
 * user enables in the accessibility settings. The reader swallows them (see [consume]), they never turn a page.
 */
object VirtualTouch {

    @Volatile
    internal var service: VirtualTouchService? = null

    val serviceEnabled: Boolean get() = service != null

    private var expectedX = 0f
    private var expectedY = 0f
    private var expectedUntil = 0L
    private var swallowedDownTime = -1L

    /** Virtual taps that reached the reader. */
    var receivedCount = 0
        private set

    /** Sends one tap at window coordinates; false when the service is off. */
    fun tap(decor: View, x: Float, y: Float): Boolean {
        val enabledService = service ?: return false
        val location = IntArray(2)
        decor.getLocationOnScreen(location)
        expectedX = location[0] + x
        expectedY = location[1] + y
        expectedUntil = SystemClock.uptimeMillis() + EXPECT_MILLIS
        return enabledService.tap(expectedX, expectedY)
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

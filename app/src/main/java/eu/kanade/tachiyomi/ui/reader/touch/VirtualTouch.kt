package eu.kanade.tachiyomi.ui.reader.touch

import android.os.SystemClock
import android.util.Log
import android.view.InputDevice
import android.view.InputEvent
import android.view.MotionEvent
import android.view.Window
import kotlin.math.abs

/**
 * Taps the reader's own window now and then while auto scroll runs. Android draws a window at the full rate for about
 * 3 seconds after a touch; on the Oppo Pad Mini (120 Hz) a list that scrolls untouched is drawn at 60 (measured with
 * framestats: 8.3 ms a frame up to 2 s after a tap, 16.7 ms from 3 s on). The reader swallows these taps (see
 * [consume]), they never turn a page or open the menu.
 *
 * Two ways to make one: handing the event to the window's own input queue, which needs no permission but uses a
 * method Android may hide, and [VirtualTouchService], which the user enables in the accessibility settings.
 */
object VirtualTouch {

    @Volatile
    internal var service: VirtualTouchService? = null

    val serviceEnabled: Boolean get() = service != null

    var ownWindowWorks = true
        private set

    private var ownDownTime = -1L

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
            ownWindowWorks && receivedCount > 0 -> "from the app"
            ownWindowWorks -> "not tried yet"
            serviceEnabled -> "accessibility service"
            else -> "blocked by Android, turn on the service"
        }

    val available: Boolean get() = ownWindowWorks || serviceEnabled

    /** Sends one tap at window coordinates; false when there is no way to send it. */
    fun tap(window: Window, x: Float, y: Float): Boolean {
        if (ownWindowWorks && tapOwnWindow(window, x, y)) return true
        val enabledService = service ?: return false
        val location = IntArray(2)
        window.decorView.getLocationOnScreen(location)
        expectedX = location[0] + x
        expectedY = location[1] + y
        expectedUntil = SystemClock.uptimeMillis() + EXPECT_MILLIS
        return enabledService.tap(expectedX, expectedY)
    }

    /** The own window method did not deliver its taps; use the service from now on. */
    fun giveUpOwnWindow() {
        ownWindowWorks = false
    }

    /** Queues the tap in the window's root, where a touch from the screen would arrive. */
    private fun tapOwnWindow(window: Window, x: Float, y: Float): Boolean {
        val downTime = SystemClock.uptimeMillis()
        return try {
            val send = findSender(window)
            ownDownTime = downTime
            for (action in intArrayOf(MotionEvent.ACTION_DOWN, MotionEvent.ACTION_UP)) {
                val event = MotionEvent.obtain(downTime, SystemClock.uptimeMillis(), action, x, y, 0)
                event.source = InputDevice.SOURCE_TOUCHSCREEN
                // The window recycles the event when it is done with it
                send(event)
            }
            true
        } catch (e: Throwable) {
            ownWindowWorks = false
            Log.i(TAG, "own window refused: $e, cause ${e.cause}")
            false
        }
    }

    // Checked against the framework of the tablet (Android API 37): both are open to apps, the shorter
    // ViewRootImpl methods are not.
    private fun findSender(window: Window): (MotionEvent) -> Unit {
        val input = InputEvent::class.java
        val errors = StringBuilder()
        runCatching { Window::class.java.getMethod("injectInputEvent", input) }
            .onSuccess { method ->
                return { method.invoke(window, it) }
            }
            .onFailure { errors.append(it).append("; ") }
        val root: Any = window.decorView.parent ?: error("no window root, $errors")
        runCatching {
            root.javaClass.getDeclaredMethod(
                "enqueueInputEvent",
                input,
                Class.forName("android.view.InputEventReceiver"),
                Int::class.javaPrimitiveType,
                Boolean::class.javaPrimitiveType,
            )
        }
            .onSuccess { method ->
                method.isAccessible = true
                return { method.invoke(root, it, null, 0, true) }
            }
            .onFailure { errors.append(it) }
        error("no method: $errors")
    }

    /** True for an event of a virtual tap, which the reader must not act on. Main thread only. */
    fun consume(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_DOWN) {
            // A finger comes from a real device, whose id is positive; injected events have 0 or less
            val fromService = SystemClock.uptimeMillis() < expectedUntil &&
                abs(event.rawX - expectedX) < POSITION_TOLERANCE &&
                abs(event.rawY - expectedY) < POSITION_TOLERANCE
            val virtual = event.deviceId <= 0 && (event.downTime == ownDownTime || fromService)
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

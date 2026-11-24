package eu.kanade.tachiyomi.ui.reader.touch

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Path
import android.util.Log
import android.view.accessibility.AccessibilityEvent

/**
 * Only sends the taps of [VirtualTouch]. It listens to no events and cannot read the screen (see
 * res/xml/virtual_touch_service.xml).
 */
class VirtualTouchService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        VirtualTouch.service = this
        Log.i(VirtualTouch.TAG, "service connected")
    }

    override fun onUnbind(intent: Intent?): Boolean {
        if (VirtualTouch.service === this) VirtualTouch.service = null
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        if (VirtualTouch.service === this) VirtualTouch.service = null
        super.onDestroy()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit

    fun tap(x: Float, y: Float): Boolean {
        val stroke = GestureDescription.StrokeDescription(Path().apply { moveTo(x, y) }, 0L, TAP_MILLIS)
        return runCatching { dispatchGesture(GestureDescription.Builder().addStroke(stroke).build(), null, null) }
            .getOrDefault(false)
    }

    private companion object {
        const val TAP_MILLIS = 10L
    }
}

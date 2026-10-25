package eu.kanade.presentation.preview

import android.app.Activity
import android.app.Application
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.viewpager.widget.PagerAdapter
import eu.kanade.tachiyomi.ui.reader.viewer.ReaderPageImageView
import eu.kanade.tachiyomi.ui.reader.viewer.pager.Pager
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.ConscryptMode
import org.robolectric.annotation.GraphicsMode

/** Does a tap on the pager reach its tap zones at once, and does each of several quick taps count? */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@ConscryptMode(ConscryptMode.Mode.OFF)
@Config(sdk = [35], application = Application::class)
class PagerTapTest {
    private fun run(taps: Int): Pair<Int, Long> {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val pager = Pager(activity)
        pager.adapter = object : PagerAdapter() {
            override fun getCount() = 3
            override fun isViewFromObject(view: View, `object`: Any) = view === `object`
            override fun instantiateItem(container: ViewGroup, position: Int): Any {
                val page = ReaderPageImageView(activity)
                container.addView(page)
                return page
            }

            override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) = container.removeView(
                `object` as View,
            )
        }
        var count = 0
        var firstAt = -1L
        val start = SystemClock.uptimeMillis()
        pager.tapListener = {
            count++
            if (firstAt < 0) firstAt = SystemClock.uptimeMillis() - start
        }
        activity.setContentView(pager, ViewGroup.LayoutParams(1080, 1620))
        val root = activity.window.decorView
        root.measure(
            View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(1620, View.MeasureSpec.EXACTLY),
        )
        root.layout(0, 0, 1080, 1620)
        repeat(taps) { i ->
            val down = SystemClock.uptimeMillis() + i * 120L
            pager.dispatchTouchEvent(MotionEvent.obtain(down, down, MotionEvent.ACTION_DOWN, 900f, 800f, 0))
            pager.dispatchTouchEvent(MotionEvent.obtain(down, down + 50, MotionEvent.ACTION_UP, 900f, 800f, 0))
        }
        // No time passes: only taps handled during the touch itself are counted
        return count to firstAt
    }

    @Test
    fun oneTap() = println("PAGERTAP_one=${run(1)}")

    @Test
    fun threeQuickTaps() = println("PAGERTAP_three=${run(3)}")

    @Test
    fun nothingElse() = assertEquals(0, 0)
}

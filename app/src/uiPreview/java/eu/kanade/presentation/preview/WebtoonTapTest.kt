package eu.kanade.presentation.preview

import android.app.Activity
import android.app.Application
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.widget.AppCompatImageView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import eu.kanade.tachiyomi.ui.reader.viewer.webtoon.WebtoonFrame
import eu.kanade.tachiyomi.ui.reader.viewer.webtoon.WebtoonPageImageView
import eu.kanade.tachiyomi.ui.reader.viewer.webtoon.WebtoonRecyclerView
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.ConscryptMode
import org.robolectric.annotation.GraphicsMode

/** Does a tap on the webtoon list reach the tap zones for the kinds of page view the reader puts in it? */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@ConscryptMode(ConscryptMode.Mode.OFF)
@Config(sdk = [35], application = Application::class)
class WebtoonTapTest {
    private fun taps(beforeTap: (WebtoonRecyclerView) -> Unit = {}, makeChild: (android.content.Context) -> View): Int {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        activity.setTheme(androidx.appcompat.R.style.Theme_AppCompat)
        val context: android.content.Context = activity
        val frame = WebtoonFrame(context)
        val recycler = WebtoonRecyclerView(context)
        recycler.layoutManager = LinearLayoutManager(context)
        recycler.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                val page = FrameLayout(context)
                page.layoutParams = RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 900)
                page.addView(
                    makeChild(context),
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
                return object : RecyclerView.ViewHolder(page) {}
            }

            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) = Unit
            override fun getItemCount() = 6
        }
        frame.addView(recycler, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        var count = 0
        recycler.tapListener = { count++ }
        activity.setContentView(frame, ViewGroup.LayoutParams(1080, 1620))
        val root = activity.window.decorView
        root.measure(
            View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(1620, View.MeasureSpec.EXACTLY),
        )
        root.layout(0, 0, 1080, 1620)
        println(
            "ATTACHED=${frame.isAttachedToWindow} H=${frame.height} W=${recycler.width} kids=${recycler.childCount}",
        )

        beforeTap(recycler)
        val down = SystemClock.uptimeMillis()
        frame.dispatchTouchEvent(MotionEvent.obtain(down, down, MotionEvent.ACTION_DOWN, 900f, 800f, 0))
        frame.dispatchTouchEvent(MotionEvent.obtain(down, down + 60, MotionEvent.ACTION_UP, 900f, 800f, 0))
        return count
    }

    @Test
    fun plainView() = println("TAP_plainView=${taps(makeChild = { View(it) })}")

    @Test
    fun defaultSubsamplingView() = println("TAP_defaultSsiv=${taps { SubsamplingScaleImageView(it) }}")

    @Test
    fun defaultSubsamplingViewWithClick() =
        println("TAP_ssivWithClick=${taps { SubsamplingScaleImageView(it).apply { setOnClickListener {} } }}")

    @Test
    fun webtoonPageView() = println("TAP_webtoonPage=${taps { WebtoonPageImageView(it) }}")

    @Test
    fun clickableImageView() = println(
        "TAP_clickableImage=${taps {
            AppCompatImageView(it).apply { setOnClickListener {} }
        }}",
    )

    @Test
    fun realPageViewDefault() = println(
        "TAP_realPage=${taps {
            eu.kanade.tachiyomi.ui.reader.viewer.ReaderPageImageView(it, isWebtoon = true)
        }}",
    )

    @Test
    fun realPageViewSubsampling() {
        val result = taps {
            eu.kanade.tachiyomi.ui.reader.viewer.ReaderPageImageView(it, isWebtoon = true).also { view ->
                val method = view.javaClass.getDeclaredMethod("prepareNonAnimatedImageView")
                method.isAccessible = true
                method.invoke(view)
            }
        }
        println("TAP_realPageSsiv=$result")
    }

    @Test
    fun tapThatStopsAFlickDoesNotAct() {
        val count = taps({
            it.onManualScroll()
            it.smoothScrollBy(0, 600)
        }) { View(it) }
        println("TAP_stopsFlick=$count")
        assertEquals(0, count)
    }

    @Test
    fun tapDuringAScrollThatIsNotAFlickStillActs() {
        val count = taps({ it.smoothScrollBy(0, 600) }) { View(it) }
        println("TAP_duringTapScroll=$count")
        assertEquals(1, count)
    }

    @Test
    fun tapAfterAFlickEndedActs() {
        val count = taps({ it.onManualScroll() }) { View(it) }
        println("TAP_afterFlick=$count")
        assertEquals(1, count)
    }

    @Test
    fun nothingElse() = assertEquals(0, 0)
}

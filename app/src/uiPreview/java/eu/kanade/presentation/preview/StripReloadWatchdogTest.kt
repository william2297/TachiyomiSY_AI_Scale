package eu.kanade.presentation.preview

import android.app.Activity
import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import eu.kanade.tachiyomi.data.coil.ImageDecoder
import eu.kanade.tachiyomi.ui.reader.viewer.webtoon.WebtoonStripImageView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.ConscryptMode
import org.robolectric.annotation.GraphicsMode
import java.time.Duration

/** A page that kept its size but lost its pixels asks to be decoded again, a few times, until it has them. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@ConscryptMode(ConscryptMode.Mode.OFF)
@Config(sdk = [35], application = Application::class)
class StripReloadWatchdogTest {

    private fun blankPage(reloads: MutableList<Int>, restoreOn: Int?): Pair<FrameLayout, WebtoonStripImageView> {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val holder = FrameLayout(activity)
        activity.setContentView(holder, ViewGroup.LayoutParams(1080, 1620))
        fun page() = ImageDecoder.StripsImage(listOf(Bitmap.createBitmap(100, 200, Bitmap.Config.ARGB_8888)))
        // A tiny limit makes any page "large", so leaving the screen frees its pixels
        val view = WebtoonStripImageView(activity, largePageBytes = 1)
        holder.addView(view, ViewGroup.LayoutParams(1080, 2000))
        view.setImage(page())
        view.onReload = {
            reloads += 1
            if (restoreOn != null && reloads.size == restoreOn) view.setImage(page())
        }
        return holder to view
    }

    private fun drawFrame(view: View) {
        view.measure(
            View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(2000, View.MeasureSpec.EXACTLY),
        )
        view.layout(0, 0, 1080, 2000)
        view.draw(Canvas(Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)))
    }

    @Test
    fun aReloadThatNeverArrivesIsAskedForAgainAFewTimesThenGivenUp() {
        val reloads = mutableListOf<Int>()
        val (holder, view) = blankPage(reloads, restoreOn = null)
        holder.removeView(view) // off screen: the pixels are freed
        holder.addView(view) // back on screen: the first reload is requested
        assertEquals(1, reloads.size)
        repeat(8) {
            drawFrame(view)
            shadowOf(android.os.Looper.getMainLooper()).idleFor(Duration.ofMillis(2_000))
        }
        println("RELOAD_giveUp=${reloads.size}")
        assertEquals(4, reloads.size)
    }

    @Test
    fun aPageThatComesBackIsNotAskedAgain() {
        val reloads = mutableListOf<Int>()
        val (holder, view) = blankPage(reloads, restoreOn = 2)
        holder.removeView(view)
        holder.addView(view)
        repeat(6) {
            drawFrame(view)
            shadowOf(android.os.Looper.getMainLooper()).idleFor(Duration.ofMillis(2_000))
        }
        println("RELOAD_recovers=${reloads.size}")
        assertTrue(reloads.size == 2)
    }
}

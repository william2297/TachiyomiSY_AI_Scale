package eu.kanade.presentation.preview

import android.app.Activity
import android.app.Application
import android.graphics.Bitmap
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import eu.kanade.tachiyomi.ui.reader.viewer.webtoon.WebtoonFrame
import eu.kanade.tachiyomi.ui.reader.viewer.webtoon.WebtoonPageImageView
import eu.kanade.tachiyomi.ui.reader.viewer.webtoon.WebtoonRecyclerView
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.ConscryptMode
import org.robolectric.annotation.GraphicsMode

/** A page holds a picture and, while the enhanced version loads, a second image view that has no image yet. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@ConscryptMode(ConscryptMode.Mode.OFF)
@Config(sdk = [35], application = Application::class)
class WebtoonPageHeightTest {
    private fun pageHeight(makeSecond: (android.content.Context) -> View): Int {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val frame = WebtoonFrame(activity)
        val recycler = WebtoonRecyclerView(activity)
        recycler.layoutParams =
            ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        recycler.layoutManager = LinearLayoutManager(activity)
        recycler.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                val page = FrameLayout(activity)
                page.layoutParams =
                    RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                val first = WebtoonPageImageView(activity)
                page.addView(first, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                first.setImage(ImageSource.bitmap(Bitmap.createBitmap(1080, 1350, Bitmap.Config.ARGB_8888)))
                page.addView(
                    makeSecond(activity),
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
                return object : RecyclerView.ViewHolder(page) {}
            }

            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) = Unit
            override fun getItemCount() = 3
        }
        frame.layoutParams = ViewGroup.LayoutParams(1080, 1620)
        frame.addView(recycler)
        activity.setContentView(frame)
        repeat(5) {
            shadowOf(android.os.Looper.getMainLooper()).idle()
            val root = activity.window.decorView
            root.measure(
                View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(1620, View.MeasureSpec.EXACTLY),
            )
            root.layout(0, 0, 1080, 1620)
        }
        return recycler.getChildAt(0).height
    }

    @Test
    fun withPlainSecondView() = println("PAGE_plainSecond=${pageHeight { SubsamplingScaleImageView(it) }}")

    @Test
    fun withWebtoonSecondView() {
        val height = pageHeight { WebtoonPageImageView(it) }
        println("PAGE_webtoonSecond=$height")
        assertEquals(1350, height)
    }
}

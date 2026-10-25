package eu.kanade.presentation.preview

import android.app.Application
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.test.core.app.ApplicationProvider
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import eu.kanade.tachiyomi.ui.reader.viewer.webtoon.WebtoonPageImageView
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.ConscryptMode
import org.robolectric.annotation.GraphicsMode

/** Shows how tall an image view that has no image yet measures inside a wrap-content page frame. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@ConscryptMode(ConscryptMode.Mode.OFF)
@Config(sdk = [35], application = Application::class)
class NotReadyImageMeasureTest {
    private fun measuredHeight(view: View): Int {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val frame = FrameLayout(context)
        frame.addView(view, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        frame.measure(
            View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(1620, View.MeasureSpec.AT_MOST),
        )
        return frame.measuredHeight
    }

    @Test
    fun webtoonPageViewWithoutImageTakesNoHeight() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        assertEquals(1620, measuredHeight(SubsamplingScaleImageView(context)))
        assertEquals(0, measuredHeight(WebtoonPageImageView(context)))
    }

    @Test
    fun plainViewWithoutImageClaimsTheOfferedHeight() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val frame = FrameLayout(context)
        frame.addView(
            SubsamplingScaleImageView(context),
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
        )
        frame.measure(
            View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(1620, View.MeasureSpec.AT_MOST),
        )
        println("MEASURED_HEIGHT_AT_MOST=${frame.measuredHeight}")
        frame.measure(
            View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
        )
        println("MEASURED_HEIGHT_UNSPECIFIED=${frame.measuredHeight}")
        assertEquals(1080, frame.measuredWidth)
    }
}

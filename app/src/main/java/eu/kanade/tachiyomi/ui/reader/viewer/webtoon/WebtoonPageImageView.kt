package eu.kanade.tachiyomi.ui.reader.viewer.webtoon

import android.content.Context
import android.view.MotionEvent
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView

/**
 * Page image for the webtoon list. It never takes touches, like [WebtoonStripImageView]: taps, scrolling and pinch
 * all belong to the recycler, and a touchable page view would swallow every tap.
 */
internal class WebtoonPageImageView(context: Context) : SubsamplingScaleImageView(context) {

    override fun onTouchEvent(event: MotionEvent): Boolean = false

    // Until an image is decoded, a plain SubsamplingScaleImageView measures as tall as the space it is offered, which
    // is a whole screen in the webtoon list. A page whose enhanced image is still loading, or never loads, then showed
    // as a black block under (or instead of) the picture. Size it by its content only: nothing until the image is ready.
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if (sWidth <= 0 || sHeight <= 0) {
            setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), 0)
        } else {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        }
    }
}

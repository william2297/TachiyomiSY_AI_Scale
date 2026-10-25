package eu.kanade.tachiyomi.ui.reader.viewer

import android.content.Context
import android.view.View
import androidx.appcompat.widget.AppCompatImageView

/**
 * Image view whose height follows its picture's shape when the height is not fixed.
 *
 * A plain ImageView measures a free height as the picture's own pixel height. The enhanced preview is twice as large
 * as the page it replaces, so in the webtoon list it made the page twice as tall and the picture, centered inside,
 * left empty bars above and below it.
 */
internal class AspectFitImageView(context: Context) : AppCompatImageView(context) {

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val drawable = drawable
        if (drawable != null && drawable.intrinsicWidth > 0 && drawable.intrinsicHeight > 0 &&
            MeasureSpec.getMode(heightMeasureSpec) != MeasureSpec.EXACTLY
        ) {
            val width = View.resolveSize(drawable.intrinsicWidth, widthMeasureSpec)
            val height = (width.toLong() * drawable.intrinsicHeight / drawable.intrinsicWidth).toInt()
            setMeasuredDimension(width, height)
        } else {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        }
    }
}

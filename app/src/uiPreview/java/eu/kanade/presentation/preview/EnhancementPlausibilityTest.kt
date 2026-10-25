package eu.kanade.presentation.preview

import android.graphics.Bitmap
import android.graphics.Color
import eu.kanade.tachiyomi.util.waifu2x.ImageEnhancementCache
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.ConscryptMode
import org.robolectric.annotation.GraphicsMode

/** Checks that enhanced results with a black band, or a different shape, are rejected and good ones are kept. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@ConscryptMode(ConscryptMode.Mode.OFF)
@Config(sdk = [35], application = android.app.Application::class)
class EnhancementPlausibilityTest {
    private fun page(width: Int, height: Int, blackFromRow: Int = height): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(width * height) { index ->
            val y = index / width
            if (y >= blackFromRow) Color.BLACK else Color.rgb(120 + (index % 90), 100, 80 + y % 100)
        }
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return bitmap
    }

    @Test
    fun keepsAGoodResult() {
        assertTrue(ImageEnhancementCache.isPlausibleEnhancement(page(540, 810), page(1080, 1620)))
    }

    @Test
    fun rejectsTenPercentBlackBandAtTheBottom() {
        val result = page(1080, 1620, blackFromRow = 1458)
        assertFalse(ImageEnhancementCache.isPlausibleEnhancement(page(540, 810), result))
    }

    @Test
    fun rejectsThreePercentBlackBandAtTheBottom() {
        val result = page(1080, 1620, blackFromRow = 1570)
        assertFalse(ImageEnhancementCache.isPlausibleEnhancement(page(540, 810), result))
    }

    @Test
    fun rejectsPaddedResult() {
        assertFalse(ImageEnhancementCache.isPlausibleEnhancement(page(540, 810), page(1080, 1800)))
    }

    @Test
    fun rejectsFullyBlackResult() {
        assertFalse(ImageEnhancementCache.isPlausibleEnhancement(page(540, 810), page(1080, 1620, blackFromRow = 0)))
    }

    @Test
    fun keepsAResultWhoseSourceAlsoHasADarkBand() {
        val source = page(540, 810, blackFromRow = 700)
        val result = page(1080, 1620, blackFromRow = 1400)
        assertTrue(ImageEnhancementCache.isPlausibleEnhancement(source, result))
    }
}

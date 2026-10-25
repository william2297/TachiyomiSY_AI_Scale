package eu.kanade.tachiyomi.util.waifu2x

import eu.kanade.tachiyomi.ui.reader.setting.ReaderPreferences

/**
 * One-tap settings for large tablets, tuned for the Oppo Pad Mini (8.8", 2520x1680, Snapdragon).
 *
 * The values come from reading the enhancement code, not from benchmarks on the device:
 * - Real-CUGAN SE 2x is the lightest model the NPU supports, with conservative denoise so line art keeps its edges.
 * - Only the width is capped for processing. A height cap would shrink tall webtoon strips before they are
 *   enlarged, so the output barely exceeds the source.
 * - Sources already wider than the screen's short side gain nothing from enhancement, so they are skipped.
 */
object EnhancementPreset {
    private const val MAX_PROCESSING_WIDTH = 1200

    fun apply(preferences: ReaderPreferences, screenWidthPx: Int, screenHeightPx: Int) {
        val shortSide = minOf(screenWidthPx, screenHeightPx)
        // Never falls back to Vulkan, which force-closes the app on this tablet; without an NPU it leaves the
        // backend as it is.
        if (Waifu2x.isQualcommNpuAvailable()) {
            preferences.realCuganProcessingBackend().set(Waifu2x.PROCESSING_BACKEND_QUALCOMM_NPU)
        }

        preferences.realCuganEnabled().set(true)
        preferences.realCuganModel().set(0)
        preferences.realCuganScale().set(2)
        preferences.realCuganNoiseLevel().set(4)
        preferences.realCuganPrecision().set(0)
        preferences.realCuganPreloadSize().set(3)
        preferences.realCuganMaxSizeWidth().set(MAX_PROCESSING_WIDTH)
        preferences.realCuganMaxSizeHeight().set(0)
        preferences.realCuganSkipMaxSizeWidth().set(shortSide)
        preferences.realCuganSkipMaxSizeHeight().set(0)
    }
}

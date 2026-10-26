package eu.kanade.tachiyomi.util.waifu2x

import eu.kanade.tachiyomi.ui.reader.setting.ReaderPreferences

/**
 * One-tap settings for large tablets, tuned for the Oppo Pad Mini (8.8", 2520x1680, Snapdragon).
 *
 * The values come from reading the enhancement code, not from benchmarks on the device:
 * - Real-CUGAN SE 2x is the lightest model the NPU supports, with conservative denoise so line art keeps its edges.
 * - Processing is capped at 1600x1600 and sources above 2560x2560 are skipped. These are the values that ended the
 *   black bars between pages on the tablet (tested by the owner); the earlier 1200 wide, no height cap did not.
 */
object EnhancementPreset {
    private const val MAX_PROCESSING_SIZE = 1600
    private const val SKIP_ABOVE_SIZE = 2560

    // No enhancing ahead in the background: it kept the NPU busy for the whole chapter and heated the tablet
    private const val BACKGROUND_AHEAD = 0

    fun apply(preferences: ReaderPreferences, screenWidthPx: Int, screenHeightPx: Int) {
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
        preferences.realCuganBackgroundAhead().set(BACKGROUND_AHEAD)
        preferences.realCuganMaxSizeWidth().set(MAX_PROCESSING_SIZE)
        preferences.realCuganMaxSizeHeight().set(MAX_PROCESSING_SIZE)
        preferences.realCuganSkipMaxSizeWidth().set(SKIP_ABOVE_SIZE)
        preferences.realCuganSkipMaxSizeHeight().set(SKIP_ABOVE_SIZE)
    }
}

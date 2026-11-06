package eu.kanade.tachiyomi.ui.reader.viewer.webtoon

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Counts the frames in which part of a webtoon page on screen had no picture yet, and why. The overlay and the
 * enhancement settings page show it, so a black area can be traced to one of three causes:
 * the page itself is not loaded, the page is loaded but not yet cut into segments, or a segment is still decoding.
 */
object PageTrace {

    enum class Reason(val label: String) {
        NOT_READY("not loaded"),
        NOT_BOUND("splitting"),
        DECODING("decoding"),
    }

    data class Snapshot(
        val frames: Int = 0,
        val blankFrames: Int = 0,
        val notReady: Int = 0,
        val notBound: Int = 0,
        val decoding: Int = 0,
        val episodes: Int = 0,
        val longestMillis: Long = 0,
        val longestWhat: String = "-",
    )

    private val mutable = MutableStateFlow(Snapshot())
    val snapshot: StateFlow<Snapshot> = mutable.asStateFlow()

    private var episodeStartNanos = 0L
    private var episodeWhat = "-"

    /** Called once per drawn frame; [reason] is null when everything visible has its picture. */
    @Synchronized
    fun sample(reason: Reason?, what: String, nowNanos: Long) {
        var s = mutable.value.copy(frames = mutable.value.frames + 1)
        if (reason != null) {
            s = s.copy(
                blankFrames = s.blankFrames + 1,
                notReady = s.notReady + if (reason == Reason.NOT_READY) 1 else 0,
                notBound = s.notBound + if (reason == Reason.NOT_BOUND) 1 else 0,
                decoding = s.decoding + if (reason == Reason.DECODING) 1 else 0,
            )
            if (episodeStartNanos == 0L) {
                episodeStartNanos = nowNanos
                s = s.copy(episodes = s.episodes + 1)
            }
            episodeWhat = "${reason.label} $what"
            val running = (nowNanos - episodeStartNanos) / 1_000_000L
            if (running > s.longestMillis) s = s.copy(longestMillis = running, longestWhat = episodeWhat)
        } else {
            episodeStartNanos = 0L
        }
        mutable.value = s
    }

    @Synchronized
    fun reset() {
        episodeStartNanos = 0L
        mutable.value = Snapshot()
    }
}

package eu.kanade.tachiyomi.ui.reader.viewer.webtoon

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Counts what happens to the strips of webtoon pages, so a page that shows blank for a moment can be traced to a cause:
 * strips freed when a big page leaves the screen, decoded again when it comes back, and how long a page stayed blank.
 * Shown in the enhancement settings page.
 */
object StripStats {

    data class Snapshot(
        val freed: Int = 0,
        val reloaded: Int = 0,
        val blankEpisodes: Int = 0,
        val longestBlankMillis: Long = 0,
    )

    private val mutable = MutableStateFlow(Snapshot())
    val snapshot: StateFlow<Snapshot> = mutable.asStateFlow()

    fun freed() = mutable.update { it.copy(freed = it.freed + 1) }

    fun reloaded() = mutable.update { it.copy(reloaded = it.reloaded + 1) }

    fun blankStarted() = mutable.update { it.copy(blankEpisodes = it.blankEpisodes + 1) }

    fun blankEnded(millis: Long) = mutable.update { it.copy(longestBlankMillis = maxOf(it.longestBlankMillis, millis)) }

    fun reset() {
        mutable.value = Snapshot()
    }

    private inline fun MutableStateFlow<Snapshot>.update(block: (Snapshot) -> Snapshot) {
        while (true) {
            val current = value
            if (compareAndSet(current, block(current))) return
        }
    }
}

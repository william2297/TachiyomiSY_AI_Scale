package eu.kanade.tachiyomi.data.discover

import eu.kanade.presentation.browse.discover.DiscoverSection
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import java.io.IOException
import kotlin.random.Random

/**
 * Builds the Discover lists: reads AniList lists, picks a few per row, finds each pick in the user's sources, and
 * returns the new cache. It never throws for a single failed search; it throws [IOException] only when the main
 * trending list cannot be read at all, so the caller can keep showing the old picks.
 */
class DiscoverRefresher(
    private val api: DiscoverListApi,
    private val searcher: TitleSearcher,
    private val now: () -> Long = { System.currentTimeMillis() },
    private val pause: suspend (Long) -> Unit = { delay(it) },
    private val countries: List<String> = listOf("KR", "JP"),
    private val maxSearches: Int = DEFAULT_MAX_SEARCHES,
    /** Titles the user already has or has read; they are left out before anything is searched. */
    private val isExcluded: (AniListTitle) -> Boolean = { false },
    /** Called with the picks found so far, so the tab can fill in while the rest is still being searched. */
    private val onProgress: suspend (DiscoverCache) -> Unit = {},
    /** The matching stops after this long, so a run always ends before the system cuts it off. */
    private val maxDurationMillis: Long = DEFAULT_MAX_DURATION_MILLIS,
    /** A short line on what is happening, for the tab to show while it waits. */
    private val onStatus: suspend (String) -> Unit = {},
) {

    suspend fun refresh(previous: DiscoverCache, sources: List<SearchSource>): DiscoverCache {
        val day = now() / DAY_MILLIS
        val lists = fetchLists().mapValues { (_, titles) -> titles.filterNot(isExcluded) }
        val picks = pickCandidates(lists, previous, day)

        val sourcesKey = sources.joinToString(",") { it.id.toString() }
        // The 12 shown in each row are searched first; the rest of the 50 fill in over the next days
        val ordered = (picks.values.flatMap { it.take(DISPLAY_SIZE) } + picks.values.flatMap { it.drop(DISPLAY_SIZE) })
            .distinctBy { it.id }
        val sourceIds = sources.map(SearchSource::id)

        fun sectionsFor(matches: Map<Long, MatchRecord>): Map<String, List<StoredEntry>> {
            val sections = linkedMapOf<String, List<StoredEntry>>()
            for ((section, titles) in picks) {
                val entries = titles.withIndex().mapNotNull { (index, title) ->
                    val match = matches[title.id]?.takeIf { it.found && it.sourceId in sourceIds }
                    match?.let { toEntry(section, title, it, rankOf(section, lists, title, index)) }
                }
                if (entries.isNotEmpty()) {
                    sections[section.name] = entries.take(MAX_PER_SECTION)
                } else {
                    // Keep what was shown before when this row could not be rebuilt (for example the list failed)
                    previous.sections[section.name]?.let { sections[section.name] = it }
                }
            }
            return sections
        }

        val matches = match(ordered, previous.matches, sources, sourcesKey) { found ->
            onProgress(previous.copy(updatedAtMillis = now(), sections = sectionsFor(found), errorMessage = null))
        }

        return DiscoverCache(
            updatedAtMillis = now(),
            sections = sectionsFor(matches),
            previousTrendingIds = lists[DiscoverSection.RISING].orEmpty().take(TRENDING_MEMORY).map { it.id },
            matches = matches.entries.sortedByDescending { it.value.checkedAtMillis }.take(MAX_MATCHES)
                .associate { it.key to it.value },
        )
    }

    private suspend fun fetchLists(): Map<DiscoverSection, List<AniListTitle>> {
        val result = linkedMapOf<DiscoverSection, List<AniListTitle>>()
        onStatus("Reading AniList's lists...")
        val sections = listOf(
            DiscoverSection.RISING,
            DiscoverSection.COMPLETED,
            DiscoverSection.CLASSICS,
            DiscoverSection.HIDDEN_GEMS,
            DiscoverSection.OLDER_ONGOING,
        )
        for (section in sections) {
            val merged = mutableListOf<AniListTitle>()
            for (country in countries) {
                try {
                    merged += api.fetch(section, country)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: IOException) {
                    // Without the trending list there is nothing to show; the other rows may fail one by one
                    if (section == DiscoverSection.RISING && country == countries.first()) throw e
                }
                pause(API_PAUSE_MILLIS)
            }
            result[section] = sorted(section, merged.distinctBy { it.id })
        }
        return result
    }

    private fun sorted(section: DiscoverSection, titles: List<AniListTitle>): List<AniListTitle> {
        return if (section == DiscoverSection.RISING) {
            titles.sortedByDescending { it.trending }
        } else {
            titles.sortedWith(compareByDescending<AniListTitle> { it.score ?: 0 }.thenByDescending { it.popularity })
        }
    }

    private fun pickCandidates(
        lists: Map<DiscoverSection, List<AniListTitle>>,
        previous: DiscoverCache,
        day: Long,
    ): Map<DiscoverSection, List<AniListTitle>> {
        val rising = lists[DiscoverSection.RISING].orEmpty()
        val previousTop = previous.previousTrendingIds.toSet()
        val picks = linkedMapOf<DiscoverSection, List<AniListTitle>>()
        picks[DiscoverSection.RISING] = rising.take(MAX_PER_SECTION)
        // Needs yesterday's list; the first refresh has nothing to compare with
        picks[DiscoverSection.NEW_IN_TOP] = if (previousTop.isEmpty()) {
            emptyList()
        } else {
            rising.take(NEW_IN_TOP_RANKS).filter { it.id !in previousTop }.take(MAX_PER_SECTION)
        }
        for (section in listOf(
            DiscoverSection.COMPLETED,
            DiscoverSection.CLASSICS,
            DiscoverSection.HIDDEN_GEMS,
            DiscoverSection.OLDER_ONGOING,
        )) {
            picks[section] =
                rotate(lists[section].orEmpty().take(POOL_SIZE), MAX_PER_SECTION, day * 31 + section.ordinal)
        }
        return picks
    }

    private suspend fun match(
        candidates: List<AniListTitle>,
        known: Map<Long, MatchRecord>,
        sources: List<SearchSource>,
        sourcesKey: String,
        progress: suspend (Map<Long, MatchRecord>) -> Unit,
    ): Map<Long, MatchRecord> {
        val matches = known.toMutableMap()
        if (sources.isEmpty()) return matches
        val startedAt = now()
        val failures = mutableMapOf<Long, Int>()
        var searches = 0
        var sinceProgress = 0
        var checked = 0
        for (title in candidates) {
            onStatus(statusLine(checked, candidates.size, matches, candidates, failures, sources))
            checked++
            if (isFresh(matches[title.id], sourcesKey)) continue
            var found: MatchRecord? = null
            var interrupted = false
            for (source in sources) {
                if ((failures[source.id] ?: 0) >= MAX_SOURCE_FAILURES) continue
                if (searches >= maxSearches || now() - startedAt > maxDurationMillis) {
                    interrupted = true
                    break
                }
                searches++
                val results = try {
                    searcher.search(source, title.title)
                } catch (e: Exception) {
                    // A cancellation of the whole run must still stop it; a stray one inside a single search must not
                    if (e is CancellationException) currentCoroutineContext().ensureActive()
                    failures[source.id] = (failures[source.id] ?: 0) + 1
                    emptyList()
                }
                pause(SEARCH_PAUSE_MILLIS)
                val best = TitleMatcher.best(title.names, results) { it.title }
                if (best != null) {
                    failures[source.id] = 0
                    found = MatchRecord(true, source.id, source.name, best.first.url, now(), sourcesKey)
                    break
                }
            }
            // A title the budget cut short is searched again next time, not remembered as missing
            if (found != null) {
                matches[title.id] = found
                if (++sinceProgress >= PROGRESS_EVERY) {
                    sinceProgress = 0
                    progress(matches)
                }
            } else if (!interrupted) {
                matches[title.id] = MatchRecord(false, checkedAtMillis = now(), sourcesKey = sourcesKey)
            }
            if (interrupted) break
        }
        return matches
    }

    private fun statusLine(
        checked: Int,
        total: Int,
        matches: Map<Long, MatchRecord>,
        candidates: List<AniListTitle>,
        failures: Map<Long, Int>,
        sources: List<SearchSource>,
    ): String {
        val found = candidates.count { matches[it.id]?.found == true }
        val skipped = sources.filter { (failures[it.id] ?: 0) >= MAX_SOURCE_FAILURES }.joinToString { it.name }
        return buildString {
            append("Searching your sources: $checked of $total titles checked, $found found.")
            if (skipped.isNotEmpty()) append(" Not responding, skipped: $skipped.")
        }
    }

    private fun isFresh(record: MatchRecord?, sourcesKey: String): Boolean {
        if (record == null || record.sourcesKey != sourcesKey) return false
        val age = now() - record.checkedAtMillis
        return age < if (record.found) FOUND_VALID_MILLIS else MISSING_VALID_MILLIS
    }

    private fun rankOf(
        section: DiscoverSection,
        lists: Map<DiscoverSection, List<AniListTitle>>,
        title: AniListTitle,
        index: Int,
    ): Int {
        return when (section) {
            DiscoverSection.RISING, DiscoverSection.NEW_IN_TOP ->
                lists[DiscoverSection.RISING].orEmpty().indexOfFirst { it.id == title.id } + 1
            else -> index + 1
        }
    }

    private fun toEntry(section: DiscoverSection, title: AniListTitle, match: MatchRecord, rank: Int): StoredEntry {
        return StoredEntry(
            anilistId = title.id,
            title = title.title,
            coverUrl = title.coverUrl,
            sourceId = match.sourceId,
            sourceName = match.sourceName,
            mangaUrl = match.mangaUrl,
            score = title.score,
            completed = title.finished,
            reason = reason(section, title, rank),
            names = title.names,
        )
    }

    companion object {
        /** Cards shown in a row before "See all". */
        const val DISPLAY_SIZE = 12

        /** Most titles a row can hold; "See all" opens up to this many. */
        const val MAX_PER_SECTION = 50
        const val DEFAULT_MAX_SEARCHES = 250
        const val DEFAULT_MAX_DURATION_MILLIS = 7 * 60 * 1000L
        private const val MAX_SOURCE_FAILURES = 3
        private const val PROGRESS_EVERY = 4
        private const val NEW_IN_TOP_RANKS = 50
        private const val TRENDING_MEMORY = 100
        private const val POOL_SIZE = 100
        private const val MAX_MATCHES = 1500
        private const val DAY_MILLIS = 86_400_000L
        private const val FOUND_VALID_MILLIS = 30 * DAY_MILLIS
        private const val MISSING_VALID_MILLIS = 14 * DAY_MILLIS
        private const val API_PAUSE_MILLIS = 2_500L
        private const val SEARCH_PAUSE_MILLIS = 400L

        /** A fixed pick of [count] from [pool] for one day: stable through the day, different the next. */
        fun <T> rotate(pool: List<T>, count: Int, seed: Long): List<T> {
            return pool.shuffled(Random(seed)).take(count)
        }

        fun reason(section: DiscoverSection, title: AniListTitle, rank: Int): String {
            return when (section) {
                DiscoverSection.RISING -> "Trending #$rank on AniList"
                DiscoverSection.NEW_IN_TOP -> "Entered the top 50 at #$rank"
                DiscoverSection.COMPLETED -> completedReason(title)
                DiscoverSection.CLASSICS -> "Rated ${title.score ?: "-"} by ${readers(title.popularity)} readers"
                DiscoverSection.HIDDEN_GEMS ->
                    "Score ${title.score ?: "-"} with only ${readers(title.popularity)} readers"
                DiscoverSection.OLDER_ONGOING ->
                    "Running since ${title.startYear ?: "long ago"}, score ${title.score ?: "-"}"
            }
        }

        private fun completedReason(title: AniListTitle): String {
            val chapters = title.chapters
            val year = title.endYear
            return when {
                chapters != null && year != null -> "$chapters chapters, finished $year"
                chapters != null -> "$chapters chapters, finished"
                else -> "Finished, score ${title.score ?: "-"}"
            }
        }

        private fun readers(popularity: Int): String {
            return if (popularity >= 1000) "${popularity / 1000}k" else popularity.toString()
        }
    }
}

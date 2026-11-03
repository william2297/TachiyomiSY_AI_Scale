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

    suspend fun refresh(
        previous: DiscoverCache,
        sources: List<SearchSource>,
        profile: TasteProfile = TasteProfile(),
    ): DiscoverCache {
        val day = now() / DAY_MILLIS
        val lists = fetchLists().mapValues { (_, titles) -> titles.filterNot(isExcluded) }
        val picks = pickCandidates(lists, previous, day)
        val myGenres = buildMyGenres(profile, previous.myGenres, localDay(now()))
        val extraPlans = buildExtraPlans(profile, day) + listOfNotNull(myGenres.plan)
        val knownIds = previous.sections.values.flatten().map { it.anilistId }.toSet() +
            previous.extras.flatMap { it.entries }.map { it.anilistId }

        val sourcesKey = sources.joinToString(",") { it.id.toString() }
        // The 12 shown in each row are searched first; the rest of the 50 fill in over the next days
        val allPicks = picks.values.toList() + extraPlans.map { it.titles }
        val ordered = (allPicks.flatMap { it.take(DISPLAY_SIZE) } + allPicks.flatMap { it.drop(DISPLAY_SIZE) })
            .distinctBy { it.id }
        val sourceIds = sources.map(SearchSource::id)

        fun sectionsFor(matches: Map<Long, MatchRecord>): Map<String, List<StoredEntry>> {
            val sections = linkedMapOf<String, List<StoredEntry>>()
            for ((section, titles) in picks) {
                val entries = titles.withIndex().mapNotNull { (index, title) ->
                    val match = matches[title.id]?.takeIf { it.found && it.sourceId in sourceIds }
                    match?.let {
                        toEntry(
                            section,
                            title,
                            it,
                            rankOf(section, lists, title, index),
                        ).copy(isNew = isNew(title.id, knownIds))
                    }
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

        fun extrasFor(matches: Map<Long, MatchRecord>): List<StoredRow> {
            return extraPlans.mapNotNull { plan ->
                val entries = plan.titles.mapNotNull { title ->
                    val match =
                        matches[title.id]?.takeIf { it.found && it.sourceId in sourceIds } ?: return@mapNotNull null
                    StoredEntry(
                        anilistId = title.id, title = title.title, coverUrl = title.coverUrl, sourceId = match.sourceId,
                        sourceName = match.sourceName, mangaUrl = match.mangaUrl, score = title.score,
                        completed = title.finished, reason = plan.reason(title), names = title.names,
                        isNew = isNew(title.id, knownIds),
                    )
                }.take(plan.limit)
                entries.takeIf { it.isNotEmpty() }?.let { StoredRow(plan.key, plan.title, plan.hint, it) }
            }
        }

        val matches = match(ordered, previous.matches, sources, sourcesKey) { found ->
            onProgress(
                previous.copy(
                    updatedAtMillis = now(),
                    sections = sectionsFor(found),
                    extras = extrasFor(found).ifEmpty { previous.extras },
                    myGenres = myGenres.pick,
                    errorMessage = null,
                ),
            )
        }

        return DiscoverCache(
            updatedAtMillis = now(),
            sections = sectionsFor(matches),
            extras = extrasFor(matches).ifEmpty { previous.extras },
            myGenres = myGenres.pick,
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

    /** One row of titles built from the user's taste, with how each title explains itself. */
    private class ExtraPlan(
        val key: String,
        val title: String,
        val hint: String,
        val titles: List<AniListTitle>,
        val reason: (AniListTitle) -> String,
        /** Most entries the row keeps; the others show a short row. */
        val limit: Int = DISPLAY_SIZE,
    )

    private class MyGenresResult(val plan: ExtraPlan?, val pick: MyGenresPick)

    /**
     * "My genres": the user's own genres, any of them, so one request per genre and a merge; a title that matches more
     * of the genres ranks higher. The ranked list is kept and reused until the local day or the genres change, so the
     * row does not reshuffle between refreshes.
     */
    private suspend fun buildMyGenres(profile: TasteProfile, previous: MyGenresPick, day: Long): MyGenresResult {
        val genres = profile.pickedGenres.filter { it in AniListDiscoverApi.KNOWN_GENRES }.distinct().sorted()
            .take(MAX_PICKED_GENRES)
        if (genres.isEmpty()) return MyGenresResult(null, MyGenresPick())

        val sameGenres = previous.genres == genres
        var pick = previous
        if (!(sameGenres && previous.day == day && previous.titles.isNotEmpty())) {
            val merged = linkedMapOf<Long, AniListTitle>()
            for (genre in genres) {
                for (country in countries) {
                    try {
                        api.byGenre(genre, country).forEach { merged.putIfAbsent(it.id, it) }
                    } catch (e: CancellationException) {
                        currentCoroutineContext().ensureActive()
                    } catch (e: Exception) {
                        // This genre is left out for now
                    }
                    pause(API_PAUSE_MILLIS)
                }
            }
            val ranked = merged.values.filterNot(isExcluded)
                .sortedWith(
                    compareByDescending<AniListTitle> { title -> title.genres.count { it in genres } }
                        .thenByDescending { it.trending }
                        .thenBy { it.id },
                )
                .take(MY_GENRES_POOL)
            pick = when {
                ranked.isNotEmpty() -> MyGenresPick(day, genres, ranked)
                // Nothing came back: keep yesterday's list for the same genres, show nothing for new genres
                sameGenres -> previous
                else -> MyGenresPick(day, genres, emptyList())
            }
        }
        val titles = pick.titles.filterNot(isExcluded)
        if (titles.isEmpty()) return MyGenresResult(null, pick)
        return MyGenresResult(
            ExtraPlan(
                key = MY_GENRES_KEY,
                title = "My genres",
                hint = genres.joinToString(", ") + ": any of them",
                titles = titles,
                reason = { title ->
                    val matched = title.genres.filter { it in genres }
                    if (matched.isEmpty()) "Popular right now" else "Matches ${matched.joinToString(", ")}"
                },
                limit = MAX_PER_SECTION,
            ),
            pick,
        )
    }

    private fun localDay(nowMillis: Long): Long {
        return (nowMillis + java.util.TimeZone.getDefault().getOffset(nowMillis)).floorDiv(DAY_MILLIS)
    }

    private fun isNew(id: Long, known: Set<Long>): Boolean = known.isNotEmpty() && id !in known

    private suspend fun buildExtraPlans(profile: TasteProfile, day: Long): List<ExtraPlan> {
        val plans = mutableListOf<ExtraPlan>()

        // "Because you read X": what AniList readers of X went on to read
        for (seed in profile.seeds) {
            val result = try {
                api.recommendationsFor(seed)
            } catch (e: CancellationException) {
                currentCoroutineContext().ensureActive()
                null
            } catch (e: Exception) {
                null
            }
            pause(API_PAUSE_MILLIS)
            // AniList searches loosely; only a series that really carries the seed's name is accepted
            if (result == null || TitleMatcher.match(result.seedNames, seed) == TitleMatcher.Quality.NONE) continue
            val titles = result.recommendations.filterNot(isExcluded).filter { it.names.isNotEmpty() }
            if (titles.isEmpty()) continue
            plans += ExtraPlan(
                key = "because-" + TitleMatcher.normalize(seed).replace(' ', '-'),
                title = "Because you read $seed",
                hint = "What readers who liked it went on to read",
                titles = titles.take(MAX_PER_SECTION),
                reason = { "Recommended by readers of $seed" },
            )
        }

        // "Your genres": any of them, so one request per genre and a merge; more matching genres ranks higher
        val genres = profile.genres.filter { it in AniListDiscoverApi.KNOWN_GENRES }.take(MAX_GENRES)
        if (genres.isNotEmpty()) {
            val merged = linkedMapOf<Long, AniListTitle>()
            for (genre in genres) {
                for (country in countries) {
                    try {
                        api.byGenre(genre, country).forEach { merged.putIfAbsent(it.id, it) }
                    } catch (e: CancellationException) {
                        currentCoroutineContext().ensureActive()
                    } catch (e: Exception) {
                        // This genre is left out today
                    }
                    pause(API_PAUSE_MILLIS)
                }
            }
            val ranked = merged.values.filterNot(isExcluded)
                .sortedWith(
                    compareByDescending<AniListTitle> { title -> title.genres.count { it in genres } }
                        .thenByDescending { it.trending },
                )
            if (ranked.isNotEmpty()) {
                plans += ExtraPlan(
                    key = "genres",
                    title = "Your genres",
                    hint = genres.joinToString(", ") + ": any of them",
                    titles = rotate(ranked.take(POOL_SIZE), MAX_PER_SECTION, day * 31 + GENRE_SEED),
                    reason = { title ->
                        val matched = title.genres.filter { it in genres }
                        if (matched.isEmpty()) "Popular right now" else "Matches ${matched.joinToString(", ")}"
                    },
                )
            }
        }

        // "Fresh starts": series from the last year that already have readers
        val fresh = linkedMapOf<Long, AniListTitle>()
        val since = yyyymmddAYearBefore(now())
        for (country in countries) {
            try {
                api.fresh(country, since).forEach { fresh.putIfAbsent(it.id, it) }
            } catch (e: CancellationException) {
                currentCoroutineContext().ensureActive()
            } catch (e: Exception) {
                // No fresh row today
            }
            pause(API_PAUSE_MILLIS)
        }
        val freshTitles = fresh.values.filterNot(isExcluded).sortedByDescending { it.popularity }
        if (freshTitles.isNotEmpty()) {
            plans += ExtraPlan(
                key = "fresh",
                title = "Fresh starts",
                hint = "New series that already have readers",
                titles = freshTitles.take(MAX_PER_SECTION),
                reason = { title ->
                    "Started ${title.startYear ?: "recently"}, ${readers(title.popularity)} readers already"
                },
            )
        }
        return plans
    }

    private fun yyyymmddAYearBefore(nowMillis: Long): Int {
        val date = java.time.Instant.ofEpochMilli(
            nowMillis,
        ).atZone(java.time.ZoneOffset.UTC).toLocalDate().minusYears(1)
        return date.year * 10_000 + date.monthValue * 100 + date.dayOfMonth
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
        private const val MAX_GENRES = 3
        private const val MAX_PICKED_GENRES = 10

        /** Titles kept for "My genres", more than the 50 shown because some are not in any of the user's sources. */
        private const val MY_GENRES_POOL = 75
        const val MY_GENRES_KEY = "my-genres"
        private const val GENRE_SEED = 97L
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

package eu.kanade.tachiyomi.data.discover

import android.content.Context
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.util.system.isConnectedToWifi
import eu.kanade.tachiyomi.util.system.workManager
import kotlinx.coroutines.flow.first
import logcat.LogPriority
import mihon.app.di.AppGraph
import mihon.app.di.appGraph
import tachiyomi.core.common.util.system.logcat
import tachiyomi.source.local.isLocal
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Refreshes the Discover picks once a day, on Wi-Fi only, and when the user asks for it (which also waits for Wi-Fi).
 */
class DiscoverRefreshWorker(private val context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        // Wi-Fi only. Checked here as well as in the constraints, which cannot tell every Wi-Fi from a metered one
        if (!context.isConnectedToWifi()) return Result.retry()
        val graph = context.appGraph
        val store = DiscoverStore(storeFile(context), graph.json)
        val previous = store.load()

        val sources = pinnedSearchSources(context)
        if (sources.isEmpty()) {
            store.save(previous.copy(errorMessage = NO_PINNED_SOURCES))
            return Result.success()
        }

        return try {
            // What is already in the library is not recommended; read titles outside the library are hidden in the tab
            val owned = graph.getFavorites.await().map { TitleMatcher.normalize(it.title) }.toSet()
            val hidden = DiscoverHiddenStore(hiddenFile(context), graph.json).load()
            val tasteStore = DiscoverTasteStore(tasteFile(context), graph.json)
            val countries = if (graph.sourcePreferences.discoverIncludeChinese.get()) {
                listOf("KR", "JP", "CN")
            } else {
                listOf("KR", "JP")
            }
            val refresher = DiscoverRefresher(
                api = AniListDiscoverApi(graph.networkHelper.client, graph.json),
                countries = countries,
                searcher = SourceTitleSearcher(graph.sourceManager),
                isExcluded = { title -> title.id in hidden || title.names.any { TitleMatcher.normalize(it) in owned } },
                onProgress = { partial -> store.save(partial) },
                onStatus = { text -> setProgress(workDataOf(KEY_STATUS to text)) },
                onTitleTags = { key, tags -> tasteStore.putTitle(key, tags) },
                recommendationSource = MangaUpdatesApi(graph.networkHelper.client, graph.json),
            )
            val picked = DiscoverGenreStore(genresFile(context), graph.json).load()
            val taste = tasteStore.load()
            val profile = tasteProfile(graph).copy(
                pickedGenres = picked.sorted(),
                pickedSeed = DiscoverSeedStore(seedFile(context)).load(),
                knownTags = taste.titles,
                negatives = taste.negatives,
            )
            store.save(refresher.refresh(previous, sources, profile))
            Result.success()
        } catch (e: IOException) {
            logcat(LogPriority.WARN, e) { "Discover refresh failed" }
            store.save(previous.copy(errorMessage = CANNOT_REACH_ANILIST))
            if (runAttemptCount < MAX_ATTEMPTS) Result.retry() else Result.success()
        }
    }

    companion object {
        const val NO_PINNED_SOURCES = "Pin a few sources in Browse > Sources to get recommendations."
        const val CANNOT_REACH_ANILIST = "Can't reach AniList. Showing saved picks."

        private const val WORK_NAME_AUTO = "DiscoverRefresh"
        private const val WORK_NAME_NOW = "DiscoverRefreshNow"
        private const val MAX_ATTEMPTS = 3
        const val KEY_STATUS = "status"
        private const val MAX_SOURCES = 8
        private const val SEED_POOL = 20
        private const val SEEDS_PER_DAY = 3

        fun storeFile(context: Context) = File(context.filesDir, "discover.json")

        /** A few titles the user read recently (a different few each day) and the genres they keep most. */
        private suspend fun tasteProfile(graph: AppGraph): TasteProfile {
            val library = graph.getLibraryManga.await()
            val day = System.currentTimeMillis() / 86_400_000L
            val recentlyRead = library.filter { it.readCount > 0 }
                .sortedByDescending { it.lastRead }
                .take(SEED_POOL)
                .map { it.manga.title }
            val seeds = DiscoverRefresher.rotate(recentlyRead, SEEDS_PER_DAY, day)
            val recentTitles = recentlyRead
            val genres = library.flatMap { it.manga.genre.orEmpty() }
                .map { it.trim() }
                .filter { it in AniListDiscoverApi.KNOWN_GENRES }
                .groupingBy { it }.eachCount()
                .entries.sortedByDescending { it.value }
                .map { it.key }
            return TasteProfile(seeds = seeds, genres = genres, recentTitles = recentTitles)
        }

        fun hiddenFile(context: Context) = File(context.filesDir, "discover_hidden.json")

        fun tasteFile(context: Context) = File(context.filesDir, "discover_taste.json")

        fun genresFile(context: Context) = File(context.filesDir, "discover_genres.json")

        fun seedFile(context: Context) = File(context.filesDir, "discover_seed.txt")

        /**
         * The pinned sources that can be searched: the [MAX_SOURCES] the library reads from most, tried with better
         * images first (see [qualityRank]). The first source with a match wins, so the order decides where a title
         * opens. Name order put "AsmHentai" before "Asura Scans" and linked mainstream series to doujin with the same
         * title.
         */
        suspend fun pinnedSearchSources(context: Context): List<SearchSource> {
            val graph = context.appGraph
            val pinned = graph.sourcePreferences.pinnedSources.get()
            val libraryCount = graph.getLibraryManga.await().groupingBy { it.manga.source }.eachCount()
            return graph.sourceManager.getAll()
                .filter { "${it.id}" in pinned && it is CatalogueSource && !it.isLocal() }
                .sortedWith(
                    compareByDescending<eu.kanade.tachiyomi.source.Source> { libraryCount[it.id] ?: 0 }
                        .thenBy { it.name.lowercase() },
                )
                .take(MAX_SOURCES)
                .sortedBy { qualityRank(it.name) }
                .map { SearchSource(it.id, it.name, adultOnly = ADULT_SOURCE_NAME.containsMatchIn(it.name)) }
        }

        /**
         * Lower is tried first. Scanlation groups publishing their own releases come first, MangaDex (group uploads)
         * next, other sources after that, and aggregators that reupload sliced chapters last, so they only catch
         * titles the others do not have. Ties keep the library order (the sort is stable).
         */
        internal fun qualityRank(name: String): Int {
            val n = name.lowercase()
            val preferred = QUALITY_ORDER.indexOfFirst { it in n }
            return when {
                preferred >= 0 -> preferred
                DiscoverRefresher.isAggregator(n) -> QUALITY_ORDER.size + 1
                else -> QUALITY_ORDER.size
            }
        }

        private val QUALITY_ORDER = listOf("asura", "flame", "temple", "mangadex")

        private val ADULT_SOURCE_NAME =Regex("hentai|doujin|\\bh-?manga\\b|18\\+", RegexOption.IGNORE_CASE)

        private fun wifiConstraints(): Constraints {
            // Any Wi-Fi counts, including one the system calls metered (a phone hotspot, some routers)
            val request = NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .build()
            return Constraints.Builder()
                .setRequiredNetworkRequest(request, NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build()
        }

        /** Keeps one daily refresh scheduled; calling it again does not restart the clock. */
        fun setupTask(context: Context) {
            val request = PeriodicWorkRequestBuilder<DiscoverRefreshWorker>(1, TimeUnit.DAYS, 2, TimeUnit.HOURS)
                .setConstraints(wifiConstraints())
                .setBackoffCriteria(BackoffPolicy.LINEAR, 30, TimeUnit.MINUTES)
                .build()
            context.workManager.enqueueUniquePeriodicWork(WORK_NAME_AUTO, ExistingPeriodicWorkPolicy.KEEP, request)
        }

        /**
         * A refresh now. It starts at once on Wi-Fi and otherwise waits until Wi-Fi is connected. A run that is
         * already going is left alone; an older request that is only waiting is replaced, so one that got stuck
         * (for example from an earlier version) cannot block every later refresh.
         */
        suspend fun startNow(context: Context, replaceRunning: Boolean = false) {
            val existing = context.workManager.getWorkInfosForUniqueWorkFlow(WORK_NAME_NOW).first()
            // A change of genres replaces a run that is going, so the new genres do not wait for tomorrow
            if (!replaceRunning && existing.any { it.state == WorkInfo.State.RUNNING }) return
            val request = OneTimeWorkRequestBuilder<DiscoverRefreshWorker>()
                .setConstraints(wifiConstraints())
                .build()
            context.workManager.enqueueUniqueWork(WORK_NAME_NOW, ExistingWorkPolicy.REPLACE, request)
        }

        fun nowWorkName() = WORK_NAME_NOW

        fun autoWorkName() = WORK_NAME_AUTO
    }
}

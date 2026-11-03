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
import androidx.work.WorkerParameters
import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.util.system.workManager
import logcat.LogPriority
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
            val refresher = DiscoverRefresher(
                api = AniListDiscoverApi(graph.networkHelper.client, graph.json),
                searcher = SourceTitleSearcher(graph.sourceManager),
                isExcluded = { title -> title.names.any { TitleMatcher.normalize(it) in owned } },
            )
            store.save(refresher.refresh(previous, sources))
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
        private const val MAX_SOURCES = 6

        fun storeFile(context: Context) = File(context.filesDir, "discover.json")

        /** The pinned sources that can be searched, at most [MAX_SOURCES], in name order. */
        suspend fun pinnedSearchSources(context: Context): List<SearchSource> {
            val graph = context.appGraph
            val pinned = graph.sourcePreferences.pinnedSources.get()
            return graph.sourceManager.getAll()
                .filter { "${it.id}" in pinned && it is CatalogueSource && !it.isLocal() }
                .sortedBy { it.name.lowercase() }
                .take(MAX_SOURCES)
                .map { SearchSource(it.id, it.name) }
        }

        private fun wifiConstraints(): Constraints {
            val request = NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
                .build()
            return Constraints.Builder()
                .setRequiredNetworkRequest(request, NetworkType.UNMETERED)
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

        /** A refresh now. It starts at once on Wi-Fi and otherwise waits until Wi-Fi is connected. */
        fun startNow(context: Context) {
            val request = OneTimeWorkRequestBuilder<DiscoverRefreshWorker>()
                .setConstraints(wifiConstraints())
                .build()
            context.workManager.enqueueUniqueWork(WORK_NAME_NOW, ExistingWorkPolicy.KEEP, request)
        }

        fun nowWorkName() = WORK_NAME_NOW

        fun autoWorkName() = WORK_NAME_AUTO
    }
}

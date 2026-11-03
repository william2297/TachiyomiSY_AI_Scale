package eu.kanade.tachiyomi.ui.browse.discover

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import eu.kanade.presentation.browse.discover.DiscoverEntry
import eu.kanade.presentation.browse.discover.DiscoverSection
import eu.kanade.presentation.browse.discover.DiscoverState
import eu.kanade.tachiyomi.data.discover.DiscoverCache
import eu.kanade.tachiyomi.data.discover.DiscoverRefreshWorker
import eu.kanade.tachiyomi.data.discover.DiscoverStore
import eu.kanade.tachiyomi.data.discover.StoredEntry
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.util.system.isConnectedToWifi
import eu.kanade.tachiyomi.util.system.workManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mihon.app.di.appGraph
import mihon.domain.manga.model.toDomainManga
import tachiyomi.domain.manga.interactor.NetworkToLocalManga

/**
 * Serves the Discover picks that the daily worker stored, and starts a refresh when asked. It reads a small file
 * and watches two work requests; the network work happens in [DiscoverRefreshWorker].
 */
@Inject
@ViewModelKey
@ContributesIntoMap(AppScope::class, binding = binding<ViewModel>())
class DiscoverViewModel(
    private val context: Context,
    private val networkToLocalManga: NetworkToLocalManga,
) : ViewModel() {

    private val store by lazy { DiscoverStore(DiscoverRefreshWorker.storeFile(context), context.appGraph.json) }

    private val cache = MutableStateFlow(DiscoverCache())
    private val loading = MutableStateFlow(false)
    private val waitingForWifi = MutableStateFlow(false)

    val state: StateFlow<DiscoverState> = combine(cache, loading, waitingForWifi) { cache, loading, waiting ->
        DiscoverState(
            sections = toSections(cache),
            loading = loading,
            updatedAtMillis = cache.updatedAtMillis.takeIf { it > 0 },
            error = if (waiting) WAITING_FOR_WIFI else cache.errorMessage,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, DiscoverState(emptyMap(), false, null, null))

    init {
        DiscoverRefreshWorker.setupTask(context)
        viewModelScope.launch {
            reload()
            val ageMillis = System.currentTimeMillis() - cache.value.updatedAtMillis
            // First open, or a day was missed (the phone was off or off Wi-Fi)
            if (cache.value.updatedAtMillis == 0L || ageMillis > STALE_AFTER_MILLIS) refresh()
        }
        viewModelScope.launch {
            val now = context.workManager.getWorkInfosForUniqueWorkFlow(DiscoverRefreshWorker.nowWorkName())
            val auto = context.workManager.getWorkInfosForUniqueWorkFlow(DiscoverRefreshWorker.autoWorkName())
            combine(now, auto) { manual, daily -> manual to daily }.collect { (manual, daily) ->
                val running = (manual + daily).any { it.state == WorkInfo.State.RUNNING }
                val queuedByUser = manual.any { it.state == WorkInfo.State.ENQUEUED }
                loading.value = running || queuedByUser
                waitingForWifi.value = queuedByUser && !running && !context.isConnectedToWifi()
                // A finished run changed the file
                reload()
            }
        }
    }

    private suspend fun reload() {
        cache.value = withContext(Dispatchers.IO) { store.load() }
    }

    /** Starts a refresh now. It waits for Wi-Fi if the tablet is not on Wi-Fi. */
    fun refresh() {
        DiscoverRefreshWorker.startNow(context)
    }

    /** Makes the matched title a local entry, as opening it from a source's list does, and returns its id. */
    suspend fun localMangaId(entry: DiscoverEntry): Long {
        val sManga = SManga.create().apply {
            url = entry.mangaUrl
            title = entry.title
            thumbnail_url = entry.coverUrl
        }
        return networkToLocalManga(sManga.toDomainManga(entry.sourceId)).id
    }

    private fun toSections(cache: DiscoverCache): Map<DiscoverSection, List<DiscoverEntry>> {
        val sections = linkedMapOf<DiscoverSection, List<DiscoverEntry>>()
        for (section in DiscoverSection.entries) {
            val entries = cache.sections[section.name].orEmpty().map { it.toEntry() }
            if (entries.isNotEmpty()) sections[section] = entries
        }
        return sections
    }

    private fun StoredEntry.toEntry() = DiscoverEntry(
        id = anilistId,
        title = title,
        coverUrl = coverUrl,
        sourceId = sourceId,
        sourceName = sourceName,
        mangaUrl = mangaUrl,
        score = score,
        completed = completed,
        reason = reason,
    )

    private companion object {
        const val WAITING_FOR_WIFI = "Waiting for Wi-Fi to refresh."
        const val STALE_AFTER_MILLIS = 26 * 60 * 60 * 1000L
    }
}

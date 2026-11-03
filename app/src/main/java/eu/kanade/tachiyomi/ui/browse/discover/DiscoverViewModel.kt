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
import eu.kanade.tachiyomi.data.discover.TitleMatcher
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
import tachiyomi.domain.manga.interactor.GetLibraryManga
import tachiyomi.domain.manga.interactor.NetworkToLocalManga
import tachiyomi.domain.manga.repository.MangaRepository

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
    private val getLibraryManga: GetLibraryManga,
    private val mangaRepository: MangaRepository,
) : ViewModel() {

    private val store by lazy { DiscoverStore(DiscoverRefreshWorker.storeFile(context), context.appGraph.json) }

    private val cache = MutableStateFlow(DiscoverCache())
    private val loading = MutableStateFlow(false)
    private val waitingForWifi = MutableStateFlow(false)

    /** Normalized titles of everything in the library, and of everything read that is not in it. */
    private val libraryTitles = MutableStateFlow<Set<String>>(emptySet())
    private val readTitles = MutableStateFlow<Set<String>>(emptySet())

    val state: StateFlow<DiscoverState> = combine(
        cache,
        loading,
        waitingForWifi,
        libraryTitles,
        readTitles,
    ) { cache, loading, waiting, inLibrary, read ->
        DiscoverState(
            sections = toSections(cache, inLibrary + read),
            loading = loading,
            updatedAtMillis = cache.updatedAtMillis.takeIf { it > 0 },
            error = if (waiting) WAITING_FOR_WIFI else cache.errorMessage,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, DiscoverState(emptyMap(), false, null, null))

    init {
        DiscoverRefreshWorker.setupTask(context)
        viewModelScope.launch {
            // Follows the library live, so marking chapters read shows up here without a refresh
            getLibraryManga.subscribe().collect { library ->
                libraryTitles.value = library.map { TitleMatcher.normalize(it.manga.title) }.toSet()
            }
        }
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
        // Titles read outside the library; the library itself is followed through its own flow
        readTitles.value = withContext(Dispatchers.IO) {
            mangaRepository.getReadMangaNotInLibrary().map { TitleMatcher.normalize(it.title) }.toSet()
        }
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

    /** The rows, without anything the user already has or has read, under any of its names. */
    private fun toSections(cache: DiscoverCache, hidden: Set<String>): Map<DiscoverSection, List<DiscoverEntry>> {
        val sections = linkedMapOf<DiscoverSection, List<DiscoverEntry>>()
        for (section in DiscoverSection.entries) {
            val entries = cache.sections[section.name].orEmpty()
                .filterNot { stored -> (stored.names + stored.title).any { TitleMatcher.normalize(it) in hidden } }
                .map { it.toEntry() }
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

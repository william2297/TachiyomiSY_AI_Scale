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
import eu.kanade.presentation.browse.discover.DiscoverDedupe
import eu.kanade.presentation.browse.discover.DiscoverEntry
import eu.kanade.presentation.browse.discover.DiscoverRow
import eu.kanade.presentation.browse.discover.DiscoverSection
import eu.kanade.presentation.browse.discover.DiscoverState
import eu.kanade.tachiyomi.data.discover.DiscoverCache
import eu.kanade.tachiyomi.data.discover.DiscoverGenreStore
import eu.kanade.tachiyomi.data.discover.DiscoverHiddenStore
import eu.kanade.tachiyomi.data.discover.DiscoverRefreshWorker
import eu.kanade.tachiyomi.data.discover.DiscoverSeedStore
import eu.kanade.tachiyomi.data.discover.DiscoverStore
import eu.kanade.tachiyomi.data.discover.DiscoverTasteStore
import eu.kanade.tachiyomi.data.discover.StoredEntry
import eu.kanade.tachiyomi.data.discover.TitleMatcher
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.util.system.isConnectedToWifi
import eu.kanade.tachiyomi.util.system.workManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
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
    private val status = MutableStateFlow<String?>(null)
    private val hiddenStore by lazy {
        DiscoverHiddenStore(DiscoverRefreshWorker.hiddenFile(context), context.appGraph.json)
    }
    private val hidden = MutableStateFlow<Set<Long>>(emptySet())
    private val tasteStore by lazy {
        DiscoverTasteStore(DiscoverRefreshWorker.tasteFile(context), context.appGraph.json)
    }
    private val genreStore by lazy {
        DiscoverGenreStore(DiscoverRefreshWorker.genresFile(context), context.appGraph.json)
    }
    private val pickedGenres = MutableStateFlow<Set<String>>(emptySet())
    private val seedStore by lazy { DiscoverSeedStore(DiscoverRefreshWorker.seedFile(context)) }
    private val pickedSeed = MutableStateFlow<String?>(null)

    /** Library titles as shown, for the "Because you like" picker. */
    val seedTitles = MutableStateFlow<List<String>>(emptyList())

    /** Normalized titles of everything in the library, and of everything read that is not in it. */
    private val libraryTitles = MutableStateFlow<Set<String>>(emptySet())
    private val readTitles = MutableStateFlow<Set<String>>(emptySet())

    val state: StateFlow<DiscoverState> = combine(
        cache,
        loading,
        waitingForWifi,
        libraryTitles,
        readTitles,
        status,
        hidden,
        pickedGenres,
        pickedSeed,
    ) { values ->
        val cache = values[0] as DiscoverCache
        val loading = values[1] as Boolean
        val waiting = values[2] as Boolean

        @Suppress("UNCHECKED_CAST")
        val inLibrary = values[3] as Set<String>

        @Suppress("UNCHECKED_CAST")
        val read = values[4] as Set<String>
        val status = values[5] as String?

        @Suppress("UNCHECKED_CAST")
        val notInterested = values[6] as Set<Long>

        @Suppress("UNCHECKED_CAST")
        val picked = values[7] as Set<String>
        val seed = values[8] as String?
        val (sections, extraRows) = DiscoverDedupe.apply(
            toSections(cache, inLibrary + read, notInterested),
            toExtraRows(cache, inLibrary + read, notInterested),
        )
        DiscoverState(
            status = status,
            sections = sections,
            extraRows = extraRows,
            pickedGenres = picked,
            pickedSeed = seed,
            tasteNote = cache.tasteNote,
            loading = loading,
            updatedAtMillis = cache.updatedAtMillis.takeIf { it > 0 },
            error = if (waiting) WAITING_FOR_WIFI else cache.errorMessage,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, DiscoverState(emptyMap(), false, null, null))

    init {
        DiscoverRefreshWorker.setupTask(context)
        viewModelScope.launch { hidden.value = withContext(Dispatchers.IO) { hiddenStore.load() } }
        viewModelScope.launch { pickedGenres.value = withContext(Dispatchers.IO) { genreStore.load() } }
        viewModelScope.launch { pickedSeed.value = withContext(Dispatchers.IO) { seedStore.load() } }
        viewModelScope.launch {
            // Follows the library live, so marking chapters read shows up here without a refresh
            getLibraryManga.subscribe().collect { library ->
                libraryTitles.value = library.map { TitleMatcher.normalize(it.manga.title) }.toSet()
                seedTitles.value = library.map { it.manga.title.trim() }
                    .filter { it.isNotEmpty() }
                    .distinct()
                    .sortedBy { it.lowercase() }
            }
        }
        viewModelScope.launch {
            reload()
            val ageMillis = System.currentTimeMillis() - cache.value.updatedAtMillis
            // First open, or a day was missed (the phone was off or off Wi-Fi)
            if (cache.value.updatedAtMillis == 0L || ageMillis > STALE_AFTER_MILLIS) refresh()
        }
        viewModelScope.launch {
            // While a refresh runs, picks are saved as they are found; look for them every few seconds
            loading.collectLatest { isLoading ->
                while (isLoading) {
                    delay(PROGRESS_POLL_MILLIS)
                    reload()
                }
            }
        }
        viewModelScope.launch {
            val now = context.workManager.getWorkInfosForUniqueWorkFlow(DiscoverRefreshWorker.nowWorkName())
            val auto = context.workManager.getWorkInfosForUniqueWorkFlow(DiscoverRefreshWorker.autoWorkName())
            combine(now, auto) { manual, daily -> manual to daily }.collect { (manual, daily) ->
                val running = (manual + daily).any { it.state == WorkInfo.State.RUNNING }
                val queuedByUser = manual.any { it.state == WorkInfo.State.ENQUEUED }
                status.value = (manual + daily).firstOrNull { it.state == WorkInfo.State.RUNNING }
                    ?.progress?.getString(DiscoverRefreshWorker.KEY_STATUS)
                    ?: if (queuedByUser) WAITING_TO_START else null
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

    /** "Not interested": the title leaves every row now and is skipped by later refreshes. */
    fun hide(entry: DiscoverEntry) {
        viewModelScope.launch {
            hidden.value = withContext(Dispatchers.IO) { hiddenStore.add(entry.id) }
            // The tags of what the user rejected count against those tags in the taste profile, fading over weeks
            withContext(Dispatchers.IO) { tasteStore.addNegative(entry.tags, System.currentTimeMillis()) }
        }
    }

    /**
     * The genres for the "My genres" row (any of them). A real change builds the row again at once, a refresh that
     * waits for Wi-Fi like any other; choosing the same genres again does nothing.
     */
    fun setGenres(genres: Set<String>) {
        viewModelScope.launch {
            val saved = withContext(Dispatchers.IO) { genreStore.save(genres) }
            if (saved == pickedGenres.value) return@launch
            pickedGenres.value = saved
            DiscoverRefreshWorker.startNow(context, replaceRunning = true)
        }
    }

    /**
     * The library title for the "Because you like" row, or null to remove the row. A real change builds the row again
     * at once; picking the same title again does nothing.
     */
    fun setSeed(title: String?) {
        viewModelScope.launch {
            val saved = withContext(Dispatchers.IO) { seedStore.save(title) }
            if (saved == pickedSeed.value) return@launch
            pickedSeed.value = saved
            if (saved != null) DiscoverRefreshWorker.startNow(context, replaceRunning = true)
        }
    }

    /** Starts a refresh now. It waits for Wi-Fi if the tablet is not on Wi-Fi. */
    fun refresh() {
        viewModelScope.launch { DiscoverRefreshWorker.startNow(context) }
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
    private fun toSections(
        cache: DiscoverCache,
        hidden: Set<String>,
        notInterested: Set<Long>,
    ): Map<DiscoverSection, List<DiscoverEntry>> {
        val sections = linkedMapOf<DiscoverSection, List<DiscoverEntry>>()
        for (section in DiscoverSection.entries) {
            val entries = cache.sections[section.name].orEmpty()
                .filterNot { stored -> stored.anilistId in notInterested }
                .filterNot { stored -> (stored.names + stored.title).any { TitleMatcher.normalize(it) in hidden } }
                .map { it.toEntry() }
            if (entries.isNotEmpty()) sections[section] = entries
        }
        return sections
    }

    private fun toExtraRows(cache: DiscoverCache, hidden: Set<String>, notInterested: Set<Long>): List<DiscoverRow> {
        return mergeSmallBecauseRows(filterExtraRows(cache, hidden, notInterested))
    }

    /**
     * "Because you read X" rows with only a few titles found in the sources are folded into one row, so the page
     * does not fill up with stubs. Rows with enough titles stay as they are.
     */
    private fun mergeSmallBecauseRows(rows: List<DiscoverRow>): List<DiscoverRow> {
        val small = rows.filter { it.key.startsWith(BECAUSE_PREFIX) && it.entries.size < SMALL_ROW }
        if (small.size < 2) return rows
        val names = small.map { it.title.removePrefix(BECAUSE_TITLE) }
        val title = BECAUSE_TITLE + when {
            names.size <= 3 -> names.dropLast(1).joinToString(", ") + " and " + names.last()
            else -> names.take(2).joinToString(", ") + " and ${names.size - 2} more"
        }
        val merged = DiscoverRow(
            BECAUSE_PREFIX + "merged",
            title,
            "What readers who liked them went on to read",
            small.flatMap { it.entries }.distinctBy { it.id }.take(MERGED_LIMIT),
        )
        val smallKeys = small.map { it.key }.toSet()
        val position = rows.indexOfFirst { it.key in smallKeys }
        return rows.filterNot { it.key in smallKeys }.toMutableList().apply {
            add(position.coerceIn(0, size), merged)
        }
    }

    private fun filterExtraRows(cache: DiscoverCache, hidden: Set<String>, notInterested: Set<Long>): List<DiscoverRow> {
        return cache.extras.mapNotNull { row ->
            val entries = row.entries
                .filterNot { it.anilistId in notInterested }
                .filterNot { stored -> (stored.names + stored.title).any { TitleMatcher.normalize(it) in hidden } }
                .map { it.toEntry() }
            entries.takeIf { it.isNotEmpty() }?.let { DiscoverRow(row.key, row.title, row.hint, it) }
        }
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
        isNew = isNew,
        tags = tags,
    )

    private companion object {
        const val WAITING_FOR_WIFI = "Waiting for Wi-Fi to refresh."
        const val WAITING_TO_START = "Waiting to start. It needs Wi-Fi and a battery that is not low."
        const val STALE_AFTER_MILLIS = 26 * 60 * 60 * 1000L
        const val PROGRESS_POLL_MILLIS = 4_000L
        const val BECAUSE_PREFIX = "because-"
        const val BECAUSE_TITLE = "Because you read "

        /** A "Because you read" row with fewer titles than this is merged with the other small ones. */
        const val SMALL_ROW = 5

        /** The merged row stays within one screen row, without a "See all" tile. */
        const val MERGED_LIMIT = 12
    }
}

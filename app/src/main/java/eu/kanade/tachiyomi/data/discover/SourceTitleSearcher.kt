package eu.kanade.tachiyomi.data.discover

import eu.kanade.tachiyomi.source.CatalogueSource
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import tachiyomi.domain.source.service.SourceManager
import java.io.IOException

/** Searches an installed source the same way the Browse search box does, first page only. */
class SourceTitleSearcher(private val sourceManager: SourceManager) : TitleSearcher {

    override suspend fun search(source: SearchSource, query: String): List<FoundManga> {
        val catalogue = sourceManager.get(source.id) as? CatalogueSource ?: return emptyList()
        // A timeout is cancellation to the coroutine library; turn it into a plain failure so one slow site cannot
        // end the whole refresh
        val page = try {
            withTimeout(SEARCH_TIMEOUT_MILLIS) {
                catalogue.getSearchManga(1, query, catalogue.getFilterList())
            }
        } catch (e: TimeoutCancellationException) {
            throw IOException("Search in ${source.name} timed out", e)
        }
        return page.mangas.map { FoundManga(title = it.title, url = it.url) }
    }

    private companion object {
        const val SEARCH_TIMEOUT_MILLIS = 12_000L
    }
}

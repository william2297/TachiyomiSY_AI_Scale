package eu.kanade.tachiyomi.data.discover

import eu.kanade.tachiyomi.source.CatalogueSource
import kotlinx.coroutines.withTimeout
import tachiyomi.domain.source.service.SourceManager

/** Searches an installed source the same way the Browse search box does, first page only. */
class SourceTitleSearcher(private val sourceManager: SourceManager) : TitleSearcher {

    override suspend fun search(source: SearchSource, query: String): List<FoundManga> {
        val catalogue = sourceManager.get(source.id) as? CatalogueSource ?: return emptyList()
        val page = withTimeout(SEARCH_TIMEOUT_MILLIS) {
            catalogue.getSearchManga(1, query, catalogue.getFilterList())
        }
        return page.mangas.map { FoundManga(title = it.title, url = it.url) }
    }

    private companion object {
        const val SEARCH_TIMEOUT_MILLIS = 20_000L
    }
}

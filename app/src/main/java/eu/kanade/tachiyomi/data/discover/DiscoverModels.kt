package eu.kanade.tachiyomi.data.discover

import kotlinx.serialization.Serializable

/** One series as AniList describes it. */
@Serializable
data class AniListTitle(
    val id: Long,
    /** Every name AniList knows: English, romaji, native and synonyms. */
    val names: List<String>,
    /** The name to search for and show. */
    val title: String,
    val coverUrl: String?,
    val trending: Int,
    val popularity: Int,
    val score: Int?,
    val finished: Boolean,
    val chapters: Int?,
    val startYear: Int?,
    val endYear: Int?,
    val genres: List<String> = emptyList(),
)

/** A recommendation already matched to a title in one of the user's sources. */
@Serializable
data class StoredEntry(
    val anilistId: Long,
    val title: String,
    val coverUrl: String?,
    val sourceId: Long,
    val sourceName: String,
    val mangaUrl: String,
    val score: Int?,
    val completed: Boolean,
    val reason: String,
    /** Every name AniList knows, so a title the user already read under another name can be hidden. */
    val names: List<String> = emptyList(),
    val isNew: Boolean = false,
)

/** What an earlier search for one series found, so it is not searched for again every day. */
@Serializable
data class MatchRecord(
    val found: Boolean,
    val sourceId: Long = 0,
    val sourceName: String = "",
    val mangaUrl: String = "",
    val checkedAtMillis: Long,
    /** The sources that were searched, so a newly pinned source triggers a new search. */
    val sourcesKey: String = "",
)

/** A row built from the user's taste, stored with its own title because the title names the seed. */
@Serializable
data class StoredRow(
    val key: String,
    val title: String,
    val hint: String,
    val entries: List<StoredEntry>,
)

@Serializable
data class DiscoverCache(
    val updatedAtMillis: Long = 0,
    /** Section name to its entries, in display order. */
    val sections: Map<String, List<StoredEntry>> = emptyMap(),
    /** AniList ids of the trending list at the last refresh, to spot titles that just entered the top. */
    val previousTrendingIds: List<Long> = emptyList(),
    val matches: Map<Long, MatchRecord> = emptyMap(),
    val extras: List<StoredRow> = emptyList(),
    /** Why the last refresh failed, or null. The picks above are then from an earlier refresh. */
    val errorMessage: String? = null,
)

/** What the user reads, in the two forms the extra rows need. */
data class TasteProfile(
    /** Titles the user read or keeps, a few per day, to ask AniList what readers of them also read. */
    val seeds: List<String> = emptyList(),
    /** AniList genre names, most liked first. */
    val genres: List<String> = emptyList(),
)

/** What AniList knows about a seed title and the series readers recommend after it. */
data class SeedRecommendations(
    val seedNames: List<String>,
    val recommendations: List<AniListTitle>,
)

data class SearchSource(val id: Long, val name: String)

data class FoundManga(val title: String, val url: String)

/** Searches one source for a title. */
fun interface TitleSearcher {
    suspend fun search(source: SearchSource, query: String): List<FoundManga>
}

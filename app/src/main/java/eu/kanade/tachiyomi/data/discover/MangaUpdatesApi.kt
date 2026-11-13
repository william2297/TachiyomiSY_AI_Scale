package eu.kanade.tachiyomi.data.discover

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.POST
import eu.kanade.tachiyomi.network.awaitSuccess
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody

/** A series MangaUpdates recommends, with how strongly ([weight], higher first). */
data class MangaUpdatesPick(val name: String, val coverUrl: String?, val weight: Long, val byReaders: Boolean)

/** Reads recommendations from MangaUpdates (api.mangaupdates.com). Public data; no account or key is needed. */
interface RecommendationSource {
    suspend fun recommendationsFor(title: String): List<MangaUpdatesPick>

    /** Like [recommendationsFor], also accepting a series known under one of [otherNames] (AniList's names). */
    suspend fun recommendationsFor(title: String, otherNames: List<String>): List<MangaUpdatesPick> =
        recommendationsFor(title)
}

class MangaUpdatesApi(
    private val client: OkHttpClient,
    private val json: Json,
) : RecommendationSource {

    /**
     * Finds [title] on MangaUpdates and returns what its readers recommend (voted) and what it lists as similar by
     * category. Empty when the series is not found under that name.
     */
    override suspend fun recommendationsFor(title: String): List<MangaUpdatesPick> =
        recommendationsFor(title, emptyList())

    override suspend fun recommendationsFor(title: String, otherNames: List<String>): List<MangaUpdatesPick> {
        val payload = buildJsonObject {
            put("search", title)
            put("perpage", SEARCH_SIZE)
        }
        val body = json.encodeToString(JsonObject.serializer(), payload).toRequestBody("application/json".toMediaType())
        val search = client.newCall(POST("$URL/series/search", body = body)).awaitSuccess().use { it.body.string() }
        val results = json.decodeFromString(SearchResponse.serializer(), search).results
        // MangaUpdates searches loosely; only a series that really carries the title is accepted. Its main title is
        // often a romanisation spelled differently from AniList's, so the name the search hit on counts as well.
        val wanted = (listOf(title) + otherNames).filter { it.isNotBlank() }.distinct()
        val record = results.firstNotNullOfOrNull { result ->
            val record = result.record ?: return@firstNotNullOfOrNull null
            val carried = listOfNotNull(record.title, result.hitTitle).filter { it.isNotBlank() }
            record.takeIf { wanted.any { name -> TitleMatcher.match(carried, name) != TitleMatcher.Quality.NONE } }
        } ?: return emptyList()
        val series = client.newCall(GET("$URL/series/${record.seriesId}")).awaitSuccess().use { it.body.string() }
        val detail = json.decodeFromString(SeriesResponse.serializer(), series)
        val voted = detail.recommendations.map { it.toPick(byReaders = true) }
        val similar = detail.categoryRecommendations.map { it.toPick(byReaders = false) }
        return (voted.sortedByDescending { it.weight } + similar.sortedByDescending { it.weight })
            .distinctBy { TitleMatcher.normalize(it.name) }
    }

    private fun RecommendationDto.toPick(byReaders: Boolean) = MangaUpdatesPick(
        name = seriesName,
        coverUrl = seriesImage?.url?.original,
        weight = weight ?: 0L,
        byReaders = byReaders,
    )

    @Serializable
    private data class SearchResponse(val results: List<SearchResult> = emptyList())

    @Serializable
    private data class SearchResult(
        val record: SearchRecord? = null,
        @SerialName("hit_title") val hitTitle: String? = null,
    )

    @Serializable
    private data class SearchRecord(
        @SerialName("series_id") val seriesId: Long,
        val title: String = "",
    )

    @Serializable
    private data class SeriesResponse(
        val recommendations: List<RecommendationDto> = emptyList(),
        @SerialName("category_recommendations") val categoryRecommendations: List<RecommendationDto> = emptyList(),
    )

    @Serializable
    private data class RecommendationDto(
        @SerialName("series_name") val seriesName: String,
        @SerialName("series_image") val seriesImage: ImageDto? = null,
        val weight: Long? = null,
    )

    @Serializable
    private data class ImageDto(val url: ImageUrlDto? = null)

    @Serializable
    private data class ImageUrlDto(val original: String? = null)

    private companion object {
        const val URL = "https://api.mangaupdates.com/v1"
        const val SEARCH_SIZE = 5
    }
}

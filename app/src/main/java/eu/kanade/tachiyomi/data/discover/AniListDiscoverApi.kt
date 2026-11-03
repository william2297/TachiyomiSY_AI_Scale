package eu.kanade.tachiyomi.data.discover

import eu.kanade.presentation.browse.discover.DiscoverSection
import eu.kanade.tachiyomi.network.POST
import eu.kanade.tachiyomi.network.awaitSuccess
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody

/** The lists the Discover tab is built from. */
fun interface DiscoverListApi {
    /** One list for [section] (NEW_IN_TOP uses the RISING list) limited to a country of origin such as "KR". */
    suspend fun fetch(section: DiscoverSection, country: String): List<AniListTitle>
}

/**
 * Reads public lists from AniList (graphql.anilist.co). No login is needed for reading. The rate limit is about 30
 * requests per minute, so the caller spaces its requests out.
 */
class AniListDiscoverApi(
    private val client: OkHttpClient,
    private val json: Json,
) : DiscoverListApi {

    override suspend fun fetch(section: DiscoverSection, country: String): List<AniListTitle> {
        val body = json.encodeToString(QueryBody.serializer(), QueryBody(query(section, country)))
            .toRequestBody("application/json".toMediaType())
        val response = client.newCall(POST(URL, body = body)).awaitSuccess()
        return response.use { parse(json, it.body.string()) }
    }

    @Serializable
    private data class QueryBody(val query: String)

    companion object {
        private const val URL = "https://graphql.anilist.co"
        private const val PER_PAGE = 50

        private const val FIELDS = "id title{romaji english native} synonyms coverImage{large} trending popularity " +
            "averageScore status chapters startDate{year} endDate{year}"

        fun query(section: DiscoverSection, country: String): String {
            val filter = when (section) {
                DiscoverSection.RISING, DiscoverSection.NEW_IN_TOP ->
                    "sort:TRENDING_DESC, popularity_greater:5000"
                DiscoverSection.COMPLETED ->
                    "status:FINISHED, sort:SCORE_DESC, popularity_greater:20000"
                DiscoverSection.CLASSICS ->
                    "sort:SCORE_DESC, popularity_greater:50000, startDate_lesser:20210101"
                DiscoverSection.HIDDEN_GEMS ->
                    "sort:SCORE_DESC, popularity_greater:4000, popularity_lesser:25000, averageScore_greater:78"
                DiscoverSection.OLDER_ONGOING ->
                    "status:RELEASING, sort:SCORE_DESC, popularity_greater:20000, startDate_lesser:20230101"
            }
            return "{ Page(perPage:$PER_PAGE){ media(type:MANGA, countryOfOrigin:$country, $filter){ $FIELDS } } }"
        }

        fun parse(json: Json, body: String): List<AniListTitle> {
            val response = json.decodeFromString(Response.serializer(), body)
            val media = response.data?.page?.media
                ?: throw java.io.IOException("AniList returned no list: ${response.errors}")
            return media.map { it.toTitle() }
        }

        private fun MediaDto.toTitle(): AniListTitle {
            val names = (listOfNotNull(title.english, title.romaji, title.native) + synonyms)
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .distinct()
            return AniListTitle(
                id = id,
                names = names,
                title = title.english?.takeIf { it.isNotBlank() } ?: title.romaji ?: names.firstOrNull().orEmpty(),
                coverUrl = coverImage?.large,
                trending = trending ?: 0,
                popularity = popularity ?: 0,
                score = averageScore,
                finished = status == "FINISHED",
                chapters = chapters,
                startYear = startDate?.year,
                endYear = endDate?.year,
            )
        }
    }

    @Serializable
    private data class Response(val data: DataDto? = null, val errors: JsonElement? = null)

    @Serializable
    private data class DataDto(@SerialName("Page") val page: PageDto? = null)

    @Serializable
    private data class PageDto(val media: List<MediaDto> = emptyList())

    @Serializable
    private data class MediaDto(
        val id: Long,
        val title: TitleDto = TitleDto(),
        val synonyms: List<String> = emptyList(),
        val coverImage: CoverDto? = null,
        val trending: Int? = null,
        val popularity: Int? = null,
        val averageScore: Int? = null,
        val status: String? = null,
        val chapters: Int? = null,
        val startDate: DateDto? = null,
        val endDate: DateDto? = null,
    )

    @Serializable
    private data class TitleDto(val romaji: String? = null, val english: String? = null, val native: String? = null)

    @Serializable
    private data class CoverDto(val large: String? = null)

    @Serializable
    private data class DateDto(val year: Int? = null)
}

package eu.kanade.tachiyomi.data.discover

import eu.kanade.presentation.browse.discover.DiscoverSection
import eu.kanade.tachiyomi.network.POST
import eu.kanade.tachiyomi.network.awaitSuccess
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody

/** The lists the Discover tab is built from. */
fun interface DiscoverListApi {
    /** One list for [section] (NEW_IN_TOP uses the RISING list) limited to a country of origin such as "KR". */
    suspend fun fetch(section: DiscoverSection, country: String): List<AniListTitle>

    /** The series AniList readers recommend after [title], or null when AniList has no such series. */
    suspend fun recommendationsFor(title: String): SeedRecommendations? = null

    /** Trending series of one genre in one country. */
    suspend fun byGenre(genre: String, country: String): List<AniListTitle> = emptyList()

    /** Trending series that carry [tag] strongly in one country. */
    suspend fun byTag(tag: String, country: String): List<AniListTitle> = emptyList()

    /** Series that started after [sinceYyyymmdd] and already have readers. */
    suspend fun fresh(country: String, sinceYyyymmdd: Int): List<AniListTitle> = emptyList()

    // The same lists for several countries in one request. AniList counts requests, not lists, against its limit of
    // about 30 a minute, so asking for KR and JP together halves the waiting in a refresh. The defaults just ask
    // one country at a time.

    suspend fun fetchAll(section: DiscoverSection, countries: List<String>): List<AniListTitle> =
        countries.flatMap { fetch(section, it) }

    suspend fun byGenreAll(genre: String, countries: List<String>): List<AniListTitle> =
        countries.flatMap { byGenre(genre, it) }

    suspend fun byTagAll(tag: String, countries: List<String>): List<AniListTitle> =
        countries.flatMap { byTag(tag, it) }

    suspend fun freshAll(countries: List<String>, sinceYyyymmdd: Int): List<AniListTitle> =
        countries.flatMap { fresh(it, sinceYyyymmdd) }
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
        return parse(json, post(buildJsonObject { put("query", query(section, country)) }))
    }

    override suspend fun byGenre(genre: String, country: String): List<AniListTitle> {
        require(genre in PICKABLE_GENRES) { "Unknown genre $genre" }
        return parse(json, post(buildJsonObject { put("query", genreQuery(genre, country)) }))
    }

    override suspend fun byTag(tag: String, country: String): List<AniListTitle> {
        // The tag travels as a variable, so a name with quotes in it cannot break the query
        val body = post(
            buildJsonObject {
                put("query", tagQuery(country))
                put("variables", buildJsonObject { put("t", tag) })
            },
        )
        return parse(json, body)
    }

    override suspend fun fresh(country: String, sinceYyyymmdd: Int): List<AniListTitle> {
        return parse(json, post(buildJsonObject { put("query", freshQuery(country, sinceYyyymmdd)) }))
    }

    override suspend fun fetchAll(section: DiscoverSection, countries: List<String>): List<AniListTitle> {
        return combined(countries.map { query(section, it) })
    }

    override suspend fun byGenreAll(genre: String, countries: List<String>): List<AniListTitle> {
        require(genre in PICKABLE_GENRES) { "Unknown genre $genre" }
        return combined(countries.map { genreQuery(genre, it) })
    }

    override suspend fun byTagAll(tag: String, countries: List<String>): List<AniListTitle> {
        return combined(countries.map { tagQuery(it) }, buildJsonObject { put("t", tag) })
    }

    override suspend fun freshAll(countries: List<String>, sinceYyyymmdd: Int): List<AniListTitle> {
        return combined(countries.map { freshQuery(it, sinceYyyymmdd) })
    }

    /**
     * Sends several one-page queries as one request, each page under its own alias (c0, c1, ...), and returns the
     * pages in order. Tested against AniList on 2026-11-07: KR and JP trending (50 each, all fields) in one request
     * counted once against the rate limit and returned no errors.
     */
    private suspend fun combined(queries: List<String>, variables: JsonObject? = null): List<AniListTitle> {
        if (queries.isEmpty()) return emptyList()
        val header = queries.first().substringBefore('{')
        val pages = queries.mapIndexed { index, query ->
            val page = query.substring(query.indexOf("Page("), query.lastIndexOf('}'))
            "c$index: $page"
        }
        val combinedQuery = header + "{ " + pages.joinToString(" ") + " }"
        val body = post(
            buildJsonObject {
                put("query", combinedQuery)
                if (variables != null) put("variables", variables)
            },
        )
        val root = json.parseToJsonElement(body) as? JsonObject
            ?: throw java.io.IOException("AniList returned no data")
        val data = root["data"] as? JsonObject
            ?: throw java.io.IOException("AniList returned no list: ${root["errors"]}")
        return queries.indices.flatMap { index ->
            val media = (data["c$index"] as? JsonObject)?.get("media") ?: return@flatMap emptyList()
            json.decodeFromJsonElement(
                kotlinx.serialization.builtins.ListSerializer(MediaDto.serializer()),
                media,
            ).map { it.toTitle() }
        }
    }

    override suspend fun recommendationsFor(title: String): SeedRecommendations? {
        // The title travels as a variable, so quotes in it cannot break the query
        val body = post(
            buildJsonObject {
                put("query", RECOMMENDATIONS_QUERY)
                put("variables", buildJsonObject { put("s", title) })
            },
        )
        return parseRecommendations(json, body)
    }

    private suspend fun post(payload: JsonObject): String {
        val body = json.encodeToString(JsonObject.serializer(), payload).toRequestBody("application/json".toMediaType())
        val response = client.newCall(POST(URL, body = body)).awaitSuccess()
        return response.use { it.body.string() }
    }

    companion object {
        private const val URL = "https://graphql.anilist.co"
        private const val PER_PAGE = 50

        private const val FIELDS = "id title{romaji english native} synonyms coverImage{large} trending popularity " +
            "averageScore status chapters genres format startDate{year} endDate{year} isAdult countryOfOrigin " +
            "tags{name rank isMediaSpoiler category}"

        /** AniList's genre names; anything else is not sent, so a genre from a source's own list cannot break a query. */
        val KNOWN_GENRES = setOf(
            "Action", "Adventure", "Comedy", "Drama", "Ecchi", "Fantasy", "Horror", "Mahou Shoujo", "Mystery",
            "Psychological", "Romance", "Sci-Fi", "Slice of Life", "Sports", "Supernatural", "Thriller",
        )

        /**
         * The genres the user can choose for "My genres": the list above plus the rest of AniList's genre list.
         * "Hentai" is AniList's adult genre; it is only ever queried when the user picks it, never for the rows that
         * are built from the library's genres.
         */
        val PICKABLE_GENRES = KNOWN_GENRES + setOf("Hentai", "Mecha", "Music")

        private val RECOMMENDATIONS_QUERY = "query(\$s:String){ Media(search:\$s, type:MANGA){ $FIELDS " +
            "recommendations(sort:RATING_DESC, perPage:20){ nodes{ rating mediaRecommendation{ $FIELDS } } } } }"

        fun genreQuery(genre: String, country: String): String {
            return "{ Page(perPage:$PER_PAGE){ media(type:MANGA, countryOfOrigin:$country, genre:\"$genre\", " +
                "format_not:NOVEL, sort:TRENDING_DESC, popularity_greater:8000){ $FIELDS } } }"
        }

        fun tagQuery(country: String): String {
            return "query(\$t:String){ Page(perPage:$PER_PAGE){ media(type:MANGA, countryOfOrigin:$country, " +
                "tag_in:[\$t], minimumTagRank:60, format_not:NOVEL, sort:TRENDING_DESC, popularity_greater:3000){ " +
                "$FIELDS } } }"
        }

        fun freshQuery(country: String, sinceYyyymmdd: Int): String {
            return "{ Page(perPage:$PER_PAGE){ media(type:MANGA, countryOfOrigin:$country, status:RELEASING, " +
                "format_not:NOVEL, sort:POPULARITY_DESC, startDate_greater:$sinceYyyymmdd, popularity_greater:3000){ " +
                "$FIELDS } } }"
        }

        fun parseRecommendations(json: Json, body: String): SeedRecommendations? {
            val media = json.decodeFromString(RecommendationsResponse.serializer(), body).data?.media ?: return null
            val seed = media.toTitle()
            val recommended = media.recommendations?.nodes.orEmpty()
                .mapNotNull { it.mediaRecommendation }
                .filter { it.format != "NOVEL" }
                .map { it.toTitle() }
            return SeedRecommendations(
                seedNames = seed.names,
                recommendations = recommended,
                seedTags = seed.tags,
            )
        }

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
                genres = genres,
                isAdult = isAdult == true,
                country = countryOfOrigin,
                tags = tags
                    .filter { it.isMediaSpoiler != true && it.category?.startsWith("Technical") != true }
                    .filter { (it.rank ?: 0) >= MIN_TAG_RANK }
                    .sortedByDescending { it.rank ?: 0 }
                    .take(MAX_TAGS)
                    .map { TitleTag(it.name, it.rank ?: 0) },
            )
        }

        private const val MIN_TAG_RANK = 40
        private const val MAX_TAGS = 12
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
        val genres: List<String> = emptyList(),
        val tags: List<TagDto> = emptyList(),
        val format: String? = null,
        val startDate: DateDto? = null,
        val endDate: DateDto? = null,
        val recommendations: RecommendationsDto? = null,
        val isAdult: Boolean? = null,
        val countryOfOrigin: String? = null,
    )

    @Serializable
    private data class TagDto(
        val name: String,
        val rank: Int? = null,
        val isMediaSpoiler: Boolean? = null,
        val category: String? = null,
    )

    @Serializable
    private data class RecommendationsDto(val nodes: List<RecommendationNodeDto> = emptyList())

    @Serializable
    private data class RecommendationNodeDto(val mediaRecommendation: MediaDto? = null)

    @Serializable
    private data class RecommendationsResponse(val data: RecommendationsDataDto? = null)

    @Serializable
    private data class RecommendationsDataDto(@SerialName("Media") val media: MediaDto? = null)

    @Serializable
    private data class TitleDto(val romaji: String? = null, val english: String? = null, val native: String? = null)

    @Serializable
    private data class CoverDto(val large: String? = null)

    @Serializable
    private data class DateDto(val year: Int? = null)
}

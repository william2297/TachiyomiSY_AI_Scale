package eu.kanade.tachiyomi.data.discover

import eu.kanade.presentation.browse.discover.DiscoverSection
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.IOException

class DiscoverRefresherTest {

    private fun title(id: Long, name: String, trending: Int = 0, score: Int = 80, popularity: Int = 30_000) =
        AniListTitle(
            id = id, names = listOf(name), title = name, coverUrl = "https://img/$id", trending = trending,
            popularity = popularity, score = score, finished = false, chapters = null, startYear = 2020, endYear = null,
        )

    private class FakeApi(val lists: Map<DiscoverSection, List<AniListTitle>>, val failRising: Boolean = false) :
        DiscoverListApi {
        override suspend fun fetch(section: DiscoverSection, country: String): List<AniListTitle> {
            if (failRising && section == DiscoverSection.RISING) throw IOException("offline")
            // Only the first country has data, like a catalogue where the other one is empty
            return if (country == "KR") lists[section].orEmpty() else emptyList()
        }
    }

    private class FakeSearcher(val catalogue: Map<Long, Set<String>>) : TitleSearcher {
        val calls = mutableListOf<Pair<Long, String>>()
        override suspend fun search(source: SearchSource, query: String): List<FoundManga> {
            calls += source.id to query
            return catalogue[source.id].orEmpty().filter {
                it.contains(query, ignoreCase = true) ||
                    query.contains(it, true)
            }
                .map { FoundManga(it, "/series/${it.lowercase().replace(' ', '-')}") }
        }
    }

    private val sources = listOf(SearchSource(1, "Asura"), SearchSource(2, "Comick"))

    private fun refresher(
        api: DiscoverListApi,
        searcher: TitleSearcher,
        max: Int = 250,
        excluded: (AniListTitle) -> Boolean = { false },
    ) = DiscoverRefresher(
        api = api,
        searcher = searcher,
        now = { 1_700_000_000_000L },
        pause = {},
        countries = listOf("KR"),
        maxSearches = max,
        isExcluded = excluded,
    )

    @Test
    fun `only titles found in a source are shown, with that source`() = runTest {
        val api = FakeApi(
            mapOf(
                DiscoverSection.RISING to
                    listOf(title(1, "Alpha Hero", 90), title(2, "Beta Knight", 80), title(3, "Gamma", 70)),
            ),
        )
        val searcher = FakeSearcher(mapOf(1L to setOf("Alpha Hero"), 2L to setOf("Beta Knight")))
        val cache = refresher(api, searcher).refresh(DiscoverCache(), sources)

        val rising = cache.sections.getValue("RISING")
        assertEquals(listOf("Alpha Hero", "Beta Knight"), rising.map { it.title })
        assertEquals(listOf("Asura", "Comick"), rising.map { it.sourceName })
        assertEquals("Trending #1 on AniList", rising[0].reason)
        assertEquals("Trending #2 on AniList", rising[1].reason)
    }

    @Test
    fun `a title that entered the top 20 since the last refresh is listed as new`() = runTest {
        val api = FakeApi(
            mapOf(
                DiscoverSection.RISING to
                    listOf(title(1, "Alpha Hero", 90), title(2, "Beta Knight", 80), title(3, "Gamma Hero", 70)),
            ),
        )
        val searcher = FakeSearcher(mapOf(1L to setOf("Alpha Hero", "Beta Knight", "Gamma Hero")))
        val previous = DiscoverCache(previousTrendingIds = listOf(1L, 2L))
        val cache = refresher(api, searcher).refresh(previous, sources)

        val fresh = cache.sections.getValue("NEW_IN_TOP")
        assertEquals(listOf("Gamma Hero"), fresh.map { it.title })
        assertEquals("Entered the top 50 at #3", fresh[0].reason)
        assertEquals(listOf(1L, 2L, 3L), cache.previousTrendingIds)
    }

    @Test
    fun `the first refresh has no new row because there is nothing to compare with`() = runTest {
        val api = FakeApi(mapOf(DiscoverSection.RISING to listOf(title(1, "Alpha Hero", 90))))
        val cache = refresher(api, FakeSearcher(mapOf(1L to setOf("Alpha Hero")))).refresh(DiscoverCache(), sources)
        assertNull(cache.sections["NEW_IN_TOP"])
    }

    @Test
    fun `a second refresh does not search again for titles it already knows`() = runTest {
        val api = FakeApi(mapOf(DiscoverSection.RISING to listOf(title(1, "Alpha Hero", 90), title(2, "Nowhere", 80))))
        val searcher = FakeSearcher(mapOf(1L to setOf("Alpha Hero")))
        val first = refresher(api, searcher).refresh(DiscoverCache(), sources)
        val callsAfterFirst = searcher.calls.size
        assertTrue(callsAfterFirst > 0)

        refresher(api, searcher).refresh(first, sources)
        assertEquals(callsAfterFirst, searcher.calls.size)
        assertFalse(first.matches.getValue(2L).found)
    }

    @Test
    fun `pinning another source searches again for titles that were missing`() = runTest {
        val api = FakeApi(mapOf(DiscoverSection.RISING to listOf(title(2, "Nowhere", 80))))
        val searcher = FakeSearcher(mapOf(3L to setOf("Nowhere")))
        val first = refresher(api, searcher).refresh(DiscoverCache(), sources)
        assertNull(first.sections["RISING"])

        val more = sources + SearchSource(3, "Third")
        val second = refresher(api, searcher).refresh(first, more)
        assertEquals(listOf("Third"), second.sections.getValue("RISING").map { it.sourceName })
    }

    @Test
    fun `the search budget stops the work and unsearched titles are not marked missing`() = runTest {
        val api = FakeApi(
            mapOf(DiscoverSection.RISING to (1L..5L).map { title(it, "Series $it", 100 - it.toInt()) }),
        )
        val searcher = FakeSearcher(emptyMap())
        val cache = refresher(api, searcher, max = 3).refresh(DiscoverCache(), listOf(SearchSource(1, "Asura")))

        assertEquals(3, searcher.calls.size)
        assertEquals(setOf(1L, 2L, 3L), cache.matches.keys)
    }

    @Test
    fun `an unreadable trending list fails so the old picks stay`() = runTest {
        val api = FakeApi(emptyMap(), failRising = true)
        assertThrows(IOException::class.java) {
            kotlinx.coroutines.runBlocking {
                refresher(api, FakeSearcher(emptyMap())).refresh(DiscoverCache(), sources)
            }
        }
    }

    @Test
    fun `no pinned sources gives no entries and no searching`() = runTest {
        val api = FakeApi(mapOf(DiscoverSection.RISING to listOf(title(1, "Alpha Hero", 90))))
        val searcher = FakeSearcher(emptyMap())
        val cache = refresher(api, searcher).refresh(DiscoverCache(), emptyList())
        assertTrue(cache.sections.isEmpty())
        assertEquals(0, searcher.calls.size)
    }

    @Test
    fun `rows without a rebuild keep the earlier picks`() = runTest {
        val old = StoredEntry(9, "Old Pick", null, 1, "Asura", "/old", 80, false, "Rated 80 by 30k readers")
        val previous = DiscoverCache(sections = mapOf("CLASSICS" to listOf(old)))
        val api = FakeApi(mapOf(DiscoverSection.RISING to listOf(title(1, "Alpha Hero", 90))))
        val cache = refresher(api, FakeSearcher(mapOf(1L to setOf("Alpha Hero")))).refresh(previous, sources)
        assertEquals(listOf("Old Pick"), cache.sections.getValue("CLASSICS").map { it.title })
    }

    @Test
    fun `the daily pick is stable through the day and has the requested size`() {
        val pool = (1..50).toList()
        assertEquals(DiscoverRefresher.rotate(pool, 12, 5), DiscoverRefresher.rotate(pool, 12, 5))
        assertEquals(12, DiscoverRefresher.rotate(pool, 12, 5).size)
        assertTrue(DiscoverRefresher.rotate(pool, 12, 5) != DiscoverRefresher.rotate(pool, 12, 6))
    }

    @Test
    fun `reasons read naturally for each row`() {
        val t = title(
            1,
            "X",
            score = 89,
            popularity = 288_438,
        ).copy(finished = true, chapters = 201, endYear = 2021, startYear = 2018)
        assertEquals("Rated 89 by 288k readers", DiscoverRefresher.reason(DiscoverSection.CLASSICS, t, 1))
        assertEquals("201 chapters, finished 2021", DiscoverRefresher.reason(DiscoverSection.COMPLETED, t, 1))
        assertEquals("Running since 2018, score 89", DiscoverRefresher.reason(DiscoverSection.OLDER_ONGOING, t, 1))
        assertEquals("Score 89 with only 288k readers", DiscoverRefresher.reason(DiscoverSection.HIDDEN_GEMS, t, 1))
    }

    @Test
    fun `AniList json is read into titles with every name`() {
        val body = """
            {"data":{"Page":{"media":[
              {"id":10,"title":{"romaji":"Myst Might Mayhem","english":"Myst, Might, Mayhem","native":"x"},
               "synonyms":["MMM"],"coverImage":{"large":"https://img/10"},"trending":124,"popularity":20970,
               "averageScore":83,"status":"RELEASING","chapters":null,"startDate":{"year":2023},"endDate":{"year":null}},
              {"id":11,"title":{"romaji":"Only Romaji"},"synonyms":[],"status":"FINISHED","chapters":201,"endDate":{"year":2021}}
            ]}}}
        """.trimIndent()
        val titles = AniListDiscoverApi.parse(Json { ignoreUnknownKeys = true }, body)
        assertEquals(2, titles.size)
        assertEquals("Myst, Might, Mayhem", titles[0].title)
        assertEquals(listOf("Myst, Might, Mayhem", "Myst Might Mayhem", "x", "MMM"), titles[0].names)
        assertEquals(124, titles[0].trending)
        assertFalse(titles[0].finished)
        assertEquals("Only Romaji", titles[1].title)
        assertTrue(titles[1].finished)
        assertEquals(2021, titles[1].endYear)
        assertNotNull(AniListDiscoverApi.query(DiscoverSection.CLASSICS, "KR"))
    }

    @Test
    fun `a row holds up to fifty titles and keeps their names`() = runTest {
        val titles = (1L..60L).map { title(it, "Series $it", 200 - it.toInt()) }
        val searcher = FakeSearcher(mapOf(1L to titles.map { it.title }.toSet()))
        val cache = refresher(FakeApi(mapOf(DiscoverSection.RISING to titles)), searcher)
            .refresh(DiscoverCache(), sources)
        val rising = cache.sections.getValue("RISING")
        assertEquals(50, rising.size)
        assertEquals("Series 1", rising.first().title)
        assertEquals(listOf("Series 1"), rising.first().names)
    }

    @Test
    fun `the cards shown in a row are searched before the rest of the fifty`() = runTest {
        val titles = (1L..30L).map { title(it, "Series $it", 200 - it.toInt()) }
        val searcher = FakeSearcher(emptyMap())
        // Budget for exactly the first twelve titles, one source each
        refresher(FakeApi(mapOf(DiscoverSection.RISING to titles)), searcher, max = 12)
            .refresh(DiscoverCache(), listOf(SearchSource(1, "Asura")))
        assertEquals((1..12).map { "Series $it" }, searcher.calls.map { it.second })
    }

    @Test
    fun `titles the user already has are left out and never searched`() = runTest {
        val titles = listOf(title(1, "Owned Series", 90), title(2, "Fresh Series", 80))
        val searcher = FakeSearcher(mapOf(1L to setOf("Owned Series", "Fresh Series")))
        val cache = refresher(FakeApi(mapOf(DiscoverSection.RISING to titles)), searcher, excluded = { it.id == 1L })
            .refresh(DiscoverCache(), sources)
        assertEquals(listOf("Fresh Series"), cache.sections.getValue("RISING").map { it.title })
        assertEquals(listOf("Fresh Series"), searcher.calls.map { it.second })
    }

    @Test
    fun `a stray cancellation inside one search does not stop the refresh`() = runTest {
        val titles = (1L..3L).map { title(it, "Series $it", 100 - it.toInt()) }
        val searcher = object : TitleSearcher {
            override suspend fun search(source: SearchSource, query: String): List<FoundManga> {
                if (query == "Series 1") throw kotlinx.coroutines.CancellationException("stray")
                return listOf(FoundManga(query, "/s/$query"))
            }
        }
        val cache = refresher(FakeApi(mapOf(DiscoverSection.RISING to titles)), searcher)
            .refresh(DiscoverCache(), listOf(SearchSource(1, "Asura")))
        assertEquals(listOf("Series 2", "Series 3"), cache.sections.getValue("RISING").map { it.title })
    }

    @Test
    fun `a source that keeps failing is skipped for the rest of the run`() = runTest {
        val titles = (1L..8L).map { title(it, "Series $it", 100 - it.toInt()) }
        val calls = mutableListOf<Long>()
        val searcher = TitleSearcher { source, query ->
            calls += source.id
            if (source.id == 1L) throw IOException("slow") else listOf(FoundManga(query, "/s/$query"))
        }
        val cache = refresher(
            FakeApi(mapOf(DiscoverSection.RISING to titles)),
            searcher,
        ).refresh(DiscoverCache(), sources)
        assertEquals(3, calls.count { it == 1L })
        assertEquals(8, cache.sections.getValue("RISING").size)
    }

    @Test
    fun `picks are reported while matching goes on`() = runTest {
        val titles = (1L..9L).map { title(it, "Series $it", 100 - it.toInt()) }
        val searcher = FakeSearcher(mapOf(1L to titles.map { it.title }.toSet()))
        val reported = mutableListOf<Int>()
        val refresher = DiscoverRefresher(
            api = FakeApi(mapOf(DiscoverSection.RISING to titles)),
            searcher = searcher,
            now = { 1_700_000_000_000L },
            pause = {},
            countries = listOf("KR"),
            onProgress = { reported += it.sections["RISING"].orEmpty().size },
        )
        val cache = refresher.refresh(DiscoverCache(), listOf(SearchSource(1, "Asura")))
        assertTrue(reported.size >= 2)
        assertTrue(reported.first() < cache.sections.getValue("RISING").size)
    }

    @Test
    fun `the time budget ends the matching`() = runTest {
        val titles = (1L..20L).map { title(it, "Series $it", 100 - it.toInt()) }
        var clock = 1_700_000_000_000L
        val searcher = FakeSearcher(emptyMap())
        val refresher = DiscoverRefresher(
            api = FakeApi(mapOf(DiscoverSection.RISING to titles)),
            searcher = searcher,
            now = {
                clock += 60_000L
                clock
            },
            pause = {},
            countries = listOf("KR"),
            maxDurationMillis = 300_000L,
        )
        refresher.refresh(DiscoverCache(), listOf(SearchSource(1, "Asura")))
        assertTrue(searcher.calls.size < 20)
    }

    private inner class ExtraApi(
        val recs: Map<String, SeedRecommendations?> = emptyMap(),
        val genres: Map<String, List<AniListTitle>> = emptyMap(),
        val fresh: List<AniListTitle> = emptyList(),
    ) : DiscoverListApi {
        override suspend fun fetch(section: DiscoverSection, country: String): List<AniListTitle> =
            if (section == DiscoverSection.RISING && country == "KR") listOf(title(900, "Filler", 5)) else emptyList()

        override suspend fun recommendationsFor(title: String) = recs[title]
        override suspend fun byGenre(genre: String, country: String) = if (country ==
            "KR"
        ) {
            genres[genre].orEmpty()
        } else {
            emptyList()
        }
        override suspend fun fresh(country: String, sinceYyyymmdd: Int) = if (country == "KR") fresh else emptyList()
    }

    private fun everythingFound(vararg names: String) = FakeSearcher(mapOf(1L to names.toSet() + "Filler"))

    @Test
    fun `because you read row comes from the recommendations of a verified seed`() = runTest {
        val api = ExtraApi(
            recs = mapOf(
                "Omniscient Reader" to SeedRecommendations(
                    listOf("Omniscient Reader", "ORV"),
                    listOf(title(10, "Solo Leveling"), title(11, "Tower of God")),
                ),
            ),
        )
        val cache = refresher(api, everythingFound("Solo Leveling", "Tower of God"))
            .refresh(DiscoverCache(), sources, TasteProfile(seeds = listOf("Omniscient Reader")))
        val row = cache.extras.single { it.key.startsWith("because-") }
        assertEquals("Because you read Omniscient Reader", row.title)
        assertEquals(listOf("Solo Leveling", "Tower of God"), row.entries.map { it.title })
        assertEquals("Recommended by readers of Omniscient Reader", row.entries[0].reason)
    }

    @Test
    fun `a seed that AniList matched to a different series is ignored`() = runTest {
        val api = ExtraApi(
            recs = mapOf(
                "My Library Title" to
                    SeedRecommendations(listOf("Something Else Entirely"), listOf(title(10, "Solo Leveling"))),
            ),
        )
        val cache = refresher(api, everythingFound("Solo Leveling"))
            .refresh(DiscoverCache(), sources, TasteProfile(seeds = listOf("My Library Title")))
        assertTrue(cache.extras.none { it.key.startsWith("because-") })
    }

    @Test
    fun `your genres row merges the genres and ranks titles that match more of them first`() = runTest {
        val api = ExtraApi(
            genres = mapOf(
                "Action" to
                    listOf(
                        title(1, "Only Action", 50).copy(genres = listOf("Action")),
                        title(2, "Both", 10).copy(genres = listOf("Action", "Fantasy")),
                    ),
                "Fantasy" to
                    listOf(
                        title(2, "Both", 10).copy(genres = listOf("Action", "Fantasy")),
                        title(3, "Only Fantasy", 40).copy(genres = listOf("Fantasy")),
                    ),
            ),
        )
        val cache = refresher(api, everythingFound("Only Action", "Both", "Only Fantasy"))
            .refresh(DiscoverCache(), sources, TasteProfile(genres = listOf("Action", "Fantasy")))
        val row = cache.extras.single { it.key == "genres" }
        assertEquals("Both", row.entries.first().title)
        assertEquals("Matches Action, Fantasy", row.entries.first().reason)
        assertEquals(3, row.entries.size)
    }

    @Test
    fun `genres that AniList does not know are not sent`() = runTest {
        val cache = refresher(ExtraApi(), everythingFound())
            .refresh(DiscoverCache(), sources, TasteProfile(genres = listOf("Martial Arts")))
        assertTrue(cache.extras.none { it.key == "genres" })
    }

    private fun genreTitle(id: Long, name: String, trending: Int, vararg genres: String) =
        title(id, name, trending).copy(genres = genres.toList())

    private fun refresherAt(api: DiscoverListApi, searcher: TitleSearcher, nowMillis: () -> Long) = DiscoverRefresher(
        api = api,
        searcher = searcher,
        now = nowMillis,
        pause = {},
        countries = listOf("KR"),
    )

    private val dayMillis = 86_400_000L

    @Test
    fun `my genres ranks titles matching more of the picked genres first and keeps up to 50`() = runTest {
        val many = (1L..60L).map { genreTitle(100 + it, "Action $it", trending = it.toInt(), "Action") }
        val api = ExtraApi(
            genres = mapOf(
                "Action" to many + genreTitle(1, "Both", 1, "Action", "Fantasy"),
                "Fantasy" to listOf(genreTitle(1, "Both", 1, "Action", "Fantasy")),
            ),
        )
        val names = (many.map { it.title } + "Both").toTypedArray()
        val cache = refresher(api, everythingFound(*names))
            .refresh(DiscoverCache(), sources, TasteProfile(pickedGenres = listOf("Fantasy", "Action")))
        val row = cache.extras.single { it.key == DiscoverRefresher.MY_GENRES_KEY }
        assertEquals("My genres", row.title)
        assertEquals("Both", row.entries.first().title)
        assertEquals("Matches Action, Fantasy", row.entries.first().reason)
        assertEquals(50, row.entries.size)
        // After the two-genre title the rest follow by trending, highest first
        assertEquals("Action 60", row.entries[1].title)
    }

    @Test
    fun `my genres stays the same through the day even when AniList changes`() = runTest {
        var clock = 1_700_000_000_000L
        val first = ExtraApi(genres = mapOf("Action" to listOf(genreTitle(1, "Old One", 9, "Action"))))
        val cache1 = refresherAt(first, everythingFound("Old One", "New One")) { clock }
            .refresh(DiscoverCache(), sources, TasteProfile(pickedGenres = listOf("Action")))

        clock += 60_000L
        val second = ExtraApi(genres = mapOf("Action" to listOf(genreTitle(2, "New One", 99, "Action"))))
        val cache2 = refresherAt(second, everythingFound("Old One", "New One")) { clock }
            .refresh(cache1, sources, TasteProfile(pickedGenres = listOf("Action")))

        val row = cache2.extras.single { it.key == DiscoverRefresher.MY_GENRES_KEY }
        assertEquals(listOf("Old One"), row.entries.map { it.title })
    }

    @Test
    fun `my genres is ranked again the next day`() = runTest {
        var clock = 1_700_000_000_000L
        val first = ExtraApi(genres = mapOf("Action" to listOf(genreTitle(1, "Old One", 9, "Action"))))
        val cache1 = refresherAt(first, everythingFound("Old One", "New One")) { clock }
            .refresh(DiscoverCache(), sources, TasteProfile(pickedGenres = listOf("Action")))

        clock += dayMillis + 60_000L
        val second = ExtraApi(genres = mapOf("Action" to listOf(genreTitle(2, "New One", 99, "Action"))))
        val cache2 = refresherAt(second, everythingFound("Old One", "New One")) { clock }
            .refresh(cache1, sources, TasteProfile(pickedGenres = listOf("Action")))

        val row = cache2.extras.single { it.key == DiscoverRefresher.MY_GENRES_KEY }
        assertEquals(listOf("New One"), row.entries.map { it.title })
    }

    @Test
    fun `changing the picked genres rebuilds my genres at once`() = runTest {
        val clock = 1_700_000_000_000L
        val api = ExtraApi(
            genres = mapOf(
                "Action" to listOf(genreTitle(1, "Action Hit", 9, "Action")),
                "Romance" to listOf(genreTitle(2, "Romance Hit", 9, "Romance")),
            ),
        )
        val searcher = everythingFound("Action Hit", "Romance Hit")
        val cache1 = refresherAt(api, searcher) { clock }
            .refresh(DiscoverCache(), sources, TasteProfile(pickedGenres = listOf("Action")))
        val cache2 = refresherAt(api, searcher) { clock }
            .refresh(cache1, sources, TasteProfile(pickedGenres = listOf("Action", "Romance")))

        val row = cache2.extras.single { it.key == DiscoverRefresher.MY_GENRES_KEY }
        assertEquals(setOf("Action Hit", "Romance Hit"), row.entries.map { it.title }.toSet())
        assertEquals(listOf("Action", "Romance"), cache2.myGenres.genres)
    }

    @Test
    fun `clearing the picked genres removes my genres`() = runTest {
        val api = ExtraApi(genres = mapOf("Action" to listOf(genreTitle(1, "Action Hit", 9, "Action"))))
        val searcher = everythingFound("Action Hit", "Fresh")
        val cache1 = refresher(api, searcher)
            .refresh(DiscoverCache(), sources, TasteProfile(pickedGenres = listOf("Action")))
        assertTrue(cache1.extras.any { it.key == DiscoverRefresher.MY_GENRES_KEY })
        val cache2 = refresher(ExtraApi(fresh = listOf(title(5, "Fresh"))), searcher)
            .refresh(cache1, sources, TasteProfile())
        assertTrue(cache2.extras.none { it.key == DiscoverRefresher.MY_GENRES_KEY })
        assertTrue(cache2.myGenres.titles.isEmpty())
    }

    @Test
    fun `my genres keeps the old list when AniList cannot be reached on a new day`() = runTest {
        var clock = 1_700_000_000_000L
        val api = ExtraApi(genres = mapOf("Action" to listOf(genreTitle(1, "Old One", 9, "Action"))))
        val cache1 = refresherAt(api, everythingFound("Old One")) { clock }
            .refresh(DiscoverCache(), sources, TasteProfile(pickedGenres = listOf("Action")))
        clock += dayMillis + 60_000L
        val cache2 = refresherAt(ExtraApi(), everythingFound("Old One")) { clock }
            .refresh(cache1, sources, TasteProfile(pickedGenres = listOf("Action")))
        val row = cache2.extras.single { it.key == DiscoverRefresher.MY_GENRES_KEY }
        assertEquals(listOf("Old One"), row.entries.map { it.title })
    }

    @Test
    fun `my genres is searched before the other rows when the search budget is small`() = runTest {
        val api =
            ExtraApi(
                genres = mapOf(
                    "Action" to (1L..5L).map {
                        genreTitle(200 + it, "Mine $it", it.toInt(), "Action")
                    },
                ),
            )
        val searcher = everythingFound("Mine 1", "Mine 2", "Mine 3", "Mine 4", "Mine 5")
        val cache = refresher(api, searcher, max = 5)
            .refresh(DiscoverCache(), sources, TasteProfile(pickedGenres = listOf("Action")))
        val row = cache.extras.single { it.key == DiscoverRefresher.MY_GENRES_KEY }
        assertEquals(5, row.entries.size)
    }

    @Test
    fun `fresh starts row lists new series with their readers`() = runTest {
        val api = ExtraApi(fresh = listOf(title(5, "New Hit", popularity = 8195).copy(startYear = 2026)))
        val cache = refresher(api, everythingFound("New Hit")).refresh(DiscoverCache(), sources)
        val row = cache.extras.single { it.key == "fresh" }
        assertEquals("Started 2026, 8k readers already", row.entries.single().reason)
    }

    @Test
    fun `a title that was not in any row at the previous refresh is marked new`() = runTest {
        val first = refresher(ExtraApi(fresh = listOf(title(5, "Old Hit"))), everythingFound("Old Hit"))
            .refresh(DiscoverCache(), sources)
        assertTrue(first.extras.single { it.key == "fresh" }.entries.none { it.isNew })

        val second = refresher(
            ExtraApi(fresh = listOf(title(5, "Old Hit"), title(6, "Brand New"))),
            everythingFound("Old Hit", "Brand New"),
        )
            .refresh(first, sources)
        val entries = second.extras.single { it.key == "fresh" }.entries.associateBy { it.title }
        assertFalse(entries.getValue("Old Hit").isNew)
        assertTrue(entries.getValue("Brand New").isNew)
    }

    @Test
    fun `titles the user hid or already has never reach the extra rows`() = runTest {
        val api = ExtraApi(fresh = listOf(title(5, "Seen It"), title(6, "Keep Me")))
        val cache = refresher(api, everythingFound("Seen It", "Keep Me"), excluded = { it.id == 5L })
            .refresh(DiscoverCache(), sources)
        assertEquals(listOf("Keep Me"), cache.extras.single { it.key == "fresh" }.entries.map { it.title })
    }
}

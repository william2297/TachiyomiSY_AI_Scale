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
}

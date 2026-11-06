package eu.kanade.presentation.browse.discover

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DiscoverDedupeTest {

    private fun entry(id: Long) = DiscoverEntry(
        id = id, title = "T$id", coverUrl = null, sourceId = 1, sourceName = "S", mangaUrl = "/$id",
        score = 80, completed = false, reason = "",
    )

    private fun row(key: String, vararg ids: Long) = DiscoverRow(key, key, "", ids.map { entry(it) })

    @Test
    fun `a title shows once, and the general rows keep what the personal rows did not take`() {
        val (sections, rows) = DiscoverDedupe.apply(
            mapOf(
                DiscoverSection.RISING to listOf(entry(1), entry(2)),
                DiscoverSection.COMPLETED to listOf(entry(2), entry(3)),
            ),
            listOf(row("because-x", 3, 4, 5)),
        )
        assertEquals(listOf(1L, 2L), sections.getValue(DiscoverSection.RISING).map { it.id })
        assertEquals(listOf(listOf(3L, 4L, 5L)), rows.map { r -> r.entries.map { it.id } })
        // 3 went to the personal row first, 2 to Rising
        assertEquals(false, DiscoverSection.COMPLETED in sections)
    }

    @Test
    fun `personal rows claim first, in their priority, and keep display order`() {
        val (sections, rows) = DiscoverDedupe.apply(
            mapOf(
                DiscoverSection.RISING to listOf(entry(1), entry(9)),
                DiscoverSection.NEW_IN_TOP to listOf(entry(1)),
            ),
            listOf(row("genres", 5, 6), row("taste", 5, 7), row("my-genres", 1, 5)),
        )
        assertEquals(listOf(listOf(6L), listOf(7L), listOf(1L, 5L)), rows.map { r -> r.entries.map { it.id } })
        assertEquals(listOf(9L), sections.getValue(DiscoverSection.RISING).map { it.id })
        assertEquals(false, DiscoverSection.NEW_IN_TOP in sections)
    }

    @Test
    fun `a thin because-you-read row is dropped, other rows only when empty`() {
        val (_, rows) = DiscoverDedupe.apply(
            mapOf(DiscoverSection.RISING to listOf(entry(1))),
            listOf(row("because-a", 2), row("fresh", 3), row("because-b", 4, 5, 6)),
        )
        assertEquals(listOf("fresh", "because-b"), rows.map { it.key })
    }
}

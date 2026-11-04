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
    fun `a title shows in the first row that has it`() {
        val (sections, rows) = DiscoverDedupe.apply(
            mapOf(
                DiscoverSection.RISING to listOf(entry(1), entry(2)),
                DiscoverSection.COMPLETED to listOf(entry(2), entry(3)),
            ),
            listOf(row("because-x", 3, 4)),
        )
        assertEquals(listOf(1L, 2L), sections.getValue(DiscoverSection.RISING).map { it.id })
        assertEquals(listOf(3L), sections.getValue(DiscoverSection.COMPLETED).map { it.id })
        assertEquals(listOf(4L), rows.single().entries.map { it.id })
    }

    @Test
    fun `rows meant to overlap keep their titles and take none from the others`() {
        val (sections, rows) = DiscoverDedupe.apply(
            mapOf(
                DiscoverSection.RISING to listOf(entry(1)),
                DiscoverSection.NEW_IN_TOP to listOf(entry(1)),
                DiscoverSection.COMPLETED to listOf(entry(5)),
            ),
            listOf(row("genres", 5, 6), row("taste", 5, 7), row("my-genres", 1, 5)),
        )
        assertEquals(listOf(1L), sections.getValue(DiscoverSection.NEW_IN_TOP).map { it.id })
        assertEquals(listOf(5L), sections.getValue(DiscoverSection.COMPLETED).map { it.id })
        assertEquals(listOf(listOf(5L, 6L), listOf(5L, 7L), listOf(1L, 5L)), rows.map { r -> r.entries.map { it.id } })
    }

    @Test
    fun `a row that loses all its titles is dropped`() {
        val (_, rows) = DiscoverDedupe.apply(
            mapOf(DiscoverSection.RISING to listOf(entry(1))),
            listOf(row("fresh", 1)),
        )
        assertEquals(0, rows.size)
    }
}

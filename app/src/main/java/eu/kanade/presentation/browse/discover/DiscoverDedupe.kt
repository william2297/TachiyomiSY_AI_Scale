package eu.kanade.presentation.browse.discover

/**
 * A title shows in one row only, the first row that has it, so the page does not repeat itself. Rows that are meant
 * to overlap or that the user steers are left alone and neither take titles nor lose them: "New in the top 50" is a
 * slice of Rising, and "Your genres", "Your taste" and "My genres" are different views of the user's own taste.
 */
internal object DiscoverDedupe {

    private val EXEMPT_SECTIONS = setOf(DiscoverSection.NEW_IN_TOP)
    private val EXEMPT_ROWS = setOf("genres", "taste", "my-genres")

    fun apply(
        sections: Map<DiscoverSection, List<DiscoverEntry>>,
        rows: List<DiscoverRow>,
    ): Pair<Map<DiscoverSection, List<DiscoverEntry>>, List<DiscoverRow>> {
        val seen = mutableSetOf<Long>()
        val newSections = linkedMapOf<DiscoverSection, List<DiscoverEntry>>()
        for ((section, entries) in sections) {
            val kept = if (section in EXEMPT_SECTIONS) entries else entries.filter { seen.add(it.id) }
            if (kept.isNotEmpty()) newSections[section] = kept
        }
        val newRows = rows.mapNotNull { row ->
            val kept = if (row.key in EXEMPT_ROWS) row.entries else row.entries.filter { seen.add(it.id) }
            kept.takeIf { it.isNotEmpty() }?.let { row.copy(entries = it) }
        }
        return newSections to newRows
    }
}

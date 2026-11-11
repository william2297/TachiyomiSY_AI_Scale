package eu.kanade.presentation.browse.discover

/**
 * A title shows in one row only, so the page does not repeat itself. The rows built from the user's own reading
 * claim their titles first (in [PERSONAL_ORDER], then any "Because you read" row), so a series stays where it is
 * explained best; the general rows (Rising, Completed, ...) keep what is left. On the Oppo Pad Mini the old rule
 * exempted the personal rows and the same series showed twice ("Reincarnated Murim Lord", "Reincarnation Coliseum").
 *
 * "Because you read" rows with fewer than [MIN_BECAUSE_ROW] titles are dropped: a single card under a big heading
 * reads like a broken row. Other rows are kept as long as anything is left.
 */
internal object DiscoverDedupe {

    private val PERSONAL_ORDER = listOf("my-genres", "taste", "genres", "fresh")
    private const val BECAUSE_PREFIX = "because-"
    private const val MIN_BECAUSE_ROW = 3

    fun apply(
        sections: Map<DiscoverSection, List<DiscoverEntry>>,
        rows: List<DiscoverRow>,
    ): Pair<Map<DiscoverSection, List<DiscoverEntry>>, List<DiscoverRow>> {
        val seen = mutableSetOf<Long>()

        fun claimOrder(row: DiscoverRow): Int {
            val index = PERSONAL_ORDER.indexOf(row.key)
            return when {
                // The row built from the title the user picked keeps everything it found
                row.key == "picked-seed" -> -1
                row.key == "my-genres" -> 0
                row.key.startsWith(BECAUSE_PREFIX) -> 1
                index >= 0 -> 1 + index
                else -> PERSONAL_ORDER.size + 1
            }
        }

        // Claim in priority order, then hand the rows back in their display order
        val keptRows = mutableMapOf<Int, DiscoverRow>()
        rows.withIndex()
            .sortedWith(compareBy({ claimOrder(it.value) }, { it.index }))
            .forEach { (index, row) ->
                val kept = row.entries.filter { seen.add(it.id) }
                val minimum = if (row.key.startsWith(BECAUSE_PREFIX)) MIN_BECAUSE_ROW else 1
                if (kept.size >= minimum) keptRows[index] = row.copy(entries = kept)
            }

        val newSections = linkedMapOf<DiscoverSection, List<DiscoverEntry>>()
        for ((section, entries) in sections) {
            val kept = entries.filter { seen.add(it.id) }
            if (kept.isNotEmpty()) newSections[section] = kept
        }
        val newRows = rows.indices.mapNotNull { keptRows[it] }
        return newSections to newRows
    }
}

package eu.kanade.presentation.browse.discover

/**
 * Sample picks so the Discover tab can be opened before the AniList matcher is wired in.
 * Titles are public series names; sources, scores and reasons are made up. Replace, do not ship as data.
 */
object DiscoverSampleData {

    private fun entry(
        id: Long,
        title: String,
        source: String,
        score: Int?,
        completed: Boolean,
        reason: String,
    ) = DiscoverEntry(
        id = id,
        title = title,
        coverUrl = null,
        sourceId = if (source == "Asura Scans") 1L else 2L,
        sourceName = source,
        mangaUrl = "/series/$id",
        score = score,
        completed = completed,
        reason = reason,
    )

    val state = DiscoverState(
        sections = mapOf(
            DiscoverSection.RISING to listOf(
                entry(1, "The Max Level Hero has Returned!", "Asura Scans", 82, false, "Trending #3, up 4 days"),
                entry(2, "Swordmaster's Youngest Son", "Asura Scans", 84, false, "Trending #5, up 2 days"),
                entry(3, "Return of the Mount Hua Sect", "Comick", 87, false, "Trending #8, new chapter today"),
                entry(4, "Surviving The Game as a Barbarian", "Comick", 80, false, "Trending #11, up 6 days"),
            ),
            DiscoverSection.NEW_IN_TOP to listOf(
                entry(10, "Myst, Might, Mayhem", "Asura Scans", 79, false, "Entered the top 20 at #17"),
                entry(11, "Pick Me Up!", "Comick", 81, false, "Entered the top 20 at #19"),
                entry(12, "Reincarnation of the Fist King", "Asura Scans", 76, false, "Entered the top 20 at #20"),
            ),
            DiscoverSection.COMPLETED to listOf(
                entry(20, "Solo Leveling", "Comick", 86, true, "179 chapters, finished 2021"),
                entry(21, "Omniscient Reader", "Comick", 89, true, "Finished, rated in the top 1%"),
                entry(22, "The Breaker", "Comick", 83, true, "72 chapters, a weekend read"),
                entry(23, "Tower of God", "Comick", 85, true, "Season finale out"),
            ),
            DiscoverSection.CLASSICS to listOf(
                entry(30, "Noblesse", "Comick", 84, true, "Since 2007, top 50 all time"),
                entry(31, "The Gamer", "Comick", 78, false, "Since 2013, top 100 all time"),
                entry(32, "Lookism", "Comick", 80, false, "Since 2014, top 100 all time"),
            ),
            DiscoverSection.HIDDEN_GEMS to listOf(
                entry(40, "Hellogin", "Asura Scans", 83, false, "Score 83, few readers so far"),
                entry(41, "Sandmancer of the Scorched Desert", "Asura Scans", 81, false, "Score 81, under 5k readers"),
                entry(42, "Limitless Predation", "Asura Scans", null, false, "Quietly climbing in its genre"),
            ),
            DiscoverSection.OLDER_ONGOING to listOf(
                entry(50, "God Of Blackfield", "Asura Scans", 77, false, "Running since 2020, weekly"),
                entry(51, "Murim Psychopath", "Comick", 75, false, "Running since 2021, steady score"),
            ),
        ),
        loading = false,
        updatedAtMillis = null,
        error = null,
    )
}

package eu.kanade.presentation.browse.discover

/**
 * One recommendation, already matched to a title in one of the user's pinned sources.
 *
 * @param score AniList mean score, 0 to 100, or null when AniList has none.
 * @param reason One short line on why it is listed, for example "Trending #3, up 4 days".
 */
data class DiscoverEntry(
    val id: Long,
    val title: String,
    val coverUrl: String?,
    val sourceId: Long,
    val sourceName: String,
    val mangaUrl: String,
    val score: Int?,
    val completed: Boolean,
    val reason: String,
)

/** Rows of the Discover tab, in display order. */
enum class DiscoverSection {
    RISING,
    NEW_IN_TOP,
    COMPLETED,
    CLASSICS,
    HIDDEN_GEMS,
    OLDER_ONGOING,
}

/**
 * @param loading True while a refresh runs. With sections present this shows the pull indicator only.
 * @param updatedAtMillis When the shown sections were fetched, or null if never.
 * @param error Set when the last refresh failed; sections then hold the cached picks, if any.
 */
data class DiscoverState(
    val sections: Map<DiscoverSection, List<DiscoverEntry>>,
    val loading: Boolean,
    val updatedAtMillis: Long?,
    val error: String?,
)

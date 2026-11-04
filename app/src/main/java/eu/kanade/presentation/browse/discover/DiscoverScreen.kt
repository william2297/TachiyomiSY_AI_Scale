package eu.kanade.presentation.browse.discover

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.icerock.moko.resources.StringResource
import eu.kanade.presentation.browse.discover.components.DiscoverCard
import eu.kanade.presentation.browse.discover.components.DiscoverCardSkeleton
import eu.kanade.presentation.browse.discover.components.DiscoverGenreSheet
import eu.kanade.presentation.browse.discover.components.DiscoverRisingCard
import eu.kanade.presentation.browse.discover.components.DiscoverRisingCardSkeleton
import eu.kanade.presentation.browse.discover.components.DiscoverRisingSeeAllCard
import eu.kanade.presentation.browse.discover.components.DiscoverSeeAllCard
import eu.kanade.presentation.browse.discover.components.MyGenresHeader
import eu.kanade.presentation.browse.discover.components.MyGenresInvite
import eu.kanade.presentation.browse.discover.components.MyGenresPending
import eu.kanade.presentation.browse.discover.components.SkeletonLine
import eu.kanade.presentation.browse.discover.components.rememberGenreSheetOpen
import mihon.icons.phosphor.Phosphor
import mihon.icons.phosphor.bold.ArrowsClockwise
import mihon.icons.phosphor.bold.CloudSlash
import mihon.icons.phosphor.bold.Compass
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.PullRefresh
import tachiyomi.presentation.core.i18n.stringResource
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * "What is worth reading now": rows of AniList picks matched to the user's pinned sources.
 * Rows without entries are hidden; with nothing at all the screen shows an empty or offline state.
 */
@Composable
fun DiscoverScreen(
    state: DiscoverState,
    contentPadding: PaddingValues,
    onOpen: (DiscoverEntry) -> Unit,
    onRefresh: () -> Unit,
    onSeeAll: (DiscoverSection) -> Unit = {},
    onSeeAllRow: (DiscoverRow) -> Unit = {},
    genres: List<String> = emptyList(),
    onPickGenres: (Set<String>) -> Unit = {},
) {
    var genreSheetOpen by rememberGenreSheetOpen()
    val visible = DiscoverSection.entries.mapNotNull { section ->
        state.sections[section]?.takeIf { it.isNotEmpty() }?.let { section to it }
    }
    val hasData = visible.isNotEmpty() || state.extraRows.isNotEmpty()

    PullRefresh(
        refreshing = state.loading && hasData,
        enabled = true,
        onRefresh = onRefresh,
        indicatorPadding = contentPadding,
    ) {
        // Landscape tablet: pair the regular rows side by side so about three rows fit on one screen
        // instead of one and a half. Rising keeps the full width; the page stays a single vertical scroll.
        val wide = LocalConfiguration.current.screenWidthDp >= WIDE_LAYOUT_MIN_WIDTH_DP
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = contentPadding.calculateTopPadding(),
                bottom = contentPadding.calculateBottomPadding() + 16.dp,
            ),
        ) {
            when {
                hasData -> {
                    item(key = "status") {
                        StatusLine(
                            updatedAtMillis = state.updatedAtMillis,
                            note = state.tasteNote,
                            onRefresh = onRefresh,
                        )
                    }
                    if (state.error != null) {
                        item(key = "offline") { OfflineBanner() }
                    }
                    if (wide) {
                        val (rising, rest) = visible.partition { it.first == DiscoverSection.RISING }
                        rising.forEach { (section, entries) -> section(section, entries, onOpen, onSeeAll) }
                        rest.chunked(2).forEach { pair -> sectionPair(pair, onOpen, onSeeAll) }
                    } else {
                        visible.forEach { (section, entries) -> section(section, entries, onOpen, onSeeAll) }
                    }
                    state.extraRows.filterNot { it.key == MY_GENRES_KEY }
                        .forEach { row -> extraRow(row, onOpen, onSeeAllRow) }
                    if (genres.isNotEmpty()) {
                        myGenres(
                            state = state,
                            genres = genres,
                            onOpen = onOpen,
                            onSeeAllRow = onSeeAllRow,
                            onEdit = { genreSheetOpen = true },
                        )
                    }
                }
                state.loading -> {
                    state.status?.let { status ->
                        item(key = "progress") {
                            Text(
                                text = status,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            )
                        }
                    }
                    loadingSkeleton(wide)
                }
                else -> item(key = "empty") {
                    EmptyOrOffline(
                        offline = state.error != null,
                        onRefresh = onRefresh,
                        modifier = Modifier.fillParentMaxSize(),
                    )
                }
            }
        }
    }

    if (genreSheetOpen) {
        DiscoverGenreSheet(
            genres = genres,
            picked = state.pickedGenres,
            onApply = { picked ->
                genreSheetOpen = false
                onPickGenres(picked)
            },
            onDismiss = { genreSheetOpen = false },
        )
    }
}

private const val WIDE_LAYOUT_MIN_WIDTH_DP = 960

/** Same key as DiscoverRefresher.MY_GENRES_KEY; the row is drawn by [myGenres], not as a plain extra row. */
private const val MY_GENRES_KEY = "my-genres"

/**
 * The "My genres" slot, after the other rows built from taste (the refresher puts it last as well). No genres picked:
 * an invitation to pick some. Genres picked: a header with them and an edit button, then the row, its placeholders
 * while it is first built, or a note when nothing matched.
 */
private fun LazyListScope.myGenres(
    state: DiscoverState,
    genres: List<String>,
    onOpen: (DiscoverEntry) -> Unit,
    onSeeAllRow: (DiscoverRow) -> Unit,
    onEdit: () -> Unit,
) {
    if (state.pickedGenres.isEmpty()) {
        item(key = "my-genres-invite") {
            MyGenresInvite(genres = genres, onChoose = onEdit)
        }
        return
    }
    val picked = genres.filter { it in state.pickedGenres }
    val row = state.extraRows.firstOrNull { it.key == MY_GENRES_KEY }
    item(key = "extra-header-$MY_GENRES_KEY") {
        MyGenresHeader(picked = picked, updating = state.loading, onEdit = onEdit)
    }
    item(key = "extra-row-$MY_GENRES_KEY") {
        if (row != null) {
            ExtraRowCards(row, onOpen, onSeeAllRow)
        } else {
            MyGenresPending(loading = state.loading)
        }
    }
}

/** Two regular rows next to each other; an odd last row keeps the left half. */
private fun LazyListScope.sectionPair(
    pair: List<Pair<DiscoverSection, List<DiscoverEntry>>>,
    onOpen: (DiscoverEntry) -> Unit,
    onSeeAll: (DiscoverSection) -> Unit,
) {
    item(key = "pair-" + pair.joinToString("-") { it.first.name }) {
        Row(Modifier.fillMaxWidth()) {
            pair.forEach { (section, entries) ->
                Column(Modifier.weight(1f)) {
                    SectionHeader(title = section.titleRes, hint = section.hintRes)
                    SectionRow(section, entries, onOpen, onSeeAll)
                }
            }
            if (pair.size == 1) Spacer(Modifier.weight(1f))
        }
    }
}

private fun LazyListScope.section(
    section: DiscoverSection,
    entries: List<DiscoverEntry>,
    onOpen: (DiscoverEntry) -> Unit,
    onSeeAll: (DiscoverSection) -> Unit,
) {
    item(key = "header-${section.name}") {
        SectionHeader(title = section.titleRes, hint = section.hintRes)
    }
    item(key = "row-${section.name}") {
        SectionRow(section, entries, onOpen, onSeeAll)
    }
}

/** A row built from the user's taste. Most are short; one with more than [ROW_LIMIT] entries ends in "See all". */
private fun LazyListScope.extraRow(
    row: DiscoverRow,
    onOpen: (DiscoverEntry) -> Unit,
    onSeeAllRow: (DiscoverRow) -> Unit,
) {
    item(key = "extra-header-${row.key}") {
        TextSectionHeader(title = row.title, hint = row.hint)
    }
    item(key = "extra-row-${row.key}") {
        ExtraRowCards(row, onOpen, onSeeAllRow)
    }
}

@Composable
private fun ExtraRowCards(row: DiscoverRow, onOpen: (DiscoverEntry) -> Unit, onSeeAllRow: (DiscoverRow) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.Top,
    ) {
        items(items = row.entries.take(ROW_LIMIT), key = { it.id }) { entry ->
            DiscoverCard(entry = entry, onClick = { onOpen(entry) })
        }
        if (row.entries.size > ROW_LIMIT) {
            item(key = "see-all") {
                DiscoverSeeAllCard(count = row.entries.size, onClick = { onSeeAllRow(row) })
            }
        }
    }
}

/** Shows the first [ROW_LIMIT] entries; with more, a "See all" tile ends the row and opens the full list. */
@Composable
private fun SectionRow(
    section: DiscoverSection,
    entries: List<DiscoverEntry>,
    onOpen: (DiscoverEntry) -> Unit,
    onSeeAll: (DiscoverSection) -> Unit,
) {
    val rising = section == DiscoverSection.RISING
    LazyRow(
        contentPadding = PaddingValues(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(if (rising) 12.dp else 4.dp),
        verticalAlignment = Alignment.Top,
    ) {
        items(items = entries.take(ROW_LIMIT), key = { it.id }) { entry ->
            if (rising) {
                DiscoverRisingCard(entry = entry, onClick = { onOpen(entry) })
            } else {
                DiscoverCard(entry = entry, onClick = { onOpen(entry) })
            }
        }
        if (entries.size > ROW_LIMIT) {
            item(key = "see-all") {
                if (rising) {
                    DiscoverRisingSeeAllCard(count = entries.size, onClick = { onSeeAll(section) })
                } else {
                    DiscoverSeeAllCard(count = entries.size, onClick = { onSeeAll(section) })
                }
            }
        }
    }
}

private const val ROW_LIMIT = 12

@Composable
private fun SectionHeader(title: StringResource, hint: StringResource) {
    TextSectionHeader(title = stringResource(title), hint = stringResource(hint))
}

@Composable
private fun TextSectionHeader(title: String, hint: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 8.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = hint,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun StatusLine(updatedAtMillis: Long?, note: String?, onRefresh: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = updatedAtMillis?.let { updatedLabel(it) }.orEmpty(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (note != null) {
                Text(
                    text = note,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        TextButton(onClick = onRefresh) {
            Icon(
                imageVector = Phosphor.Bold.ArrowsClockwise,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.size(8.dp))
            Text(text = stringResource(MR.strings.discover_refresh))
        }
    }
}

/** Small, quiet note: the cached rows below are still usable. */
@Composable
private fun OfflineBanner() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = Phosphor.Bold.CloudSlash,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = stringResource(MR.strings.discover_offline_cached),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun LazyListScope.loadingSkeleton(wide: Boolean) {
    // Same shape as the loaded page: a Rising row of wide cards, then regular rows (paired when wide)
    item(key = "skeleton-rising") {
        Column(Modifier.padding(top = 20.dp)) {
            SkeletonHeader()
            // Fixed-width cards cut at the edge, like the real carousel, instead of squeezed
            Box(Modifier.fillMaxWidth().clipToBounds()) {
                Row(
                    modifier = Modifier
                        .wrapContentWidth(Alignment.Start, unbounded = true)
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    repeat(4) { DiscoverRisingCardSkeleton() }
                }
            }
        }
    }
    repeat(2) { index ->
        item(key = "skeleton-$index") {
            Row(Modifier.fillMaxWidth().padding(top = 20.dp)) {
                repeat(if (wide) 2 else 1) {
                    Column(Modifier.weight(1f)) {
                        SkeletonHeader()
                        Box(Modifier.fillMaxWidth().clipToBounds()) {
                            Row(
                                modifier = Modifier
                                    .wrapContentWidth(Alignment.Start, unbounded = true)
                                    .padding(horizontal = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                repeat(8) { DiscoverCardSkeleton() }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SkeletonHeader() {
    SkeletonLine(width = 120.dp, modifier = Modifier.padding(horizontal = 16.dp))
    Spacer(Modifier.height(8.dp))
    SkeletonLine(width = 200.dp, modifier = Modifier.padding(horizontal = 16.dp))
    Spacer(Modifier.height(12.dp))
}

@Composable
private fun EmptyOrOffline(offline: Boolean, onRefresh: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = if (offline) Phosphor.Bold.CloudSlash else Phosphor.Bold.Compass,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(48.dp),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(if (offline) MR.strings.discover_offline_title else MR.strings.discover_empty_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(if (offline) MR.strings.discover_offline_hint else MR.strings.discover_empty_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        TextButton(onClick = onRefresh) {
            Icon(
                imageVector = Phosphor.Bold.ArrowsClockwise,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.size(8.dp))
            Text(text = stringResource(MR.strings.discover_refresh))
        }
    }
}

@Composable
private fun updatedLabel(updatedAtMillis: Long): String {
    val minutes = ((System.currentTimeMillis() - updatedAtMillis) / 60_000L).coerceAtLeast(0L)
    return when {
        minutes < 1 -> stringResource(MR.strings.discover_updated_just_now)
        minutes < 60 -> stringResource(MR.strings.discover_updated_minutes, minutes.toInt())
        minutes < 24 * 60 -> stringResource(MR.strings.discover_updated_hours, (minutes / 60).toInt())
        else -> {
            val date = Instant.ofEpochMilli(updatedAtMillis).atZone(ZoneId.systemDefault()).toLocalDate()
            stringResource(
                MR.strings.discover_updated_date,
                DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).format(date),
            )
        }
    }
}

internal val DiscoverSection.titleRes: StringResource
    get() = when (this) {
        DiscoverSection.RISING -> MR.strings.discover_section_rising
        DiscoverSection.NEW_IN_TOP -> MR.strings.discover_section_new_in_top
        DiscoverSection.COMPLETED -> MR.strings.discover_section_completed
        DiscoverSection.CLASSICS -> MR.strings.discover_section_classics
        DiscoverSection.HIDDEN_GEMS -> MR.strings.discover_section_hidden_gems
        DiscoverSection.OLDER_ONGOING -> MR.strings.discover_section_older_ongoing
    }

internal val DiscoverSection.hintRes: StringResource
    get() = when (this) {
        DiscoverSection.RISING -> MR.strings.discover_section_rising_hint
        DiscoverSection.NEW_IN_TOP -> MR.strings.discover_section_new_in_top_hint
        DiscoverSection.COMPLETED -> MR.strings.discover_section_completed_hint
        DiscoverSection.CLASSICS -> MR.strings.discover_section_classics_hint
        DiscoverSection.HIDDEN_GEMS -> MR.strings.discover_section_hidden_gems_hint
        DiscoverSection.OLDER_ONGOING -> MR.strings.discover_section_older_ongoing_hint
    }

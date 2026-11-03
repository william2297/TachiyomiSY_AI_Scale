package eu.kanade.presentation.browse.discover

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.icerock.moko.resources.StringResource
import eu.kanade.presentation.browse.discover.components.DiscoverCard
import eu.kanade.presentation.browse.discover.components.DiscoverCardSkeleton
import eu.kanade.presentation.browse.discover.components.DiscoverRisingCard
import eu.kanade.presentation.browse.discover.components.DiscoverRisingCardSkeleton
import eu.kanade.presentation.browse.discover.components.SkeletonLine
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
) {
    val visible = DiscoverSection.entries.mapNotNull { section ->
        state.sections[section]?.takeIf { it.isNotEmpty() }?.let { section to it }
    }
    val hasData = visible.isNotEmpty()

    PullRefresh(
        refreshing = state.loading && hasData,
        enabled = true,
        onRefresh = onRefresh,
        indicatorPadding = contentPadding,
    ) {
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
                        StatusLine(updatedAtMillis = state.updatedAtMillis, onRefresh = onRefresh)
                    }
                    if (state.error != null) {
                        item(key = "offline") { OfflineBanner() }
                    }
                    visible.forEach { (section, entries) -> section(section, entries, onOpen) }
                }
                state.loading -> loadingSkeleton()
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
}

private fun LazyListScope.section(
    section: DiscoverSection,
    entries: List<DiscoverEntry>,
    onOpen: (DiscoverEntry) -> Unit,
) {
    item(key = "header-${section.name}") {
        SectionHeader(title = section.titleRes, hint = section.hintRes)
    }
    item(key = "row-${section.name}") {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(if (section == DiscoverSection.RISING) 12.dp else 4.dp),
        ) {
            items(items = entries, key = { it.id }) { entry ->
                if (section == DiscoverSection.RISING) {
                    DiscoverRisingCard(entry = entry, onClick = { onOpen(entry) })
                } else {
                    DiscoverCard(entry = entry, onClick = { onOpen(entry) })
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: StringResource, hint: StringResource) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 8.dp),
    ) {
        Text(
            text = stringResource(title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = stringResource(hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun StatusLine(updatedAtMillis: Long?, onRefresh: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = updatedAtMillis?.let { updatedLabel(it) }.orEmpty(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
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

private fun LazyListScope.loadingSkeleton() {
    // Rising row first (wide cards), then two regular rows: the shape of the page before data arrives
    repeat(3) { index ->
        item(key = "skeleton-$index") {
            Column(Modifier.padding(top = 20.dp)) {
                SkeletonLine(width = 120.dp, modifier = Modifier.padding(horizontal = 16.dp))
                Spacer(Modifier.height(8.dp))
                SkeletonLine(width = 200.dp, modifier = Modifier.padding(horizontal = 16.dp))
                Spacer(Modifier.height(12.dp))
                if (index == 0) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        repeat(3) { DiscoverRisingCardSkeleton() }
                    }
                } else {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        repeat(8) { DiscoverCardSkeleton() }
                    }
                }
            }
        }
    }
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

private val DiscoverSection.titleRes: StringResource
    get() = when (this) {
        DiscoverSection.RISING -> MR.strings.discover_section_rising
        DiscoverSection.NEW_IN_TOP -> MR.strings.discover_section_new_in_top
        DiscoverSection.COMPLETED -> MR.strings.discover_section_completed
        DiscoverSection.CLASSICS -> MR.strings.discover_section_classics
        DiscoverSection.HIDDEN_GEMS -> MR.strings.discover_section_hidden_gems
        DiscoverSection.OLDER_ONGOING -> MR.strings.discover_section_older_ongoing
    }

private val DiscoverSection.hintRes: StringResource
    get() = when (this) {
        DiscoverSection.RISING -> MR.strings.discover_section_rising_hint
        DiscoverSection.NEW_IN_TOP -> MR.strings.discover_section_new_in_top_hint
        DiscoverSection.COMPLETED -> MR.strings.discover_section_completed_hint
        DiscoverSection.CLASSICS -> MR.strings.discover_section_classics_hint
        DiscoverSection.HIDDEN_GEMS -> MR.strings.discover_section_hidden_gems_hint
        DiscoverSection.OLDER_ONGOING -> MR.strings.discover_section_older_ongoing_hint
    }

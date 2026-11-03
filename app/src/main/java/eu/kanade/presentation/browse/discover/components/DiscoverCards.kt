package eu.kanade.presentation.browse.discover.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.browse.discover.DiscoverEntry
import eu.kanade.presentation.manga.components.MangaCover
import mihon.icons.phosphor.Phosphor
import mihon.icons.phosphor.boldautomirrored.ArrowLeft
import mihon.icons.phosphor.fill.Star
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

/** Width of a regular card; six fit across a portrait 8.8" tablet next to the rail. */
internal val DiscoverCardWidth = 116.dp

/** Width of the larger Rising card (cover beside the text). */
internal val DiscoverRisingCardWidth = 320.dp

private val RisingCoverWidth = 92.dp

/** Set by the screens that can hide a title; cards show "Not interested" on a long press when it is set. */
internal val LocalDiscoverHide = staticCompositionLocalOf<((DiscoverEntry) -> Unit)?> { null }

@Composable
private fun NotInterestedMenu(entry: DiscoverEntry, expanded: Boolean, onDismiss: () -> Unit) {
    val hide = LocalDiscoverHide.current
    DropdownMenu(expanded = expanded && hide != null, onDismissRequest = onDismiss) {
        DropdownMenuItem(
            text = { Text(stringResource(MR.strings.discover_not_interested)) },
            onClick = {
                onDismiss()
                hide?.invoke(entry)
            },
        )
    }
}

/**
 * Regular card: 2:3 cover, title (two lines reserved so a row stays aligned), status chip and score,
 * source name and a short reason (up to two lines: the reason is the point of the card).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun DiscoverCard(
    entry: DiscoverEntry,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    // Fixed width in the carousels; null lets a grid cell decide the width
    width: Dp? = DiscoverCardWidth,
) {
    var menuOpen by remember { mutableStateOf(false) }
    Column(
        modifier = modifier
            .then(if (width != null) Modifier.width(width) else Modifier.fillMaxWidth())
            .clip(MaterialTheme.shapes.small)
            .combinedClickable(onClick = onClick, onLongClick = { menuOpen = true })
            .padding(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        MangaCover.Book(
            data = entry.coverUrl,
            modifier = Modifier.fillMaxWidth(),
            contentDescription = entry.title,
        )
        Text(
            text = entry.title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            minLines = 2,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 2.dp),
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            StatusChip(completed = entry.completed)
            Spacer(Modifier.weight(1f))
            entry.score?.let { ScoreLabel(it) }
        }
        SourceAndReason(entry)
        NotInterestedMenu(entry, menuOpen) { menuOpen = false }
    }
}

/**
 * Rising gets a wider card with the reason on top: in this row the "why now" is the point.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun DiscoverRisingCard(
    entry: DiscoverEntry,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var menuOpen by remember { mutableStateOf(false) }
    Surface(
        modifier = modifier
            .width(DiscoverRisingCardWidth)
            .clip(MaterialTheme.shapes.medium)
            .combinedClickable(onClick = onClick, onLongClick = { menuOpen = true }),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            MangaCover.Book(
                data = entry.coverUrl,
                modifier = Modifier.width(RisingCoverWidth),
                contentDescription = entry.title,
            )
            Column(
                modifier = Modifier.padding(vertical = 2.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = entry.reason,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = entry.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    StatusChip(completed = entry.completed)
                    entry.score?.let { ScoreLabel(it) }
                }
                Text(
                    text = entry.sourceName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            NotInterestedMenu(entry, menuOpen) { menuOpen = false }
        }
    }
}

@Composable
private fun SourceAndReason(entry: DiscoverEntry) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = entry.sourceName,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        if (entry.isNew) {
            Text(
                text = stringResource(MR.strings.discover_new_today),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
            )
        }
    }
    Text(
        text = entry.reason,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
    )
}

/** Completed is tinted (the thing a binge reader looks for); Ongoing stays neutral. Text carries the meaning. */
@Composable
internal fun StatusChip(completed: Boolean) {
    val colors = MaterialTheme.colorScheme
    Text(
        text = stringResource(if (completed) MR.strings.completed else MR.strings.ongoing),
        style = MaterialTheme.typography.labelSmall,
        color = if (completed) colors.onTertiaryContainer else colors.onSurfaceVariant,
        maxLines = 1,
        modifier = Modifier
            .clip(CircleShape)
            .background(if (completed) colors.tertiaryContainer else colors.surfaceContainerHighest)
            .padding(horizontal = 8.dp, vertical = 2.dp),
    )
}

/** AniList mean score (0 to 100) as a small star and number. */
@Composable
internal fun ScoreLabel(score: Int) {
    val description = stringResource(MR.strings.discover_score, score)
    Row(
        modifier = Modifier.clearAndSetSemantics { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Icon(
            imageVector = Phosphor.Fill.Star,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(12.dp),
        )
        Text(
            text = "$score",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/** Static placeholder (no shimmer: nothing should pulse in a dark room). */
@Composable
internal fun DiscoverCardSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .width(DiscoverCardWidth)
            .padding(4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(MangaCover.Book.ratio)
                .clip(MaterialTheme.shapes.extraSmall)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        )
        SkeletonLine(width = DiscoverCardWidth - 24.dp)
        SkeletonLine(width = 64.dp)
    }
}

/** Placeholder with the outline of a Rising card. */
@Composable
internal fun DiscoverRisingCardSkeleton(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .width(DiscoverRisingCardWidth)
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier
                .width(RisingCoverWidth)
                .aspectRatio(MangaCover.Book.ratio)
                .clip(MaterialTheme.shapes.extraSmall)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        )
        Column(Modifier.padding(top = 4.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SkeletonLine(width = 140.dp)
            SkeletonLine(width = 180.dp)
            SkeletonLine(width = 96.dp)
        }
    }
}

@Composable
internal fun SkeletonLine(width: Dp, modifier: Modifier = Modifier) {
    Box(
        modifier
            .width(width)
            .height(10.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
    )
}

/**
 * Last item of a row that has more entries than it shows. Same footprint as a regular card's cover so the
 * row keeps its rhythm; a tonal tile with an arrow reads as something to tap, the count says how much is behind it.
 */
@Composable
internal fun DiscoverSeeAllCard(
    count: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .width(DiscoverCardWidth)
            .padding(4.dp),
    ) {
        SeeAllTile(
            count = count,
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(MangaCover.Book.ratio),
            shape = MaterialTheme.shapes.extraSmall,
        )
    }
}

/** Rising variant: as tall as a Rising card, narrower. */
@Composable
internal fun DiscoverRisingSeeAllCard(
    count: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SeeAllTile(
        count = count,
        onClick = onClick,
        modifier = modifier
            .width(DiscoverRisingSeeAllWidth)
            .height(RisingCoverWidth / MangaCover.Book.ratio + 20.dp),
        shape = MaterialTheme.shapes.medium,
    )
}

internal val DiscoverRisingSeeAllWidth = 148.dp

@Composable
private fun SeeAllTile(
    count: Int,
    onClick: () -> Unit,
    shape: androidx.compose.ui.graphics.Shape,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val description = stringResource(MR.strings.discover_see_all_description, count)
    Surface(
        onClick = onClick,
        modifier = modifier.semantics(mergeDescendants = true) { contentDescription = description },
        shape = shape,
        color = colors.surfaceContainer,
        border = BorderStroke(1.dp, colors.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(colors.secondaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Phosphor.BoldAutoMirrored.ArrowLeft,
                    contentDescription = null,
                    tint = colors.onSecondaryContainer,
                    // Flipped back-arrow = forward arrow; the auto-mirroring keeps it right for RTL too
                    modifier = Modifier
                        .size(22.dp)
                        .graphicsLayer(scaleX = -1f),
                )
            }
            Text(
                text = stringResource(MR.strings.discover_see_all),
                style = MaterialTheme.typography.titleSmall,
                color = colors.onSurface,
                modifier = Modifier.padding(top = 12.dp),
            )
            Text(
                text = stringResource(MR.strings.discover_see_all_count, count),
                style = MaterialTheme.typography.labelMedium,
                color = colors.onSurfaceVariant,
            )
        }
    }
}

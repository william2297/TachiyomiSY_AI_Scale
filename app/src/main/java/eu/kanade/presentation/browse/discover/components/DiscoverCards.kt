package eu.kanade.presentation.browse.discover.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.browse.discover.DiscoverEntry
import eu.kanade.presentation.manga.components.MangaCover
import mihon.icons.phosphor.Phosphor
import mihon.icons.phosphor.fill.Star
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

/** Width of a regular card; six fit across a portrait 8.8" tablet next to the rail. */
internal val DiscoverCardWidth = 116.dp

/** Width of the larger Rising card (cover beside the text). */
internal val DiscoverRisingCardWidth = 320.dp

private val RisingCoverWidth = 92.dp

/**
 * Regular card: 2:3 cover, title (two lines reserved so a row stays aligned), status chip and score,
 * source name and a short reason (up to two lines: the reason is the point of the card).
 */
@Composable
internal fun DiscoverCard(
    entry: DiscoverEntry,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .width(DiscoverCardWidth)
            .clip(MaterialTheme.shapes.small)
            .clickable(onClick = onClick)
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
    }
}

/**
 * Rising gets a wider card with the reason on top: in this row the "why now" is the point.
 */
@Composable
internal fun DiscoverRisingCard(
    entry: DiscoverEntry,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.width(DiscoverRisingCardWidth),
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
        }
    }
}

@Composable
private fun SourceAndReason(entry: DiscoverEntry) {
    Text(
        text = entry.sourceName,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
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

package eu.kanade.presentation.browse.discover.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import mihon.icons.phosphor.Phosphor
import mihon.icons.phosphor.bold.Brain
import mihon.icons.phosphor.bold.Check
import mihon.icons.phosphor.bold.Coffee
import mihon.icons.phosphor.bold.Compass
import mihon.icons.phosphor.bold.Fire
import mihon.icons.phosphor.bold.Ghost
import mihon.icons.phosphor.bold.Heart
import mihon.icons.phosphor.bold.Knife
import mihon.icons.phosphor.bold.MagicWand
import mihon.icons.phosphor.bold.MagnifyingGlass
import mihon.icons.phosphor.bold.MaskSad
import mihon.icons.phosphor.bold.RocketLaunch
import mihon.icons.phosphor.bold.Skull
import mihon.icons.phosphor.bold.SlidersHorizontal
import mihon.icons.phosphor.bold.Smiley
import mihon.icons.phosphor.bold.SoccerBall
import mihon.icons.phosphor.bold.Sparkle
import mihon.icons.phosphor.bold.Sword
import mihon.icons.phosphor.bold.Tag
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

/** AniList calls its adult genre "Hentai"; the screen says what it is. */
internal fun genreLabel(genre: String): String = if (genre == "Hentai") "Adult (18+)" else genre

/** One glyph per AniList genre, so a row of picked genres can be read at a glance, not only by its words. */
internal fun genreIcon(genre: String): ImageVector = when (genre) {
    "Action" -> Phosphor.Bold.Sword
    "Adventure" -> Phosphor.Bold.Compass
    "Comedy" -> Phosphor.Bold.Smiley
    "Drama" -> Phosphor.Bold.MaskSad
    "Ecchi" -> Phosphor.Bold.Fire
    "Fantasy" -> Phosphor.Bold.MagicWand
    "Horror" -> Phosphor.Bold.Skull
    "Mahou Shoujo" -> Phosphor.Bold.Sparkle
    "Mystery" -> Phosphor.Bold.MagnifyingGlass
    "Psychological" -> Phosphor.Bold.Brain
    "Romance" -> Phosphor.Bold.Heart
    "Sci-Fi" -> Phosphor.Bold.RocketLaunch
    "Slice of Life" -> Phosphor.Bold.Coffee
    "Sports" -> Phosphor.Bold.SoccerBall
    "Supernatural" -> Phosphor.Bold.Ghost
    "Thriller" -> Phosphor.Bold.Knife
    else -> Phosphor.Bold.Tag
}

/**
 * Bottom sheet for the "My genres" row. Bottom, not a centered dialog: on an 8.8" tablet held in two hands the
 * thumbs rest near the lower corners, where Clear (left) and Apply (right) sit.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DiscoverGenreSheet(
    genres: List<String>,
    picked: Set<String>,
    onApply: (Set<String>) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        sheetMaxWidth = GENRE_SHEET_MAX_WIDTH,
    ) {
        DiscoverGenrePickerContent(
            genres = genres,
            initial = picked,
            onApply = onApply,
            modifier = Modifier.navigationBarsPadding(),
        )
    }
}

private val GENRE_SHEET_MAX_WIDTH = 720.dp

/** The sheet's content, separate so it can be shown and checked without a window. */
@Composable
internal fun DiscoverGenrePickerContent(
    genres: List<String>,
    initial: Set<String>,
    onApply: (Set<String>) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selected by rememberSaveable(initial) { mutableStateOf(initial.toList()) }
    val selection = selected.toSet()
    Column(modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState())
                .padding(start = 24.dp, end = 24.dp, bottom = 20.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(MR.strings.discover_my_genres),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                PickedCount(count = selection.size)
            }
            Spacer(Modifier.size(4.dp))
            Text(
                text = stringResource(MR.strings.discover_genres_sheet_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.size(20.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                genres.forEach { genre ->
                    GenreChip(
                        genre = genre,
                        selected = genre in selection,
                        onClick = {
                            selected = if (genre in selection) selected - genre else selected + genre
                        },
                    )
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TextButton(
                onClick = { selected = emptyList() },
                enabled = selection.isNotEmpty(),
                modifier = Modifier.heightIn(min = 48.dp),
            ) {
                Text(text = stringResource(MR.strings.discover_genres_clear))
            }
            Text(
                text = if (selection.isEmpty() && initial.isNotEmpty()) {
                    stringResource(MR.strings.discover_genres_none_hides_row)
                } else {
                    ""
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Button(
                onClick = { onApply(selection) },
                contentPadding = PaddingValues(horizontal = 28.dp),
                modifier = Modifier.heightIn(min = 48.dp).widthIn(min = 120.dp),
            ) {
                Text(text = stringResource(MR.strings.discover_genres_apply))
            }
        }
    }
}

/** "3 picked": the number slides when it changes, so a tap on a chip visibly lands somewhere. */
@Composable
private fun PickedCount(count: Int) {
    val active = count > 0
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = if (active) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        contentColor = if (active) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
    ) {
        AnimatedContent(
            targetState = count,
            transitionSpec = {
                val up = targetState > initialState
                (slideInVertically(tween(200)) { if (up) it else -it } + fadeIn(tween(200))) togetherWith
                    (slideOutVertically(tween(200)) { if (up) -it else it } + fadeOut(tween(120)))
            },
            label = "picked-count",
        ) { value ->
            Text(
                text = if (value == 0) {
                    stringResource(MR.strings.discover_genres_none_picked)
                } else {
                    stringResource(MR.strings.discover_genres_picked, value)
                },
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
    }
}

/** 48 dp tall so it is an easy thumb target; picked shows a filled chip, a heavier label and a check. */
@Composable
private fun GenreChip(genre: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                text = genreLabel(genre),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            )
        },
        leadingIcon = {
            Icon(imageVector = genreIcon(genre), contentDescription = null, modifier = Modifier.size(18.dp))
        },
        trailingIcon = if (selected) {
            { Icon(imageVector = Phosphor.Bold.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
        } else {
            null
        },
        shape = MaterialTheme.shapes.extraLarge,
        colors = FilterChipDefaults.filterChipColors(
            iconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
            selectedTrailingIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = MaterialTheme.colorScheme.outlineVariant,
            selectedBorderColor = MaterialTheme.colorScheme.primary,
            selectedBorderWidth = 1.dp,
        ),
        modifier = Modifier.heightIn(min = 48.dp),
    )
}

/**
 * Shown where the "My genres" row goes while no genre is picked. The tags on the right are a preview of what the
 * sheet offers, and the whole card opens it.
 */
@Composable
internal fun MyGenresInvite(genres: List<String>, onChoose: () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        MyGenresInviteCard(genres = genres, onChoose = onChoose, showTags = maxWidth >= INVITE_TAGS_MIN_WIDTH)
    }
}

private val INVITE_TAGS_MIN_WIDTH = 600.dp

@Composable
private fun MyGenresInviteCard(genres: List<String>, onChoose: () -> Unit, showTags: Boolean) {
    Surface(
        onClick = onChoose,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 24.dp),
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(MR.strings.discover_my_genres),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.size(4.dp))
                Text(
                    text = stringResource(MR.strings.discover_my_genres_invite),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.size(16.dp))
                FilledTonalButton(onClick = onChoose, modifier = Modifier.heightIn(min = 48.dp)) {
                    Icon(
                        imageVector = Phosphor.Bold.SlidersHorizontal,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.size(8.dp))
                    Text(text = stringResource(MR.strings.discover_choose_genres))
                }
            }
            if (showTags) {
                FlowRow(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.End),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    INVITE_SAMPLE.filter { it in genres }.forEach { GenreTag(genre = it, quiet = true) }
                }
            }
        }
    }
}

/** A mixed handful, so the preview does not read as the start of an alphabetical list. */
private val INVITE_SAMPLE = listOf(
    "Romance", "Action", "Mystery", "Slice of Life", "Fantasy", "Horror", "Sci-Fi", "Comedy", "Sports",
)

/**
 * Header of the "My genres" row: title, an edit button, and the picked genres as small tags (the first few, then
 * "+N"). Tapping the tags opens the sheet too. While the row is being rebuilt a small spinner says so.
 */
@Composable
internal fun MyGenresHeader(picked: List<String>, updating: Boolean, onEdit: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(MR.strings.discover_my_genres),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            if (updating) {
                CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(14.dp))
                Spacer(Modifier.size(8.dp))
                Text(
                    text = stringResource(MR.strings.discover_genres_updating),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.size(12.dp))
            }
            OutlinedButton(
                onClick = onEdit,
                contentPadding = PaddingValues(start = 12.dp, end = 16.dp),
                modifier = Modifier.heightIn(min = 40.dp),
            ) {
                Icon(
                    imageVector = Phosphor.Bold.SlidersHorizontal,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.size(8.dp))
                Text(text = stringResource(MR.strings.discover_edit_genres))
            }
        }
        Row(
            modifier = Modifier
                .padding(top = 6.dp)
                .clickable(role = Role.Button, onClick = onEdit),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            picked.take(HEADER_TAGS).forEach { GenreTag(genre = it, quiet = false) }
            val rest = picked.size - HEADER_TAGS
            if (rest > 0) {
                Surface(
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ) {
                    Text(
                        text = stringResource(MR.strings.discover_genres_more, rest),
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    )
                }
            }
            Spacer(Modifier.size(2.dp))
            Text(
                text = stringResource(MR.strings.discover_genres_any),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private const val HEADER_TAGS = 5

/** A small, non-interactive genre label with its glyph. */
@Composable
private fun GenreTag(genre: String, quiet: Boolean) {
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = if (quiet) {
            MaterialTheme.colorScheme.surfaceContainerHighest
        } else {
            MaterialTheme.colorScheme.secondaryContainer
        },
        contentColor = if (quiet) {
            MaterialTheme.colorScheme.onSurfaceVariant
        } else {
            MaterialTheme.colorScheme.onSecondaryContainer
        },
    ) {
        Row(
            modifier = Modifier.padding(start = 8.dp, end = 10.dp, top = 5.dp, bottom = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(imageVector = genreIcon(genre), contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(Modifier.size(5.dp))
            Text(text = genreLabel(genre), style = MaterialTheme.typography.labelMedium, maxLines = 1)
        }
    }
}

/** Under the header when the picked genres have no row yet: placeholders while it is built, a note otherwise. */
@Composable
internal fun MyGenresPending(loading: Boolean) {
    if (loading) {
        Box(Modifier.fillMaxWidth().clipToBounds()) {
            Row(
                modifier = Modifier
                    .wrapContentWidth(Alignment.Start, unbounded = true)
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                repeat(PENDING_SKELETONS) { DiscoverCardSkeleton() }
            }
        }
    } else {
        Text(
            text = stringResource(MR.strings.discover_my_genres_empty),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }
}

private const val PENDING_SKELETONS = 8

/** Remembers whether the genre sheet is open across rotation. */
@Composable
internal fun rememberGenreSheetOpen() = rememberSaveable { mutableStateOf(false) }

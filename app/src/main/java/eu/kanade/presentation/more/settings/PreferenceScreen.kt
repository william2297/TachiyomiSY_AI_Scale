package eu.kanade.presentation.more.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import eu.kanade.presentation.more.settings.screen.SearchableSettings
import eu.kanade.presentation.more.settings.widget.PreferenceGroupHeader
import eu.kanade.presentation.more.settings.widget.PrefsGroupHorizontalInset
import kotlinx.coroutines.delay
import tachiyomi.presentation.core.components.ScrollbarLazyColumn
import kotlin.time.Duration.Companion.seconds

/**
 * Preference Screen composable which contains a list of [Preference] items
 * @param items [Preference] items which should be displayed on the preference screen. An item can be a single [PreferenceItem] or a group ([Preference.PreferenceGroup])
 * @param modifier [Modifier] to be applied to the preferenceScreen layout
 */
@Composable
fun PreferenceScreen(
    items: List<Preference>,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    val state = rememberLazyListState()
    val highlightKey = SearchableSettings.highlightKey
    if (highlightKey != null) {
        LaunchedEffect(Unit) {
            val i = items.findHighlightedIndex(highlightKey)
            if (i >= 0) {
                delay(0.5.seconds)
                state.animateScrollToItem(i)
            }
            SearchableSettings.highlightKey = null
        }
    }

    ScrollbarLazyColumn(
        modifier = modifier,
        state = state,
        contentPadding = contentPadding,
    ) {
        items.fastForEachIndexed { i, preference ->
            when (preference) {
                // Create Preference Group
                is Preference.PreferenceGroup -> {
                    if (!preference.visible) return@fastForEachIndexed

                    item {
                        Column {
                            PreferenceGroupHeader(
                                title = preference.title,
                                highlighted = preference.title == highlightKey,
                            )
                        }
                    }
                    val visibleItems = preference.preferenceItems.filter { it.visible }
                    items(preference.preferenceItems) { item ->
                        PreferenceItem(
                            item = item,
                            highlightKey = highlightKey,
                            modifier = Modifier.groupedItem(
                                position = visibleItems.indexOf(item),
                                count = visibleItems.size,
                            ),
                        )
                    }
                    item {
                        if (i < items.lastIndex) {
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                }

                // Create Preference Item
                is Preference.PreferenceItem<*, *> -> item {
                    PreferenceItem(
                        item = preference,
                        highlightKey = highlightKey,
                    )
                }
            }
        }
    }
}

private val GroupOuterCorner = 20.dp
private val GroupInnerCorner = 4.dp
private val GroupItemGap = 2.dp

/**
 * Items of one group form a single rounded card: large outer corners, small inner corners and a
 * hairline gap between rows (the grouped-list pattern of current Android settings).
 * Hidden items get no decoration so the visible ones keep correct first/last corners.
 */
@Composable
private fun Modifier.groupedItem(position: Int, count: Int): Modifier {
    if (position < 0) return this
    val top = if (position == 0) GroupOuterCorner else GroupInnerCorner
    val bottom = if (position == count - 1) GroupOuterCorner else GroupInnerCorner
    return this
        .padding(horizontal = PrefsGroupHorizontalInset)
        .padding(bottom = if (position == count - 1) 0.dp else GroupItemGap)
        .clip(RoundedCornerShape(topStart = top, topEnd = top, bottomStart = bottom, bottomEnd = bottom))
        .background(MaterialTheme.colorScheme.surfaceContainer)
}

private fun List<Preference>.findHighlightedIndex(highlightKey: String): Int {
    return flatMap {
        if (it is Preference.PreferenceGroup) {
            buildList<String?> {
                add(it.title) // Header
                addAll(it.preferenceItems.map { groupItem -> groupItem.title })
                add(null) // Spacer
            }
        } else {
            listOf(it.title)
        }
    }.indexOfFirst { it == highlightKey }
}

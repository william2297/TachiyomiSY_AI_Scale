package eu.kanade.presentation.browse.discover

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.browse.discover.components.DiscoverCard
import eu.kanade.presentation.components.AppBar
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.plus

/**
 * Full list of one Discover row: same cards as the row, in a grid whose column count follows the width
 * (about three across a portrait 8.8" tablet, five in landscape).
 */
@Composable
fun DiscoverSectionContent(
    section: DiscoverSection,
    entries: List<DiscoverEntry>,
    onBack: () -> Unit,
    onOpen: (DiscoverEntry) -> Unit,
) {
    Scaffold(
        topBar = { scrollBehavior ->
            AppBar(
                title = stringResource(section.titleRes),
                subtitle = stringResource(section.hintRes),
                navigateUp = onBack,
                scrollBehavior = scrollBehavior,
            )
        },
    ) { contentPadding ->
        if (entries.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding)
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = stringResource(MR.strings.discover_section_empty),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(12.dp))
                TextButton(onClick = onBack) { Text(stringResource(MR.strings.discover_back)) }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = GridCellMinWidth),
                contentPadding = contentPadding + PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(items = entries, key = { it.id }) { entry ->
                    DiscoverCard(entry = entry, onClick = { onOpen(entry) }, width = null)
                }
            }
        }
    }
}

/** 781 dp wide portrait gives three columns, 1172 dp landscape gives five. */
private val GridCellMinWidth = 200.dp

package eu.kanade.tachiyomi.ui.browse.discover

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import dev.zacsweers.metrox.viewmodel.metroViewModel
import eu.kanade.presentation.browse.discover.DiscoverScreen
import eu.kanade.presentation.components.TabContent
import tachiyomi.i18n.MR

@Composable
fun Screen.discoverTab(): TabContent {
    val viewModel = metroViewModel<DiscoverViewModel>()
    val state by viewModel.state.collectAsStateWithLifecycle()

    return TabContent(
        // No toolbar action: refresh lives next to the "Updated" line and on pull-down, one control is enough
        titleRes = MR.strings.label_discover,
        content = { contentPadding, _ ->
            DiscoverScreen(
                state = state,
                contentPadding = contentPadding,
                onOpen = viewModel::open,
                onRefresh = viewModel::refresh,
            )
        },
    )
}

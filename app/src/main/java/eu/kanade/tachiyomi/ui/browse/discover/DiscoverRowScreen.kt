package eu.kanade.tachiyomi.ui.browse.discover

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.zacsweers.metrox.viewmodel.metroViewModel
import eu.kanade.presentation.browse.discover.DiscoverListContent
import eu.kanade.presentation.browse.discover.components.LocalDiscoverHide
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.ui.manga.MangaScreen
import kotlinx.coroutines.launch

/**
 * "See all" for a row built from the user's taste, such as "My genres". Only the row key travels as an argument; the
 * entries come from the Discover view model, so the list is never serialised into the back stack.
 */
class DiscoverRowScreen(private val rowKey: String) : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = metroViewModel<DiscoverViewModel>()
        val state by viewModel.state.collectAsStateWithLifecycle()
        val scope = rememberCoroutineScope()
        val row = state.extraRows.firstOrNull { it.key == rowKey }

        CompositionLocalProvider(LocalDiscoverHide provides viewModel::hide) {
            DiscoverListContent(
                title = row?.title.orEmpty(),
                hint = row?.hint.orEmpty(),
                entries = row?.entries.orEmpty(),
                onBack = navigator::pop,
                onOpen = { entry ->
                    scope.launch { navigator.push(MangaScreen(viewModel.localMangaId(entry), true)) }
                },
            )
        }
    }
}

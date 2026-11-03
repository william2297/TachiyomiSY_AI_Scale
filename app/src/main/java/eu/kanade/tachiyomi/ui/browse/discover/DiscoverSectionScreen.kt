package eu.kanade.tachiyomi.ui.browse.discover

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.zacsweers.metrox.viewmodel.metroViewModel
import eu.kanade.presentation.browse.discover.DiscoverSection
import eu.kanade.presentation.browse.discover.DiscoverSectionContent
import eu.kanade.presentation.browse.discover.components.LocalDiscoverHide
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.ui.manga.MangaScreen
import kotlinx.coroutines.launch

/**
 * "See all" for one Discover row. Only the section travels as an argument; the entries come from the
 * Discover view model, so the list is never serialised into the back stack.
 */
class DiscoverSectionScreen(private val section: DiscoverSection) : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = metroViewModel<DiscoverViewModel>()
        val state by viewModel.state.collectAsStateWithLifecycle()
        val scope = rememberCoroutineScope()

        CompositionLocalProvider(LocalDiscoverHide provides viewModel::hide) {
            DiscoverSectionContent(
                section = section,
                entries = state.sections[section].orEmpty(),
                onBack = navigator::pop,
                onOpen = { entry ->
                    scope.launch { navigator.push(MangaScreen(viewModel.localMangaId(entry), true)) }
                },
            )
        }
    }
}

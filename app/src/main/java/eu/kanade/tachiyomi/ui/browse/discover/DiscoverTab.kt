package eu.kanade.tachiyomi.ui.browse.discover

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.zacsweers.metrox.viewmodel.metroViewModel
import eu.kanade.presentation.browse.discover.DiscoverScreen
import eu.kanade.presentation.browse.discover.components.LocalDiscoverHide
import eu.kanade.presentation.components.TabContent
import eu.kanade.tachiyomi.data.discover.AniListDiscoverApi
import eu.kanade.tachiyomi.ui.manga.MangaScreen
import kotlinx.coroutines.launch
import tachiyomi.i18n.MR

@Composable
fun Screen.discoverTab(): TabContent {
    val viewModel = metroViewModel<DiscoverViewModel>()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val seedTitles by viewModel.seedTitles.collectAsStateWithLifecycle()
    val navigator = LocalNavigator.currentOrThrow
    val scope = rememberCoroutineScope()

    return TabContent(
        // No toolbar action: refresh lives next to the "Updated" line and on pull-down, one control is enough
        titleRes = MR.strings.label_discover,
        content = { contentPadding, _ ->
            CompositionLocalProvider(LocalDiscoverHide provides viewModel::hide) {
                DiscoverScreen(
                    state = state,
                    contentPadding = contentPadding,
                    onOpen = { entry ->
                        scope.launch { navigator.push(MangaScreen(viewModel.localMangaId(entry), true)) }
                    },
                    onRefresh = viewModel::refresh,
                    onSeeAll = { section -> navigator.push(DiscoverSectionScreen(section)) },
                    onSeeAllRow = { row -> navigator.push(DiscoverRowScreen(row.key)) },
                    genres = GENRES,
                    onPickGenres = viewModel::setGenres,
                    seedTitles = seedTitles,
                    onPickSeed = viewModel::setSeed,
                )
            }
        },
    )
}

/** AniList's genres in the order the picker shows them. */
private val GENRES = AniListDiscoverApi.PICKABLE_GENRES.sorted()

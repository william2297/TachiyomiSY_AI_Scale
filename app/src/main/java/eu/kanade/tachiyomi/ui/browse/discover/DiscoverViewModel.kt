package eu.kanade.tachiyomi.ui.browse.discover

import androidx.lifecycle.ViewModel
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import eu.kanade.presentation.browse.discover.DiscoverEntry
import eu.kanade.presentation.browse.discover.DiscoverSampleData
import eu.kanade.presentation.browse.discover.DiscoverState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Placeholder: serves sample picks so the tab opens. The AniList fetch, source matching and cache
 * belong here later; the screen only reads [state] and calls [refresh] and [open].
 */
@Inject
@ViewModelKey
@ContributesIntoMap(AppScope::class, binding = binding<ViewModel>())
class DiscoverViewModel : ViewModel() {

    private val mutableState = MutableStateFlow(
        DiscoverSampleData.state.copy(updatedAtMillis = System.currentTimeMillis()),
    )
    val state: StateFlow<DiscoverState> = mutableState.asStateFlow()

    fun refresh() {
        mutableState.update { it.copy(updatedAtMillis = System.currentTimeMillis(), error = null) }
    }

    /** Opening the matched title in its source is wired with the real data. */
    @Suppress("UNUSED_PARAMETER")
    fun open(entry: DiscoverEntry) = Unit
}

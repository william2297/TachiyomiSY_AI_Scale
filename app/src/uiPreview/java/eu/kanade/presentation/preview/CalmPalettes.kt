package eu.kanade.presentation.preview

import androidx.compose.material3.ColorScheme
import eu.kanade.presentation.theme.colorscheme.BaseColorScheme
import eu.kanade.presentation.theme.colorscheme.GraphiteColorScheme
import eu.kanade.presentation.theme.colorscheme.MutedJadeColorScheme
import eu.kanade.presentation.theme.colorscheme.WarmCharcoalColorScheme

/** The three calm dark themes as shipped in the app, in the order they were compared (A, B, C). */
internal class CalmPalette(val id: String, val title: String, val scheme: ColorScheme)

private fun BaseColorScheme.dark() = getColorScheme(
    isDark = true,
    isAmoled = false,
    overrideDarkSurfaceContainers = false,
)

internal val CalmPalettes = listOf(
    CalmPalette(id = "A", title = "Muted Jade", scheme = MutedJadeColorScheme.dark()),
    CalmPalette(id = "B", title = "Warm Charcoal (default)", scheme = WarmCharcoalColorScheme.dark()),
    CalmPalette(id = "C", title = "Graphite", scheme = GraphiteColorScheme.dark()),
)

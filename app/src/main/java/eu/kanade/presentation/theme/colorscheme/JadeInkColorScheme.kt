package eu.kanade.presentation.theme.colorscheme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Colors for Jade Ink theme
 * Built for AMOLED tablets: true black canvas so the comic panels are the only lit pixels,
 * a cool-neutral tonal ladder for chrome, jade for actions/unread and lilac for downloaded.
 * See docs/desain-tema-modern.md for the full token table and contrast notes.
 *
 * Key colors:
 * Primary #5FE3B8
 * Secondary #5FE3B8
 * Tertiary #C8B6FF
 * Neutral #121614
 */
internal object JadeInkColorScheme : BaseColorScheme() {

    override val darkScheme = darkColorScheme(
        primary = Color(0xFF5FE3B8),
        onPrimary = Color(0xFF00382A),
        primaryContainer = Color(0xFF00513D),
        onPrimaryContainer = Color(0xFF7FFBD2),
        inversePrimary = Color(0xFF006C52),
        secondary = Color(0xFF5FE3B8), // Unread badge
        onSecondary = Color(0xFF00382A), // Unread badge text
        secondaryContainer = Color(0xFF13362C), // Navigation bar selector pill & progress indicator (remaining)
        onSecondaryContainer = Color(0xFFA6F5DA), // Navigation bar selector icon
        tertiary = Color(0xFFC8B6FF), // Downloaded badge
        onTertiary = Color(0xFF2F1D6B), // Downloaded badge text
        tertiaryContainer = Color(0xFF463A82),
        onTertiaryContainer = Color(0xFFE6DEFF),
        background = Color(0xFF000000),
        onBackground = Color(0xFFE8ECEA),
        surface = Color(0xFF000000),
        onSurface = Color(0xFFE8ECEA),
        surfaceVariant = Color(0xFF121614), // Navigation bar background (ThemePrefWidget)
        onSurfaceVariant = Color(0xFFA8B2AD),
        surfaceTint = Color(0xFF5FE3B8),
        inverseSurface = Color(0xFFE8ECEA),
        inverseOnSurface = Color(0xFF121614),
        error = Color(0xFFFFB4AB),
        onError = Color(0xFF690005),
        errorContainer = Color(0xFF93000A),
        onErrorContainer = Color(0xFFFFDAD6),
        outline = Color(0xFF7A8580),
        outlineVariant = Color(0xFF2A302D),
        scrim = Color(0xFF000000),
        surfaceBright = Color(0xFF2E3532),
        surfaceDim = Color(0xFF000000),
        surfaceContainerLowest = Color(0xFF000000),
        surfaceContainerLow = Color(0xFF0B0E0D),
        surfaceContainer = Color(0xFF121614), // Navigation bar background
        surfaceContainerHigh = Color(0xFF1A1F1D),
        surfaceContainerHighest = Color(0xFF232927),
    )

    override val lightScheme = lightColorScheme(
        primary = Color(0xFF006C52),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFA6F5DA),
        onPrimaryContainer = Color(0xFF002117),
        inversePrimary = Color(0xFF5FE3B8),
        secondary = Color(0xFF006C52), // Unread badge
        onSecondary = Color(0xFFFFFFFF), // Unread badge text
        secondaryContainer = Color(0xFFCDEFE2), // Navigation bar selector pill & progress indicator (remaining)
        onSecondaryContainer = Color(0xFF062B20), // Navigation bar selector icon
        tertiary = Color(0xFF5E4FA2), // Downloaded badge
        onTertiary = Color(0xFFFFFFFF), // Downloaded badge text
        tertiaryContainer = Color(0xFFE6DEFF),
        onTertiaryContainer = Color(0xFF1A0A5C),
        background = Color(0xFFF6F8F7),
        onBackground = Color(0xFF141917),
        surface = Color(0xFFF6F8F7),
        onSurface = Color(0xFF141917),
        surfaceVariant = Color(0xFFE7ECE9), // Navigation bar background (ThemePrefWidget)
        onSurfaceVariant = Color(0xFF404945),
        surfaceTint = Color(0xFF006C52),
        inverseSurface = Color(0xFF29302D),
        inverseOnSurface = Color(0xFFEDF2EF),
        error = Color(0xFFBA1A1A),
        onError = Color(0xFFFFFFFF),
        errorContainer = Color(0xFFFFDAD6),
        onErrorContainer = Color(0xFF410002),
        outline = Color(0xFF707975),
        outlineVariant = Color(0xFFC0C9C4),
        scrim = Color(0xFF000000),
        surfaceBright = Color(0xFFF6F8F7),
        surfaceDim = Color(0xFFD5DCD8),
        surfaceContainerLowest = Color(0xFFFFFFFF),
        surfaceContainerLow = Color(0xFFF0F4F2),
        surfaceContainer = Color(0xFFE7ECE9), // Navigation bar background
        surfaceContainerHigh = Color(0xFFE1E7E4),
        surfaceContainerHighest = Color(0xFFDAE1DD),
    )
}

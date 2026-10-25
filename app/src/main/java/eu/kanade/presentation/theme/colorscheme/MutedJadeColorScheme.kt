package eu.kanade.presentation.theme.colorscheme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Colors for Muted Jade theme
 * True black with a muted jade accent and dimmed text.
 * Calm palette for long reading in dim rooms: text around 10-11:1, accent 6.5-7:1, no pure white.
 * Generated from OKLCH (fixed hue/chroma, lightness solved for a WCAG target); see docs/desain-tema-modern.md.
 *
 * Key colors (dark):
 * Primary #729F8D
 * Secondary #698C7E
 * Tertiary #84819C
 * Neutral #000000
 */
internal object MutedJadeColorScheme : BaseColorScheme() {

    override val darkScheme = darkColorScheme(
        primary = Color(0xFF729F8D),
        onPrimary = Color(0xFF091F18),
        primaryContainer = Color(0xFF1E392F),
        onPrimaryContainer = Color(0xFFADC8BD),
        inversePrimary = Color(0xFF345F4F),
        secondary = Color(0xFF698C7E), // Unread badge, selected navigation label
        onSecondary = Color(0xFF03130D), // Unread badge text
        secondaryContainer = Color(0xFF1C3028), // Navigation bar selector pill & progress indicator (remaining)
        onSecondaryContainer = Color(0xFFADC3B9), // Navigation bar selector icon
        tertiary = Color(0xFF84819C), // Downloaded badge
        onTertiary = Color(0xFF0F0E18), // Downloaded badge text
        tertiaryContainer = Color(0xFF2D2B3D),
        onTertiaryContainer = Color(0xFFB9B8C6),
        background = Color(0xFF000000),
        onBackground = Color(0xFFAEB5B1),
        surface = Color(0xFF000000),
        onSurface = Color(0xFFAEB5B1),
        surfaceVariant = Color(0xFF161918), // Navigation bar background (ThemePrefWidget)
        onSurfaceVariant = Color(0xFF898E8B),
        surfaceTint = Color(0xFF729F8D),
        inverseSurface = Color(0xFFAEB5B1),
        inverseOnSurface = Color(0xFF161918),
        error = Color(0xFFB9827C),
        onError = Color(0xFF3A1412),
        errorContainer = Color(0xFF512320),
        onErrorContainer = Color(0xFFD2B9B6),
        outline = Color(0xFF5D6360),
        outlineVariant = Color(0xFF313432),
        scrim = Color(0xFF000000),
        surfaceBright = Color(0xFF363937),
        surfaceDim = Color(0xFF000000),
        surfaceContainerLowest = Color(0xFF000000),
        surfaceContainerLow = Color(0xFF0E100F),
        surfaceContainer = Color(0xFF161918), // Navigation bar background
        surfaceContainerHigh = Color(0xFF202221),
        surfaceContainerHighest = Color(0xFF292C2B),
    )

    override val lightScheme = lightColorScheme(
        primary = Color(0xFF1D6F56),
        onPrimary = Color(0xFFF2FBF7),
        primaryContainer = Color(0xFFBDE1D2),
        onPrimaryContainer = Color(0xFF064332),
        inversePrimary = Color(0xFF729F8D),
        secondary = Color(0xFF2B6D57), // Unread badge, selected navigation label
        onSecondary = Color(0xFFF2FBF7), // Unread badge text
        secondaryContainer = Color(0xFFBADACC), // Navigation bar selector pill & progress indicator (remaining)
        onSecondaryContainer = Color(0xFF003E2D), // Navigation bar selector icon
        tertiary = Color(0xFF635D86), // Downloaded badge
        onTertiary = Color(0xFFF8F8FE), // Downloaded badge text
        tertiaryContainer = Color(0xFFD7D4EC),
        onTertiaryContainer = Color(0xFF39364E),
        background = Color(0xFFF0F5F2),
        onBackground = Color(0xFF2A312E),
        surface = Color(0xFFF0F5F2),
        onSurface = Color(0xFF2A312E),
        surfaceVariant = Color(0xFFE2E8E5), // Navigation bar background (ThemePrefWidget)
        onSurfaceVariant = Color(0xFF4C5451),
        surfaceTint = Color(0xFF1D6F56),
        inverseSurface = Color(0xFF2A2F2D),
        inverseOnSurface = Color(0xFFE8ECEA),
        error = Color(0xFFA93F38),
        onError = Color(0xFFFFFFFF),
        errorContainer = Color(0xFFFED2CC),
        onErrorContainer = Color(0xFF622C27),
        outline = Color(0xFF808583),
        outlineVariant = Color(0xFFC7CCC9),
        scrim = Color(0xFF000000),
        surfaceBright = Color(0xFFF0F5F2),
        surfaceDim = Color(0xFFCDD2D0),
        surfaceContainerLowest = Color(0xFFF8FBF9),
        surfaceContainerLow = Color(0xFFE9EEEC),
        surfaceContainer = Color(0xFFE2E8E5), // Navigation bar background
        surfaceContainerHigh = Color(0xFFDCE1DE),
        surfaceContainerHighest = Color(0xFFD5DAD8),
    )
}

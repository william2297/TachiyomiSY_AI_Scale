package eu.kanade.presentation.theme.colorscheme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Colors for Graphite theme
 * Near-neutral graphite with a faint slate-blue accent.
 * Calm palette for long reading in dim rooms: text around 10-11:1, accent 6.5-7:1, no pure white.
 * Generated from OKLCH (fixed hue/chroma, lightness solved for a WCAG target); see docs/desain-tema-modern.md.
 *
 * Key colors (dark):
 * Primary #8994A7
 * Secondary #808998
 * Tertiary #7C8087
 * Neutral #08090B
 */
internal object GraphiteColorScheme : BaseColorScheme() {

    override val darkScheme = darkColorScheme(
        primary = Color(0xFF8994A7),
        onPrimary = Color(0xFF161B24),
        primaryContainer = Color(0xFF2D333F),
        onPrimaryContainer = Color(0xFFBAC1CE),
        inversePrimary = Color(0xFF4C5667),
        secondary = Color(0xFF808998), // Unread badge, selected navigation label
        onSecondary = Color(0xFF0C0F16), // Unread badge text
        secondaryContainer = Color(0xFF272C34), // Navigation bar selector pill & progress indicator (remaining)
        onSecondaryContainer = Color(0xFFB8BEC8), // Navigation bar selector icon
        tertiary = Color(0xFF7C8087), // Downloaded badge
        onTertiary = Color(0xFF0E0F12), // Downloaded badge text
        tertiaryContainer = Color(0xFF2B2E33),
        onTertiaryContainer = Color(0xFFB8BABD),
        background = Color(0xFF08090B),
        onBackground = Color(0xFFBBBDC0),
        surface = Color(0xFF08090B),
        onSurface = Color(0xFFBBBDC0),
        surfaceVariant = Color(0xFF1A1B1C), // Navigation bar background (ThemePrefWidget)
        onSurfaceVariant = Color(0xFF8F9194),
        surfaceTint = Color(0xFF8994A7),
        inverseSurface = Color(0xFFBBBDC0),
        inverseOnSurface = Color(0xFF1A1B1C),
        error = Color(0xFFBE8580),
        onError = Color(0xFF3A1412),
        errorContainer = Color(0xFF512320),
        onErrorContainer = Color(0xFFD2B9B6),
        outline = Color(0xFF636568),
        outlineVariant = Color(0xFF323335),
        scrim = Color(0xFF000000),
        surfaceBright = Color(0xFF37383A),
        surfaceDim = Color(0xFF08090B),
        surfaceContainerLowest = Color(0xFF060608),
        surfaceContainerLow = Color(0xFF111213),
        surfaceContainer = Color(0xFF1A1B1C), // Navigation bar background
        surfaceContainerHigh = Color(0xFF222325),
        surfaceContainerHighest = Color(0xFF2A2B2D),
    )

    override val lightScheme = lightColorScheme(
        primary = Color(0xFF526280),
        onPrimary = Color(0xFFF6F9FD),
        primaryContainer = Color(0xFFCED8E9),
        onPrimaryContainer = Color(0xFF2E394E),
        inversePrimary = Color(0xFF8994A7),
        secondary = Color(0xFF54627C), // Unread badge, selected navigation label
        onSecondary = Color(0xFFF6F9FD), // Unread badge text
        secondaryContainer = Color(0xFFC9D1E0), // Navigation bar selector pill & progress indicator (remaining)
        onSecondaryContainer = Color(0xFF2B3549), // Navigation bar selector icon
        tertiary = Color(0xFF5F626A), // Downloaded badge
        onTertiary = Color(0xFFF8F8FA), // Downloaded badge text
        tertiaryContainer = Color(0xFFD4D7DE),
        onTertiaryContainer = Color(0xFF363940),
        background = Color(0xFFF2F3F7),
        onBackground = Color(0xFF2D2F34),
        surface = Color(0xFFF2F3F7),
        onSurface = Color(0xFF2D2F34),
        surfaceVariant = Color(0xFFE5E6E9), // Navigation bar background (ThemePrefWidget)
        onSurfaceVariant = Color(0xFF505258),
        surfaceTint = Color(0xFF526280),
        inverseSurface = Color(0xFF2C2E31),
        inverseOnSurface = Color(0xFFEAEBEE),
        error = Color(0xFFA83F38),
        onError = Color(0xFFFFFFFF),
        errorContainer = Color(0xFFFED2CC),
        onErrorContainer = Color(0xFF622C27),
        outline = Color(0xFF828387),
        outlineVariant = Color(0xFFC9CACE),
        scrim = Color(0xFF000000),
        surfaceBright = Color(0xFFF2F3F7),
        surfaceDim = Color(0xFFCFD1D4),
        surfaceContainerLowest = Color(0xFFF9FAFC),
        surfaceContainerLow = Color(0xFFEBEDF0),
        surfaceContainer = Color(0xFFE5E6E9), // Navigation bar background
        surfaceContainerHigh = Color(0xFFDEE0E3),
        surfaceContainerHighest = Color(0xFFD7D9DC),
    )
}

package eu.kanade.presentation.theme.colorscheme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Colors for Warm Charcoal theme
 * Lifted warm charcoal, warm low-blue text and a sage accent. Default theme.
 * Calm palette for long reading in dim rooms: text around 10-11:1, accent 6.5-7:1, no pure white.
 * Generated from OKLCH (fixed hue/chroma, lightness solved for a WCAG target); see docs/desain-tema-modern.md.
 *
 * Key colors (dark):
 * Primary #899C80
 * Secondary #808F79
 * Tertiary #9D8677
 * Neutral #100E0B
 */
internal object WarmCharcoalColorScheme : BaseColorScheme() {

    override val darkScheme = darkColorScheme(
        primary = Color(0xFF899C80),
        onPrimary = Color(0xFF151D11),
        primaryContainer = Color(0xFF2C3726),
        onPrimaryContainer = Color(0xFFBAC6B4),
        inversePrimary = Color(0xFF4B5B42),
        secondary = Color(0xFF808F79), // Unread badge, selected navigation label
        onSecondary = Color(0xFF0B1208), // Unread badge text
        secondaryContainer = Color(0xFF262E22), // Navigation bar selector pill & progress indicator (remaining)
        onSecondaryContainer = Color(0xFFB7C1B2), // Navigation bar selector icon
        tertiary = Color(0xFF9D8677), // Downloaded badge
        onTertiary = Color(0xFF150D08), // Downloaded badge text
        tertiaryContainer = Color(0xFF392A20),
        onTertiaryContainer = Color(0xFFC3B7AF),
        background = Color(0xFF100E0B),
        onBackground = Color(0xFFC6BFB3),
        surface = Color(0xFF100E0B),
        onSurface = Color(0xFFC6BFB3),
        surfaceVariant = Color(0xFF221F1A), // Navigation bar background (ThemePrefWidget)
        onSurfaceVariant = Color(0xFF999387),
        surfaceTint = Color(0xFF899C80),
        inverseSurface = Color(0xFFC6BFB3),
        inverseOnSurface = Color(0xFF221F1A),
        error = Color(0xFFC08882),
        onError = Color(0xFF3A1412),
        errorContainer = Color(0xFF512320),
        onErrorContainer = Color(0xFFD2B9B6),
        outline = Color(0xFF6D665C),
        outlineVariant = Color(0xFF3B3732),
        scrim = Color(0xFF000000),
        surfaceBright = Color(0xFF403D37),
        surfaceDim = Color(0xFF100E0B),
        surfaceContainerLowest = Color(0xFF0E0B06),
        surfaceContainerLow = Color(0xFF191611),
        surfaceContainer = Color(0xFF221F1A), // Navigation bar background
        surfaceContainerHigh = Color(0xFF2A2722),
        surfaceContainerHighest = Color(0xFF33302A),
    )

    override val lightScheme = lightColorScheme(
        primary = Color(0xFF4E6A3F),
        onPrimary = Color(0xFFF6FAF4),
        primaryContainer = Color(0xFFCEDDC6),
        onPrimaryContainer = Color(0xFF2D3F22),
        inversePrimary = Color(0xFF899C80),
        secondary = Color(0xFF516945), // Unread badge, selected navigation label
        onSecondary = Color(0xFFF6FAF4), // Unread badge text
        secondaryContainer = Color(0xFFC8D6C1), // Navigation bar selector pill & progress indicator (remaining)
        onSecondaryContainer = Color(0xFF283A1E), // Navigation bar selector icon
        tertiary = Color(0xFF7D5B45), // Downloaded badge
        onTertiary = Color(0xFFFCF7F4), // Downloaded badge text
        tertiaryContainer = Color(0xFFE7D3C6),
        onTertiaryContainer = Color(0xFF483528),
        background = Color(0xFFF9F3E9),
        onBackground = Color(0xFF372E20),
        surface = Color(0xFFF9F3E9),
        onSurface = Color(0xFF372E20),
        surfaceVariant = Color(0xFFEBE5DC), // Navigation bar background (ThemePrefWidget)
        onSurfaceVariant = Color(0xFF5A5142),
        surfaceTint = Color(0xFF4E6A3F),
        inverseSurface = Color(0xFF332D24),
        inverseOnSurface = Color(0xFFEFEAE3),
        error = Color(0xFFA93F38),
        onError = Color(0xFFFFFFFF),
        errorContainer = Color(0xFFFED2CC),
        onErrorContainer = Color(0xFF622C27),
        outline = Color(0xFF898378),
        outlineVariant = Color(0xFFCFCAC0),
        scrim = Color(0xFF000000),
        surfaceBright = Color(0xFFF9F3E9),
        surfaceDim = Color(0xFFD6D0C7),
        surfaceContainerLowest = Color(0xFFFDFAF3),
        surfaceContainerLow = Color(0xFFF2ECE2),
        surfaceContainer = Color(0xFFEBE5DC), // Navigation bar background
        surfaceContainerHigh = Color(0xFFE5DFD5),
        surfaceContainerHighest = Color(0xFFDED8CF),
    )
}

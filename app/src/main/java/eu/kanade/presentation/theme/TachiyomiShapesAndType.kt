package eu.kanade.presentation.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * App-wide corner scale (all color schemes).
 *
 * Library grid items clip with [Shapes.small] and pad the cover by 4.dp, while the cover itself
 * clips with [Shapes.extraSmall]. Keeping small - 4 == extraSmall makes the selection outline and
 * the cover corners concentric.
 *
 * Material defaults for comparison: 4 / 8 / 12 / 16 / 28.
 */
internal val TachiyomiShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp), // covers, badges, snackbars, menus
    small = RoundedCornerShape(12.dp), // grid item touch area, chips, list rows
    medium = RoundedCornerShape(16.dp), // cards, dialogs content blocks
    large = RoundedCornerShape(20.dp), // bottom action menu, FAB
    extraLarge = RoundedCornerShape(28.dp), // sheets, section cards, pills
)

/**
 * App-wide type scale (all color schemes), still on the system sans so nothing new is bundled.
 *
 * Headings get more weight and tighter tracking; small text loses the wide 0.4-0.5sp tracking
 * that makes the default M3 scale look dated on a dense tablet layout.
 */
internal val TachiyomiTypography: Typography = Typography().let { base ->
    base.copy(
        headlineLarge = base.headlineLarge.copy(fontWeight = FontWeight.SemiBold, letterSpacing = (-0.25).sp),
        headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.SemiBold, letterSpacing = (-0.2).sp),
        headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.SemiBold, letterSpacing = (-0.1).sp),
        titleLarge = base.titleLarge.copy(fontWeight = FontWeight.SemiBold),
        titleMedium = base.titleMedium.copy(letterSpacing = 0.1.sp),
        bodyLarge = base.bodyLarge.copy(letterSpacing = 0.2.sp),
        bodyMedium = base.bodyMedium.copy(letterSpacing = 0.15.sp),
        bodySmall = base.bodySmall.copy(letterSpacing = 0.2.sp),
        labelMedium = base.labelMedium.copy(letterSpacing = 0.3.sp),
        labelSmall = base.labelSmall.copy(letterSpacing = 0.3.sp),
    )
}

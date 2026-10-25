package eu.kanade.presentation.reader.appbars

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import eu.kanade.tachiyomi.ui.reader.setting.ReaderOrientation
import eu.kanade.tachiyomi.ui.reader.setting.ReadingMode
import mihon.icons.phosphor.Phosphor
import mihon.icons.phosphor.bold.GearSix
import mihon.icons.phosphor.bold.Image
import mihon.icons.phosphor.bold.Pause
import mihon.icons.phosphor.bold.Play
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun ReaderBottomBar(
    readingMode: ReadingMode,
    onClickReadingMode: () -> Unit,
    onClickPageLayout: () -> Unit,
    orientation: ReaderOrientation,
    onClickOrientation: () -> Unit,
    cropEnabled: Boolean,
    onClickCropBorder: () -> Unit,
    imageEnhancementEnabled: Boolean,
    onClickImageEnhancement: () -> Unit,
    autoScrollActive: Boolean,
    onClickAutoScroll: () -> Unit,
    onClickSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Page layout, rotation and crop buttons are hidden on purpose; the parameters stay so callers are unchanged.
    val iconTint = MaterialTheme.colorScheme.onSurface
    val inactiveTint = iconTint.copy(alpha = 0.6f)

    Row(
        modifier = modifier
            .pointerInput(Unit) {},
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onClickReadingMode) {
            Icon(
                imageVector = readingMode.icon,
                contentDescription = stringResource(MR.strings.viewer),
            )
        }

        IconButton(onClick = onClickImageEnhancement) {
            Icon(
                imageVector = Phosphor.Bold.Image,
                contentDescription = stringResource(MR.strings.reader_image_enhancement),
                tint = if (imageEnhancementEnabled) iconTint else inactiveTint,
            )
        }

        IconButton(onClick = onClickAutoScroll) {
            Icon(
                imageVector = if (autoScrollActive) Phosphor.Bold.Pause else Phosphor.Bold.Play,
                contentDescription = stringResource(MR.strings.reader_auto_scroll),
                tint = if (autoScrollActive) iconTint else inactiveTint,
            )
        }

        IconButton(onClick = onClickSettings) {
            Icon(
                imageVector = Phosphor.Bold.GearSix,
                contentDescription = stringResource(MR.strings.action_settings),
            )
        }
    }
}

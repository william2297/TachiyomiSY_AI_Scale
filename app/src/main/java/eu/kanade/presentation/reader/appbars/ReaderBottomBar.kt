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
import androidx.compose.ui.res.painterResource
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.reader.setting.ReaderOrientation
import eu.kanade.tachiyomi.ui.reader.setting.ReadingMode
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Photo
import mihon.icons.materialsymbols.rounded.Settings
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
                imageVector = mihon.icons.materialsymbols.MaterialSymbols.Rounded.Photo,
                contentDescription = stringResource(MR.strings.reader_image_enhancement),
                tint = if (imageEnhancementEnabled) iconTint else inactiveTint,
            )
        }

        IconButton(onClick = onClickAutoScroll) {
            Icon(
                painter = painterResource(if (autoScrollActive) R.drawable.ic_pause_24dp else R.drawable.ic_play_arrow_24dp),
                contentDescription = stringResource(MR.strings.reader_auto_scroll),
                tint = if (autoScrollActive) iconTint else inactiveTint,
            )
        }

        IconButton(onClick = onClickSettings) {
            Icon(
                imageVector = MaterialSymbols.Rounded.Settings,
                contentDescription = stringResource(MR.strings.action_settings),
            )
        }
    }
}

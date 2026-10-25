package eu.kanade.presentation.reader.appbars

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.util.isTabletUi
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import kotlin.math.roundToInt

private const val MIN_INTERVAL = 1
private const val MAX_INTERVAL = 15

/**
 * Auto scroll controls shown above the reader's bottom bar: one switch to start or stop and one slider for the speed.
 * The slider is a speed (right is faster), stored as seconds per page or per screen.
 */
@Composable
fun AutoScrollPanel(
    visible: Boolean,
    active: Boolean,
    intervalSeconds: Float,
    onToggle: () -> Unit,
    onIntervalChange: (Float) -> Unit,
    backgroundColor: Color,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(visible = visible, modifier = modifier) {
        val interval = intervalSeconds.roundToInt().coerceIn(MIN_INTERVAL, MAX_INTERVAL)
        // Floating card with the same inset and corner as the ChapterNavigator pill above it
        val sideInset = if (isTabletUi()) 24.dp else 8.dp
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = sideInset)
                .clip(RoundedCornerShape(24.dp))
                .background(backgroundColor)
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(MR.strings.reader_auto_scroll),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f),
                )
                Switch(checked = active, onCheckedChange = { onToggle() })
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(MR.strings.reader_auto_scroll_speed),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Slider(
                    value = (MAX_INTERVAL + MIN_INTERVAL - interval).toFloat(),
                    onValueChange = { onIntervalChange((MAX_INTERVAL + MIN_INTERVAL - it.roundToInt()).toFloat()) },
                    valueRange = MIN_INTERVAL.toFloat()..MAX_INTERVAL.toFloat(),
                    steps = MAX_INTERVAL - MIN_INTERVAL - 1,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp),
                )
                Text(
                    text = stringResource(MR.strings.reader_auto_scroll_seconds, interval),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

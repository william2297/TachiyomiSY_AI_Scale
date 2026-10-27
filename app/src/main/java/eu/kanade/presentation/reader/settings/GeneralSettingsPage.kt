package eu.kanade.presentation.reader.settings

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import eu.kanade.tachiyomi.ui.reader.setting.ReaderPreferences
import eu.kanade.tachiyomi.ui.reader.setting.ReaderSettingsViewModel
import eu.kanade.tachiyomi.ui.reader.setting.ReadingMode
import eu.kanade.tachiyomi.util.system.hasDisplayCutout
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.CheckboxItem
import tachiyomi.presentation.core.components.SettingsChipRow
import tachiyomi.presentation.core.components.SliderItem
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState
import kotlin.math.roundToInt

private val themes = listOf(
    MR.strings.black_background to 1,
    MR.strings.gray_background to 2,
    MR.strings.white_background to 0,
    MR.strings.automatic_background to 3,
)

private val flashColors = listOf(
    MR.strings.pref_flash_style_black to ReaderPreferences.FlashColor.BLACK,
    MR.strings.pref_flash_style_white to ReaderPreferences.FlashColor.WHITE,
    MR.strings.pref_flash_style_white_black to ReaderPreferences.FlashColor.WHITE_BLACK,
)

@Composable
internal fun ColumnScope.GeneralPage(viewModel: ReaderSettingsViewModel) {
    val readerTheme by viewModel.preferences.readerTheme.collectAsState()

    val flashPageState by viewModel.preferences.flashOnPageChange.collectAsState()

    val flashMillisPref = viewModel.preferences.flashDurationMillis
    val flashMillis by flashMillisPref.collectAsState()

    val flashIntervalPref = viewModel.preferences.flashPageInterval
    val flashInterval by flashIntervalPref.collectAsState()

    val flashColorPref = viewModel.preferences.flashColor
    val flashColor by flashColorPref.collectAsState()

    SettingsChipRow(MR.strings.pref_reader_theme) {
        themes.map { (labelRes, value) ->
            FilterChip(
                selected = readerTheme == value,
                onClick = { viewModel.preferences.readerTheme.set(value) },
                label = { Text(stringResource(labelRes)) },
            )
        }
    }

    CheckboxItem(
        label = stringResource(MR.strings.pref_show_page_number),
        pref = viewModel.preferences.showPageNumber,
    )

    val verticalNavigatorModes by viewModel.preferences.verticalNavigator.collectAsState()

    SettingsChipRow(MR.strings.pref_vertical_navigator) {
        ReadingMode.entries.filter { it != ReadingMode.DEFAULT }.forEach { mode ->
            FilterChip(
                selected = verticalNavigatorModes.contains(mode),
                onClick = {
                    val newModes = if (verticalNavigatorModes.contains(mode)) {
                        verticalNavigatorModes - mode
                    } else {
                        verticalNavigatorModes + mode
                    }
                    viewModel.preferences.verticalNavigator.set(newModes)
                },
                label = { Text(stringResource(mode.stringRes)) },
            )
        }
    }

    if (verticalNavigatorModes.isNotEmpty()) {
        val verticalNavigatorHeightPref = viewModel.preferences.verticalNavigatorHeight
        val verticalNavigatorHeight by verticalNavigatorHeightPref.collectAsState()

        CheckboxItem(
            label = stringResource(MR.strings.pref_webtoon_vertical_navigator_on_left),
            pref = viewModel.preferences.verticalNavigatorOnLeft,
        )

        SliderItem(
            label = stringResource(MR.strings.pref_vertical_navigator_height),
            value = verticalNavigatorHeight,
            valueRange = 65..100,
            steps = 6,
            onChange = { verticalNavigatorHeightPref.set(it) },
        )
    }

    CheckboxItem(
        label = stringResource(MR.strings.pref_fullscreen),
        pref = viewModel.preferences.fullscreen,
    )

    val isFullscreen by viewModel.preferences.fullscreen.collectAsState()
    if (LocalActivity.current?.hasDisplayCutout() == true && isFullscreen) {
        CheckboxItem(
            label = stringResource(MR.strings.pref_cutout_short),
            pref = viewModel.preferences.drawUnderCutout,
        )
    }

    CheckboxItem(
        label = stringResource(MR.strings.pref_keep_screen_on),
        pref = viewModel.preferences.keepScreenOn,
    )

    CheckboxItem(
        label = stringResource(MR.strings.pref_read_with_long_tap),
        pref = viewModel.preferences.readWithLongTap,
    )

    CheckboxItem(
        label = stringResource(MR.strings.pref_always_show_chapter_transition),
        pref = viewModel.preferences.alwaysShowChapterTransition,
    )

    CheckboxItem(
        label = stringResource(MR.strings.pref_page_transitions),
        pref = viewModel.preferences.pageTransitions,
    )

    CheckboxItem(
        label = stringResource(MR.strings.pref_flash_page),
        pref = viewModel.preferences.flashOnPageChange,
    )
    if (flashPageState) {
        SliderItem(
            value = flashMillis / ReaderPreferences.MILLI_CONVERSION,
            valueRange = 1..15,
            label = stringResource(MR.strings.pref_flash_duration),
            valueString = stringResource(MR.strings.pref_flash_duration_summary, flashMillis),
            onChange = { flashMillisPref.set(it * ReaderPreferences.MILLI_CONVERSION) },
            pillColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        )
        SliderItem(
            value = flashInterval,
            valueRange = 1..10,
            label = stringResource(MR.strings.pref_flash_page_interval),
            valueString = pluralStringResource(MR.plurals.pref_pages, flashInterval, flashInterval),
            onChange = {
                flashIntervalPref.set(it)
            },
            pillColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        )
        SettingsChipRow(MR.strings.pref_flash_with) {
            flashColors.map { (labelRes, value) ->
                FilterChip(
                    selected = flashColor == value,
                    onClick = { flashColorPref.set(value) },
                    label = { Text(stringResource(labelRes)) },
                )
            }
        }
    }

    val autoScrollIntervalPref = viewModel.preferences.autoScrollInterval
    val autoScrollInterval by autoScrollIntervalPref.collectAsState()
    // Shown as a speed (right is faster) but stored as seconds per page or per screen
    val autoScrollSpeed = (AUTO_SCROLL_MAX_SECONDS + 1 - autoScrollInterval.roundToInt())
        .coerceIn(1, AUTO_SCROLL_MAX_SECONDS)
    SliderItem(
        value = autoScrollSpeed,
        valueRange = 1..AUTO_SCROLL_MAX_SECONDS,
        label = stringResource(MR.strings.reader_auto_scroll_speed),
        valueString = stringResource(
            MR.strings.reader_auto_scroll_seconds,
            AUTO_SCROLL_MAX_SECONDS + 1 - autoScrollSpeed,
        ),
        onChange = { autoScrollIntervalPref.set((AUTO_SCROLL_MAX_SECONDS + 1 - it).toFloat()) },
        steps = 0,
        pillColor = MaterialTheme.colorScheme.surfaceContainerHighest,
    )
    CheckboxItem(
        label = stringResource(MR.strings.reader_auto_scroll_smooth),
        pref = viewModel.preferences.smoothAutoScroll,
    )
    val tapScrollPercentPref = viewModel.preferences.tapScrollPercent
    val tapScrollPercent by tapScrollPercentPref.collectAsState()
    SliderItem(
        value = tapScrollPercent / 5,
        valueRange = 5..30,
        label = stringResource(MR.strings.reader_tap_scroll_distance),
        valueString = "$tapScrollPercent%",
        onChange = { tapScrollPercentPref.set(it * 5) },
        pillColor = MaterialTheme.colorScheme.surfaceContainerHighest,
    )
    val tapScrollMillisPref = viewModel.preferences.tapScrollMillis
    val tapScrollMillis by tapScrollMillisPref.collectAsState()
    SliderItem(
        value = tapScrollMillis / 100,
        valueRange = 0..20,
        label = stringResource(MR.strings.reader_tap_scroll_time),
        valueString = if (tapScrollMillis == 0) "Instant" else "$tapScrollMillis ms",
        onChange = { tapScrollMillisPref.set(it * 100) },
        pillColor = MaterialTheme.colorScheme.surfaceContainerHighest,
    )
    CheckboxItem(
        label = stringResource(MR.strings.reader_auto_scroll_gestures),
        pref = viewModel.preferences.autoscrollGesturesEnabled,
    )
    CheckboxItem(
        label = stringResource(MR.strings.reader_auto_scroll_gesture_toasts),
        pref = viewModel.preferences.autoscrollGestureToastsEnabled,
    )
    CheckboxItem(
        label = stringResource(MR.strings.reader_debug_page_layers),
        pref = viewModel.preferences.debugPageLayers,
    )
}

private const val AUTO_SCROLL_MAX_SECONDS = 15

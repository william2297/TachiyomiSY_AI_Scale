package eu.kanade.domain.ui

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import eu.kanade.domain.ui.model.AppTheme
import eu.kanade.domain.ui.model.TabletUiMode
import eu.kanade.domain.ui.model.ThemeMode
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.core.common.preference.getEnum
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

@Inject
@SingleIn(AppScope::class)
class UiPreferences(
    private val preferenceStore: PreferenceStore,
) {

    val themeMode: Preference<ThemeMode> = preferenceStore.getEnum("pref_theme_mode_key", ThemeMode.SYSTEM)

    // Fresh installs start on Warm Charcoal (calm dark palette for long reading). Stored choices are untouched.
    val appTheme: Preference<AppTheme> = preferenceStore.getEnum("pref_app_theme", AppTheme.WARM_CHARCOAL)

    init {
        // One time: builds before the calm themes kept the old default stored (Mihon default or Monet), so a
        // fresh APK would still look the same. Move to Warm Charcoal once; later choices are respected.
        val migrated = preferenceStore.getBoolean("pref_calm_theme_migrated", false)
        if (!migrated.get()) {
            appTheme.set(AppTheme.WARM_CHARCOAL)
            migrated.set(true)
        }
    }

    val themeDarkAmoled: Preference<Boolean> = preferenceStore.getBoolean("pref_theme_dark_amoled_key", false)

    val relativeTime: Preference<Boolean> = preferenceStore.getBoolean("relative_time_v2", true)

    val dateFormat: Preference<String> = preferenceStore.getString("app_date_format", "")

    val tabletUiMode: Preference<TabletUiMode> = preferenceStore.getEnum("tablet_ui_mode", TabletUiMode.AUTOMATIC)

    val imagesInDescription: Preference<Boolean> = preferenceStore.getBoolean("pref_render_images_description", true)

    companion object {
        fun dateFormat(format: String): DateTimeFormatter = when (format) {
            "" -> DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT)
            else -> DateTimeFormatter.ofPattern(format, Locale.getDefault())
        }
    }
}

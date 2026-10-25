package eu.kanade.presentation.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.presentation.more.settings.PreferenceScreen
import eu.kanade.presentation.reader.appbars.ReaderBottomBar
import eu.kanade.presentation.theme.TachiyomiShapes
import eu.kanade.presentation.theme.TachiyomiTypography
import eu.kanade.presentation.theme.colorscheme.WarmCharcoalColorScheme
import eu.kanade.tachiyomi.ui.reader.setting.ReaderOrientation
import eu.kanade.tachiyomi.ui.reader.setting.ReadingMode
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.ConscryptMode
import org.robolectric.annotation.GraphicsMode
import tachiyomi.presentation.core.components.Badge
import tachiyomi.presentation.core.components.BadgeGroup

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@ConscryptMode(ConscryptMode.Mode.OFF)
@Config(sdk = [35], application = android.app.Application::class, qualifiers = "w1680dp-h1050dp-xhdpi")
open class DesignPreviewTest {
    @get:Rule
    val composeRule = createComposeRule()

    /** Appended to every file name, so another orientation can reuse the same screens. */
    protected open val suffix: String = ""

    @Test
    fun defaultThemeDark() = capture("default-theme-dark.png") { Sheet() }

    @Test
    fun settings() = capture("settings.png") {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            PreferenceScreen(
                items = listOf(
                    Preference.PreferenceGroup(
                        title = "Appearance",
                        preferenceItems = listOf(
                            Preference.PreferenceItem.TextPreference(title = "App theme", subtitle = "Warm Charcoal"),
                            Preference.PreferenceItem.TextPreference(title = "Dark mode", subtitle = "Follow system"),
                            Preference.PreferenceItem.TextPreference(title = "Tablet UI", subtitle = "Automatic"),
                        ),
                    ),
                    Preference.PreferenceGroup(
                        title = "Reader",
                        preferenceItems = listOf(
                            Preference.PreferenceItem.TextPreference(
                                title = "Default reading mode",
                                subtitle = "Long strip",
                            ),
                            Preference.PreferenceItem.TextPreference(
                                title = "Image enhancement",
                                subtitle = "Qualcomm NPU, Real-CUGAN SE 2x",
                            ),
                        ),
                    ),
                ),
                modifier = Modifier.width(900.dp),
            )
        }
    }

    @Test
    fun readerOverlay() = capture("reader-overlay.png") {
        Box(Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color(0xFF2A2A2A))) {
            Column(Modifier.align(Alignment.BottomCenter)) {
                ReaderBottomBar(
                    modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainer),
                    readingMode = ReadingMode.WEBTOON,
                    onClickReadingMode = {},
                    onClickPageLayout = {},
                    orientation = ReaderOrientation.DEFAULT,
                    onClickOrientation = {},
                    cropEnabled = false,
                    onClickCropBorder = {},
                    imageEnhancementEnabled = true,
                    onClickImageEnhancement = {},
                    autoScrollActive = false,
                    onClickAutoScroll = {},
                    onClickSettings = {},
                )
            }
        }
    }

    private fun capture(fileName: String, content: @Composable () -> Unit) {
        composeRule.setContent {
            MaterialExpressiveTheme(
                colorScheme = WarmCharcoalColorScheme.getColorScheme(
                    isDark = true,
                    isAmoled = false,
                    overrideDarkSurfaceContainers = false,
                ),
                shapes = TachiyomiShapes,
                typography = TachiyomiTypography,
            ) {
                // Same locals the app provides in setComposeContent
                CompositionLocalProvider(
                    LocalTextStyle provides MaterialTheme.typography.bodySmall,
                    LocalContentColor provides MaterialTheme.colorScheme.onBackground,
                ) {
                    content()
                }
            }
        }
        composeRule.onRoot().captureRoboImage(
            System.getProperty("preview.output.dir") + "/" + fileName.removeSuffix(".png") + suffix + ".png",
        )
    }
}

@Composable
private fun Sheet() {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Row(Modifier.fillMaxSize()) {
            NavigationRail(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                listOf("Library", "Updates", "History", "Browse", "More").forEachIndexed { i, label ->
                    NavigationRailItem(selected = i == 0, onClick = {
                    }, icon = {
                        Box(
                            Modifier.size(
                                24.dp,
                            ).background(MaterialTheme.colorScheme.onSurfaceVariant, RoundedCornerShape(6.dp)),
                        )
                    }, label = { Text(label) })
                }
            }
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Text("Library", style = MaterialTheme.typography.headlineMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    repeat(5) { index ->
                        Box(
                            Modifier
                                .width(150.dp)
                                .height(210.dp)
                                .background(
                                    MaterialTheme.colorScheme.surfaceContainerHigh,
                                    MaterialTheme.shapes.extraSmall,
                                ),
                        ) {
                            BadgeGroup(Modifier.padding(6.dp)) {
                                Badge(
                                    text = "${index + 3}",
                                    color = MaterialTheme.colorScheme.primary,
                                    textColor = MaterialTheme.colorScheme.onPrimary,
                                )
                                if (index % 2 == 0) {
                                    Badge(
                                        text = "DL",
                                        color = MaterialTheme.colorScheme.tertiary,
                                        textColor = MaterialTheme.colorScheme.onTertiary,
                                    )
                                }
                            }
                            Text(
                                "Sample title ${index + 1}",
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.align(Alignment.BottomStart).padding(8.dp),
                            )
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = true, onClick = {}, label = { Text("Unread") })
                    FilterChip(selected = false, onClick = {}, label = { Text("Downloaded") })
                    FilterChip(selected = false, onClick = {}, label = { Text("Started") })
                    TextButton(onClick = {}) { Text("Apply") }
                    Switch(checked = true, onCheckedChange = {})
                }
                Text("Reader", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.fillMaxWidth())
            }
        }
    }
}

/** Portrait Oppo Pad Mini: 1680x2520 px at about 344 dpi is roughly 781x1172 dp. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@ConscryptMode(ConscryptMode.Mode.OFF)
@Config(sdk = [35], application = android.app.Application::class, qualifiers = "w781dp-h1172dp-xhdpi")
class DesignPreviewPortraitTest : DesignPreviewTest() {
    override val suffix: String = "-portrait"
}

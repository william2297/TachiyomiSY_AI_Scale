package eu.kanade.presentation.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.presentation.more.settings.PreferenceScreen
import eu.kanade.presentation.reader.appbars.ReaderBottomBar
import eu.kanade.presentation.reader.appbars.ReaderTopBar
import eu.kanade.presentation.theme.TachiyomiShapes
import eu.kanade.presentation.theme.TachiyomiTypography
import eu.kanade.presentation.theme.colorscheme.JadeInkColorScheme
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

/**
 * Side-by-side comparison of the calm palette candidates (A, B, C) plus the current Jade Ink as reference.
 * Landscape puts the candidates in columns, portrait stacks them in rows.
 * Covers are sample gradients, the white block in the reader stands in for a bright manhwa page.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@ConscryptMode(ConscryptMode.Mode.OFF)
// Oppo Pad Mini: 2520x1680 px at about 344 dpi is roughly 1172x781 dp
@Config(sdk = [35], application = android.app.Application::class, qualifiers = "w1172dp-h781dp-xhdpi")
open class CalmPalettePreviewTest {
    @get:Rule
    val composeRule = createComposeRule()

    protected open val suffix: String = "-landscape"

    @Test
    fun library() = compare("calm-library") { LibraryMock() }

    @Test
    fun settings() = compare("calm-settings") { SettingsMock() }

    @Test
    fun reader() = compare("calm-reader") { ReaderMock() }

    /** Current Jade Ink in the same three mocks, for a before/after look. */
    @Test
    fun baseline() = capture("calm-baseline-jade-ink") {
        Cells(
            listOf(
                Entry("Before: Jade Ink, library", baselineScheme()) { LibraryMock() },
                Entry("Before: Jade Ink, settings", baselineScheme()) { SettingsMock() },
                Entry("Before: Jade Ink, reader", baselineScheme()) { ReaderMock() },
            ),
        )
    }

    private fun compare(name: String, content: @Composable () -> Unit) = capture(name) {
        Cells(CalmPalettes.map { Entry("${it.id}. ${it.title}", it.scheme, content) })
    }

    private fun capture(name: String, content: @Composable () -> Unit) {
        composeRule.setContent { content() }
        composeRule.onRoot().captureRoboImage(System.getProperty("preview.output.dir") + "/" + name + suffix + ".png")
    }
}

/** Portrait Oppo Pad Mini, roughly 781x1172 dp. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@ConscryptMode(ConscryptMode.Mode.OFF)
@Config(sdk = [35], application = android.app.Application::class, qualifiers = "w781dp-h1172dp-xhdpi")
class CalmPalettePortraitPreviewTest : CalmPalettePreviewTest() {
    override val suffix: String = "-portrait"
}

private fun baselineScheme() = JadeInkColorScheme.getColorScheme(
    isDark = true,
    isAmoled = false,
    overrideDarkSurfaceContainers = false,
)

private class Entry(
    val label: String,
    val scheme: androidx.compose.material3.ColorScheme,
    val content: @Composable () -> Unit,
)

@Composable
private fun Cells(cells: List<Entry>) {
    BoxWithConstraints(Modifier.fillMaxSize().background(Color(0xFF3A3A3A))) {
        val landscape = maxWidth > maxHeight
        if (landscape) {
            Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                cells.forEach { cell ->
                    Box(Modifier.weight(1f).fillMaxHeight()) { Cell(cell.label, cell.scheme, cell.content) }
                }
            }
        } else {
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                cells.forEach { cell ->
                    Box(Modifier.weight(1f).fillMaxWidth()) { Cell(cell.label, cell.scheme, cell.content) }
                }
            }
        }
    }
}

@Composable
private fun Cell(label: String, scheme: androidx.compose.material3.ColorScheme, content: @Composable () -> Unit) {
    MaterialExpressiveTheme(colorScheme = scheme, shapes = TachiyomiShapes, typography = TachiyomiTypography) {
        CompositionLocalProvider(
            LocalTextStyle provides MaterialTheme.typography.bodySmall,
            LocalContentColor provides MaterialTheme.colorScheme.onBackground,
        ) {
            Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                Column {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                    Box(Modifier.weight(1f).fillMaxWidth()) { content() }
                }
            }
        }
    }
}

/** Sample cover art: mid-saturated gradients similar in brightness to typical manhwa covers. */
private val SampleCovers = listOf(
    listOf(Color(0xFFE9B48A), Color(0xFF8A3B4A)),
    listOf(Color(0xFF7FB6E6), Color(0xFF283D73)),
    listOf(Color(0xFFF3E2C2), Color(0xFFB0644A)),
    listOf(Color(0xFF6C4D9C), Color(0xFF191433)),
    listOf(Color(0xFFDDE7EC), Color(0xFF5E7C8C)),
    listOf(Color(0xFFE7738A), Color(0xFF3B1B3A)),
)

@Composable
private fun LibraryMock() {
    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier.weight(1f).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Library", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = true, onClick = {}, label = { Text("Unread") })
                FilterChip(selected = false, onClick = {}, label = { Text("Downloaded") })
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                (0 until 6).forEach { i -> CoverMock(i, Modifier.width(112.dp)) }
            }
        }
        NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
            listOf("Library", "Updates", "History", "More").forEachIndexed { i, tab ->
                NavigationBarItem(
                    selected = i == 0,
                    onClick = {},
                    icon = {
                        Box(
                            Modifier
                                .size(20.dp)
                                .background(LocalContentColor.current, RoundedCornerShape(5.dp)),
                        )
                    },
                    label = { Text(tab, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                )
            }
        }
    }
}

@Composable
private fun CoverMock(index: Int, modifier: Modifier) {
    val selected = index == 4
    Box(
        modifier
            .aspectRatio(2f / 3f)
            .clip(MaterialTheme.shapes.extraSmall)
            .background(Brush.linearGradient(SampleCovers[index % SampleCovers.size])),
    ) {
        BadgeGroup(Modifier.padding(4.dp)) {
            if (index % 3 != 2) {
                Badge(
                    text = "${index * 7 + 3}",
                    color = MaterialTheme.colorScheme.tertiary,
                    textColor = MaterialTheme.colorScheme.onTertiary,
                )
            }
            Badge(text = "${index + 2}")
        }
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(0.45f)
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.35f to Color(0x99000000),
                        1f to Color(0xF0000000),
                    ),
                ),
        )
        Text(
            text = listOf("The Tower Returner", "Solo Leveling Again", "Moonlit Academy")[index % 3],
            style = MaterialTheme.typography.titleSmall,
            // Same as the app: theme text colour on a dense scrim
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            modifier = Modifier.align(Alignment.BottomStart).padding(8.dp),
        )
        if (selected) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f)),
            )
        }
    }
}

@Composable
private fun SettingsMock() {
    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f)) {
            PreferenceScreen(
                items = listOf(
                    Preference.PreferenceGroup(
                        title = "Appearance",
                        preferenceItems = listOf(
                            Preference.PreferenceItem.TextPreference(title = "App theme", subtitle = "Warm Charcoal"),
                            Preference.PreferenceItem.TextPreference(title = "Dark mode", subtitle = "Follow system"),
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
            )
        }
        Row(
            Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Switch(checked = true, onCheckedChange = {})
            Switch(checked = false, onCheckedChange = {})
            TextButton(onClick = {}) { Text("Save") }
        }
    }
}

@Composable
private fun ColumnScope.MangaPage(modifier: Modifier = Modifier) {
    // Bright page: white paper, two grey panels and a speech bubble outline
    Column(
        modifier
            .background(Color.White)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.fillMaxWidth().weight(1f).background(Color(0xFFD9D4CC)))
        Box(
            Modifier
                .width(120.dp)
                .height(40.dp)
                .background(Color(0xFFF4F4F4), RoundedCornerShape(20.dp)),
        )
        Box(Modifier.fillMaxWidth().weight(1.3f).background(Color(0xFFB9C3CC)))
    }
}

@Composable
private fun ReaderMock() {
    val bars = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.97f)
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            MangaPage(Modifier.fillMaxHeight().fillMaxWidth(0.72f))
        }
        Column(Modifier.fillMaxSize()) {
            ReaderTopBar(
                modifier = Modifier.background(bars),
                mangaTitle = "The Tower Returner",
                chapterTitle = "Chapter 118",
                navigateUp = {},
                bookmarked = true,
                onToggleBookmarked = {},
                onOpenInWebView = {},
                onOpenInBrowser = null,
                onShare = null,
            )
            Spacer(Modifier.weight(1f))
            Spacer(Modifier.height(8.dp))
            ReaderBottomBar(
                modifier = Modifier.fillMaxWidth().background(bars),
                readingMode = ReadingMode.WEBTOON,
                onClickReadingMode = {},
                onClickPageLayout = {},
                orientation = ReaderOrientation.DEFAULT,
                onClickOrientation = {},
                cropEnabled = false,
                onClickCropBorder = {},
                imageEnhancementEnabled = true,
                onClickImageEnhancement = {},
                autoScrollActive = true,
                onClickAutoScroll = {},
                onClickSettings = {},
            )
        }
    }
}

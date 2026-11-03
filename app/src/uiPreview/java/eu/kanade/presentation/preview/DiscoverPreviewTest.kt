package eu.kanade.presentation.preview

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.annotation.ExperimentalCoilApi
import coil3.asImage
import coil3.compose.AsyncImagePreviewHandler
import coil3.compose.LocalAsyncImagePreviewHandler
import com.github.takahirom.roborazzi.captureRoboImage
import eu.kanade.presentation.browse.discover.DiscoverSampleData
import eu.kanade.presentation.browse.discover.DiscoverScreen
import eu.kanade.presentation.browse.discover.DiscoverSection
import eu.kanade.presentation.browse.discover.DiscoverSectionContent
import eu.kanade.presentation.browse.discover.DiscoverState
import eu.kanade.presentation.components.TabContent
import eu.kanade.presentation.components.TabbedScreen
import eu.kanade.presentation.theme.TachiyomiShapes
import eu.kanade.presentation.theme.TachiyomiTypography
import eu.kanade.presentation.theme.colorscheme.WarmCharcoalColorScheme
import mihon.icons.phosphor.Phosphor
import mihon.icons.phosphor.bold.Bell
import mihon.icons.phosphor.bold.Books
import mihon.icons.phosphor.bold.ClockCounterClockwise
import mihon.icons.phosphor.bold.DotsThree
import mihon.icons.phosphor.fill.Compass
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.ConscryptMode
import org.robolectric.annotation.GraphicsMode
import tachiyomi.i18n.MR

/**
 * Browse > Discover with sample picks, in every state the screen has: filled, loading (skeleton),
 * empty, offline with cached picks, offline with nothing cached. Warm Charcoal, Browse selected in the rail.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@ConscryptMode(ConscryptMode.Mode.OFF)
@Config(sdk = [35], application = android.app.Application::class, qualifiers = "w781dp-h1172dp-xhdpi")
open class DiscoverPreviewTest {
    @get:Rule
    val composeRule = createComposeRule()

    protected open val suffix: String = "-portrait"

    private val sample = DiscoverSampleData.state.let { state ->
        // Covers come from the preview image handler below
        state.copy(
            sections = state.sections.mapValues { (_, entries) ->
                entries.map { it.copy(coverUrl = "preview://${it.id}") }
            },
            updatedAtMillis = System.currentTimeMillis() - 2 * 60 * 60 * 1000L,
        )
    }

    @Test
    fun filled() = capture("discover-filled") { Browse(sample) }

    @Test
    fun loading() = capture("discover-loading") {
        Browse(DiscoverState(sections = emptyMap(), loading = true, updatedAtMillis = null, error = null))
    }

    @Test
    fun empty() = capture("discover-empty") {
        Browse(DiscoverState(sections = emptyMap(), loading = false, updatedAtMillis = null, error = null))
    }

    @Test
    fun offlineCached() = capture("discover-offline-cached") { Browse(sample.copy(error = "timeout")) }

    @Test
    fun offlineEmpty() = capture("discover-offline-empty") {
        Browse(DiscoverState(sections = emptyMap(), loading = false, updatedAtMillis = null, error = "timeout"))
    }

    /** "See all" screen: pushed over Home, so no rail, just the list of one row. */
    @Test
    fun seeAll() = capture("discover-see-all") {
        DiscoverSectionContent(
            section = DiscoverSection.COMPLETED,
            entries = sample.sections[DiscoverSection.COMPLETED].orEmpty(),
            onBack = {},
            onOpen = {},
        )
    }

    @Test
    fun seeAllEmpty() = capture("discover-see-all-empty") {
        DiscoverSectionContent(section = DiscoverSection.HIDDEN_GEMS, entries = emptyList(), onBack = {}, onOpen = {})
    }

    @OptIn(ExperimentalCoilApi::class)
    private fun capture(name: String, content: @Composable () -> Unit) {
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
                CompositionLocalProvider(
                    LocalTextStyle provides MaterialTheme.typography.bodySmall,
                    LocalContentColor provides MaterialTheme.colorScheme.onBackground,
                    LocalInspectionMode provides true,
                    LocalAsyncImagePreviewHandler provides AsyncImagePreviewHandler { request ->
                        val id = (request.data as? String)?.removePrefix("preview://")?.toLongOrNull() ?: 0L
                        discoverCover(id).asImage()
                    },
                ) {
                    content()
                }
            }
        }
        composeRule.onRoot().captureRoboImage(System.getProperty("preview.output.dir") + "/" + name + suffix + ".png")
    }
}

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@ConscryptMode(ConscryptMode.Mode.OFF)
@Config(sdk = [35], application = android.app.Application::class, qualifiers = "w1172dp-h781dp-xhdpi")
class DiscoverLandscapePreviewTest : DiscoverPreviewTest() {
    override val suffix: String = "-landscape"
}

private val CoverGradients = listOf(
    0xFFE9B48A to 0xFF8A3B4A,
    0xFF7FB6E6 to 0xFF283D73,
    0xFFF3E2C2 to 0xFFB0644A,
    0xFF6C4D9C to 0xFF191433,
    0xFFDDE7EC to 0xFF5E7C8C,
    0xFFE7738A to 0xFF3B1B3A,
    0xFF9BC59D to 0xFF2B4A3A,
)

private fun discoverCover(id: Long): Bitmap {
    val (top, bottom) = CoverGradients[(id % CoverGradients.size).toInt()]
    val bitmap = Bitmap.createBitmap(200, 300, Bitmap.Config.ARGB_8888)
    val paint = Paint().apply {
        shader = LinearGradient(0f, 0f, 200f, 300f, top.toInt(), bottom.toInt(), Shader.TileMode.CLAMP)
    }
    Canvas(bitmap).drawRect(0f, 0f, 200f, 300f, paint)
    return bitmap
}

/** Browse screen as the app builds it: rail on the left, real TabbedScreen, Discover selected. */
@Composable
private fun Browse(state: DiscoverState) {
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Row(Modifier.fillMaxSize()) {
            Rail()
            Box(Modifier.weight(1f)) {
                TabbedScreen(
                    titleRes = MR.strings.browse,
                    tabs = listOf(
                        TabContent(titleRes = MR.strings.label_sources) { _, _ -> },
                        TabContent(titleRes = MR.strings.label_discover) { contentPadding, _ ->
                            DiscoverScreen(state = state, contentPadding = contentPadding, onOpen = {}, onRefresh = {})
                        },
                        TabContent(titleRes = MR.strings.label_extensions) { _, _ -> },
                        TabContent(titleRes = MR.strings.label_migration) { _, _ -> },
                    ),
                    // Discover is the second tab, after Sources
                    state = rememberPagerState(initialPage = 1) { 4 },
                )
            }
        }
    }
}

/** Same shape as HomeScreen.SlimNavigationRail, Browse selected. */
@Composable
private fun Rail() {
    val colors = MaterialTheme.colorScheme
    val items: List<Pair<String, ImageVector>> = listOf(
        "Library" to Phosphor.Bold.Books,
        "Updates" to Phosphor.Bold.Bell,
        "History" to Phosphor.Bold.ClockCounterClockwise,
        "Browse" to Phosphor.Fill.Compass,
        "More" to Phosphor.Bold.DotsThree,
    )
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(68.dp)
            .background(colors.surface)
            .padding(top = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        items.forEachIndexed { i, (label, icon) ->
            val selected = i == 3
            Column(Modifier.width(68.dp).heightIn(min = 56.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier
                        .size(width = 52.dp, height = 36.dp)
                        .clip(CircleShape)
                        .background(if (selected) colors.secondaryContainer else Color.Transparent),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        icon,
                        contentDescription = label,
                        tint = if (selected) colors.onSecondaryContainer else colors.onSurfaceVariant,
                        modifier = Modifier.size(28.dp),
                    )
                }
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                    color = if (selected) colors.onSurface else colors.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

package eu.kanade.presentation.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import coil3.annotation.ExperimentalCoilApi
import coil3.asImage
import coil3.compose.AsyncImagePreviewHandler
import coil3.compose.LocalAsyncImagePreviewHandler
import com.github.takahirom.roborazzi.captureRoboImage
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.library.components.DownloadsBadge
import eu.kanade.presentation.library.components.LibraryTabs
import eu.kanade.presentation.library.components.MangaListItem
import eu.kanade.presentation.library.components.UnreadBadge
import eu.kanade.presentation.theme.TachiyomiShapes
import eu.kanade.presentation.theme.TachiyomiTypography
import eu.kanade.presentation.theme.colorscheme.WarmCharcoalColorScheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.ConscryptMode
import org.robolectric.annotation.GraphicsMode
import tachiyomi.domain.manga.model.MangaCover as MangaCoverModel

/**
 * Three rail directions for Peter to choose from, each with its own icon family and rail shape:
 * A = Phosphor bold/fill on a slim rail, label only under the selected item.
 * B = Fluent regular/filled in a floating rounded rail with a filled circle on the selected item.
 * C = Tabler strokes on a classic rail, heavier stroke and a solid accent pill when selected.
 * The library content is the current code (LibraryTabs, MangaListItem); the top bar uses each family's icons.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@ConscryptMode(ConscryptMode.Mode.OFF)
@Config(sdk = [35], application = android.app.Application::class, qualifiers = "w781dp-h1172dp-xhdpi")
open class RailStylesPreviewTest {
    @get:Rule
    val composeRule = createComposeRule()

    protected open val suffix: String = "-portrait"

    @Test
    fun sideBySide() = capture("rail-styles") { SideBySide() }

    @Test
    fun styleA() = capture("rail-style-a") { StyledLibrary(RailStyle.A) }

    @Test
    fun styleB() = capture("rail-style-b") { StyledLibrary(RailStyle.B) }

    @Test
    fun styleC() = capture("rail-style-c") { StyledLibrary(RailStyle.C) }

    @Test
    fun closeUp() = capture("rail-icons-closeup") { CloseUp() }

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
                        gradientCover((request.data as? MangaCoverModel)?.mangaId ?: 0L).asImage()
                    },
                ) {
                    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { content() }
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
class RailStylesLandscapePreviewTest : RailStylesPreviewTest() {
    override val suffix: String = "-landscape"
}

internal enum class RailStyle(val title: String) {
    A("A. Phosphor, slim rail"),
    B("B. Fluent, floating rail"),
    C("C. Tabler, bold classic rail"),
}

private class RailEntryIcons(val label: String, val icons: IconPair)

private fun RailStyle.railItems(): List<RailEntryIcons> = when (this) {
    RailStyle.A -> with(PhosphorIcons) { listOf(Library, Updates, History, Browse, More) }
    RailStyle.B -> with(FluentIcons) { listOf(Library, Updates, History, Browse, More) }
    RailStyle.C -> with(TablerIcons) { listOf(Library, Updates, History, Browse, More) }
}.zip(listOf("Library", "Updates", "History", "Browse", "More")) { pair, label -> RailEntryIcons(label, pair) }

private fun RailStyle.topBarIcons(): List<IconPair> = when (this) {
    RailStyle.A -> with(PhosphorIcons) { listOf(Search, Filter, Overflow) }
    RailStyle.B -> with(FluentIcons) { listOf(Search, Filter, Overflow) }
    RailStyle.C -> with(TablerIcons) { listOf(Search, Filter, Overflow) }
}

@Composable
private fun SideBySide() {
    BoxWithConstraints(Modifier.fillMaxSize().background(Color(0xFF3A3A3A))) {
        val landscape = maxWidth > maxHeight
        if (landscape) {
            Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                RailStyle.entries.forEach { Box(Modifier.weight(1f).fillMaxHeight()) { Labelled(it) } }
            }
        } else {
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                RailStyle.entries.forEach { Box(Modifier.weight(1f).fillMaxWidth()) { Labelled(it) } }
            }
        }
    }
}

@Composable
private fun Labelled(style: RailStyle) {
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column {
            Text(
                style.title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            )
            Box(Modifier.weight(1f)) { StyledLibrary(style) }
        }
    }
}

@Composable
internal fun StyledLibrary(style: RailStyle) {
    Row(Modifier.fillMaxSize()) {
        Rail(style, Modifier.fillMaxHeight())
        Column(Modifier.weight(1f)) {
            AppBar(
                title = "Library",
                actions = {
                    style.topBarIcons().forEach { pair ->
                        IconButton(onClick = {}) { Icon(pair.normal, contentDescription = null) }
                    }
                },
            )
            LibraryTabs(
                categories = SampleCategories,
                pagerState = rememberPagerState { SampleCategories.size },
                getItemCountForCategory = { if (it.id == 0L) 41 else 12 },
                onTabItemClick = {},
            )
            LazyVerticalGrid(
                columns = GridCells.Adaptive(480.dp),
                contentPadding = PaddingValues(vertical = 8.dp),
            ) {
                items(SampleRows.withIndex().toList()) { (i, row) ->
                    val (title, unread, downloads) = row
                    MangaListItem(
                        coverData = cover(i.toLong()),
                        title = title,
                        onClick = {},
                        onLongClick = {},
                        badge = {
                            DownloadsBadge(count = downloads)
                            UnreadBadge(count = unread)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun Rail(style: RailStyle, modifier: Modifier = Modifier, selectedIndex: Int = 0) {
    val entries = style.railItems()
    when (style) {
        RailStyle.A -> Column(
            modifier.width(68.dp).padding(top = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            entries.forEachIndexed { i, e -> SlimItem(e, i == selectedIndex) }
        }
        RailStyle.B -> Box(modifier.padding(start = 12.dp, top = 12.dp, bottom = 12.dp)) {
            Column(
                Modifier
                    .width(72.dp)
                    .clip(RoundedCornerShape(36.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                entries.forEachIndexed { i, e -> FloatingItem(e, i == selectedIndex) }
            }
        }
        RailStyle.C -> Column(
            modifier.width(88.dp).padding(top = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            entries.forEachIndexed { i, e -> BoldItem(e, i == selectedIndex) }
        }
    }
}

/** A: big icon, no label unless selected; selected = filled glyph on a tonal pill. */
@Composable
private fun SlimItem(e: RailEntryIcons, selected: Boolean) {
    val colors = MaterialTheme.colorScheme
    Column(Modifier.width(68.dp).height(64.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(width = 52.dp, height = 36.dp)
                .clip(CircleShape)
                .background(if (selected) colors.secondaryContainer else Color.Transparent),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (selected) e.icons.selected else e.icons.normal,
                contentDescription = e.label,
                tint = if (selected) colors.onSecondaryContainer else colors.onSurfaceVariant,
                modifier = Modifier.size(28.dp),
            )
        }
        if (selected) {
            Text(
                e.label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = colors.onSurface,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

/** B: icon above a small label inside a floating rail; selected = filled glyph in a solid accent circle. */
@Composable
private fun FloatingItem(e: RailEntryIcons, selected: Boolean) {
    val colors = MaterialTheme.colorScheme
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(if (selected) colors.primary else Color.Transparent),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (selected) e.icons.selected else e.icons.normal,
                contentDescription = e.label,
                tint = if (selected) colors.onPrimary else colors.onSurfaceVariant,
                modifier = Modifier.size(24.dp),
            )
        }
        Text(
            e.label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = if (selected) colors.onSurface else colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

/** C: classic rail with labels always on; selected = heavier stroke on a solid accent pill. */
@Composable
private fun BoldItem(e: RailEntryIcons, selected: Boolean) {
    val colors = MaterialTheme.colorScheme
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(width = 64.dp, height = 36.dp)
                .clip(CircleShape)
                .background(if (selected) colors.primary else Color.Transparent),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (selected) e.icons.selected else e.icons.normal,
                contentDescription = e.label,
                tint = if (selected) colors.onPrimary else colors.onSurfaceVariant,
                modifier = Modifier.size(26.dp),
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            e.label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) colors.onSurface else colors.onSurfaceVariant,
        )
    }
}

/** Five rail items per style, unselected then selected, drawn at twice the size. */
@Composable
private fun CloseUp() {
    val density = LocalDensity.current
    CompositionLocalProvider(LocalDensity provides Density(density.density * 1.6f, density.fontScale)) {
        Column(
            Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            RailStyle.entries.forEach { style ->
                Text(style.title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                listOf(false, true).forEach { selected ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
                        style.railItems().forEach { e ->
                            when (style) {
                                RailStyle.A -> SlimItem(e, selected)
                                RailStyle.B -> FloatingItem(e, selected)
                                RailStyle.C -> BoldItem(e, selected)
                            }
                        }
                    }
                }
            }
        }
    }
}

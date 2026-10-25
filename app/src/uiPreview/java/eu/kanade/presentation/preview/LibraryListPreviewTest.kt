package eu.kanade.presentation.preview

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import androidx.compose.animation.graphics.ExperimentalAnimationGraphicsApi
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.annotation.ExperimentalCoilApi
import coil3.asImage
import coil3.compose.AsyncImagePreviewHandler
import coil3.compose.LocalAsyncImagePreviewHandler
import com.github.takahirom.roborazzi.captureRoboImage
import eu.kanade.presentation.category.visualName
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.components.AppBarActions
import eu.kanade.presentation.library.components.DownloadsBadge
import eu.kanade.presentation.library.components.LibraryTabs
import eu.kanade.presentation.library.components.MangaListItem
import eu.kanade.presentation.library.components.UnreadBadge
import eu.kanade.presentation.manga.components.MangaCover
import eu.kanade.presentation.theme.TachiyomiShapes
import eu.kanade.presentation.theme.TachiyomiTypography
import eu.kanade.presentation.theme.colorscheme.WarmCharcoalColorScheme
import eu.kanade.tachiyomi.R
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.CollectionsBookmark
import mihon.icons.materialsymbols.rounded.Explore
import mihon.icons.materialsymbols.rounded.FilterList
import mihon.icons.materialsymbols.rounded.History
import mihon.icons.materialsymbols.rounded.MoreHoriz
import mihon.icons.materialsymbols.rounded.NewReleases
import mihon.icons.materialsymbols.rounded.Search
import mihon.icons.materialsymbols.roundedfilled.CollectionsBookmark
import mihon.icons.materialsymbols.roundedfilled.Explore
import mihon.icons.materialsymbols.roundedfilled.NewReleases
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.ConscryptMode
import org.robolectric.annotation.GraphicsMode
import tachiyomi.domain.category.model.Category
import tachiyomi.presentation.core.components.BadgeGroup
import tachiyomi.presentation.core.components.material.TabText
import tachiyomi.presentation.core.util.selectedBackground
import tachiyomi.domain.manga.model.MangaCover as MangaCoverModel

/**
 * Library in list mode, before (old structure) and after (current code), both in Warm Charcoal so only the
 * structure differs. "After" uses the real LibraryTabs, MangaListItem and badges; the rail is rebuilt with the
 * same icons and label style as HomeScreen because HomeScreen needs Voyager. Covers are sample gradients.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@ConscryptMode(ConscryptMode.Mode.OFF)
@Config(sdk = [35], application = android.app.Application::class, qualifiers = "w781dp-h1172dp-xhdpi")
open class LibraryListPreviewTest {
    @get:Rule
    val composeRule = createComposeRule()

    protected open val suffix: String = "-portrait"

    @Test
    fun before() = capture("library-list-before") { LibraryScreen(after = false) }

    @Test
    fun after() = capture("library-list-after") { LibraryScreen(after = true) }

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
                    // Coil serves preview images synchronously only in inspection mode
                    LocalInspectionMode provides true,
                    LocalAsyncImagePreviewHandler provides AsyncImagePreviewHandler { request ->
                        val id = (request.data as? MangaCoverModel)?.mangaId ?: 0L
                        gradientCover(id).asImage()
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
class LibraryListLandscapePreviewTest : LibraryListPreviewTest() {
    override val suffix: String = "-landscape"
}

/** Sample titles (public series names) with unread and downloaded counts. */
internal val SampleRows = listOf(
    Triple("The Max Level Hero has Returned!", 31L, 0),
    Triple("The Hero Cannot Rest", 0L, 4),
    Triple("Swordmaster's Youngest Son", 141L, 12),
    Triple("Surviving The Game as a Barbarian", 0L, 0),
    Triple("Sandmancer of the Scorched Desert", 1L, 1),
    Triple("Return of the Mount Hua Sect", 0L, 0),
    Triple("Reincarnation of the Fist King", 1L, 0),
    Triple("Regressor of the Fallen family", 2L, 2),
    Triple("Record Of Ragnarok", 0L, 0),
    Triple("Pick Me Up!", 1L, 0),
    Triple("Myst, Might, Mayhem", 1L, 1),
    Triple("Murim Psychopath", 9L, 0),
    Triple("Limitless Predation", 7L, 3),
    Triple("Hellogin", 0L, 0),
    Triple("God Of Blackfield", 134L, 0),
)

internal val SampleGradients = listOf(
    0xFFE9B48A to 0xFF8A3B4A,
    0xFF7FB6E6 to 0xFF283D73,
    0xFFF3E2C2 to 0xFFB0644A,
    0xFF6C4D9C to 0xFF191433,
    0xFFDDE7EC to 0xFF5E7C8C,
    0xFFE7738A to 0xFF3B1B3A,
)

internal fun gradientCover(id: Long): Bitmap {
    val (top, bottom) = SampleGradients[(id % SampleGradients.size).toInt()]
    val bitmap = Bitmap.createBitmap(200, 300, Bitmap.Config.ARGB_8888)
    val paint = Paint().apply {
        shader = LinearGradient(0f, 0f, 200f, 300f, top.toInt(), bottom.toInt(), Shader.TileMode.CLAMP)
    }
    Canvas(bitmap).drawRect(0f, 0f, 200f, 300f, paint)
    return bitmap
}

internal fun cover(id: Long) = MangaCoverModel(
    mangaId = id,
    sourceId = 0L,
    isMangaFavorite = true,
    url = "preview://$id",
    lastModified = null,
)

internal val SampleCategories = listOf(
    Category(id = 0L, name = "", order = 0L, flags = 0L),
    Category(id = 1L, name = "FOLLOWING", order = 1L, flags = 0L),
    Category(id = 2L, name = "Completed", order = 2L, flags = 0L),
)

@Composable
private fun LibraryScreen(after: Boolean) {
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Row(Modifier.fillMaxSize()) {
            if (after) RailAfter() else RailBefore()
            Column(Modifier.weight(1f)) {
                AppBar(
                    title = "Library",
                    actions = {
                        AppBarActions(
                            listOf(
                                AppBar.Action(title = "Search", icon = MaterialSymbols.Rounded.Search, onClick = {}),
                                AppBar.Action(title = "Filter", icon = MaterialSymbols.Rounded.FilterList, onClick = {
                                }),
                                AppBar.OverflowAction(title = "Update library", onClick = {}),
                            ),
                        )
                    },
                )
                if (after) {
                    LibraryTabs(
                        categories = SampleCategories,
                        pagerState = rememberPagerState { SampleCategories.size },
                        getItemCountForCategory = { if (it.id == 0L) 41 else 12 },
                        onTabItemClick = {},
                    )
                } else {
                    TabsBefore()
                }
                // Same column rule as LibraryList: adaptive 480 dp (one column portrait, two landscape)
                LazyVerticalGrid(
                    columns = if (after) GridCells.Adaptive(480.dp) else GridCells.Fixed(1),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 8.dp),
                ) {
                    items(SampleRows.withIndex().toList()) { (i, row) ->
                        val (title, unread, downloads) = row
                        if (after) {
                            MangaListItem(
                                coverData = cover(i.toLong()),
                                title = title,
                                onClick = {},
                                onLongClick = {},
                                isSelected = i == 2,
                                badge = {
                                    DownloadsBadge(count = downloads)
                                    UnreadBadge(count = unread)
                                },
                            )
                        } else {
                            RowBefore(i, title, unread, downloads)
                        }
                    }
                }
            }
        }
    }
}

private data class RailEntry(val label: String, val outline: Painter, val filled: Painter)

@Composable
private fun RailAfter() {
    val entries = listOf(
        RailEntry(
            "Library",
            rememberVectorPainter(MaterialSymbols.Rounded.CollectionsBookmark),
            rememberVectorPainter(MaterialSymbols.RoundedFilled.CollectionsBookmark),
        ),
        RailEntry(
            "Updates",
            rememberVectorPainter(MaterialSymbols.Rounded.NewReleases),
            rememberVectorPainter(MaterialSymbols.RoundedFilled.NewReleases),
        ),
        RailEntry(
            "History",
            rememberVectorPainter(MaterialSymbols.Rounded.History),
            rememberVectorPainter(MaterialSymbols.Rounded.History),
        ),
        RailEntry(
            "Browse",
            rememberVectorPainter(MaterialSymbols.Rounded.Explore),
            rememberVectorPainter(MaterialSymbols.RoundedFilled.Explore),
        ),
        RailEntry(
            "More",
            rememberVectorPainter(MaterialSymbols.Rounded.MoreHoriz),
            rememberVectorPainter(MaterialSymbols.Rounded.MoreHoriz),
        ),
    )
    NavigationRail(containerColor = MaterialTheme.colorScheme.surface) {
        Spacer(Modifier.height(8.dp))
        entries.forEachIndexed { i, entry ->
            val selected = i == 0
            NavigationRailItem(
                selected = selected,
                onClick = {},
                icon = { Icon(if (selected) entry.filled else entry.outline, contentDescription = entry.label) },
                label = {
                    Text(
                        entry.label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                    )
                },
            )
        }
    }
}

/** Old rail: animated vector icons, tonal-elevation container, items centred vertically. */
@OptIn(ExperimentalAnimationGraphicsApi::class)
@Composable
private fun RailBefore() {
    val entries = listOf(
        "Library" to R.drawable.animated_library,
        "Updates" to R.drawable.animated_updates,
        "History" to R.drawable.animated_history,
        "Browse" to R.drawable.animated_browse_enter,
        "More" to R.drawable.animated_more,
    )
    NavigationRail(
        modifier = Modifier.fillMaxHeight(),
        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
    ) {
        Spacer(Modifier.weight(1f))
        entries.forEachIndexed { i, (label, res) ->
            val image = AnimatedImageVector.animatedVectorResource(res)
            NavigationRailItem(
                selected = i == 0,
                onClick = {},
                icon = { Icon(rememberAnimatedVectorPainter(image, i == 0), contentDescription = label) },
                label = { Text(label, style = MaterialTheme.typography.labelLarge) },
            )
        }
        Spacer(Modifier.weight(1f))
    }
}

/** Old tab strip: PrimaryScrollableTabRow with underline indicator and divider. */
@Composable
private fun TabsBefore() {
    PrimaryScrollableTabRow(selectedTabIndex = 0, edgePadding = 0.dp) {
        SampleCategories.forEachIndexed { index, category ->
            Tab(
                selected = index == 0,
                onClick = {},
                text = { TabText(text = category.visualName) },
                unselectedContentColor = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

/** Old list row: 56 dp, square cover crop, bodyMedium title. */
@Composable
private fun RowBefore(i: Int, title: String, unread: Long, downloads: Int) {
    Row(
        modifier = Modifier
            .selectedBackground(i == 2)
            .height(56.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MangaCover.Square(modifier = Modifier.fillMaxHeight(), data = cover(i.toLong()))
        Text(
            text = title,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .weight(1f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyMedium,
        )
        BadgeGroup {
            DownloadsBadge(count = downloads)
            UnreadBadge(count = unread)
        }
    }
}

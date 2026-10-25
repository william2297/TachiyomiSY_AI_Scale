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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
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
import eu.kanade.presentation.more.MoreScreen
import eu.kanade.presentation.theme.TachiyomiShapes
import eu.kanade.presentation.theme.TachiyomiTypography
import eu.kanade.presentation.theme.colorscheme.WarmCharcoalColorScheme
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.more.DownloadQueueState
import mihon.icons.phosphor.Phosphor
import mihon.icons.phosphor.bold.ArrowClockwise
import mihon.icons.phosphor.bold.ArrowsClockwise
import mihon.icons.phosphor.bold.Bell
import mihon.icons.phosphor.bold.BookOpen
import mihon.icons.phosphor.bold.BookmarkSimple
import mihon.icons.phosphor.bold.Books
import mihon.icons.phosphor.bold.CalendarBlank
import mihon.icons.phosphor.bold.ChartBar
import mihon.icons.phosphor.bold.ClockCounterClockwise
import mihon.icons.phosphor.bold.CloudSlash
import mihon.icons.phosphor.bold.Code
import mihon.icons.phosphor.bold.Compass
import mihon.icons.phosphor.bold.Database
import mihon.icons.phosphor.bold.DotsThree
import mihon.icons.phosphor.bold.DotsThreeVertical
import mihon.icons.phosphor.bold.DownloadSimple
import mihon.icons.phosphor.bold.Eyeglasses
import mihon.icons.phosphor.bold.FunnelSimple
import mihon.icons.phosphor.bold.GearSix
import mihon.icons.phosphor.bold.GlobeSimple
import mihon.icons.phosphor.bold.HandHeart
import mihon.icons.phosphor.bold.HardDrives
import mihon.icons.phosphor.bold.Image
import mihon.icons.phosphor.bold.Info
import mihon.icons.phosphor.bold.MagnifyingGlass
import mihon.icons.phosphor.bold.Palette
import mihon.icons.phosphor.bold.Pause
import mihon.icons.phosphor.bold.Play
import mihon.icons.phosphor.bold.Question
import mihon.icons.phosphor.bold.SelectionAll
import mihon.icons.phosphor.bold.SelectionInverse
import mihon.icons.phosphor.bold.ShieldCheck
import mihon.icons.phosphor.bold.SkipBack
import mihon.icons.phosphor.bold.SkipForward
import mihon.icons.phosphor.bold.Tag
import mihon.icons.phosphor.bold.Trash
import mihon.icons.phosphor.bold.X
import mihon.icons.phosphor.boldautomirrored.ArrowLeft
import mihon.icons.phosphor.fill.Bell
import mihon.icons.phosphor.fill.BookmarkSimple
import mihon.icons.phosphor.fill.Books
import mihon.icons.phosphor.fill.ClockCounterClockwise
import mihon.icons.phosphor.fill.Compass
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
    protected open val phone: Boolean = false

    @Test
    fun before() {
        if (phone) return // the phone check only needs the new bottom bar
        capture("library-list-before") { LibraryScreen(after = false) }
    }

    @Test
    fun after() = capture("library-list-after") { LibraryScreen(after = true, phone = phone) }

    @Test
    fun more() {
        if (phone) return
        capture("more-after") {
            Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                Row(Modifier.fillMaxSize()) {
                    RailAfter(selectedIndex = 4)
                    Box(Modifier.weight(1f)) {
                        MoreScreen(
                            downloadQueueStateProvider = { DownloadQueueState.Stopped },
                            downloadedOnly = false,
                            onDownloadedOnlyChange = {},
                            incognitoMode = true,
                            onIncognitoModeChange = {},
                            onClickDownloadQueue = {},
                            onClickCategories = {},
                            onClickStats = {},
                            onClickDataAndStorage = {},
                            onClickSettings = {},
                            onClickSupport = {},
                            onClickAbout = {},
                        )
                    }
                }
            }
        }
    }

    @Test
    fun iconSheet() {
        if (phone) return
        capture("phosphor-icons") { PhosphorSheet() }
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
private fun LibraryScreen(after: Boolean, phone: Boolean = false) {
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Row(Modifier.fillMaxSize()) {
            if (!phone) {
                if (after) RailAfter() else RailBefore()
            }
            Column(Modifier.weight(1f)) {
                AppBar(
                    title = "Library",
                    actions = {
                        AppBarActions(
                            listOf(
                                AppBar.Action(title = "Search", icon = Phosphor.Bold.MagnifyingGlass, onClick = {}),
                                AppBar.Action(title = "Filter", icon = Phosphor.Bold.FunnelSimple, onClick = {
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
                    modifier = Modifier.weight(1f),
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
                if (phone) PhoneBar()
            }
        }
    }
}

private fun phosphorEntries(): List<Triple<String, ImageVector, ImageVector>> = listOf(
    Triple("Library", Phosphor.Bold.Books, Phosphor.Fill.Books),
    Triple("Updates", Phosphor.Bold.Bell, Phosphor.Fill.Bell),
    Triple("History", Phosphor.Bold.ClockCounterClockwise, Phosphor.Fill.ClockCounterClockwise),
    Triple("Browse", Phosphor.Bold.Compass, Phosphor.Fill.Compass),
    Triple("More", Phosphor.Bold.DotsThree, Phosphor.Bold.DotsThree),
)

/** Mirrors HomeScreen.SlimNavigationRail: 68 dp, 28 dp icons, small label under every item. */
@Composable
private fun RailAfter(selectedIndex: Int = 0) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(68.dp)
            .background(colors.surface)
            .padding(top = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        phosphorEntries().forEachIndexed { i, (label, idle, active) ->
            val selected = i == selectedIndex
            Column(Modifier.width(68.dp).heightIn(min = 56.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier
                        .size(width = 52.dp, height = 36.dp)
                        .clip(CircleShape)
                        .background(if (selected) colors.secondaryContainer else Color.Transparent),
                    contentAlignment = Alignment.Center,
                ) {
                    BadgedBox(badge = { if (label == "Updates") Badge { Text("12") } }) {
                        Icon(
                            if (selected) active else idle,
                            contentDescription = label,
                            tint = if (selected) colors.onSecondaryContainer else colors.onSurfaceVariant,
                            modifier = Modifier.size(28.dp),
                        )
                    }
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

/** Phone: the stock M3 NavigationBar fed with the same Phosphor icons, as NavigationSuiteScaffold does. */
@Composable
private fun PhoneBar() {
    NavigationBar {
        phosphorEntries().forEachIndexed { i, (label, idle, active) ->
            NavigationBarItem(
                selected = i == 0,
                onClick = {},
                icon = { Icon(if (i == 0) active else idle, contentDescription = label) },
                label = { Text(label, style = MaterialTheme.typography.labelMedium) },
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

/** Phone width (about 411x891 dp) to check the bottom navigation with the Phosphor icons. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@ConscryptMode(ConscryptMode.Mode.OFF)
@Config(sdk = [35], application = android.app.Application::class, qualifiers = "w411dp-h891dp-xxhdpi")
class LibraryListPhonePreviewTest : LibraryListPreviewTest() {
    override val suffix: String = "-phone"
    override val phone: Boolean = true
}

/** Every Phosphor glyph now used on the main screens, with where it is used, at rail and toolbar size. */
@Composable
private fun PhosphorSheet() {
    val items = listOf(
        "Library tab" to Phosphor.Bold.Books, "Library tab, selected" to Phosphor.Fill.Books,
        "Updates tab" to Phosphor.Bold.Bell, "History tab" to Phosphor.Bold.ClockCounterClockwise,
        "Browse tab" to Phosphor.Bold.Compass, "More tab" to Phosphor.Bold.DotsThree,
        "Back" to Phosphor.BoldAutoMirrored.ArrowLeft, "Close" to Phosphor.Bold.X,
        "Search" to Phosphor.Bold.MagnifyingGlass, "Filter" to Phosphor.Bold.FunnelSimple,
        "Overflow" to Phosphor.Bold.DotsThreeVertical, "Select all" to Phosphor.Bold.SelectionAll,
        "Invert selection" to Phosphor.Bold.SelectionInverse, "Refresh" to Phosphor.Bold.ArrowClockwise,
        "Upcoming" to Phosphor.Bold.CalendarBlank, "Clear history" to Phosphor.Bold.Trash,
        "Global search" to Phosphor.Bold.GlobeSimple, "Help" to Phosphor.Bold.Question,
        "Categories" to Phosphor.Bold.Tag, "Downloaded only" to Phosphor.Bold.CloudSlash,
        "Downloads" to Phosphor.Bold.DownloadSimple, "Incognito" to Phosphor.Bold.Eyeglasses,
        "About" to Phosphor.Bold.Info, "Statistics" to Phosphor.Bold.ChartBar,
        "Settings" to Phosphor.Bold.GearSix, "Data and storage (More)" to Phosphor.Bold.HardDrives,
        "Support" to Phosphor.Bold.HandHeart, "Reader settings" to Phosphor.Bold.BookOpen,
        "Advanced" to Phosphor.Bold.Code, "Appearance" to Phosphor.Bold.Palette,
        "Security" to Phosphor.Bold.ShieldCheck, "Data and storage (Settings)" to Phosphor.Bold.Database,
        "Tracking" to Phosphor.Bold.ArrowsClockwise, "Bookmark" to Phosphor.Bold.BookmarkSimple,
        "Bookmarked" to Phosphor.Fill.BookmarkSimple, "Image enhancement" to Phosphor.Bold.Image,
        "Previous chapter" to Phosphor.Bold.SkipBack, "Next chapter" to Phosphor.Bold.SkipForward,
        "Auto scroll off" to Phosphor.Bold.Play, "Auto scroll on" to Phosphor.Bold.Pause,
    )
    LazyVerticalGrid(
        columns = GridCells.Adaptive(240.dp),
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
    ) {
        items(items) { (name, icon) ->
            Row(Modifier.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = name, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    name,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(start = 12.dp),
                )
            }
        }
    }
}

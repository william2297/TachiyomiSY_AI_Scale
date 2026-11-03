package eu.kanade.presentation.preview

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import coil3.annotation.ExperimentalCoilApi
import coil3.asImage
import coil3.compose.AsyncImagePreviewHandler
import coil3.compose.LocalAsyncImagePreviewHandler
import com.github.takahirom.roborazzi.captureRoboImage
import eu.kanade.presentation.browse.discover.DiscoverEntry
import eu.kanade.presentation.browse.discover.DiscoverRow
import eu.kanade.presentation.browse.discover.DiscoverScreen
import eu.kanade.presentation.browse.discover.DiscoverState
import eu.kanade.presentation.browse.discover.components.DiscoverGenrePickerContent
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

/**
 * The "My genres" slot at the end of Discover (invite, row, rebuilding, nothing matched) and the genre sheet,
 * dark and light. Only the rows built from taste are filled, so the slot is on screen without scrolling.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@ConscryptMode(ConscryptMode.Mode.OFF)
@Config(sdk = [35], application = android.app.Application::class, qualifiers = "w781dp-h1172dp-xhdpi")
open class DiscoverGenresPreviewTest {
    @get:Rule
    val composeRule = createComposeRule()

    protected open val suffix: String = "-portrait"
    protected open val dark: Boolean = true

    private val genres = listOf(
        "Action", "Adventure", "Comedy", "Drama", "Ecchi", "Fantasy", "Horror", "Mahou Shoujo", "Mystery",
        "Psychological", "Romance", "Sci-Fi", "Slice of Life", "Sports", "Supernatural", "Thriller",
    )

    private fun entry(id: Long, title: String, reason: String) = DiscoverEntry(
        id = id,
        title = title,
        coverUrl = "preview://$id",
        sourceId = 1L,
        sourceName = if (id % 2 == 0L) "Asura Scans" else "Comick",
        mangaUrl = "/series/$id",
        score = 76 + (id % 12).toInt(),
        completed = id % 3 == 0L,
        reason = reason,
    )

    private val becauseRow = DiscoverRow(
        key = "because-1",
        title = "Because you read Solo Leveling",
        hint = "Readers who liked it also rate these",
        entries = listOf("Eleceed", "Nano Machine", "Wind Breaker", "Overgeared", "Infinite Mage", "Dice", "Bastard")
            .mapIndexed { i, t -> entry(100L + i, t, "Liked by its readers") },
    )
    private val yourGenresRow = DiscoverRow(
        key = "your-genres",
        title = "Your genres",
        hint = "Action, Fantasy: any of them",
        entries = listOf("Tower of God", "The Breaker", "Noblesse", "Lookism", "The Gamer", "Hellbound")
            .mapIndexed { i, t -> entry(200L + i, t, "Matches Action") },
    )
    private val myGenresRow = DiscoverRow(
        key = "my-genres",
        title = "My genres",
        hint = "Mystery, Psychological, Romance: any of them",
        entries = listOf(
            "Pigpen", "Sweet Home", "Annarasumanara", "Distant Sky", "Love Revolution", "Light and Shadow",
            "The Horizon", "Shotgun Boy", "Dice", "Hellogin", "Pick Me Up!", "Murim Psychopath", "Bastard",
            "The Boxer",
        ).mapIndexed { i, t ->
            entry(
                300L + i,
                t,
                if (i % 2 ==
                    0
                ) {
                    "Matches Mystery, Psychological"
                } else {
                    "Matches Romance"
                },
            )
        },
    )

    private fun state(picked: Set<String>, rows: List<DiscoverRow>, loading: Boolean = false) = DiscoverState(
        sections = emptyMap(),
        loading = loading,
        updatedAtMillis = System.currentTimeMillis() - 3 * 60 * 60 * 1000L,
        error = null,
        extraRows = rows,
        pickedGenres = picked,
    )

    private val picked = setOf("Mystery", "Psychological", "Romance")
    private val manyPicked = setOf("Action", "Adventure", "Comedy", "Drama", "Fantasy", "Horror", "Mystery", "Romance")

    @Test
    fun invite() = both("genres-invite") { Screen(state(emptySet(), listOf(yourGenresRow))) }

    @Test
    fun row() = both("genres-row") { Screen(state(picked, listOf(yourGenresRow, myGenresRow))) }

    @Test
    fun rowMany() = both("genres-row-many") {
        Screen(state(manyPicked, listOf(yourGenresRow, myGenresRow), loading = true))
    }

    @Test
    fun rebuilding() = both("genres-rebuilding") {
        Screen(state(picked, listOf(yourGenresRow), loading = true))
    }

    @Test
    fun nothingMatched() = both("genres-nothing") { Screen(state(picked, listOf(yourGenresRow))) }

    @Test
    fun sheet() = both("genres-sheet") { SheetOver(state(picked, listOf(yourGenresRow)), picked) }

    @Test
    fun sheetEmpty() = both("genres-sheet-empty") { SheetOver(state(emptySet(), listOf(becauseRow)), emptySet()) }

    @Composable
    private fun Screen(state: DiscoverState) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            DiscoverScreen(
                state = state,
                contentPadding = PaddingValues(top = 8.dp),
                onOpen = {},
                onRefresh = {},
                genres = genres,
            )
        }
    }

    /** The sheet as ModalBottomSheet draws it (scrim, max width, top corners, drag handle), without a window. */
    @Composable
    private fun SheetOver(state: DiscoverState, initial: Set<String>) {
        Box(Modifier.fillMaxSize()) {
            Screen(state)
            Box(Modifier.fillMaxSize().background(BottomSheetDefaults.ScrimColor))
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .widthIn(max = 720.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                color = BottomSheetDefaults.ContainerColor,
                tonalElevation = BottomSheetDefaults.Elevation,
            ) {
                androidx.compose.foundation.layout.Column {
                    Box(Modifier.fillMaxWidth().padding(vertical = 4.dp), contentAlignment = Alignment.Center) {
                        BottomSheetDefaults.DragHandle()
                    }
                    DiscoverGenrePickerContent(genres = genres, initial = initial, onApply = {})
                }
            }
        }
    }

    private fun both(name: String, content: @Composable () -> Unit) {
        capture(name + if (dark) "-dark" else "-light", dark, content)
    }

    @OptIn(ExperimentalCoilApi::class)
    protected fun capture(name: String, dark: Boolean, content: @Composable () -> Unit) {
        composeRule.setContent {
            MaterialExpressiveTheme(
                colorScheme = WarmCharcoalColorScheme.getColorScheme(
                    isDark = dark,
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
                        genreCover(id).asImage()
                    },
                ) {
                    content()
                }
            }
        }
        composeRule.onRoot().captureRoboImage(System.getProperty("preview.output.dir") + "/" + name + suffix + ".png")
    }
}

/** Same screens, light theme. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@ConscryptMode(ConscryptMode.Mode.OFF)
@Config(sdk = [35], application = android.app.Application::class, qualifiers = "w781dp-h1172dp-xhdpi")
class DiscoverGenresLightPreviewTest : DiscoverGenresPreviewTest() {
    override val dark: Boolean = false
}

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@ConscryptMode(ConscryptMode.Mode.OFF)
@Config(sdk = [35], application = android.app.Application::class, qualifiers = "w1172dp-h781dp-xhdpi")
class DiscoverGenresLandscapePreviewTest : DiscoverGenresPreviewTest() {
    override val suffix: String = "-landscape"
}

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@ConscryptMode(ConscryptMode.Mode.OFF)
@Config(sdk = [35], application = android.app.Application::class, qualifiers = "w1172dp-h781dp-xhdpi")
class DiscoverGenresLandscapeLightPreviewTest : DiscoverGenresPreviewTest() {
    override val suffix: String = "-landscape"
    override val dark: Boolean = false
}

private val GenreCoverGradients = listOf(
    0xFF7FB6E6 to 0xFF283D73,
    0xFFE7738A to 0xFF3B1B3A,
    0xFF9BC59D to 0xFF2B4A3A,
    0xFFE9B48A to 0xFF8A3B4A,
    0xFF6C4D9C to 0xFF191433,
)

private fun genreCover(id: Long): Bitmap {
    val (top, bottom) = GenreCoverGradients[(id % GenreCoverGradients.size).toInt()]
    val bitmap = Bitmap.createBitmap(200, 300, Bitmap.Config.ARGB_8888)
    val paint = Paint().apply {
        shader = LinearGradient(0f, 0f, 200f, 300f, top.toInt(), bottom.toInt(), Shader.TileMode.CLAMP)
    }
    Canvas(bitmap).drawRect(0f, 0f, 200f, 300f, paint)
    return bitmap
}

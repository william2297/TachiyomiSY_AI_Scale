package eu.kanade.tachiyomi.ui.reader.viewer.pager

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapRegionDecoder
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.model.RebuiltPage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import logcat.LogPriority
import okio.Buffer
import tachiyomi.core.common.util.system.ImageUtil
import tachiyomi.core.common.util.system.logcat
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Rebuilds whole pages for the paged viewers when a source cuts a chapter into uneven slices.
 *
 * A chapter is analysed once all of its pages are downloaded. Runs of consecutive pages that share a
 * width and contain slices (much shorter or much taller than a page) are joined into one virtual
 * strip, which is cut again at blank rows into pages of about [TARGET_RATIO] x width. Pages outside
 * such runs are kept as they are. The long strip never uses this class.
 */
internal class PagedSliceRebuilder(
    context: Context,
    private val scope: CoroutineScope,
    private val onRebuilt: (ReaderChapter) -> Unit,
) {

    private val appContext = context.applicationContext
    private val cacheRoot = File(appContext.cacheDir, "paged_rebuild")
    private val renderLocks = ConcurrentHashMap<String, Any>()

    private class Result(val sourcePages: List<ReaderPage>, val pages: List<ReaderPage>?)
    private class Running(val sourcePages: List<ReaderPage>, val job: Job)

    // Main thread only.
    private val results = mutableMapOf<Long, Result>()
    private val running = mutableMapOf<Long, Running>()

    init {
        scope.launch(Dispatchers.IO) { pruneOldFiles() }
    }

    /** Pages to show for [chapter]: its rebuilt list when one is ready, otherwise its source pages. */
    fun displayPages(chapter: ReaderChapter): List<ReaderPage>? {
        val source = chapter.pages ?: return null
        val id = chapter.chapter.id ?: return source
        val result = results[id]?.takeIf { it.sourcePages === source } ?: return source
        return result.pages ?: source
    }

    fun isRebuilt(chapter: ReaderChapter): Boolean {
        val source = chapter.pages ?: return false
        val id = chapter.chapter.id ?: return false
        return results[id]?.takeIf { it.sourcePages === source }?.pages != null
    }

    /** Starts the analysis of [chapter] unless it is done or running for its current page list. */
    fun ensure(chapter: ReaderChapter) {
        val source = chapter.pages ?: return
        val id = chapter.chapter.id ?: return
        if (results[id]?.sourcePages === source) return
        val current = running[id]
        if (current != null && current.sourcePages === source && current.job.isActive) return
        current?.job?.cancel()

        val job = scope.launch {
            val outcome = try {
                withContext(Dispatchers.IO) { analyze(chapter, source) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                logcat(LogPriority.ERROR, e) { "PagedRebuild: analysis failed for ${chapter.chapter.id}" }
                Outcome.Done(null)
            }
            if (running[id]?.sourcePages === source) running.remove(id)
            if (outcome is Outcome.Done) {
                results[id] = Result(source, outcome.pages)
                if (outcome.pages != null) onRebuilt(chapter)
            }
        }
        running[id] = Running(source, job)
    }

    fun cancelAll() {
        running.values.forEach { it.job.cancel() }
        running.clear()
    }

    private sealed interface Outcome {
        /** [pages] is null when the chapter is kept as it is. */
        class Done(val pages: List<ReaderPage>?) : Outcome
        /** A page failed to load; try again on the next chapter update. */
        data object Retry : Outcome
    }

    private class Profile(
        val width: Int,
        val height: Int,
        /** Source pixels per profiled row. */
        val rowHeight: Float,
        /** Luminance range (max - min) of each profiled row. */
        val ranges: IntArray,
    )

    private class Cut(val position: Float, val strength: Float, val forced: Boolean)

    private class PageSpec(val sourceIndex: Int, val width: Int, val height: Int, val pieces: List<RebuiltPage.Piece>)

    private suspend fun analyze(chapter: ReaderChapter, pages: List<ReaderPage>): Outcome {
        if (pages.size < 2) return Outcome.Done(null)
        for (page in pages) {
            val state = page.statusFlow.first { it == Page.State.Ready || it is Page.State.Error }
            if (state is Page.State.Error) return Outcome.Retry
        }

        val sizes = pages.map { readSize(it) ?: return Outcome.Done(null) }
        val runs = findSlicedRuns(sizes)
        logcat(LogPriority.INFO) {
            "PagedRebuild: chapter ${chapter.chapter.id} sizes=" +
                sizes.joinToString(" ") { "${it.first}x${it.second}" } + " runs=$runs"
        }
        if (runs.isEmpty()) return Outcome.Done(null)

        val specsByRunStart = mutableMapOf<Int, List<PageSpec>>()
        val keyBuilder = StringBuilder("v1:${chapter.chapter.id}")
        for (run in runs) {
            val profiles = run.map { index ->
                profile(pages[index], sizes[index].first, sizes[index].second) ?: return Outcome.Done(null)
            }
            val specs = layoutRun(pages, run, profiles)
            specsByRunStart[run.first] = specs
            keyBuilder.append('|').append(run.first).append(':')
            specs.forEach { keyBuilder.append(it.height).append(',') }
        }
        sizes.forEach { keyBuilder.append(';').append(it.first).append('x').append(it.second) }
        val layoutKey = keyBuilder.toString().hashCode().toUInt().toString(16)

        val display = mutableListOf<ReaderPage>()
        val rebuilt = mutableListOf<RebuiltPage>()
        var index = 0
        while (index < pages.size) {
            val run = runs.firstOrNull { it.first == index }
            if (run == null) {
                display += pages[index]
                index++
                continue
            }
            specsByRunStart.getValue(run.first).forEach { spec ->
                val page = RebuiltPage(
                    index = spec.sourceIndex,
                    displayIndex = display.size,
                    layoutKey = layoutKey,
                    width = spec.width,
                    height = spec.height,
                    pieces = spec.pieces,
                    sourceChapter = chapter,
                    opener = ::openRendered,
                )
                display += page
                rebuilt += page
            }
            index = run.last + 1
        }
        rebuilt.forEach { it.chapterPages = display }
        logcat(LogPriority.INFO) {
            "PagedRebuild: chapter ${chapter.chapter.id} ${pages.size} source pages -> ${display.size} pages " +
                "(${rebuilt.size} rebuilt, key=$layoutKey)"
        }
        return Outcome.Done(display)
    }

    private fun readSize(page: ReaderPage): Pair<Int, Int>? {
        return try {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            val stream = page.stream?.invoke() ?: return null
            stream.use { BitmapFactory.decodeStream(it, null, options) }
            if (options.outWidth > 0 && options.outHeight > 0) options.outWidth to options.outHeight else null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logcat(LogPriority.WARN, e) { "PagedRebuild: unable to read size of page ${page.index}" }
            null
        }
    }

    /** Runs of same-width pages that hold enough slices to be rebuilt. */
    private fun findSlicedRuns(sizes: List<Pair<Int, Int>>): List<IntRange> {
        val runs = mutableListOf<IntRange>()
        var start = 0
        for (index in 1..sizes.size) {
            val ends = index == sizes.size || !similarWidth(sizes[start].first, sizes[index].first)
            if (ends) {
                runs += start until index
                start = index
            }
        }
        return runs.filter { run ->
            val count = run.count()
            val slices = run.count { index ->
                val ratio = sizes[index].second.toFloat() / sizes[index].first
                ratio < SLICE_RATIO_MIN || ratio > SLICE_RATIO_MAX
            }
            count >= 2 && slices >= 2 && slices * 5 >= count
        }
    }

    private fun similarWidth(first: Int, second: Int): Boolean {
        return abs(first - second) <= max(first, second) * WIDTH_TOLERANCE
    }

    private fun profile(page: ReaderPage, width: Int, height: Int): Profile? {
        var sample = 1
        while (width / (sample * 2) >= PROFILE_MIN_WIDTH) sample *= 2
        val options = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val bitmap = try {
            val stream = page.stream?.invoke() ?: return null
            stream.use { BitmapFactory.decodeStream(it, null, options) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logcat(LogPriority.WARN, e) { "PagedRebuild: unable to profile page ${page.index}" }
            null
        } ?: return null

        try {
            val bitmapWidth = bitmap.width
            val bitmapHeight = bitmap.height
            if (bitmapWidth <= 0 || bitmapHeight <= 0) return null
            // Scans and JPEG edges often leave a thin dark border; ignore the outer columns.
            val margin = (bitmapWidth * 0.03f).toInt()
            val row = IntArray(bitmapWidth)
            val ranges = IntArray(bitmapHeight)
            for (y in 0 until bitmapHeight) {
                bitmap.getPixels(row, 0, bitmapWidth, 0, y, bitmapWidth, 1)
                var low = 255
                var high = 0
                for (x in margin until bitmapWidth - margin) {
                    val color = row[x]
                    val luma = (
                        299 * ((color shr 16) and 0xFF) +
                            587 * ((color shr 8) and 0xFF) +
                            114 * (color and 0xFF)
                        ) / 1000
                    if (luma < low) low = luma
                    if (luma > high) high = luma
                }
                ranges[y] = if (high >= low) high - low else 0
            }
            return Profile(width, height, height.toFloat() / bitmapHeight, ranges)
        } finally {
            bitmap.recycle()
        }
    }

    /** Cuts one run into pages. Coordinates are in pixels of the run's first page width. */
    private fun layoutRun(pages: List<ReaderPage>, run: IntRange, profiles: List<Profile>): List<PageSpec> {
        val width = profiles.first().width
        val target = width * TARGET_RATIO
        val scales = FloatArray(profiles.size) { width.toFloat() / profiles[it].width }
        val sliceTops = FloatArray(profiles.size + 1)
        profiles.forEachIndexed { i, profile -> sliceTops[i + 1] = sliceTops[i] + profile.height * scales[i] }
        val total = sliceTops.last()

        val rowCount = profiles.sumOf { it.ranges.size }
        val rowTops = FloatArray(rowCount)
        val rowBottoms = FloatArray(rowCount)
        val rowRanges = IntArray(rowCount)
        var row = 0
        profiles.forEachIndexed { i, profile ->
            val step = profile.rowHeight * scales[i]
            for (r in profile.ranges.indices) {
                val top = sliceTops[i] + r * step
                rowTops[row] = top
                rowBottoms[row] = min(sliceTops[i + 1], top + step)
                rowRanges[row] = profile.ranges[r]
                row++
            }
        }

        // Blank bands become cut candidates; thick bands are likely the margins between real pages.
        val candidates = mutableListOf<Cut>()
        var bandStart = -1
        for (r in 0..rowCount) {
            val blank = r < rowCount && rowRanges[r] <= BLANK_RANGE
            if (blank && bandStart == -1) bandStart = r
            if (!blank && bandStart != -1) {
                val top = rowTops[bandStart]
                val bottom = rowBottoms[r - 1]
                val thickness = bottom - top
                val position = (top + bottom) / 2f
                val edge = width * EDGE_EXCLUSION
                if (thickness >= width * MIN_BAND && position > edge && position < total - edge) {
                    candidates += Cut(position, min(1f, thickness / (width * STRONG_BAND)), false)
                }
                bandStart = -1
            }
        }

        // Long stretches without a gutter (a tall panel) still need page breaks.
        val cuts = mutableListOf(Cut(0f, 1f, false))
        (candidates + Cut(total, 1f, false)).forEach { next ->
            var previous = cuts.last().position
            while (next.position - previous > target * MAX_GAP) {
                val forced = quietestRow(rowTops, rowBottoms, rowRanges, previous + target * 0.85f, previous + target * 1.15f)
                cuts += Cut(forced, 0f, true)
                previous = forced
            }
            if (next.position > previous) cuts += next
        }

        // Pick the cuts that keep pages closest to the target height while preferring strong gutters.
        val count = cuts.size
        val costs = DoubleArray(count) { Double.MAX_VALUE }
        val previousCut = IntArray(count) { -1 }
        costs[0] = 0.0
        for (j in 1 until count) {
            val cutCost = if (j == count - 1) {
                0.0
            } else {
                (if (cuts[j].forced) FORCED_CUT_COST else 0.0) - GUTTER_BONUS * cuts[j].strength
            }
            for (i in j - 1 downTo 0) {
                val height = cuts[j].position - cuts[i].position
                if (i < j - 1 && height > target * MAX_PAGE) break
                if (height <= 0f || costs[i] == Double.MAX_VALUE) continue
                val cost = costs[i] + pageCost(height / target) + cutCost
                if (cost < costs[j]) {
                    costs[j] = cost
                    previousCut[j] = i
                }
            }
        }

        val chosen = mutableListOf<Int>()
        var at = count - 1
        while (at > 0) {
            chosen += at
            at = previousCut[at]
            if (at == -1) {
                // Should not happen: the previous cut is always considered.
                return fallbackSpecs(pages, run, profiles)
            }
        }
        chosen += 0
        chosen.reverse()

        return chosen.zipWithNext().map { (startCut, endCut) ->
            val top = cuts[startCut].position
            val bottom = cuts[endCut].position
            val pageHeight = max(1, (bottom - top).roundToInt())
            val pieces = mutableListOf<RebuiltPage.Piece>()
            var lastSource = run.first
            profiles.forEachIndexed { i, profile ->
                val overlapTop = max(top, sliceTops[i])
                val overlapBottom = min(bottom, sliceTops[i + 1])
                if (overlapBottom - overlapTop < 0.5f) return@forEachIndexed
                val sourceTop = ((overlapTop - sliceTops[i]) / scales[i]).roundToInt().coerceIn(0, profile.height - 1)
                val sourceBottom = ((overlapBottom - sliceTops[i]) / scales[i]).roundToInt()
                    .coerceIn(sourceTop + 1, profile.height)
                val pieceTop = (overlapTop - top).roundToInt().coerceIn(0, pageHeight - 1)
                val pieceBottom = (overlapBottom - top).roundToInt().coerceIn(pieceTop + 1, pageHeight)
                pieces += RebuiltPage.Piece(
                    source = pages[run.first + i],
                    sourceWidth = profile.width,
                    sourceTop = sourceTop,
                    sourceBottom = sourceBottom,
                    top = pieceTop,
                    bottom = pieceBottom,
                )
                lastSource = run.first + i
            }
            PageSpec(pages[lastSource].index, width, pageHeight, pieces)
        }
    }

    /** One page per slice, used only if the cut search fails. */
    private fun fallbackSpecs(pages: List<ReaderPage>, run: IntRange, profiles: List<Profile>): List<PageSpec> {
        val width = profiles.first().width
        return profiles.mapIndexed { i, profile ->
            val height = max(1, (profile.height * width.toFloat() / profile.width).roundToInt())
            val source = pages[run.first + i]
            PageSpec(
                source.index,
                width,
                height,
                listOf(RebuiltPage.Piece(source, profile.width, 0, profile.height, 0, height)),
            )
        }
    }

    private fun quietestRow(
        rowTops: FloatArray,
        rowBottoms: FloatArray,
        rowRanges: IntArray,
        from: Float,
        to: Float,
    ): Float {
        val middle = (from + to) / 2f
        var best = -1
        for (r in rowTops.indices) {
            val center = (rowTops[r] + rowBottoms[r]) / 2f
            if (center < from || center > to) continue
            if (
                best == -1 ||
                rowRanges[r] < rowRanges[best] ||
                (
                    rowRanges[r] == rowRanges[best] &&
                        abs(center - middle) < abs((rowTops[best] + rowBottoms[best]) / 2f - middle)
                    )
            ) {
                best = r
            }
        }
        return if (best == -1) middle else (rowTops[best] + rowBottoms[best]) / 2f
    }

    private fun pageCost(ratio: Float): Double {
        val r = ratio.toDouble()
        var cost = 4.0 * (r - 1.0) * (r - 1.0)
        // Pages wider than tall would trigger "zoom into wide images" and read as strips again.
        if (r < SHORT_PAGE) cost += 3.0 + 20.0 * (SHORT_PAGE - r)
        if (r > TALL_PAGE) cost += 10.0 * (r - TALL_PAGE)
        return cost
    }

    private fun openRendered(page: RebuiltPage): InputStream = renderedFile(page).inputStream()

    private fun renderedFile(page: RebuiltPage): File {
        val directory = File(cacheRoot, (page.chapter.chapter.id ?: -1L).toString())
        val file = File(directory, "${page.layoutKey}_${page.displayIndex}.jpg")
        if (file.length() > 0L) return file
        val lock = renderLocks.getOrPut(file.path) { Any() }
        synchronized(lock) {
            if (file.length() > 0L) return file
            directory.mkdirs()
            directory.setLastModified(System.currentTimeMillis())
            val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
            try {
                val canvas = Canvas(bitmap)
                canvas.drawColor(Color.WHITE)
                val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG)
                page.pieces.forEach { piece ->
                    val region = decodeRegion(piece)
                        ?: throw IOException("Unable to decode slice ${piece.source.index} for rebuilt page")
                    try {
                        canvas.drawBitmap(region, null, Rect(0, piece.top, page.width, piece.bottom), paint)
                    } finally {
                        region.recycle()
                    }
                }
                val temporary = File(directory, "${file.name}.tmp")
                temporary.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it) }
                if (!temporary.renameTo(file)) {
                    temporary.copyTo(file, overwrite = true)
                    temporary.delete()
                }
            } finally {
                bitmap.recycle()
            }
        }
        renderLocks.remove(file.path)
        return file
    }

    private fun decodeRegion(piece: RebuiltPage.Piece): Bitmap? {
        val openSource = piece.source.stream ?: return null
        val rect = Rect(0, piece.sourceTop, piece.sourceWidth, piece.sourceBottom)
        try {
            openSource().use { input ->
                @Suppress("DEPRECATION")
                val decoder: BitmapRegionDecoder? = BitmapRegionDecoder.newInstance(input, false)
                if (decoder != null) {
                    try {
                        decoder.decodeRegion(rect, null)?.let { return it }
                    } finally {
                        decoder.recycle()
                    }
                }
            }
        } catch (e: Exception) {
            logcat(LogPriority.WARN, e) { "PagedRebuild: region decoder failed for page ${piece.source.index}" }
        }

        // Formats the region decoder cannot read: decode the whole slice and crop it.
        val full = try {
            openSource().use { input -> BitmapFactory.decodeStream(input) }
                ?: openSource().use { input -> ImageUtil.decodeBitmap(Buffer().readFrom(input)) }
        } catch (e: Exception) {
            logcat(LogPriority.WARN, e) { "PagedRebuild: full decode failed for page ${piece.source.index}" }
            null
        } ?: return null

        val top = rect.top.coerceIn(0, full.height - 1)
        val height = (rect.bottom - top).coerceIn(1, full.height - top)
        if (top == 0 && height == full.height) return full
        return try {
            Bitmap.createBitmap(full, 0, top, full.width, height)
        } finally {
            full.recycle()
        }
    }

    private fun pruneOldFiles() {
        val limit = System.currentTimeMillis() - CACHE_MAX_AGE_MILLIS
        cacheRoot.listFiles()?.forEach { directory ->
            if (directory.lastModified() < limit) directory.deleteRecursively()
        }
    }

    companion object {
        /** Target page height as a multiple of the page width. */
        const val TARGET_RATIO = 1.42f
        private const val SLICE_RATIO_MIN = 1.0f
        private const val SLICE_RATIO_MAX = 2.0f
        private const val WIDTH_TOLERANCE = 0.04f
        private const val PROFILE_MIN_WIDTH = 200
        private const val BLANK_RANGE = 24
        private const val MIN_BAND = 0.006f
        private const val STRONG_BAND = 0.06f
        private const val EDGE_EXCLUSION = 0.05f
        private const val MAX_GAP = 1.7f
        private const val MAX_PAGE = 2.3f
        private const val SHORT_PAGE = 0.72
        private const val TALL_PAGE = 1.45
        private const val GUTTER_BONUS = 1.2
        private const val FORCED_CUT_COST = 2.0
        private const val JPEG_QUALITY = 95
        private const val CACHE_MAX_AGE_MILLIS = 3L * 24 * 60 * 60 * 1000
    }
}

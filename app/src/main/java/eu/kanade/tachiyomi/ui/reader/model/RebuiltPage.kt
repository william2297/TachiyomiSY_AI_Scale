package eu.kanade.tachiyomi.ui.reader.model

import java.io.InputStream

/**
 * A whole page rebuilt for the paged viewers from consecutive source slices.
 *
 * Some sources cut a chapter into uneven slices of the real pages. The long strip joins them
 * seamlessly, but the paged viewers showed every slice as its own page. A [RebuiltPage] joins the
 * slices into a virtual strip and keeps the part between two cut points.
 *
 * [index] is the last source page this page shows, so saved progress and the "chapter read" check
 * keep working on source page indices.
 */
class RebuiltPage(
    index: Int,
    /** Position of this page in the chapter's display list, from 0. */
    val displayIndex: Int,
    /** Identifies the slice layout; part of the enhancement and file cache keys. */
    val layoutKey: String,
    val width: Int,
    val height: Int,
    val pieces: List<Piece>,
    sourceChapter: ReaderChapter,
    opener: (RebuiltPage) -> InputStream,
) : ReaderPage(index) {

    /** Part of [source] between [sourceTop] and [sourceBottom] drawn between [top] and [bottom]. */
    class Piece(
        val source: ReaderPage,
        val sourceWidth: Int,
        val sourceTop: Int,
        val sourceBottom: Int,
        val top: Int,
        val bottom: Int,
    )

    override var chapter: ReaderChapter = sourceChapter

    /** All display pages of the chapter, filled once the layout is complete. */
    var chapterPages: List<ReaderPage> = emptyList()
        internal set

    val firstSourceIndex: Int get() = pieces.minOfOrNull { it.source.index } ?: index

    init {
        status = State.Ready
        stream = { opener(this) }
        // A separate cache variant keeps rebuilt pages away from the long strip's per-slice cache.
        enhancementKeySuffix = "rebuilt_${layoutKey}_$displayIndex"
    }

    override fun isFromSamePage(other: ReaderPage): Boolean {
        if (other.chapter.chapter.id != chapter.chapter.id) return false
        return if (other is RebuiltPage) {
            other.layoutKey == layoutKey && other.displayIndex == displayIndex
        } else {
            // A source page maps to the first rebuilt page that ends in it.
            other.index == index
        }
    }
}

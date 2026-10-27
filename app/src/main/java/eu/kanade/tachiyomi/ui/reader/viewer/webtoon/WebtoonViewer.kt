package eu.kanade.tachiyomi.ui.reader.viewer.webtoon

import android.graphics.PointF
import android.os.SystemClock
import android.view.Choreographer
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.animation.LinearInterpolator
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.view.children
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.core.view.postDelayed
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.NO_POSITION
import androidx.recyclerview.widget.WebtoonLayoutManager
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.ui.reader.model.ChapterTransition
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.model.ViewerChapters
import eu.kanade.tachiyomi.ui.reader.setting.ReaderPreferences
import eu.kanade.tachiyomi.ui.reader.viewer.ReaderPageImageView
import eu.kanade.tachiyomi.ui.reader.viewer.Viewer
import eu.kanade.tachiyomi.ui.reader.viewer.ViewerNavigation.NavigationRegion
import eu.kanade.tachiyomi.util.system.toast
import eu.kanade.tachiyomi.util.waifu2x.ImageEnhancer
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import mihon.app.di.appGraph
import tachiyomi.core.common.util.system.logcat
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Implementation of a [Viewer] to display pages with a [RecyclerView].
 */
class WebtoonViewer(val activity: ReaderActivity, val isContinuous: Boolean = true) : Viewer {

    val graph by lazy { activity.appGraph }
    val downloadManager by lazy { graph.downloadManager }
    val readerPreferences by lazy { graph.readerPreferences }

    private val scope = MainScope()

    /**
     * Recycler view used by this viewer.
     */
    val recycler = WebtoonRecyclerView(activity)

    /**
     * Frame containing the recycler view.
     */
    private val frame = WebtoonFrame(activity)

    /**
     * Distance to scroll when the user taps on one side of the recycler view.
     */
    private val scrollDistance = activity.resources.displayMetrics.heightPixels * 3 / 4

    /**
     * Layout manager of the recycler view.
     */
    // Pages are bound this far beyond the visible area so they start loading before they scroll into view.
    // Upscaling makes each page slower to appear, so two screens of look-ahead keeps the area below the
    // current page from showing as a black block (upstream used one scroll distance, about 0.75 screens).
    private val layoutManager = WebtoonLayoutManager(
        activity,
        maxOf(scrollDistance, activity.resources.displayMetrics.heightPixels * 2),
    )

    /**
     * Configuration used by this viewer, like allow taps, or crop image borders.
     */
    val config = WebtoonConfig(scope, readerPreferences)

    /**
     * Adapter of the recycler view.
     */
    private val adapter = WebtoonAdapter(this)

    /**
     * Currently active item. It can be a chapter page or a chapter transition.
     */
    private var currentPage: Any? = null
    private var pageSelectionSequence = 0

    private val threshold: Int by lazy { readerPreferences.readerHideThreshold.get().threshold }

    init {
        recycler.sidePaddingPercent = config.sidePadding
        recycler.setItemViewCacheSize(RECYCLER_VIEW_CACHE_SIZE)
        recycler.isVisible = false // Don't let the recycler layout yet
        recycler.layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
        recycler.isFocusable = false
        recycler.itemAnimator = null
        recycler.layoutManager = layoutManager
        recycler.adapter = adapter
        recycler.addOnScrollListener(
            object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    // A finger took over: whatever the taps still had to scroll is dropped
                    if (newState == RecyclerView.SCROLL_STATE_DRAGGING) cancelTapScroll()
                }

                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    onScrolled()

                    if ((dy > threshold || dy < -threshold) && activity.viewModel.state.value.menuVisible) {
                        activity.hideMenu()
                    }

                    if (dy < 0) {
                        val firstIndex = layoutManager.findFirstVisibleItemPosition()
                        val firstItem = adapter.items.getOrNull(firstIndex)
                        if (firstItem is ChapterTransition.Prev && firstItem.to != null) {
                            activity.requestPreloadChapter(firstItem.to)
                        }
                    }

                    val lastIndex = layoutManager.findLastEndVisibleItemPosition()
                    val lastItem = adapter.items.getOrNull(lastIndex)
                    if (lastItem is ChapterTransition.Next && lastItem.to == null) {
                        activity.showMenu()
                    }
                }
            },
        )
        recycler.tapListener = { event ->
            val height = recycler.originalHeight.takeIf { it > 0 } ?: recycler.height
            val pos = if (recycler.scaleX == 1f && recycler.width > 0 && height > 0) {
                // Not zoomed: the event is already in the list's own coordinates
                PointF(event.x / recycler.width, event.y / height)
            } else {
                val viewPosition = IntArray(2)
                recycler.getLocationOnScreen(viewPosition)
                val viewPositionRelativeToWindow = IntArray(2)
                recycler.getLocationInWindow(viewPositionRelativeToWindow)
                PointF(
                    (event.rawX - viewPosition[0] + viewPositionRelativeToWindow[0]) / recycler.width,
                    (event.rawY - viewPosition[1] + viewPositionRelativeToWindow[1]) / recycler.originalHeight,
                )
            }
            val action = config.navigator.getAction(pos)
            if (readerPreferences.debugPageLayers.get()) {
                activity.toast("tap %.2f, %.2f -> %s".format(pos.x, pos.y, action::class.simpleName))
            }
            when (action) {
                NavigationRegion.MENU -> activity.toggleMenu()
                NavigationRegion.NEXT, NavigationRegion.RIGHT -> scrollDown()
                NavigationRegion.PREV, NavigationRegion.LEFT -> scrollUp()
            }
        }
        recycler.longTapListener = f@{ event ->
            if (activity.viewModel.state.value.menuVisible || config.longTapEnabled) {
                val child = recycler.findChildViewUnder(event.x, event.y)
                if (child != null) {
                    val position = recycler.getChildAdapterPosition(child)
                    val item = adapter.items.getOrNull(position)
                    if (item is ReaderPage) {
                        activity.onPageLongTap(item)
                        return@f true
                    }
                }
            }
            false
        }

        // SY -->
        recycler.autoScrollTogglePauseListener = ::toggleAutoScrollPause
        recycler.autoScrollActivateListener = ::startAutoScrollFromGesture
        recycler.autoScrollStopListener = ::stopAutoScroll
        recycler.autoScrollSpeedChangeListener = ::adjustAutoScrollSpeed
        recycler.autoScrollInteractionListener = {
            interruptAutoScrollDelay = true
        }
        // A finger arriving cuts the wait for the next step short, so the step after release is not a whole interval away
        recycler.touchStartListener = { interruptAutoScrollDelay = true }
        // SY <--

        config.imagePropertyChangedListener = {
            recycler.sidePaddingPercent = config.sidePadding
            refreshAdapter()
        }

        config.themeChangedListener = {
            ActivityCompat.recreate(activity)
        }

        config.doubleTapZoomChangedListener = {
            frame.doubleTapZoom = it
        }

        config.zoomPropertyChangedListener = {
            frame.zoomOutDisabled = it
        }

        config.navigationModeChangedListener = {
            val showOnStart = config.navigationOverlayOnStart || config.forceNavigationOverlay
            activity.binding.navigationOverlay.setNavigation(config.navigator, showOnStart)
        }

        frame.layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
        frame.addView(recycler)
    }

    private fun checkAllowPreload(page: ReaderPage?): Boolean {
        // Page is transition page - preload allowed
        page ?: return true

        // Initial opening - preload allowed
        currentPage ?: return true

        val nextItem = adapter.items.getOrNull(adapter.items.size - 1)
        val nextChapter = (nextItem as? ChapterTransition.Next)?.to ?: (nextItem as? ReaderPage)?.chapter

        // Allow preload for
        // 1. Going between pages of same chapter
        // 2. Next chapter page
        return when (page.chapter) {
            (currentPage as? ReaderPage)?.chapter -> true
            nextChapter -> true
            else -> false
        }
    }

    /**
     * Returns the view this viewer uses.
     */
    override fun getView(): View {
        return frame
    }

    /**
     * Destroys this viewer. Called when leaving the reader or swapping viewers.
     */
    override fun destroy() {
        super.destroy()
        cancelTapScroll()
        autoScrollToast?.cancel()
        eu.kanade.tachiyomi.util.waifu2x.ImageEnhancer.cancelAll("webtoon viewer destroyed")
        scope.cancel()
    }

    /**
     * Called from the RecyclerView listener when a [page] is marked as active. It notifies the
     * activity of the change and requests the preload of the next chapter if this is the last page.
     */
    private fun onPageSelected(page: ReaderPage, allowPreload: Boolean) {
        val pages = page.chapter.pages ?: return
        logcat { "onPageSelected: ${page.number}/${pages.size}" }
        ReaderPageImageView.currentGlobalPageIndex = page.index
        ImageEnhancer.reprioritizeAround(page.index, page.enhancementKeySuffix)
        activity.onPageSelected(page)
        pageSelectionSequence++
        dispatchPageSelectedToHolder(page, pageSelectionSequence)
        preloadNextLogicalPageLeadingSegment(page)

        // Preload next chapter once we're within the last 5 pages of the current chapter
        val inPreloadRange = pages.size - page.number < 5
        if (inPreloadRange && allowPreload && page.chapter == adapter.currentChapter) {
            logcat { "Request preload next chapter because we're at page ${page.number} of ${pages.size}" }
            val nextItem = adapter.items.getOrNull(adapter.items.size - 1)
            val transitionChapter = (nextItem as? ChapterTransition.Next)?.to ?: (nextItem as?ReaderPage)?.chapter
            if (transitionChapter != null) {
                logcat { "Requesting to preload chapter ${transitionChapter.chapter.chapterNumber}" }
                activity.requestPreloadChapter(transitionChapter)
            }
        }
    }

    /**
     * Called from the RecyclerView listener when a [transition] is marked as active. It request the
     * preload of the destination chapter of the transition.
     */
    private fun onTransitionSelected(transition: ChapterTransition) {
        logcat { "onTransitionSelected: $transition" }
        val toChapter = transition.to
        if (toChapter != null) {
            logcat { "Request preload destination chapter because we're on the transition" }
            activity.requestPreloadChapter(toChapter)
        }
    }

    /**
     * Tells this viewer to set the given [chapters] as active.
     */
    override fun setChapters(chapters: ViewerChapters) {
        val forceTransition = config.alwaysShowChapterTransition || currentPage is ChapterTransition
        adapter.setChapters(chapters, forceTransition)

        if (recycler.isGone) {
            logcat { "Recycler first layout" }
            val pages = chapters.currChapter.pages ?: return
            moveToPage(pages[min(chapters.currChapter.requestedPage, pages.lastIndex)])
            recycler.isVisible = true
        }
    }

    /**
     * Tells this viewer to move to the given [page].
     */
    override fun moveToPage(page: ReaderPage) {
        val position = adapter.items.indexOf(page)
        if (position != -1) {
            layoutManager.scrollToPositionWithOffset(position, 0)
            if (findActiveItemPosition() == NO_POSITION) {
                onScrolled(pos = position)
            }
        } else {
            logcat { "Page $page not found in adapter" }
        }
    }

    fun onScrolled(pos: Int? = null) {
        val position = pos ?: findActiveItemPosition()
        val item = adapter.items.getOrNull(position)
        val allowPreload = checkAllowPreload(item as? ReaderPage)
        if (item != null && currentPage != item) {
            currentPage = item
            when (item) {
                is ReaderPage -> onPageSelected(item, allowPreload)
                is ChapterTransition -> onTransitionSelected(item)
            }
        }
    }

    /**
     * Scrolls up by [scrollDistance].
     */
    private fun scrollUp() {
        scrollByTap(-scrollDistance)
    }

    /**
     * Taps move the list toward a target that every further tap extends. The motion is a critically damped spring:
     * it starts gently, ends gently and never overshoots, and its speed stays continuous when another tap moves the
     * target. Restarting a fresh scroll for each tap made quick taps stutter and skip.
     */
    private var tapRemaining = 0f
    private var tapVelocity = 0f
    private var tapCarry = 0f
    private var tapLastFrameNanos = 0L
    private var tapAnimating = false

    private val tapFrameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (!tapAnimating) return
            var seconds = if (tapLastFrameNanos == 0L) {
                1f / 60f
            } else {
                ((frameTimeNanos - tapLastFrameNanos) / 1_000_000_000f).coerceIn(0f, 0.05f)
            }
            tapLastFrameNanos = frameTimeNanos

            var moved = 0f
            while (seconds > 0f) {
                val dt = minOf(seconds, TAP_SCROLL_STEP_SECONDS)
                seconds -= dt
                val acceleration = TAP_SCROLL_OMEGA * TAP_SCROLL_OMEGA * tapRemaining -
                    2f * TAP_SCROLL_OMEGA * tapVelocity
                tapVelocity += acceleration * dt
                val step = tapVelocity * dt
                tapRemaining -= step
                moved += step
            }

            val finishing = abs(tapRemaining) < TAP_SCROLL_FINISH_PX && abs(tapVelocity) < TAP_SCROLL_FINISH_SPEED
            tapCarry += moved + if (finishing) tapRemaining else 0f
            val whole = tapCarry.toInt()
            if (whole != 0) {
                recycler.scrollBy(0, whole)
                tapCarry -= whole
            }
            if (finishing) {
                cancelTapScroll()
            } else {
                Choreographer.getInstance().postFrameCallback(this)
            }
        }
    }

    private fun cancelTapScroll() {
        tapAnimating = false
        tapRemaining = 0f
        tapVelocity = 0f
        tapCarry = 0f
        tapLastFrameNanos = 0L
        Choreographer.getInstance().removeFrameCallback(tapFrameCallback)
    }

    private fun scrollByTap(distance: Int) {
        if (!config.usePageTransitions) {
            recycler.scrollBy(0, distance)
            return
        }
        if (!tapAnimating) {
            // A flick or an auto scroll step still running would fight the tap
            if (recycler.scrollState == RecyclerView.SCROLL_STATE_SETTLING) recycler.stopScroll()
            tapAnimating = true
            tapLastFrameNanos = 0L
            tapCarry = 0f
            tapRemaining = 0f
            tapVelocity = 0f
            Choreographer.getInstance().postFrameCallback(tapFrameCallback)
        }
        // A tap the other way cancels what is left instead of adding to it
        if ((tapRemaining > 0) != (distance > 0)) {
            tapRemaining = 0f
            tapVelocity = 0f
        }
        tapRemaining += distance
    }

    /**
     * Scrolls one screen over [duration] at constant speed. Used by auto scroll.
     */
    fun linearScroll(duration: Duration) {
        recycler.smoothScrollBy(
            0,
            activity.resources.displayMetrics.heightPixels,
            LinearInterpolator(),
            duration.inWholeMilliseconds.toInt(),
        )
    }

    /**
     * Scrolls down by [scrollDistance].
     */
    fun scrollDown() {
        scrollByTap(scrollDistance)
    }

    // SY -->
    private var autoScrollEnabled = false
    private var autoScrollPaused = false
    private var autoScrollHasRuntimeSpeedOverride = false
    private var autoScrollIntervalMillis = DEFAULT_AUTO_SCROLL_INTERVAL_MILLIS
    private var interruptAutoScrollDelay = false
    private var autoScrollToast: Toast? = null
    private var autoScrollUiBlocked = false
    private var autoScrollGesturesEnabled = false
    private var autoScrollGestureToastsEnabled = true

    fun syncAutoScrollSettings(enabled: Boolean, interval: Duration, forceInterval: Boolean = false) {
        val intervalMillis =
            interval.inWholeMilliseconds
                .coerceIn(MIN_AUTO_SCROLL_INTERVAL_MILLIS, MAX_AUTO_SCROLL_INTERVAL_MILLIS)

        if (!enabled) {
            autoScrollEnabled = false
            autoScrollPaused = false
            autoScrollHasRuntimeSpeedOverride = false
            interruptAutoScrollDelay = false
            recycler.stopScroll()
            updateAutoScrollGestureMode()
            return
        }

        if (!autoScrollEnabled || forceInterval) {
            autoScrollIntervalMillis = intervalMillis
            autoScrollHasRuntimeSpeedOverride = false
            autoScrollPaused = false
        } else if (!autoScrollHasRuntimeSpeedOverride) {
            autoScrollIntervalMillis = intervalMillis
        }

        autoScrollEnabled = true
        updateAutoScrollGestureMode()
    }

    /** The menu is open: auto scroll holds still and carries on when the menu closes. */
    fun setAutoScrollUiBlocked(blocked: Boolean) {
        autoScrollUiBlocked = blocked
        if (blocked) {
            recycler.stopScroll()
            interruptAutoScrollDelay = true
        }
        updateAutoScrollGestureMode()
    }

    fun setAutoScrollGesturesEnabled(enabled: Boolean) {
        autoScrollGesturesEnabled = enabled
        updateAutoScrollGestureMode()
    }

    fun setAutoScrollGestureToastsEnabled(enabled: Boolean) {
        autoScrollGestureToastsEnabled = enabled
        if (!enabled) {
            autoScrollToast?.cancel()
            autoScrollToast = null
        }
    }

    private fun isAutoScrollHeld(): Boolean {
        // After a finger lifts, wait a second before moving on so the page can still be read
        val recentlyTouched = SystemClock.uptimeMillis() - recycler.lastTouchEndMillis < AUTO_SCROLL_RESUME_DELAY_MILLIS
        return autoScrollPaused || autoScrollUiBlocked || recycler.isTouched || recentlyTouched ||
            recycler.isAutoScrollGestureInProgress()
    }

    fun performAutoScrollStep(smooth: Boolean) {
        if (!autoScrollEnabled || isAutoScrollHeld()) return

        if (smooth) {
            linearScroll(autoScrollIntervalMillis.milliseconds)
        } else {
            scrollDown()
        }
    }

    fun autoScrollDelayMillis(): Long {
        return if (isAutoScrollHeld()) AUTO_SCROLL_POLL_DELAY_MILLIS else autoScrollIntervalMillis
    }

    fun consumeAutoScrollDelayInterrupt(): Boolean {
        val shouldInterrupt = interruptAutoScrollDelay
        interruptAutoScrollDelay = false
        return shouldInterrupt
    }

    private fun toggleAutoScrollPause() {
        if (!autoScrollEnabled) return

        autoScrollPaused = !autoScrollPaused
        interruptAutoScrollDelay = true
        recycler.stopScroll()

        showAutoScrollToast("Auto scroll: " + if (autoScrollPaused) "paused" else "resumed")
    }

    private fun stopAutoScroll() {
        if (!autoScrollEnabled) return

        autoScrollEnabled = false
        autoScrollPaused = false
        autoScrollHasRuntimeSpeedOverride = false
        interruptAutoScrollDelay = true
        updateAutoScrollGestureMode()
        recycler.stopScroll()
        activity.onAutoScrollStoppedByViewer()
        showAutoScrollToast("Auto scroll: stopped")
    }

    private fun adjustAutoScrollSpeed(steps: Int) {
        if (!autoScrollEnabled || steps == 0) return

        recycler.stopScroll()
        interruptAutoScrollDelay = true
        autoScrollHasRuntimeSpeedOverride = true

        var newInterval = autoScrollIntervalMillis.toDouble()
        repeat(abs(steps)) {
            newInterval *= if (steps > 0) AUTO_SCROLL_SPEED_UP_FACTOR else AUTO_SCROLL_SPEED_DOWN_FACTOR
        }

        autoScrollIntervalMillis =
            newInterval.roundToLong().coerceIn(MIN_AUTO_SCROLL_INTERVAL_MILLIS, MAX_AUTO_SCROLL_INTERVAL_MILLIS)

        showAutoScrollToast("Auto scroll: ${formatAutoScrollInterval()}")
    }

    private fun formatAutoScrollInterval(): String {
        val seconds = autoScrollIntervalMillis / 1000f
        val pattern = if (seconds < 10f) "%.2fs" else "%.1fs"
        return String.format(Locale.getDefault(), pattern, seconds)
    }

    private fun showAutoScrollToast(message: String) {
        if (!autoScrollGestureToastsEnabled) return

        autoScrollToast?.cancel()
        autoScrollToast = activity.toast(message)
    }

    private fun startAutoScrollFromGesture() {
        if (autoScrollEnabled) return

        activity.startAutoScrollFromViewer()
        showAutoScrollToast("Auto scroll: started")
    }

    private fun updateAutoScrollGestureMode() {
        val gesturesAvailable = autoScrollGesturesEnabled && !autoScrollUiBlocked
        recycler.autoScrollGestureModeEnabled = gesturesAvailable && autoScrollEnabled
        recycler.autoScrollActivationGestureEnabled = gesturesAvailable && !autoScrollEnabled
    }
    // SY <--

    /**
     * Called from the containing activity when a key [event] is received. It should return true
     * if the event was handled, false otherwise.
     */
    override fun handleKeyEvent(event: KeyEvent): Boolean {
        val isUp = event.action == KeyEvent.ACTION_UP

        when (event.keyCode) {
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                if (!config.volumeKeysEnabled || activity.viewModel.state.value.menuVisible) {
                    return false
                } else if (isUp) {
                    if (!config.volumeKeysInverted) scrollDown() else scrollUp()
                }
            }
            KeyEvent.KEYCODE_VOLUME_UP -> {
                if (!config.volumeKeysEnabled || activity.viewModel.state.value.menuVisible) {
                    return false
                } else if (isUp) {
                    if (!config.volumeKeysInverted) scrollUp() else scrollDown()
                }
            }
            KeyEvent.KEYCODE_MENU -> if (isUp) activity.toggleMenu()

            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_UP,
            KeyEvent.KEYCODE_PAGE_UP,
            -> if (isUp) scrollUp()

            KeyEvent.KEYCODE_DPAD_RIGHT,
            KeyEvent.KEYCODE_DPAD_DOWN,
            KeyEvent.KEYCODE_PAGE_DOWN,
            -> if (isUp) scrollDown()
            else -> return false
        }
        return true
    }

    /**
     * Called from the containing activity when a generic motion [event] is received. It should
     * return true if the event was handled, false otherwise.
     */
    override fun handleGenericMotionEvent(event: MotionEvent): Boolean {
        return false
    }

    private fun dispatchPageSelectedToHolder(page: ReaderPage, selectionSequence: Int) {
        val notifyHolder = {
            findAttachedHolderForPage(page)?.notifyPageSelected(selectionSequence)
        }
        notifyHolder()
        recycler.post { notifyHolder() }
        recycler.postDelayed(120) { notifyHolder() }
        recycler.postDelayed(350) { notifyHolder() }
    }

    private fun preloadNextLogicalPageLeadingSegment(page: ReaderPage) {
        val nextPage = findNextReaderPage(page) ?: return
        val preloadHolder = {
            findAttachedHolderForPage(nextPage)?.preloadLeadingSegment()
        }
        preloadHolder()
        recycler.post { preloadHolder() }
        recycler.postDelayed(120) { preloadHolder() }
        recycler.postDelayed(350) { preloadHolder() }
    }

    private fun findNextReaderPage(page: ReaderPage): ReaderPage? {
        val startIndex = adapter.items.indexOf(page)
        if (startIndex == -1) return null
        return adapter.items
            .drop(startIndex + 1)
            .firstOrNull { it is ReaderPage } as? ReaderPage
    }

    private fun findAttachedHolderForPage(page: ReaderPage): WebtoonPageHolder? {
        return recycler.findViewHolderForAdapterPosition(adapter.items.indexOf(page)) as? WebtoonPageHolder
            ?: recycler.children
                .mapNotNull { recycler.getChildViewHolder(it) as? WebtoonPageHolder }
                .firstOrNull { holder -> holder.boundPage()?.isFromSamePage(page) == true }
    }

    private fun findActiveItemPosition(): Int {
        var bestPosition = NO_POSITION
        var bestVisibleHeight = -1

        for (child in recycler.children) {
            val top = max(child.top, recycler.paddingTop)
            val bottom = min(child.bottom, recycler.height - recycler.paddingBottom)
            val visibleHeight = (bottom - top).coerceAtLeast(0)
            if (visibleHeight <= 0) continue

            val position = recycler.getChildAdapterPosition(child)
            if (position == NO_POSITION) continue

            if (visibleHeight > bestVisibleHeight) {
                bestVisibleHeight = visibleHeight
                bestPosition = position
            }
        }

        return bestPosition
    }

    /**
     * Notifies adapter of changes around the current page to trigger a relayout in the recycler.
     * Used when an image configuration is changed.
     */
    private fun refreshAdapter() {
        val position = layoutManager.findLastEndVisibleItemPosition()
        adapter.refresh()
        adapter.notifyItemRangeChanged(
            max(0, position - 3),
            min(position + 3, adapter.itemCount - 1),
        )
    }
}

// Double the cache size to reduce rebinds/recycles incurred by the extra layout space on scroll direction changes
private const val RECYCLER_VIEW_CACHE_SIZE = 6

// About 0.45 s from a tap to nearly at rest; larger is quicker
private const val TAP_SCROLL_OMEGA = 10f
private const val TAP_SCROLL_STEP_SECONDS = 1f / 120f
private const val TAP_SCROLL_FINISH_PX = 3f
private const val TAP_SCROLL_FINISH_SPEED = 60f

// SY -->
private const val AUTO_SCROLL_POLL_DELAY_MILLIS = 100L
private const val AUTO_SCROLL_RESUME_DELAY_MILLIS = 1_000L
private const val DEFAULT_AUTO_SCROLL_INTERVAL_MILLIS = 3_000L
private const val MIN_AUTO_SCROLL_INTERVAL_MILLIS = 250L
private const val MAX_AUTO_SCROLL_INTERVAL_MILLIS = 9_999_000L
private const val AUTO_SCROLL_SPEED_UP_FACTOR = 0.9
private const val AUTO_SCROLL_SPEED_DOWN_FACTOR = 1.1
// SY <--

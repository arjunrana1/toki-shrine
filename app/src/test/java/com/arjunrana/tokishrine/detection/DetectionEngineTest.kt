package com.arjunrana.tokishrine.detection

import com.arjunrana.tokishrine.data.repo.EventRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/*
 * DetectionEngine rules (PRD §13 / phase-04): immediate app triggers,
 * settle-then-trigger for sites, focus and search-query exclusion,
 * per-window caching, stale-settle cancellation and repeat debounce.
 * The clock is virtual; the engine has no Android dependencies.
 */
class DetectionEngineTest {

    private var now: Long = 0
    private lateinit var engine: DetectionEngine

    private val instagram = BlockRef(blockId = 1, blockName = "Dooms")
    private val reddit = BlockRef(blockId = 2, blockName = "Reddit")

    @Before
    fun setUp() {
        now = 0
        engine = DetectionEngine(clock = { now }, settleDelayMs = 2000, debounceMs = 10_000)
        engine.onBlocksChanged(
            apps = mapOf("com.instagram.android" to instagram),
            sites = mapOf("reddit.com" to reddit),
        )
    }

    private fun appTrigger(target: String = "com.instagram.android", latency: Long = 0) =
        DetectionAction.Trigger(
            DetectionTrigger(EventRepository.TARGET_TYPE_APP, target, 1, "Dooms", latency),
        )

    private fun siteTrigger(latency: Long) =
        DetectionAction.Trigger(
            DetectionTrigger(EventRepository.TARGET_TYPE_SITE, "reddit.com", 2, "Reddit", latency),
        )

    // A window always reaches the service through TYPE_WINDOW_STATE_CHANGED
    // before any content event, so site-path tests open the window first.
    private fun openChromeWindow(windowId: Int = 20) {
        assertEquals(emptyList<DetectionAction>(), engine.onWindowStateChanged(windowId, "com.android.chrome"))
    }

    private fun readReddit(windowId: Int = 20, focused: Boolean = false) =
        engine.onBrowserAddressReading(windowId, "com.android.chrome", "https://old.reddit.com/r/all", focused)

    // — app triggers —

    @Test
    fun blockedAppWindowChangeTriggersImmediately() {
        val actions = engine.onWindowStateChanged(10, "com.instagram.android")
        assertEquals(listOf(appTrigger()), actions)
    }

    @Test
    fun unblockedAppNeverTriggers() {
        assertEquals(emptyList<DetectionAction>(), engine.onWindowStateChanged(10, "com.other.app"))
    }

    @Test
    fun repeatedAppTriggerInsideDebounceWindowIsSuppressed() {
        engine.onWindowStateChanged(10, "com.instagram.android")
        now = 1_000
        assertEquals(emptyList<DetectionAction>(), engine.onWindowStateChanged(10, "com.instagram.android"))
        now = 10_000
        assertEquals(listOf(appTrigger()), engine.onWindowStateChanged(10, "com.instagram.android"))
    }

    // — site settle path —

    @Test
    fun matchingUrlSchedulesTheSettleDelay() {
        openChromeWindow()
        assertEquals(listOf(DetectionAction.ScheduleSettle(1, 2000)), readReddit())
    }

    @Test
    fun settleElapsesIntoSiteTriggerWithSettleLatency() {
        openChromeWindow()
        readReddit()
        now = 2_000
        assertEquals(listOf(siteTrigger(latency = 2_000)), engine.onSettleElapsed(1))
    }

    @Test
    fun nonMatchingUrlNeverSchedules() {
        openChromeWindow()
        assertEquals(
            emptyList<DetectionAction>(),
            engine.onBrowserAddressReading(20, "com.android.chrome", "https://example.com", false),
        )
    }

    @Test
    fun identicalReadingWhileSettlingIsIdempotent() {
        openChromeWindow()
        readReddit()
        assertEquals(emptyList<DetectionAction>(), readReddit())
    }

    // — typing and search exclusion —

    @Test
    fun focusedAddressBarCancelsPendingAndNeverSchedules() {
        openChromeWindow()
        readReddit()
        assertEquals(
            listOf(DetectionAction.CancelSettle(1)),
            readReddit(focused = true),
        )
        now = 2_000
        // The cancelled settle's callback is a no-op even if it still fires.
        assertEquals(emptyList<DetectionAction>(), engine.onSettleElapsed(1))
    }

    @Test
    fun focusedReadingWithoutPendingSchedulesNothing() {
        openChromeWindow()
        assertEquals(emptyList<DetectionAction>(), readReddit(focused = true))
    }

    @Test
    fun searchQueryWithSpacesCancelsPendingAndNeverSchedules() {
        openChromeWindow()
        readReddit()
        assertEquals(
            listOf(DetectionAction.CancelSettle(1)),
            engine.onBrowserAddressReading(20, "com.android.chrome", "reddit com photos", false),
        )
        assertEquals(
            emptyList<DetectionAction>(),
            engine.onBrowserAddressReading(20, "com.android.chrome", "how to block reddit", false),
        )
    }

    @Test
    fun blankValueIsTreatedAsSearchNotNavigation() {
        openChromeWindow()
        assertEquals(
            emptyList<DetectionAction>(),
            engine.onBrowserAddressReading(20, "com.android.chrome", "   ", false),
        )
    }

    // — per-window caching —

    @Test
    fun missingAddressBarNodeKeepsPendingSettleAlive() {
        // Chrome hides the bar on scroll: the cached URL stands and the
        // settle in flight must survive the absence.
        openChromeWindow()
        readReddit()
        assertEquals(
            emptyList<DetectionAction>(),
            engine.onBrowserAddressReading(20, "com.android.chrome", null, false),
        )
        now = 2_000
        assertEquals(listOf(siteTrigger(latency = 2_000)), engine.onSettleElapsed(1))
    }

    @Test
    fun missingNodeOnStateChangeEmitsTheUrlReadFailedCanary() {
        assertEquals(
            listOf(DetectionAction.UrlReadFailed("com.android.chrome")),
            engine.onAddressBarMissing(20, "com.android.chrome"),
        )
    }

    // — staleness and cancellation —

    @Test
    fun changedUrlCancelsOldSettleAndSchedulesANewOne() {
        openChromeWindow()
        readReddit()
        now = 1_000
        val actions = engine.onBrowserAddressReading(20, "com.android.chrome", "https://reddit.com/r/news", false)
        assertEquals(
            listOf(DetectionAction.CancelSettle(1), DetectionAction.ScheduleSettle(2, 2000)),
            actions,
        )
        now = 3_000
        assertEquals(emptyList<DetectionAction>(), engine.onSettleElapsed(1))
        assertEquals(listOf(siteTrigger(latency = 2_000)), engine.onSettleElapsed(2))
    }

    @Test
    fun windowSwitchCancelsPendingSettle() {
        openChromeWindow()
        readReddit()
        assertEquals(
            listOf(DetectionAction.CancelSettle(1)),
            engine.onWindowStateChanged(30, "com.android.chrome"),
        )
        now = 2_000
        assertEquals(emptyList<DetectionAction>(), engine.onSettleElapsed(1))
    }

    @Test
    fun sameWindowAgainDoesNotCancelThePendingSettle() {
        openChromeWindow()
        readReddit()
        assertEquals(emptyList<DetectionAction>(), engine.onWindowStateChanged(20, "com.android.chrome"))
        now = 2_000
        assertEquals(listOf(siteTrigger(latency = 2_000)), engine.onSettleElapsed(1))
    }

    @Test
    fun settleCallbackWithUnknownGenerationIsIgnored() {
        openChromeWindow()
        readReddit()
        now = 2_000
        assertEquals(emptyList<DetectionAction>(), engine.onSettleElapsed(99))
    }

    // — site debounce —

    @Test
    fun repeatedSiteTriggerInsideDebounceWindowIsSuppressed() {
        openChromeWindow()
        readReddit()
        now = 2_000
        assertEquals(listOf(siteTrigger(latency = 2_000)), engine.onSettleElapsed(1))

        // Second full settle cycle for the same URL inside the window.
        now = 3_000
        assertEquals(listOf(DetectionAction.ScheduleSettle(2, 2000)), readReddit())
        now = 5_000
        assertEquals(emptyList<DetectionAction>(), engine.onSettleElapsed(2))

        // After the debounce window, the same URL can trigger again.
        now = 13_000
        assertEquals(listOf(DetectionAction.ScheduleSettle(3, 2000)), readReddit())
        now = 15_000
        assertEquals(listOf(siteTrigger(latency = 2_000)), engine.onSettleElapsed(3))
    }

    // — recreation —

    @Test
    fun recreatedEngineStartsCleanAndCanTriggerAgain() {
        engine.onWindowStateChanged(10, "com.instagram.android")
        // Service killed and rebound: the fresh engine has no debounce
        // memory, so the same foreground app is a new trigger decision.
        val recreated = DetectionEngine(clock = { now }, settleDelayMs = 2000, debounceMs = 10_000)
        recreated.onBlocksChanged(
            apps = mapOf("com.instagram.android" to instagram),
            sites = mapOf("reddit.com" to reddit),
        )
        now = 500
        assertEquals(listOf(appTrigger(latency = 0)), recreated.onWindowStateChanged(10, "com.instagram.android"))
    }

    @Test
    fun freshEngineIgnoresSettleCallbacksFromItsPredecessor() {
        openChromeWindow()
        readReddit()
        val recreated = DetectionEngine(clock = { now }, settleDelayMs = 2000, debounceMs = 10_000)
        now = 2_000
        assertEquals(listOf(siteTrigger(latency = 2_000)), engine.onSettleElapsed(1))
        assertEquals(emptyList<DetectionAction>(), recreated.onSettleElapsed(1))
    }
}

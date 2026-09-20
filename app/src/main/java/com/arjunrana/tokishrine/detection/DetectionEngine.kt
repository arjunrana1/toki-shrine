package com.arjunrana.tokishrine.detection

import com.arjunrana.tokishrine.data.repo.EventRepository

// What actually fires when detection succeeds. triggerType uses the §10
// values (app | site); startedAtElapsedMs is the original app event or site
// reading time. BlockActivity combines it with its resumed time so §10
// latency includes both settling and the actual activity launch.
data class DetectionTrigger(
    val triggerType: String,
    val target: String,
    val blockId: Long,
    val blockName: String,
    val startedAtElapsedMs: Long,
)

// The engine's decisions for one input. The service executes them: settle
// scheduling runs on its handler, triggers launch the interruption Activity (which
// logs block_screen_shown once resumed), and url-read failures log the §10
// canary. Keeping the actions declarative makes the machine JVM-testable.
sealed interface DetectionAction {
    data class ScheduleSettle(val generation: Long, val delayMs: Long) : DetectionAction
    data class CancelSettle(val generation: Long) : DetectionAction
    data class Trigger(val trigger: DetectionTrigger) : DetectionAction
    data class UrlReadFailed(val browserPackage: String) : DetectionAction
}

// The last known address-bar reading for one accessibility window. Chrome
// hides the address bar on scroll, so a missing node is "unknown", not
// "no site" — url stays at its last value until a real reading replaces
// it or the window is evicted.
private data class WindowReading(
    val packageName: String,
    var url: String?,
    var focused: Boolean = false,
)

// The single in-flight settle. Exactly one exists at a time; a generation
// counter invalidates a handler callback that fires after the state it
// was scheduled for has been replaced or cancelled.
private data class PendingSettle(
    val generation: Long,
    val windowId: Int,
    val packageName: String,
    val url: String,
    val blockedDomain: String,
    val ref: BlockRef,
    val scheduledAtMs: Long,
)

/*
 * Phase 4 detection state machine (PRD §13): foreground-app triggers,
 * supported-browser URL settling, per-window caching and repeat debounce,
 * with no Android types so every rule is deterministic under test.
 *
 * Invariants:
 * - App triggers are immediate on the blocked app's window-state change.
 * - Site triggers only fire after the settle delay elapses with the
 *   reading unchanged: same window, same URL, address bar unfocused.
 * - A focused address bar or a spaced (search) value cancels any pending
 *   settle and never schedules one; a missing address-bar node changes
 *   nothing (the cached URL stands).
 * - A window/package/address change cancels the stale settle; a settle
 *   callback with an old generation is ignored.
 * - One trigger per (type, target, block) inside the debounce window.
 * - The engine is recreation-safe: a fresh instance starts with empty
 *   caches and debounce memory (a service restart may re-trigger).
 */
class DetectionEngine(
    private val clock: () -> Long = System::currentTimeMillis,
    private val settleDelayMs: Long = SETTLE_DELAY_MS,
    private val debounceMs: Long = REPEAT_TRIGGER_DEBOUNCE_MS,
) {

    private var appBlocks: Map<String, BlockRef> = emptyMap()
    private var siteBlocks: Map<String, BlockRef> = emptyMap()

    // Insertion-ordered for cheap eldest eviction; window ids are unique
    // per window lifetime, so re-put refreshes recency.
    private val windows = LinkedHashMap<Int, WindowReading>()
    private var currentWindowId: Int? = null

    private var pending: PendingSettle? = null
    private var generation: Long = 0

    // debounce key → fire time
    private val lastFired = HashMap<String, Long>()

    @Synchronized
    fun onBlocksChanged(
        apps: Map<String, BlockRef>,
        sites: Map<String, BlockRef>,
    ): List<DetectionAction> {
        appBlocks = apps
        siteBlocks = sites
        val activePending = pending
        return if (
            activePending != null &&
            sites[activePending.blockedDomain] != activePending.ref
        ) {
            listOf(cancelPending())
        } else {
            emptyList()
        }
    }

    @Synchronized
    fun onWindowStateChanged(windowId: Int, packageName: String): List<DetectionAction> {
        val now = clock()
        currentWindowId = windowId
        val packageChanged = touchWindow(windowId, packageName).packageChanged
        val actions = ArrayList<DetectionAction>()
        pending?.let {
            if (it.windowId != windowId || packageChanged) actions += cancelPending()
        }
        appBlocks[packageName]?.let { ref ->
            maybeTrigger(
                EventRepository.TARGET_TYPE_APP,
                packageName,
                ref,
                startedAtElapsedMs = now,
                now = now,
            )?.let { actions += it }
        }
        return actions
    }

    // A real address-bar reading from a supported browser. text == null
    // means the node was absent (hidden or missing); focused readings and
    // spaced values are typing/search, never navigation state.
    @Synchronized
    fun onBrowserAddressReading(
        windowId: Int,
        packageName: String,
        text: String?,
        focused: Boolean,
    ): List<DetectionAction> {
        val touch = touchWindow(windowId, packageName)
        val window = touch.reading
        val actions = ArrayList<DetectionAction>()
        if (touch.packageChanged && pending?.windowId == windowId) {
            actions += cancelPending()
        }
        if (focused) {
            // Typing: don't cache the partial text, just stop anything
            // that was settling on the previous value.
            window.focused = true
            actions += cancelIfPendingHere(windowId)
            return actions
        }
        window.focused = false
        if (text == null) {
            // Hidden address bar: the cached URL stands, and a settle in
            // flight for this window must survive the scroll.
            return actions
        }
        if (text.contains(' ') || text.isBlank()) {
            // A search query, not a URL (PRD §13 space rule).
            actions += cancelIfPendingHere(windowId)
            return actions
        }
        window.url = text
        val host = DomainMatcher.extractHost(text)
        val blockedDomain = host?.let { DomainMatcher.matchDomain(it, siteBlocks.keys) }
        if (blockedDomain == null) {
            // Nothing blockable here; a settle from an earlier value is
            // stale the moment the bar shows a different URL.
            actions += cancelIfPendingHere(windowId)
            return actions
        }
        if (pending?.windowId == windowId && pending?.url == text) {
            // Identical reading to the one already settling.
            return actions
        }
        pending?.let { actions += cancelPending() }
        val now = clock()
        generation += 1
        pending = PendingSettle(
            generation = generation,
            windowId = windowId,
            packageName = packageName,
            url = text,
            blockedDomain = blockedDomain,
            ref = siteBlocks.getValue(blockedDomain),
            scheduledAtMs = now,
        )
        actions += DetectionAction.ScheduleSettle(generation, settleDelayMs)
        return actions
    }

    // The address-bar node was absent. Only called for window-state
    // changes, where a present browser exposes its bar — that absence is
    // the §10 url_read_failed canary. Mid-scroll content changes take the
    // text == null path above instead, so legitimate hides don't flood it.
    @Synchronized
    fun onAddressBarMissing(windowId: Int, packageName: String): List<DetectionAction> {
        val touch = touchWindow(windowId, packageName)
        val actions = ArrayList<DetectionAction>()
        if (touch.packageChanged && pending?.windowId == windowId) {
            actions += cancelPending()
        }
        actions += DetectionAction.UrlReadFailed(packageName)
        return actions
    }

    // The service's settle handler reports back with the generation it
    // was scheduled for. Anything older than the current pending settle —
    // or any state drift since scheduling — turns the callback into a
    // no-op instead of a block.
    @Synchronized
    fun onSettleElapsed(
        generation: Long,
        activeWindowId: Int?,
        activePackageName: String?,
    ): List<DetectionAction> {
        val pending = this.pending ?: return emptyList()
        if (pending.generation != generation) return emptyList()
        this.pending = null
        val window = windows[pending.windowId]
        val stillValid = currentWindowId == pending.windowId &&
            activeWindowId == pending.windowId &&
            activePackageName == pending.packageName &&
            window != null &&
            !window.focused &&
            window.url == pending.url &&
            window.packageName == pending.packageName &&
            siteBlocks[pending.blockedDomain] == pending.ref
        if (!stillValid) return emptyList()
        val now = clock()
        return listOfNotNull(
            maybeTrigger(
                EventRepository.TARGET_TYPE_SITE,
                pending.blockedDomain,
                pending.ref,
                startedAtElapsedMs = pending.scheduledAtMs,
                now = now,
            ),
        )
    }

    private fun cancelIfPendingHere(windowId: Int): List<DetectionAction> =
        if (pending?.windowId == windowId) listOf(cancelPending()) else emptyList()

    private fun cancelPending(): DetectionAction.CancelSettle {
        val cancelled = pending!!
        pending = null
        return DetectionAction.CancelSettle(cancelled.generation)
    }

    private fun maybeTrigger(
        triggerType: String,
        target: String,
        ref: BlockRef,
        startedAtElapsedMs: Long,
        now: Long,
    ): DetectionAction.Trigger? {
        val key = "$triggerType:$target:${ref.blockId}"
        val last = lastFired[key]
        if (last != null && now - last < debounceMs) return null
        pruneDebounce(now)
        lastFired[key] = now
        return DetectionAction.Trigger(
            DetectionTrigger(
                triggerType = triggerType,
                target = target,
                blockId = ref.blockId,
                blockName = ref.blockName,
                startedAtElapsedMs = startedAtElapsedMs,
            ),
        )
    }

    private fun pruneDebounce(now: Long) {
        if (lastFired.size < MAX_DEBOUNCE_ENTRIES) return
        lastFired.entries.removeAll { now - it.value >= debounceMs }
    }

    private fun touchWindow(windowId: Int, packageName: String): WindowTouch {
        val existing = windows.remove(windowId)
        val packageChanged = existing != null && existing.packageName != packageName
        val reading = if (existing == null || packageChanged) {
            WindowReading(packageName, url = null)
        } else {
            existing
        }
        windows[windowId] = reading
        if (windows.size > MAX_CACHED_WINDOWS) {
            val eldest = windows.keys.first()
            windows.remove(eldest)
            if (pending?.windowId == eldest) pending = null
        }
        return WindowTouch(reading, packageChanged)
    }

    private data class WindowTouch(
        val reading: WindowReading,
        val packageChanged: Boolean,
    )

    companion object {
        // Prototype reference value (PRD §13): the settle keeps a block
        // from firing mid-typing. Tunable, not a platform limit.
        const val SETTLE_DELAY_MS = 2000L

        // Until Phase 6 consumes PauseRequested and grants temporary access,
        // this window prevents an immediate re-trigger loop after the Phase 5
        // interruption is dismissed. Tunable.
        const val REPEAT_TRIGGER_DEBOUNCE_MS = 10_000L

        private const val MAX_CACHED_WINDOWS = 16
        private const val MAX_DEBOUNCE_ENTRIES = 64
    }
}

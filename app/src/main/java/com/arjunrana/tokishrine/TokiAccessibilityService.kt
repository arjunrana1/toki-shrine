package com.arjunrana.tokishrine

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.arjunrana.tokishrine.data.repo.EventRepository
import com.arjunrana.tokishrine.detection.ActiveBlockIndex
import com.arjunrana.tokishrine.detection.BlockRef
import com.arjunrana.tokishrine.detection.DetectionAction
import com.arjunrana.tokishrine.detection.DetectionEngine
import com.arjunrana.tokishrine.detection.DetectionTrigger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

/*
 * Phase 4 detection engine adapter (PRD §§10/13/14): foreground-app
 * detection for enabled blocked apps and address-bar reading for the
 * supported browsers in the bundled JSON map. All timing and state rules
 * live in DetectionEngine; this class only translates accessibility
 * events into engine inputs and executes the returned actions.
 *
 * The runtime package filter (setServiceInfo) is enabled blocked apps
 * plus supported browsers, so the service is idle for everything else.
 * Unsupported browsers and in-app webviews are never in the filter and
 * never send events. Toki Shrine's own events are dropped on arrival —
 * the interruption Activity itself must not re-trigger detection.
 */
class TokiAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val settleHandler = Handler(Looper.getMainLooper())
    private val disconnectLogged = AtomicBoolean(false)

    private var engine: DetectionEngine? = null

    @Volatile
    private var browserViewIds: Map<String, String> = emptyMap()

    @Volatile
    private var appBlocks: Map<String, BlockRef> = emptyMap()

    private var settleCallback: Runnable? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        val app = application as? TokiApplication ?: return
        engine?.let(app.detectionCoordinator::detach)
        engine = DetectionEngine(clock = SystemClock::elapsedRealtime).also(app.detectionCoordinator::attach)
        // A service reconnect is an enforcement boundary. Resolve any
        // elapsed-realtime deadline that crossed while uptime callbacks were
        // delayed before constructing the detection index.
        app.pauseCoordinator.evaluate()
        logServiceEvent { log(EventRepository.EVENT_ACCESSIBILITY_CONNECTED) }

        // Bundled JSON (PRD §13): supported browsers plus the OEM battery
        // text, behind the replaceable loader. A failed load degrades to
        // app-only detection rather than crashing the service.
        serviceScope.launch {
            runCatching { app.detectionConfigLoader.load() }
                .onFailure {
                    Log.w(TAG, "Detection config failed to load; site detection disabled", it)
                }
                .onSuccess { browserViewIds = it.browsers }
            applyServiceInfoOnMain()
        }

        // Match maps follow the database and the live pause set (Phase 6):
        // turn a block off and it leaves detection with the next emission —
        // no event-path DB reads — while a paused block's targets are open
        // for the pause and return the same way at re-arm.
        serviceScope.launch {
            var lastPausedIds: Set<Long> = emptySet()
            combine(
                app.blockRepository.observeBlocksWithContents().distinctUntilChanged(),
                app.pauseCoordinator.pauses,
            ) { blocks, pauses ->
                ActiveBlockIndex.from(
                    blocks,
                    excludePackage = packageName,
                    pausedBlockIds = pauses.keys,
                ) to pauses.keys
            }.collect { (index, pausedIds) ->
                appBlocks = index.apps
                engine?.onBlocksChanged(index.apps, index.sites)?.forEach(::execute)
                applyServiceInfoOnMain()
                if (pausedIds != lastPausedIds) {
                    lastPausedIds = pausedIds
                    // A pause started or ended: the foreground window's
                    // enforcement changed, so it is re-evaluated once
                    // without waiting for a fresh window event. This is
                    // what makes re-arm immediate even while a blocked app
                    // is already on screen (PRD §4: re-arms immediately).
                    withContext(Dispatchers.Main) { reevaluateActiveWindow() }
                }
            }
        }
    }

    // Feeds the current foreground window through the engine exactly like a
    // TYPE_WINDOW_STATE_CHANGED event: a paused-then-re-armed app in front
    // triggers its gate now, not on the next window change.
    private fun reevaluateActiveWindow() {
        val activeEngine = engine ?: return
        val root = rootInActiveWindow ?: return
        val packageName = root.packageName?.toString() ?: return
        if (packageName == this.packageName) return
        activeEngine.onWindowStateChanged(root.windowId, packageName).forEach(::execute)
        if (browserViewIds.containsKey(packageName)) {
            readAddressBar(activeEngine, root.windowId, packageName, canaryOnMissing = true)
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        // Handler delays use uptime and can be postponed by deep sleep. Every
        // real enforcement event first advances the elapsed-realtime pause
        // registry; an expiry emission then rebuilds the index and re-feeds
        // the active window exactly once.
        (application as? TokiApplication)?.pauseCoordinator?.evaluate()
        val activeEngine = engine ?: return
        val packageName = event.packageName?.toString() ?: return
        if (packageName == this.packageName) return
        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                activeEngine.onWindowStateChanged(event.windowId, packageName).forEach(::execute)
                if (browserViewIds.containsKey(packageName)) {
                    readAddressBar(activeEngine, event.windowId, packageName, canaryOnMissing = true)
                }
            }
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                if (browserViewIds.containsKey(packageName)) {
                    readAddressBar(activeEngine, event.windowId, packageName, canaryOnMissing = false)
                }
            }
        }
    }

    override fun onInterrupt() = Unit

    // System stopped the service (disabled or unbound). Best effort: the
    // write runs detached so the immediate onDestroy cannot cancel it.
    override fun onUnbind(intent: Intent?): Boolean {
        if (disconnectLogged.compareAndSet(false, true)) {
            (application as? TokiApplication)?.let { app ->
                CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                    runCatching { app.eventRepository.log(EventRepository.EVENT_ACCESSIBILITY_DISCONNECTED) }
                }
            }
        }
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        val app = application as? TokiApplication
        engine?.let { app?.detectionCoordinator?.detach(it) }
        engine = null
        serviceScope.cancel()
        cancelSettle()
        super.onDestroy()
    }

    // — event → engine translation —

    private fun readAddressBar(
        engine: DetectionEngine,
        windowId: Int,
        packageName: String,
        canaryOnMissing: Boolean,
    ) {
        val viewId = browserViewIds[packageName] ?: return
        // The active root is the address bar's window in the normal
        // single-foreground-browser case; a package mismatch (event from a
        // background window) means there is nothing reliable to read.
        val root = rootInActiveWindow?.takeIf { it.packageName?.toString() == packageName }
        readAddressBarFromRoot(engine, windowId, packageName, viewId, root, canaryOnMissing)
    }

    private fun readAddressBarFromRoot(
        engine: DetectionEngine,
        windowId: Int,
        packageName: String,
        viewId: String,
        root: AccessibilityNodeInfo?,
        canaryOnMissing: Boolean,
    ) {
        val node = root?.findAccessibilityNodeInfosByViewId(viewId)?.firstOrNull()
        if (node == null) {
            if (canaryOnMissing) {
                // §10 silent-failure canary: a full window from a supported
                // browser with no address-bar node counts as a read
                // failure. Mid-scroll content changes (bar hidden) stay
                // quiet so the cache keeps the last known URL.
                engine.onAddressBarMissing(windowId, packageName).forEach(::execute)
            }
            return
        }
        val focused = node.isFocused || node.isAccessibilityFocused
        val text = node.text?.toString() ?: node.contentDescription?.toString()
        engine.onBrowserAddressReading(windowId, packageName, text, focused).forEach(::execute)
    }

    // — action execution —

    private fun execute(action: DetectionAction) {
        when (action) {
            is DetectionAction.ScheduleSettle -> scheduleSettle(action.generation, action.delayMs)
            is DetectionAction.CancelSettle -> cancelSettle()
            is DetectionAction.Trigger -> launchBlockScreen(action.trigger)
            is DetectionAction.UrlReadFailed ->
                logServiceEvent {
                    log(EventRepository.EVENT_URL_READ_FAILED, params = mapOf("browser_package" to action.browserPackage))
                }
        }
    }

    private fun scheduleSettle(generation: Long, delayMs: Long) {
        cancelSettle()
        val runnable = Runnable {
            settleCallback = null
            val activeEngine = engine ?: return@Runnable
            val activeRoot = rootInActiveWindow
            val activePackage = activeRoot?.packageName?.toString()
            val activeWindowId = activeRoot?.windowId
            val viewId = activePackage?.let(browserViewIds::get)
            if (activeRoot != null && activePackage != null && activeWindowId != null && viewId != null) {
                // Refresh focus/text from the actual foreground root at the
                // settle boundary. A hidden node preserves the cached URL;
                // a changed/focused value cancels or replaces the old work.
                readAddressBarFromRoot(
                    activeEngine,
                    activeWindowId,
                    activePackage,
                    viewId,
                    activeRoot,
                    canaryOnMissing = false,
                )
            }
            activeEngine.onSettleElapsed(
                generation,
                activeWindowId = activeWindowId,
                activePackageName = activePackage,
            ).forEach(::execute)
        }
        settleCallback = runnable
        settleHandler.postDelayed(runnable, delayMs)
    }

    private fun cancelSettle() {
        settleCallback?.let(settleHandler::removeCallbacks)
        settleCallback = null
    }

    private fun launchBlockScreen(trigger: DetectionTrigger) {
        // Phase 6 pause access: a paused block's targets are open (PRD §4).
        // The guard is synchronous against the coordinator so a trigger
        // racing the index re-emission right after a pause starts (or right
        // before its re-arm) is still decided by the pause state itself.
        val app = application as? TokiApplication
        if (app?.pauseCoordinator?.isPaused(trigger.blockId) == true) return
        val intent = Intent(this, BlockActivity::class.java)
            .addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP,
            )
            .putExtra(BlockActivity.EXTRA_TRIGGER_TYPE, trigger.triggerType)
            .putExtra(BlockActivity.EXTRA_TARGET, trigger.target)
            .putExtra(BlockActivity.EXTRA_BLOCK_NAME, trigger.blockName)
            .putExtra(BlockActivity.EXTRA_BLOCK_ID, trigger.blockId)
            .putExtra(BlockActivity.EXTRA_STARTED_AT_ELAPSED_MS, trigger.startedAtElapsedMs)
        // Accessibility services are exempt from background-activity-launch
        // restrictions (PRD §14), so no overlay is involved. The §10
        // BlockActivity records block_screen_shown only after it reaches a
        // resumed lifecycle. An accepted request is not itself a shown UI.
        runCatching { startActivity(intent) }
    }

    private fun logServiceEvent(write: suspend EventRepository.() -> Unit) {
        val app = application as? TokiApplication ?: return
        serviceScope.launch { runCatching { app.eventRepository.write() } }
    }

    // — runtime package filter —

    private suspend fun applyServiceInfoOnMain() {
        withContext(Dispatchers.Main) {
            applyServiceInfo()
        }
    }

    private fun applyServiceInfo() {
        val packages = (browserViewIds.keys + appBlocks.keys).distinct()
        serviceInfo = serviceInfo.apply {
            // null (empty relevant set) means no filtering; with an empty
            // map the engine matches nothing anyway, so the moment is
            // harmless and the next emission narrows it again.
            packageNames = if (packages.isEmpty()) null else packages.toTypedArray()
        }
    }

    private companion object {
        const val TAG = "TokiAccessibilityService"
    }
}

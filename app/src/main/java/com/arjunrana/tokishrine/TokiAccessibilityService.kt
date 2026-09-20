package com.arjunrana.tokishrine

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
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
 * the placeholder itself must not re-trigger detection.
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
        engine = DetectionEngine()
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

        // Match maps follow the database: turn a block off and it leaves
        // detection with the next emission — no event-path DB reads.
        serviceScope.launch {
            app.blockRepository.observeBlocksWithContents().distinctUntilChanged().collect { blocks ->
                val index = ActiveBlockIndex.from(blocks, excludePackage = packageName)
                appBlocks = index.apps
                engine?.onBlocksChanged(index.apps, index.sites)
                applyServiceInfoOnMain()
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
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
        val root = rootInActiveWindow?.takeIf { it.packageName == packageName }
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
            is DetectionAction.Trigger -> launchPlaceholder(action.trigger)
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
            engine?.onSettleElapsed(generation)?.forEach(::execute)
        }
        settleCallback = runnable
        settleHandler.postDelayed(runnable, delayMs)
    }

    private fun cancelSettle() {
        settleCallback?.let(settleHandler::removeCallbacks)
        settleCallback = null
    }

    private fun launchPlaceholder(trigger: DetectionTrigger) {
        val intent = Intent(this, BlockActivity::class.java)
            .addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP,
            )
            .putExtra(BlockActivity.EXTRA_TRIGGER_TYPE, trigger.triggerType)
            .putExtra(BlockActivity.EXTRA_TARGET, trigger.target)
            .putExtra(BlockActivity.EXTRA_BLOCK_NAME, trigger.blockName)
        // Accessibility services are exempt from background-activity-launch
        // restrictions (PRD §14), so no overlay is involved. The §10
        // block_screen_shown event is written only when the launch was
        // accepted — a failed start shows nothing and logs nothing.
        if (runCatching { startActivity(intent) }.isSuccess) {
            logServiceEvent {
                log(
                    EventRepository.EVENT_BLOCK_SCREEN_SHOWN,
                    blockId = trigger.blockId,
                    target = trigger.target,
                    targetType = trigger.triggerType,
                    params = mapOf(
                        "trigger_type" to trigger.triggerType,
                        "latency_ms" to trigger.latencyMs,
                    ),
                )
            }
        }
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

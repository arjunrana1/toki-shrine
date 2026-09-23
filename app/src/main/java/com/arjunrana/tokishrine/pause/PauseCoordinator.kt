package com.arjunrana.tokishrine.pause

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.core.content.ContextCompat
import com.arjunrana.tokishrine.data.repo.BlockRepository
import com.arjunrana.tokishrine.data.repo.EventRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Process owner of the live pause set (Phase 6). The pure [PauseRegistry]
 * holds every rule; this coordinator translates it into process state:
 *
 * - [startPause] publishes the new pause synchronously on the main thread,
 *   so the moment a completed challenge returns the user to the triggering
 *   app, detection already treats the block as open. `pause_started` and the
 *   PauseService start follow.
 * - Expiry evaluation is scheduled at the soonest monotonic deadline and can
 *   also be poked by [evaluate] from timers or UI; each expired pause logs
 *   `pause_expired` exactly once and re-publishes, which re-arms detection,
 *   drops the notification and refreshes the bubble.
 * - Blocks turned off or deleted mid-pause lose their pause silently (no
 *   `pause_expired`) through the enabled-blocks observation.
 *
 * Pause state is deliberately process-local: process death or reboot ends
 * every pause (the block re-arms — the safe direction). Deep sleep can
 * delay the expiry callback, but the deadline itself is monotonic, so a
 * pause can only ever end at or after its true duration.
 */
class PauseCoordinator(
    context: Context,
    private val eventRepository: EventRepository,
    blockRepository: BlockRepository,
    private val applicationScope: CoroutineScope,
    private val clock: () -> Long = SystemClock::elapsedRealtime,
    private val mainHandler: Handler = Handler(Looper.getMainLooper()),
) {
    private val appContext = context.applicationContext
    private val registry = PauseRegistry()

    private val _pauses = MutableStateFlow<Map<Long, PauseState>>(emptyMap())

    /** Snapshots keyed by block id; emissions carry the full live set. */
    val pauses: StateFlow<Map<Long, PauseState>> = _pauses.asStateFlow()

    private val expiryCallback = Runnable { evaluate() }

    init {
        applicationScope.launch {
            blockRepository.observeBlocksWithContents()
                .map { blocks -> blocks.filter { it.block.enabled }.map { it.block.id }.toSet() }
                .distinctUntilChanged()
                .collect { liveBlockIds ->
                    if (registry.retainBlocks(liveBlockIds).isNotEmpty()) publish()
                }
        }
    }

    /**
     * Synchronous access decision for the detection launch path. Deadline
     * expiry is resolved before answering, so stale membership cannot keep a
     * block open when an uptime-based Handler callback was delayed by sleep.
     */
    fun isPaused(blockId: Long): Boolean {
        val evaluation = registry.evaluateForAccess(blockId, clock())
        dispatchEvaluation(evaluation.expirations)
        return evaluation.isPaused
    }

    /**
     * Consumes the Phase 5 `PauseRequested` seam: the block's targets are
     * open the moment this returns (on the main thread), before the user is
     * returned to the triggering app.
     */
    fun startPause(blockId: Long, minutes: Int) {
        if (Looper.myLooper() == mainHandler.looper) {
            startPauseOnMain(blockId, minutes)
        } else {
            mainHandler.post { startPauseOnMain(blockId, minutes) }
        }
    }

    private fun startPauseOnMain(blockId: Long, minutes: Int) {
        val started = registry.start(blockId, minutes, clock())
        _pauses.value = registry.snapshot()
        applicationScope.launch {
            runCatching {
                eventRepository.log(
                    EventRepository.EVENT_PAUSE_STARTED,
                    blockId = started.blockId,
                    params = mapOf("minutes" to started.minutes),
                )
            }
        }
        scheduleNextExpiry()
        ensureServiceRunning()
    }

    /**
     * Ends every pause past its deadline. Idempotent; safe to call from
     * timers, service renders or the accessibility service. Each expiration
     * is emitted by the registry exactly once.
     */
    fun evaluate() {
        if (Looper.myLooper() == mainHandler.looper) {
            evaluateOnMain()
        } else {
            mainHandler.post { evaluateOnMain() }
        }
    }

    private fun evaluateOnMain() {
        val expired = registry.advance(clock())
        applyExpirationsOnMain(expired)
    }

    /**
     * Publishes/logs expirations on the main thread. Registry removal is the
     * exactly-once gate: duplicate timer, wake and enforcement evaluations
     * arrive here with an empty list and cannot emit a second re-arm/event.
     */
    private fun dispatchEvaluation(expired: List<PauseExpiration>) {
        if (Looper.myLooper() == mainHandler.looper) {
            applyExpirationsOnMain(expired)
        } else {
            mainHandler.post { applyExpirationsOnMain(expired) }
        }
    }

    private fun applyExpirationsOnMain(expired: List<PauseExpiration>) {
        if (expired.isEmpty()) {
            scheduleNextExpiry()
            return
        }
        _pauses.value = registry.snapshot()
        expired.forEach { expiration ->
            applicationScope.launch {
                runCatching {
                    eventRepository.log(EventRepository.EVENT_PAUSE_EXPIRED, blockId = expiration.state.blockId)
                }
            }
        }
        scheduleNextExpiry()
    }

    private fun publish() {
        if (Looper.myLooper() == mainHandler.looper) {
            _pauses.value = registry.snapshot()
            scheduleNextExpiry()
        } else {
            mainHandler.post {
                _pauses.value = registry.snapshot()
                scheduleNextExpiry()
            }
        }
    }

    private fun scheduleNextExpiry() {
        mainHandler.removeCallbacks(expiryCallback)
        val delayMs = registry.soonest()?.remainingMs(clock()) ?: return
        // postDelayed counts awake uptime, so deep sleep can only delay the
        // callback; evaluateOnMain re-reads the monotonic clock, never
        // firing early.
        mainHandler.postDelayed(expiryCallback, delayMs + EXPIRY_GRACE_MS)
    }

    private fun ensureServiceRunning() {
        val started = runCatching {
            ContextCompat.startForegroundService(appContext, Intent(appContext, PauseService::class.java))
        }
        if (started.isFailure && registry.snapshot().isNotEmpty()) {
            // The service could not be asked to start (rare OEM/restriction
            // path). The pause itself is already live in-process; only the
            // hosted notification/bubble would be missing until the next
            // evaluate/start finds the service able to run.
            _pauses.value = registry.snapshot()
        }
    }

    private companion object {
        // Never evaluate a hair before the deadline's millisecond boundary.
        const val EXPIRY_GRACE_MS = 1L
    }
}

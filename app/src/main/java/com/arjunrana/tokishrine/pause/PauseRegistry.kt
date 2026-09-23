package com.arjunrana.tokishrine.pause

import com.arjunrana.tokishrine.ui.interruption.formatClock
import kotlin.math.ceil

/**
 * One block's temporary access window. Both timestamps are monotonic
 * (elapsedRealtime semantics, PRD §7.3): the device clock can move freely
 * without changing what these mean. Immutable, so snapshots can travel
 * through StateFlows and to observers.
 */
data class PauseState(
    val blockId: Long,
    val startedAtMs: Long,
    val deadlineMs: Long,
    val minutes: Int,
) {
    fun remainingMs(nowMs: Long): Long = (deadlineMs - nowMs).coerceIn(0L, deadlineMs - startedAtMs)
}

/** A pause that ended by reaching its deadline. Delivered exactly once. */
data class PauseExpiration(val state: PauseState)

/**
 * Atomic answer for an enforcement decision. Any overdue pauses are removed
 * before [isPaused] is computed, so a delayed scheduler callback can never
 * make stale registry membership grant access past the monotonic deadline.
 */
data class PauseAccessEvaluation(
    val isPaused: Boolean,
    val expirations: List<PauseExpiration>,
)

/**
 * Pure owner of which blocks are paused and when each pause ends (PRD §4:
 * a pause temporarily opens one block and re-arms automatically; pauses
 * cannot be extended). Every input is an injectable monotonic timestamp, so
 * device-clock changes cannot shorten or extend anything here.
 *
 * Invariants:
 * - One pause per block; starting again restarts that pause's deadline
 *   rather than extending it. Detection cannot offer a second challenge for
 *   a paused block, so this path only guards against races.
 * - [advance] ends each expired pause exactly once and is idempotent against
 *   duplicate or delayed evaluation callbacks.
 * - Blocks turned off or deleted mid-pause leave through [retainBlocks],
 *   which is not an expiry and emits no expiration.
 */
class PauseRegistry {

    private val pauses = LinkedHashMap<Long, PauseState>()

    @Synchronized
    fun start(blockId: Long, minutes: Int, nowMs: Long): PauseState {
        val state = PauseState(
            blockId = blockId,
            startedAtMs = nowMs,
            deadlineMs = nowMs + minutes * MILLIS_PER_MINUTE,
            minutes = minutes,
        )
        pauses[blockId] = state
        return state
    }

    @Synchronized
    fun advance(nowMs: Long): List<PauseExpiration> = advanceLocked(nowMs)

    /**
     * Resolves deadline expiry and one block's access decision under the same
     * lock. This is the enforcement backstop when an uptime-based Android
     * callback was delayed by deep sleep.
     */
    @Synchronized
    fun evaluateForAccess(blockId: Long, nowMs: Long): PauseAccessEvaluation {
        val expirations = advanceLocked(nowMs)
        return PauseAccessEvaluation(
            isPaused = pauses.containsKey(blockId),
            expirations = expirations,
        )
    }

    private fun advanceLocked(nowMs: Long): List<PauseExpiration> {
        val expired = ArrayList<PauseExpiration>()
        val iterator = pauses.entries.iterator()
        while (iterator.hasNext()) {
            val state = iterator.next().value
            if (nowMs >= state.deadlineMs) {
                expired += PauseExpiration(state)
                iterator.remove()
            }
        }
        return expired
    }

    @Synchronized
    fun isPaused(blockId: Long): Boolean = pauses.containsKey(blockId)

    @Synchronized
    fun pausedBlockIds(): Set<Long> = pauses.keys.toSet()

    @Synchronized
    fun snapshot(): Map<Long, PauseState> = pauses.toMap()

    @Synchronized
    fun soonest(): PauseState? = pauses.values.minByOrNull { it.deadlineMs }

    /** Drops pauses whose block no longer exists or is no longer ON. */
    @Synchronized
    fun retainBlocks(liveBlockIds: Set<Long>): List<Long> {
        val removed = ArrayList<Long>()
        val iterator = pauses.entries.iterator()
        while (iterator.hasNext()) {
            val blockId = iterator.next().key
            if (blockId !in liveBlockIds) {
                removed += blockId
                iterator.remove()
            }
        }
        return removed
    }

    companion object {
        const val MILLIS_PER_MINUTE = 60_000L
    }
}

/**
 * Pure presentation math shared by the bubble and notification. Remaining
 * time always derives from the monotonic deadline; the wall clock appears
 * only when anchoring the notification chronometer at (re-)post time.
 */
object PauseFormat {

    /** Remaining pause time as m:ss — 0:47, 12:04 (screens 20/21). */
    fun clockText(remainingMs: Long): String = formatClock(ceil(remainingMs / 1000.0).toInt())

    /**
     * The notification `when` that makes the system chronometer count down
     * [remainingMonotonicMs] from now. The system anchors chronometers on
     * the monotonic clock at post time, so re-posting after a device-clock
     * jump simply re-derives the same remaining time — the countdown cannot
     * be shortened by moving the clock.
     */
    fun chronometerWhen(currentTimeMillis: Long, remainingMonotonicMs: Long): Long =
        currentTimeMillis + remainingMonotonicMs

    /** Elapsed fraction of a pause as per-mille of its full duration, 0..1000. */
    fun elapsedPerMille(state: PauseState, nowMs: Long): Int {
        val total = state.deadlineMs - state.startedAtMs
        if (total <= 0L) return 1000
        return (((nowMs - state.startedAtMs).coerceIn(0L, total)) * 1000L / total).toInt()
    }
}

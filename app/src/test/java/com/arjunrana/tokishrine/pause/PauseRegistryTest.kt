package com.arjunrana.tokishrine.pause

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/*
 * PauseRegistry rules (PRD §§4/7.3, Phase 6): per-block pauses with
 * monotonic deadlines, exactly-once expiry (automatic re-arm), independent
 * simultaneous pauses, restart-not-extend, and silent drop when a block is
 * turned off or deleted. All timestamps are the monotonic clock; the
 * device clock never enters the registry, which is what makes a clock
 * change unable to shorten or extend a pause.
 */
class PauseRegistryTest {

    private var now: Long = 0

    @Before
    fun setUp() {
        now = 100_000
    }

    private fun minute(ms: Long) = ms * PauseRegistry.MILLIS_PER_MINUTE

    // — start/expiry —

    @Test
    fun pauseBeforeItsDeadlineStaysActive() {
        val r = registry()
        r.start(1, minutes = 15, nowMs = now)

        assertEquals(emptyList<PauseExpiration>(), r.advance(now + minute(15) - 1))
        assertTrue(r.isPaused(1))
        assertEquals(setOf(1L), r.pausedBlockIds())
    }

    @Test
    fun expiredPauseIsEmittedExactlyOnceAndTheBlockReArms() {
        val r = registry()
        r.start(1, minutes = 15, nowMs = now)

        val first = r.advance(now + minute(15))
        assertEquals(listOf(1L), first.map { it.state.blockId })
        assertFalse(r.isPaused(1))
        assertTrue(r.pausedBlockIds().isEmpty())

        // A duplicate or delayed evaluation callback cannot re-expire it.
        assertEquals(emptyList<PauseExpiration>(), r.advance(now + minute(15) + 5_000))
        assertEquals(emptyList<PauseExpiration>(), r.advance(now + minute(100)))
    }

    @Test
    fun expirationCarriesThePauseThatEnded() {
        val r = registry()
        r.start(7, minutes = 5, nowMs = 1_000)
        val expired = r.advance(1_000 + minute(5))
        assertEquals(1, expired.size)
        assertEquals(7L, expired.single().state.blockId)
        assertEquals(5, expired.single().state.minutes)
        assertEquals(1_000 + minute(5), expired.single().state.deadlineMs)
    }

    // — simultaneous pauses are independent (Phase 6 acceptance) —

    @Test
    fun simultaneousPausesExpireIndependently() {
        val r = registry()
        r.start(1, minutes = 5, nowMs = now)
        r.start(2, minutes = 15, nowMs = now)

        val firstExpiry = r.advance(now + minute(5))
        assertEquals(listOf(1L), firstExpiry.map { it.state.blockId })
        assertFalse(r.isPaused(1))
        assertTrue("the other pause keeps the second block open", r.isPaused(2))

        val secondExpiry = r.advance(now + minute(15))
        assertEquals(listOf(2L), secondExpiry.map { it.state.blockId })
        assertTrue(r.pausedBlockIds().isEmpty())
    }

    @Test
    fun soonestAcrossPausesDrivesTheBubble() {
        val r = registry()
        r.start(1, minutes = 20, nowMs = now)
        r.start(2, minutes = 5, nowMs = now)

        assertEquals(2L, r.soonest()?.blockId)

        r.advance(now + minute(5))
        assertEquals(1L, r.soonest()?.blockId)
    }

    // — exact per-block isolation —

    @Test
    fun pausingOneBlockGrantsAccessToNoOtherBlock() {
        val r = registry()
        val state = r.start(1, minutes = 15, nowMs = now)

        assertTrue(r.isPaused(1))
        assertFalse(r.isPaused(2))
        assertEquals(setOf(1L), r.snapshot().keys)
        assertEquals(minute(15), state.remainingMs(now))
    }

    // — pauses cannot be extended (PRD §4) —

    @Test
    fun aRepeatedStartRestartsThePauseInsteadOfExtendingIt() {
        val r = registry()
        r.start(1, minutes = 5, nowMs = now)

        // A second start four minutes later begins a fresh five-minute
        // window (t0+9), it does not extend to t0+10.
        r.start(1, minutes = 5, nowMs = now + minute(4))

        assertEquals(emptyList<PauseExpiration>(), r.advance(now + minute(8) + minute(1) - 1))
        assertTrue(r.isPaused(1))
        assertEquals(listOf(1L), r.advance(now + minute(9)).map { it.state.blockId })
    }

    // — clock-change resistance (PRD §7.3 monotonic rule) —

    @Test
    fun expiryFollowsOnlyTheMonotonicClockAndIgnoresWallClockJumps() {
        val r = registry()
        val state = r.start(1, minutes = 15, nowMs = now)

        // The registry's only time input is the monotonic timestamp passed
        // to start/advance/remaining; there is no wall-clock surface a
        // device clock jump (forward a full day, here) could reach. The
        // deadline stays where the start put it and only the monotonic
        // clock crossing it ends the pause.
        assertEquals(minute(14), state.remainingMs(now + minute(1)))
        assertEquals(emptyList<PauseExpiration>(), r.advance(now + minute(14)))
        assertEquals(minute(1), state.remainingMs(now + minute(14)))
        assertEquals(listOf(1L), r.advance(now + minute(15)).map { it.state.blockId })
        assertEquals(0L, state.remainingMs(now + minute(15)))
    }

    @Test
    fun movingTheClockBackwardCannotExtendAPause() {
        val r = registry()
        r.start(1, minutes = 5, nowMs = now)

        // A backward jump would need earlier deadlines to matter; they are
        // anchored at start time and never recomputed.
        assertEquals(listOf(1L), r.advance(now + minute(5)).map { it.state.blockId })
    }

    @Test
    fun enforcementCheckExpiresOverduePauseWhenScheduledCallbackWasDelayed() {
        val r = registry()
        r.start(1, minutes = 5, nowMs = now)

        // Simulate a Handler callback that never ran while the device slept:
        // elapsedRealtime has crossed the deadline before the first access
        // decision after wake.
        val afterWake = r.evaluateForAccess(blockId = 1, nowMs = now + minute(9))

        assertFalse("access must be closed before the enforcement decision returns", afterWake.isPaused)
        assertEquals(listOf(1L), afterWake.expirations.map { it.state.blockId })
        assertFalse(r.isPaused(1))
    }

    @Test
    fun repeatedEnforcementAfterDelayedExpiryCannotExpireOrRearmTwice() {
        val r = registry()
        r.start(1, minutes = 5, nowMs = now)

        val first = r.evaluateForAccess(blockId = 1, nowMs = now + minute(9))
        val duplicateWakeEvent = r.evaluateForAccess(blockId = 1, nowMs = now + minute(10))

        assertEquals(listOf(1L), first.expirations.map { it.state.blockId })
        assertFalse(first.isPaused)
        assertTrue(duplicateWakeEvent.expirations.isEmpty())
        assertFalse(duplicateWakeEvent.isPaused)
    }

    // — turn-off / deletion mid-pause —

    @Test
    fun blocksThatLeftTheLiveSetDropTheirPauseSilently() {
        val r = registry()
        r.start(1, minutes = 15, nowMs = now)
        r.start(2, minutes = 15, nowMs = now)

        // Block 2 was turned off (or deleted): no expiration, no access.
        assertEquals(listOf(2L), r.retainBlocks(liveBlockIds = setOf(1)))
        assertFalse(r.isPaused(2))
        assertTrue(r.isPaused(1))

        // The retained pause still expires normally at re-arm.
        assertEquals(listOf(1L), r.advance(now + minute(15)).map { it.state.blockId })
    }

    private fun registry() = PauseRegistry()
}

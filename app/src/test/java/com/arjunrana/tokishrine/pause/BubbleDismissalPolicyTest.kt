package com.arjunrana.tokishrine.pause

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BubbleDismissalPolicyTest {

    private val pauseA = 1L to 500_000L
    private val pauseB = 2L to 700_000L

    @Test
    fun `dismissal hides the bubble while only dismissed instances stay live`() {
        val policy = BubbleDismissalPolicy()
        policy.recordDismissal(snapshotOf(pauseA, pauseB))
        assertFalse(policy.shouldShow(listOf(pauseA)))
        assertFalse(policy.shouldShow(listOf(pauseB)))
        assertFalse(policy.shouldShow(listOf(pauseA, pauseB)))
    }

    @Test
    fun `a pause that was not visible at dismissal shows the bubble`() {
        val policy = BubbleDismissalPolicy()
        policy.recordDismissal(snapshotOf(pauseA))
        assertTrue(policy.shouldShow(listOf(pauseA, pauseB)))
        assertTrue(policy.shouldShow(listOf(pauseB)))
    }

    @Test
    fun `a restarted or fresh pause of the same block carries a new identity`() {
        val policy = BubbleDismissalPolicy()
        policy.recordDismissal(snapshotOf(pauseA))
        // Restart-not-extend and re-pauses derive a new monotonic deadline,
        // so the bubble returns for the new pause instance.
        assertTrue(policy.shouldShow(listOf(1L to 900_000L)))
    }

    @Test
    fun `an empty live set resets the memory`() {
        val policy = BubbleDismissalPolicy()
        policy.recordDismissal(snapshotOf(pauseA))
        policy.resetIfIdle(emptyList())
        assertTrue(policy.shouldShow(listOf(pauseA)))
    }

    @Test
    fun `a non-empty live set keeps the memory`() {
        val policy = BubbleDismissalPolicy()
        policy.recordDismissal(snapshotOf(pauseA))
        policy.resetIfIdle(listOf(pauseA))
        assertFalse(policy.shouldShow(listOf(pauseA)))
    }

    @Test
    fun `a pause arriving between dismissal decision and animation completion shows the bubble`() {
        // P6C-R1 regression: the snapshot is captured at release, before the
        // cosmetic 140 ms exit animation; a pause starting during that
        // interval must not be recorded as already dismissed when the
        // snapshot is applied afterwards.
        val policy = BubbleDismissalPolicy()
        val snapshot = snapshotOf(pauseA)
        val liveAfterAnimation = listOf(pauseA, pauseB) // pauseB started mid-animation
        policy.recordDismissal(snapshot)
        assertTrue(policy.shouldShow(liveAfterAnimation))
    }

    @Test
    fun `the snapshot ignores pause state that changes after the decision`() {
        // P6C-R1: bubble_dismissed attribution and dismissal memory read the
        // captured decision, so a pause (or displayed-block change) arriving
        // after release cannot fold into it.
        val liveAtRelease = mutableListOf(pauseA)
        val snapshot = BubbleDismissalSnapshot(blockId = 1L, keys = liveAtRelease)
        liveAtRelease.add(pauseB)
        assertEquals(listOf(pauseA), snapshot.activePauseKeys)
        assertEquals(1L, snapshot.blockId)
    }

    private fun snapshotOf(vararg keys: Pair<Long, Long>): BubbleDismissalSnapshot =
        BubbleDismissalSnapshot(blockId = 1L, keys = keys.toList())
}

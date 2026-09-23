package com.arjunrana.tokishrine.pause

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BubbleDismissalPolicyTest {

    private val pauseA = 1L to 500_000L
    private val pauseB = 2L to 700_000L

    @Test
    fun `dismissal hides the bubble while only dismissed instances stay live`() {
        val policy = BubbleDismissalPolicy()
        policy.recordDismissal(listOf(pauseA, pauseB))
        assertFalse(policy.shouldShow(listOf(pauseA)))
        assertFalse(policy.shouldShow(listOf(pauseB)))
        assertFalse(policy.shouldShow(listOf(pauseA, pauseB)))
    }

    @Test
    fun `a pause that was not visible at dismissal shows the bubble`() {
        val policy = BubbleDismissalPolicy()
        policy.recordDismissal(listOf(pauseA))
        assertTrue(policy.shouldShow(listOf(pauseA, pauseB)))
        assertTrue(policy.shouldShow(listOf(pauseB)))
    }

    @Test
    fun `a restarted or fresh pause of the same block carries a new identity`() {
        val policy = BubbleDismissalPolicy()
        policy.recordDismissal(listOf(pauseA))
        // Restart-not-extend and re-pauses derive a new monotonic deadline,
        // so the bubble returns for the new pause instance.
        assertTrue(policy.shouldShow(listOf(1L to 900_000L)))
    }

    @Test
    fun `an empty live set resets the memory`() {
        val policy = BubbleDismissalPolicy()
        policy.recordDismissal(listOf(pauseA))
        policy.resetIfIdle(emptyList())
        assertTrue(policy.shouldShow(listOf(pauseA)))
    }

    @Test
    fun `a non-empty live set keeps the memory`() {
        val policy = BubbleDismissalPolicy()
        policy.recordDismissal(listOf(pauseA))
        policy.resetIfIdle(listOf(pauseA))
        assertFalse(policy.shouldShow(listOf(pauseA)))
    }
}

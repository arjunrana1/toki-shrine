package com.arjunrana.tokishrine.ui.util

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// Pins the terminal-action contract (review blocker 2): single-flight
// across competing taps, after-effects only after a successful commit,
// unchanged stored values skip the changed-effect, failures release the
// guard without side effects, and an accepted commit survives its caller's
// scope being cancelled (a screen leaving composition).
class TerminalActionTest {

    private fun action() = TerminalAction(CoroutineScope(Dispatchers.Unconfined))

    @Test
    fun doubleTapWhileCommitIsPendingRunsExactlyOneTransition() = runBlocking {
        val started = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val effects = mutableListOf<String>()
        val action = action()

        assertTrue(action.run(commit = { started.complete(Unit); release.await(); effects.add("commit"); true },
                           onChanged = { effects.add("haptic") },
                           onCommitted = { effects.add("close") }))
        assertTrue(started.isCompleted)

        // The second rapid tap is synchronously ignored while the first
        // commit is still pending.
        assertFalse(action.run(commit = { error("must not run") }, onChanged = {}, onCommitted = {}))

        release.complete(Unit)
        // Unconfined resumption runs the continuation inline, so the
        // transition has finished by the time complete() returns.
        assertEquals(listOf("commit", "haptic", "close"), effects)
        assertFalse(action.busy)
    }

    @Test
    fun commitFailureEmitsNoEffectsAndReleasesTheGuardForRetry() = runBlocking {
        val effects = mutableListOf<String>()
        val action = action()

        assertTrue(action.run(commit = { effects.add("failed-commit"); error("disk exploded") },
                           onChanged = { effects.add("haptic") },
                           onCommitted = { effects.add("close") }))

        assertEquals(listOf("failed-commit"), effects)
        assertFalse(action.busy)

        // Retry is accepted and can succeed.
        assertTrue(action.run(commit = { true }, onChanged = { effects.add("haptic") },
                              onCommitted = { effects.add("close") }))
        // Unconfined runs the second commit inline to completion.
        assertEquals(listOf("failed-commit", "haptic", "close"), effects)
    }

    @Test
    fun unchangedStoredValueSkipsTheChangedEffectButStillCommits() = runBlocking {
        val effects = mutableListOf<String>()
        val action = action()

        assertTrue(action.run(commit = { false }, onChanged = { effects.add("haptic") },
                              onCommitted = { effects.add("close") }))

        assertEquals(listOf("close"), effects)
    }

    @Test
    fun acceptedCommitSurvivesItsCallerScopeBeingCancelled() = runBlocking {
        // The caller scope stands in for the destination screen's
        // composition scope; cancelling it must not cancel a commit the
        // lifecycle-scoped action already accepted.
        val callerScope = CoroutineScope(Dispatchers.Unconfined)
        val release = CompletableDeferred<Unit>()
        val effects = mutableListOf<String>()
        val action = action()

        assertTrue(action.run(commit = { release.await(); effects.add("commit"); true },
                              onChanged = { effects.add("haptic") },
                              onCommitted = { effects.add("close") }))
        callerScope.cancel()
        release.complete(Unit)

        assertEquals(listOf("commit", "haptic", "close"), effects)
    }

    @Test
    fun effectsRunOnlyAfterTheCommitCompletes() = runBlocking {
        val release = CompletableDeferred<Unit>()
        val order = mutableListOf<String>()
        val action = action()

        assertTrue(action.run(commit = { release.await(); order.add("commit"); true },
                              onChanged = { order.add("haptic") },
                              onCommitted = { order.add("close") }))
        // While the commit is suspended (a Back/Not-yet tap window), no
        // after-effect has run and the guard is held.
        assertTrue(order.isEmpty())
        assertTrue(action.busy)

        release.complete(Unit)
        assertEquals(listOf("commit", "haptic", "close"), order)
    }
}

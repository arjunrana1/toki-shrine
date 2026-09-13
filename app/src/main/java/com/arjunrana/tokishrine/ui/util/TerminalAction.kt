package com.arjunrana.tokishrine.ui.util

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Single-flight runner for terminal (once-only) operations: block
 * activation transitions and onboarding completion (review blocker 2).
 *
 * `commit` runs in the supplied lifecycle scope — the activity's, never a
 * destination screen's — so an accepted commit finishes even if its route
 * leaves composition while the writes are in flight. Competing taps are
 * ignored synchronously while [busy] is true (screens also disable their
 * terminal buttons against it). [onChanged] (the haptic) runs only when the
 * commit reports a real change, and [onCommitted] (navigation/refresh) only
 * after the commit succeeds — on failure neither runs, the screen stays,
 * and the guard releases for retry. Orchestration is pinned in
 * TerminalActionTest; the database pairing behind `commit` is owned by the
 * repositories' transactional methods.
 */
class TerminalAction(private val scope: CoroutineScope) {

    var busy by mutableStateOf(false)
        private set

    /** Returns false when another operation from this guard is still pending. */
    fun run(commit: suspend () -> Boolean, onChanged: () -> Unit, onCommitted: () -> Unit): Boolean {
        if (busy) return false
        busy = true
        scope.launch {
            try {
                if (commit()) onChanged()
                onCommitted()
            } catch (c: CancellationException) {
                throw c
            } catch (t: Throwable) {
                // Failure path: no haptic, no navigation; the guard releases
                // below so the user can retry.
            } finally {
                busy = false
            }
        }
        return true
    }
}

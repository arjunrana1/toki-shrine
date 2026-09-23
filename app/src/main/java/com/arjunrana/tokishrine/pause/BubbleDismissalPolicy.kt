package com.arjunrana.tokishrine.pause

/**
 * Pure dismissal memory for the §6 overlay bubble (Phase 6 owner addendum,
 * 23 September 2026): dragging the bubble to the bottom screen edge hides
 * it while the pause instances visible at dismissal are the only ones still
 * live. A pause is identified by its block id paired with its monotonic
 * deadline, so a restarted or fresh pause of the same block carries a new
 * identity and brings the bubble back; when nothing is live the memory
 * resets.
 */
class BubbleDismissalPolicy {

    private val dismissed = mutableSetOf<Pair<Long, Long>>()

    /** Remembers every pause instance visible when the bubble was dismissed. */
    fun recordDismissal(active: Collection<Pair<Long, Long>>) {
        dismissed.addAll(active)
    }

    /** True when at least one live pause was not on screen at dismissal time. */
    fun shouldShow(active: Collection<Pair<Long, Long>>): Boolean =
        active.any { it !in dismissed }

    /** Forgets recorded instances once no pause is live, bounding the memory. */
    fun resetIfIdle(active: Collection<Pair<Long, Long>>) {
        if (active.isEmpty()) dismissed.clear()
    }
}

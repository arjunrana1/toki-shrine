package com.arjunrana.tokishrine.pause

/**
 * Immutable dismissal decision (P6C-R1 repair, 23 September 2026): the
 * displayed block and the live pause instances captured at release, before
 * the pill's cosmetic exit animation runs. A pause started or a displayed
 * block changed during that animation is not part of the decision, so
 * applying the snapshot later can never hide the bubble for a pause the
 * owner never saw dismissed nor attribute the event to a different pill.
 */
class BubbleDismissalSnapshot(val blockId: Long, keys: List<Pair<Long, Long>>) {
    /** Defensive copy: pause state changing after the decision cannot enter it. */
    val activePauseKeys: List<Pair<Long, Long>> = keys.toList()
}

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

    /** Remembers the pause instances the snapshot captured at dismissal decision time. */
    fun recordDismissal(snapshot: BubbleDismissalSnapshot) {
        dismissed.addAll(snapshot.activePauseKeys)
    }

    /** True when at least one live pause was not on screen at dismissal time. */
    fun shouldShow(active: Collection<Pair<Long, Long>>): Boolean =
        active.any { it !in dismissed }

    /** Forgets recorded instances once no pause is live, bounding the memory. */
    fun resetIfIdle(active: Collection<Pair<Long, Long>>) {
        if (active.isEmpty()) dismissed.clear()
    }
}

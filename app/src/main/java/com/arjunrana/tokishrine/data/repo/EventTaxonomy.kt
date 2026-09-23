package com.arjunrana.tokishrine.data.repo

/**
 * The approved §10 event taxonomy (Phase 7): which event names are **active**
 * and which are **retired**. Active events are the complete set a full
 * manual run-through must fire at least once (Phase 7 acceptance); retired
 * names are historical compatibility only — existing Room rows are preserved,
 * never re-emitted and excluded from the audit.
 *
 * The active set is generated from the EventRepository constants so the two
 * cannot drift: every emitted event name is classified active by
 * construction, and any newly added constant automatically owes its place in
 * the audit.
 */
object EventTaxonomy {

    val ACTIVE: Set<String> = setOf(
        // Onboarding
        EventRepository.EVENT_ONBOARDING_STARTED,
        EventRepository.EVENT_PERMISSION_REQUESTED,
        EventRepository.EVENT_PERMISSION_GRANTED,
        EventRepository.EVENT_PERMISSION_DENIED,
        EventRepository.EVENT_ONBOARDING_COMPLETED,
        // Block management
        EventRepository.EVENT_BLOCK_CREATE_STARTED,
        EventRepository.EVENT_BLOCK_CREATE_STEP_COMPLETED,
        EventRepository.EVENT_BLOCK_CREATE_ABANDONED,
        EventRepository.EVENT_BLOCK_CREATED,
        EventRepository.EVENT_BLOCK_EDITED,
        EventRepository.EVENT_BLOCK_DELETED,
        EventRepository.EVENT_BLOCK_TURNED_ON,
        EventRepository.EVENT_BLOCK_TURNED_OFF,
        // The interruption
        EventRepository.EVENT_BLOCK_SCREEN_SHOWN,
        EventRepository.EVENT_WALK_AWAY,
        EventRepository.EVENT_CHALLENGE_STARTED,
        EventRepository.EVENT_CHALLENGE_COMPLETED,
        EventRepository.EVENT_TYPING_MISMATCH,
        EventRepository.EVENT_COUNTDOWN_STARTED,
        EventRepository.EVENT_COUNTDOWN_COMPLETED,
        // Pause
        EventRepository.EVENT_PAUSE_STARTED,
        EventRepository.EVENT_PAUSE_EXPIRED,
        EventRepository.EVENT_BUBBLE_SHOWN,
        EventRepository.EVENT_BUBBLE_TAPPED,
        EventRepository.EVENT_BUBBLE_DISMISSED,
        // Turn off
        EventRepository.EVENT_TURNOFF_STARTED,
        EventRepository.EVENT_TURNOFF_COMPLETED,
        EventRepository.EVENT_TURNOFF_ABANDONED,
        // Utilities (feature engagement)
        EventRepository.EVENT_STATS_VIEWED,
        EventRepository.EVENT_SETTINGS_VIEWED,
        EventRepository.EVENT_FEEDBACK_OPENED,
        EventRepository.EVENT_FEEDBACK_SENT,
        // Service health (diagnostics)
        EventRepository.EVENT_ACCESSIBILITY_CONNECTED,
        EventRepository.EVENT_ACCESSIBILITY_DISCONNECTED,
        EventRepository.EVENT_URL_READ_FAILED,
    )

    // Names with no application constant left: emitting code and constants
    // were removed with their features. Historical rows may exist in the
    // event table; queries must tolerate them without resurrecting them.
    val RETIRED: Set<String> = setOf(
        "block_conflict_shown",
        "block_conflict_resolved",
        "challenge_abandoned",
        "countdown_stalled",
        "countdown_resumed",
        "bubble_dragged",
    )

    fun isActive(name: String): Boolean = name in ACTIVE

    fun isRetired(name: String): Boolean = name in RETIRED
}

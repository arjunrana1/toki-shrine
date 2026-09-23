package com.arjunrana.tokishrine.data.repo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// The Phase 7 approved taxonomy: active events are exactly the names the
// application can emit (every EVENT_ constant), retired events are historical
// only. A reflection sweep keeps the classification honest — a newly added
// constant that forgets to join the active audit set fails here, and a
// retired name can never keep a live constant.
class EventTaxonomyTest {

    // Every public String constant the store can emit, read from the
    // compiled EventRepository itself rather than a hand-copied list.
    private val emittedNames: Set<String> =
        EventRepository::class.java.declaredFields
            .filter { it.type == String::class.java && it.name.startsWith("EVENT_") }
            .map { field -> field.isAccessible = true; field.get(null) as String }
            .toSet()

    @Test
    fun activeSetIsExactlyTheEmittableConstants() {
        assertEquals(emittedNames, EventTaxonomy.ACTIVE)
        assertEquals(35, EventTaxonomy.ACTIVE.size)
    }

    @Test
    fun retiredEventsCarryNoApplicationConstant() {
        // The removed instrumentation (bubble_dragged, challenge_abandoned)
        // and the older conflict/stall removals leave names only.
        assertEquals(
            setOf(
                "block_conflict_shown",
                "block_conflict_resolved",
                "challenge_abandoned",
                "countdown_stalled",
                "countdown_resumed",
                "bubble_dragged",
            ),
            EventTaxonomy.RETIRED,
        )
        assertTrue(EventTaxonomy.RETIRED.none { it in emittedNames })
    }

    @Test
    fun activeAndRetiredAreDisjoint() {
        assertTrue(EventTaxonomy.ACTIVE.none { EventTaxonomy.isRetired(it) })
        assertTrue(EventTaxonomy.RETIRED.none { EventTaxonomy.isActive(it) })
    }

    @Test
    fun countdownEventsRemainActive() {
        // Phase 7 decision: countdown_started/completed stay active
        // diagnostics even as abandonment instrumentation retires.
        assertTrue(EventTaxonomy.isActive("countdown_started"))
        assertTrue(EventTaxonomy.isActive("countdown_completed"))
    }

    @Test
    fun phaseSevenFeatureEngagementEventsAreActive() {
        assertTrue(EventTaxonomy.isActive("stats_viewed"))
        assertTrue(EventTaxonomy.isActive("settings_viewed"))
        assertTrue(EventTaxonomy.isActive("feedback_opened"))
        assertTrue(EventTaxonomy.isActive("feedback_sent"))
    }
}

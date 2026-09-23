package com.arjunrana.tokishrine.data.repo

import org.junit.Assert.assertEquals
import org.junit.Test

// Phase 7 final approval removes the temporary debug overrides (§17 Phase 5
// addendum): every build variant — debug included — enforces the production
// §7 values at the persistence boundary. These assertions pin the single
// main-source constants so an attempted variant override fails loudly here.
class ProductionChallengeValuesTest {

    @Test
    fun pauseDurationBoundsAreFiveToOneHundredMinutes() {
        assertEquals(5, PAUSE_MINUTES_MIN)
        assertEquals(100, PAUSE_MINUTES_MAX)
        assertEquals(5, PAUSE_MINUTES_STEP)
        assertEquals(15, PAUSE_MINUTES_DEFAULT)
    }

    @Test
    fun pauseTypingBoundsAreProductionValues() {
        assertEquals(100, PAUSE_CHARS_MIN)
        assertEquals(200, PAUSE_CHARS_MAX)
        assertEquals(10, PAUSE_CHARS_STEP)
        assertEquals(150, PAUSE_CHARS_DEFAULT)
    }

    @Test
    fun pauseWaitingBoundsAreProductionValues() {
        assertEquals(60, PAUSE_WAIT_SECONDS_MIN)
        assertEquals(300, PAUSE_WAIT_SECONDS_MAX)
        assertEquals(5, PAUSE_WAIT_SECONDS_STEP)
        assertEquals(60, PAUSE_WAIT_SECONDS_DEFAULT)
    }

    @Test
    fun disableLaddersAreTheProductionFixedChoices() {
        assertEquals(listOf(220, 350, 700), DISABLE_CHARS_CHOICES)
        assertEquals(listOf(180, 360, 720), DISABLE_WAIT_SECONDS_CHOICES)
        assertEquals(350, DISABLE_CHARS_DEFAULT)
        assertEquals(360, DISABLE_WAIT_SECONDS_DEFAULT)
    }
}

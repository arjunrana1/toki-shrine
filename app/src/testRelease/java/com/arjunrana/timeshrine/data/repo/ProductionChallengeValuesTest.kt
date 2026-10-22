package com.arjunrana.timeshrine.data.repo

import com.arjunrana.timeshrine.config.AppConfig.PAUSE_CHARS_DEFAULT
import com.arjunrana.timeshrine.config.AppConfig.PAUSE_CHARS_MAX
import com.arjunrana.timeshrine.config.AppConfig.PAUSE_CHARS_MIN
import com.arjunrana.timeshrine.config.AppConfig.PAUSE_CHARS_STEP
import com.arjunrana.timeshrine.config.AppConfig.PAUSE_WAIT_SECONDS_DEFAULT
import com.arjunrana.timeshrine.config.AppConfig.PAUSE_WAIT_SECONDS_MAX
import com.arjunrana.timeshrine.config.AppConfig.PAUSE_WAIT_SECONDS_MIN
import com.arjunrana.timeshrine.config.AppConfig.PAUSE_WAIT_SECONDS_STEP
import com.arjunrana.timeshrine.config.AppConfig.UNLOCK_MINUTE_CHOICES
import org.junit.Assert.assertEquals
import org.junit.Test

// Release-variant guard (run with testReleaseUnitTest): the release build
// enforces the production values (PRD §17 23 October pre-launch config
// values addendum) at the persistence boundary. The pause values are the
// same in every build; the debug variant's temporary disable rung
// (7 October 2026) is pinned separately by DebugChallengeValuesTest, so
// neither set can drift silently.
class ProductionChallengeValuesTest {

    // Supersedes pauseDurationBoundsAreFiveToOneHundredMinutes: the pause
    // length is picked at the gate from fixed choices.
    @Test
    fun pauseLengthIsTheGateChoiceOfFiveToTwentyMinutes() {
        assertEquals(listOf(5, 10, 15, 20), UNLOCK_MINUTE_CHOICES)
    }

    // Supersedes 20..150 / 10, default 20 (11 October) and 100..200 / 10, default 150.
    @Test
    fun pauseTypingBoundsAreProductionValues() {
        assertEquals(30, PAUSE_CHARS_MIN)
        assertEquals(150, PAUSE_CHARS_MAX)
        assertEquals(10, PAUSE_CHARS_STEP)
        assertEquals(40, PAUSE_CHARS_DEFAULT)
    }

    // Supersedes 20..240 / 10 seconds, default 60 (11 October) and 60..300 / 5, default 60.
    @Test
    fun pauseWaitingBoundsAreProductionValues() {
        assertEquals(30, PAUSE_WAIT_SECONDS_MIN)
        assertEquals(120, PAUSE_WAIT_SECONDS_MAX)
        assertEquals(10, PAUSE_WAIT_SECONDS_STEP)
        assertEquals(30, PAUSE_WAIT_SECONDS_DEFAULT)
    }

    // Supersedes 220 / 350 / 700 chars (default 350) and 3 / 6 / 12 min (default 6).
    @Test
    fun disableLaddersAreTheProductionFixedChoices() {
        assertEquals(listOf(150, 200, 300), DISABLE_CHARS_CHOICES)
        assertEquals(listOf(60, 120, 240), DISABLE_WAIT_SECONDS_CHOICES)
        assertEquals(200, DISABLE_CHARS_DEFAULT)
        assertEquals(120, DISABLE_WAIT_SECONDS_DEFAULT)
    }
}

package com.arjunrana.tokishrine.data.repo

import com.arjunrana.tokishrine.config.AppConfig.PAUSE_CHARS_DEFAULT
import com.arjunrana.tokishrine.config.AppConfig.PAUSE_CHARS_MAX
import com.arjunrana.tokishrine.config.AppConfig.PAUSE_CHARS_MIN
import com.arjunrana.tokishrine.config.AppConfig.PAUSE_CHARS_STEP
import com.arjunrana.tokishrine.config.AppConfig.PAUSE_WAIT_SECONDS_DEFAULT
import com.arjunrana.tokishrine.config.AppConfig.PAUSE_WAIT_SECONDS_MAX
import com.arjunrana.tokishrine.config.AppConfig.PAUSE_WAIT_SECONDS_MIN
import com.arjunrana.tokishrine.config.AppConfig.PAUSE_WAIT_SECONDS_STEP
import com.arjunrana.tokishrine.config.AppConfig.UNLOCK_MINUTE_CHOICES
import org.junit.Assert.assertEquals
import org.junit.Test

// Release-variant guard (run with testReleaseUnitTest): the release build
// enforces the production §7 / §17 values at the persistence boundary. The
// pause values are the same in every build since the 11 October
// unlock-minutes addendum; the debug variant's temporary disable rung
// (7 October 2026) is pinned separately by DebugChallengeValuesTest, so
// neither set can drift silently.
class ProductionChallengeValuesTest {

    // Supersedes pauseDurationBoundsAreFiveToOneHundredMinutes: the pause
    // length is picked at the gate from fixed choices.
    @Test
    fun pauseLengthIsTheGateChoiceOfFiveToTwentyMinutes() {
        assertEquals(listOf(5, 10, 15, 20), UNLOCK_MINUTE_CHOICES)
    }

    // Supersedes 100..200 / 10, default 150.
    @Test
    fun pauseTypingBoundsAreProductionValues() {
        assertEquals(20, PAUSE_CHARS_MIN)
        assertEquals(150, PAUSE_CHARS_MAX)
        assertEquals(10, PAUSE_CHARS_STEP)
        assertEquals(20, PAUSE_CHARS_DEFAULT)
    }

    // Supersedes 60..300 / 5 seconds, default 60.
    @Test
    fun pauseWaitingBoundsAreProductionValues() {
        assertEquals(20, PAUSE_WAIT_SECONDS_MIN)
        assertEquals(240, PAUSE_WAIT_SECONDS_MAX)
        assertEquals(10, PAUSE_WAIT_SECONDS_STEP)
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

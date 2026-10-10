package com.arjunrana.tokishrine.data.repo

import com.arjunrana.tokishrine.config.AppConfig.PAUSE_CHARS_MAX
import com.arjunrana.tokishrine.config.AppConfig.PAUSE_CHARS_STEP
import com.arjunrana.tokishrine.config.AppConfig.PAUSE_MINUTES_DEFAULT
import com.arjunrana.tokishrine.config.AppConfig.PAUSE_MINUTES_MAX
import com.arjunrana.tokishrine.config.AppConfig.PAUSE_MINUTES_MIN
import com.arjunrana.tokishrine.config.AppConfig.PAUSE_MINUTES_STEP
import com.arjunrana.tokishrine.config.AppConfig.PAUSE_WAIT_SECONDS_MAX
import com.arjunrana.tokishrine.config.AppConfig.PAUSE_WAIT_SECONDS_STEP
import org.junit.Assert.assertEquals
import org.junit.Test

// Release-variant guard (run with testReleaseUnitTest): the release build
// enforces the production §7 / §17 values at the persistence boundary. The
// debug variant's temporary owner-testing values (7 October 2026) are pinned
// separately by DebugChallengeValuesTest, so neither set can drift silently.
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

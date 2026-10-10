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

// Debug-variant guard for Arjun's temporary owner-testing values (7 October
// 2026): 20-character / 20-second floors and defaults, a 20 first disable
// rung preselected, everything else at production. The release values are
// pinned by ProductionChallengeValuesTest (testReleaseUnitTest). Reverting
// the debug values means replacing these assertions with the production ones.
class DebugChallengeValuesTest {

    @Test
    fun pauseDurationBoundsAreUnchanged() {
        assertEquals(5, PAUSE_MINUTES_MIN)
        assertEquals(100, PAUSE_MINUTES_MAX)
        assertEquals(5, PAUSE_MINUTES_STEP)
        assertEquals(15, PAUSE_MINUTES_DEFAULT)
    }

    @Test
    fun pauseTypingFloorsAndStartsAtTwentyCharacters() {
        assertEquals(20, PAUSE_CHARS_MIN)
        assertEquals(200, PAUSE_CHARS_MAX)
        assertEquals(10, PAUSE_CHARS_STEP)
        assertEquals(20, PAUSE_CHARS_DEFAULT)
    }

    @Test
    fun pauseWaitingFloorsAndStartsAtTwentySeconds() {
        assertEquals(20, PAUSE_WAIT_SECONDS_MIN)
        assertEquals(300, PAUSE_WAIT_SECONDS_MAX)
        assertEquals(5, PAUSE_WAIT_SECONDS_STEP)
        assertEquals(20, PAUSE_WAIT_SECONDS_DEFAULT)
    }

    @Test
    fun disableLaddersLowerOnlyTheFirstRungWhichIsPreselected() {
        assertEquals(listOf(20, 350, 700), DISABLE_CHARS_CHOICES)
        assertEquals(listOf(20, 360, 720), DISABLE_WAIT_SECONDS_CHOICES)
        assertEquals(20, DISABLE_CHARS_DEFAULT)
        assertEquals(20, DISABLE_WAIT_SECONDS_DEFAULT)
    }
}

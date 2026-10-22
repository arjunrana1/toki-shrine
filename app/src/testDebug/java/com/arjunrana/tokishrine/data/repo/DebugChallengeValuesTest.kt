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

// Debug-variant guard for Arjun's owner-testing values (7 October 2026,
// ladders updated 23 October): a 20 first disable rung preselected,
// everything else at production. The pause values are the same in every
// build (AppConfig) and are pinned here too, so the debug build
// cannot drift from them. The release values are pinned by
// ProductionChallengeValuesTest (testReleaseUnitTest). Reverting the debug
// disable rung means replacing that assertion with the production one.
class DebugChallengeValuesTest {

    // Supersedes pauseDurationBoundsAreUnchanged (5..100 / 5, default 15):
    // the pause length is picked at the gate from fixed choices.
    @Test
    fun pauseLengthIsTheGateChoiceOfFiveToTwentyMinutes() {
        assertEquals(listOf(5, 10, 15, 20), UNLOCK_MINUTE_CHOICES)
    }

    // Supersedes pauseTypingIsTwentyToOneFiftyStartingAtTwenty (20..150, default 20).
    @Test
    fun pauseTypingIsThirtyToOneFiftyStartingAtForty() {
        assertEquals(30, PAUSE_CHARS_MIN)
        assertEquals(150, PAUSE_CHARS_MAX)
        assertEquals(10, PAUSE_CHARS_STEP)
        assertEquals(40, PAUSE_CHARS_DEFAULT)
    }

    // Supersedes pauseWaitingIsTwentyToTwoFortySecondsStartingAtSixty (20..240, default 60).
    @Test
    fun pauseWaitingIsThirtyToOneTwentySecondsStartingAtThirty() {
        assertEquals(30, PAUSE_WAIT_SECONDS_MIN)
        assertEquals(120, PAUSE_WAIT_SECONDS_MAX)
        assertEquals(10, PAUSE_WAIT_SECONDS_STEP)
        assertEquals(30, PAUSE_WAIT_SECONDS_DEFAULT)
    }

    // Supersedes 20 / 350 / 700 chars and 20 s / 6 / 12 min.
    @Test
    fun disableLaddersLowerOnlyTheFirstRungWhichIsPreselected() {
        assertEquals(listOf(20, 200, 300), DISABLE_CHARS_CHOICES)
        assertEquals(listOf(20, 120, 240), DISABLE_WAIT_SECONDS_CHOICES)
        assertEquals(20, DISABLE_CHARS_DEFAULT)
        assertEquals(20, DISABLE_WAIT_SECONDS_DEFAULT)
    }
}

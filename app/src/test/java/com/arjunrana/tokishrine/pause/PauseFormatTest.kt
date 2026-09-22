package com.arjunrana.tokishrine.pause

import org.junit.Assert.assertEquals
import org.junit.Test

/*
 * Presentation math for the bubble and the countdown notification. The
 * anchoring tests pin the clock-change rule where wall time actually
 * appears: every `when` is re-derived from the monotonic remaining time at
 * post time, so moving the device clock cannot shorten the countdown.
 */
class PauseFormatTest {

    private fun pause(startedAtMs: Long, minutes: Int) =
        PauseState(
            blockId = 1,
            startedAtMs = startedAtMs,
            deadlineMs = startedAtMs + minutes * PauseRegistry.MILLIS_PER_MINUTE,
            minutes = minutes,
        )

    @Test
    fun clockTextMatchesTheScreenMssFormat() {
        assertEquals("12:04", PauseFormat.clockText(12 * 60_000L + 4_000L))
        assertEquals("0:47", PauseFormat.clockText(47_000L))
        assertEquals("0:00", PauseFormat.clockText(0L))
        // Pause durations run up to 100 minutes (PRD §7).
        assertEquals("100:00", PauseFormat.clockText(100 * 60_000L))
        // Sub-second leftovers round up, never down: 0:01 still shows.
        assertEquals("0:01", PauseFormat.clockText(1L))
    }

    @Test
    fun chronometerAnchorPlacesTheDeadlineAtWallNowPlusMonotonicRemaining() {
        val when1 = PauseFormat.chronometerWhen(currentTimeMillis = 1_000_000L, remainingMonotonicMs = 60_000L)
        assertEquals(1_060_000L, when1)
    }

    @Test
    fun aForwardWallClockJumpBetweenPostsCannotShortenTheCountdown() {
        // One minute of a 15-minute pause elapses monotonically while the
        // device clock jumps two hours forward. The re-posted `when` keeps
        // the rendered countdown equal to the monotonic remaining time, so
        // the jump changed nothing the user can see.
        val wall0 = 10_000_000L
        val first = PauseFormat.chronometerWhen(wall0, remainingMonotonicMs = 15 * 60_000L)
        val wall1 = wall0 + 2 * 60 * 60_000L
        val second = PauseFormat.chronometerWhen(wall1, remainingMonotonicMs = 14 * 60_000L)

        assertEquals(15 * 60_000L, first - wall0)
        assertEquals(14 * 60_000L, second - wall1)
    }

    @Test
    fun elapsedPerMilleFillsAsThePauseRunsOut() {
        val startedAt = 0L
        val fifteenMinutes = pause(startedAt, minutes = 15)

        assertEquals(0, PauseFormat.elapsedPerMille(fifteenMinutes, startedAt))
        assertEquals(250, PauseFormat.elapsedPerMille(fifteenMinutes, startedAt + 15 * 60_000L / 4))
        assertEquals(1000, PauseFormat.elapsedPerMille(fifteenMinutes, startedAt + 15 * 60_000L))
        // Clamped at both ends, monotonic timestamps before start included.
        assertEquals(0, PauseFormat.elapsedPerMille(fifteenMinutes, startedAt - 5_000L))
        assertEquals(1000, PauseFormat.elapsedPerMille(fifteenMinutes, startedAt + 15 * 60_000L + 5_000L))
    }
}

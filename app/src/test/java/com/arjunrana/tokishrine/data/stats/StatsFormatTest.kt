package com.arjunrana.tokishrine.data.stats

import org.junit.Assert.assertEquals
import org.junit.Test

// Screen 23 display formatting boundaries: the rate's whole-percent
// rendering and the "since you started" line across launch day, singular and
// plural days.
class StatsFormatTest {

    @Test
    fun walkAwayRatePercent_rendersWholePercent() {
        assertEquals("63%", StatsFormat.walkAwayRatePercent(0.63))
        assertEquals("70%", StatsFormat.walkAwayRatePercent(0.7))
        assertEquals("100%", StatsFormat.walkAwayRatePercent(1.0))
        assertEquals("0%", StatsFormat.walkAwayRatePercent(0.0))
    }

    @Test
    fun walkAwayRatePercent_roundsHalfUp() {
        // 1/3 and 2/3 must not both collapse to the same displayed value.
        assertEquals("33%", StatsFormat.walkAwayRatePercent(1.0 / 3.0))
        assertEquals("67%", StatsFormat.walkAwayRatePercent(2.0 / 3.0))
    }

    @Test
    fun sinceStartedLine_launchDayIsToday() {
        assertEquals("since you started, today", StatsFormat.sinceStartedLine(0))
    }

    @Test
    fun sinceStartedLine_singularAndPluralDays() {
        assertEquals("since you started, 1 day ago", StatsFormat.sinceStartedLine(1))
        assertEquals("since you started, 21 days ago", StatsFormat.sinceStartedLine(21))
    }
}

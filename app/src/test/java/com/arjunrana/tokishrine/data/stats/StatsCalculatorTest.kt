package com.arjunrana.tokishrine.data.stats

import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class StatsCalculatorTest {

    @Test
    fun walkAwayRate_matchesFormula() {
        assertEquals(0.7, StatsCalculator.walkAwayRate(14, 6), 1e-9)
        assertEquals(1.0, StatsCalculator.walkAwayRate(3, 0), 1e-9)
        assertEquals(0.5, StatsCalculator.walkAwayRate(1, 1), 1e-9)
    }

    @Test
    fun walkAwayRate_zeroData_isZero_notNaN() {
        assertEquals(0.0, StatsCalculator.walkAwayRate(0, 0), 0.0)
    }

    @Test
    fun calendarWeekStart_opensAtStartOfSixDaysAgo_utc() {
        val zone = ZoneId.of("UTC")
        val now = ZonedDateTime.of(2026, 9, 9, 15, 30, 0, 0, zone).toInstant().toEpochMilli()
        val expected = ZonedDateTime.of(2026, 9, 3, 0, 0, 0, 0, zone).toInstant().toEpochMilli()
        assertEquals(expected, StatsCalculator.calendarWeekStart(now, zone))
    }

    @Test
    fun calendarWeekStart_usesLocalCalendar_notUtc() {
        // 2026-09-09T01:00Z is already 2026-09-08 in Los Angeles, so the local
        // week opens on 09-02 local, not 09-03.
        val zone = ZoneId.of("America/Los_Angeles")
        val now = ZonedDateTime.of(2026, 9, 9, 1, 0, 0, 0, ZoneId.of("UTC")).toInstant().toEpochMilli()
        val expected = ZonedDateTime.of(2026, 9, 2, 0, 0, 0, 0, zone).toInstant().toEpochMilli()
        assertEquals(expected, StatsCalculator.calendarWeekStart(now, zone))
    }

    @Test
    fun daysSince_countsWholeCalendarDays() {
        val zone = ZoneId.of("UTC")
        val first = ZonedDateTime.of(2026, 8, 30, 10, 0, 0, 0, zone).toInstant().toEpochMilli()
        val now = ZonedDateTime.of(2026, 9, 9, 9, 0, 0, 0, zone).toInstant().toEpochMilli()
        assertEquals(10, StatsCalculator.daysSince(first, now, zone))
    }

    @Test
    fun daysSince_launchDayIsZero() {
        val zone = ZoneId.of("Asia/Kolkata")
        val first = ZonedDateTime.of(2026, 9, 9, 8, 0, 0, 0, zone).toInstant().toEpochMilli()
        val now = ZonedDateTime.of(2026, 9, 9, 22, 0, 0, 0, zone).toInstant().toEpochMilli()
        assertEquals(0, StatsCalculator.daysSince(first, now, zone))
    }

    @Test
    fun daysSince_futureFirstLaunch_clampsToZero() {
        val zone = ZoneId.of("UTC")
        val first = ZonedDateTime.of(2026, 9, 10, 0, 0, 0, 0, zone).toInstant().toEpochMilli()
        val now = ZonedDateTime.of(2026, 9, 9, 0, 0, 0, 0, zone).toInstant().toEpochMilli()
        assertEquals(0, StatsCalculator.daysSince(first, now, zone))
    }
}

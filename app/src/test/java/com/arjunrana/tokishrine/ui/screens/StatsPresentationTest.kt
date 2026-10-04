package com.arjunrana.tokishrine.ui.screens

import com.arjunrana.tokishrine.data.stats.StatsApp
import com.arjunrana.tokishrine.data.stats.StatsDashboard
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/*
 * Presentation rules for the redesigned Stats screen (PRD §9), pinned on the
 * JVM: app-row ordering and per-visit labels, rolling weekday letters, the
 * hero screen-time line's hide rules, the stats_viewed state classification
 * and chart bar geometry. Metric formulas themselves are pinned in
 * StatsRedesignTest; these helpers only map them for display.
 */
class StatsPresentationTest {

    private fun app(pkg: String, savedMs: Long, nopes: Int = 1, visitMs: Long = 6 * 60_000L) =
        StatsApp(pkg, nopes, savedMs, visitMs, usesFallback = visitMs == 600_000L)

    @Test
    fun appRowsSortBySavedDurationThenDisplayLabel() {
        val rows = statsAppRows(
            listOf(app("com.b", 120_000), app("com.c", 120_000), app("com.a", 300_000)),
            labelFor = { pkg ->
                mapOf("com.a" to "Alpha", "com.b" to "Zulu", "com.c" to "Beta")[pkg] ?: pkg
            },
        )
        assertEquals(listOf("Alpha", "Beta", "Zulu"), rows.map { it.label })
    }

    @Test
    fun appRowSubtitleUsesWholeMinuteVisitLabelWithOneMinuteFloor() {
        val rows = statsAppRows(
            listOf(
                app("a", 60_000, nopes = 22, visitMs = 6 * 60_000L),
                app("b", 0, nopes = 0, visitMs = 5_000L),
                app("c", 0, nopes = 0, visitMs = 600_000L),
            ),
            labelFor = { it },
        )
        assertEquals("22 nopes this week · ~6m per visit", appRowSubtitle(rows[0]))
        assertEquals("0 nopes this week · ~1m per visit", appRowSubtitle(rows[1]))
        assertEquals("0 nopes this week · ~10m per visit", appRowSubtitle(rows[2]))
    }

    @Test
    fun weekdayLabelsAreRollingNarrowLettersWithTodayLast() {
        val days = (6L downTo 0L).map { LocalDate.parse("2026-10-04").minusDays(it) }
        assertEquals(listOf("M", "T", "W", "T", "F", "S", "Today"), weekdayLabels(days))
    }

    @Test
    fun screenTimeLineHidesWithoutAverageAndSuppressesZeroOrUnknownDelta() {
        val average = 220 * 60_000L // 3h 40m
        assertNull(screenTimeLine(null, -22))
        assertEquals("Screen time 3h 40m/day", screenTimeLine(average, null))
        assertEquals("Screen time 3h 40m/day", screenTimeLine(average, 0))
        assertEquals("Screen time 3h 40m/day · ↓ 22% vs your usual", screenTimeLine(average, -22))
        assertEquals("Screen time 3h 40m/day · ↑ 9% vs your usual", screenTimeLine(average, 9))
    }

    private fun dashboard(capturedAt: Long?) = StatsDashboard(
        days = emptyList(), savedMs = 0, savedPerDayMs = 0, attempts = 0, attemptsPerDay = 0.0,
        nopes = 0, passes = 0, nopeRatePercent = 0, apps = emptyList(), spentMs = 0,
        unmeasuredPasses = 0, screenDailyMs = null, screenChangePercent = null,
        baselineCapturedAt = capturedAt, usageAvailable = true,
    )

    @Test
    fun viewedStateReadsPartialUntilTheWindowIsFullyCovered() {
        val weekStart = 1_000_000L
        assertEquals("partial", statsViewedState(dashboard(capturedAt = null), weekStart))
        assertEquals("partial", statsViewedState(dashboard(capturedAt = weekStart), weekStart))
        assertEquals("partial", statsViewedState(dashboard(capturedAt = weekStart + 1), weekStart))
        assertEquals("default", statsViewedState(dashboard(capturedAt = weekStart - 1), weekStart))
    }

    @Test
    fun barsScaleToTheMaxWithStubFloorsForZeroAndUnknown() {
        val minute = 60_000L
        val bars = statsBarSpecs(listOf(52 * minute, 0L, 61 * minute, 45 * minute, null))
        assertEquals("52m", bars[0].valueLabel)
        assertEquals(63, bars[0].heightDp) // 52/61 * 74, rounded
        assertEquals("0m", bars[1].valueLabel)
        assertEquals(3, bars[1].heightDp)
        assertEquals(74, bars[2].heightDp)
        assertEquals(55, bars[3].heightDp) // 45/61 * 74, rounded
        assertEquals("—", bars[4].valueLabel)
        assertEquals(3, bars[4].heightDp)
        assertEquals(false, bars[4].filled)
        assertEquals(true, bars[0].filled)
    }

    @Test
    fun tinyBarsKeepTheFourDpFloorAndAllZeroWeeksStayStubs() {
        val minute = 60_000L
        val bars = statsBarSpecs(listOf(minute, 100 * minute))
        assertEquals(4, bars[0].heightDp)
        assertEquals(74, bars[1].heightDp)
        val flat = statsBarSpecs(listOf(0L, 0L, 0L))
        assertEquals(listOf(3, 3, 3), flat.map { it.heightDp })
        assertEquals(listOf("0m", "0m", "0m"), flat.map { it.valueLabel })
    }

    @Test
    fun heavyDayBarLabelsStayMinutesOnly() {
        val minute = 60_000L
        val bars = statsBarSpecs(listOf(140 * minute, 100 * minute, 99 * minute + 30_000))
        assertEquals(listOf("140m", "100m", "100m"), bars.map { it.valueLabel })
    }
}

package com.arjunrana.tokishrine.ui.screens

import com.arjunrana.tokishrine.data.stats.StatsApp
import com.arjunrana.tokishrine.data.stats.StatsDashboard
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/*
 * Presentation rules for the redesigned Stats screen (PRD §9 + the 8 October
 * §17 addendum: S1 v2 layout, S7 chips, S8 sheet), pinned on the JVM:
 * app-row ordering, visit-chip text, the visit-sheet copy and live line, the
 * today screen-time line's hide rules, THIS WEEK's N-of-M value, the
 * stats_viewed state classification and chart bar geometry. Metric formulas
 * themselves are pinned in StatsRedesignTest; these helpers only map them
 * for display.
 */
class StatsPresentationTest {

    private fun app(
        pkg: String,
        savedMs: Long,
        nopes: Int = 1,
        visitMs: Long = 6 * 60_000L,
        userVisitMs: Long? = null,
    ) = StatsApp(
        packageName = pkg,
        nopes = nopes,
        savedMs = savedMs,
        usualVisitMs = userVisitMs ?: visitMs,
        usesFallback = visitMs == 600_000L,
        measuredVisitMs = visitMs,
        userVisitMs = userVisitMs,
    )

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
    fun weekdayLabelsAreRollingNarrowLettersWithTodayLast() {
        val days = (6L downTo 0L).map { LocalDate.parse("2026-10-04").minusDays(it) }
        assertEquals(listOf("M", "T", "W", "T", "F", "S", "Today"), weekdayLabels(days))
    }

    @Test
    fun appRowsReadNopesThisWeekPlusTheS7VisitChip() {
        val rows = statsAppRows(
            listOf(
                app("a", 60_000, nopes = 22, visitMs = 6 * 60_000L),
                app("b", 0, nopes = 0, visitMs = 5_000L),
                app("c", 0, nopes = 0, visitMs = 600_000L),
                app("d", 96 * 60_000, nopes = 8, visitMs = 7 * 60_000L, userVisitMs = 12 * 60_000L),
            ),
            labelFor = { it },
        )
        // Sorted by saved duration first; the assertions resolve by package.
        assertEquals("d", rows[0].packageName)
        assertEquals("22 nopes this week ·", appRowSubtitle(rows.first { it.packageName == "a" }))
        assertEquals("8 nopes this week ·", appRowSubtitle(rows.first { it.packageName == "d" }))
        // Every chip reads "~Xm/visit" with the pencil — a user-set value
        // is not marked (9 October §17 addendum; "· yours" is gone).
        assertEquals("~6m/visit", rows.first { it.packageName == "a" }.visitChip)
        assertEquals("~1m/visit", rows.first { it.packageName == "b" }.visitChip) // visitMinutes' 1-minute floor
        assertEquals("~10m/visit", rows.first { it.packageName == "c" }.visitChip) // the fallback baseline
        assertEquals("~12m/visit", rows[0].visitChip) // the override, shown like any other value
    }

    @Test
    fun todayScreenTimeLineHidesWhenUnknownAndNeverComparesToYourUsual() {
        val today = 220 * 60_000L // 3h 40m
        assertNull(todayScreenTimeLine(null))
        assertEquals("Screen time 3h 40m today", todayScreenTimeLine(today))
    }

    @Test
    fun weekNopeRateLineReadsPercentOnlyWithAnEmDashForNoTries() {
        // The 9 October §17 addendum drops "N of M" from the THIS WEEK card.
        assertEquals("68%", weekNopeRateLine(68, 60))
        assertEquals("0%", weekNopeRateLine(0, 3))
        assertEquals("—", weekNopeRateLine(0, 0))
    }

    @Test
    fun nopeRateSheetExplainsTodaysFiguresNotTheWeeks() {
        // Week: 41 of 60 (68%); today: 3 nopes of 4 attempts (75%).
        val model = dashboard(capturedAt = 0).copy(
            attempts = 60, nopes = 41, passes = 19, nopeRatePercent = 68,
            todayAttempts = 4, todayNopes = 3, todayNopeRatePercent = 75,
        )
        assertEquals(NopeRateSheetFigures(nopes = 3, passes = 1, attempts = 4, ratePercent = 75), todayNopeRateSheetFigures(model))
        assertEquals(NopeRateSheetFigures(0, 0, 0, 0), todayNopeRateSheetFigures(dashboard(capturedAt = 0)))
    }

    @Test
    fun visitSheetSaysMeasuredOrAssumesTheFallback() {
        assertEquals("We measured 7m.", visitSheetMeasuredLabel(usesFallback = false, measuredVisitMs = 7 * 60_000L))
        assertEquals("We assume 10m.", visitSheetMeasuredLabel(usesFallback = true, measuredVisitMs = 600_000L))
    }

    @Test
    fun visitSheetOpensAtTheOverrideElseTheMeasuredBaseline() {
        val overridden = statsAppRows(listOf(app("a", 60_000, visitMs = 7 * 60_000L, userVisitMs = 12 * 60_000L)), labelFor = { it }).single()
        val measured = statsAppRows(listOf(app("b", 60_000, visitMs = 7 * 60_000L)), labelFor = { it }).single()
        assertEquals(12, initialSheetMinutes(overridden))
        assertEquals(7, initialSheetMinutes(measured))
    }

    @Test
    fun visitSheetLiveLineRevaluesNopesAtTheSteppedMinutes() {
        assertEquals("8 nopes × 12m", visitSheetNopesLine(nopes = 8, minutes = 12))
        assertEquals("1 nope × 5m", visitSheetNopesLine(nopes = 1, minutes = 5))
        // §9 duration form: minutes under 100 stay minutes; 8 × 15m = 2h.
        assertEquals("96m saved this week", visitSheetSavedLine(8 * 12 * 60_000L))
        assertEquals("2h saved this week", visitSheetSavedLine(8 * 15 * 60_000L))
        assertEquals("0m saved this week", visitSheetSavedLine(0))
    }

    private fun dashboard(capturedAt: Long?) = StatsDashboard(
        days = emptyList(), savedMs = 0, savedPerDayMs = 0, attempts = 0,
        nopes = 0, passes = 0, nopeRatePercent = 0, todaySavedMs = 0, todayAttempts = 0,
        todayNopes = 0, todayNopeRatePercent = 0, todayScreenMs = null, apps = emptyList(), spentMs = 0,
        unmeasuredPasses = 0,
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

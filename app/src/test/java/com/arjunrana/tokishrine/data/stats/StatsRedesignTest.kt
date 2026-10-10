package com.arjunrana.tokishrine.data.stats

import com.arjunrana.tokishrine.data.db.BlockWithContents
import com.arjunrana.tokishrine.data.entity.Block
import com.arjunrana.tokishrine.data.entity.BlockedApp
import com.arjunrana.tokishrine.data.entity.BlockedSite
import com.arjunrana.tokishrine.data.entity.FrictionType
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.*
import org.junit.Test

class StatsRedesignTest {
    private fun outcome(at: Long = 0, kind: String = "nope", counted: Boolean = true, pkg: String = "app", saved: Long = 10_000) =
        StatsOutcome("$at:$pkg:$kind", pkg, at, "2026-10-04", kind, counted, if (kind == "nope") saved else 0, null)
    private fun event(at: Long, kind: Int, pkg: String = "app", activity: String = "A", zone: String = "UTC") =
        StatsUsageEvent("$at:$kind:$pkg:$activity", at, pkg, activity, kind, zone)
    private val resume = UsageKind.RESUME
    private val pause = UsageKind.PAUSE

    @Test fun nopeWindowHasFixedAnchorAndPassResetsIt() {
        val first = outcome()
        assertFalse(StatsLedger.shouldCountNope(first, 119_999)) // owner, 10 October: two-minute window
        assertTrue(StatsLedger.shouldCountNope(first, 120_000))
        assertTrue(StatsLedger.shouldCountNope(first.copy(kind = "pass"), 1))
        assertTrue(StatsLedger.shouldCountNope(first, -1)) // wall clock rollback cannot suppress indefinitely
    }

    @Test fun siteOutcomesBelongToTheHostingBrowserPackage() {
        assertEquals("com.app", StatsLedger.statsPackage("com.app", "app", null))
        assertEquals("com.app", StatsLedger.statsPackage("com.app", "app", "com.android.chrome"))
        assertEquals("com.android.chrome", StatsLedger.statsPackage("reddit.com", "site", "com.android.chrome"))
        assertNull(StatsLedger.statsPackage("reddit.com", "site", null)) // unknown browser: no outcome, never the domain
        assertNull(StatsLedger.statsPackage("reddit.com", "site", " "))
        assertNull(StatsLedger.statsPackage("com.app", null, "com.app"))
        assertNull(StatsLedger.statsPackage("", "app", null))
    }

    @Test fun enabledSiteBlocksAddHostingBrowsersToTheLeaderboard() {
        fun block(id: Long, apps: List<String>, sites: List<String>) = BlockWithContents(
            Block(id, "B$id", FrictionType.TYPING, 15, 150, 350, 60, 360, enabled = true),
            apps.map { BlockedApp(blockId = id, packageName = it) },
            sites.map { BlockedSite(blockId = id, domain = it) },
        )
        val browsers = setOf("com.android.chrome", "org.mozilla.firefox")
        assertEquals(setOf("com.app"), StatsRepository.leaderboardPackages(listOf(block(1, listOf("com.app"), emptyList())), browsers))
        assertEquals(setOf("com.app") + browsers,
            StatsRepository.leaderboardPackages(listOf(block(1, listOf("com.app"), emptyList()), block(2, emptyList(), listOf("reddit.com"))), browsers))
        // A browser that is also an app target appears once.
        assertEquals(browsers, StatsRepository.leaderboardPackages(listOf(block(1, listOf("com.android.chrome"), listOf("reddit.com"))), browsers))
        assertEquals(emptySet<String>(), StatsRepository.leaderboardPackages(emptyList(), browsers))
        // P7-F10: apps with a counted outcome this week stay listed with no enabled block.
        assertEquals(setOf("com.old", "com.android.chrome"),
            StatsRepository.leaderboardPackages(emptyList(), browsers, setOf("com.old", "com.android.chrome")))
        assertEquals(setOf("com.app", "com.old"),
            StatsRepository.leaderboardPackages(listOf(block(1, listOf("com.app"), emptyList())), browsers, setOf("com.old")))
    }

    @Test fun dashboardUsesResolvedAppOutcomesAndRetainsRemovedAppTotals() {
        val model = StatsDashboardCalculator.calculate(LocalDate.parse("2026-10-04"), listOf(
            outcome(saved = 25_000), outcome(1, counted = false), outcome(2, "pass"), outcome(3, pkg = "removed", saved = 25_000),
            outcome(4).copy(localDate = "2026-09-27"), outcome(5).copy(localDate = "2026-10-05")),
            setOf("app", "new"), emptyList(), null, true)
        assertEquals(3, model.attempts)
        assertEquals(2, model.nopes)
        assertEquals(67, model.nopeRatePercent)
        assertEquals(50_000L, model.savedMs)
        assertEquals(50_000L / 7, model.savedPerDayMs)
        assertEquals(listOf("app", "new"), model.apps.map { it.packageName })
        assertNull(model.spentMs)
        assertEquals(1, model.unmeasuredPasses)
        assertEquals(0L, model.days.first().spentMs)
    }

    @Test fun zeroOutcomesHasZeroRateAndSevenCalendarBuckets() {
        val model = StatsDashboardCalculator.calculate(LocalDate.parse("2026-10-04"), emptyList(), setOf("app"), emptyList(), null, false)
        assertEquals(0, model.nopeRatePercent)
        assertEquals(7, model.days.size)
        assertEquals(LocalDate.parse("2026-09-28"), model.days.first().date)
        assertEquals(0L, model.spentMs)
    }

    @Test fun usualVisitIsSeventyFifthPercentileOfVisitsOfAtLeastThirtySeconds() {
        // 10 s and 29.999 s are ignored; p75 of 30/60/120/240 s interpolates to 150 s.
        assertEquals(150_000L to 4, StatsLedger.usualVisit(listOf(240_000, 10_000, 60_000, 29_999, 30_000, 120_000)))
        assertEquals(75_000L to 3, StatsLedger.usualVisit(listOf(30_000, 60_000, 90_000)))
        assertEquals(40_000L to 3, StatsLedger.usualVisit(listOf(40_000, 40_000, 40_000)))
        // Fewer than three qualifying visits falls back to 10 m, however many short ones there are.
        assertEquals(StatsLedger.FALLBACK_MS to 2, StatsLedger.usualVisit(listOf(30_000, 60_000, 5_000, 5_000, 5_000)))
        assertEquals(StatsLedger.FALLBACK_MS to 0, StatsLedger.usualVisit(emptyList()))
    }

    @Test fun todayFieldsCountFromLocalMidnightAndWeekKeepsItsAverage() {
        val model = StatsDashboardCalculator.calculate(LocalDate.parse("2026-10-04"), listOf(
            outcome(saved = 10_000), outcome(1, counted = false), outcome(2, "pass"),
            outcome(3, saved = 20_000).copy(localDate = "2026-10-03")),
            setOf("app"), emptyList(), null, true, todayScreenMs = 42_000)
        assertEquals(10_000L, model.todaySavedMs)
        assertEquals(2, model.todayAttempts) // counted nope + push-through; the deduped nope adds nothing
        assertEquals(1, model.todayNopes)
        assertEquals(50, model.todayNopeRatePercent)
        assertEquals(42_000L, model.todayScreenMs)
        assertEquals(3, model.attempts)
        assertEquals(2, model.nopes) // "2 of 3"
        assertEquals(30_000L / 7, model.savedPerDayMs)
        // One more counted nope today raises Attempts today by exactly one.
        val next = StatsDashboardCalculator.calculate(LocalDate.parse("2026-10-04"), listOf(
            outcome(saved = 10_000), outcome(1, counted = false), outcome(2, "pass"), outcome(400_000)),
            setOf("app"), emptyList(), null, true)
        assertEquals(3, next.todayAttempts)
        val empty = StatsDashboardCalculator.calculate(LocalDate.parse("2026-10-04"), emptyList(), setOf("app"), emptyList(), null, true)
        assertEquals(0, empty.todayAttempts)
        assertEquals(0, empty.todayNopeRatePercent)
        assertNull(empty.todayScreenMs)
    }

    @Test fun overrideRevaluesThatAppsCountedNopesForTheWeekShownOnly() {
        val overrides = listOf(StatsVisitOverride("app", 180_000, 0))
        val baselines = listOf(StatsBaseline("app", 90_000, 5))
        val model = StatsDashboardCalculator.calculate(LocalDate.parse("2026-10-04"), listOf(
            outcome(saved = 10_000), outcome(1, counted = false), outcome(2, saved = 20_000).copy(localDate = "2026-10-03"),
            outcome(3, pkg = "other", saved = 5_000)),
            setOf("app", "other"), baselines, null, true, overrides = overrides)
        val app = model.apps.first { it.packageName == "app" }
        assertEquals(2, app.nopes)
        assertEquals(360_000L, app.savedMs) // 2 counted nopes × 3 m; the deduped nope stays 0
        assertEquals(180_000L, app.usualVisitMs)
        assertEquals(180_000L, app.userVisitMs)
        assertEquals(90_000L, app.measuredVisitMs)
        assertFalse(app.usesFallback)
        assertEquals(app.savedMs, app.savedWith(180_000))
        assertEquals(600_000L, app.savedWith(300_000)) // live sheet preview
        val other = model.apps.first { it.packageName == "other" }
        assertEquals(5_000L, other.savedMs) // frozen value without an override
        assertNull(other.userVisitMs)
        assertEquals(StatsLedger.FALLBACK_MS, other.usualVisitMs)
        assertTrue(other.usesFallback)
        assertEquals(365_000L, model.savedMs)
        assertEquals(185_000L, model.todaySavedMs)
        assertEquals(185_000L, model.days.last().savedMs)
        assertEquals(180_000L, model.days[5].savedMs)
        // Without the override the same rows read their frozen values again.
        val cleared = StatsDashboardCalculator.calculate(LocalDate.parse("2026-10-04"), listOf(
            outcome(saved = 10_000), outcome(2, saved = 20_000).copy(localDate = "2026-10-03")),
            setOf("app"), baselines, null, true)
        assertEquals(30_000L, cleared.apps.single().savedMs)
        assertEquals(90_000L, cleared.apps.single().usualVisitMs)
    }

    @Test fun durationSumsBeforeRoundingAndNeverFloorsArithmetic() {
        assertEquals("1m", StatsDurationFormat.duration(25_000 + 25_000))
        assertEquals("99m", StatsDurationFormat.duration(99 * 60_000L))
        assertEquals("1h 40m", StatsDurationFormat.duration(100 * 60_000L))
        assertEquals("2h", StatsDurationFormat.duration(120 * 60_000L))
        assertEquals(1L, StatsDurationFormat.visitMinutes(5_000))
    }

    @Test fun activityHandoffsMergeWithoutCountingTransitionGap() {
        val visits = UsageVisits.parse(listOf(event(0, resume), event(10_000, pause), event(10_100, resume, activity = "B"), event(20_000, pause, activity = "B")))
        assertEquals(1, visits.size)
        assertEquals(19_900L, visits.single().durationMs)
    }

    @Test fun overlappingActivityCallbacksDoNotDoubleCountOrCloseNewActivity() {
        val visits = UsageVisits.parse(listOf(event(0, resume), event(10, resume, activity = "B"), event(11, pause), event(20, pause, activity = "B")))
        assertEquals(1, visits.size)
        assertEquals(20L, visits.single().durationMs)
    }

    @Test fun sameActivityReopenAndLockAreSeparateVisits() {
        val visits = UsageVisits.parse(listOf(event(0, resume), event(10, pause), event(11, resume), event(20, UsageKind.STOP), event(21, resume, activity = "B"), event(30, pause, activity = "B")))
        assertEquals(3, visits.size)
    }

    @Test fun incompleteVisitCannotBecomeCompletedMeasurementButCanShowScreenTime() {
        val raw = listOf(event(0, resume), event(10, pause), event(11, resume, activity = "B"))
        assertTrue(UsageVisits.parse(raw).isEmpty())
        assertEquals(19L, UsageVisits.parse(raw, until = 20).single().durationMs)
    }

    @Test fun orphanPauseIsNotAnInventedVisitAndDuplicateResumeIsIgnored() {
        assertTrue(UsageVisits.parse(listOf(event(10, pause))).isEmpty())
        assertEquals(20L, UsageVisits.parse(listOf(event(0, resume), event(1, resume), event(20, pause))).single().durationMs)
    }

    @Test fun postChallengeRequiresFirstAppVisitAndDoesNotUseOtherPausedAppsOrLateVisits() {
        val target = UsageVisit("app", 100, 200, listOf(UsageSegment(100, 200, "UTC")))
        assertEquals(target, UsageVisits.firstPostChallenge(outcome(kind = "pass"), listOf(target), "self"))
        assertNull(UsageVisits.firstPostChallenge(outcome(kind = "pass"), listOf(target.copy(packageName = "other"), target), "self"))
        assertNull(UsageVisits.firstPostChallenge(outcome(kind = "pass"), listOf(target.copy(start = 30_001)), "self"))
        assertEquals(target, UsageVisits.firstPostChallenge(outcome(kind = "pass"), listOf(target.copy(packageName = "self", start = 1), target), "self"))
    }

    @Test fun coverageNeverBridgesMissingIntervals() {
        val spans = UsageVisits.mergeCoverage(listOf(StatsCoverage(0, 10), StatsCoverage(5, 20), StatsCoverage(30, 40)))
        assertEquals(listOf(StatsCoverage(0, 20), StatsCoverage(30, 40)), spans)
        assertFalse(UsageVisits.covered(1, 35, spans))
        assertTrue(UsageVisits.covered(1, 20, spans))
    }

    @Test fun usageSplitsAtLocalMidnightIncludingDstAndKeepsRecordedZone() {
        val zone = ZoneId.of("America/New_York")
        val date = LocalDate.parse("2026-11-01")
        val start = date.atStartOfDay(zone).toInstant().toEpochMilli()
        val end = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() + 60_000
        val daily = UsageVisits.dailyMs(listOf(UsageVisit("app", start, end, listOf(UsageSegment(start, end, zone.id)))))
        assertEquals(25 * 3_600_000L, daily["2026-11-01"])
        assertEquals(60_000L, daily["2026-11-02"])
    }
}

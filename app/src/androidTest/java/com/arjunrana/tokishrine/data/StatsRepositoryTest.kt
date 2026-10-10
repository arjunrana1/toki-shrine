package com.arjunrana.tokishrine.data

import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import com.arjunrana.tokishrine.data.db.TokiDatabase
import com.arjunrana.tokishrine.data.entity.Event
import com.arjunrana.tokishrine.data.entity.FrictionType
import com.arjunrana.tokishrine.data.repo.BlockDraft
import com.arjunrana.tokishrine.data.repo.BlockRepository
import com.arjunrana.tokishrine.data.stats.*
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class StatsRepositoryTest {
    private lateinit var db: TokiDatabase
    private val zone = ZoneId.of("UTC")
    private var now = Instant.parse("2026-10-04T12:00:00Z").toEpochMilli()
    private var access = true
    private var available = true
    private var raw = emptyList<StatsUsageEvent>()
    private lateinit var repo: StatsRepository
    private val source = object : UsageSource {
        override fun hasAccess() = access
        override fun read(start: Long, end: Long) = if (available) raw.filter { it.at in start until end } else null
    }
    @Before fun setup() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), TokiDatabase::class.java).build()
        repo = StatsRepository(db, source, "self", { setOf("app") }, { now }, { zone })
    }
    @After fun close() = db.close()
    private fun visits(ms: Long) = (1L..3).flatMap { i ->
        val at = now - 3_600_000 + i * 100_000
        listOf(StatsUsageEvent("r:$at", at, "app", "A", UsageKind.RESUME, "UTC"),
            StatsUsageEvent("p:$at", at + ms, "app", "A", UsageKind.PAUSE, "UTC"))
    }
    private suspend fun nope(id: String) = db.withTransaction { StatsLedger(db.statsDao()).record(id, "app", "app", null, false, now, zone) }

    @Test fun baselineFrozenRecalibrationAffectsFutureOnlyAndRevocationPreservesData() = runBlocking {
        raw = visits(40_000)
        repo.refresh()
        assertEquals(40_000L, db.statsDao().baselines().single().visitMs)
        nope("old")
        val baseline = db.statsDao().state()!!.baselineId
        now += 600_000
        raw = visits(80_000)
        repo.refresh()
        assertEquals(baseline, db.statsDao().state()!!.baselineId)
        assertEquals(40_000L, db.statsDao().outcome("old")!!.savedMs)
        access = false
        repo.refresh()
        assertEquals(StatsScreenState.NoUsageAccess, repo.state.value)
        assertEquals(baseline, db.statsDao().state()!!.baselineId)
        access = true
        repo.refresh()
        assertEquals(baseline, db.statsDao().state()!!.baselineId)
        assertTrue(repo.recalibrate())
        assertNotEquals(baseline, db.statsDao().state()!!.baselineId)
        nope("new")
        assertEquals(40_000L, db.statsDao().outcome("old")!!.savedMs)
        assertEquals(80_000L, db.statsDao().baselines().single().visitMs) // p75 of three 40 s and three 80 s visits
        assertEquals(db.statsDao().baselines().single().visitMs, db.statsDao().outcome("new")!!.savedMs)
        assertEquals(1, db.eventDao().getAll().count { it.name == "baseline_recalibrated" })
    }

    @Test fun insufficientHistoryFallsBackAndFailedRecalibrationPreservesBaseline() = runBlocking {
        raw = visits(40_000).take(4)
        repo.refresh()
        assertEquals(600_000L, db.statsDao().baselines().single().visitMs)
        val before = db.statsDao().state()
        available = false
        assertFalse(repo.recalibrate())
        assertEquals(before, db.statsDao().state())
    }

    @Test fun visitsUnderThirtySecondsNeverFormABaseline() = runBlocking {
        raw = visits(29_999)
        repo.refresh()
        assertEquals(StatsBaseline("app", StatsLedger.FALLBACK_MS, 0), db.statsDao().baselines().single())
    }

    @Test fun overrideRevaluesTheWeekSurvivesRecalibrateAndClearingRestoresFrozenValues() = runBlocking {
        BlockRepository(db, clock = { now }).let { blocks ->
            blocks.setEnabled(blocks.createBlock(BlockDraft("B", listOf("app"), emptyList(), FrictionType.TYPING, 15, 150, 350, 60, 360)), true)
        }
        raw = visits(40_000)
        repo.refresh()
        nope("a"); now += 600_000; nope("b")
        assertTrue(repo.setVisitOverride("app", 12))
        fun row() = (repo.state.value as StatsScreenState.Ready).dashboard.apps.single()
        assertEquals(2 * 720_000L, row().savedMs)
        assertEquals(720_000L, row().userVisitMs)
        assertEquals(40_000L, row().measuredVisitMs)
        // Stored contributions are never rewritten.
        assertEquals(listOf(40_000L, 40_000L), db.statsDao().outcomes("0000").map { it.savedMs })
        assertTrue(repo.recalibrate())
        assertEquals(2 * 720_000L, row().savedMs)
        assertTrue(repo.clearVisitOverride("app"))
        assertEquals(80_000L, row().savedMs)
        assertNull(row().userVisitMs)
        assertThrows(IllegalArgumentException::class.java) { runBlocking { repo.setVisitOverride("app", 0) } }
        assertThrows(IllegalArgumentException::class.java) { runBlocking { repo.setVisitOverride("app", StatsLedger.OVERRIDE_MINUTES_MAX + 1) } }
        assertTrue(db.statsDao().overrides().isEmpty())
    }

    @Test fun emptyStateOnlyWithoutEnabledBlocksAndWithoutOutcomesThisWeek() = runBlocking {
        // A pre-midnight lock event makes today's history complete from local midnight.
        val midnight = Instant.parse("2026-10-04T00:00:00Z").toEpochMilli()
        raw = listOf(StatsUsageEvent("stop", midnight - 60_000, "app", "", UsageKind.STOP, "UTC")) + visits(40_000)
        repo.refresh()
        assertEquals(StatsScreenState.NoBlocks, repo.state.value)
        nope("kept")
        repo.refresh()
        val ready = (repo.state.value as StatsScreenState.Ready).dashboard
        assertFalse(ready.blocksOn)
        assertEquals(listOf("app"), ready.apps.map { it.packageName }) // its block is gone, its outcome this week keeps it
        assertEquals(1, ready.todayAttempts)
        assertEquals(1, ready.todayNopes)
        assertEquals(120_000L, ready.todayScreenMs)
        now += 7 * StatsRepository.DAY_MS
        repo.refresh()
        assertEquals(StatsScreenState.NoBlocks, repo.state.value)
    }

    @Test fun ignoredNopesNeverExtendWindowAndSuccessEndsSequence() = runBlocking {
        // Relative to DEDUP_MS so a window change cannot make this pass vacuously:
        // "ignored" lands 1 s inside the window, "two" exactly at its end. Had the
        // ignored nope extended the window, "two" would be ignored too.
        nope("one"); now += StatsLedger.DEDUP_MS - 1_000; nope("ignored"); now += 1_000; nope("two")
        db.withTransaction { StatsLedger(db.statsDao()).record("pass", "app", "app", null, true, now, zone) }
        nope("three"); nope("three")
        val rows = db.statsDao().outcomes("0000")
        assertEquals(5, rows.size)
        assertEquals(
            mapOf("one" to true, "ignored" to false, "two" to true, "pass" to true, "three" to true),
            rows.associate { it.sessionId to it.counted },
        )
    }

    @Test fun siteNopesAttributeToHostingBrowserShareItsSequenceAndStatsWriteFailureRollsBackPairedEvent() = runBlocking {
        db.withTransaction { StatsLedger(db.statsDao()).record("site", "example.com", "site", "browser", false, now, zone) }
        now += 60_000
        // An app nope on the same browser package continues the site nope's dedup sequence.
        db.withTransaction { StatsLedger(db.statsDao()).record("app-on-browser", "browser", "app", "browser", false, now, zone) }
        // Without a known hosting browser a site outcome is never recorded against the domain.
        db.withTransaction { StatsLedger(db.statsDao()).record("no-host", "example.com", "site", null, false, now, zone) }
        val sites = db.statsDao().outcomes("0000")
        assertEquals(listOf("site", "app-on-browser"), sites.map { it.sessionId })
        assertTrue(sites.all { it.packageName == "browser" })
        assertEquals(listOf(true, false), sites.map { it.counted })
        assertEquals(listOf(StatsLedger.FALLBACK_MS, 0L), sites.map { it.savedMs })
        db.openHelper.writableDatabase.execSQL("CREATE TRIGGER fail_stats BEFORE INSERT ON stats_outcome BEGIN SELECT RAISE(ABORT, 'forced'); END")
        try {
            db.withTransaction {
                db.eventDao().insert(Event(name = "walk_away", timestampUtc = now))
                StatsLedger(db.statsDao()).record("retry", "app", "app", null, false, now, zone)
            }
            fail("Expected rollback")
        } catch (_: android.database.sqlite.SQLiteException) { }
        assertTrue(db.eventDao().getAll().isEmpty())
        db.openHelper.writableDatabase.execSQL("DROP TRIGGER fail_stats")
        nope("retry")
        assertEquals(3, db.statsDao().outcomes("0000").size)
    }

    @Test fun enabledSiteBlockShowsHostingBrowserRowWithSiteSavingsAndFirstBrowserVisitSpent() = runBlocking {
        val sites = StatsRepository(db, source, "self", { setOf("app", "browser") }, { now }, { zone },
            siteHostPackages = { setOf("browser", "uninstalled.browser") })
        val blocks = BlockRepository(db, clock = { now })
        fun draft(apps: List<String>, domains: List<String>) =
            BlockDraft("B", apps, domains, FrictionType.TYPING, 15, 150, 350, 60, 360)
        val siteBlock = blocks.createBlock(draft(emptyList(), listOf("example.com")))
        blocks.setEnabled(siteBlock, true)
        blocks.setEnabled(blocks.createBlock(draft(listOf("app"), emptyList())), true)
        raw = visits(5_000)
        sites.refresh()
        db.withTransaction { StatsLedger(db.statsDao()).record("site-nope", "example.com", "site", "browser", false, now, zone) }
        db.withTransaction { StatsLedger(db.statsDao()).record("site-pass", "example.com", "site", "browser", true, now, zone) }
        val start = now + 100
        raw = raw + listOf(StatsUsageEvent("b-r", start, "browser", "B", UsageKind.RESUME, "UTC"),
            StatsUsageEvent("b-p", start + 7_000, "browser", "B", UsageKind.PAUSE, "UTC"))
        now += 20_000
        sites.refresh()
        val ready = (sites.state.value as StatsScreenState.Ready).dashboard
        // Installed hosting browser appears (uninstalled one does not); browser has no baseline → fallback.
        assertEquals(listOf("browser", "app"), ready.apps.map { it.packageName })
        assertEquals(listOf(StatsLedger.FALLBACK_MS, 0L), ready.apps.map { it.savedMs })
        assertEquals(2, ready.attempts)
        assertEquals(7_000L, db.statsDao().outcome("site-pass")!!.spentMs)
        assertEquals(7_000L, ready.spentMs)

        blocks.setEnabled(siteBlock, false)
        sites.refresh()
        val withoutSites = (sites.state.value as StatsScreenState.Ready).dashboard
        assertEquals(listOf("app"), withoutSites.apps.map { it.packageName })
        assertEquals(StatsLedger.FALLBACK_MS, withoutSites.savedMs) // historical total retained
    }

    @Test fun repeatedRefreshClaimsFirstVisitOnceAndDeduplicatesOverlappingReads() = runBlocking {
        raw = visits(5_000)
        repo.refresh()
        db.withTransaction { StatsLedger(db.statsDao()).record("pass", "app", "app", null, true, now, zone) }
        val start = now + 100
        raw = raw + listOf(StatsUsageEvent("new-r", start, "app", "A", UsageKind.RESUME, "UTC"),
            StatsUsageEvent("new-p", start + 10_000, "app", "A", UsageKind.PAUSE, "UTC"))
        now += 20_000
        repeat(2) { repo.refresh() }
        assertEquals(10_000L, db.statsDao().outcome("pass")!!.spentMs)
        assertEquals(1, db.statsDao().outcomes("0000").size)
        assertEquals(8, db.statsDao().usage().size)
    }
}

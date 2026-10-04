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
        raw = visits(5_000)
        repo.refresh()
        assertEquals(5_000L, db.statsDao().baselines().single().visitMs)
        nope("old")
        val baseline = db.statsDao().state()!!.baselineId
        now += 600_000
        raw = visits(20_000)
        repo.refresh()
        assertEquals(baseline, db.statsDao().state()!!.baselineId)
        assertEquals(5_000L, db.statsDao().outcome("old")!!.savedMs)
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
        assertEquals(5_000L, db.statsDao().outcome("old")!!.savedMs)
        assertEquals(db.statsDao().baselines().single().visitMs, db.statsDao().outcome("new")!!.savedMs)
        assertEquals(1, db.eventDao().getAll().count { it.name == "baseline_recalibrated" })
    }

    @Test fun insufficientHistoryFallsBackAndFailedRecalibrationPreservesBaseline() = runBlocking {
        raw = visits(5_000).take(4)
        repo.refresh()
        assertEquals(600_000L, db.statsDao().baselines().single().visitMs)
        val before = db.statsDao().state()
        available = false
        assertFalse(repo.recalibrate())
        assertEquals(before, db.statsDao().state())
    }

    @Test fun ignoredNopesNeverExtendWindowAndSuccessEndsSequence() = runBlocking {
        nope("one"); now += 299_000; nope("ignored"); now += 1_000; nope("two")
        db.withTransaction { StatsLedger(db.statsDao()).record("pass", "app", "app", null, true, now, zone) }
        nope("three"); nope("three")
        val rows = db.statsDao().outcomes("0000")
        assertEquals(5, rows.size)
        assertEquals(4, rows.count { it.counted })
    }

    @Test fun siteNopesAttributeToHostingBrowserShareItsSequenceAndStatsWriteFailureRollsBackPairedEvent() = runBlocking {
        db.withTransaction { StatsLedger(db.statsDao()).record("site", "example.com", "site", "browser", false, now, zone) }
        now += 60_000
        // An app nope on the same browser package continues the site nope's five-minute sequence.
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

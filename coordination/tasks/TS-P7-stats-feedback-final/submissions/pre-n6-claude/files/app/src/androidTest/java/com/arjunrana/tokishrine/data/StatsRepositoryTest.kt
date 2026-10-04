package com.arjunrana.tokishrine.data

import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import com.arjunrana.tokishrine.data.db.TokiDatabase
import com.arjunrana.tokishrine.data.entity.Event
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
    @Before fun setup() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), TokiDatabase::class.java).build()
        repo = StatsRepository(db, object : UsageSource {
            override fun hasAccess() = access
            override fun read(start: Long, end: Long) = if (available) raw.filter { it.at in start until end } else null
        }, "self", { setOf("app") }, { now }, { zone })
    }
    @After fun close() = db.close()
    private fun visits(ms: Long) = (1L..3).flatMap { i ->
        val at = now - 3_600_000 + i * 100_000
        listOf(StatsUsageEvent("r:$at", at, "app", "A", UsageKind.RESUME, "UTC"),
            StatsUsageEvent("p:$at", at + ms, "app", "A", UsageKind.PAUSE, "UTC"))
    }
    private suspend fun nope(id: String) = db.withTransaction { StatsLedger(db.statsDao()).record(id, "app", "app", false, now, zone) }

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
        db.withTransaction { StatsLedger(db.statsDao()).record("pass", "app", "app", true, now, zone) }
        nope("three"); nope("three")
        val rows = db.statsDao().outcomes("0000")
        assertEquals(5, rows.size)
        assertEquals(4, rows.count { it.counted })
    }

    @Test fun sitesAreExcludedAndStatsWriteFailureRollsBackPairedEvent() = runBlocking {
        db.withTransaction { StatsLedger(db.statsDao()).record("site", "example.com", "site", false, now, zone) }
        assertTrue(db.statsDao().outcomes("0000").isEmpty())
        db.openHelper.writableDatabase.execSQL("CREATE TRIGGER fail_stats BEFORE INSERT ON stats_outcome BEGIN SELECT RAISE(ABORT, 'forced'); END")
        try {
            db.withTransaction {
                db.eventDao().insert(Event(name = "walk_away", timestampUtc = now))
                StatsLedger(db.statsDao()).record("retry", "app", "app", false, now, zone)
            }
            fail("Expected rollback")
        } catch (_: android.database.sqlite.SQLiteException) { }
        assertTrue(db.eventDao().getAll().isEmpty())
        db.openHelper.writableDatabase.execSQL("DROP TRIGGER fail_stats")
        nope("retry")
        assertEquals(1, db.statsDao().outcomes("0000").size)
    }

    @Test fun repeatedRefreshClaimsFirstVisitOnceAndDeduplicatesOverlappingReads() = runBlocking {
        raw = visits(5_000)
        repo.refresh()
        db.withTransaction { StatsLedger(db.statsDao()).record("pass", "app", "app", true, now, zone) }
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

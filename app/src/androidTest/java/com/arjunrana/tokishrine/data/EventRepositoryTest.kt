package com.arjunrana.tokishrine.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.arjunrana.tokishrine.data.db.TokiDatabase
import com.arjunrana.tokishrine.data.entity.AppMeta
import com.arjunrana.tokishrine.data.entity.Event
import com.arjunrana.tokishrine.data.permissions.AppPermission
import com.arjunrana.tokishrine.data.permissions.PermissionEventLogic
import com.arjunrana.tokishrine.data.repo.EventRepository
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EventRepositoryTest {

    private lateinit var db: TokiDatabase
    private lateinit var repo: EventRepository
    private var now: Long = 0

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            TokiDatabase::class.java,
        ).build()
        now = System.currentTimeMillis()
        repo = EventRepository(db, clock = { now })
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun insert(
        name: String,
        timestampUtc: Long,
        target: String? = null,
        targetType: String? = null,
    ) = runBlocking {
        db.eventDao().insert(
            Event(name = name, timestampUtc = timestampUtc, target = target, targetType = targetType),
        )
    }

    // Local noon of the day `daysAgo` before today. Noon keeps every seeded
    // event in exactly one calendar-day bucket regardless of when the test
    // runs, so Best day / This week are deterministic.
    private fun localNoonDaysAgo(daysAgo: Long): Long =
        ZonedDateTime.now(ZoneId.systemDefault())
            .minusDays(daysAgo)
            .toLocalDate()
            .atTime(12, 0)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

    // Acceptance: insert 100 events, query walk-aways for the last 7 days,
    // count is correct.
    @Test
    fun hundredEventsWalkAwaysLastSevenDaysCorrect() = runBlocking {
        repeat(35) {
            insert(
                EventRepository.EVENT_WALK_AWAY,
                now - 1L * 60 * 60 * 1000,
                target = "com.instagram.android",
                targetType = EventRepository.TARGET_TYPE_APP,
            )
        }
        repeat(25) { insert(EventRepository.EVENT_WALK_AWAY, now - 30L * 24 * 60 * 60 * 1000) }
        repeat(20) { insert(EventRepository.EVENT_CHALLENGE_COMPLETED, now - 2L * 24 * 60 * 60 * 1000) }
        repeat(20) { insert(EventRepository.EVENT_BLOCK_SCREEN_SHOWN, now - 10L * 24 * 60 * 60 * 1000) }

        assertEquals(100, repo.countAllEvents())

        // Rolling 7×24h window.
        val rollingSince = now - 7L * 24 * 60 * 60 * 1000
        assertEquals(35, repo.countWalkAwaysSince(rollingSince))

        // The calendar-day "This week" window must agree on this fixture:
        // the −1h walk-aways are inside it, the −30d ones far outside.
        assertEquals(35, repo.getStats(now, ZoneId.systemDefault()).thisWeek)
    }

    // Acceptance: the Stats formulas in PRD §9 return correct values against
    // a seeded fixture, including walk-away rate.
    @Test
    fun statsFormulasMatchSeededFixture() = runBlocking {
        val zone = ZoneId.systemDefault()

        // First launch 10 days ago → Days active = 10.
        db.appMetaDao().putIfAbsent(
            AppMeta(AppMeta.KEY_FIRST_LAUNCH_AT, localNoonDaysAgo(10).toString()),
        )

        // 14 walk-aways: today ×3, yesterday ×2, 3 days ago ×4, 12 days ago ×5.
        // Per-app: instagram 6, reddit 5, youtube 3.
        val seed = listOf(
            Pair(0L, "com.instagram.android"), Pair(0L, "com.instagram.android"),
            Pair(0L, "com.reddit.frontpage"),
            Pair(1L, "com.instagram.android"), Pair(1L, "com.reddit.frontpage"),
            Pair(3L, "com.instagram.android"), Pair(3L, "com.instagram.android"),
            Pair(3L, "com.instagram.android"), Pair(3L, "com.youtube.app"),
            Pair(12L, "com.reddit.frontpage"), Pair(12L, "com.reddit.frontpage"),
            Pair(12L, "com.reddit.frontpage"), Pair(12L, "com.youtube.app"),
            Pair(12L, "com.youtube.app"),
        )
        seed.forEach { (daysAgo, target) ->
            insert(
                EventRepository.EVENT_WALK_AWAY,
                localNoonDaysAgo(daysAgo),
                target = target,
                targetType = EventRepository.TARGET_TYPE_APP,
            )
        }

        // 6 completed challenges count in the rate denominator; historical
        // challenge_abandoned rows (the event is retired, Phase 7) and
        // turn-offs must not.
        repeat(6) { insert(EventRepository.EVENT_CHALLENGE_COMPLETED, localNoonDaysAgo(2)) }
        repeat(2) { insert("challenge_abandoned", localNoonDaysAgo(2)) }
        insert(EventRepository.EVENT_TURNOFF_COMPLETED, localNoonDaysAgo(4))

        val stats = repo.getStats(now, zone)

        assertEquals(14, stats.totalWalkAways)
        assertEquals(10, stats.daysActive)
        // Today 3 + yesterday 2 + 3-days-ago 4; the 12-days-ago 5 fall outside.
        assertEquals(9, stats.thisWeek)
        assertEquals(5, stats.bestDay)
        assertEquals(0.7, stats.walkAwayRate, 1e-9)
        assertEquals(3, stats.mostWalkedAwayFrom.size)
        // Descending by walk-away count, per PRD §9.
        assertEquals("com.instagram.android", stats.mostWalkedAwayFrom[0].target)
        assertEquals(6, stats.mostWalkedAwayFrom[0].count)
        assertEquals("com.reddit.frontpage", stats.mostWalkedAwayFrom[1].target)
        assertEquals(5, stats.mostWalkedAwayFrom[1].count)
        assertEquals("com.youtube.app", stats.mostWalkedAwayFrom[2].target)
        assertEquals(3, stats.mostWalkedAwayFrom[2].count)
    }

    // P2 fix: the leaderboard is per-app; website walk-aways count in global
    // totals but never appear as leaderboard rows. Both packages and domains
    // contain dots, so the stored target_type decides — never the shape.
    @Test
    fun leaderboardExcludesWebsiteTargetsButTotalsStayInclusive() = runBlocking {
        insert(
            EventRepository.EVENT_WALK_AWAY,
            now - 1,
            target = "com.instagram.android",
            targetType = EventRepository.TARGET_TYPE_APP,
        )
        insert(
            EventRepository.EVENT_WALK_AWAY,
            now - 2,
            target = "reddit.com",
            targetType = EventRepository.TARGET_TYPE_SITE,
        )
        insert(
            EventRepository.EVENT_WALK_AWAY,
            now - 3,
            target = "reddit.com",
            targetType = EventRepository.TARGET_TYPE_SITE,
        )

        val stats = repo.getStats(now, ZoneId.systemDefault())

        assertEquals(3, stats.totalWalkAways)
        assertEquals(1, stats.mostWalkedAwayFrom.size)
        assertEquals("com.instagram.android", stats.mostWalkedAwayFrom[0].target)
        assertEquals(1, stats.mostWalkedAwayFrom[0].count)
    }

    // Phase 7 retirement: rows for retired event names are historical data —
    // they stay readable in the event table (never deleted by the app) and
    // stay excluded from every §9 figure. Seeded literal names mirror what a
    // pre-retirement install actually wrote.
    @Test
    fun retiredEventRowsArePreservedAndExcludedFromStats() = runBlocking {
        listOf(
            "challenge_abandoned",
            "bubble_dragged",
            "block_conflict_shown",
            "countdown_stalled",
        ).forEach { name ->
            insert(name, now - 1, target = "com.instagram.android", targetType = EventRepository.TARGET_TYPE_APP)
        }

        val stats = repo.getStats(now, ZoneId.systemDefault())

        assertEquals(4, repo.countAllEvents())
        assertEquals(0, stats.totalWalkAways)
        assertEquals(0, stats.thisWeek)
        assertEquals(0, stats.bestDay)
        assertEquals(0.0, stats.walkAwayRate, 0.0)
        assertTrue(stats.mostWalkedAwayFrom.isEmpty())
    }

    // Phase 7 acceptance: empty data renders zeros, not NaN or crashes — the
    // walk-away rate is defined as 0 with no denominator.
    @Test
    fun statsWithNoEventsAreAllZero() = runBlocking {
        val stats = repo.getStats(now, ZoneId.systemDefault())

        assertEquals(0, stats.totalWalkAways)
        // The repository records first launch on read, so today is day 0.
        assertEquals(0, stats.daysActive)
        assertEquals(0, stats.thisWeek)
        assertEquals(0, stats.bestDay)
        assertEquals(0.0, stats.walkAwayRate, 0.0)
        assertTrue(stats.mostWalkedAwayFrom.isEmpty())
    }

    // Phase 7 feature-engagement events are ordinary rows: each Phase 7
    // screen/entry logs exactly its §10 event through the shared write path.
    @Test
    fun phaseSevenEngagementEventsWriteThroughLog() = runBlocking {
        repo.log(EventRepository.EVENT_STATS_VIEWED)
        repo.log(EventRepository.EVENT_SETTINGS_VIEWED)
        repo.log(EventRepository.EVENT_FEEDBACK_OPENED)
        repo.log(EventRepository.EVENT_FEEDBACK_SENT)

        listOf(
            EventRepository.EVENT_STATS_VIEWED,
            EventRepository.EVENT_SETTINGS_VIEWED,
            EventRepository.EVENT_FEEDBACK_OPENED,
            EventRepository.EVENT_FEEDBACK_SENT,
        ).forEach { name ->
            assertEquals(1, db.eventDao().countByName(name))
        }
    }

    // Phase 7 migration posture: with fallbackToDestructiveMigration removed,
    // reopening an existing database file must carry every prior row —
    // active and retired — into the new process untouched. This is the
    // non-destructive open the shipped TokiApplication now performs.
    @Test
    fun existingRowsSurviveReopenWithoutDestructiveFallback() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val dbName = "p7-reopen-preservation.db"
        context.getDatabasePath(dbName).parentFile?.mkdirs()
        context.getDatabasePath(dbName).delete()

        val first = Room.databaseBuilder(context, TokiDatabase::class.java, dbName).build()
        first.eventDao().insert(
            Event(
                name = EventRepository.EVENT_WALK_AWAY,
                timestampUtc = now,
                target = "com.instagram.android",
                targetType = EventRepository.TARGET_TYPE_APP,
            ),
        )
        first.eventDao().insert(Event(name = "challenge_abandoned", timestampUtc = now))
        first.appMetaDao().putIfAbsent(AppMeta(AppMeta.KEY_FIRST_LAUNCH_AT, (now - 86_400_000L).toString()))
        first.close()

        val second = Room.databaseBuilder(context, TokiDatabase::class.java, dbName).build()
        val reopened = EventRepository(second, clock = { now })
        val stats = reopened.getStats(now, ZoneId.systemDefault())

        // The active walk-away and the retired row both survive the reopen.
        assertEquals(2, reopened.countAllEvents())
        assertEquals(1, stats.totalWalkAways)
        assertEquals(1, stats.daysActive)

        second.close()
        context.getDatabasePath(dbName).delete()
    }

    @Test
    fun logRecordsEventsAndParams() = runBlocking {
        repo.log(
            EventRepository.EVENT_BLOCK_SCREEN_SHOWN,
            blockId = 3,
            target = "com.instagram.android",
            targetType = EventRepository.TARGET_TYPE_APP,
            params = mapOf("trigger_type" to "app", "latency_ms" to 42L),
        )

        assertEquals(1, repo.countAllEvents())
        val stored = db.eventDao().getAll().single()
        assertEquals(EventRepository.EVENT_BLOCK_SCREEN_SHOWN, stored.name)
        assertEquals(3L, stored.blockId)
        assertEquals("com.instagram.android", stored.target)
        assertEquals(EventRepository.TARGET_TYPE_APP, stored.targetType)
        // Parse the JSON and verify the actual key/value round-trip.
        val params = JSONObject(stored.paramsJson!!)
        assertEquals("app", params.getString("trigger_type"))
        assertEquals(42L, params.getLong("latency_ms"))
    }

    @Test
    fun logRejectsTargetWithoutType() {
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                repo.log(EventRepository.EVENT_WALK_AWAY, target = "reddit.com")
            }
        }
    }

    @Test
    fun firstLaunchRecordedOnceAcrossManyLogs() = runBlocking {
        repo.log(EventRepository.EVENT_STATS_VIEWED)
        db.appMetaDao().putIfAbsent(AppMeta(AppMeta.KEY_FIRST_LAUNCH_AT, "12345"))
        // The meta row already exists, so a later log must not overwrite it —
        // putIfAbsent ignores, and the original first-launch stays.
        val first = db.appMetaDao().get(AppMeta.KEY_FIRST_LAUNCH_AT)
        repo.log(EventRepository.EVENT_STATS_VIEWED)
        assertEquals(first, db.appMetaDao().get(AppMeta.KEY_FIRST_LAUNCH_AT))
    }

    @Test
    fun onboardingCompletionCommitsMarkerAndEventAtMostOnce() = runBlocking {
        assertTrue(repo.completeOnboarding(grantedCount = 3))
        assertEquals(false, repo.completeOnboarding(grantedCount = 4))

        assertNotNull(db.appMetaDao().get(AppMeta.KEY_ONBOARDING_COMPLETED_AT))
        val events = db.eventDao().getAll()
            .filter { it.name == EventRepository.EVENT_ONBOARDING_COMPLETED }
        assertEquals(1, events.size)
        assertEquals(3, JSONObject(events.single().paramsJson!!).getInt("granted_count"))
    }

    @Test
    fun onboardingCompletionRollsBackMarkerWhenEventInsertFails() = runBlocking {
        db.openHelper.writableDatabase.execSQL(
            """CREATE TRIGGER fail_onboarding_event
                BEFORE INSERT ON event
                WHEN NEW.name = '${EventRepository.EVENT_ONBOARDING_COMPLETED}'
                BEGIN SELECT RAISE(ABORT, 'forced onboarding event failure'); END""",
        )

        assertThrows(android.database.sqlite.SQLiteException::class.java) {
            runBlocking { repo.completeOnboarding(grantedCount = 2) }
        }

        assertNull(db.appMetaDao().get(AppMeta.KEY_ONBOARDING_COMPLETED_AT))
        assertNull(db.appMetaDao().get(AppMeta.KEY_FIRST_LAUNCH_AT))
        assertEquals(0, db.eventDao().countByName(EventRepository.EVENT_ONBOARDING_COMPLETED))
    }

    @Test
    fun restoredPermissionOutcomeUsesRoomAndRetriesAfterFailedWriteExactlyOnce() = runBlocking {
        val pending = mutableListOf(AppPermission.OVERLAY)
        val emit: suspend (String, Map<String, Any?>) -> Unit = { name, params ->
            repo.log(name, params = params)
        }
        db.openHelper.writableDatabase.execSQL(
            """CREATE TRIGGER fail_permission_outcome
                BEFORE INSERT ON event
                WHEN NEW.name = '${EventRepository.EVENT_PERMISSION_GRANTED}'
                BEGIN SELECT RAISE(ABORT, 'forced permission event failure'); END""",
        )

        val beforeRecreation = PermissionEventLogic(pending, emit)
        assertThrows(android.database.sqlite.SQLiteException::class.java) {
            runBlocking { beforeRecreation.settle { true } }
        }
        assertEquals(listOf(AppPermission.OVERLAY), pending)
        assertEquals(0, db.eventDao().countByName(EventRepository.EVENT_PERMISSION_GRANTED))

        db.openHelper.writableDatabase.execSQL("DROP TRIGGER fail_permission_outcome")
        val afterRecreation = PermissionEventLogic(pending, emit)
        afterRecreation.settle { true }
        afterRecreation.settle { true }

        assertTrue(pending.isEmpty())
        val outcomes = db.eventDao().getAll()
            .filter { it.name == EventRepository.EVENT_PERMISSION_GRANTED }
        assertEquals(1, outcomes.size)
        assertEquals("overlay", JSONObject(outcomes.single().paramsJson!!).getString("permission"))
    }
}

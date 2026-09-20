package com.arjunrana.tokishrine.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.arjunrana.tokishrine.challenge.CompletionRequest
import com.arjunrana.tokishrine.data.db.TokiDatabase
import com.arjunrana.tokishrine.data.entity.Event
import com.arjunrana.tokishrine.data.entity.FrictionType
import com.arjunrana.tokishrine.data.repo.BlockDraft
import com.arjunrana.tokishrine.data.repo.BlockRepository
import com.arjunrana.tokishrine.data.repo.ChallengeRepository
import com.arjunrana.tokishrine.data.repo.EventRepository
import com.arjunrana.tokishrine.ui.interruption.ChallengePurpose
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChallengeRepositoryTest {
    private lateinit var db: TokiDatabase
    private lateinit var blocks: BlockRepository
    private lateinit var challenges: ChallengeRepository
    private var now = 0L

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            TokiDatabase::class.java,
        ).build()
        now = ZonedDateTime.now().withHour(12).withMinute(0).withSecond(0).withNano(0)
            .toInstant().toEpochMilli()
        blocks = BlockRepository(db, clock = { now })
        challenges = ChallengeRepository(db, clock = { now }, zone = { ZoneId.systemDefault() })
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun walkAwayInsertAndInclusiveLocalDayCountShareOneTransaction() = runBlocking {
        val blockId = createEnabledBlock()
        db.eventDao().insert(
            Event(
                name = EventRepository.EVENT_WALK_AWAY,
                timestampUtc = now - 86_400_000L,
                blockId = blockId,
                target = "old.example",
                targetType = EventRepository.TARGET_TYPE_SITE,
            ),
        )

        assertEquals(
            1,
            challenges.recordWalkAwayAndCount(
                "walk-1",
                blockId,
                "com.example.target",
                EventRepository.TARGET_TYPE_APP,
                "block_screen",
            ),
        )
        assertEquals(
            2,
            challenges.recordWalkAwayAndCount(
                "walk-2",
                blockId,
                "example.com",
                EventRepository.TARGET_TYPE_SITE,
                "typing",
            ),
        )
        assertEquals(
            2,
            challenges.recordWalkAwayAndCount(
                "walk-2",
                blockId,
                "example.com",
                EventRepository.TARGET_TYPE_SITE,
                "typing",
            ),
        )
        val today = db.eventDao().getAll().filter { it.timestampUtc == now }
        assertEquals(listOf("block_screen", "typing"), today.map { JSONObject(it.paramsJson!!).getString("source") })
    }

    @Test
    fun disableMutationAndAllTerminalEventsCommitAtMostOnce() = runBlocking {
        val blockId = createEnabledBlock()
        val completion = typingCompletion(blockId, "terminal-once")

        assertTrue(challenges.completeTurnOff(completion))
        assertTrue(challenges.completeTurnOff(completion))

        assertFalse(blocks.getBlockWithContents(blockId)!!.block.enabled)
        val names = db.eventDao().getAll().map { it.name }
        assertEquals(0, names.count { it == EventRepository.EVENT_CHALLENGE_COMPLETED })
        assertEquals(1, names.count { it == EventRepository.EVENT_TURNOFF_COMPLETED })
        assertEquals(1, names.count { it == EventRepository.EVENT_BLOCK_TURNED_OFF })
    }

    @Test
    fun completedTurnOffDoesNotChangeWalkAwayRate() = runBlocking {
        val blockId = createEnabledBlock()
        challenges.recordWalkAwayAndCount(
            sessionId = "walk-before-turnoff",
            blockId = blockId,
            target = "com.example.target",
            targetType = EventRepository.TARGET_TYPE_APP,
            source = "block_screen",
        )
        val events = EventRepository(db, clock = { now })
        assertEquals(1.0, events.getStats(now).walkAwayRate, 0.0)

        assertTrue(challenges.completeTurnOff(typingCompletion(blockId, "stats-boundary")))

        assertEquals(1.0, events.getStats(now).walkAwayRate, 0.0)
        assertEquals(0, db.eventDao().countByName(EventRepository.EVENT_CHALLENGE_COMPLETED))
        assertEquals(1, db.eventDao().countByName(EventRepository.EVENT_TURNOFF_COMPLETED))
    }

    @Test
    fun waitingDisableUsesStoredFixedChoiceAndRecordsCountdownOutcome() = runBlocking {
        val blockId = createEnabledBlock(FrictionType.DELAY)
        val completion = CompletionRequest(
            token = 2,
            sessionId = "waiting-terminal",
            blockId = blockId,
            purpose = ChallengePurpose.TURN_OFF,
            method = FrictionType.DELAY,
            durationMs = 360_400,
            attempts = 1,
            countdownElapsedMs = 360_000,
            configuredAmount = 360,
            pauseMinutes = 15,
        )

        assertTrue(challenges.completeTurnOff(completion))
        val events = db.eventDao().getAll()
        assertEquals(1, events.count { it.name == EventRepository.EVENT_COUNTDOWN_COMPLETED })
        val countdown = events.single { it.name == EventRepository.EVENT_COUNTDOWN_COMPLETED }
        assertEquals(360_000L, JSONObject(countdown.paramsJson!!).getLong("elapsed_ms"))
    }

    @Test
    fun failedTerminalEventRollsBackDisableAndRetrySucceeds() = runBlocking {
        val blockId = createEnabledBlock()
        val completion = typingCompletion(blockId, "retry-session")
        db.openHelper.writableDatabase.execSQL(
            """CREATE TRIGGER fail_turnoff_completion
                BEFORE INSERT ON event
                WHEN NEW.name = '${EventRepository.EVENT_TURNOFF_COMPLETED}'
                BEGIN SELECT RAISE(ABORT, 'forced turnoff failure'); END""",
        )

        assertThrows(android.database.sqlite.SQLiteException::class.java) {
            runBlocking { challenges.completeTurnOff(completion) }
        }
        assertTrue(blocks.getBlockWithContents(blockId)!!.block.enabled)
        assertEquals(0, db.eventDao().countByName(EventRepository.EVENT_CHALLENGE_COMPLETED))
        assertEquals(0, db.eventDao().countByName(EventRepository.EVENT_BLOCK_TURNED_OFF))

        db.openHelper.writableDatabase.execSQL("DROP TRIGGER fail_turnoff_completion")
        assertTrue(challenges.completeTurnOff(completion))
        assertFalse(blocks.getBlockWithContents(blockId)!!.block.enabled)
    }

    @Test
    fun missingDisabledAndChangedBlocksDoNotFabricateSuccess() = runBlocking {
        assertFalse(challenges.completeTurnOff(typingCompletion(999, "missing")))

        val changed = createEnabledBlock()
        assertFalse(
            challenges.completeTurnOff(
                typingCompletion(changed, "changed").copy(configuredAmount = 220),
            ),
        )
        assertTrue(blocks.getBlockWithContents(changed)!!.block.enabled)

        val alreadyOff = createEnabledBlock()
        blocks.setEnabled(alreadyOff, false)
        assertFalse(challenges.completeTurnOff(typingCompletion(alreadyOff, "already-off")))
        assertEquals(0, db.eventDao().countByName(EventRepository.EVENT_CHALLENGE_COMPLETED))
        assertEquals(0, db.eventDao().countByName(EventRepository.EVENT_TURNOFF_COMPLETED))
    }

    @Test
    fun startedAndPauseCompletionAreIdempotentByStableSession() = runBlocking {
        val blockId = createEnabledBlock()
        repeat(2) {
            challenges.recordStarted(
                "pause-session",
                blockId,
                ChallengePurpose.PAUSE,
                FrictionType.TYPING,
                150,
            )
        }
        val request = typingCompletion(blockId, "pause-session").copy(
            purpose = ChallengePurpose.PAUSE,
            configuredAmount = 150,
            pauseMinutes = 15,
        )
        assertTrue(challenges.completePause(request))
        assertTrue(challenges.completePause(request))

        assertEquals(1, db.eventDao().countByName(EventRepository.EVENT_CHALLENGE_STARTED))
        assertEquals(1, db.eventDao().countByName(EventRepository.EVENT_CHALLENGE_COMPLETED))
        assertTrue(blocks.getBlockWithContents(blockId)!!.block.enabled)
    }

    private suspend fun createEnabledBlock(method: FrictionType = FrictionType.TYPING): Long {
        val id = blocks.createBlock(
            BlockDraft(
                name = "Social",
                appPackageNames = listOf("com.example.target.${System.nanoTime()}"),
                siteDomains = emptyList(),
                frictionType = method,
                pauseMinutes = 15,
                pauseChars = 150,
                turnoffChars = 350,
                countdownSeconds = 60,
                turnoffSeconds = 360,
            ),
        )
        blocks.setEnabled(id, true)
        return id
    }

    private fun typingCompletion(blockId: Long, sessionId: String) = CompletionRequest(
        token = 1,
        sessionId = sessionId,
        blockId = blockId,
        purpose = ChallengePurpose.TURN_OFF,
        method = FrictionType.TYPING,
        durationMs = 4_000,
        attempts = 2,
        countdownElapsedMs = 0,
        configuredAmount = 350,
        pauseMinutes = 15,
    )
}

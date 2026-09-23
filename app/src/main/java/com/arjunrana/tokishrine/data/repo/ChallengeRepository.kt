package com.arjunrana.tokishrine.data.repo

import androidx.room.withTransaction
import com.arjunrana.tokishrine.challenge.CompletionRequest
import com.arjunrana.tokishrine.data.db.TokiDatabase
import com.arjunrana.tokishrine.data.entity.AppMeta
import com.arjunrana.tokishrine.data.entity.Event
import com.arjunrana.tokishrine.data.entity.FrictionType
import com.arjunrana.tokishrine.ui.interruption.ChallengePurpose
import java.time.Instant
import java.time.ZoneId
import org.json.JSONObject

/** Room boundary for Phase 5 challenge events and terminal mutations. */
class ChallengeRepository(
    private val db: TokiDatabase,
    private val clock: () -> Long = System::currentTimeMillis,
    private val zone: () -> ZoneId = ZoneId::systemDefault,
) {
    private val blocks = db.blockDao()
    private val events = db.eventDao()
    private val meta = db.appMetaDao()

    suspend fun recordStarted(
        sessionId: String,
        blockId: Long,
        purpose: ChallengePurpose,
        method: FrictionType,
        configuredAmount: Int,
    ) = db.withTransaction {
        val markerKey = startedMarker(sessionId)
        if (meta.get(markerKey) != null) return@withTransaction
        ensureFirstLaunch()
        insert(
            EventRepository.EVENT_CHALLENGE_STARTED,
            blockId,
            params = mapOf(
                "type" to method.eventValue,
            ),
        )
        if (method == FrictionType.DELAY) {
            insert(
                EventRepository.EVENT_COUNTDOWN_STARTED,
                blockId,
                params = mapOf("seconds" to configuredAmount),
            )
        }
        if (purpose == ChallengePurpose.TURN_OFF) {
            insert(
                EventRepository.EVENT_TURNOFF_STARTED,
                blockId,
                params = buildMap {
                    put("type", method.eventValue)
                    put(if (method == FrictionType.TYPING) "chars" else "seconds", configuredAmount)
                },
            )
        }
        meta.putIfAbsent(AppMeta(markerKey, clock().toString()))
    }

    suspend fun recordTypingMismatch(blockId: Long, charsTyped: Int) {
        db.withTransaction {
            ensureFirstLaunch()
            insert(
                EventRepository.EVENT_TYPING_MISMATCH,
                blockId,
                params = mapOf("chars_typed" to charsTyped),
            )
        }
    }

    /** Inserts the walk-away before reading the inclusive local-day global count. */
    suspend fun recordWalkAwayAndCount(
        sessionId: String,
        blockId: Long,
        target: String,
        targetType: String,
        source: String,
    ): Int = db.withTransaction {
        require(targetType == EventRepository.TARGET_TYPE_APP || targetType == EventRepository.TARGET_TYPE_SITE)
        val markerKey = walkAwayMarker(sessionId)
        val existingTime = meta.get(markerKey)?.toLongOrNull()
        val eventTime = existingTime ?: clock()
        if (existingTime == null) {
            ensureFirstLaunch()
            events.insert(
                Event(
                    name = EventRepository.EVENT_WALK_AWAY,
                    timestampUtc = eventTime,
                    blockId = blockId,
                    target = target,
                    targetType = targetType,
                    paramsJson = json(mapOf("source" to source)),
                ),
            )
            meta.putIfAbsent(AppMeta(markerKey, eventTime.toString()))
        }
        val dayStart = Instant.ofEpochMilli(eventTime)
            .atZone(zone())
            .toLocalDate()
            .atStartOfDay(zone())
            .toInstant()
            .toEpochMilli()
        events.countByNameSince(EventRepository.EVENT_WALK_AWAY, dayStart)
    }

    suspend fun recordTurnOffAbandoned(blockId: Long, progressPct: Int) = db.withTransaction {
        ensureFirstLaunch()
        insert(
            EventRepository.EVENT_TURNOFF_ABANDONED,
            blockId,
            params = mapOf("progress_pct" to progressPct),
        )
    }

    /**
     * Persists a pause challenge outcome before the Phase 6 seam is emitted.
     * The countdown outcome, when present, cannot be split from completion.
     */
    suspend fun completePause(request: CompletionRequest): Boolean = db.withTransaction {
        require(request.purpose == ChallengePurpose.PAUSE)
        if (completionExists(request.sessionId)) return@withTransaction true
        val block = blocks.getBlock(request.blockId)
            ?: return@withTransaction false
        if (!block.enabled || block.frictionType != request.method) return@withTransaction false
        if (!configMatches(block.pauseChars, block.countdownSeconds, request)) return@withTransaction false
        ensureFirstLaunch()
        insertPauseCompletionEvents(request)
        markCompleted(request.sessionId)
        true
    }

    /**
     * The OFF mutation and all terminal events share one Room transaction.
     * A failed event insert rolls the state back; a retry succeeds once; a
     * repeated callback sees OFF and writes nothing.
     */
    suspend fun completeTurnOff(request: CompletionRequest): Boolean = db.withTransaction {
        require(request.purpose == ChallengePurpose.TURN_OFF)
        if (completionExists(request.sessionId)) return@withTransaction true
        val block = blocks.getBlock(request.blockId) ?: return@withTransaction false
        if (!block.enabled || block.frictionType != request.method) return@withTransaction false
        if (!configMatches(block.turnoffChars, block.turnoffSeconds, request)) return@withTransaction false
        if (blocks.setEnabled(block.id, false) == 0) return@withTransaction false

        ensureFirstLaunch()
        insertCountdownCompletionEvent(request)
        insert(
            EventRepository.EVENT_TURNOFF_COMPLETED,
            block.id,
            params = mapOf("duration_ms" to request.durationMs),
        )
        insert(EventRepository.EVENT_BLOCK_TURNED_OFF, block.id)
        markCompleted(request.sessionId)
        true
    }

    private suspend fun insertPauseCompletionEvents(request: CompletionRequest) {
        insert(
            EventRepository.EVENT_CHALLENGE_COMPLETED,
            request.blockId,
            params = mapOf(
                "type" to request.method.eventValue,
                "duration_ms" to request.durationMs,
                "attempts" to request.attempts,
            ),
        )
        insertCountdownCompletionEvent(request)
    }

    private suspend fun insertCountdownCompletionEvent(request: CompletionRequest) {
        if (request.method == FrictionType.DELAY) {
            insert(
                EventRepository.EVENT_COUNTDOWN_COMPLETED,
                request.blockId,
                params = mapOf(
                    "wall_ms" to request.durationMs,
                    "elapsed_ms" to request.countdownElapsedMs,
                ),
            )
        }
    }

    private fun configMatches(typingChars: Int, waitingSeconds: Int, request: CompletionRequest): Boolean =
        if (request.method == FrictionType.TYPING) {
            typingChars == request.configuredAmount
        } else {
            waitingSeconds == request.configuredAmount
        }

    private suspend fun ensureFirstLaunch() {
        meta.putIfAbsent(AppMeta(AppMeta.KEY_FIRST_LAUNCH_AT, clock().toString()))
    }

    private suspend fun completionExists(sessionId: String): Boolean =
        meta.get(completedMarker(sessionId)) != null

    private suspend fun markCompleted(sessionId: String) {
        meta.putIfAbsent(AppMeta(completedMarker(sessionId), clock().toString()))
    }

    private fun startedMarker(sessionId: String) = "challenge_started:$sessionId"
    private fun completedMarker(sessionId: String) = "challenge_completed:$sessionId"
    private fun walkAwayMarker(sessionId: String) = "challenge_walk_away:$sessionId"

    private suspend fun insert(name: String, blockId: Long, params: Map<String, Any?> = emptyMap()) {
        events.insert(
            Event(
                name = name,
                timestampUtc = clock(),
                blockId = blockId,
                paramsJson = json(params),
            ),
        )
    }

    private fun json(params: Map<String, Any?>): String? =
        if (params.isEmpty()) null else JSONObject(params).toString()
}

private val FrictionType.eventValue: String
    get() = if (this == FrictionType.TYPING) "typing" else "delay"

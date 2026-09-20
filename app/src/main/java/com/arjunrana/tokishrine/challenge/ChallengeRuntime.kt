package com.arjunrana.tokishrine.challenge

import com.arjunrana.tokishrine.data.entity.FrictionType
import com.arjunrana.tokishrine.ui.interruption.ChallengePurpose
import kotlin.math.ceil

/** Immutable inputs for one stable challenge session. */
data class ChallengeConfig(
    val sessionId: String,
    val blockId: Long,
    val purpose: ChallengePurpose,
    val method: FrictionType,
    val passage: String = "",
    val totalSeconds: Int = 0,
    val pauseMinutes: Int = 0,
    val target: String? = null,
    val targetType: String? = null,
)

enum class ChallengePhase { GATE, ACTIVE, COMMITTING, WALK_AWAY, TERMINAL }

enum class AbandonReason(val eventValue: String) {
    APP_SWITCH("app_switch"),
    SCREEN_OFF("screen_off"),
}

data class ChallengeSnapshot(
    val phase: ChallengePhase,
    val generation: Int,
    val started: Boolean,
    val startedAtMs: Long?,
    val typedText: String,
    val attempts: Int,
    val accruedVisibleMs: Long,
    val visibleSinceMs: Long?,
)

data class ChallengeView(
    val phase: ChallengePhase,
    val typedText: String,
    val attempts: Int,
    val remainingSeconds: Int,
)

data class CompletionRequest(
    val token: Int,
    val sessionId: String,
    val blockId: Long,
    val purpose: ChallengePurpose,
    val method: FrictionType,
    val durationMs: Long,
    val attempts: Int,
    val countdownElapsedMs: Long,
    val configuredAmount: Int,
    val pauseMinutes: Int,
)

sealed interface ChallengeEffect {
    data class Started(
        val sessionId: String,
        val blockId: Long,
        val purpose: ChallengePurpose,
        val method: FrictionType,
        val configuredAmount: Int,
    ) : ChallengeEffect

    data class TypingMismatch(val blockId: Long, val charsTyped: Int) : ChallengeEffect

    data class WalkAway(
        val sessionId: String,
        val blockId: Long,
        val target: String,
        val targetType: String,
        val source: String,
    ) : ChallengeEffect

    data class Abandoned(
        val blockId: Long,
        val method: FrictionType,
        val progressPct: Int,
        val reason: AbandonReason,
    ) : ChallengeEffect

    data class TurnOffAbandoned(val blockId: Long, val progressPct: Int) : ChallengeEffect
    data class PersistCompletion(val request: CompletionRequest) : ChallengeEffect
}

sealed interface ChallengeTerminalResult {
    data class PauseRequested(
        val blockId: Long,
        val pauseMinutes: Int,
        val sessionId: String,
    ) : ChallengeTerminalResult

    data class BlockDisabled(val blockId: Long) : ChallengeTerminalResult
}

/**
 * Pure deterministic owner of challenge progress and terminality.
 *
 * Android lifecycle callbacks only translate into these inputs. A generation
 * token invalidates stale ticks and callbacks; once persistence begins the
 * completion owns the terminal race, while a cancellation that arrives first
 * invalidates every later completion callback.
 */
class ChallengeRuntime(
    val config: ChallengeConfig,
    restored: ChallengeSnapshot? = null,
) {
    private var phase = restored?.phase ?: if (config.purpose == ChallengePurpose.PAUSE) {
        ChallengePhase.GATE
    } else {
        ChallengePhase.ACTIVE
    }
    private var generation = restored?.generation ?: 0
    private var started = restored?.started ?: false
    private var startedAtMs = restored?.startedAtMs
    private var typedText = restored?.typedText.orEmpty()
    private var attempts = restored?.attempts ?: 0
    private var accruedVisibleMs = restored?.accruedVisibleMs ?: 0L
    private var visibleSinceMs = restored?.visibleSinceMs

    fun snapshot(): ChallengeSnapshot = ChallengeSnapshot(
        phase = phase,
        generation = generation,
        started = started,
        startedAtMs = startedAtMs,
        typedText = typedText,
        attempts = attempts,
        accruedVisibleMs = accruedVisibleMs,
        visibleSinceMs = visibleSinceMs,
    )

    fun view(nowMs: Long): ChallengeView = ChallengeView(
        phase = phase,
        typedText = typedText,
        attempts = attempts,
        remainingSeconds = remainingSeconds(nowMs),
    )

    fun beginTurnOff(nowMs: Long): ChallengeEffect? {
        if (config.purpose != ChallengePurpose.TURN_OFF || phase != ChallengePhase.ACTIVE) return null
        return startOnce(nowMs)
    }

    fun enterChallenge(nowMs: Long): ChallengeEffect? {
        if (phase != ChallengePhase.GATE || config.purpose != ChallengePurpose.PAUSE) return null
        phase = ChallengePhase.ACTIVE
        generation++
        return startOnce(nowMs)
    }

    private fun startOnce(nowMs: Long): ChallengeEffect? {
        if (started) return null
        started = true
        startedAtMs = nowMs
        return ChallengeEffect.Started(
            sessionId = config.sessionId,
            blockId = config.blockId,
            purpose = config.purpose,
            method = config.method,
            configuredAmount = configuredAmount(),
        )
    }

    fun updateTypedText(value: String) {
        if (phase == ChallengePhase.ACTIVE && config.method == FrictionType.TYPING) typedText = value
    }

    fun submitTyping(nowMs: Long): ChallengeEffect? {
        if (phase != ChallengePhase.ACTIVE || config.method != FrictionType.TYPING) return null
        attempts++
        return if (typedText == config.passage) {
            beginCompletion(nowMs)
        } else {
            ChallengeEffect.TypingMismatch(config.blockId, typedText.length)
        }
    }

    fun onVisible(nowMs: Long) {
        if (phase == ChallengePhase.ACTIVE && config.method == FrictionType.DELAY && visibleSinceMs == null) {
            visibleSinceMs = nowMs
        }
    }

    /** Pauses elapsed accounting for a configuration recreation without cancelling the session. */
    fun onConfigurationHidden(nowMs: Long) {
        accrueVisible(nowMs)
    }

    fun tick(nowMs: Long, callbackGeneration: Int = generation): ChallengeEffect? {
        if (callbackGeneration != generation || phase != ChallengePhase.ACTIVE || config.method != FrictionType.DELAY) {
            return null
        }
        if (elapsedVisibleMs(nowMs) < config.totalSeconds * 1_000L) return null
        return beginCompletion(nowMs)
    }

    fun currentGeneration(): Int = generation

    fun escape(nowMs: Long): ChallengeEffect? {
        if (phase == ChallengePhase.GATE) return gateWalkAway()
        if (phase != ChallengePhase.ACTIVE) return null
        accrueVisible(nowMs)
        val progress = progressPct(nowMs)
        terminalize()
        return if (config.purpose == ChallengePurpose.TURN_OFF) {
            ChallengeEffect.TurnOffAbandoned(config.blockId, progress)
        } else {
            challengeWalkAway()
        }
    }

    fun abandon(reason: AbandonReason, nowMs: Long): ChallengeEffect? {
        if (phase != ChallengePhase.ACTIVE) return null
        accrueVisible(nowMs)
        val progress = progressPct(nowMs)
        terminalize()
        return ChallengeEffect.Abandoned(config.blockId, config.method, progress, reason)
    }

    fun gateWalkAway(): ChallengeEffect? {
        if (phase != ChallengePhase.GATE) return null
        terminalize(ChallengePhase.WALK_AWAY)
        return walkAway("block_screen")
    }

    private fun challengeWalkAway(): ChallengeEffect = walkAway(
        if (config.method == FrictionType.TYPING) "typing" else "countdown",
    )

    private fun walkAway(source: String): ChallengeEffect.WalkAway {
        check(config.target != null && config.targetType != null) { "Pause walk-away requires a target" }
        return ChallengeEffect.WalkAway(
            sessionId = config.sessionId,
            blockId = config.blockId,
            target = config.target,
            targetType = config.targetType,
            source = source,
        )
    }

    private fun beginCompletion(nowMs: Long): ChallengeEffect.PersistCompletion {
        accrueVisible(nowMs)
        phase = ChallengePhase.COMMITTING
        val token = ++generation
        return ChallengeEffect.PersistCompletion(completionRequest(token, nowMs))
    }

    /** Reissues an interrupted persistence request with the same stable session id. */
    fun resumePendingCompletion(nowMs: Long): ChallengeEffect.PersistCompletion? {
        if (phase != ChallengePhase.COMMITTING) return null
        return ChallengeEffect.PersistCompletion(completionRequest(generation, nowMs))
    }

    private fun completionRequest(token: Int, nowMs: Long): CompletionRequest {
        val duration = (nowMs - (startedAtMs ?: nowMs)).coerceAtLeast(0L)
        return CompletionRequest(
            token = token,
            sessionId = config.sessionId,
            blockId = config.blockId,
            purpose = config.purpose,
            method = config.method,
            durationMs = duration,
            attempts = if (config.method == FrictionType.TYPING) attempts else 1,
            countdownElapsedMs = accruedVisibleMs,
            configuredAmount = configuredAmount(),
            pauseMinutes = config.pauseMinutes,
        )
    }

    fun commitSucceeded(token: Int): ChallengeTerminalResult? {
        if (phase != ChallengePhase.COMMITTING || token != generation) return null
        terminalize()
        return if (config.purpose == ChallengePurpose.PAUSE) {
            ChallengeTerminalResult.PauseRequested(config.blockId, config.pauseMinutes, config.sessionId)
        } else {
            ChallengeTerminalResult.BlockDisabled(config.blockId)
        }
    }

    fun commitFailed(token: Int, nowMs: Long): Boolean {
        if (phase != ChallengePhase.COMMITTING || token != generation) return false
        phase = ChallengePhase.ACTIVE
        generation++
        if (config.method == FrictionType.DELAY) visibleSinceMs = nowMs
        return true
    }

    private fun terminalize(next: ChallengePhase = ChallengePhase.TERMINAL) {
        phase = next
        visibleSinceMs = null
        generation++
    }

    private fun accrueVisible(nowMs: Long) {
        val since = visibleSinceMs ?: return
        accruedVisibleMs += (nowMs - since).coerceAtLeast(0L)
        visibleSinceMs = null
    }

    private fun elapsedVisibleMs(nowMs: Long): Long = accruedVisibleMs +
        (visibleSinceMs?.let { (nowMs - it).coerceAtLeast(0L) } ?: 0L)

    private fun remainingSeconds(nowMs: Long): Int {
        if (config.method != FrictionType.DELAY) return 0
        val remainingMs = (config.totalSeconds * 1_000L - elapsedVisibleMs(nowMs)).coerceAtLeast(0L)
        return ceil(remainingMs / 1_000.0).toInt()
    }

    private fun progressPct(nowMs: Long): Int = when (config.method) {
        FrictionType.TYPING -> if (config.passage.isEmpty()) 0 else {
            (typedText.length * 100 / config.passage.length).coerceIn(0, 100)
        }
        FrictionType.DELAY -> if (config.totalSeconds <= 0) 0 else {
            (elapsedVisibleMs(nowMs) * 100 / (config.totalSeconds * 1_000L)).toInt().coerceIn(0, 100)
        }
    }

    private fun configuredAmount(): Int = if (config.method == FrictionType.TYPING) {
        config.passage.length
    } else {
        config.totalSeconds
    }
}

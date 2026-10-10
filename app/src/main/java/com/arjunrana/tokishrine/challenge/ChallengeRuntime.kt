package com.arjunrana.tokishrine.challenge

import com.arjunrana.tokishrine.config.AppConfig
import com.arjunrana.tokishrine.data.entity.FrictionType
import com.arjunrana.tokishrine.data.repo.EventRepository
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
    /** Foreground package that fired: the app, or the hosting browser of a site. */
    val hostPackage: String? = null,
)

enum class ChallengePhase { GATE, ACTIVE, COMMITTING, WALK_AWAY, TERMINAL }

data class ChallengeSnapshot(
    val phase: ChallengePhase,
    val generation: Int,
    val started: Boolean,
    val startedAtMs: Long?,
    val typedText: String,
    val showTypingMismatches: Boolean,
    val attempts: Int,
    val accruedVisibleMs: Long,
    val visibleSinceMs: Long?,
    /** Start of the current continuous away period (elapsed realtime), or null while visible. */
    val awayStartMs: Long? = null,
)

data class ChallengeView(
    val phase: ChallengePhase,
    val typedText: String,
    val showTypingMismatches: Boolean,
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
    val target: String? = null,
    val targetType: String? = null,
    val hostPackage: String? = null,
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
        val hostPackage: String? = null,
    ) : ChallengeEffect

    /**
     * The turn-off challenge ended without disabling the block. [auto] marks
     * the 15 s-away auto-nope (P7-F3), which lands on the phone's home screen
     * (P7-F16); the button escape returns to the block in Toki.
     */
    data class TurnOffAbandoned(val blockId: Long, val progressPct: Int, val auto: Boolean = false) : ChallengeEffect
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
    private var showTypingMismatches = restored?.showTypingMismatches ?: false
    private var attempts = restored?.attempts ?: 0
    private var accruedVisibleMs = restored?.accruedVisibleMs ?: 0L
    private var visibleSinceMs = restored?.visibleSinceMs
    private var awayStartMs = restored?.awayStartMs

    fun snapshot(): ChallengeSnapshot = ChallengeSnapshot(
        phase = phase,
        generation = generation,
        started = started,
        startedAtMs = startedAtMs,
        typedText = typedText,
        showTypingMismatches = showTypingMismatches,
        attempts = attempts,
        accruedVisibleMs = accruedVisibleMs,
        visibleSinceMs = visibleSinceMs,
        awayStartMs = awayStartMs,
    )

    fun view(nowMs: Long): ChallengeView = ChallengeView(
        phase = phase,
        typedText = typedText,
        showTypingMismatches = showTypingMismatches,
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
        if (phase == ChallengePhase.ACTIVE && config.method == FrictionType.TYPING) {
            typedText = value
            showTypingMismatches = false
        }
    }

    fun submitTyping(nowMs: Long): ChallengeEffect? {
        if (phase != ChallengePhase.ACTIVE || config.method != FrictionType.TYPING) return null
        if (typedText.length < config.passage.length) return null
        attempts++
        return if (typedText == config.passage) {
            beginCompletion(nowMs)
        } else {
            showTypingMismatches = true
            ChallengeEffect.TypingMismatch(config.blockId, typedText.length)
        }
    }

    /**
     * Starts a visible waiting segment. Ignored while an away period is open
     * (P7-F-A1): a late callback such as a delayed `recordStarted` completion
     * between SCREEN_OFF and `onStop` must not count offscreen time. Only
     * [onReturned] closes the away period and makes the screen visible again.
     */
    fun onVisible(nowMs: Long) {
        if (awayStartMs == null && phase == ChallengePhase.ACTIVE &&
            config.method == FrictionType.DELAY && visibleSinceMs == null
        ) {
            visibleSinceMs = nowMs
        }
    }

    /** Pauses elapsed accounting for a configuration recreation without cancelling the session. */
    fun onConfigurationHidden(nowMs: Long) {
        accrueVisible(nowMs)
    }

    /**
     * App switch, Home, lock or screen-off while the gate or challenge is
     * live starts (or keeps) the away clock (8 October §17 addendum). Waiting
     * progress resets and its generation changes so a late tick from the
     * previous visible segment cannot complete the challenge. Repeated
     * signals keep the earliest away-start.
     */
    fun onBackgrounded(nowMs: Long) {
        if (phase != ChallengePhase.GATE && phase != ChallengePhase.ACTIVE) return
        if (awayStartMs == null) awayStartMs = nowMs
        if (phase == ChallengePhase.ACTIVE && config.method == FrictionType.DELAY) {
            val hadProgress = accruedVisibleMs != 0L || visibleSinceMs != null
            accruedVisibleMs = 0L
            visibleSinceMs = null
            if (hadProgress) generation++
        }
    }

    /**
     * The gate or challenge is visible again. After [AUTO_NOPE_AWAY_MS] away
     * this is the auto-nope; sooner, the same challenge resumes with typed
     * text cleared (the wait already restarted when it was hidden).
     */
    fun onReturned(nowMs: Long): ChallengeEffect? {
        if (awayStartMs == null) return null
        expireAway(nowMs)?.let { return it }
        awayStartMs = null
        if (phase == ChallengePhase.ACTIVE && config.method == FrictionType.TYPING) {
            typedText = ""
            showTypingMismatches = false
        }
        return null
    }

    /**
     * Auto-nope once the away period reaches [AUTO_NOPE_AWAY_MS]: a pause
     * gate/challenge walks away with source `auto_away`; a turn-off challenge
     * ends exactly like Never Mind. A clock that went backwards (reboot)
     * counts as expired. Returns null while still in time, when not away, or
     * once a commit has started (the commit wins).
     */
    fun expireAway(nowMs: Long): ChallengeEffect? {
        val since = awayStartMs ?: return null
        if (phase != ChallengePhase.GATE && phase != ChallengePhase.ACTIVE) return null
        if (nowMs >= since && nowMs - since < AUTO_NOPE_AWAY_MS) return null
        if (phase == ChallengePhase.GATE) {
            terminalize(ChallengePhase.WALK_AWAY)
            return walkAway(EventRepository.WALK_AWAY_SOURCE_AUTO_AWAY)
        }
        val progress = progressPct(nowMs)
        terminalize()
        return if (config.purpose == ChallengePurpose.TURN_OFF) {
            ChallengeEffect.TurnOffAbandoned(config.blockId, progress, auto = true)
        } else {
            walkAway(EventRepository.WALK_AWAY_SOURCE_AUTO_AWAY)
        }
    }

    /** True between leaving the screen and [onReturned]/expiry. */
    fun isAway(): Boolean = awayStartMs != null

    /** Milliseconds until the away period expires; null when not away or no longer live. */
    fun awayRemainingMs(nowMs: Long): Long? {
        val since = awayStartMs ?: return null
        if (phase != ChallengePhase.GATE && phase != ChallengePhase.ACTIVE) return null
        if (nowMs < since) return 0L
        return (AUTO_NOPE_AWAY_MS - (nowMs - since)).coerceAtLeast(0L)
    }

    fun tick(nowMs: Long, callbackGeneration: Int = generation): ChallengeEffect? {
        if (callbackGeneration != generation || phase != ChallengePhase.ACTIVE ||
            config.method != FrictionType.DELAY || awayStartMs != null
        ) {
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

    // challenge_abandoned is retired (Phase 7). Back no longer suspends, and
    // a short absence is nonterminal; 15 s away ends via expireAway(). The
    // intentional escapes are escape()/gateWalkAway(). No abandon() surface.

    fun gateWalkAway(): ChallengeEffect? {
        if (phase != ChallengePhase.GATE) return null
        terminalize(ChallengePhase.WALK_AWAY)
        return walkAway(EventRepository.WALK_AWAY_SOURCE_BLOCK_SCREEN)
    }

    private fun challengeWalkAway(): ChallengeEffect = walkAway(
        if (config.method == FrictionType.TYPING) {
            EventRepository.WALK_AWAY_SOURCE_TYPING
        } else {
            EventRepository.WALK_AWAY_SOURCE_COUNTDOWN
        },
    )

    private fun walkAway(source: String): ChallengeEffect.WalkAway {
        check(config.target != null && config.targetType != null) { "Pause walk-away requires a target" }
        return ChallengeEffect.WalkAway(
            sessionId = config.sessionId,
            blockId = config.blockId,
            target = config.target,
            targetType = config.targetType,
            source = source,
            hostPackage = config.hostPackage,
        )
    }

    private fun beginCompletion(nowMs: Long): ChallengeEffect.PersistCompletion {
        accrueVisible(nowMs)
        phase = ChallengePhase.COMMITTING
        awayStartMs = null
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
            target = config.target,
            targetType = config.targetType,
            hostPackage = config.hostPackage,
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

    /**
     * A completion save threw. Owner rule (8 October, P7-F-A2): a failed save
     * never costs a completed challenge, and never starts the away clock.
     * On screen it behaves as [commitFailed] (back to ACTIVE: a wait
     * re-completes on the next tick, typing keeps its text for Submit).
     * Hidden or screen off, it stays COMMITTING with the same token, so no
     * away clock, wait reset or text clearing applies; the caller reissues
     * [resumePendingCompletion] when the user is next on screen. Returns true
     * only when the challenge went back to ACTIVE.
     */
    fun saveFailed(token: Int, nowMs: Long, onScreen: Boolean): Boolean {
        if (phase != ChallengePhase.COMMITTING || token != generation || !onScreen) return false
        return commitFailed(token, nowMs)
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
        awayStartMs = null
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

    companion object {
        /**
         * Continuous time away before a live gate/challenge auto-nopes
         * (8 October §17 addendum). The editable seconds live in AppConfig
         * (P7-F11); this derived millisecond form is what the runtime reads.
         */
        const val AUTO_NOPE_AWAY_MS = AppConfig.AUTO_NOPE_AWAY_SECONDS * 1000L
    }
}

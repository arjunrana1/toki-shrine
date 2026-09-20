package com.arjunrana.tokishrine

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Bundle
import android.os.PowerManager
import android.os.SystemClock
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.arjunrana.tokishrine.challenge.AbandonReason
import com.arjunrana.tokishrine.challenge.ChallengeConfig
import com.arjunrana.tokishrine.challenge.ChallengeContentSelector
import com.arjunrana.tokishrine.challenge.ChallengeEffect
import com.arjunrana.tokishrine.challenge.ChallengePhase
import com.arjunrana.tokishrine.challenge.ChallengeRuntime
import com.arjunrana.tokishrine.challenge.ChallengeSnapshot
import com.arjunrana.tokishrine.challenge.ChallengeTerminalResult
import com.arjunrana.tokishrine.challenge.isValidDetectionLaunch
import com.arjunrana.tokishrine.data.db.BlockWithContents
import com.arjunrana.tokishrine.data.entity.FrictionType
import com.arjunrana.tokishrine.data.repo.EventRepository
import com.arjunrana.tokishrine.detection.BlockShownEvent
import com.arjunrana.tokishrine.ui.interruption.BlockGateScreen
import com.arjunrana.tokishrine.ui.interruption.ChallengePurpose
import com.arjunrana.tokishrine.ui.interruption.DelayCountdownScreen
import com.arjunrana.tokishrine.ui.interruption.TypingChallengeScreen
import com.arjunrana.tokishrine.ui.interruption.WalkAwayMomentScreen
import com.arjunrana.tokishrine.ui.theme.NocturneTheme
import com.arjunrana.tokishrine.ui.util.BlockHaptics
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Thin Android/Compose adapter around the pure Phase 5 challenge runtime. */
class BlockActivity : ComponentActivity() {

    private val activityScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val selector = ChallengeContentSelector()
    private lateinit var app: TokiApplication

    private var loaded by mutableStateOf<LoadedSession?>(null)
    private var dailyWalkAwayCount by mutableStateOf<Int?>(null)
    private var renderVersion by mutableIntStateOf(0)
    private var inputReady by mutableStateOf(false)
    private var pendingWalkAwaySource: String? = null
    private var destroyed = false
    private var runtime: ChallengeRuntime? = null
    private var resumed = false
    private var shownEventScheduled = false
    private var ticker: Job? = null
    private var autoDismiss: Job? = null

    private val screenOffReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_SCREEN_OFF) abandon(AbandonReason.SCREEN_OFF)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        app = application as TokiApplication
        shownEventScheduled = savedInstanceState?.getBoolean(STATE_SHOWN_EVENT_SCHEDULED) == true
        dailyWalkAwayCount = savedInstanceState?.getInt(STATE_DAILY_COUNT, -1)?.takeIf { it >= 0 }
        pendingWalkAwaySource = savedInstanceState?.getString(STATE_WALK_AWAY_SOURCE)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        registerReceiver(screenOffReceiver, IntentFilter(Intent.ACTION_SCREEN_OFF), RECEIVER_NOT_EXPORTED)

        setContent {
            NocturneTheme {
                renderVersion // snapshot read: pure runtime updates bump this value
                BackHandler {
                    val phase = runtime?.view(now())?.phase
                    when (phase) {
                        ChallengePhase.ACTIVE -> abandon(AbandonReason.APP_SWITCH)
                        ChallengePhase.COMMITTING -> Unit
                        else -> finish()
                    }
                }
                val session = loaded
                val engine = runtime
                val count = dailyWalkAwayCount
                when {
                    count != null -> WalkAwayMomentScreen(count, onDismiss = ::finish)
                    session == null || engine == null -> Box(
                        Modifier.fillMaxSize().background(NocturneTheme.colors.bg),
                    )
                    engine.view(now()).phase == ChallengePhase.GATE -> BlockGateScreen(
                        blockName = session.block.block.name,
                        targetName = session.target.orEmpty(),
                        humourLine = session.humour,
                        backgroundRes = session.backgroundRes,
                        onWalkAway = { handle(engine.gateWalkAway()) },
                        onEnterChallenge = {
                            handle(engine.enterChallenge(now()))
                            engine.onVisible(now())
                            refreshRuntimeUi()
                        },
                    )
                    session.block.block.frictionType == FrictionType.TYPING -> TypingChallengeScreen(
                        purpose = session.purpose,
                        blockName = session.block.block.name,
                        passage = engine.config.passage,
                        typedText = engine.view(now()).typedText,
                        onTypedTextChanged = {
                            if (inputReady) {
                                engine.updateTypedText(it)
                                refreshRuntimeUi()
                            }
                        },
                        onSubmit = { if (inputReady) handle(engine.submitTyping(now())) },
                        onWalkAway = { if (inputReady) handle(engine.escape(now())) },
                        turnOffChars = if (session.purpose == ChallengePurpose.TURN_OFF) {
                            session.block.block.turnoffChars
                        } else {
                            0
                        },
                    )
                    else -> DelayCountdownScreen(
                        purpose = session.purpose,
                        blockName = session.block.block.name,
                        totalSeconds = engine.config.totalSeconds,
                        remainingSeconds = engine.view(now()).remainingSeconds,
                        onEscape = { if (inputReady) handle(engine.escape(now())) },
                    )
                }
            }
        }
        resolveSession(savedInstanceState)
    }

    private fun resolveSession(saved: Bundle?) {
        activityScope.launch {
            val blockId = intent.getLongExtra(EXTRA_BLOCK_ID, -1L)
            val purpose = intent.getStringExtra(EXTRA_PURPOSE)
                ?.let { runCatching { ChallengePurpose.valueOf(it) }.getOrNull() }
                ?: ChallengePurpose.PAUSE
            val block = withContext(Dispatchers.IO) { app.blockRepository.getBlockWithContents(blockId) }
            if (block == null || !block.block.enabled) return@launch finish()

            val triggerType = intent.getStringExtra(EXTRA_TRIGGER_TYPE)
            val target = intent.getStringExtra(EXTRA_TARGET)
            if (purpose == ChallengePurpose.PAUSE && !isValidDetectionLaunch(block, triggerType, target)) {
                return@launch finish()
            }

            val configuredAmount = when {
                block.block.frictionType == FrictionType.TYPING && purpose == ChallengePurpose.PAUSE -> block.block.pauseChars
                block.block.frictionType == FrictionType.TYPING -> block.block.turnoffChars
                purpose == ChallengePurpose.PAUSE -> block.block.countdownSeconds
                else -> block.block.turnoffSeconds
            }
            val sessionId = saved?.getString(STATE_SESSION_ID) ?: UUID.randomUUID().toString()
            val passage = if (block.block.frictionType == FrictionType.TYPING) {
                saved?.getString(STATE_PASSAGE) ?: selector.passage(configuredAmount)
            } else {
                ""
            }
            if (block.block.frictionType == FrictionType.TYPING && passage.length != configuredAmount) {
                return@launch finish()
            }
            runtime = ChallengeRuntime(
                ChallengeConfig(
                    sessionId = sessionId,
                    blockId = block.block.id,
                    purpose = purpose,
                    method = block.block.frictionType,
                    passage = passage,
                    totalSeconds = if (block.block.frictionType == FrictionType.DELAY) configuredAmount else 0,
                    pauseMinutes = block.block.pauseMinutes,
                    target = target,
                    targetType = triggerType,
                ),
                restored = saved?.let(::readSnapshot),
            )
            loaded = LoadedSession(
                block = block,
                purpose = purpose,
                target = target,
                triggerType = triggerType,
                humour = saved?.getString(STATE_HUMOUR) ?: selector.humour(),
                backgroundRes = saved?.getInt(STATE_BACKGROUND)?.takeIf { it != 0 }
                    ?: selector.backgroundRes(),
            )
            val pendingSource = pendingWalkAwaySource
            if (pendingSource != null && dailyWalkAwayCount == null) {
                handle(
                    ChallengeEffect.WalkAway(
                        sessionId = runtime!!.config.sessionId,
                        blockId = block.block.id,
                        target = target.orEmpty(),
                        targetType = triggerType.orEmpty(),
                        source = pendingSource,
                    ),
                )
                return@launch
            }
            if (dailyWalkAwayCount != null) scheduleWalkAwayDismiss()
            maybeRecordShown()
            val pendingCompletion = runtime?.resumePendingCompletion(now())
            if (pendingCompletion != null) {
                handle(pendingCompletion)
                return@launch
            }
            if (purpose == ChallengePurpose.TURN_OFF) {
                val started = runtime?.beginTurnOff(now())
                if (started != null) handle(started) else persistStarted(runtime!!)
            } else if (runtime?.snapshot()?.started == true) {
                persistStarted(runtime!!)
            }
            if (resumed) {
                runtime?.onVisible(now())
                startTickerIfNeeded()
            }
            refreshRuntimeUi()
        }
    }

    override fun onPostResume() {
        super.onPostResume()
        resumed = true
        runtime?.onVisible(now())
        maybeRecordShown()
        startTickerIfNeeded()
        refreshRuntimeUi()
    }

    override fun onStop() {
        ticker?.cancel()
        ticker = null
        val engine = runtime
        if (engine != null && engine.view(now()).phase == ChallengePhase.ACTIVE) {
            if (isChangingConfigurations) {
                engine.onConfigurationHidden(now())
            } else {
                val interactive = (getSystemService(POWER_SERVICE) as PowerManager).isInteractive
                abandon(if (interactive) AbandonReason.APP_SWITCH else AbandonReason.SCREEN_OFF)
            }
        }
        resumed = false
        super.onStop()
    }

    override fun onDestroy() {
        destroyed = true
        unregisterReceiver(screenOffReceiver)
        autoDismiss?.cancel()
        activityScope.cancel()
        super.onDestroy()
    }

    private fun startTickerIfNeeded() {
        val engine = runtime ?: return
        if (!resumed || engine.config.method != FrictionType.DELAY ||
            engine.view(now()).phase != ChallengePhase.ACTIVE || !inputReady
        ) return
        if (ticker?.isActive == true) return
        val generation = engine.currentGeneration()
        ticker = activityScope.launch {
            while (true) {
                delay(TICK_MS)
                val effect = engine.tick(now(), generation)
                refreshRuntimeUi()
                if (effect != null) {
                    handle(effect)
                    return@launch
                }
            }
        }
    }

    private fun handle(effect: ChallengeEffect?) {
        when (effect) {
            null -> Unit
            is ChallengeEffect.Started -> persistStarted(runtime ?: return)
            is ChallengeEffect.TypingMismatch -> app.applicationScope.launch {
                app.challengeRepository.recordTypingMismatch(effect.blockId, effect.charsTyped)
            }
            is ChallengeEffect.WalkAway -> {
                pendingWalkAwaySource = effect.source
                clearLiveChallenge()
                app.applicationScope.launch {
                    val count = app.challengeRepository.recordWalkAwayAndCount(
                        effect.sessionId,
                        effect.blockId,
                        effect.target,
                        effect.targetType,
                        effect.source,
                    )
                    withContext(Dispatchers.Main.immediate) {
                        if (destroyed) return@withContext
                        dailyWalkAwayCount = count
                        scheduleWalkAwayDismiss()
                    }
                }
            }
            is ChallengeEffect.Abandoned -> {
                clearLiveChallenge()
                app.applicationScope.launch {
                    app.challengeRepository.recordAbandoned(
                        effect.blockId,
                        effect.method,
                        effect.progressPct,
                        effect.reason,
                    )
                }
                finish()
            }
            is ChallengeEffect.TurnOffAbandoned -> {
                clearLiveChallenge()
                app.applicationScope.launch {
                    app.challengeRepository.recordTurnOffAbandoned(effect.blockId, effect.progressPct)
                }
                finish()
            }
            is ChallengeEffect.PersistCompletion -> persistCompletion(effect)
        }
        refreshRuntimeUi()
    }

    private fun persistStarted(engine: ChallengeRuntime) {
        inputReady = false
        val config = engine.config
        activityScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    app.challengeRepository.recordStarted(
                        sessionId = config.sessionId,
                        blockId = config.blockId,
                        purpose = config.purpose,
                        method = config.method,
                        configuredAmount = if (config.method == FrictionType.TYPING) {
                            config.passage.length
                        } else {
                            config.totalSeconds
                        },
                    )
                }
                inputReady = true
                if (resumed) {
                    engine.onVisible(now())
                    startTickerIfNeeded()
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Throwable) {
                inputReady = false
            }
        }
    }

    private fun persistCompletion(effect: ChallengeEffect.PersistCompletion) {
        inputReady = false
        ticker?.cancel()
        app.applicationScope.launch {
            try {
                val committed = if (effect.request.purpose == ChallengePurpose.PAUSE) {
                    app.challengeRepository.completePause(effect.request)
                } else {
                    app.challengeRepository.completeTurnOff(effect.request)
                }
                withContext(Dispatchers.Main.immediate) {
                    if (destroyed) return@withContext
                    if (!committed) {
                        runtime?.commitFailed(effect.request.token, now())
                        finish()
                        return@withContext
                    }
                    when (val result = runtime?.commitSucceeded(effect.request.token) ?: return@withContext) {
                        is ChallengeTerminalResult.PauseRequested -> setResult(
                            RESULT_OK,
                            Intent().putExtra(EXTRA_RESULT, RESULT_PAUSE_REQUESTED)
                                .putExtra(EXTRA_BLOCK_ID, result.blockId)
                                .putExtra(EXTRA_PAUSE_MINUTES, result.pauseMinutes)
                                .putExtra(EXTRA_SESSION_ID, result.sessionId),
                        )
                        is ChallengeTerminalResult.BlockDisabled -> {
                            BlockHaptics.turnedOff(this@BlockActivity)
                            setResult(
                                RESULT_OK,
                                Intent().putExtra(EXTRA_RESULT, RESULT_BLOCK_DISABLED)
                                    .putExtra(EXTRA_BLOCK_ID, result.blockId),
                            )
                        }
                    }
                    clearLiveChallenge()
                    finish()
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Throwable) {
                withContext(Dispatchers.Main.immediate) {
                    if (!destroyed && runtime?.commitFailed(effect.request.token, now()) == true) {
                        inputReady = true
                        runtime?.onVisible(now())
                        startTickerIfNeeded()
                        refreshRuntimeUi()
                    }
                }
            }
        }
    }

    private fun abandon(reason: AbandonReason) {
        handle(runtime?.abandon(reason, now()))
    }

    private fun maybeRecordShown() {
        val session = loaded ?: return
        if (!resumed || session.purpose != ChallengePurpose.PAUSE || shownEventScheduled) return
        val shownEvent = BlockShownEvent.create(
            blockId = session.block.block.id,
            triggerType = session.triggerType,
            target = session.target,
            startedAtElapsedMs = intent.getLongExtra(EXTRA_STARTED_AT_ELAPSED_MS, -1L),
            shownAtElapsedMs = now(),
        ) ?: return
        shownEventScheduled = true
        app.applicationScope.launch {
            runCatching {
                app.eventRepository.log(
                    EventRepository.EVENT_BLOCK_SCREEN_SHOWN,
                    blockId = shownEvent.blockId,
                    target = shownEvent.target,
                    targetType = shownEvent.triggerType,
                    params = mapOf(
                        "trigger_type" to shownEvent.triggerType,
                        "latency_ms" to shownEvent.latencyMs,
                    ),
                )
            }
        }
    }

    private fun clearLiveChallenge() {
        ticker?.cancel()
        ticker = null
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        inputReady = false
    }

    private fun scheduleWalkAwayDismiss() {
        autoDismiss?.cancel()
        autoDismiss = activityScope.launch {
            delay(WALK_AWAY_MS)
            finish()
        }
    }

    private fun refreshRuntimeUi() {
        val phase = runtime?.view(now())?.phase
        if (phase == ChallengePhase.ACTIVE || phase == ChallengePhase.COMMITTING) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        renderVersion++
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(STATE_SHOWN_EVENT_SCHEDULED, shownEventScheduled)
        outState.putInt(STATE_DAILY_COUNT, dailyWalkAwayCount ?: -1)
        outState.putString(STATE_WALK_AWAY_SOURCE, pendingWalkAwaySource)
        val session = loaded
        val engine = runtime
        if (session != null && engine != null) {
            outState.putString(STATE_SESSION_ID, engine.config.sessionId)
            outState.putString(STATE_PASSAGE, engine.config.passage)
            outState.putString(STATE_HUMOUR, session.humour)
            outState.putInt(STATE_BACKGROUND, session.backgroundRes)
            val snapshot = engine.snapshot()
            val saveNow = now()
            writeSnapshot(
                outState,
                if (snapshot.visibleSinceMs != null) {
                    snapshot.copy(
                        accruedVisibleMs = snapshot.accruedVisibleMs +
                            (saveNow - snapshot.visibleSinceMs).coerceAtLeast(0L),
                        visibleSinceMs = null,
                    )
                } else {
                    snapshot
                },
            )
        }
        super.onSaveInstanceState(outState)
    }

    private fun writeSnapshot(out: Bundle, snapshot: ChallengeSnapshot) {
        out.putString(STATE_PHASE, snapshot.phase.name)
        out.putInt(STATE_GENERATION, snapshot.generation)
        out.putBoolean(STATE_STARTED, snapshot.started)
        out.putLong(STATE_STARTED_AT, snapshot.startedAtMs ?: -1L)
        out.putString(STATE_TYPED, snapshot.typedText)
        out.putInt(STATE_ATTEMPTS, snapshot.attempts)
        out.putLong(STATE_ACCRUED, snapshot.accruedVisibleMs)
        out.putLong(STATE_VISIBLE_SINCE, snapshot.visibleSinceMs ?: -1L)
    }

    private fun readSnapshot(saved: Bundle): ChallengeSnapshot? {
        val phase = saved.getString(STATE_PHASE)?.let {
            runCatching { ChallengePhase.valueOf(it) }.getOrNull()
        } ?: return null
        return ChallengeSnapshot(
            phase = phase,
            generation = saved.getInt(STATE_GENERATION),
            started = saved.getBoolean(STATE_STARTED),
            startedAtMs = saved.getLong(STATE_STARTED_AT, -1L).takeIf { it >= 0 },
            typedText = saved.getString(STATE_TYPED).orEmpty(),
            attempts = saved.getInt(STATE_ATTEMPTS),
            accruedVisibleMs = saved.getLong(STATE_ACCRUED),
            visibleSinceMs = null,
        )
    }

    private fun now(): Long = SystemClock.elapsedRealtime()

    private data class LoadedSession(
        val block: BlockWithContents,
        val purpose: ChallengePurpose,
        val target: String?,
        val triggerType: String?,
        val humour: String,
        val backgroundRes: Int,
    )

    companion object {
        const val EXTRA_TRIGGER_TYPE = "trigger_type"
        const val EXTRA_TARGET = "target"
        const val EXTRA_BLOCK_NAME = "block_name"
        const val EXTRA_BLOCK_ID = "block_id"
        const val EXTRA_STARTED_AT_ELAPSED_MS = "started_at_elapsed_ms"
        const val EXTRA_PURPOSE = "challenge_purpose"
        const val EXTRA_RESULT = "challenge_result"
        const val EXTRA_PAUSE_MINUTES = "pause_minutes"
        const val EXTRA_SESSION_ID = "session_id"
        const val RESULT_PAUSE_REQUESTED = "pause_requested"
        const val RESULT_BLOCK_DISABLED = "block_disabled"

        private const val STATE_SHOWN_EVENT_SCHEDULED = "shown_event_scheduled"
        private const val STATE_SESSION_ID = "challenge_session_id"
        private const val STATE_PASSAGE = "challenge_passage"
        private const val STATE_HUMOUR = "challenge_humour"
        private const val STATE_BACKGROUND = "challenge_background"
        private const val STATE_PHASE = "challenge_phase"
        private const val STATE_GENERATION = "challenge_generation"
        private const val STATE_STARTED = "challenge_started"
        private const val STATE_STARTED_AT = "challenge_started_at"
        private const val STATE_TYPED = "challenge_typed"
        private const val STATE_ATTEMPTS = "challenge_attempts"
        private const val STATE_ACCRUED = "challenge_accrued"
        private const val STATE_VISIBLE_SINCE = "challenge_visible_since"
        private const val STATE_DAILY_COUNT = "daily_walk_away_count"
        private const val STATE_WALK_AWAY_SOURCE = "walk_away_source"
        private const val TICK_MS = 200L
        private const val WALK_AWAY_MS = 2_000L

        fun turnOffIntent(context: Context, blockId: Long): Intent =
            Intent(context, BlockActivity::class.java)
                .putExtra(EXTRA_BLOCK_ID, blockId)
                .putExtra(EXTRA_PURPOSE, ChallengePurpose.TURN_OFF.name)
    }
}

package com.arjunrana.tokishrine.ui.screens

import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.BackHandler
import androidx.activity.ComponentActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.arjunrana.tokishrine.data.apps.AppEntry
import com.arjunrana.tokishrine.data.apps.InstalledAppsRepository
import com.arjunrana.tokishrine.data.entity.FrictionType
import com.arjunrana.tokishrine.data.repo.BlockDraft
import com.arjunrana.tokishrine.data.repo.BlockRepository
import com.arjunrana.tokishrine.data.repo.ConflictingOwnershipException
import com.arjunrana.tokishrine.data.repo.DISABLE_CHARS_CHOICES
import com.arjunrana.tokishrine.data.repo.DISABLE_CHARS_DEFAULT
import com.arjunrana.tokishrine.data.repo.DISABLE_WAIT_SECONDS_CHOICES
import com.arjunrana.tokishrine.data.repo.DISABLE_WAIT_SECONDS_DEFAULT
import com.arjunrana.tokishrine.data.repo.EventRepository
import com.arjunrana.tokishrine.data.repo.PAUSE_CHARS_DEFAULT
import com.arjunrana.tokishrine.data.repo.PAUSE_CHARS_MAX
import com.arjunrana.tokishrine.data.repo.PAUSE_CHARS_MIN
import com.arjunrana.tokishrine.data.repo.PAUSE_CHARS_STEP
import com.arjunrana.tokishrine.data.repo.PAUSE_MINUTES_DEFAULT
import com.arjunrana.tokishrine.data.repo.PAUSE_MINUTES_MAX
import com.arjunrana.tokishrine.data.repo.PAUSE_MINUTES_MIN
import com.arjunrana.tokishrine.data.repo.PAUSE_MINUTES_STEP
import com.arjunrana.tokishrine.data.repo.PAUSE_WAIT_SECONDS_DEFAULT
import com.arjunrana.tokishrine.data.repo.PAUSE_WAIT_SECONDS_MAX
import com.arjunrana.tokishrine.data.repo.PAUSE_WAIT_SECONDS_MIN
import com.arjunrana.tokishrine.data.repo.PAUSE_WAIT_SECONDS_STEP
import com.arjunrana.tokishrine.ui.components.ButtonVariant
import com.arjunrana.tokishrine.ui.components.BoundedTargetList
import com.arjunrana.tokishrine.ui.components.NocturneAppbar
import com.arjunrana.tokishrine.ui.components.NocturneButton
import com.arjunrana.tokishrine.ui.components.NocturneSegmented
import com.arjunrana.tokishrine.ui.components.NocturneStepper
import com.arjunrana.tokishrine.ui.components.NocturneTextField
import com.arjunrana.tokishrine.ui.components.ProgressDots
import com.arjunrana.tokishrine.ui.components.SectionLabel
import com.arjunrana.tokishrine.ui.components.TargetRow
import com.arjunrana.tokishrine.ui.icons.Ph
import com.arjunrana.tokishrine.ui.icons.PhosphorIcon
import com.arjunrana.tokishrine.ui.theme.NocturneTheme
import com.arjunrana.tokishrine.ui.util.formatCountdown
import com.arjunrana.tokishrine.ui.util.isValidFullDomain
import com.arjunrana.tokishrine.ui.util.toImageBitmap
import com.arjunrana.tokishrine.ui.util.typingEstimateSeconds
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// Draft defaults (PRD §7; §17's 19 September wizard amendment). Ranges,
// steps and the fixed disable ladders live beside the persistence boundary
// in BlockRepository so the UI and storage cannot drift.
private const val NAME_MAX_CHARS = 30

// Five screens with progress segments (contents, name, method, disable,
// review). The details sheet is an optional refinement inside step 3, never
// a sixth step (PRD §17, 19 September).
private const val STEP_CONTENTS = 1
private const val STEP_NAME = 2
private const val STEP_METHOD = 3
private const val STEP_DISABLE = 4
private const val STEP_REVIEW = 5

// Draft state for the five-step create/edit flow. The flow may be entered
// directly at the method step (detail → THE FRICTION → Edit); entryStep is
// the floor Back unwinds to before exiting the flow.
//
// Step 3/4 settings are kept per method (the redesign retains each method's
// draft adjustments and disable choice when switching), so the stored per-
// method columns are the natural draft homes: typingPauseChars/typingTurn-
// offChars hold the typing method's passage and disable passage, waitPause-
// Seconds/waitTurnoffSeconds hold the waiting method's durations. The saved
// row keeps all of them; frictionType decides which are active.
class CreateFlowState(
    val editBlockId: Long?,
    entryStep: Int = 1,
    val flowId: String = UUID.randomUUID().toString(),
) {
    var step by mutableStateOf(entryStep)
    val entryStep = entryStep.coerceIn(STEP_CONTENTS, STEP_REVIEW)
    var showSearch by mutableStateOf(false)
    var showDetails by mutableStateOf(false)
    var appsTab by mutableStateOf(true)

    val apps = mutableStateListOf<AppEntry>()
    val sites = mutableStateListOf<String>()

    var name by mutableStateOf("")

    var frictionType by mutableStateOf(FrictionType.TYPING)
    var pauseMinutes by mutableStateOf(PAUSE_MINUTES_DEFAULT)
    var typingPauseChars by mutableStateOf(PAUSE_CHARS_DEFAULT)
    var waitPauseSeconds by mutableStateOf(PAUSE_WAIT_SECONDS_DEFAULT)
    var typingTurnoffChars by mutableStateOf(DISABLE_CHARS_DEFAULT)
    var waitTurnoffSeconds by mutableStateOf(DISABLE_WAIT_SECONDS_DEFAULT)

    // Edit mode: true once the stored block has been loaded into the draft.
    // Saveable, so a recreated activity keeps the restored draft without the
    // prefill overwriting the user's adjustments from the stored values again.
    var prefilled by mutableStateOf(false)

    // Set when a save was rejected for conflicting ownership: the draft is
    // kept and the review step explains inline (owner decision — no owner
    // name, no dialog, no transfer).
    var saveRejected by mutableStateOf(false)

    fun draft() = BlockDraft(
        name = name.trim(),
        appPackageNames = apps.map { it.packageName },
        siteDomains = sites.toList(),
        frictionType = frictionType,
        pauseMinutes = pauseMinutes,
        pauseChars = typingPauseChars,
        turnoffChars = typingTurnoffChars,
        countdownSeconds = waitPauseSeconds,
        turnoffSeconds = waitTurnoffSeconds,
    )

    // — step 3 adjustments; deltas arrive in the approved step sizes —
    fun adjustPauseMinutes(delta: Int) {
        pauseMinutes = (pauseMinutes + delta).coerceIn(PAUSE_MINUTES_MIN, PAUSE_MINUTES_MAX)
    }

    fun adjustTypingPause(delta: Int) {
        typingPauseChars = (typingPauseChars + delta).coerceIn(PAUSE_CHARS_MIN, PAUSE_CHARS_MAX)
    }

    fun adjustWaitPause(delta: Int) {
        waitPauseSeconds = (waitPauseSeconds + delta).coerceIn(PAUSE_WAIT_SECONDS_MIN, PAUSE_WAIT_SECONDS_MAX)
    }

    // The sheet's Reset restores only the displayed method's own field and
    // the shared pause duration; per-method drafts and the step-4 disable
    // choices are untouched (PRD §17, 19 September).
    fun resetDetails() {
        when (frictionType) {
            FrictionType.TYPING -> typingPauseChars = PAUSE_CHARS_DEFAULT
            FrictionType.DELAY -> waitPauseSeconds = PAUSE_WAIT_SECONDS_DEFAULT
        }
        pauseMinutes = PAUSE_MINUTES_DEFAULT
    }

    // — step 4: fixed ladder choices for the inherited method —
    val disableChoices: List<Int>
        get() = when (frictionType) {
            FrictionType.TYPING -> DISABLE_CHARS_CHOICES
            FrictionType.DELAY -> DISABLE_WAIT_SECONDS_CHOICES
        }

    var disableChoiceIndex: Int
        get() = disableChoices.indexOf(
            if (frictionType == FrictionType.TYPING) typingTurnoffChars else waitTurnoffSeconds,
        )
        set(value) {
            when (frictionType) {
                FrictionType.TYPING -> typingTurnoffChars = DISABLE_CHARS_CHOICES[value]
                FrictionType.DELAY -> waitTurnoffSeconds = DISABLE_WAIT_SECONDS_CHOICES[value]
            }
        }
}

// Restores the draft across activity recreation (BW-06): every adjustment
// lives in instance-saved primitives, so an in-progress create/edit resumes
// on the same step with the draft intact. App icons are not saved — selected
// rows fall back to glyphs, exactly as in edit prefill. A damaged or stale
// shape falls back to a fresh state instead of crashing.
val CreateFlowStateSaver: Saver<CreateFlowState, ArrayList<Any>> = Saver(
    save = { s ->
        arrayListOf(
            s.editBlockId ?: -1L,
            s.step,
            s.entryStep,
            s.showSearch,
            s.showDetails,
            s.appsTab,
            s.name,
            s.frictionType.name,
            s.pauseMinutes,
            s.typingPauseChars,
            s.waitPauseSeconds,
            s.typingTurnoffChars,
            s.waitTurnoffSeconds,
            s.prefilled,
            s.saveRejected,
            ArrayList(s.apps.map { "${it.packageName}\u0000${it.label}" }),
            ArrayList(s.sites),
            s.flowId,
        )
    },
    restore = { saved ->
        runCatching {
            val state = CreateFlowState(
                editBlockId = (saved[0] as Long).takeIf { it != -1L },
                entryStep = saved[2] as Int,
                flowId = saved[17] as String,
            )
            state.step = (saved[1] as Int).coerceIn(STEP_CONTENTS, STEP_REVIEW)
            state.showSearch = saved[3] as Boolean
            state.showDetails = saved[4] as Boolean
            state.appsTab = saved[5] as Boolean
            state.name = saved[6] as String
            state.frictionType = FrictionType.valueOf(saved[7] as String)
            state.pauseMinutes = saved[8] as Int
            state.typingPauseChars = saved[9] as Int
            state.waitPauseSeconds = saved[10] as Int
            state.typingTurnoffChars = saved[11] as Int
            state.waitTurnoffSeconds = saved[12] as Int
            state.prefilled = saved[13] as Boolean
            state.saveRejected = saved[14] as Boolean
            (saved[15] as ArrayList<*>).forEach {
                val (pkg, label) = (it as String).split("\u0000", limit = 2)
                state.apps.add(AppEntry(pkg, label, null))
            }
            (saved[16] as ArrayList<*>).forEach { state.sites.add(it as String) }
            state
        }.getOrNull()
    },
)

internal enum class CreateFlowOutcome { CLOSE, SAVE_REJECTED }

/**
 * One ordered operation stream for a logical editor flow. The owner scope is
 * retained outside the composition, so activity recreation neither cancels a
 * committed save between its events nor starts a second terminal operation.
 */
internal class CreateFlowSession(
    scope: CoroutineScope,
    private val isCreate: Boolean,
    private val onStart: suspend () -> Unit,
    private val onStep: suspend (Int) -> Unit,
    private val onSave: suspend (BlockDraft) -> Unit,
    private val onAbandon: suspend (Int) -> Unit,
) {
    private sealed interface Operation {
        data object Start : Operation
        data class Step(val number: Int) : Operation
        data class Save(val draft: BlockDraft) : Operation
        data class Abandon(val step: Int) : Operation
    }

    private val operations = Channel<Operation>(Channel.UNLIMITED)
    private val terminalTaken = AtomicBoolean(false)
    private val _outcome = MutableStateFlow<CreateFlowOutcome?>(null)
    val outcome: StateFlow<CreateFlowOutcome?> = _outcome
    val terminalInFlight: Boolean get() = terminalTaken.get()

    private val worker = scope.launch {
        for (operation in operations) {
            when (operation) {
                Operation.Start -> onStart()
                is Operation.Step -> onStep(operation.number)
                is Operation.Abandon -> {
                    onAbandon(operation.step)
                    _outcome.value = CreateFlowOutcome.CLOSE
                }
                is Operation.Save -> {
                    try {
                        onSave(operation.draft)
                        _outcome.value = CreateFlowOutcome.CLOSE
                    } catch (_: ConflictingOwnershipException) {
                        terminalTaken.set(false)
                        _outcome.value = CreateFlowOutcome.SAVE_REJECTED
                    }
                }
            }
        }
    }

    init {
        if (isCreate) check(operations.trySend(Operation.Start).isSuccess)
    }

    fun recordStep(step: Int): Boolean {
        if (terminalTaken.get()) return false
        if (!isCreate) return true
        return operations.trySend(Operation.Step(step)).isSuccess
    }

    fun requestSave(draft: BlockDraft): Boolean {
        if (!terminalTaken.compareAndSet(false, true)) return false
        if (operations.trySend(Operation.Save(draft)).isSuccess) return true
        terminalTaken.set(false)
        return false
    }

    fun requestAbandon(step: Int): Boolean {
        if (!terminalTaken.compareAndSet(false, true)) return false
        if (operations.trySend(Operation.Abandon(step)).isSuccess) return true
        terminalTaken.set(false)
        return false
    }

    fun consumeOutcome(expected: CreateFlowOutcome): Boolean =
        _outcome.compareAndSet(expected, null)

    fun close() {
        operations.close()
        worker.cancel()
    }
}

/** Activity-retained registry. Completed flows are removed after delivery. */
internal class CreateFlowOperationStore : ViewModel() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val sessions = mutableMapOf<String, CreateFlowSession>()

    fun session(
        flowId: String,
        isCreate: Boolean,
        onStart: suspend () -> Unit,
        onStep: suspend (Int) -> Unit,
        onSave: suspend (BlockDraft) -> Unit,
        onAbandon: suspend (Int) -> Unit,
    ): CreateFlowSession = synchronized(sessions) {
        sessions.getOrPut(flowId) {
            CreateFlowSession(scope, isCreate, onStart, onStep, onSave, onAbandon)
        }
    }

    fun release(flowId: String, expected: CreateFlowSession) {
        synchronized(sessions) {
            if (sessions[flowId] === expected) sessions.remove(flowId)?.close()
        }
    }

    override fun onCleared() {
        synchronized(sessions) {
            sessions.values.forEach { it.close() }
            sessions.clear()
        }
        scope.cancel()
    }
}

private tailrec fun Context.findComponentActivity(): ComponentActivity? = when (this) {
    is ComponentActivity -> this
    is ContextWrapper -> baseContext.findComponentActivity()
    else -> null
}

@Composable
fun CreateFlowScreen(
    editBlockId: Long?,
    blockRepo: BlockRepository,
    eventRepo: EventRepository,
    appsRepo: InstalledAppsRepository,
    onClose: () -> Unit,
    initialStep: Int = 1,
) {
    val state = rememberSaveable(saver = CreateFlowStateSaver) {
        CreateFlowState(editBlockId, initialStep)
    }
    val context = LocalContext.current
    val activity = remember(context) {
        checkNotNull(context.findComponentActivity()) { "CreateFlowScreen requires a ComponentActivity" }
    }
    val operationStore = remember(activity) {
        ViewModelProvider(activity)[CreateFlowOperationStore::class.java]
    }
    val session = remember(state.flowId, operationStore, editBlockId, blockRepo, eventRepo) {
        operationStore.session(
            flowId = state.flowId,
            isCreate = editBlockId == null,
            onStart = {
                eventRepo.log(EventRepository.EVENT_BLOCK_CREATE_STARTED)
            },
            onStep = { step ->
                eventRepo.log(EventRepository.EVENT_BLOCK_CREATE_STEP_COMPLETED, params = mapOf("step" to step))
            },
            onSave = { draft ->
                if (editBlockId == null) {
                    eventRepo.atomically {
                        val id = blockRepo.createBlock(draft)
                        eventRepo.log(
                            EventRepository.EVENT_BLOCK_CREATE_STEP_COMPLETED,
                            params = mapOf("step" to STEP_REVIEW),
                        )
                        eventRepo.log(
                            EventRepository.EVENT_BLOCK_CREATED,
                            blockId = id,
                            params = mapOf(
                                "app_count" to draft.appPackageNames.size,
                                "site_count" to draft.siteDomains.size,
                                "friction_type" to draft.frictionType.name.lowercase(),
                                "pause_minutes" to draft.pauseMinutes,
                                "pause_chars" to draft.pauseChars,
                                "turnoff_chars" to draft.turnoffChars,
                                "countdown_seconds" to draft.countdownSeconds,
                                "turnoff_seconds" to draft.turnoffSeconds,
                            ),
                        )
                    }
                } else {
                    eventRepo.atomically {
                        val changed = changedFields(blockRepo, editBlockId, draft)
                        blockRepo.updateBlock(editBlockId, draft)
                        eventRepo.log(
                            EventRepository.EVENT_BLOCK_EDITED,
                            blockId = editBlockId,
                            params = mapOf("fields_changed" to changed),
                        )
                    }
                }
            },
            onAbandon = { step ->
                if (editBlockId == null) {
                    eventRepo.log(
                        EventRepository.EVENT_BLOCK_CREATE_ABANDONED,
                        params = mapOf("step" to step),
                    )
                }
            },
        )
    }

    // The retained session completes independently of a particular activity
    // instance. A recreated screen receives the pending outcome and invokes
    // the current navigation callback exactly once.
    LaunchedEffect(session) {
        session.outcome.collect { outcome ->
            if (outcome != null && session.consumeOutcome(outcome)) {
                when (outcome) {
                    CreateFlowOutcome.CLOSE -> {
                        operationStore.release(state.flowId, session)
                        onClose()
                    }
                    CreateFlowOutcome.SAVE_REJECTED -> state.saveRejected = true
                }
            }
        }
    }

    // Edit mode: prefill the draft from the stored block exactly once per
    // flow instance — after recreation the saveable draft is already current
    // and must not be clobbered with the stored values.
    LaunchedEffect(editBlockId) {
        if (editBlockId != null && !state.prefilled) {
            blockRepo.getBlockWithContents(editBlockId)?.let { stored ->
                state.name = stored.block.name
                state.frictionType = stored.block.frictionType
                state.pauseMinutes = stored.block.pauseMinutes
                state.typingPauseChars = stored.block.pauseChars
                state.typingTurnoffChars = stored.block.turnoffChars
                state.waitPauseSeconds = stored.block.countdownSeconds
                state.waitTurnoffSeconds = stored.block.turnoffSeconds
                state.apps.addAll(
                    stored.apps.map { AppEntry(it.packageName, appsRepo.labelFor(it.packageName), null) },
                )
                state.sites.addAll(stored.sites.map { it.domain })
            }
            state.prefilled = true
        }
    }

    fun stepCompleted(step: Int): Boolean = session.recordStep(step)

    fun abandonAndClose() {
        // Capture the exit step and take the guard synchronously, before the
        // async event write, so any Back/appbar input queued while that write
        // is suspended finds the guard taken and does nothing.
        session.requestAbandon(state.step)
    }

    fun save() {
        // Same terminal guard as abandon: while a save is in flight, Back and
        // further Save taps are no-ops, so exactly one of the two terminal
        // outcomes can ever run.
        state.saveRejected = false
        session.requestSave(state.draft())
    }

    // Android Back mirrors the appbar: app-search closes first, then the
    // details sheet (a sheet Back dismisses the sheet before any wizard
    // navigation), then the flow steps backwards down to the entry step, and
    // a Back there exits — routed through the abandonment logger exactly once
    // (create mode; edits close silently). Direct method entry unwinds only
    // to step 3 before exiting. With the keyboard open the system consumes
    // Back to dismiss it, so this handler runs only once the keyboard is
    // down. The open sheet's own back handling wins over this handler while
    // it is visible; this branch is the fallback.
    fun stepBack(): Boolean {
        if (session.terminalInFlight) return true
        if (state.step > state.entryStep) {
            state.step -= 1
            return true
        }
        return false
    }

    BackHandler {
        when {
            state.showSearch -> state.showSearch = false
            state.showDetails -> state.showDetails = false
            !stepBack() -> abandonAndClose()
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(NocturneTheme.colors.bg)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .padding(bottom = 16.dp),
        ) {
            when {
                state.showSearch -> AppSearchPane(
                    state = state,
                    blockRepo = blockRepo,
                    appsRepo = appsRepo,
                    onClose = { state.showSearch = false },
                )
                state.step == STEP_CONTENTS -> StepContents(
                    state,
                    blockRepo,
                    onBack = ::abandonAndClose,
                    onNext = { if (stepCompleted(1)) state.step = STEP_NAME },
                )
                state.step == STEP_NAME -> StepName(
                    state,
                    onBack = { if (!session.terminalInFlight) state.step = STEP_CONTENTS },
                    onNext = { if (stepCompleted(2)) state.step = STEP_METHOD },
                )
                state.step == STEP_METHOD -> StepFriction(
                    state,
                    onBack = { if (!stepBack()) abandonAndClose() },
                    onNext = { if (stepCompleted(3)) state.step = STEP_DISABLE },
                )
                state.step == STEP_DISABLE -> StepDisable(
                    state,
                    onBack = { if (!session.terminalInFlight) state.step = STEP_METHOD },
                    onNext = { if (stepCompleted(4)) state.step = STEP_REVIEW },
                )
                else -> StepReview(
                    state,
                    onBack = {
                        if (!session.terminalInFlight) {
                            state.saveRejected = false
                            state.step = STEP_DISABLE
                        }
                    },
                    onSave = ::save,
                )
            }
        }

    }
}

// Compared against the stored block BEFORE the update writes.
private suspend fun changedFields(
    blockRepo: BlockRepository,
    editBlockId: Long,
    draft: BlockDraft,
): String {
    val current = blockRepo.getBlockWithContents(editBlockId) ?: return "unknown"
    val changed = buildList {
        if (current.block.name != draft.name) add("name")
        if (current.apps.map { it.packageName }.toSet() != draft.appPackageNames.toSet()) add("apps")
        if (current.sites.map { it.domain }.toSet() != draft.siteDomains.map { it.lowercase() }.toSet()) add("sites")
        if (current.block.frictionType != draft.frictionType) add("friction_type")
        if (current.block.pauseMinutes != draft.pauseMinutes) add("pause_minutes")
        if (current.block.pauseChars != draft.pauseChars) add("pause_chars")
        if (current.block.turnoffChars != draft.turnoffChars) add("turnoff_chars")
        if (current.block.countdownSeconds != draft.countdownSeconds) add("countdown_seconds")
        if (current.block.turnoffSeconds != draft.turnoffSeconds) add("turnoff_seconds")
    }
    return changed.joinToString(",").ifEmpty { "none" }
}

// — Step 1: apps & sites —

@Composable
private fun StepContents(
    state: CreateFlowState,
    blockRepo: BlockRepository,
    onBack: () -> Unit,
    onNext: () -> Unit,
) {
    var siteInput by remember { mutableStateOf("") }
    var siteOwnerships by remember { mutableStateOf<Map<String, Long>?>(null) }

    LaunchedEffect(Unit) {
        siteOwnerships = blockRepo.siteOwnerships()
    }

    // A domain owned by another block cannot be added: the entry shows
    // Already added to a block (owner decision — no owner name, no dialog).
    // Nothing can be added until ownership finishes loading, so a target can
    // never enter the draft unchecked. Input must also be a complete domain
    // (owner addendum, 13 September): the field is single-line and pasted
    // line breaks stay in the value, keeping it invalid — "reddit" or a
    // multiline paste like "reddit.com\nabdes" is rejected, never merged.
    val ownerships = siteOwnerships
    val canonicalInput = siteInput.trim().trimEnd('.').lowercase()
    val inputIsValidDomain = isValidFullDomain(canonicalInput)
    val inputHeldElsewhere = ownerships != null &&
        canonicalInput.isNotEmpty() &&
        ownerships[canonicalInput]?.let { it != state.editBlockId } == true

    Column(Modifier.fillMaxSize()) {
        NocturneAppbar(
            title = if (state.editBlockId == null) "New block" else "Edit block",
            stepLabel = "$STEP_CONTENTS / 5",
            onBack = onBack,
        )
        ProgressDots(total = 5, current = STEP_CONTENTS)
        Spacer(Modifier.height(16.dp))
        Text(
            "What should this cover?",
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium,
            lineHeight = 23.sp,
            letterSpacing = (-0.2).sp,
            color = NocturneTheme.colors.text,
        )
        Spacer(Modifier.height(12.dp))
        NocturneSegmented(
            options = listOf(Ph.SquaresFour to "Apps", Ph.Globe to "Websites"),
            selectedIndex = if (state.appsTab) 0 else 1,
            onSelect = { state.appsTab = it == 0 },
        )
        Spacer(Modifier.height(14.dp))

        if (state.appsTab) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(NocturneTheme.colors.surface, RoundedCornerShape(10.dp))
                    .border(1.dp, NocturneTheme.colors.divider, RoundedCornerShape(10.dp))
                    .clickable { state.showSearch = true }
                    .padding(horizontal = 11.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PhosphorIcon(Ph.MagnifyingGlass, tint = NocturneTheme.colors.neutral.step500)
                Spacer(Modifier.width(8.dp))
                Text("Search your installed apps…", fontSize = 13.5.sp, color = NocturneTheme.colors.neutral.step500)
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                NocturneTextField(
                    value = siteInput,
                    // Single-line field: Enter inserts nothing. Pasted line
                    // breaks are NOT stripped (owner clarification,
                    // 13 September): the raw value stays and keeps Add
                    // disabled — "reddit.com\nabdes" must never merge into
                    // an addable domain. Only removing the break does.
                    onValueChange = { siteInput = it },
                    hint = "example.com",
                    leading = { PhosphorIcon(Ph.Globe, tint = NocturneTheme.colors.neutral.step500) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                )
                Spacer(Modifier.width(8.dp))
                NocturneButton(
                    "Add",
                    variant = ButtonVariant.SECONDARY,
                    height = 38.dp,
                    enabled = ownerships != null && inputIsValidDomain && !inputHeldElsewhere,
                    onClick = {
                        if (ownerships == null || !inputIsValidDomain || inputHeldElsewhere) return@NocturneButton
                        siteInput = ""
                        if (state.sites.none { it == canonicalInput }) state.sites.add(canonicalInput)
                    },
                )
            }
            Spacer(Modifier.height(6.dp))
            when {
                inputHeldElsewhere -> Text(
                    "Already added to a block",
                    fontSize = 11.5.sp,
                    lineHeight = 17.sp,
                    color = NocturneTheme.colors.neutral.step300,
                )
                siteInput.isNotBlank() && !inputIsValidDomain -> Text(
                    "Enter a complete domain like reddit.com",
                    fontSize = 11.5.sp,
                    lineHeight = 17.sp,
                    color = NocturneTheme.colors.neutral.step300,
                )
                else -> Text(
                    "Type a full domain. Works in Chrome and other supported browsers.",
                    fontSize = 11.5.sp,
                    lineHeight = 17.sp,
                    color = NocturneTheme.colors.neutral.step500,
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        SectionLabel("SELECTED APPS AND SITES")
        Spacer(Modifier.height(8.dp))

        // Owner addendum, 13 September: the selected list scrolls on its
        // own while the CTA row stays pinned and floating at the bottom
        // (mirroring the friction step), so a long selection can never
        // push Next below the fold.
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            state.apps.forEach { entry ->
                SelectedRow(
                    label = entry.label,
                    glyph = Ph.SquaresFour,
                    onRemove = { state.apps.remove(entry) },
                )
            }
            state.sites.forEach { domain ->
                SelectedRow(
                    label = domain,
                    glyph = Ph.Globe,
                    onRemove = { state.sites.remove(domain) },
                )
            }
        }

        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "${state.apps.size + state.sites.size} selected",
                fontSize = 12.5.sp,
                color = NocturneTheme.colors.neutral.step500,
                modifier = Modifier.weight(1f),
            )
            // PRD §17 R8: all targets may be removed temporarily, but the
            // flow cannot advance past step 1 (or save) while empty.
            NocturneButton(
                "Next",
                height = 44.dp,
                paddingHorizontal = 26.dp,
                enabled = state.apps.isNotEmpty() || state.sites.isNotEmpty(),
                onClick = onNext,
            )
        }
    }
}

@Composable
private fun SelectedRow(label: String, glyph: Int, onRemove: () -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PhosphorIcon(glyph, tint = NocturneTheme.colors.neutral.step500, size = 15)
        Spacer(Modifier.width(11.dp))
        Text(
            label,
            fontSize = 14.sp,
            color = NocturneTheme.colors.text,
            modifier = Modifier.weight(1f),
        )
        PhosphorIcon(
            Ph.X,
            tint = NocturneTheme.colors.neutral.step500,
            size = 14,
            modifier = Modifier.clickable { onRemove() },
        )
    }
}

// — App search (screen 8) —

@Composable
private fun AppSearchPane(
    state: CreateFlowState,
    blockRepo: BlockRepository,
    appsRepo: InstalledAppsRepository,
    onClose: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val allApps = remember { mutableStateListOf<AppEntry>() }
    var ownerships by remember { mutableStateOf<Map<String, Long>?>(null) }
    var appsLoaded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        allApps.addAll(appsRepo.loadApps())
        appsLoaded = true
        ownerships = blockRepo.appOwnerships()
    }

    val filtered = if (query.isBlank()) {
        allApps.toList()
    } else {
        allApps.filter {
            it.label.contains(query, ignoreCase = true) || it.packageName.contains(query, ignoreCase = true)
        }
    }

    Column(Modifier.fillMaxSize()) {
        NocturneAppbar(title = "Add apps", onBack = onClose)
        NocturneTextField(
            value = query,
            onValueChange = { query = it },
            hint = "Search your apps",
            leading = { PhosphorIcon(Ph.MagnifyingGlass, tint = NocturneTheme.colors.accentRamp.step300) },
            trailing = if (query.isNotEmpty()) {
                {
                    PhosphorIcon(
                        Ph.XCircle,
                        tint = NocturneTheme.colors.neutral.step500,
                        modifier = Modifier.clickable { query = "" },
                    )
                }
            } else {
                null
            },
        )
        Spacer(Modifier.height(8.dp))
        if (!appsLoaded) {
            // Owner request, 19 September: the first launcher-apps query
            // takes a moment; say so below the search bar instead of
            // showing an apparently empty list (or a wrong "0 apps match").
            Text(
                "loading...",
                fontSize = 11.5.sp,
                color = NocturneTheme.colors.neutral.step500,
            )
        } else if (query.isNotBlank()) {
            Text(
                "${filtered.size} apps match “$query”",
                fontSize = 11.5.sp,
                color = NocturneTheme.colors.neutral.step500,
            )
            Spacer(Modifier.height(14.dp))
        }

        LazyColumn(Modifier.weight(1f)) {
            items(filtered, key = { it.packageName }) { entry ->
                val selected = state.apps.any { it.packageName == entry.packageName }
                // Owned by another block (ON or OFF): visible but not
                // selectable, labelled without naming the owner block. Rows
                // stay inert until the ownership read completes — selecting
                // before it is known could admit a target the repository
                // would later have to reject.
                val heldElsewhere = ownerships?.get(entry.packageName)?.let { it != state.editBlockId } == true
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable(enabled = ownerships != null && !heldElsewhere) {
                            if (selected) {
                                state.apps.removeAll { it.packageName == entry.packageName }
                            } else {
                                state.apps.add(entry)
                            }
                        }
                        .padding(vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AppIcon(entry)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        entry.label,
                        fontSize = 14.5.sp,
                        color = NocturneTheme.colors.text,
                        modifier = Modifier.weight(1f),
                    )
                    when {
                        heldElsewhere -> HeldPill()
                        selected -> AddedPill()
                        else -> PhosphorIcon(Ph.PlusCircle, tint = NocturneTheme.colors.accentRamp.step300, size = 22)
                    }
                }
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(NocturneTheme.colors.divider),
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        NocturneButton("Done", onClick = onClose, block = true, height = 46.dp)
    }
}

@Composable
private fun AppIcon(entry: AppEntry) {
    val bitmap = remember(entry.packageName) { entry.icon?.let { it.toImageBitmap() } }
    if (bitmap != null) {
        Image(bitmap = bitmap, contentDescription = null, modifier = Modifier.size(24.dp))
    } else {
        PhosphorIcon(Ph.SquaresFour, tint = NocturneTheme.colors.neutral.step400, size = 20)
    }
}

@Composable
private fun AddedPill() {
    Row(
        Modifier
            .background(NocturneTheme.colors.accentRamp.step800, RoundedCornerShape(20.dp))
            .padding(horizontal = 9.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PhosphorIcon(Ph.Check, tint = NocturneTheme.colors.accentRamp.step200, size = 11)
        Spacer(Modifier.width(4.dp))
        Text("Added", fontSize = 11.sp, color = NocturneTheme.colors.accentRamp.step200)
    }
}

@Composable
private fun HeldPill() {
    Text(
        "Already added to a block",
        fontSize = 11.sp,
        color = NocturneTheme.colors.neutral.step100,
        modifier = Modifier
            .background(NocturneTheme.colors.neutral.step800, RoundedCornerShape(20.dp))
            .padding(horizontal = 9.dp, vertical = 4.dp),
    )
}

// — Step 2: name —

@Composable
private fun StepName(state: CreateFlowState, onBack: () -> Unit, onNext: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        NocturneAppbar(
            title = if (state.editBlockId == null) "New block" else "Edit block",
            stepLabel = "$STEP_NAME / 5",
            onBack = onBack,
        )
        ProgressDots(total = 5, current = STEP_NAME)
        Spacer(Modifier.height(22.dp))
        Text(
            "Give it a name",
            fontSize = 23.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = (-0.23).sp,
            lineHeight = 26.sp,
            color = NocturneTheme.colors.text,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "You'll see this on the block screen list. Name it something which allows you to remember what all it blocks!!",
            fontSize = 13.sp,
            lineHeight = 20.sp,
            color = NocturneTheme.colors.neutral.step500,
        )
        Spacer(Modifier.height(18.dp))
        // PRD §17 R7 (cap raised 20 → 30 after device testing, 13 September):
        // single-line input, at most 30 characters. Display names may wrap
        // to two lines; no ellipsis anywhere. Paste (and the SetText path)
        // can carry line breaks past singleLine, so they are stripped here;
        // the cap truncates rather than rejecting the update, so the field
        // always shows exactly the stored value.
        NocturneTextField(
            value = state.name,
            onValueChange = { input ->
                state.name = input
                    .filterNot { it == '\n' || it == '\r' }
                    .take(NAME_MAX_CHARS)
            },
            hint = "Block name",
            fontSize = 17,
            minHeight = 48.dp,
            singleLine = true,
        )
        Spacer(Modifier.weight(1f))
        NocturneButton("Next", block = true, height = 46.dp, enabled = state.name.isNotBlank(), onClick = onNext)
    }
}

// — Step 3: method —

// Two tappable method cards with live plain-words summaries; selecting a
// card changes nothing else on the screen (PRD §17, 19 September). The
// details live in an optional sheet, not a sixth step.
@Composable
private fun StepFriction(state: CreateFlowState, onBack: () -> Unit, onNext: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        NocturneAppbar(
            title = if (state.editBlockId == null) "New block" else "Edit block",
            stepLabel = "$STEP_METHOD / 5",
            onBack = onBack,
        )
        ProgressDots(total = 5, current = STEP_METHOD)
        Spacer(Modifier.height(16.dp))
        Text(
            "What should getting in cost?",
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium,
            lineHeight = 23.sp,
            letterSpacing = (-0.2).sp,
            color = NocturneTheme.colors.text,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            "Pick whichever you'll actually respect.",
            fontSize = 11.sp,
            lineHeight = 17.sp,
            color = NocturneTheme.colors.neutral.step500,
        )
        Spacer(Modifier.height(12.dp))

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            MethodCard(
                selected = state.frictionType == FrictionType.TYPING,
                glyph = Ph.Keyboard,
                title = "Type a passage",
                summary = "${state.typingPauseChars} characters. Random words. " +
                    "Then ${state.pauseMinutes} minutes before the block comes back.",
                onClick = { state.frictionType = FrictionType.TYPING },
            )
            Spacer(Modifier.height(10.dp))
            MethodCard(
                selected = state.frictionType == FrictionType.DELAY,
                glyph = Ph.Hourglass,
                title = "Wait it out",
                summary = "A ${formatCountdown(state.waitPauseSeconds)} wait to pause the block. " +
                    "Then ${state.pauseMinutes} minutes before the block comes back.",
                onClick = { state.frictionType = FrictionType.DELAY },
            )
            Spacer(Modifier.height(14.dp))
            // Outlined secondary button in normal layout space under the
            // cards — not a floating overlay (PRD §17, 19 September).
            NocturneButton(
                "Adjust the details",
                variant = ButtonVariant.SECONDARY,
                block = true,
                height = 44.dp,
                leading = {
                    PhosphorIcon(Ph.SlidersHorizontal, tint = NocturneTheme.colors.text, size = 16)
                },
                onClick = { state.showDetails = true },
            )
        }

        Spacer(Modifier.height(12.dp))
        NocturneButton(
            "Next",
            block = true,
            height = 46.dp,
            enabled = state.apps.isNotEmpty() || state.sites.isNotEmpty(),
            onClick = onNext,
        )
    }

    if (state.showDetails) {
        DetailsSheet(state)
    }
}

@Composable
private fun MethodCard(
    selected: Boolean,
    glyph: Int,
    title: String,
    summary: String,
    onClick: () -> Unit,
) {
    val colors = NocturneTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .background(if (selected) colors.accentRamp.step900 else colors.surface, RoundedCornerShape(12.dp))
            .border(1.dp, if (selected) colors.accent else colors.divider, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.Top,
    ) {
        PhosphorIcon(
            glyph,
            tint = if (selected) colors.accentRamp.step300 else colors.neutral.step400,
            size = 18,
            modifier = Modifier.padding(top = 1.dp),
        )
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Text(
                title,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.Medium,
                color = colors.text,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                summary,
                fontSize = 12.5.sp,
                lineHeight = 18.sp,
                color = colors.neutral.step500,
            )
        }
        // Owner correction, 19 September: both cards carry the tick circle —
        // filled and checked on the selected card, empty ring on the other.
        Spacer(Modifier.width(10.dp))
        if (selected) {
            Box(
                Modifier
                    .size(20.dp)
                    .background(colors.accent, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                PhosphorIcon(Ph.Check, tint = colors.bg, size = 11)
            }
        } else {
            Box(
                Modifier
                    .size(20.dp)
                    .border(1.dp, colors.neutral.step500, CircleShape),
            )
        }
    }
}

// The details sheet (PRD §17, 19 September): only the selected method's own
// field plus the shared pause duration. Adjustments update the draft and the
// card summaries immediately; no save happens here. Done, Back, swipe and
// outside dismissal all just close the sheet — the draft keeps the changes.
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun DetailsSheet(state: CreateFlowState) {
    val colors = NocturneTheme.colors
    ModalBottomSheet(
        onDismissRequest = { state.showDetails = false },
        // DC-06: the reference sheet is the darker Nocturne background, not
        // the lighter card surface. Keeping elevation at zero prevents M3
        // from compositing a lavender surface tint over that exact token.
        containerColor = colors.bg,
        scrimColor = MaterialTheme.colorScheme.scrim,
        tonalElevation = 0.dp,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp),
        ) {
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "The details",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.text,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    "Reset",
                    fontSize = 13.sp,
                    color = colors.accentRamp.step300,
                    modifier = Modifier.clickable { state.resetDetails() },
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                when (state.frictionType) {
                    FrictionType.TYPING -> "Typing a passage to get in."
                    FrictionType.DELAY -> "Waiting it out to get in."
                },
                fontSize = 12.sp,
                lineHeight = 17.sp,
                color = colors.neutral.step500,
            )
            Spacer(Modifier.height(16.dp))

            when (state.frictionType) {
                FrictionType.TYPING -> {
                    Text("Passage length", fontSize = 12.sp, color = colors.neutral.step400)
                    Spacer(Modifier.height(8.dp))
                    NocturneStepper(
                        value = "${state.typingPauseChars} characters",
                        onDecrement = { state.adjustTypingPause(-PAUSE_CHARS_STEP) },
                        onIncrement = { state.adjustTypingPause(PAUSE_CHARS_STEP) },
                        canDecrement = state.typingPauseChars > PAUSE_CHARS_MIN,
                        canIncrement = state.typingPauseChars < PAUSE_CHARS_MAX,
                    )
                }
                FrictionType.DELAY -> {
                    Text("Wait before you're in", fontSize = 12.sp, color = colors.neutral.step400)
                    Spacer(Modifier.height(8.dp))
                    NocturneStepper(
                        value = formatCountdown(state.waitPauseSeconds),
                        onDecrement = { state.adjustWaitPause(-PAUSE_WAIT_SECONDS_STEP) },
                        onIncrement = { state.adjustWaitPause(PAUSE_WAIT_SECONDS_STEP) },
                        canDecrement = state.waitPauseSeconds > PAUSE_WAIT_SECONDS_MIN,
                        canIncrement = state.waitPauseSeconds < PAUSE_WAIT_SECONDS_MAX,
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            Text("Block stays off for", fontSize = 12.sp, color = colors.neutral.step400)
            Spacer(Modifier.height(8.dp))
            NocturneStepper(
                value = "${state.pauseMinutes} min",
                onDecrement = { state.adjustPauseMinutes(-PAUSE_MINUTES_STEP) },
                onIncrement = { state.adjustPauseMinutes(PAUSE_MINUTES_STEP) },
                canDecrement = state.pauseMinutes > PAUSE_MINUTES_MIN,
                canIncrement = state.pauseMinutes < PAUSE_MINUTES_MAX,
            )
            Spacer(Modifier.height(5.dp))
            Text(
                // Written spec prevails over the mock's stale 10-minute note.
                "Then it comes back on its own. Minimum 5 minutes.",
                fontSize = 10.5.sp,
                color = colors.neutral.step600,
            )

            Spacer(Modifier.height(18.dp))
            NocturneButton(
                "Done",
                block = true,
                height = 46.dp,
                onClick = { state.showDetails = false },
            )
        }
    }
}

// — Step 4: disable difficulty —

// The method is inherited from step 3 and never asked again; the three
// ladder choices are fixed and independent of the pause settings, with the
// middle one recommended and preselected so Next alone is valid (PRD §17,
// 19 September). Each method's choice is remembered per method draft.
@Composable
private fun StepDisable(state: CreateFlowState, onBack: () -> Unit, onNext: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        NocturneAppbar(
            title = if (state.editBlockId == null) "New block" else "Edit block",
            stepLabel = "$STEP_DISABLE / 5",
            onBack = onBack,
        )
        ProgressDots(total = 5, current = STEP_DISABLE)
        Spacer(Modifier.height(16.dp))
        Text(
            "Disabling the block",
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium,
            lineHeight = 23.sp,
            letterSpacing = (-0.2).sp,
            color = NocturneTheme.colors.text,
        )
        Spacer(Modifier.height(2.dp))
        // Owner correction, 19 September: states what disabling is for,
        // replacing the PRD §17 amendment's original explainer sentence.
        Text(
            "Disable the block to edit or delete the block. It needs to be a little inconvenient!",
            fontSize = 12.sp,
            lineHeight = 18.sp,
            color = NocturneTheme.colors.neutral.step500,
        )
        Spacer(Modifier.height(12.dp))

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            state.disableChoices.forEachIndexed { index, value ->
                val (title, body) = disableCardCopy(state.frictionType, index, value)
                DisableChoiceCard(
                    selected = state.disableChoiceIndex == index,
                    title = title,
                    body = body,
                    recommended = index == 1,
                    onClick = { state.disableChoiceIndex = index },
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        NocturneButton(
            "Next",
            block = true,
            height = 46.dp,
            enabled = state.apps.isNotEmpty() || state.sites.isNotEmpty(),
            onClick = onNext,
        )
    }
}

// Visible card copy from the disable reference, with the written ladder
// values (3/6/12 minutes, not the mock's old note).
private fun disableCardCopy(method: FrictionType, index: Int, value: Int): Pair<String, String> {
    val title = when (method) {
        FrictionType.TYPING -> when (index) {
            0 -> "Type a bit"
            1 -> "Type a bit more"
            else -> "Make it hurt"
        }
        FrictionType.DELAY -> when (index) {
            0 -> "Wait a bit"
            1 -> "Wait a bit more"
            else -> "Make it hurt"
        }
    }
    val body = when (method) {
        FrictionType.TYPING -> when (index) {
            0 -> "Type $value characters. Random words."
            1 -> "Type $value characters. Random words. Not easy but not impossible either."
            else -> "Type $value characters. For blocks you don't trust yourself with."
        }
        FrictionType.DELAY -> when (index) {
            0 -> "Wait for ${value / 60} minutes."
            1 -> "Wait for ${value / 60} minutes."
            else -> "Wait for ${value / 60} minutes. For blocks you don't trust yourself with."
        }
    }
    return title to body
}

@Composable
private fun DisableChoiceCard(
    selected: Boolean,
    title: String,
    body: String,
    recommended: Boolean,
    onClick: () -> Unit,
) {
    val colors = NocturneTheme.colors
    Column(
        Modifier
            .fillMaxWidth()
            .background(if (selected) colors.accentRamp.step900 else colors.surface, RoundedCornerShape(12.dp))
            .border(1.dp, if (selected) colors.accent else colors.divider, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Text(
            title,
            fontSize = 14.5.sp,
            fontWeight = FontWeight.Medium,
            color = colors.text,
        )
        Spacer(Modifier.height(3.dp))
        Text(
            body,
            fontSize = 12.5.sp,
            lineHeight = 18.sp,
            color = colors.neutral.step500,
        )
        if (recommended) {
            Spacer(Modifier.height(5.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                PhosphorIcon(Ph.Check, tint = colors.accentRamp.step300, size = 11)
                Spacer(Modifier.width(4.dp))
                Text("Recommended", fontSize = 11.5.sp, color = colors.accentRamp.step300)
            }
        }
    }
}

// — Step 5: review & save —

@Composable
private fun StepReview(state: CreateFlowState, onBack: () -> Unit, onSave: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        NocturneAppbar(
            title = if (state.editBlockId == null) "New block" else "Edit block",
            stepLabel = "$STEP_REVIEW / 5",
            onBack = onBack,
        )
        ProgressDots(total = 5, current = STEP_REVIEW)
        // PRD §17 R6: the summary and Save stay reachable whatever the list
        // length — the review content scrolls, the Save stays pinned.
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(Modifier.height(18.dp))
            Text(
                state.name.ifBlank { "Untitled block" },
                fontSize = 23.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = (-0.23).sp,
                lineHeight = 26.sp,
                color = NocturneTheme.colors.text,
            )
            Spacer(Modifier.height(16.dp))
            ReviewTargetList(state)
            Spacer(Modifier.height(18.dp))
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(NocturneTheme.colors.surface, RoundedCornerShape(12.dp)),
            ) {
                val typing = state.frictionType == FrictionType.TYPING
                CostRow(
                    glyph = if (typing) Ph.Keyboard else Ph.Hourglass,
                    tint = NocturneTheme.colors.accentRamp.step300,
                    title = "To pause it",
                    subtitle = if (typing) {
                        "Type ${state.typingPauseChars} characters · ~${typingEstimateSeconds(state.typingPauseChars)} sec"
                    } else {
                        "Wait ${formatCountdown(state.waitPauseSeconds)}"
                    },
                )
                CostRow(
                    glyph = Ph.LockKeyOpen,
                    tint = NocturneTheme.colors.neutral.step400,
                    title = "To turn this block off",
                    subtitle = if (typing) {
                        "Type ${state.typingTurnoffChars} characters · ~${typingEstimateSeconds(state.typingTurnoffChars)} sec"
                    } else {
                        "Wait ${formatCountdown(state.waitTurnoffSeconds)}"
                    },
                    background = NocturneTheme.colors.neutral.step900,
                )
                CostRow(
                    glyph = Ph.ClockCountdown,
                    tint = NocturneTheme.colors.accentRamp.step300,
                    title = "Each pause lasts",
                    subtitle = "${state.pauseMinutes} minutes, then re-arms",
                )
            }
            Spacer(Modifier.height(16.dp))
        }
        if (state.saveRejected) {
            // A rejected save kept the draft; the exact owner wording, with
            // no owner name, explains why the save did not go through.
            Text(
                "Already added to a block",
                fontSize = 11.5.sp,
                lineHeight = 17.sp,
                color = NocturneTheme.colors.neutral.step300,
                modifier = Modifier.padding(bottom = 6.dp),
            )
        }
        NocturneButton(
            "Save block",
            block = true,
            height = 46.dp,
            enabled = state.apps.isNotEmpty() || state.sites.isNotEmpty(),
            onClick = onSave,
        )
        // P3-F06 (owner request, 19 September): the OFF-after-save path is
        // stated explicitly; the block still saves switched off and is
        // turned on only from the Blocks homescreen.
        Text(
            "Turn it on from the \"Blocks\" homescreen.",
            fontSize = 11.5.sp,
            lineHeight = 17.sp,
            color = NocturneTheme.colors.neutral.step500,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                // Owner correction, 19 September: keep clear space below
                // the helper so it doesn't sit on the screen's bottom
                // edge above the flow container's own padding.
                .padding(top = 10.dp, bottom = 10.dp),
        )
    }
}

// The review's vertical list of everything the block covers (PRD §17 R6 and
// the 13 September addendum): the shared bounded target list — icon + label
// rows with hairline dividers, four visible rows, internal scrolling with a
// right-edge scrollbar beyond that.
@Composable
private fun ReviewTargetList(state: CreateFlowState) {
    BoundedTargetList(
        rows = buildList {
            state.apps.forEach { entry ->
                add(
                    TargetRow(
                        key = entry.packageName,
                        label = entry.label,
                        icon = entry.icon,
                        glyph = Ph.SquaresFour,
                    ),
                )
            }
            state.sites.forEach { domain ->
                add(TargetRow(key = domain, label = domain, icon = null, glyph = Ph.Globe))
            }
        },
    )
}

@Composable
private fun CostRow(glyph: Int, tint: Color, title: String, subtitle: String, background: Color = Color.Transparent) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(background)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PhosphorIcon(glyph, tint = tint, size = 20)
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, fontSize = 13.5.sp, color = NocturneTheme.colors.text)
            Text(subtitle, fontSize = 12.sp, color = NocturneTheme.colors.neutral.step500)
        }
    }
}

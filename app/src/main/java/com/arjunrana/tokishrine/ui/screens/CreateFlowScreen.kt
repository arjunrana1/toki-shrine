package com.arjunrana.tokishrine.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arjunrana.tokishrine.data.apps.AppEntry
import com.arjunrana.tokishrine.data.apps.InstalledAppsRepository
import com.arjunrana.tokishrine.data.entity.FrictionType
import com.arjunrana.tokishrine.data.repo.BlockDraft
import com.arjunrana.tokishrine.data.repo.BlockRepository
import com.arjunrana.tokishrine.data.repo.ConflictingOwnershipException
import com.arjunrana.tokishrine.data.repo.EventRepository
import com.arjunrana.tokishrine.ui.components.ButtonVariant
import com.arjunrana.tokishrine.ui.components.NocturneAppbar
import com.arjunrana.tokishrine.ui.components.NocturneButton
import com.arjunrana.tokishrine.ui.components.NocturneSegmented
import com.arjunrana.tokishrine.ui.components.NocturneStepper
import com.arjunrana.tokishrine.ui.components.NocturneSwitch
import com.arjunrana.tokishrine.ui.components.NocturneTextField
import com.arjunrana.tokishrine.ui.components.ProgressDots
import com.arjunrana.tokishrine.ui.components.SectionLabel
import com.arjunrana.tokishrine.ui.icons.Ph
import com.arjunrana.tokishrine.ui.icons.PhosphorIcon
import com.arjunrana.tokishrine.ui.theme.NocturneTheme
import com.arjunrana.tokishrine.ui.util.formatCountdown
import com.arjunrana.tokishrine.ui.util.formatEstimate
import com.arjunrana.tokishrine.ui.util.toImageBitmap
import com.arjunrana.tokishrine.ui.util.typingEstimateSeconds
import kotlinx.coroutines.launch

// Editor ranges and steps (PRD §7, revised by §17 R1/R7).
private const val PAUSE_MIN = 5
private const val PAUSE_MAX = 120
private const val PAUSE_STEP = 5
private const val PAUSE_CHARS_MIN = 50
private const val PAUSE_CHARS_MAX = 200
private const val CHARS_STEP = 10
private const val TURNOFF_CHARS_MIN = 100
private const val TURNOFF_CHARS_MAX = 350
private const val COUNTDOWN_MIN = 10
private const val COUNTDOWN_MAX = 300
private const val COUNTDOWN_STEP = 5
private const val NAME_MAX_CHARS = 20

// Review target list (PRD §17 R6): vertical rows with dividers; four rows
// are visible before the list scrolls internally.
private val REVIEW_ROW_HEIGHT = 46.dp
private val REVIEW_LIST_MAX_HEIGHT = REVIEW_ROW_HEIGHT * 4 + 3.dp

// Draft state for the four-step create/edit flow. The flow may be entered
// directly at the friction step (PRD §17 R9, detail → THE FRICTION → Edit);
// entryStep is the floor Back unwinds to before exiting the flow.
class CreateFlowState(val editBlockId: Long?, entryStep: Int = 1) {
    var step by mutableStateOf(entryStep)
    val entryStep = entryStep.coerceIn(1, 4)
    var showSearch by mutableStateOf(false)
    var appsTab by mutableStateOf(true)

    val apps = mutableStateListOf<AppEntry>()
    val sites = mutableStateListOf<String>()

    var name by mutableStateOf("")
    var frictionType by mutableStateOf(FrictionType.TYPING)
    var pauseMinutes by mutableStateOf(15)
    var pauseChars by mutableStateOf(100)
    var turnoffChars by mutableStateOf(300)
    var countdownSeconds by mutableStateOf(30)
    var showTypos by mutableStateOf(true)

    // Set when a save was rejected for conflicting ownership: the draft is
    // kept and the review step explains inline (owner decision — no owner
    // name, no dialog, no transfer).
    var saveRejected by mutableStateOf(false)

    // One-shot guard for the flow's single terminal transition (save or
    // abandon). It is taken synchronously, before any async work starts, so
    // repeated Back/Save input while an event write is in flight can neither
    // enqueue a second terminal event nor navigate twice. A rejected save
    // releases it, because the flow stays open.
    private var terminalTaken = false

    fun takeTerminal(): Boolean {
        if (terminalTaken) return false
        terminalTaken = true
        return true
    }

    fun releaseTerminal() {
        terminalTaken = false
    }

    fun draft() = BlockDraft(
        name = name.trim(),
        appPackageNames = apps.map { it.packageName },
        siteDomains = sites.toList(),
        frictionType = frictionType,
        pauseMinutes = pauseMinutes,
        pauseChars = pauseChars,
        turnoffChars = turnoffChars,
        countdownSeconds = countdownSeconds,
        showTypos = showTypos,
    )
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
    val scope = rememberCoroutineScope()
    val state = remember { CreateFlowState(editBlockId, initialStep) }

    // Edit mode: prefill the draft from the stored block.
    LaunchedEffect(editBlockId) {
        if (editBlockId != null) {
            blockRepo.getBlockWithContents(editBlockId)?.let { stored ->
                state.name = stored.block.name
                state.frictionType = stored.block.frictionType
                state.pauseMinutes = stored.block.pauseMinutes
                state.pauseChars = stored.block.pauseChars
                state.turnoffChars = stored.block.turnoffChars
                state.countdownSeconds = stored.block.countdownSeconds
                state.showTypos = stored.block.showTypos
                state.apps.addAll(
                    stored.apps.map { AppEntry(it.packageName, appsRepo.labelFor(it.packageName), null) },
                )
                state.sites.addAll(stored.sites.map { it.domain })
            }
        }
    }

    // PRD §10: creation telemetry, create mode only — edits log block_edited.
    LaunchedEffect(Unit) {
        if (editBlockId == null) eventRepo.log(EventRepository.EVENT_BLOCK_CREATE_STARTED)
    }

    fun stepCompleted(step: Int) {
        if (editBlockId == null) {
            scope.launch {
                eventRepo.log(EventRepository.EVENT_BLOCK_CREATE_STEP_COMPLETED, params = mapOf("step" to step))
            }
        }
    }

    fun abandonAndClose() {
        // Capture the exit step and take the guard synchronously, before the
        // async event write, so any Back/appbar input queued while that write
        // is suspended finds the guard taken and does nothing.
        val exitStep = state.step
        if (!state.takeTerminal()) return
        if (editBlockId == null) {
            scope.launch {
                eventRepo.log(
                    EventRepository.EVENT_BLOCK_CREATE_ABANDONED,
                    params = mapOf("step" to exitStep),
                )
                onClose()
            }
        } else {
            onClose()
        }
    }

    suspend fun persist(draft: BlockDraft) {
        run {
            if (editBlockId == null) {
                val id = blockRepo.createBlock(draft)
                eventRepo.log(EventRepository.EVENT_BLOCK_CREATE_STEP_COMPLETED, params = mapOf("step" to 4))
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
                    ),
                )
            } else {
                val changed = changedFields(blockRepo, editBlockId, draft)
                blockRepo.updateBlock(editBlockId, draft)
                eventRepo.log(
                    EventRepository.EVENT_BLOCK_EDITED,
                    blockId = editBlockId,
                    params = mapOf("fields_changed" to changed),
                )
            }
        }
    }

    fun save() {
        // Same terminal guard as abandon: while a save is in flight, Back and
        // further Save taps are no-ops, so exactly one of the two terminal
        // outcomes can ever run.
        if (!state.takeTerminal()) return
        state.saveRejected = false
        scope.launch {
            val draft = state.draft()
            try {
                persist(draft)
            } catch (e: ConflictingOwnershipException) {
                // Ownership changed while the editor was open — a stale draft.
                // The repository rolled the save back; keep the draft and
                // explain inline instead of silently discarding the work.
                state.releaseTerminal()
                state.saveRejected = true
                return@launch
            }
            onClose()
        }
    }

    // Android Back mirrors the appbar: app-search closes first, then the
    // flow steps backwards down to the entry step, and a Back there exits —
    // routed through the abandonment logger exactly once (create mode;
    // edits close silently). Direct friction entry (§17 R9) unwinds only to
    // step 3 before exiting. With the keyboard open the system consumes
    // Back to dismiss it, so this handler runs only once the keyboard is
    // down.
    fun stepBack(): Boolean {
        if (state.step > state.entryStep) {
            state.step -= 1
            return true
        }
        return false
    }

    BackHandler {
        when {
            state.showSearch -> state.showSearch = false
            !stepBack() -> abandonAndClose()
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(NocturneTheme.colors.bg)
            .statusBarsPadding(),
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
                state.step == 1 -> StepContents(
                    state,
                    blockRepo,
                    onBack = ::abandonAndClose,
                    onNext = { stepCompleted(1); state.step = 2 },
                )
                state.step == 2 -> StepName(state, onBack = { state.step = 1 }, onNext = { stepCompleted(2); state.step = 3 })
                state.step == 3 -> StepFriction(
                    state,
                    onBack = { if (!stepBack()) abandonAndClose() },
                    onNext = { stepCompleted(3); state.step = 4 },
                )
                else -> StepReview(
                    state,
                    onBack = {
                        state.saveRejected = false
                        state.step = 3
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
        if (current.block.showTypos != draft.showTypos) add("show_typos")
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
    // never enter the draft unchecked.
    val ownerships = siteOwnerships
    val canonicalInput = siteInput.trim().trimEnd('.').lowercase()
    val inputHeldElsewhere = ownerships != null &&
        canonicalInput.isNotEmpty() &&
        ownerships[canonicalInput]?.let { it != state.editBlockId } == true

    Column(Modifier.fillMaxSize()) {
        NocturneAppbar(
            title = if (state.editBlockId == null) "New block" else "Edit block",
            stepLabel = "1 / 4",
            onBack = onBack,
        )
        ProgressDots(total = 4, current = 1)
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
                    onValueChange = { siteInput = it },
                    hint = "example.com",
                    leading = { PhosphorIcon(Ph.Globe, tint = NocturneTheme.colors.neutral.step500) },
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                NocturneButton(
                    "Add",
                    variant = ButtonVariant.SECONDARY,
                    height = 38.dp,
                    enabled = ownerships != null &&
                        siteInput.isNotBlank() &&
                        !siteInput.contains(' ') &&
                        !inputHeldElsewhere,
                    onClick = {
                        if (ownerships == null || inputHeldElsewhere) return@NocturneButton
                        val domain = siteInput.trim().trimEnd('.').lowercase()
                        siteInput = ""
                        if (state.sites.none { it == domain }) state.sites.add(domain)
                    },
                )
            }
            Spacer(Modifier.height(6.dp))
            if (inputHeldElsewhere) {
                Text(
                    "Already added to a block",
                    fontSize = 11.5.sp,
                    lineHeight = 17.sp,
                    color = NocturneTheme.colors.neutral.step300,
                )
            } else {
                Text(
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
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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

        Spacer(Modifier.weight(1f))
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

    LaunchedEffect(Unit) {
        allApps.addAll(appsRepo.loadApps())
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
        if (query.isNotBlank()) {
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
            stepLabel = "2 / 4",
            onBack = onBack,
        )
        ProgressDots(total = 4, current = 2)
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
        // PRD §17 R7: single-line input, at most 20 characters. Display
        // names may wrap to two lines; no ellipsis anywhere. Paste (and the
        // SetText path) can carry line breaks past singleLine, so they are
        // stripped here; the 20-character cap truncates rather than
        // rejecting the update, so the field always shows exactly the
        // stored value.
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

// — Step 3: friction —

@Composable
private fun StepFriction(state: CreateFlowState, onBack: () -> Unit, onNext: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        NocturneAppbar(
            title = if (state.editBlockId == null) "New block" else "Edit block",
            stepLabel = "3 / 4",
            onBack = onBack,
        )
        ProgressDots(total = 4, current = 3)
        Spacer(Modifier.height(16.dp))
        Text(
            "What should getting in cost?",
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium,
            lineHeight = 23.sp,
            color = NocturneTheme.colors.text,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            "Tap either option to see how it's set up.",
            fontSize = 11.sp,
            lineHeight = 17.sp,
            color = NocturneTheme.colors.neutral.step500,
        )
        Spacer(Modifier.height(12.dp))
        NocturneSegmented(
            options = listOf(Ph.Keyboard to "Type words", Ph.Hourglass to "Wait it out"),
            selectedIndex = if (state.frictionType == FrictionType.TYPING) 0 else 1,
            onSelect = { state.frictionType = if (it == 0) FrictionType.TYPING else FrictionType.DELAY },
        )
        Spacer(Modifier.height(18.dp))

        // PRD §17 R2: control order and headings. Typing: Type to pause →
        // Pause duration → Disable this block. Delay: Wait for this
        // duration → Pause duration → Disable this block. The scroller keeps
        // the estimate and Next reachable with the longer helper copy.
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (state.frictionType == FrictionType.TYPING) {
                StepperField(
                    label = "Type to pause",
                    helper = "You'll need to type this number of characters each time you want to pause the block.",
                    value = "${state.pauseChars} characters",
                    onDecrement = { state.pauseChars = (state.pauseChars - CHARS_STEP).coerceAtLeast(PAUSE_CHARS_MIN) },
                    onIncrement = { state.pauseChars = (state.pauseChars + CHARS_STEP).coerceAtMost(PAUSE_CHARS_MAX) },
                    canDecrement = state.pauseChars > PAUSE_CHARS_MIN,
                    canIncrement = state.pauseChars < PAUSE_CHARS_MAX,
                )
            } else {
                StepperField(
                    label = "Wait for this duration",
                    helper = "You'll need to wait for this long each time you want to pause the block.",
                    value = formatCountdown(state.countdownSeconds),
                    onDecrement = { state.countdownSeconds = (state.countdownSeconds - COUNTDOWN_STEP).coerceAtLeast(COUNTDOWN_MIN) },
                    onIncrement = { state.countdownSeconds = (state.countdownSeconds + COUNTDOWN_STEP).coerceAtMost(COUNTDOWN_MAX) },
                    canDecrement = state.countdownSeconds > COUNTDOWN_MIN,
                    canIncrement = state.countdownSeconds < COUNTDOWN_MAX,
                )
            }

            StepperField(
                label = "Pause duration",
                helper = "Stay unblocked for this long. Then the block will be activated again.",
                value = "${state.pauseMinutes} min",
                onDecrement = { state.pauseMinutes = (state.pauseMinutes - PAUSE_STEP).coerceAtLeast(PAUSE_MIN) },
                onIncrement = { state.pauseMinutes = (state.pauseMinutes + PAUSE_STEP).coerceAtMost(PAUSE_MAX) },
                canDecrement = state.pauseMinutes > PAUSE_MIN,
                canIncrement = state.pauseMinutes < PAUSE_MAX,
                footNote = "Minimum is 5 minutes",
            )

            // Turning a block off is always a typed passage, so it is
            // configured on both variants.
            StepperField(
                label = "Disable this block",
                helper = "Type these many characters to disable the block completely. You can edit or delete it once it's off.",
                value = "${state.turnoffChars} characters",
                onDecrement = { state.turnoffChars = (state.turnoffChars - CHARS_STEP).coerceAtLeast(TURNOFF_CHARS_MIN) },
                onIncrement = { state.turnoffChars = (state.turnoffChars + CHARS_STEP).coerceAtMost(TURNOFF_CHARS_MAX) },
                canDecrement = state.turnoffChars > TURNOFF_CHARS_MIN,
                canIncrement = state.turnoffChars < TURNOFF_CHARS_MAX,
            )

            if (state.frictionType == FrictionType.TYPING) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Show where the typos are",
                        fontSize = 13.5.sp,
                        color = NocturneTheme.colors.text,
                        modifier = Modifier.weight(1f),
                    )
                    NocturneSwitch(checked = state.showTypos, onCheckedChange = { state.showTypos = it })
                }
            }
        }

        // The live estimate stays pinned above Next while the passage length
        // is adjusted. PRD §17 R4 removed the delay variant of this box; the
        // countdown itself is the delay estimate.
        if (state.frictionType == FrictionType.TYPING) {
            Spacer(Modifier.height(14.dp))
            EstimateBox(state)
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

@Composable
private fun StepperField(
    label: String,
    helper: String?,
    value: String,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    canDecrement: Boolean,
    canIncrement: Boolean,
    footNote: String? = null,
) {
    Column {
        Text(label, fontSize = 12.sp, color = NocturneTheme.colors.neutral.step400)
        if (helper != null) {
            Spacer(Modifier.height(2.dp))
            Text(helper, fontSize = 11.5.sp, lineHeight = 17.sp, color = NocturneTheme.colors.neutral.step500)
        }
        Spacer(Modifier.height(8.dp))
        NocturneStepper(
            value = value,
            onDecrement = onDecrement,
            onIncrement = onIncrement,
            canDecrement = canDecrement,
            canIncrement = canIncrement,
        )
        if (footNote != null) {
            Spacer(Modifier.height(5.dp))
            Text(footNote, fontSize = 10.5.sp, color = NocturneTheme.colors.neutral.step600)
        }
    }
}

@Composable
private fun EstimateBox(state: CreateFlowState) {
    val colors = NocturneTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .background(colors.accentRamp.step900, RoundedCornerShape(12.dp))
            .border(1.dp, colors.accentRamp.step800, RoundedCornerShape(12.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PhosphorIcon(Ph.Timer, tint = colors.accentRamp.step300, size = 22)
        Spacer(Modifier.width(11.dp))
        Text(
            "${formatEstimate(typingEstimateSeconds(state.pauseChars))} to type each time you want in",
            fontSize = 13.sp,
            lineHeight = 18.sp,
            color = colors.text,
        )
    }
}

// — Step 4: review & save —

@Composable
private fun StepReview(state: CreateFlowState, onBack: () -> Unit, onSave: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        NocturneAppbar(
            title = if (state.editBlockId == null) "New block" else "Edit block",
            stepLabel = "4 / 4",
            onBack = onBack,
        )
        ProgressDots(total = 4, current = 4)
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
                CostRow(
                    glyph = if (state.frictionType == FrictionType.TYPING) Ph.Keyboard else Ph.Hourglass,
                    tint = NocturneTheme.colors.accentRamp.step300,
                    title = "To pause it",
                    subtitle = if (state.frictionType == FrictionType.TYPING) {
                        "Type ${state.pauseChars} characters · ~${typingEstimateSeconds(state.pauseChars)} sec"
                    } else {
                        "Wait ${formatCountdown(state.countdownSeconds)}, phone in hand"
                    },
                )
                CostRow(
                    glyph = Ph.LockKeyOpen,
                    tint = NocturneTheme.colors.neutral.step400,
                    title = "To turn this block off",
                    subtitle = "Type ${state.turnoffChars} characters · ~${typingEstimateSeconds(state.turnoffChars)} sec",
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
    }
}

// Vertical review list of everything the block covers (PRD §17 R6, after the
// owner's reference image): icon + label rows with hairline dividers, apps
// first then sites, bounded to four visible rows — longer lists scroll
// inside the card. Labels wrap rather than overflow horizontally.
@Composable
private fun ReviewTargetList(state: CreateFlowState) {
    Column(
        Modifier
            .fillMaxWidth()
            .heightIn(max = REVIEW_LIST_MAX_HEIGHT)
            .verticalScroll(rememberScrollState())
            .background(NocturneTheme.colors.surface, RoundedCornerShape(12.dp)),
    ) {
        state.apps.forEachIndexed { index, entry ->
            if (index > 0) ReviewRowDivider()
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = REVIEW_ROW_HEIGHT)
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AppIcon(entry)
                Spacer(Modifier.width(12.dp))
                Text(
                    entry.label,
                    fontSize = 15.sp,
                    lineHeight = 20.sp,
                    color = NocturneTheme.colors.text,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        state.sites.forEachIndexed { index, domain ->
            if (index > 0 || state.apps.isNotEmpty()) ReviewRowDivider()
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = REVIEW_ROW_HEIGHT)
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PhosphorIcon(Ph.Globe, tint = NocturneTheme.colors.neutral.step400, size = 22)
                Spacer(Modifier.width(12.dp))
                Text(
                    domain,
                    fontSize = 15.sp,
                    lineHeight = 20.sp,
                    color = NocturneTheme.colors.text,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun ReviewRowDivider() {
    Box(
        Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(NocturneTheme.colors.divider),
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


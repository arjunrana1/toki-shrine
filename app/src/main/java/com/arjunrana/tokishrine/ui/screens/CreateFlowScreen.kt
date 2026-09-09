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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.arjunrana.tokishrine.ui.components.NocturneChip
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

// Editor ranges and steps (PRD §7).
private const val PAUSE_MIN = 15
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

// Draft state for the four-step create/edit flow.
class CreateFlowState(val editBlockId: Long?) {
    var step by mutableStateOf(1)
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
) {
    val scope = rememberCoroutineScope()
    val state = remember { CreateFlowState(editBlockId) }

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
        if (editBlockId == null) {
            scope.launch {
                eventRepo.log(
                    EventRepository.EVENT_BLOCK_CREATE_ABANDONED,
                    params = mapOf("step" to state.step),
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
        scope.launch {
            val draft = state.draft()
            try {
                persist(draft)
            } catch (e: ConflictingOwnershipException) {
                // The picker blocks owned targets, so this means ownership
                // changed while the editor was open. The repository rolled
                // the save back; closing shows the current reality.
            }
            onClose()
        }
    }

    // Android Back mirrors the appbar: app-search closes first, then the
    // flow steps backwards, and only a Back on step 1 exits — routed through
    // the abandonment logger exactly once (create mode). With the keyboard
    // open the system consumes Back to dismiss it, so this handler runs only
    // once the keyboard is down.
    BackHandler {
        when {
            state.showSearch -> state.showSearch = false
            state.step > 1 -> state.step -= 1
            else -> abandonAndClose()
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
                state.step == 3 -> StepFriction(state, onBack = { state.step = 2 }, onNext = { stepCompleted(3); state.step = 4 })
                else -> StepReview(state, onBack = { state.step = 3 }, onSave = ::save)
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
    var siteOwnerships by remember { mutableStateOf<Map<String, Long>>(emptyMap()) }

    LaunchedEffect(Unit) {
        siteOwnerships = blockRepo.siteOwnerships()
    }

    // A domain owned by another block cannot be added: the entry shows
    // Already added to a block (owner decision — no owner name, no dialog).
    val canonicalInput = siteInput.trim().trimEnd('.').lowercase()
    val inputHeldElsewhere = canonicalInput.isNotEmpty() &&
        siteOwnerships[canonicalInput]?.let { it != state.editBlockId } == true

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
                    enabled = siteInput.isNotBlank() && !siteInput.contains(' ') && !inputHeldElsewhere,
                    onClick = {
                        if (inputHeldElsewhere) return@NocturneButton
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
            NocturneButton("Next", height = 44.dp, paddingHorizontal = 26.dp, onClick = onNext)
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
    var ownerships by remember { mutableStateOf<Map<String, Long>>(emptyMap()) }

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
                // selectable, labelled without naming the owner block.
                val heldElsewhere = ownerships[entry.packageName]?.let { it != state.editBlockId } == true
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !heldElsewhere) {
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
        NocturneTextField(
            value = state.name,
            onValueChange = { state.name = it },
            hint = "Block name",
            fontSize = 17,
            minHeight = 48.dp,
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

        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            StepperField(
                label = "Pause lasts",
                helper = "Once you're in, the block stays off for this long — then it quietly comes back on its own. No need to re-lock it.",
                value = "${state.pauseMinutes} min",
                onDecrement = { state.pauseMinutes = (state.pauseMinutes - PAUSE_STEP).coerceAtLeast(PAUSE_MIN) },
                onIncrement = { state.pauseMinutes = (state.pauseMinutes + PAUSE_STEP).coerceAtMost(PAUSE_MAX) },
                canDecrement = state.pauseMinutes > PAUSE_MIN,
                canIncrement = state.pauseMinutes < PAUSE_MAX,
                footNote = "Minimum is 15 minutes",
            )

            if (state.frictionType == FrictionType.TYPING) {
                StepperField(
                    label = "Passage length",
                    helper = null,
                    value = "${state.pauseChars} characters",
                    onDecrement = { state.pauseChars = (state.pauseChars - CHARS_STEP).coerceAtLeast(PAUSE_CHARS_MIN) },
                    onIncrement = { state.pauseChars = (state.pauseChars + CHARS_STEP).coerceAtMost(PAUSE_CHARS_MAX) },
                    canDecrement = state.pauseChars > PAUSE_CHARS_MIN,
                    canIncrement = state.pauseChars < PAUSE_CHARS_MAX,
                )
            } else {
                StepperField(
                    label = "Wait before you're in",
                    helper = "You sit on a quiet countdown for this long before the app opens. Nothing to type.",
                    value = formatCountdown(state.countdownSeconds),
                    onDecrement = { state.countdownSeconds = (state.countdownSeconds - COUNTDOWN_STEP).coerceAtLeast(COUNTDOWN_MIN) },
                    onIncrement = { state.countdownSeconds = (state.countdownSeconds + COUNTDOWN_STEP).coerceAtMost(COUNTDOWN_MAX) },
                    canDecrement = state.countdownSeconds > COUNTDOWN_MIN,
                    canIncrement = state.countdownSeconds < COUNTDOWN_MAX,
                )
            }

            // The third numeric control (PRD §7.1): turning a block off is
            // always a typed passage, so it is configured on both variants.
            StepperField(
                label = "Turn-off passage length",
                helper = null,
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

        Spacer(Modifier.weight(1f))
        EstimateBox(state)
        Spacer(Modifier.height(12.dp))
        NocturneButton("Next", block = true, height = 46.dp, onClick = onNext)
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
    val text = if (state.frictionType == FrictionType.TYPING) {
        "${formatEstimate(typingEstimateSeconds(state.pauseChars))} to type each time you want in · ~20 wpm on mobile"
    } else {
        "A ${formatCountdown(state.countdownSeconds)} wait each time you want in · no typing, just sit it out"
    }
    Row(
        Modifier
            .fillMaxWidth()
            .background(colors.accentRamp.step900, RoundedCornerShape(12.dp))
            .border(1.dp, colors.accentRamp.step800, RoundedCornerShape(12.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PhosphorIcon(
            if (state.frictionType == FrictionType.TYPING) Ph.Timer else Ph.Hourglass,
            tint = colors.accentRamp.step300,
            size = 22,
        )
        Spacer(Modifier.width(11.dp))
        Text(text, fontSize = 13.sp, lineHeight = 18.sp, color = colors.text)
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
        Spacer(Modifier.height(18.dp))
        Text(
            state.name.ifBlank { "Untitled block" },
            fontSize = 23.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = (-0.23).sp,
            lineHeight = 26.sp,
            color = NocturneTheme.colors.text,
        )
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            state.apps.take(3).forEach { NocturneChip(it.label) }
            if (state.apps.size > 3) NocturneChip("+${state.apps.size - 3} apps")
            if (state.sites.size == 1) NocturneChip(state.sites.first())
            if (state.sites.size > 1) NocturneChip("${state.sites.size} sites")
        }
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
        Spacer(Modifier.height(14.dp))
        Row {
            Text(
                "It'll be saved switched ",
                fontSize = 12.sp,
                lineHeight = 18.sp,
                color = NocturneTheme.colors.neutral.step500,
            )
            Text(
                "off",
                fontSize = 12.sp,
                lineHeight = 18.sp,
                color = NocturneTheme.colors.neutral.step300,
                fontWeight = FontWeight.Bold,
            )
            Text(
                ". Nothing happens until you turn it on.",
                fontSize = 12.sp,
                lineHeight = 18.sp,
                color = NocturneTheme.colors.neutral.step500,
            )
        }
        Spacer(Modifier.weight(1f))
        NocturneButton("Save block", block = true, height = 46.dp, onClick = onSave)
    }
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


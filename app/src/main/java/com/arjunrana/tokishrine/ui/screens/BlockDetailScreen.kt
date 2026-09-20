package com.arjunrana.tokishrine.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.lifecycleScope
import com.arjunrana.tokishrine.data.apps.InstalledAppsRepository
import com.arjunrana.tokishrine.data.db.BlockWithContents
import com.arjunrana.tokishrine.data.entity.FrictionType
import com.arjunrana.tokishrine.data.repo.BlockRepository
import com.arjunrana.tokishrine.data.repo.EventRepository
import com.arjunrana.tokishrine.ui.components.ButtonVariant
import com.arjunrana.tokishrine.ui.components.BoundedTargetList
import com.arjunrana.tokishrine.ui.components.NocturneAppbar
import com.arjunrana.tokishrine.ui.components.NocturneButton
import com.arjunrana.tokishrine.ui.components.NocturneSwitch
import com.arjunrana.tokishrine.ui.components.SectionLabel
import com.arjunrana.tokishrine.ui.components.TargetRow
import com.arjunrana.tokishrine.ui.icons.Ph
import com.arjunrana.tokishrine.ui.icons.PhosphorIcon
import com.arjunrana.tokishrine.ui.theme.NocturneTheme
import com.arjunrana.tokishrine.ui.util.BlockHaptics
import com.arjunrana.tokishrine.ui.util.TerminalAction
import com.arjunrana.tokishrine.ui.util.formatCountdown
import com.arjunrana.tokishrine.ui.util.typingEstimateSeconds
import kotlinx.coroutines.launch

// Block detail & edit (screen 14). Editing is reachable only while the block
// is OFF — when it is ON the edit actions are hidden and the screen says the
// block must be turned off first (PRD §4). The two Edit actions open the
// shared editor at different steps (PRD §17 R9): contents at 1/4, friction
// directly at 3/4.
@Composable
fun BlockDetailScreen(
    blockId: Long,
    blockRepo: BlockRepository,
    eventRepo: EventRepository,
    appsRepo: InstalledAppsRepository,
    onBack: () -> Unit,
    onTurnOn: (Long) -> Unit,
    onEditContents: (Long) -> Unit,
    onEditFriction: (Long) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var block by remember { mutableStateOf<BlockWithContents?>(null) }
    var refresh by remember { mutableIntStateOf(0) }
    var confirmDelete by remember { mutableStateOf(false) }

    // Terminal transitions run in the activity lifecycle scope and are
    // single-flight (review blocker 2).
    val terminal = remember(lifecycleOwner) { TerminalAction(lifecycleOwner.lifecycleScope) }

    LaunchedEffect(blockId, refresh) {
        block = blockRepo.getBlockWithContents(blockId)
    }

    val loaded = block ?: return
    val isOff = !loaded.block.enabled

    fun turnOff() {
        // State change and block_turned_off commit in one transaction; the
        // lighter haptic (owner addendum, 13 September) fires only after a
        // real change; the screen refreshes once the commit succeeds.
        terminal.run(
            commit = { blockRepo.setEnabledRecordingTransition(blockId, false) },
            onChanged = { BlockHaptics.turnedOff(context) },
            onCommitted = { refresh++ },
        )
    }

    fun delete() {
        scope.launch {
            blockRepo.deleteBlock(blockId)
            eventRepo.log(EventRepository.EVENT_BLOCK_DELETED, blockId = blockId)
            onBack()
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
            NocturneAppbar(
                title = loaded.block.name,
                onBack = onBack,
                trailing = {
                    if (isOff) {
                        // PRD §17 R14: deleting asks first — Confirm is the
                        // only path that removes anything.
                        PhosphorIcon(
                            Ph.Trash,
                            tint = NocturneTheme.colors.neutral.step500,
                            size = 19,
                            modifier = Modifier.clickable { confirmDelete = true },
                        )
                    }
                },
            )

            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                // Status card. PRD §17 R10: the switch is the sole activation
                // control — no separate Turn on button.
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(NocturneTheme.colors.surface, RoundedCornerShape(12.dp))
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    NocturneSwitch(checked = loaded.block.enabled, onCheckedChange = { on ->
                        if (on) onTurnOn(blockId) else turnOff()
                    })
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            if (loaded.block.enabled) "On" else "Off",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = NocturneTheme.colors.text,
                        )
                        Text(
                            if (loaded.block.enabled) "Enforcing right now" else "Not enforcing right now",
                            fontSize = 11.5.sp,
                            color = NocturneTheme.colors.neutral.step500,
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // The edit rule, stated on the screen either way (PRD §4).
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(NocturneTheme.colors.neutral.step900, RoundedCornerShape(10.dp))
                        .padding(horizontal = 13.dp, vertical = 11.dp),
                ) {
                    PhosphorIcon(
                        Ph.Info,
                        tint = NocturneTheme.colors.neutral.step400,
                        size = 15,
                        modifier = Modifier.padding(top = 1.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (isOff) {
                            "A block has to be off before you can edit it. It's off now, so go ahead."
                        } else {
                            "A block has to be off before you can edit it. Turn it off first."
                        },
                        fontSize = 11.5.sp,
                        lineHeight = 17.sp,
                        color = NocturneTheme.colors.neutral.step500,
                    )
                }
                Spacer(Modifier.height(18.dp))

                SectionLabel("APPS & SITES", trailing = {
                    if (isOff) {
                        Text(
                            "Edit",
                            fontSize = 11.5.sp,
                            color = NocturneTheme.colors.accentRamp.step300,
                            modifier = Modifier.clickable { onEditContents(blockId) },
                        )
                    }
                })
                Spacer(Modifier.height(10.dp))
                // Owner addendum, 13 September: the detail list matches the
                // create-review list — vertical icon + label rows with
                // dividers, four visible rows then a scrollbar.
                if (loaded.apps.isEmpty() && loaded.sites.isEmpty()) {
                    Text("Nothing in this block", fontSize = 12.5.sp, color = NocturneTheme.colors.neutral.step500)
                } else {
                    BoundedTargetList(
                        rows = remember(loaded) {
                            buildList {
                                loaded.apps.forEach {
                                    add(
                                        TargetRow(
                                            key = it.packageName,
                                            label = appsRepo.labelFor(it.packageName),
                                            icon = appsRepo.iconFor(it.packageName),
                                            glyph = Ph.SquaresFour,
                                        ),
                                    )
                                }
                                loaded.sites.forEach {
                                    add(TargetRow(key = it.domain, label = it.domain, icon = null, glyph = Ph.Globe))
                                }
                            }
                        },
                    )
                }
                Spacer(Modifier.height(20.dp))

                SectionLabel("THE FRICTION", trailing = {
                    if (isOff) {
                        Text(
                            "Edit",
                            fontSize = 11.5.sp,
                            color = NocturneTheme.colors.accentRamp.step300,
                            modifier = Modifier.clickable { onEditFriction(blockId) },
                        )
                    }
                })
                Spacer(Modifier.height(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    FrictionLine(
                        label = "To pause",
                        value = if (loaded.block.frictionType == FrictionType.TYPING) {
                            "Type ${loaded.block.pauseChars} · ~${typingEstimateSeconds(loaded.block.pauseChars)}s"
                        } else {
                            "Wait ${formatCountdown(loaded.block.countdownSeconds)}"
                        },
                    )
                    // Wizard redesign (19 September): disabling follows the
                    // block's method — the summary must not describe typing
                    // for a waiting block.
                    FrictionLine(
                        label = "To turn off",
                        value = if (loaded.block.frictionType == FrictionType.TYPING) {
                            "Type ${loaded.block.turnoffChars} · ~${typingEstimateSeconds(loaded.block.turnoffChars)}s"
                        } else {
                            "Wait ${formatCountdown(loaded.block.turnoffSeconds)}"
                        },
                    )
                    FrictionLine(
                        label = "Pause lasts",
                        value = "${loaded.block.pauseMinutes} minutes",
                    )
                }
            }
        }

        // PRD §17 R14: deleting an OFF block asks first. Go back (or tapping
        // outside) leaves everything untouched; only Confirm deletes. The
        // detail screen has no text field, so the One UI keyboard+dialog
        // freeze guidance does not apply here.
        if (confirmDelete) {
            Dialog(onDismissRequest = { confirmDelete = false }) {
                Column(
                    Modifier
                        .widthIn(min = 280.dp)
                        .background(NocturneTheme.colors.surface, RoundedCornerShape(14.dp))
                        .padding(20.dp),
                ) {
                    Text(
                        "Are you sure?",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = NocturneTheme.colors.text,
                    )
                    Spacer(Modifier.height(18.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        NocturneButton(
                            "Confirm",
                            modifier = Modifier.weight(1f),
                            height = 42.dp,
                            onClick = {
                                confirmDelete = false
                                delete()
                            },
                        )
                        NocturneButton(
                            "Go back",
                            variant = ButtonVariant.SECONDARY,
                            modifier = Modifier.weight(1f),
                            height = 42.dp,
                            onClick = { confirmDelete = false },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FrictionLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth()) {
        Text(
            label,
            fontSize = 13.sp,
            color = NocturneTheme.colors.neutral.step400,
            modifier = Modifier.weight(1f),
        )
        Text(value, fontSize = 13.sp, color = NocturneTheme.colors.text)
    }
}

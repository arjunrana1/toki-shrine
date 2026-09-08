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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arjunrana.tokishrine.data.apps.InstalledAppsRepository
import com.arjunrana.tokishrine.data.db.BlockWithContents
import com.arjunrana.tokishrine.data.entity.FrictionType
import com.arjunrana.tokishrine.data.repo.BlockRepository
import com.arjunrana.tokishrine.data.repo.EventRepository
import com.arjunrana.tokishrine.ui.components.NocturneAppbar
import com.arjunrana.tokishrine.ui.components.NocturneButton
import com.arjunrana.tokishrine.ui.components.NocturneChip
import com.arjunrana.tokishrine.ui.components.NocturneSwitch
import com.arjunrana.tokishrine.ui.components.SectionLabel
import com.arjunrana.tokishrine.ui.icons.Ph
import com.arjunrana.tokishrine.ui.icons.PhosphorIcon
import com.arjunrana.tokishrine.ui.theme.NocturneTheme
import com.arjunrana.tokishrine.ui.util.formatCountdown
import com.arjunrana.tokishrine.ui.util.typingEstimateSeconds
import kotlinx.coroutines.launch

// Block detail & edit (screen 14). Editing is reachable only while the block
// is OFF — when it is ON the edit actions are hidden and the screen says the
// block must be turned off first (PRD §4).
@Composable
fun BlockDetailScreen(
    blockId: Long,
    blockRepo: BlockRepository,
    eventRepo: EventRepository,
    appsRepo: InstalledAppsRepository,
    onBack: () -> Unit,
    onTurnOn: (Long) -> Unit,
    onEdit: (Long) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var block by remember { mutableStateOf<BlockWithContents?>(null) }
    var refresh by remember { mutableIntStateOf(0) }

    LaunchedEffect(blockId, refresh) {
        block = blockRepo.getBlockWithContents(blockId)
    }

    val loaded = block ?: return
    val isOff = !loaded.block.enabled

    fun turnOff() {
        scope.launch {
            blockRepo.setEnabled(blockId, false)
            eventRepo.log(EventRepository.EVENT_BLOCK_TURNED_OFF, blockId = blockId)
            refresh++
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
            NocturneAppbar(
                title = loaded.block.name,
                onBack = onBack,
                trailing = {
                    if (isOff) {
                        PhosphorIcon(
                            Ph.Trash,
                            tint = NocturneTheme.colors.neutral.step500,
                            size = 19,
                            modifier = Modifier.clickable {
                                scope.launch {
                                    blockRepo.deleteBlock(blockId)
                                    eventRepo.log(EventRepository.EVENT_BLOCK_DELETED, blockId = blockId)
                                    onBack()
                                }
                            },
                        )
                    }
                },
            )

            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                // Status card.
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
                    if (isOff) {
                        NocturneButton("Turn on", height = 38.dp, onClick = { onTurnOn(blockId) })
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
                            modifier = Modifier.clickable { onEdit(blockId) },
                        )
                    }
                })
                Spacer(Modifier.height(10.dp))
                Row {
                    loaded.apps.take(3).forEach { NocturneChip(appsRepo.labelFor(it.packageName)) }
                    if (loaded.apps.size > 3) NocturneChip("+${loaded.apps.size - 3} apps")
                    if (loaded.sites.size == 1) NocturneChip(loaded.sites[0].domain)
                    if (loaded.sites.size > 1) NocturneChip("${loaded.sites.size} sites")
                    if (loaded.apps.isEmpty() && loaded.sites.isEmpty()) {
                        Text("Nothing in this block", fontSize = 12.5.sp, color = NocturneTheme.colors.neutral.step500)
                    }
                }
                Spacer(Modifier.height(20.dp))

                SectionLabel("THE FRICTION", trailing = {
                    if (isOff) {
                        Text(
                            "Edit",
                            fontSize = 11.5.sp,
                            color = NocturneTheme.colors.accentRamp.step300,
                            modifier = Modifier.clickable { onEdit(blockId) },
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
                    FrictionLine(
                        label = "To turn off",
                        value = "Type ${loaded.block.turnoffChars} · ~${typingEstimateSeconds(loaded.block.turnoffChars)}s",
                    )
                    FrictionLine(
                        label = "Pause lasts",
                        value = "${loaded.block.pauseMinutes} minutes",
                    )
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

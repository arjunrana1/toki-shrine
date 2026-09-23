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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arjunrana.tokishrine.data.db.BlockWithContents
import com.arjunrana.tokishrine.data.apps.InstalledAppsRepository
import com.arjunrana.tokishrine.data.entity.FrictionType
import com.arjunrana.tokishrine.data.repo.BlockRepository
import com.arjunrana.tokishrine.ui.components.ButtonVariant
import com.arjunrana.tokishrine.ui.components.NocturneButton
import com.arjunrana.tokishrine.ui.components.NocturneSwitch
import com.arjunrana.tokishrine.ui.icons.Ph
import com.arjunrana.tokishrine.ui.icons.PhosphorIcon
import com.arjunrana.tokishrine.ui.theme.NocturneTheme
import com.arjunrana.tokishrine.ui.util.formatCountdown

// Home screen — block list, empty and populated (screens 5 and 6).
@Composable
fun BlockListScreen(
    blockRepo: BlockRepository,
    appsRepo: InstalledAppsRepository,
    onCreate: () -> Unit,
    onOpenDetail: (Long) -> Unit,
    onTurnOn: (Long) -> Unit,
    onTurnOff: (Long) -> Unit,
    onOpenStats: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenFeedback: () -> Unit,
) {
    val blocks by blockRepo.observeBlocksWithContents().collectAsState(initial = null as List<BlockWithContents>?)

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
            Row(
                Modifier
                    .fillMaxWidth()
                    // Owner feel correction, 19 September: the home title,
                    // like the app bars, needs a real gap below the status
                    // bar — this row is this screen's only header.
                    .padding(top = 18.dp, bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Blocks",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Medium,
                    color = NocturneTheme.colors.text,
                    modifier = Modifier.weight(1f),
                )
                // Header icon buttons (PRD §6 screens 5–6): Stats (Phase 7)
                // and Settings. Both carry a semantic label and a touch
                // target larger than their glyph.
                HeaderIconButton(glyph = Ph.ChartBar, label = "Stats", onClick = onOpenStats)
                Spacer(Modifier.width(8.dp))
                HeaderIconButton(glyph = Ph.GearSix, label = "Settings", onClick = onOpenSettings)
            }

            val list = blocks
            if (list == null) {
                // First load: keep the frame empty rather than flashing the
                // empty state.
            } else if (list.isEmpty()) {
                EmptyState(onCreate = onCreate, modifier = Modifier.weight(1f))
            } else {
                Column(
                    Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(11.dp),
                ) {
                    list.forEach { block ->
                        BlockRow(
                            block = block,
                            labelFor = { appsRepo.labelFor(it) },
                            onClick = { onOpenDetail(block.block.id) },
                            onToggle = { on ->
                                if (on) {
                                    onTurnOn(block.block.id)
                                } else {
                                    onTurnOff(block.block.id)
                                }
                            },
                        )
                    }
                }
            }

            if (!list.isNullOrEmpty()) {
                // PRD §17 R11 plus the 13 September addendum: one pinned
                // bottom action row — smaller Feedback on the left (label
                // only, per owner feedback), wider + New Block on the right
                // — with padding above and below.
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    NocturneButton(
                        "Feedback",
                        variant = ButtonVariant.SECONDARY,
                        modifier = Modifier.weight(1f),
                        height = 44.dp,
                        fontSize = 13,
                        onClick = onOpenFeedback,
                    )
                    NocturneButton(
                        "New Block",
                        modifier = Modifier.weight(1.5f),
                        height = 44.dp,
                        fontSize = 13,
                        onClick = onCreate,
                        leading = { PhosphorIcon(Ph.Plus, tint = NocturneTheme.colors.accent, size = 16) },
                    )
                }
            }
        }
    }
}

@Composable
private fun HeaderIconButton(glyph: Int, label: String, onClick: () -> Unit) {
    val colors = NocturneTheme.colors
    Box(
        Modifier
            .size(44.dp)
            // clearAndSetSemantics keeps the decorative glyph unannounced and
            // gives the button its accessible name and role (Phase 7 pass).
            .clearAndSetSemantics { contentDescription = label; role = Role.Button }
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        PhosphorIcon(glyph, tint = colors.neutral.step300, size = 21)
    }
}

@Composable
private fun EmptyState(onCreate: () -> Unit, modifier: Modifier = Modifier) {
    val colors = NocturneTheme.colors
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier
                .size(64.dp)
                .background(colors.neutral.step900, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center,
        ) {
            PhosphorIcon(Ph.Stack, tint = colors.neutral.step500, size = 30)
        }
        Spacer(Modifier.height(20.dp))
        Text("No blocks yet", fontSize = 17.sp, fontWeight = FontWeight.Medium, color = colors.text)
        Spacer(Modifier.height(8.dp))
        Text(
            "A block is a group of apps or sites with one pause in front of them. Make your first one whenever you're ready.",
            fontSize = 13.5.sp,
            lineHeight = 22.sp,
            color = colors.neutral.step500,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Spacer(Modifier.height(22.dp))
        NocturneButton("Create your first block", height = 44.dp, onClick = onCreate)
    }
}

@Composable
private fun BlockRow(
    block: BlockWithContents,
    labelFor: (String) -> String,
    onClick: () -> Unit,
    onToggle: (Boolean) -> Unit,
) {
    val colors = NocturneTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .background(colors.surface, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(block.block.name, fontSize = 14.5.sp, fontWeight = FontWeight.Medium, color = colors.text)
            Spacer(Modifier.height(1.dp))
            Text(
                summaryLine(block, labelFor),
                fontSize = 11.5.sp,
                color = colors.neutral.step500,
            )
        }
        Spacer(Modifier.width(12.dp))
        NocturneSwitch(checked = block.block.enabled, onCheckedChange = onToggle)
    }
}

// Row summary: "Instagram, TikTok · Type 150" / "2 sites · Wait 30 sec".
private fun summaryLine(block: BlockWithContents, labelFor: (String) -> String): String {
    val labels = block.apps.map { labelFor(it.packageName) }
    val parts = buildList {
        if (labels.isNotEmpty()) add(labels.joinToString(", "))
        when (block.sites.size) {
            0 -> {}
            1 -> add(block.sites[0].domain)
            else -> add("${block.sites.size} sites")
        }
    }
    val contents = if (parts.isEmpty()) "Empty" else parts.joinToString(" + ")
    val friction = if (block.block.frictionType == FrictionType.TYPING) {
        "Type ${block.block.pauseChars}"
    } else {
        "Wait ${formatCountdown(block.block.countdownSeconds)}"
    }
    return "$contents · $friction"
}

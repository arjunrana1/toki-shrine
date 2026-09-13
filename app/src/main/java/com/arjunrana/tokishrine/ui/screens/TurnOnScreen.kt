package com.arjunrana.tokishrine.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.arjunrana.tokishrine.data.db.BlockWithContents
import com.arjunrana.tokishrine.data.entity.FrictionType
import com.arjunrana.tokishrine.data.repo.BlockRepository
import com.arjunrana.tokishrine.ui.components.ButtonVariant
import com.arjunrana.tokishrine.ui.components.NocturneButton
import com.arjunrana.tokishrine.ui.icons.Ph
import com.arjunrana.tokishrine.ui.icons.PhosphorIcon
import com.arjunrana.tokishrine.ui.theme.NocturneTheme
import com.arjunrana.tokishrine.ui.util.BlockHaptics
import com.arjunrana.tokishrine.ui.util.TerminalAction
import com.arjunrana.tokishrine.ui.util.formatCountdown
import com.arjunrana.tokishrine.ui.util.formatEstimate
import com.arjunrana.tokishrine.ui.util.typingEstimateSeconds

// The commitment moment (screen 13). States both costs in plain language
// under a Block Summary heading (PRD §17 R12). Activation is gated on
// accessibility here too — the final confirmation re-checks before
// persisting, so a permission revoked while this screen is open cannot
// slip a block through (PRD §12, Phase 3).
@Composable
fun TurnOnScreen(
    blockId: Long,
    blockRepo: BlockRepository,
    canActivate: () -> Boolean,
    onPermissionsNeeded: () -> Unit,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var block by remember { mutableStateOf<BlockWithContents?>(null) }

    // The accepted commit runs in the activity lifecycle scope and the
    // terminal buttons disable against it, so Back / Not yet can neither
    // cancel an accepted commit nor race a second route pop (review
    // blocker 2).
    val terminal = remember(lifecycleOwner) { TerminalAction(lifecycleOwner.lifecycleScope) }
    BackHandler(enabled = terminal.busy) {
        // A commit is in flight; system Back is absorbed so the single
        // navigation result comes from the commit itself.
    }

    LaunchedEffect(blockId) {
        block = blockRepo.getBlockWithContents(blockId)
    }

    val loaded = block ?: return

    Box(
        Modifier
            .fillMaxSize()
            .background(NocturneTheme.colors.bg)
            .statusBarsPadding(),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 26.dp, vertical = 24.dp),
        ) {
            Spacer(Modifier.weight(1f))
            Box(
                Modifier
                    .size(56.dp)
                    .background(NocturneTheme.colors.accentRamp.step900, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center,
            ) {
                PhosphorIcon(Ph.ScribbleLoop, tint = NocturneTheme.colors.accentRamp.step300, size = 26)
            }
            Spacer(Modifier.height(22.dp))
            Text(
                "Turning on\n${loaded.block.name}",
                fontSize = 25.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 30.sp,
                color = NocturneTheme.colors.text,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "From now on, wasting your time on whatever's in this block won't be easy!",
                fontSize = 13.5.sp,
                lineHeight = 22.sp,
                color = NocturneTheme.colors.neutral.step500,
            )
            Spacer(Modifier.height(24.dp))
            Text(
                "Block Summary:",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = NocturneTheme.colors.neutral.step400,
            )
            Spacer(Modifier.height(14.dp))

            Row {
                PhosphorIcon(
                    if (loaded.block.frictionType == FrictionType.TYPING) Ph.Keyboard else Ph.Hourglass,
                    tint = NocturneTheme.colors.accentRamp.step300,
                    size = 22,
                    modifier = Modifier.padding(top = 2.dp),
                )
                Spacer(Modifier.width(13.dp))
                Column {
                    Text(
                        if (loaded.block.frictionType == FrictionType.TYPING) {
                            "To pause it, you'll type ${loaded.block.pauseChars} characters."
                        } else {
                            "To pause it, you'll wait ${formatCountdown(loaded.block.countdownSeconds)}."
                        },
                        fontSize = 14.5.sp,
                        lineHeight = 20.sp,
                        color = NocturneTheme.colors.text,
                    )
                    Text(
                        if (loaded.block.frictionType == FrictionType.TYPING) {
                            "${formatEstimate(typingEstimateSeconds(loaded.block.pauseChars))}, every time."
                        } else {
                            "Phone in hand, every time."
                        },
                        fontSize = 12.5.sp,
                        lineHeight = 18.sp,
                        color = NocturneTheme.colors.neutral.step500,
                    )
                }
            }
            Spacer(Modifier.height(18.dp))
            Row {
                PhosphorIcon(
                    Ph.LockKeyOpen,
                    tint = NocturneTheme.colors.accentRamp.step300,
                    size = 22,
                    modifier = Modifier.padding(top = 2.dp),
                )
                Spacer(Modifier.width(13.dp))
                Column {
                    Text(
                        "To turn this whole block off, you'll type ${loaded.block.turnoffChars} characters!",
                        fontSize = 14.5.sp,
                        lineHeight = 20.sp,
                        color = NocturneTheme.colors.text,
                    )
                    Text(
                        "${formatEstimate(typingEstimateSeconds(loaded.block.turnoffChars))}.",
                        fontSize = 12.5.sp,
                        lineHeight = 18.sp,
                        color = NocturneTheme.colors.neutral.step500,
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            NocturneButton(
                "Turn it on",
                block = true,
                height = 48.dp,
                fontSize = 15,
                enabled = !terminal.busy,
                onClick = {
                    // The one path that enables a block — both toggle entry
                    // points route here. The final accessibility gate runs
                    // immediately before the transaction; a missing
                    // permission enables nothing and opens the checklist
                    // instead. The commit pairs the state change with its
                    // event in one transaction; the success haptic and the
                    // single close run only after it succeeds.
                    if (!canActivate()) {
                        onPermissionsNeeded()
                        return@NocturneButton
                    }
                    terminal.run(
                        commit = { blockRepo.setEnabledRecordingTransition(blockId, true) },
                        onChanged = { BlockHaptics.turnedOn(context) },
                        onCommitted = onClose,
                    )
                },
            )
            NocturneButton(
                "Not yet",
                variant = ButtonVariant.GHOST,
                block = true,
                enabled = !terminal.busy,
                onClick = onClose,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

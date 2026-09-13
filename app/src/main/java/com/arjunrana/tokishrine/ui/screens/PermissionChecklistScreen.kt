package com.arjunrana.tokishrine.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.arjunrana.tokishrine.data.permissions.AppPermission
import com.arjunrana.tokishrine.data.permissions.PermissionEventLogic
import com.arjunrana.tokishrine.data.repo.EventRepository
import com.arjunrana.tokishrine.ui.components.NocturneAppbar
import com.arjunrana.tokishrine.ui.components.NocturneButton
import com.arjunrana.tokishrine.ui.icons.Ph
import com.arjunrana.tokishrine.ui.icons.PhosphorIcon
import com.arjunrana.tokishrine.ui.navigation.ChecklistMode
import com.arjunrana.tokishrine.ui.theme.NocturneTheme
import com.arjunrana.tokishrine.ui.util.TerminalAction
import kotlinx.coroutines.launch

// Screen 2: exactly four rows, two states each — pending and granted —
// plus a progress bar and "n of 4" count (PRD §6). The displayed state is
// the hoisted system snapshot, refreshed on every app resume. ChecklistMode
// lives in ui.navigation with the routes: it is restored navigation state
// (review blocker 1).
@Composable
fun PermissionChecklistScreen(
    mode: ChecklistMode,
    states: Map<AppPermission, Boolean>,
    permissionEvents: PermissionEventLogic,
    eventRepo: EventRepository,
    onStatesChanged: () -> Unit,
    onRequestPermission: (permission: AppPermission, launchSystemUi: () -> Unit) -> Unit,
    onOpenAccessibilityExplainer: () -> Unit,
    onOpenBatteryInstructions: () -> Unit,
    onBack: () -> Unit,
    onContinue: () -> Unit,
) {
    val colors = NocturneTheme.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    // Onboarding completion is terminal: single-flight, atomic in the
    // repository, and Back is absorbed while the commit is in flight so it
    // cannot race the single navigation result (review blocker 2).
    val terminal = remember(lifecycleOwner) { TerminalAction(lifecycleOwner.lifecycleScope) }
    BackHandler(enabled = terminal.busy) {
        // Completion commit in flight; its own onContinue performs the
        // single navigation.
    }

    // The runtime notification dialog reports its outcome through this
    // callback rather than an app resume, so it settles the request itself.
    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        scope.launch {
            permissionEvents.resolve(AppPermission.NOTIFICATIONS, granted)
            onStatesChanged()
        }
    }

    val grantedCount = AppPermission.values().count { states[it] == true }

    Box(
        Modifier
            .fillMaxSize()
            .background(colors.bg)
            .statusBarsPadding(),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .padding(bottom = 16.dp),
        ) {
            if (mode != ChecklistMode.ONBOARDING) {
                NocturneAppbar(title = "Permissions", onBack = onBack)
            }
            Text(
                "A few permissions",
                fontSize = 23.sp,
                fontWeight = FontWeight.Medium,
                color = colors.text,
                modifier = Modifier.padding(top = 6.dp),
            )
            Text(
                "Toki Shrine can't work without these. Here's exactly what each one is for.",
                fontSize = 13.sp,
                lineHeight = 20.sp,
                color = colors.neutral.step500,
                modifier = Modifier.padding(top = 4.dp, bottom = 14.dp),
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp),
            ) {
                Box(
                    Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(colors.neutral.step800),
                ) {
                    Box(
                        Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(grantedCount / 4f)
                            .background(colors.accent),
                    )
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    "$grantedCount of 4",
                    fontSize = 12.sp,
                    color = colors.neutral.step500,
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                AppPermission.values().forEach { permission ->
                    PermissionRow(
                        permission = permission,
                        granted = states[permission] == true,
                        onRequest = {
                            when (permission) {
                                AppPermission.ACCESSIBILITY -> onOpenAccessibilityExplainer()
                                AppPermission.BATTERY -> onOpenBatteryInstructions()
                                AppPermission.OVERLAY -> onRequestPermission(AppPermission.OVERLAY) {
                                    context.startActivity(
                                        Intent(
                                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                            Uri.parse("package:${context.packageName}"),
                                        ),
                                    )
                                }
                                AppPermission.NOTIFICATIONS -> onRequestPermission(AppPermission.NOTIFICATIONS) {
                                    notificationLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                                }
                            }
                        },
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            NocturneButton(
                "Continue",
                block = true,
                height = 46.dp,
                enabled = !terminal.busy,
                onClick = {
                    // Onboarding mode completes terminally: the §10 event
                    // and the one-way app_meta marker commit atomically and
                    // at most once (EventRepository.completeOnboarding), and
                    // navigation happens only after the commit succeeds.
                    // Gate/settings modes perform a plain pop.
                    terminal.run(
                        commit = {
                            if (mode == ChecklistMode.ONBOARDING) {
                                eventRepo.completeOnboarding(grantedCount)
                            } else {
                                true
                            }
                        },
                        onChanged = {},
                        onCommitted = onContinue,
                    )
                },
            )
        }
    }
}

// One checklist row (.prow): icon tile, title + description, and a state
// badge with exactly two possible looks — Granted pill or Grant outline.
@Composable
private fun PermissionRow(
    permission: AppPermission,
    granted: Boolean,
    onRequest: () -> Unit,
) {
    val colors = NocturneTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .background(colors.surface, RoundedCornerShape(12.dp))
            .padding(13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(36.dp)
                .background(colors.accentRamp.step900, RoundedCornerShape(9.dp)),
            contentAlignment = Alignment.Center,
        ) {
            PhosphorIcon(rowIcon(permission), tint = colors.accentRamp.step300, size = 18)
        }
        Spacer(Modifier.width(13.dp))
        Column(Modifier.weight(1f)) {
            Text(
                permission.rowTitle,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = colors.text,
            )
            Text(
                permission.rowDescription,
                fontSize = 11.5.sp,
                lineHeight = 17.sp,
                color = colors.neutral.step500,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Spacer(Modifier.width(10.dp))
        if (granted) {
            Row(
                Modifier
                    .background(colors.accentRamp.step800, RoundedCornerShape(20.dp))
                    .padding(horizontal = 9.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PhosphorIcon(Ph.Check, tint = colors.accentRamp.step200, size = 11)
                Spacer(Modifier.width(4.dp))
                Text("Granted", fontSize = 11.sp, color = colors.accentRamp.step200)
            }
        } else {
            Text(
                "Grant",
                fontSize = 11.sp,
                color = colors.neutral.step300,
                modifier = Modifier
                    .border(1.dp, colors.neutral.step700, RoundedCornerShape(20.dp))
                    .clickable { onRequest() }
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            )
        }
    }
}

private fun rowIcon(permission: AppPermission): Int = when (permission) {
    AppPermission.ACCESSIBILITY -> Ph.Eye
    AppPermission.OVERLAY -> Ph.SquareHalf
    AppPermission.BATTERY -> Ph.BatteryCharging
    AppPermission.NOTIFICATIONS -> Ph.Bell
}

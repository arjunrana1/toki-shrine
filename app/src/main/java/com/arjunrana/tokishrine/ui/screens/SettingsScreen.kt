package com.arjunrana.tokishrine.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arjunrana.tokishrine.data.permissions.AppPermission
import com.arjunrana.tokishrine.data.repo.EventRepository
import com.arjunrana.tokishrine.ui.components.NocturneAppbar
import com.arjunrana.tokishrine.ui.icons.Ph
import com.arjunrana.tokishrine.ui.icons.PhosphorIcon
import com.arjunrana.tokishrine.ui.theme.NocturneTheme

// Screen 24: permission health · supported browsers · theme · send
// feedback · about and version. The mock's "Countdown messages — updated
// remotely" row is removed (PRD §6). Send feedback opens screen 25 (Phase 7);
// the supported browsers row remains informational (PRD §13 list).
@Composable
fun SettingsScreen(
    states: Map<AppPermission, Boolean>,
    eventRepo: EventRepository,
    onBack: () -> Unit,
    onOpenPermissionHealth: () -> Unit,
    onOpenFeedback: () -> Unit,
) {
    val colors = NocturneTheme.colors

    LaunchedEffect(Unit) {
        eventRepo.log(EventRepository.EVENT_SETTINGS_VIEWED)
    }

    val grantedCount = AppPermission.values().count { states[it] == true }
    val versionName = rememberVersionName()

    Box(
        Modifier
            .fillMaxSize()
            .background(colors.bg)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .padding(bottom = 16.dp),
        ) {
            NocturneAppbar(title = "Settings", onBack = onBack)
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(colors.surface, RoundedCornerShape(12.dp)),
            ) {
                SettingsRow(
                    icon = { PhosphorIcon(Ph.Heartbeat, tint = colors.accentRamp.step300, size = 19) },
                    title = "Permission health",
                    subtitle = if (grantedCount == 4) "All 4 granted" else "$grantedCount of 4 granted",
                    onClick = onOpenPermissionHealth,
                    trailing = {
                        if (grantedCount == 4) {
                            Box(
                                Modifier
                                    .border(1.dp, colors.neutral.step700, RoundedCornerShape(20.dp))
                                    .padding(horizontal = 9.dp, vertical = 4.dp),
                            ) {
                                PhosphorIcon(Ph.Check, tint = colors.accentRamp.step200, size = 11)
                            }
                        }
                    },
                )
                RowDivider()
                SettingsRow(
                    icon = { PhosphorIcon(Ph.Globe, tint = colors.neutral.step400, size = 19) },
                    title = "Supported browsers",
                    subtitle = "Chrome, Firefox, Samsung Internet + 6 more",
                    trailing = { PhosphorIcon(Ph.CaretRight, tint = colors.neutral.step500, size = 17) },
                )
            }
            Spacer(Modifier.height(14.dp))
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(colors.surface, RoundedCornerShape(12.dp)),
            ) {
                SettingsRow(
                    icon = { PhosphorIcon(Ph.Moon, tint = colors.neutral.step400, size = 19) },
                    title = "Theme",
                    value = "Dark",
                )
                RowDivider()
                SettingsRow(
                    icon = { PhosphorIcon(Ph.PaperPlaneTilt, tint = colors.neutral.step400, size = 19) },
                    title = "Send feedback",
                    onClick = onOpenFeedback,
                    trailing = { PhosphorIcon(Ph.CaretRight, tint = colors.neutral.step500, size = 17) },
                )
                RowDivider()
                SettingsRow(
                    icon = { PhosphorIcon(Ph.Info, tint = colors.neutral.step400, size = 19) },
                    title = "About Toki Shrine",
                    value = versionName,
                )
            }
        }
    }
}

@Composable
private fun SettingsRow(
    icon: @Composable () -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    value: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val colors = NocturneTheme.colors
    Row(
        modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon()
        Spacer(Modifier.width(13.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 14.sp, color = colors.text)
            if (subtitle != null) {
                Text(
                    subtitle,
                    fontSize = 11.5.sp,
                    color = colors.neutral.step500,
                    modifier = Modifier.padding(top = 1.dp),
                )
            }
        }
        if (value != null) {
            Text(value, fontSize = if (subtitle == null) 13.sp else 12.sp, color = colors.neutral.step500)
        }
        if (trailing != null) trailing()
    }
}

@Composable
private fun RowDivider() {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(start = 46.dp)
            .height(1.dp)
            .background(NocturneTheme.colors.divider),
    )
}

// The mock's static "v0.1" is replaced by the real build version, read
// from the package manager.
@Composable
private fun rememberVersionName(): String {
    val context = LocalContext.current
    return remember {
        runCatching {
            "v" + (context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "")
        }.getOrDefault("v0")
    }
}

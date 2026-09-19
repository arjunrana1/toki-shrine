package com.arjunrana.tokishrine.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arjunrana.tokishrine.data.permissions.BatteryOem
import com.arjunrana.tokishrine.data.permissions.BatteryStepPart
import com.arjunrana.tokishrine.data.permissions.OemBattery
import com.arjunrana.tokishrine.ui.components.NocturneAppbar
import com.arjunrana.tokishrine.ui.components.NocturneButton
import com.arjunrana.tokishrine.ui.theme.NocturneTheme

// Screen 4: battery guidance. Manufacturer handling is automatic and
// invisible to the user (owner correction, 19 September): Samsung hardware
// gets the authored One UI steps, every other device gets the universal
// Android battery-optimisation dialog, and the former "Not a Samsung?
// Pick your phone" override picker is removed — the intro speaks about
// Android, not Samsung. There is no text field on this screen, so the One
// UI keyboard+dialog freeze guidance (DECISIONS.md, Phase 2) does not
// apply — still flagged for device validation like every dialog. The
// earlier completion CTA was also removed by owner request (P3-F05).
@Composable
fun BatteryInstructionsScreen(
    detectedManufacturer: String?,
    onBack: () -> Unit,
    onOpenBatterySettings: (BatteryOem) -> Unit,
) {
    val colors = NocturneTheme.colors
    // Detection is derived, never user-set, so nothing survives—or needs
    // to survive—activity recreation beyond the manufacturer input itself.
    val oem = remember(detectedManufacturer) { OemBattery.detect(detectedManufacturer) }
    val instructions = OemBattery.instructionsFor(oem)

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
            NocturneAppbar(title = "Keep us awake", onBack = onBack)
            Text(
                instructions.intro,
                fontSize = 13.5.sp,
                lineHeight = 22.sp,
                color = colors.neutral.step500,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
            )
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                instructions.steps.forEachIndexed { index, step ->
                    Row {
                        Box(
                            Modifier
                                .size(24.dp)
                                .background(colors.accentRamp.step900, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                "${index + 1}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.accentRamp.step200,
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(
                            stepText(step),
                            fontSize = 13.5.sp,
                            lineHeight = 20.sp,
                            color = colors.text,
                        )
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            NocturneButton(
                "Open battery settings",
                block = true,
                height = 46.dp,
                onClick = { onOpenBatterySettings(oem) },
            )
        }
    }
}

private fun stepText(step: List<BatteryStepPart>) = buildAnnotatedString {
    step.forEach { part ->
        if (part.bold) {
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(part.text) }
        } else {
            append(part.text)
        }
    }
}

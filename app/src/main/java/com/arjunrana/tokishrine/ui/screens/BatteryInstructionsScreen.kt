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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.arjunrana.tokishrine.data.permissions.BatteryOem
import com.arjunrana.tokishrine.data.permissions.BatteryStepPart
import com.arjunrana.tokishrine.data.permissions.OemBattery
import com.arjunrana.tokishrine.ui.components.NocturneAppbar
import com.arjunrana.tokishrine.ui.components.NocturneButton
import com.arjunrana.tokishrine.ui.theme.NocturneTheme

// Screen 4: manufacturer battery guidance. Detection defaults to Samsung
// on Samsung hardware; "Not a Samsung? Pick your phone" opens the override
// picker. There is no text field on this screen, so the One UI
// keyboard+dialog freeze guidance (DECISIONS.md, Phase 2) does not apply —
// still flagged for device validation like every dialog. The completion
// CTA was removed by owner request (P3-F05, 19 September); Back and
// returning from battery settings both settle state on the checklist.
@Composable
fun BatteryInstructionsScreen(
    detectedManufacturer: String?,
    onBack: () -> Unit,
    onOpenBatterySettings: (BatteryOem) -> Unit,
) {
    val colors = NocturneTheme.colors
    val detected = remember(detectedManufacturer) { OemBattery.detect(detectedManufacturer) }
    // The override survives activity recreation while this screen is
    // active (review blocker 1); enum values are Serializable, so the
    // default saver handles them.
    var selected by rememberSaveable { mutableStateOf(detected) }
    var pickerOpen by remember { mutableStateOf(false) }
    val instructions = OemBattery.instructionsFor(selected)

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
                onClick = { onOpenBatterySettings(selected) },
            )
            Text(
                buildAnnotatedString {
                    append("Not a Samsung? ")
                    withStyle(SpanStyle(color = colors.accentRamp.step300)) { append("Pick your phone") }
                },
                fontSize = 11.5.sp,
                color = colors.neutral.step500,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { pickerOpen = true }
                    .padding(top = 10.dp),
            )
        }

        if (pickerOpen) {
            Dialog(onDismissRequest = { pickerOpen = false }) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(colors.surface, RoundedCornerShape(14.dp))
                        .padding(20.dp),
                ) {
                    Text(
                        "Pick your phone",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.text,
                    )
                    Spacer(Modifier.height(18.dp))
                    BatteryOem.values().forEach { oem ->
                        Text(
                            oem.pickerLabel,
                            fontSize = 14.sp,
                            color = if (oem == selected) colors.accent else colors.text,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selected = oem
                                    pickerOpen = false
                                }
                                .padding(vertical = 10.dp),
                        )
                    }
                }
            }
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

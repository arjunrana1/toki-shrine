package com.arjunrana.tokishrine.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arjunrana.tokishrine.ui.icons.Ph
import com.arjunrana.tokishrine.ui.icons.PhosphorIcon
import com.arjunrana.tokishrine.ui.theme.NocturneTheme

/*
 * Nocturne components translated from the mock's CSS classes (.btn, .tgl,
 * .stepper, .chip, .appbar, .dots, .seg, .est, .brow …). Outlined buttons are
 * never filled; disabled controls drop to 45% opacity (readme rules).
 */

// — buttons (.btn / .btn-primary / .btn-secondary / .btn-ghost) —

enum class ButtonVariant { PRIMARY, SECONDARY, GHOST }

@Composable
fun NocturneButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: ButtonVariant = ButtonVariant.PRIMARY,
    enabled: Boolean = true,
    block: Boolean = false,
    height: Dp = 44.dp,
    fontSize: Int = 14,
    paddingHorizontal: Dp = (8.4.dp * 1.2f),
    leading: (@Composable () -> Unit)? = null,
) {
    val colors = NocturneTheme.colors
    val textColor = when (variant) {
        ButtonVariant.PRIMARY, ButtonVariant.GHOST -> colors.accent
        ButtonVariant.SECONDARY -> colors.text
    }
    val contentColor = if (enabled) textColor else textColor.copy(alpha = 0.45f)
    val borderColor: Color? = when (variant) {
        ButtonVariant.PRIMARY -> colors.accent
        ButtonVariant.SECONDARY -> colors.divider
        ButtonVariant.GHOST -> null
    }?.let { if (enabled) it else it.copy(alpha = 0.45f) }
    val horizontalPadding = when (variant) {
        ButtonVariant.GHOST -> NocturneTheme.spacing.s1
        else -> if (block) 0.dp else paddingHorizontal
    }
    val buttonShape = MaterialTheme.shapes.medium

    Row(
        modifier = modifier
            .then(if (block) Modifier.fillMaxWidth() else Modifier)
            .height(height)
            .border(
                BorderStroke(1.dp, borderColor ?: Color.Transparent),
                buttonShape,
            )
            .background(Color.Transparent, buttonShape)
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = horizontalPadding),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null) {
            leading()
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = text,
            color = contentColor,
            style = TextStyle(
                fontFamily = MaterialTheme.typography.bodyLarge.fontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = fontSize.sp,
                lineHeight = (fontSize * 1.2f).sp,
            ),
        )
    }
}

// — switch (.tgl / .k) —

@Composable
fun NocturneSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = NocturneTheme.colors
    val track = if (checked) colors.accent else colors.neutral.step800
    val thumb = if (checked) colors.bg else colors.neutral.step500
    // PRD §17 R13: the thumb stays visibly inside the track in both states —
    // 3.dp inset from the ends, vertically centered — instead of running
    // flush against the track edge when ON (the mock's 21dp/2dp geometry).
    // Track dimensions are unchanged (.tgl: 42×25, radius 13).
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) (42.dp - 3.dp - 17.dp - 3.dp) else 0.dp,
        animationSpec = tween(durationMillis = 120),
        label = "switchThumb",
    )
    Box(
        modifier = modifier
            .size(width = 42.dp, height = 25.dp)
            .background(track, RoundedCornerShape(13.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 3.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            Modifier
                .offset(x = thumbOffset)
                .size(17.dp)
                .background(thumb, CircleShape),
        )
    }
}

// — stepper (.stepper) —

@Composable
fun NocturneStepper(
    value: String,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    modifier: Modifier = Modifier,
    canDecrement: Boolean = true,
    canIncrement: Boolean = true,
) {
    val colors = NocturneTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.surface, RoundedCornerShape(10.dp))
            .border(1.dp, colors.divider, RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StepperButton(Ph.Minus, canDecrement, onDecrement)
        Text(
            text = value,
            fontSize = 14.sp,
            color = colors.text,
        )
        StepperButton(Ph.Plus, canIncrement, onIncrement)
    }
}

@Composable
private fun StepperButton(glyph: Int, enabled: Boolean, onClick: () -> Unit) {
    val colors = NocturneTheme.colors
    Box(
        modifier = Modifier
            .size(30.dp)
            .border(1.dp, colors.neutral.step700, RoundedCornerShape(8.dp))
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        PhosphorIcon(
            glyph,
            tint = if (enabled) colors.neutral.step200 else colors.neutral.step700,
            size = 15,
        )
    }
}

// — chip (.chip) —

@Composable
fun NocturneChip(text: String, modifier: Modifier = Modifier) {
    val colors = NocturneTheme.colors
    Box(
        modifier = modifier
            .background(colors.neutral.step900, RoundedCornerShape(8.dp))
            .padding(horizontal = 11.dp, vertical = 6.dp),
    ) {
        Text(text = text, fontSize = 12.5.sp, color = colors.neutral.step200)
    }
}

// — section label (11px uppercase, letter-spacing .08em) —

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier, trailing: (@Composable () -> Unit)? = null) {
    val colors = NocturneTheme.colors
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = text,
            fontSize = 11.sp,
            letterSpacing = 0.88.sp,
            color = colors.neutral.step500,
            modifier = Modifier.weight(1f),
        )
        if (trailing != null) trailing()
    }
}

// — appbar (.appbar) —

@Composable
fun NocturneAppbar(
    title: String,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
    stepLabel: String? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val colors = NocturneTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            PhosphorIcon(
                Ph.ArrowLeft,
                tint = colors.neutral.step300,
                modifier = Modifier.clickable { onBack() },
            )
            Spacer(Modifier.width(12.dp))
        }
        Text(
            text = title,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = colors.text,
            modifier = Modifier.weight(1f),
        )
        if (stepLabel != null) {
            Text(
                text = stepLabel,
                fontSize = 12.sp,
                color = colors.neutral.step500,
            )
        }
        if (trailing != null) trailing()
    }
}

// — create-flow progress dots (.dots) —

@Composable
fun ProgressDots(total: Int, current: Int, modifier: Modifier = Modifier) {
    val colors = NocturneTheme.colors
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(total) { index ->
            Box(
                Modifier
                    .size(width = 22.dp, height = 3.dp)
                    .background(
                        if (index < current) colors.accent else colors.neutral.step800,
                        RoundedCornerShape(2.dp),
                    ),
            )
        }
    }
}

// — text field (.input) —

@Composable
fun NocturneTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    hint: String = "",
    fontSize: Int = 14,
    minHeight: Dp = 36.dp,
    singleLine: Boolean = false,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    readOnly: Boolean = false,
) {
    val colors = NocturneTheme.colors
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        readOnly = readOnly,
        singleLine = singleLine,
        textStyle = TextStyle(
            fontFamily = MaterialTheme.typography.bodyLarge.fontFamily,
            fontSize = fontSize.sp,
            color = colors.text,
        ),
        cursorBrush = SolidColor(colors.accent),
        modifier = modifier.fillMaxWidth(),
        decorationBox = { inner ->
            Row(
                Modifier
                    .defaultMinSize(minHeight = minHeight)
                    .background(colors.surface, RoundedCornerShape(10.dp))
                    .border(1.dp, colors.divider, RoundedCornerShape(10.dp))
                    .padding(horizontal = 11.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (leading != null) leading()
                Box(Modifier.weight(1f)) {
                    if (value.isEmpty() && hint.isNotEmpty()) {
                        Text(
                            text = hint,
                            fontSize = fontSize.sp,
                            color = colors.neutral.step500,
                        )
                    }
                    inner()
                }
                if (trailing != null) trailing()
            }
        },
    )
}

// — segmented control (.seg / .seg-opt) —

@Composable
fun NocturneSegmented(
    options: List<Pair<Int, String>>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = NocturneTheme.colors
    Row(
        modifier = modifier
            .background(colors.surface, RoundedCornerShape(10.dp))
            .border(1.dp, colors.divider, RoundedCornerShape(10.dp))
            .padding(3.dp),
    ) {
        options.forEachIndexed { index, (glyph, label) ->
            val selected = index == selectedIndex
            Row(
                modifier = Modifier
                    .weight(1f)
                    .background(
                        if (selected) colors.accent.copy(alpha = 0.10f) else Color.Transparent,
                        RoundedCornerShape(8.dp),
                    )
                    .border(
                        BorderStroke(1.dp, if (selected) colors.accent else Color.Transparent),
                        RoundedCornerShape(8.dp),
                    )
                    .clickable { onSelect(index) }
                    .padding(vertical = 7.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PhosphorIcon(
                    glyph,
                    tint = if (selected) colors.accent else colors.neutral.step300,
                    size = 14,
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = label,
                    fontSize = 13.sp,
                    color = if (selected) colors.accent else colors.text,
                )
            }
        }
    }
}

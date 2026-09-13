package com.arjunrana.tokishrine.ui.icons

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import com.arjunrana.tokishrine.R

/*
 * Phosphor glyph rendering: the design language mandates Phosphor icons
 * (nocturne readme). The regular-weight TTF is bundled and glyphs render by
 * codepoint, so no icon dependency is added. Codepoints from
 * @phosphor-icons/web 2.1.1 regular style.css.
 */

internal val PhosphorFontFamily = FontFamily(Font(R.font.phosphor_regular))

object Ph {
    const val ArrowLeft = 0xe058
    const val BatteryCharging = 0xe0ba
    const val Bell = 0xe0ce
    const val CaretLeft = 0xe138
    const val CaretRight = 0xe13a
    const val ChartBar = 0xe150
    const val ChatCircleText = 0xe16e
    const val Check = 0xe182
    const val CheckSquare = 0xe186
    const val ClockCountdown = 0xed2c
    const val Eye = 0xe220
    const val GearSix = 0xe272
    const val Globe = 0xe288
    const val Heartbeat = 0xe2ac
    const val Hourglass = 0xe2b2
    const val HourglassMedium = 0xe2b8
    const val Info = 0xe2ce
    const val Keyboard = 0xe2d8
    const val LockKeyOpen = 0xe300
    const val MagnifyingGlass = 0xe30c
    const val Minus = 0xe32a
    const val Moon = 0xe330
    const val PaperPlaneTilt = 0xe398
    const val Plus = 0xe3d4
    const val PlusCircle = 0xe3d6
    const val ScribbleLoop = 0xe662
    const val Square = 0xe45e
    const val SquareHalf = 0xe462
    const val SquaresFour = 0xe464
    const val Stack = 0xe466
    const val Timer = 0xe492
    const val Trash = 0xe4a6
    const val X = 0xe4f6
    const val XCircle = 0xe4f8
}

/**
 * A Phosphor glyph rendered as text in the bundled icon font. Glyphs are
 * decorative; the accessible name comes from the control that contains them.
 */
@Composable
fun PhosphorIcon(
    glyph: Int,
    modifier: Modifier = Modifier,
    tint: Color = Color.Unspecified,
    size: Int = 20,
) {
    Text(
        text = String(Character.toChars(glyph)),
        fontFamily = PhosphorFontFamily,
        style = MaterialTheme.typography.bodyLarge.copy(fontSize = size.sp),
        color = tint,
        modifier = modifier,
    )
}

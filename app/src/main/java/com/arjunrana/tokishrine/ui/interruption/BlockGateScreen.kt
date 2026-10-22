package com.arjunrana.tokishrine.ui.interruption

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arjunrana.tokishrine.config.AppConfig
import com.arjunrana.tokishrine.ui.theme.NocturneTheme
import kotlinx.coroutines.delay

// Full-bleed photo dim (PRD §11): the Nocturne foreground stays legible and
// the humour photo still reads as an image. Owner correction, 23 September:
// 0.78 dimmed the photos into a near-black void on device; 0.65 lets the
// subject through while headline and actions keep their contrast.
private const val GATE_SCRIM_ALPHA = 0.65f

// 14 v2 (11 October §17 unlock-minutes addendum). The buttons sit 76 dp higher
// than before (bottom padding 24 → 100 dp) so the "Minutes to unlock" row can
// drop into the space under them, and the clear meme layer rises by the same
// amount so it stays fully visible above the buttons.
private val GATE_BUTTON_LIFT = 76.dp

// Under the challenge button: 8 dp gap, label line, 6 dp gap, 36 dp chips = 64 dp,
// then 36 dp to the bottom edge. The row is always composed and always takes
// this height, open or closed, so the buttons above it never move. 64 + 36 is
// the 100 dp of the handoff.
private val GATE_ROW_TOP_GAP = 8.dp
private val GATE_ROW_LABEL_LINE = 14.sp
private val GATE_ROW_INNER_GAP = 6.dp
private val GATE_CHIP_HEIGHT = 36.dp
private val GATE_BOTTOM_SPACE = 36.dp

// Motion, handoff §1. CSS `ease` is cubic-bezier(.25,.1,.25,1). The chips' y
// control point above 1 is the overshoot that makes them drop and settle.
private val GateEase = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f)
private val GateLabelSlideEasing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)
private val GateChipDropEasing = CubicBezierEasing(0.2f, 0.9f, 0.25f, 1.15f)
private const val GATE_LABEL_FADE_MS = 250
private const val GATE_LABEL_SLIDE_MS = 350
private const val GATE_CHIP_FADE_MS = 260
private const val GATE_CHIP_DROP_MS = 420
private const val GATE_SELECTION_MS = 150
private val GATE_LABEL_SLIDE_FROM = 10.dp
private val GATE_CHIP_DROP_FROM = 18.dp

// How long a tapped chip shows its selected colours before the challenge
// starts: the 0.15 s colour change plus a short beat to read it.
internal const val GATE_SELECTION_HOLD_MS = 200L

private const val GATE_ROW_LABEL = "Minutes to unlock"

/** Delay before a chip starts moving: left to right on open, reversed on close. */
internal fun gateChipDelayMs(index: Int, chipCount: Int, opening: Boolean): Int =
    if (opening) 80 + 60 * index else (chipCount - 1 - index) * 30

/**
 * State of the gate's "Minutes to unlock" row. The way-in button toggles it and
 * clears any pick; while the row is open exactly one chip tap is accepted.
 * Only [open] is saved, so rotation keeps the row open but a half-made pick is
 * dropped.
 */
internal class UnlockRowState(open: Boolean = false) {
    var open by mutableStateOf(open)
        private set
    var picked by mutableStateOf<Int?>(null)
        private set

    fun toggle() {
        open = !open
        picked = null
    }

    /** True when this tap is the one that starts the challenge. */
    fun pick(minutes: Int): Boolean {
        if (!open || picked != null) return false
        picked = minutes
        return true
    }

    companion object {
        val Saver: Saver<UnlockRowState, Boolean> = Saver(
            save = { it.open },
            restore = { UnlockRowState(it) },
        )
    }
}

/*
 * The real block gate (PRD §6 screen 15, §11). Stateless: the caller supplies
 * the block/target names, one humour line and the background drawable —
 * random selection of either is the runtime task's job. The walk-away is the
 * filled prominent action and the way in the quieter outlined one; the screen
 * is comprehensible without scrolling. Buttons only report; BlockActivity
 * integration, walk_away events and challenge routing belong to TS-P5B.
 *
 * The way-in opens a "Minutes to unlock" row (11 October §17 unlock-minutes
 * addendum, 14 v2); a chip shows its selected state briefly and then reports
 * its minutes once through [onEnterChallenge].
 */
@Composable
fun BlockGateScreen(
    blockName: String,
    headline: String,
    humourLine: String,
    backgroundRes: Int,
    onWalkAway: () -> Unit,
    onEnterChallenge: (minutes: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val unlockRow = rememberSaveable(saver = UnlockRowState.Saver) { UnlockRowState() }
    val picked = unlockRow.picked
    val currentOnEnterChallenge by rememberUpdatedState(onEnterChallenge)
    // The brief selected state, then straight into the challenge. Folding the
    // row clears the pick, which cancels this before it reports.
    LaunchedEffect(picked) {
        if (picked != null) {
            delay(GATE_SELECTION_HOLD_MS)
            currentOnEnterChallenge(picked)
        }
    }
    Box(modifier.fillMaxSize()) {
        Image(
            painter = painterResource(backgroundRes),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        // Repeat the same asset without distortion so its complete subject is
        // always visible. The cropped copy behind it supplies the uncovered
        // top/bottom ground on tall screens. This box ends two lifts above the
        // bottom, which centres the picture one lift higher, in step with the
        // buttons; a picture too tall for that scales down rather than being
        // cut off.
        Image(
            painter = painterResource(backgroundRes),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            alignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = GATE_BUTTON_LIFT * 2),
        )
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = GATE_SCRIM_ALPHA)))
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(start = 28.dp, end = 28.dp, top = 24.dp, bottom = GATE_BOTTOM_SPACE),
        ) {
            Text(
                text = blockName.uppercase(),
                fontSize = 12.sp,
                letterSpacing = 1.2.sp,
                color = NocturneTheme.colors.neutral.step600,
                modifier = Modifier.padding(top = 8.dp),
            )
            // Owner-tested placement: the headline cluster sits in the top
            // segment under the block name — generous 20–25px pad above the
            // heading, centered — leaving the photo subject clear mid-screen.
            Text(
                text = headline,
                fontFamily = MaterialTheme.typography.headlineMedium.fontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 30.sp,
                lineHeight = 37.sp,
                textAlign = TextAlign.Center,
                color = NocturneTheme.colors.text,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 22.dp),
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = humourLine,
                fontSize = 14.sp,
                lineHeight = 22.sp,
                textAlign = TextAlign.Center,
                color = NocturneTheme.colors.neutral.step400,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.weight(1f))
            GateFilledButton(
                text = "Nope, not now", // owner, 10 October: matches the challenge escape
                height = 58,
                fontSize = 17.5f,
                onClick = onWalkAway,
            )
            GateOutlinedButton(
                text = "I'll do the challenge. Let me in 🚩🤨",
                height = 50,
                fontSize = 14.5f,
                onClick = unlockRow::toggle,
                modifier = Modifier.padding(top = 12.dp),
            )
            UnlockMinutesRow(
                open = unlockRow.open,
                picked = picked,
                onPick = { unlockRow.pick(it) },
            )
        }
    }
}

// The gate's two actions are screen-specific treatments, not Nocturne .btn
// variants: the reference fills the walk-away with accent-400 over the photo
// ground and sets the way-in as translucent glass. The near-white/near-black
// values are the screen-15 reference's explicit exceptions to the no-pure-
// black/white rule, expressed from Color constants rather than new hex tokens.
// The "Minutes to unlock" chips below use the same glass literals.

@Composable
private fun GateFilledButton(
    text: String,
    height: Int,
    fontSize: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = NocturneTheme.colors
    val shape = RoundedCornerShape(8.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(height.dp)
            // CSS 0 6px 22px accent-45% glow, halved per the elevation
            // convention; Compose only tints the shadow, it cannot match the
            // exact blur (owner visual pass pending).
            .shadow(
                elevation = 11.dp,
                shape = shape,
                ambientColor = colors.accent.copy(alpha = 0.45f),
                spotColor = colors.accent.copy(alpha = 0.45f),
            )
            .background(colors.accentRamp.step400, shape)
            .clickable { onClick() },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            color = Color.Black.copy(alpha = 0.96f),
            fontWeight = FontWeight.Medium,
            fontSize = fontSize.sp,
            lineHeight = (fontSize * 1.2f).sp,
        )
    }
}

@Composable
private fun GateOutlinedButton(
    text: String,
    height: Int,
    fontSize: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(8.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(height.dp)
            .background(Color.Black.copy(alpha = 0.72f), shape)
            .border(1.5.dp, Color.White.copy(alpha = 0.5f), shape)
            .clickable { onClick() },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            color = Color.White,
            fontWeight = FontWeight.Medium,
            fontSize = fontSize.sp,
            lineHeight = (fontSize * 1.2f).sp,
        )
    }
}

/**
 * The label and the 4-up chip row under the way-in button (14 v2). Always
 * composed, so it takes the same height open or closed and the fold-away can
 * play; while closed it is invisible, not clickable and hidden from
 * accessibility.
 */
@Composable
private fun UnlockMinutesRow(
    open: Boolean,
    picked: Int?,
    onPick: (Int) -> Unit,
) {
    val labelFade by animateFloatAsState(
        targetValue = if (open) 1f else 0f,
        animationSpec = tween(GATE_LABEL_FADE_MS, easing = GateEase),
        label = "unlockLabelFade",
    )
    val labelSlide by animateFloatAsState(
        targetValue = if (open) 1f else 0f,
        animationSpec = tween(GATE_LABEL_SLIDE_MS, easing = GateLabelSlideEasing),
        label = "unlockLabelSlide",
    )
    val choices = AppConfig.UNLOCK_MINUTE_CHOICES
    Column(
        Modifier
            .padding(top = GATE_ROW_TOP_GAP)
            .then(if (open) Modifier else Modifier.clearAndSetSemantics { }),
        verticalArrangement = Arrangement.spacedBy(GATE_ROW_INNER_GAP),
    ) {
        Text(
            text = GATE_ROW_LABEL.uppercase(),
            fontSize = 11.sp,
            lineHeight = GATE_ROW_LABEL_LINE,
            letterSpacing = 0.88.sp, // .08em
            color = NocturneTheme.colors.neutral.step400,
            modifier = Modifier
                .graphicsLayer {
                    alpha = labelFade.coerceIn(0f, 1f)
                    translationY = (labelSlide - 1f) * GATE_LABEL_SLIDE_FROM.toPx()
                }
                // Shown in capitals, spoken and matched in sentence case.
                .clearAndSetSemantics { text = AnnotatedString(GATE_ROW_LABEL) },
        )
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            choices.forEachIndexed { index, minutes ->
                UnlockChip(
                    minutes = minutes,
                    delayMs = gateChipDelayMs(index, choices.size, opening = open),
                    open = open,
                    selected = picked == minutes,
                    interactive = open && picked == null,
                    onClick = { onPick(minutes) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun UnlockChip(
    minutes: Int,
    delayMs: Int,
    open: Boolean,
    selected: Boolean,
    interactive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = NocturneTheme.colors
    val shape = RoundedCornerShape(10.dp)
    val fade by animateFloatAsState(
        targetValue = if (open) 1f else 0f,
        animationSpec = tween(GATE_CHIP_FADE_MS, delayMillis = delayMs, easing = GateEase),
        label = "unlockChipFade",
    )
    // 0 → 1 is -18 dp → 0; the easing overshoots 1, which drops the chip
    // slightly past its resting place before it settles.
    val drop by animateFloatAsState(
        targetValue = if (open) 1f else 0f,
        animationSpec = tween(GATE_CHIP_DROP_MS, delayMillis = delayMs, easing = GateChipDropEasing),
        label = "unlockChipDrop",
    )
    val selectionSpec = tween<Color>(GATE_SELECTION_MS)
    val background by animateColorAsState(
        targetValue = if (selected) colors.accentRamp.step900 else Color.Black.copy(alpha = 0.72f),
        animationSpec = selectionSpec,
        label = "unlockChipBackground",
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) colors.accentRamp.step400 else Color.White.copy(alpha = 0.3f),
        animationSpec = selectionSpec,
        label = "unlockChipBorder",
    )
    val textColor by animateColorAsState(
        targetValue = if (selected) colors.accentRamp.step100 else colors.neutral.step100,
        animationSpec = selectionSpec,
        label = "unlockChipText",
    )
    val borderWidth by animateDpAsState(
        targetValue = if (selected) 1.5.dp else 1.dp,
        animationSpec = tween(GATE_SELECTION_MS),
        label = "unlockChipBorderWidth",
    )
    Box(
        modifier = modifier
            .graphicsLayer {
                alpha = fade.coerceIn(0f, 1f)
                translationY = (drop - 1f) * GATE_CHIP_DROP_FROM.toPx()
            }
            .height(GATE_CHIP_HEIGHT)
            .background(background, shape)
            .border(borderWidth, borderColor, shape)
            .clickable(enabled = interactive, role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) {
                contentDescription = "$minutes minutes"
                this.selected = selected
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = minutes.toString(),
            color = textColor,
            fontWeight = FontWeight.Medium,
            fontSize = 15.sp,
            style = TextStyle(fontFeatureSettings = "tnum"),
        )
    }
}

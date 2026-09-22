package com.arjunrana.tokishrine.ui.interruption

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.platform.TextToolbarStatus
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arjunrana.tokishrine.ui.icons.Ph
import com.arjunrana.tokishrine.ui.icons.PhosphorIcon
import com.arjunrana.tokishrine.ui.theme.NocturneTheme

// @phosphor-icons/web 2.1.1 regular codepoints for glyphs the interruption
// hints need beyond the shared Ph table (verified against the same release
// Ph.Eye and Ph.Check come from; Phosphor.kt stays untouched per task bounds).
private const val PhClipboard = 0xe196
private const val PhMagicWand = 0xe6b6
private const val PhArrowsCounterClockwise = 0xe096
private const val PhLockKey = 0xe2fe

// PRD §7.1 hard requirements: the input offers no paste entry point and no
// autocorrect/predictive help. The toolbar shows no long-press menu at all
// (the acceptance check is "no Paste option"), and any insertion larger than
// one keystroke is dropped by acceptTypingEdit even if a keyboard-side
// clipboard path tries.
private object NoMenuTextToolbar : TextToolbar {
    override val status: TextToolbarStatus
        get() = TextToolbarStatus.Hidden

    override fun showMenu(
        rect: Rect,
        onCopyRequested: (() -> Unit)?,
        onPasteRequested: (() -> Unit)?,
        onCutRequested: (() -> Unit)?,
        onSelectAllRequested: (() -> Unit)?,
    ) {}

    override fun hide() {}
}

/**
 * Styles the typed text with per-character mismatch marks after a failed
 * explicit Submit. Editing offsets are identity:
 * the transformation only recolors, so cursor/selection semantics are intact.
 * The mock's #8B0000 underline is approximated with the approved error
 * family's container pairing — a token-pure mark, owner visual pass pending.
 */
private class MismatchTransformation(
    private val passage: String,
    private val markBackground: Color,
    private val markText: Color,
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val builder = AnnotatedString.Builder(text)
        mismatchSpans(passage, text.text).forEach { span ->
            builder.addStyle(
                SpanStyle(background = markBackground, color = markText),
                span.start,
                span.start + span.length,
            )
        }
        return TransformedText(builder.toAnnotatedString(), OffsetMapping.Identity)
    }
}

/*
 * The typing challenge (PRD §6 screens 17–18, and 22's turn-off variant —
 * one surface, copy chosen by [purpose]). Fully hoisted: the passage and
 * typed text are supplied, every user action is an explicit callback, and
 * the screen never clears or regenerates anything. [onSubmit] reports the
 * current text; completion and duplicate suppression are TS-P5B's. The
 * visible button is the only submit path; the escape action is the header's
 * ghost control.
 */
@Composable
fun TypingChallengeScreen(
    purpose: ChallengePurpose,
    blockName: String,
    passage: String,
    typedText: String,
    showTypingMismatches: Boolean,
    onTypedTextChanged: (String) -> Unit,
    onSubmit: (String) -> Unit,
    onWalkAway: () -> Unit,
    modifier: Modifier = Modifier,
    turnOffChars: Int = 0,
) {
    val colors = NocturneTheme.colors
    val mismatches = if (showTypingMismatches) mismatchSpans(passage, typedText) else emptyList()
    val hasMismatch = mismatches.isNotEmpty()
    val submitEnabled = canSubmitTyping(typedText.length, passage.length)

    Column(
        modifier
            .fillMaxSize()
            .background(colors.bg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            // Long disable passages (700 chars) must stay reachable above
            // the IME; the reference's fixed-height keyboard art is not a
            // real constraint.
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 6.dp),
    ) {
        Row(
            Modifier.padding(top = 2.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = challengeTitle(purpose, blockName).uppercase(),
                fontSize = 12.sp,
                letterSpacing = 0.96.sp,
                color = colors.neutral.step600,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = typingEscapeLabel(purpose),
                fontSize = 13.sp,
                color = colors.neutral.step300,
                modifier = Modifier
                    .clickable { onWalkAway() }
                    .padding(vertical = 2.dp),
            )
        }
        if (purpose == ChallengePurpose.TURN_OFF) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(colors.neutral.step900, RoundedCornerShape(10.dp))
                    .padding(horizontal = 13.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                PhosphorIcon(
                    PhLockKey,
                    tint = colors.accentRamp.step300,
                    size = 17,
                    modifier = Modifier.padding(top = 1.dp),
                )
                Text(
                    text = "Type in $turnOffChars characters to turn this block off.",
                    fontSize = 12.5.sp,
                    lineHeight = 19.sp,
                    color = colors.neutral.step300,
                )
            }
            Spacer(Modifier.height(14.dp))
        }
        Box(
            Modifier
                .fillMaxWidth()
                .background(colors.neutral.step900, RoundedCornerShape(12.dp))
                .padding(15.dp),
        ) {
            Text(
                text = passage,
                fontSize = 15.sp,
                lineHeight = 25.5.sp,
                color = colors.neutral.step400,
            )
        }
        Spacer(Modifier.height(14.dp))
        Text(
            text = "Type it out",
            fontSize = 11.sp,
            letterSpacing = 0.88.sp,
            color = colors.neutral.step500,
            modifier = Modifier.padding(bottom = 6.dp),
        )
        CompositionLocalProvider(LocalTextToolbar provides NoMenuTextToolbar) {
            val error = MaterialTheme.colorScheme.error
            BasicTextField(
                value = typedText,
                onValueChange = { raw ->
                    val accepted = acceptTypingEdit(typedText, raw)
                    if ('\n' !in accepted && accepted != typedText) {
                        onTypedTextChanged(accepted)
                    }
                },
                textStyle = TextStyle(
                    fontFamily = MaterialTheme.typography.bodyLarge.fontFamily,
                    fontSize = 15.sp,
                    lineHeight = 25.5.sp,
                    color = colors.text,
                ),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    autoCorrect = false,
                    capitalization = KeyboardCapitalization.None,
                    imeAction = ImeAction.None,
                ),
                keyboardActions = KeyboardActions(),
                visualTransformation = if (showTypingMismatches) {
                    MismatchTransformation(
                        passage = passage,
                        markBackground = MaterialTheme.colorScheme.errorContainer,
                        markText = MaterialTheme.colorScheme.onErrorContainer,
                    )
                } else {
                    VisualTransformation.None
                },
                cursorBrush = SolidColor(colors.accent),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { inner ->
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 96.dp)
                            .background(colors.surface, RoundedCornerShape(12.dp))
                            .border(
                                1.dp,
                                if (hasMismatch) error else colors.accent,
                                RoundedCornerShape(12.dp),
                            )
                            .padding(15.dp),
                    ) {
                        inner()
                    }
                },
            )
        }
        Row(
            Modifier.padding(top = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                Modifier
                    .weight(1f)
                    .height(6.dp)
                    .background(colors.neutral.step800, RoundedCornerShape(3.dp)),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(
                            typingProgressChars(typedText.length, passage.length),
                        )
                        .fillMaxHeight()
                        .background(colors.accent),
                )
            }
            Text(
                text = "${typedText.length} / ${passage.length}",
                style = TextStyle(fontSize = 12.sp, fontFeatureSettings = "tnum"),
                color = colors.neutral.step500,
            )
        }
        Row(
            Modifier.padding(top = 14.dp, bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (hasMismatch) {
                Hint(Ph.Eye, "Typo positions shown")
                Hint(PhArrowsCounterClockwise, "Kept what you typed")
            } else {
                Hint(PhClipboard, "Paste is off")
                Hint(PhMagicWand, "Autocorrect off")
            }
        }
        TypingSubmitButton(
            enabled = submitEnabled,
            onClick = { onSubmit(typedText) },
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
        )
    }
}

@Composable
private fun TypingSubmitButton(
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = NocturneTheme.colors
    val shape = RoundedCornerShape(10.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .background(
                if (enabled) colors.accentRamp.step300 else colors.neutral.step800,
                shape,
            )
            .clickable(enabled = enabled) { onClick() },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Submit",
            fontSize = 15.sp,
            color = if (enabled) Color.Black.copy(alpha = 0.96f) else colors.neutral.step600,
        )
    }
}

@Composable
private fun Hint(glyph: Int, text: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PhosphorIcon(glyph, tint = NocturneTheme.colors.neutral.step600, size = 13)
        Text(
            text = text,
            fontSize = 11.sp,
            color = NocturneTheme.colors.neutral.step600,
        )
    }
}

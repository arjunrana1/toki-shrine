package com.arjunrana.tokishrine.ui.interruption

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arjunrana.tokishrine.ui.theme.NocturneTheme

// Full-bleed photo dim (PRD §11): dark enough that the Nocturne foreground
// stays legible and the photo recedes into the dark ground.
private const val GATE_SCRIM_ALPHA = 0.78f

/*
 * The real block gate (PRD §6 screen 15, §11). Stateless: the caller supplies
 * the block/target names, one humour line and the background drawable —
 * random selection of either is the runtime task's job. The walk-away is the
 * filled prominent action and the way in the quieter outlined one; the screen
 * is comprehensible without scrolling. Buttons only report; BlockActivity
 * integration, walk_away events and challenge routing belong to TS-P5B.
 */
@Composable
fun BlockGateScreen(
    blockName: String,
    headline: String,
    humourLine: String,
    backgroundRes: Int,
    onWalkAway: () -> Unit,
    onEnterChallenge: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxSize()) {
        Image(
            painter = painterResource(backgroundRes),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        // Repeat the same asset without distortion so its complete subject is
        // always visible. The cropped copy behind it supplies the uncovered
        // top/bottom ground on tall screens.
        Image(
            painter = painterResource(backgroundRes),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            alignment = Alignment.Center,
            modifier = Modifier.fillMaxSize(),
        )
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = GATE_SCRIM_ALPHA)))
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 28.dp, vertical = 24.dp),
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
                text = "Not now",
                height = 58,
                fontSize = 17.5f,
                onClick = onWalkAway,
            )
            GateOutlinedButton(
                text = "I'll do the challenge. Let me in 🚩🤨",
                height = 58,
                fontSize = 14.5f,
                onClick = onEnterChallenge,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
    }
}

// The gate's two actions are screen-specific treatments, not Nocturne .btn
// variants: the reference fills the walk-away with accent-300 over the photo
// ground and sets the way-in as translucent glass. The near-white/near-black
// values are the screen-15 reference's explicit exceptions to the no-pure-
// black/white rule, expressed from Color constants rather than new hex tokens.

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
            .background(colors.accentRamp.step300, shape)
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

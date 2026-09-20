package com.arjunrana.tokishrine.ui.interruption

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arjunrana.tokishrine.ui.theme.NocturneTheme

/*
 * The delay countdown (PRD §6 screen 19, and screen 22's waiting variant via
 * [purpose]). Render-only: total and remaining seconds are supplied and the
 * ring drains with the remaining fraction — no timer, no lifecycle or lock
 * observation, no keep-awake; all of that is TS-P5B. Per PRD §6 no status
 * message is shown (the mock's "keep holding" line is removed with hold
 * detection); the escape action is the only control.
 */
@Composable
fun DelayCountdownScreen(
    purpose: ChallengePurpose,
    blockName: String,
    totalSeconds: Int,
    remainingSeconds: Int,
    onEscape: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = NocturneTheme.colors
    val glow = colors.accentRamp.step900
    val ground = colors.neutral.step900
    Box(
        modifier
            .fillMaxSize()
            // The reference's radial indigo-to-black ground, rebuilt from
            // existing tokens plus the documented shade-black exception
            // (120% 90% at 50% 12%); an approximation pending the owner's
            // visual pass, like all translated CSS gradients.
            .drawBehind {
                drawRect(
                    Brush.radialGradient(
                        colors = listOf(glow, ground, Color.Black),
                        center = Offset(size.width * 0.5f, size.height * 0.12f),
                        radius = size.width * 1.2f,
                    ),
                )
            }
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 30.dp, vertical = 24.dp),
        ) {
            if (purpose == ChallengePurpose.TURN_OFF) {
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
                        text = delayEscapeLabel(purpose),
                        fontSize = 13.sp,
                        color = colors.neutral.step300,
                        modifier = Modifier
                            .clickable { onEscape() }
                            .padding(vertical = 2.dp),
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            Box(
                Modifier
                    .size(200.dp)
                    .align(Alignment.CenterHorizontally),
                contentAlignment = Alignment.Center,
            ) {
                val track = colors.neutral.step800
                val arc = colors.accent
                val sweep = countdownProgress(totalSeconds, remainingSeconds)
                Canvas(Modifier.size(200.dp)) {
                    val stroke = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                    val inset = stroke.width / 2
                    val arcSize = Size(size.width - stroke.width, size.height - stroke.width)
                    drawArc(
                        color = track,
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = Offset(inset, inset),
                        size = arcSize,
                        style = stroke,
                    )
                    drawArc(
                        color = arc,
                        startAngle = -90f,
                        sweepAngle = 360f * sweep,
                        useCenter = false,
                        topLeft = Offset(inset, inset),
                        size = arcSize,
                        style = stroke,
                    )
                }
                Text(
                    text = formatClock(remainingSeconds),
                    style = TextStyle(
                        fontSize = 52.sp,
                        letterSpacing = (-1).sp,
                        fontFeatureSettings = "tnum",
                    ),
                    color = colors.text,
                )
            }
            Spacer(Modifier.weight(1f))
            if (purpose == ChallengePurpose.PAUSE) {
                Text(
                    text = delayEscapeLabel(purpose),
                    fontSize = 13.5.sp,
                    color = colors.neutral.step400,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .clickable { onEscape() }
                        .padding(vertical = 2.dp),
                )
            }
        }
    }
}

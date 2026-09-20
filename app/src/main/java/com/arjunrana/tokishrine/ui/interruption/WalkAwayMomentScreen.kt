package com.arjunrana.tokishrine.ui.interruption

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arjunrana.tokishrine.ui.icons.Ph
import com.arjunrana.tokishrine.ui.icons.PhosphorIcon
import com.arjunrana.tokishrine.ui.theme.NocturneTheme

/*
 * The walk-away moment (PRD §6 screen 16): confirmation of the declined
 * challenge with the supplied global daily count. Stateless by contract —
 * the 2-second auto-dismiss timer is the runtime task's, never owned here;
 * this screen only reports taps through [onDismiss] (tap anywhere to
 * continue immediately). The count line copy follows PRD §6 ("That's the
 * 4th time today"), superseding the mock's "instance" wording.
 */
@Composable
fun WalkAwayMomentScreen(
    dailyCount: Int,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = NocturneTheme.colors
    Box(
        modifier
            .fillMaxSize()
            // The reference grounds this screen darker than --color-bg
            // (#0b0d17); shade black is the system's documented exception.
            .background(Color.Black)
            .statusBarsPadding()
            .navigationBarsPadding()
            .clickable { onDismiss() },
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 30.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(1f))
            // 88px accent-900 disc in a 12px accent-8% halo (.sb reference).
            Box(
                Modifier
                    .size(112.dp)
                    .background(colors.accent.copy(alpha = 0.08f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier
                        .size(88.dp)
                        .background(colors.accentRamp.step900, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    PhosphorIcon(Ph.Check, tint = colors.accentRamp.step200, size = 42)
                }
            }
            Spacer(Modifier.height(28.dp))
            Text(
                text = "Nice! You just saved your precious time",
                fontSize = 26.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 32.sp,
                color = colors.text,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "That's the ${ordinal(dailyCount)} time today",
                fontSize = 15.sp,
                lineHeight = 23.sp,
                color = colors.neutral.step300,
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = "tap to continue",
                fontSize = 10.5.sp,
                letterSpacing = 0.1.sp,
                color = colors.neutral.step600,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }
    }
}

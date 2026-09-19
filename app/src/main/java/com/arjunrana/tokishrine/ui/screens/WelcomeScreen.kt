package com.arjunrana.tokishrine.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arjunrana.tokishrine.ui.components.NocturneButton
import com.arjunrana.tokishrine.ui.icons.Ph
import com.arjunrana.tokishrine.ui.icons.PhosphorIcon
import com.arjunrana.tokishrine.ui.theme.NocturneTheme

// Screen 1 (first launch only). Single CTA Get started — the mock's
// "How it works" secondary CTA is removed per PRD §6. Browsing and block
// creation are never gated behind this screen or onboarding (PRD §12).
// Headline and body copy are the owner's replacement wording (direct
// correction, 19 September), superseding the PRD §6 screen-1 text.
@Composable
fun WelcomeScreen(
    onGetStarted: () -> Unit,
) {
    val colors = NocturneTheme.colors
    Column(
        Modifier
            .fillMaxSize()
            .background(colors.bg)
            .statusBarsPadding()
            .padding(horizontal = 30.dp, vertical = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(1f))
        Box(
            Modifier
                .size(76.dp)
                .background(colors.accentRamp.step900, RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center,
        ) {
            PhosphorIcon(Ph.HourglassMedium, tint = colors.accentRamp.step300, size = 38)
        }
        Spacer(Modifier.height(26.dp))
        Text(
            "Your time, your rules",
            fontSize = 28.sp,
            lineHeight = 34.sp,
            fontWeight = FontWeight.Medium,
            color = colors.text,
        )
        Spacer(Modifier.height(14.dp))
        Text(
            "Doomscrolling? Time-blindness? Yeah, we got you.",
            fontSize = 15.sp,
            lineHeight = 24.sp,
            color = colors.neutral.step300,
            textAlign = TextAlign.Center,
        )
        Text(
            "We don't block you! We just do a vibe check before you fall in.",
            fontSize = 14.sp,
            lineHeight = 22.sp,
            color = colors.neutral.step500,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 16.dp),
        )
        Text(
            "You choose what gets paused, for how long, and how you get back.",
            fontSize = 14.sp,
            lineHeight = 22.sp,
            color = colors.neutral.step500,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 12.dp),
        )
        Spacer(Modifier.weight(1f))
        NocturneButton(
            "Get started",
            block = true,
            height = 46.dp,
            fontSize = 15,
            onClick = onGetStarted,
        )
    }
}

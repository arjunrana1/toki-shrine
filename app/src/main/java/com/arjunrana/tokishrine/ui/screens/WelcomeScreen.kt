package com.arjunrana.tokishrine.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arjunrana.tokishrine.ui.components.NocturneButton
import com.arjunrana.tokishrine.ui.theme.NocturneTheme
import com.arjunrana.tokishrine.ui.util.toImageBitmap

// Screen 1 (first launch only). Single CTA Get started — the mock's
// "How it works" secondary CTA is removed per PRD §6. Browsing and block
// creation are never gated behind this screen or onboarding (PRD §12).
// Headline, body copy and the app-icon mark are the owner's replacement
// wording (direct correction, 19 September), superseding both the PRD §6
// screen-1 text and the earlier Phase 3 welcome corrections.
@Composable
fun WelcomeScreen(
    onGetStarted: () -> Unit,
) {
    val colors = NocturneTheme.colors
    // The welcome mark is the app's own launcher icon (owner request,
    // 19 September): rasterize the same adaptive drawable the launcher
    // shows, large enough to stay crisp at display size.
    val context = LocalContext.current
    val appIconBitmap = remember {
        runCatching { context.packageManager.getApplicationIcon(context.packageName) }
            .getOrNull()
            ?.toImageBitmap(size = 256)
    }
    Column(
        Modifier
            .fillMaxSize()
            .background(colors.bg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 30.dp, vertical = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(1f))
        if (appIconBitmap != null) {
            Image(
                bitmap = appIconBitmap,
                contentDescription = null, // decorative; the headline names the app's promise
                modifier = Modifier
                    .size(76.dp)
                    .clip(RoundedCornerShape(20.dp)),
            )
        }
        Spacer(Modifier.height(26.dp))
        Text(
            "your time, your rules",
            fontSize = 28.sp,
            lineHeight = 34.sp,
            fontWeight = FontWeight.Medium,
            color = colors.text,
        )
        Spacer(Modifier.height(14.dp))
        Text(
            "Doomscrolling? Time-blindness? We got you covered",
            fontSize = 15.sp,
            lineHeight = 24.sp,
            color = colors.neutral.step300,
            textAlign = TextAlign.Center,
        )
        Text(
            "This isn't a block.",
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

package com.arjunrana.tokishrine.ui.interruption

import android.app.Activity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.arjunrana.tokishrine.ui.theme.NocturneTheme

/*
 * The walk-away moment (PRD §6 screen 16), redesigned by the 9 October §17
 * addendum (P7-F17): a random title with its own image, a random subtitle
 * and the "N wins today" pill, on a light lavender ground (accent-300, owner 9 October) — a deliberate
 * exception to the dark theme for this screen only. Stateless by contract:
 * the auto-dismiss timer is BlockActivity's; this screen only reports taps
 * through [onDismiss] (tap anywhere to continue immediately). The pick is
 * made by CelebrationPicker from a seed BlockActivity keeps across
 * recreation.
 */
@Composable
fun WalkAwayMomentScreen(
    celebration: Celebration,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = NocturneTheme.colors
    val ink = colors.accentRamp.step900

    // Dark status/navigation icons while the light screen is up; restored on exit.
    val view = LocalView.current
    DisposableEffect(view) {
        val window = (view.context as? Activity)?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        controller?.isAppearanceLightStatusBars = true
        controller?.isAppearanceLightNavigationBars = true
        onDispose {
            controller?.isAppearanceLightStatusBars = false
            controller?.isAppearanceLightNavigationBars = false
        }
    }

    Box(
        modifier
            .fillMaxSize()
            .background(colors.accentRamp.step300)
            .statusBarsPadding()
            .navigationBarsPadding()
            .clickable { onDismiss() },
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(1f))
            // Owner, 9 October: the meme is the hero — as wide as the screen
            // allows up to 300dp, inside a thin accent ring.
            Box(
                Modifier
                    .widthIn(max = 300.dp)
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .background(colors.accentRamp.step400, CircleShape)
                    .padding(6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(celebration.image),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                )
            }
            Spacer(Modifier.height(28.dp))
            // Type follows the block screen's headline cluster: bundled Inter
            // Medium (no synthesized bold), 28/34 headline, 15/23 body.
            Text(
                text = celebration.title,
                fontFamily = MaterialTheme.typography.headlineMedium.fontFamily,
                fontSize = 28.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 34.sp,
                color = ink,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = celebration.subtitle,
                fontSize = 15.sp,
                lineHeight = 23.sp,
                color = colors.accentRamp.step700,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(16.dp))
            // Plain line, no pill (owner, 9 October).
            Text(
                text = "🏆  ${celebration.winsLine}",
                fontSize = 15.sp,
                lineHeight = 23.sp,
                fontWeight = FontWeight.Medium,
                color = ink,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = "tap to continue",
                fontSize = 12.sp,
                color = colors.accentRamp.step700,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }
    }
}

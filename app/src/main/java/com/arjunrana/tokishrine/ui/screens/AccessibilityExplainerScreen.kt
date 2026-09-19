package com.arjunrana.tokishrine.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arjunrana.tokishrine.ui.components.NocturneAppbar
import com.arjunrana.tokishrine.ui.components.NocturneButton
import com.arjunrana.tokishrine.ui.icons.Ph
import com.arjunrana.tokishrine.ui.icons.PhosphorIcon
import com.arjunrana.tokishrine.ui.theme.NocturneTheme

// Screen 3: shown before Android's own accessibility screen (PRD §5/§6 —
// this content is load-bearing; if it fails, users abandon here). States
// plainly what the service does and never does. Title, body and list
// content follow the owner-supplied `Accessibility Explainer.png`
// replacement reference (P3-F03, 19 September).
@Composable
fun AccessibilityExplainerScreen(
    onBack: () -> Unit,
    onContinueToSettings: () -> Unit,
) {
    val colors = NocturneTheme.colors
    Box(
        Modifier
            .fillMaxSize()
            .background(colors.bg)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .padding(bottom = 16.dp),
        ) {
            NocturneAppbar(title = "Accessibility", onBack = onBack)
            Text(
                "About the accessibility screen",
                fontSize = 23.sp,
                fontWeight = FontWeight.Medium,
                color = colors.text,
            )
            Text(
                "Android calls this \"full control of your device\" and warns we can \"view everything you do.\" That's its standard wording for accessibility access — here's what we actually do with it.",
                fontSize = 13.5.sp,
                lineHeight = 22.sp,
                color = colors.neutral.step500,
                modifier = Modifier.padding(top = 4.dp, bottom = 18.dp),
            )
            Text(
                "WHAT WE DO",
                fontSize = 11.sp,
                letterSpacing = 0.88.sp,
                color = colors.accentRamp.step300,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            Column {
                DoRow(true, "See which app or site is open")
                Spacer(Modifier.height(9.dp))
                DoRow(true, "Show the pause screen over it")
            }
            Text(
                "WHAT WE NEVER DO",
                fontSize = 11.sp,
                letterSpacing = 0.88.sp,
                color = colors.neutral.step500,
                modifier = Modifier.padding(top = 18.dp, bottom = 8.dp),
            )
            Column {
                DoRow(false, "Read the contents of any page")
                Spacer(Modifier.height(9.dp))
                DoRow(false, "Tap, type, or act for you")
                Spacer(Modifier.height(9.dp))
                DoRow(false, "Log, store, or send your activity anywhere")
            }
            Spacer(Modifier.weight(1f))
            NocturneButton(
                "Continue to settings",
                block = true,
                height = 46.dp,
                onClick = onContinueToSettings,
            )
        }
    }
}

@Composable
private fun DoRow(positive: Boolean, text: String) {
    val colors = NocturneTheme.colors
    Row {
        PhosphorIcon(
            if (positive) Ph.Check else Ph.X,
            tint = if (positive) colors.accentRamp.step300 else colors.neutral.step400,
            size = 15,
            modifier = Modifier.padding(top = 3.dp),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text,
            fontSize = 13.5.sp,
            lineHeight = 20.sp,
            color = if (positive) colors.text else colors.neutral.step400,
        )
    }
}

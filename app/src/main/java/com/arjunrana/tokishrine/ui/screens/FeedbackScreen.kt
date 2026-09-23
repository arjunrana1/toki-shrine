package com.arjunrana.tokishrine.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arjunrana.tokishrine.data.repo.EventRepository
import com.arjunrana.tokishrine.ui.components.NocturneAppbar
import com.arjunrana.tokishrine.ui.components.NocturneButton
import com.arjunrana.tokishrine.ui.components.NocturneTextField
import com.arjunrana.tokishrine.ui.theme.NocturneTheme

// Screen 25 (PRD §6/§13): free-text field and *Send it*, which hands the
// typed text to the device mail client via the email intent. The mock's
// "Attach a diagnostic log" toggle is removed — no logs are collected.
//
// The draft is saveable, so a recreation or process death keeps the typed
// text. A failed handoff (no mail app / launch failure) keeps the screen and
// the draft, and explains itself inline; only a successful handoff pops.
@Composable
fun FeedbackScreen(
    eventRepo: EventRepository,
    onSend: (body: String) -> Boolean,
    onSent: () -> Unit,
    onBack: () -> Unit,
) {
    val colors = NocturneTheme.colors

    // Feature-engagement event (§10): one feedback_opened per intentional
    // entry, matching the settings_viewed pattern.
    LaunchedEffect(Unit) {
        runCatching { eventRepo.log(EventRepository.EVENT_FEEDBACK_OPENED) }
    }

    var body by rememberSaveable { mutableStateOf("") }
    var handoffFailed by rememberSaveable { mutableStateOf(false) }

    Box(
        Modifier
            .fillMaxSize()
            .background(colors.bg)
            .statusBarsPadding()
            .navigationBarsPadding()
            // The field and the bottom CTA must both stay reachable with the
            // keyboard open (Phase 7 insets pass); scrolling covers large
            // text and small screens.
            .imePadding(),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            NocturneAppbar(title = "Send feedback", onBack = onBack)
            Text(
                text = "How's it going?",
                fontSize = 21.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = (-0.21).sp,
                color = colors.text,
            )
            Text(
                text = "You're one of a handful of people trying this. Tell us what's working and what's driving you up the wall.",
                fontSize = 13.sp,
                lineHeight = 20.8.sp,
                color = colors.neutral.step500,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
            )
            NocturneTextField(
                value = body,
                onValueChange = {
                    body = it
                    handoffFailed = false
                },
                hint = "What's on your mind…",
                minHeight = 150.dp,
                multiline = true,
            )
            if (handoffFailed) {
                Text(
                    text = "No email app could be reached, so nothing was sent. Your message is still here.",
                    fontSize = 12.sp,
                    lineHeight = 16.8.sp,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 10.dp),
                )
            }
            Spacer(Modifier.height(16.dp))
            NocturneButton(
                "Send it",
                block = true,
                height = 46.dp,
                onClick = {
                    if (onSend(body)) {
                        onSent()
                    } else {
                        handoffFailed = true
                    }
                },
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}

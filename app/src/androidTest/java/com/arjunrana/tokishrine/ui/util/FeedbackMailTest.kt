package com.arjunrana.tokishrine.ui.util

import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith

// Phase 7 acceptance for the §13 feedback handoff: the intent fixes the
// recipient, subject and typed body, and carries no log/attachment extras.
// (Execution requires owner-authorized device/emulator policy; compilation is
// verified by assembleDebugAndroidTest.)
@RunWith(AndroidJUnit4::class)
class FeedbackMailTest {

    @Test
    fun mailIntentCarriesRecipientSubjectAndTypedBodyOnly() {
        val body = "The typing challenge passage generator repeated a word."
        val intent = feedbackMailIntent(body)

        assertEquals(Intent.ACTION_SENDTO, intent.action)
        assertEquals("mailto:${FeedbackEmail.RECIPIENT}", intent.data.toString())
        assertEquals(FeedbackEmail.SUBJECT, intent.getStringExtra(Intent.EXTRA_SUBJECT))
        assertEquals(body, intent.getStringExtra(Intent.EXTRA_TEXT))
        // No attachment/stream extras: no diagnostic log exists (PRD §6/§13).
        assertFalse(intent.hasExtra(Intent.EXTRA_STREAM))
    }

    @Test
    fun emptyTypedBodyIsPassedThroughUntouched() {
        val intent = feedbackMailIntent("")

        assertEquals("", intent.getStringExtra(Intent.EXTRA_TEXT))
    }
}

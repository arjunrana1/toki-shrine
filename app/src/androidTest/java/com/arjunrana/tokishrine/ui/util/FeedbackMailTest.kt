package com.arjunrana.tokishrine.ui.util

import android.content.Intent
import android.net.MailTo
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

// Phase 7 acceptance for the §13 feedback handoff: the intent fixes the
// recipient, subject and typed body — in the mailto: URI itself (P7-F14, Gmail
// ignores extras alone) and as extras — and carries no log/attachment extras.
// (Execution requires owner-authorized device/emulator policy; compilation is
// verified by assembleDebugAndroidTest.)
@RunWith(AndroidJUnit4::class)
class FeedbackMailTest {

    @Test
    fun mailIntentCarriesRecipientSubjectAndTypedBodyOnly() {
        val body = "The typing challenge passage generator repeated a word."
        val intent = feedbackMailIntent(body)

        assertEquals(Intent.ACTION_SENDTO, intent.action)
        assertEquals(FeedbackEmail.mailtoUri(body), intent.data.toString())
        assertEquals(FeedbackEmail.SUBJECT, intent.getStringExtra(Intent.EXTRA_SUBJECT))
        assertEquals(body, intent.getStringExtra(Intent.EXTRA_TEXT))
        assertArrayEquals(
            arrayOf(FeedbackEmail.RECIPIENT),
            intent.getStringArrayExtra(Intent.EXTRA_EMAIL),
        )
        // No attachment/stream extras: no diagnostic log exists (PRD §6/§13).
        assertFalse(intent.hasExtra(Intent.EXTRA_STREAM))
    }

    @Test
    fun mailtoUriParsesBackToRecipientSubjectAndBody() {
        val body = "Slow & odd?\nSecond line = 100% #1 + more — ünïcode 😀"
        val intent = feedbackMailIntent(body)

        val dataString = intent.dataString!!
        assertTrue(MailTo.isMailTo(dataString))
        // RFC 6068 order: split the still-encoded query on & and =, then decode
        // each part. android.net.MailTo.parse cut the body at its first encoded
        // & on API 36 (launch-gate LG-2), so it cannot be the oracle here.
        val (to, query) = Uri.parse(dataString).encodedSchemeSpecificPart.split("?", limit = 2)
        val fields = query.split("&").associate { field ->
            val (name, value) = field.split("=", limit = 2)
            Uri.decode(name) to Uri.decode(value)
        }
        assertEquals(FeedbackEmail.RECIPIENT, Uri.decode(to))
        assertEquals(FeedbackEmail.SUBJECT, fields["subject"])
        assertEquals(body.replace("\n", "\r\n"), fields["body"])
    }

    @Test
    fun emptyTypedBodyIsPassedThroughUntouched() {
        val intent = feedbackMailIntent("")

        assertEquals("", intent.getStringExtra(Intent.EXTRA_TEXT))
        assertEquals(FeedbackEmail.SUBJECT, MailTo.parse(intent.dataString!!).subject)
    }
}

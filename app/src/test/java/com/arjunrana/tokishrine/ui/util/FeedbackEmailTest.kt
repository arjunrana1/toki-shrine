package com.arjunrana.tokishrine.ui.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.URLDecoder

// The §6 screen 25 / §13 feedback contract, pinned at the pure-spec level:
// fixed recipient/subject, and feedback_sent meaning a successful intent
// handoff — never confirmed delivery, never a guess past a missing handler.
class FeedbackEmailTest {

    @Test
    fun recipientAndSubjectMatchTheProductDecision() {
        assertEquals("arjranaprep@gmail.com", FeedbackEmail.RECIPIENT)
        assertEquals("Feedback from user", FeedbackEmail.SUBJECT)
    }

    // P7-F14: Gmail ignored the subject/body extras, so both must be in the
    // mailto: URI itself. Pinned on the pure string (android.net.Uri is not
    // available on the JVM); FeedbackMailTest covers the real Intent/MailTo.

    @Test
    fun mailtoUriCarriesRecipientSubjectAndBodyAsPercentEncodedQuery() {
        assertEquals(
            "mailto:arjranaprep@gmail.com?subject=Feedback%20from%20user&body=Hello%20there",
            FeedbackEmail.mailtoUri("Hello there"),
        )
    }

    @Test
    fun mailtoUriEncodesQueryDelimitersSoTheBodyCannotSplitTheQuery() {
        val uri = FeedbackEmail.mailtoUri("a&b=c?d#e%f+g")

        assertEquals(
            "mailto:arjranaprep@gmail.com?subject=Feedback%20from%20user" +
                "&body=a%26b%3Dc%3Fd%23e%25f%2Bg",
            uri,
        )
        // Exactly one query start and one separator between the two fields.
        assertEquals(1, uri.count { it == '?' })
        assertEquals(1, uri.count { it == '&' })
    }

    @Test
    fun mailtoUriNeverUsesPlusForSpaces() {
        // '+' is a literal plus in mailto, not a space (RFC 6068).
        val uri = FeedbackEmail.mailtoUri("one two  three")

        assertFalse(uri.contains('+'))
        assertTrue(uri.endsWith("body=one%20two%20%20three"))
    }

    @Test
    fun mailtoUriEncodesEveryLineBreakStyleAsCrLf() {
        assertTrue(FeedbackEmail.mailtoUri("a\nb").endsWith("body=a%0D%0Ab"))
        assertTrue(FeedbackEmail.mailtoUri("a\r\nb").endsWith("body=a%0D%0Ab"))
        assertTrue(FeedbackEmail.mailtoUri("a\rb").endsWith("body=a%0D%0Ab"))
        assertTrue(FeedbackEmail.mailtoUri("a\n\nb").endsWith("body=a%0D%0A%0D%0Ab"))
    }

    @Test
    fun mailtoUriEncodesNonAsciiAsUtf8() {
        assertTrue(FeedbackEmail.mailtoUri("café").endsWith("body=caf%C3%A9"))
        assertTrue(FeedbackEmail.mailtoUri("😀").endsWith("body=%F0%9F%98%80"))
    }

    @Test
    fun mailtoUriWithEmptyBodyKeepsTheSubject() {
        assertEquals(
            "mailto:arjranaprep@gmail.com?subject=Feedback%20from%20user&body=",
            FeedbackEmail.mailtoUri(""),
        )
    }

    @Test
    fun mailtoUriRoundTripsTheTypedBody() {
        val body = "It's \"slow\" & weird?\nSecond line=100% #1 + more — ünïcode 😀\n"
        val uri = FeedbackEmail.mailtoUri(body)

        val query = uri.substringAfter('?')
        val fields = query.split('&').associate {
            it.substringBefore('=') to URLDecoder.decode(it.substringAfter('='), "UTF-8")
        }

        assertEquals(setOf("subject", "body"), fields.keys)
        assertEquals(FeedbackEmail.SUBJECT, fields["subject"])
        assertEquals(body.replace("\n", "\r\n"), fields["body"])
    }

    @Test
    fun handedOffRequiresBothHandlerAndSuccessfulLaunch() {
        assertTrue(FeedbackEmail.handedOff(hasMailHandler = true, launchSucceeded = true))
    }

    @Test
    fun missingHandlerMeansNoFeedbackSent() {
        assertFalse(FeedbackEmail.handedOff(hasMailHandler = false, launchSucceeded = true))
        assertFalse(FeedbackEmail.handedOff(hasMailHandler = false, launchSucceeded = false))
    }

    @Test
    fun failedLaunchMeansNoFeedbackSent() {
        // A handler existing is not enough: nothing was handed off unless the
        // populated intent actually launched.
        assertFalse(FeedbackEmail.handedOff(hasMailHandler = true, launchSucceeded = false))
    }
}

package com.arjunrana.tokishrine.ui.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// The §6 screen 25 / §13 feedback contract, pinned at the pure-spec level:
// fixed recipient/subject, and feedback_sent meaning a successful intent
// handoff — never confirmed delivery, never a guess past a missing handler.
class FeedbackEmailTest {

    @Test
    fun recipientAndSubjectMatchTheProductDecision() {
        assertEquals("arjranaprep@gmail.com", FeedbackEmail.RECIPIENT)
        assertEquals("Feedback from user", FeedbackEmail.SUBJECT)
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

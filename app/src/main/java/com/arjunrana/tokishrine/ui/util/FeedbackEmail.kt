package com.arjunrana.tokishrine.ui.util

/**
 * Pure specification of the §6 screen 25 / §13 feedback handoff: where the
 * email goes and what counts as a send. The Intent itself is built by
 * [feedbackMailIntent] so this object stays JVM-testable with no Android
 * dependencies.
 *
 * feedback_sent means the populated intent was successfully handed to an
 * external mail application — never confirmed delivery. No logs, no
 * attachments, no collection.
 */
object FeedbackEmail {

    const val RECIPIENT = "arjranaprep@gmail.com"
    const val SUBJECT = "Feedback from user"

    /**
     * Whether feedback_sent may be logged: the handoff requires a mail
     * handler to exist and the launch of the populated intent to succeed.
     * Handler absence (graceful failure) and a failed launch both mean no
     * event — nothing was handed off.
     */
    fun handedOff(hasMailHandler: Boolean, launchSucceeded: Boolean): Boolean =
        hasMailHandler && launchSucceeded
}

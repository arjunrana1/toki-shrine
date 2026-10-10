package com.arjunrana.tokishrine.ui.util

import java.net.URLEncoder

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
     * The RFC 6068 `mailto:` URI carrying the fixed subject and the typed
     * body (P7-F14). Gmail ignores EXTRA_SUBJECT/EXTRA_TEXT on ACTION_SENDTO,
     * so both must live in the URI itself. Values are UTF-8 percent-encoded
     * with spaces as `%20` (never `+`, which mail clients read literally) and
     * every line break as `%0D%0A`; the recipient is a fixed address and stays
     * unencoded.
     */
    fun mailtoUri(body: String): String =
        "mailto:$RECIPIENT?subject=${encode(SUBJECT)}&body=${encode(body)}"

    private fun encode(value: String): String =
        URLEncoder.encode(value.replace(LINE_BREAK, "\r\n"), "UTF-8").replace("+", "%20")

    private val LINE_BREAK = Regex("\r\n|\r|\n")

    /**
     * Whether feedback_sent may be logged: the handoff requires a mail
     * handler to exist and the launch of the populated intent to succeed.
     * Handler absence (graceful failure) and a failed launch both mean no
     * event — nothing was handed off.
     */
    fun handedOff(hasMailHandler: Boolean, launchSucceeded: Boolean): Boolean =
        hasMailHandler && launchSucceeded
}

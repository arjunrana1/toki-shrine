package com.arjunrana.tokishrine.ui.util

import android.content.Intent
import android.net.Uri

/**
 * The §13 feedback email intent: ACTION_SENDTO with a mailto URI so only mail
 * clients resolve it, the fixed recipient/subject from [FeedbackEmail], and
 * the typed text as the body. Deliberately no stream/attachment extras — no
 * diagnostic log exists anywhere (PRD §6 screen 25).
 */
internal fun feedbackMailIntent(body: String): Intent =
    Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${FeedbackEmail.RECIPIENT}"))
        .putExtra(Intent.EXTRA_SUBJECT, FeedbackEmail.SUBJECT)
        .putExtra(Intent.EXTRA_TEXT, body)

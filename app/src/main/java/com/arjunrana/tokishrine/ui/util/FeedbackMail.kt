package com.arjunrana.tokishrine.ui.util

import android.content.Intent
import android.net.Uri

/**
 * The §13 feedback email intent: ACTION_SENDTO with a mailto URI so only mail
 * clients resolve it. The fixed recipient, subject and the typed body are
 * encoded in the URI ([FeedbackEmail.mailtoUri]) because Gmail ignores them as
 * extras alone (P7-F14); the same values are also passed as extras for clients
 * that read those instead. Deliberately no stream/attachment extras — no
 * diagnostic log exists anywhere (PRD §6 screen 25).
 */
internal fun feedbackMailIntent(body: String): Intent =
    Intent(Intent.ACTION_SENDTO, Uri.parse(FeedbackEmail.mailtoUri(body)))
        .putExtra(Intent.EXTRA_EMAIL, arrayOf(FeedbackEmail.RECIPIENT))
        .putExtra(Intent.EXTRA_SUBJECT, FeedbackEmail.SUBJECT)
        .putExtra(Intent.EXTRA_TEXT, body)

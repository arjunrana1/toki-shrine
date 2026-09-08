package com.arjunrana.tokishrine.ui.util

import kotlin.math.roundToInt

/*
 * Live typing-cost estimate (PRD §7.1). The acceptance-pinned point is
 * 100 characters ≈ 30 seconds, i.e. 0.3 s per character (~20 wpm on mobile
 * with autocorrect off and paste blocked). The PRD's prose figure of "300
 * around two and a half minutes" is inconsistent with that same basis; the
 * tested constant wins. Change RATE only with the owner.
 */

private const val SECONDS_PER_CHAR = 0.3

fun typingEstimateSeconds(chars: Int): Int = (chars * SECONDS_PER_CHAR).roundToInt()

fun formatEstimate(seconds: Int): String {
    val minutes = seconds / 60
    val remainder = seconds % 60
    return when {
        seconds < 60 -> "About $seconds seconds"
        remainder == 0 -> "About $minutes min"
        else -> "About $minutes min $remainder sec"
    }
}

fun formatCountdown(seconds: Int): String = when {
    seconds < 60 -> "$seconds sec"
    seconds % 60 == 0 -> "${seconds / 60} min"
    else -> "${seconds / 60} min ${seconds % 60} sec"
}

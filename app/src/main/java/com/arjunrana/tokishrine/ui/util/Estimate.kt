package com.arjunrana.tokishrine.ui.util

import kotlin.math.roundToInt

/*
 * Live typing-cost estimate. PRD §17 R3 (12 September 2026) pins the rate at
 * 0.4 seconds per character — 100 characters ≈ 40 seconds, 300 ≈ 120 — used
 * consistently across configuration, review, detail and activation
 * summaries. It supersedes the earlier 0.3 s/char constant. Change
 * SECONDS_PER_CHAR only with the owner.
 */

private const val SECONDS_PER_CHAR = 0.4

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

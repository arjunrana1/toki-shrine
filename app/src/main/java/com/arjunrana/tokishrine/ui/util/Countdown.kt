package com.arjunrana.tokishrine.ui.util

/*
 * Countdown rendering for stored wait values. Typing time estimates are
 * removed everywhere (PRD §17 "Unlock minutes addendum — 11 October 2026",
 * TS-unlock-minutes UM-12); surfaces show the character count only.
 */

fun formatCountdown(seconds: Int): String = when {
    seconds < 60 -> "$seconds sec"
    seconds % 60 == 0 -> "${seconds / 60} min"
    else -> "${seconds / 60} min ${seconds % 60} sec"
}

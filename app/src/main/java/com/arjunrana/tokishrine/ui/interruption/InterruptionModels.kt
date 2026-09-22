package com.arjunrana.tokishrine.ui.interruption

/*
 * Pure presentation model and helpers for the Phase 5 interruption surfaces
 * (PRD §§6–8, §11 and §17's always-show-typos amendment). Everything here is
 * stateless and JVM-testable: the screens format supplied values and nothing
 * more — no Room reads, no random selection, no timers, no event writes.
 * Terminal decisions (completion, mismatch accounting, walk-away events)
 * belong to the challenge runtime task (TS-P5B).
 */

/**
 * Why a typing or waiting challenge is running. Pause and turn-off surfaces
 * share one layout and differ only in copy (PRD §6 screens 17–19 vs 22), so
 * the mode is an explicit model input rather than a duplicated screen.
 */
enum class ChallengePurpose { PAUSE, TURN_OFF }

/** A contiguous run of characters rendered with mismatch styling. */
data class CharSpan(val start: Int, val length: Int)

/**
 * Per-character mismatch positions of [typed] against [passage]. The runtime
 * decides when a failed Submit reveals these positions; there is no user
 * preference. Consecutive positions merge into one span. Characters typed
 * beyond the end of the passage are mismatches too.
 */
fun mismatchSpans(passage: String, typed: String): List<CharSpan> {
    if (typed.isEmpty()) return emptyList()
    val spans = mutableListOf<CharSpan>()
    var runStart = -1
    val compared = minOf(passage.length, typed.length)
    for (i in 0 until typed.length) {
        val mismatched = i >= compared || typed[i] != passage[i]
        if (mismatched && runStart < 0) runStart = i
        if (!mismatched && runStart >= 0) {
            spans += CharSpan(runStart, i - runStart)
            runStart = -1
        }
    }
    if (runStart >= 0) spans += CharSpan(runStart, typed.length - runStart)
    return spans
}

/**
 * Paste suppression for the typing input (PRD §7.1 hard requirement). Accepts
 * an edit only when it is exactly one keystroke's worth of change — a single
 * inserted character, a single substituted character, or any contiguous
 * deletion. Multi-character insertions and replacements (paste, clipboard
 * chips, share targets) are rejected by returning the previous text. The
 * screen additionally treats a rejected trailing newline as a submit signal.
 */
fun acceptTypingEdit(previous: String, next: String): String {
    if (next == previous) return next
    if (next.length > previous.length + 1) return previous
    if (next.length == previous.length + 1 && isSingleInsertion(previous, next)) return next
    if (next.length == previous.length && differsInAtMostOneChar(previous, next)) return next
    if (next.length < previous.length && isContiguousDeletion(previous, next)) return next
    return previous
}

private fun isSingleInsertion(previous: String, next: String): Boolean {
    var prefix = 0
    while (prefix < previous.length && previous[prefix] == next[prefix]) prefix++
    var suffix = 0
    while (suffix < previous.length - prefix &&
        previous[previous.length - 1 - suffix] == next[next.length - 1 - suffix]
    ) suffix++
    return prefix + suffix == previous.length
}

private fun isContiguousDeletion(previous: String, next: String): Boolean {
    var prefix = 0
    while (prefix < next.length && previous[prefix] == next[prefix]) prefix++
    var suffix = 0
    while (suffix < next.length - prefix &&
        previous[previous.length - 1 - suffix] == next[next.length - 1 - suffix]
    ) suffix++
    return prefix + suffix == next.length
}

private fun differsInAtMostOneChar(previous: String, next: String): Boolean {
    var differences = 0
    for (i in previous.indices) {
        if (previous[i] != next[i] && ++differences > 1) return false
    }
    return true
}

/** Fraction of the passage typed, clamped to 0..1, for the progress bar. */
fun typingProgressChars(typedLength: Int, passageLength: Int): Float =
    if (passageLength <= 0) 0f else (typedLength.toFloat() / passageLength).coerceIn(0f, 1f)

/** Submit is available only after the configured passage length is reached. */
fun canSubmitTyping(typedLength: Int, passageLength: Int): Boolean =
    passageLength > 0 && typedLength >= passageLength

/**
 * Fraction of the wait still remaining, clamped to 0..1. The countdown ring
 * drains as the wait elapses; the runtime supplies both values.
 */
fun countdownProgress(totalSeconds: Int, remainingSeconds: Int): Float =
    if (totalSeconds <= 0) 0f else (remainingSeconds.toFloat() / totalSeconds).coerceIn(0f, 1f)

/** Countdown clock as m:ss — 0:47, 1:00, 12:00 (screens 19/22). */
fun formatClock(totalSeconds: Int): String {
    val seconds = totalSeconds.coerceAtLeast(0)
    return "${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}"
}

/** 1st, 2nd, 3rd, 4th … with the 11th/12th/13th exception, for the walk-away count line. */
fun ordinal(n: Int): String {
    if (n <= 0) return n.toString()
    val suffix = when (n % 100) {
        11, 12, 13 -> "th"
        else -> when (n % 10) {
            1 -> "st"
            2 -> "nd"
            3 -> "rd"
            else -> "th"
        }
    }
    return "$n$suffix"
}

// — copy selectors: one layout, two vocabularies (PRD §6 screens 17–19 vs 22) —

fun challengeTitle(purpose: ChallengePurpose, blockName: String): String =
    if (purpose == ChallengePurpose.TURN_OFF) "Turning off · $blockName" else blockName

fun typingEscapeLabel(purpose: ChallengePurpose): String =
    if (purpose == ChallengePurpose.TURN_OFF) "Leave it on" else "Never mind"

fun delayEscapeLabel(purpose: ChallengePurpose): String =
    if (purpose == ChallengePurpose.TURN_OFF) "Leave it on" else "Never mind"

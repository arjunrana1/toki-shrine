package com.arjunrana.tokishrine.ui.interruption

import org.junit.Assert.assertEquals
import org.junit.Test

// Pure presentation logic for the Phase 5 interruption surfaces (PRD §6–8,
// §11, §17 always-show-typos). Mismatch positions, the paste-blocking input
// filter, progress/clock/ordinal formatting and the pause-vs-turn-off copy
// selectors — no Compose, no randomness.
class InterruptionModelsTest {

    // — mismatchSpans —

    @Test
    fun exactMatchHasNoSpans() {
        assertEquals(emptyList<CharSpan>(), mismatchSpans("meadow copper", "meadow copper"))
    }

    @Test
    fun emptyTypedTextHasNoSpans() {
        assertEquals(emptyList<CharSpan>(), mismatchSpans("meadow copper", ""))
    }

    @Test
    fun transposedLettersMarkOnlyTheTwoWrongPositions() {
        // lantern vs lantren: e/r swapped at indices 4 and 5.
        val spans = mismatchSpans("lantern", "lantren")
        assertEquals(listOf(CharSpan(4, 2)), spans)
    }

    @Test
    fun separatedMistakesStaySeparateSpans() {
        // "aXcdeY" vs "abcdef": index 1 and index 5.
        val spans = mismatchSpans("abcdef", "aXcdeY")
        assertEquals(listOf(CharSpan(1, 1), CharSpan(5, 1)), spans)
    }

    @Test
    fun shorterTypedTextMarksNothingBeyondWhatWasTyped() {
        val spans = mismatchSpans("abcdef", "abcX")
        assertEquals(listOf(CharSpan(3, 1)), spans)
    }

    @Test
    fun typedPastTheEndIsAllMismatch() {
        val spans = mismatchSpans("abc", "abcd")
        assertEquals(listOf(CharSpan(3, 1)), spans)
        // ...and merges with a wrong last typed char before the boundary.
        assertEquals(listOf(CharSpan(2, 2)), mismatchSpans("abc", "abXY"))
    }

    @Test
    fun mismatchComparisonIsCaseSensitive() {
        assertEquals(listOf(CharSpan(0, 1)), mismatchSpans("abc", "Abc"))
    }

    // — acceptTypingEdit (paste suppression) —

    @Test
    fun singleAppendedCharacterIsAccepted() {
        assertEquals("meado", acceptTypingEdit("mead", "meado"))
    }

    @Test
    fun singleCharacterInsertedMidTextIsAccepted() {
        assertEquals("mea dow", acceptTypingEdit("mea ow", "mea dow"))
    }

    @Test
    fun singleCharacterSubstitutionIsAccepted() {
        // Backspace and retype can arrive coalesced as one replacement.
        assertEquals("meaXow", acceptTypingEdit("meadow", "meaXow"))
    }

    @Test
    fun contiguousDeletionsAreAcceptedIncludingClearAll() {
        assertEquals("meadow", acceptTypingEdit("meadow ", "meadow"))
        assertEquals("mea ow", acceptTypingEdit("mea dow", "mea ow"))
        assertEquals("", acceptTypingEdit("meadow", ""))
    }

    @Test
    fun multiCharacterPasteIsRejected() {
        val previous = "meadow "
        assertEquals(previous, acceptTypingEdit(previous, "meadow cinder maple"))
    }

    @Test
    fun pasteOverSelectionIsRejected() {
        val previous = "meadow cinder"
        assertEquals(previous, acceptTypingEdit(previous, "meadow maple velvet"))
    }

    @Test
    fun twoCharacterReplacementIsRejected() {
        val previous = "meadow"
        assertEquals(previous, acceptTypingEdit(previous, "meXYow"))
    }

    @Test
    fun identicalEditPassesThrough() {
        assertEquals("meadow", acceptTypingEdit("meadow", "meadow"))
    }

    // — progress and formatting —

    @Test
    fun typingProgressTracksTypedFractionAndClamps() {
        assertEquals(0f, typingProgressChars(0, 150), 0f)
        assertEquals(0.5f, typingProgressChars(75, 150), 0f)
        assertEquals(1f, typingProgressChars(150, 150), 0f)
        assertEquals(1f, typingProgressChars(160, 150), 0f)
        assertEquals(0f, typingProgressChars(10, 0), 0f)
    }

    @Test
    fun submitRequiresTheConfiguredCharacterCount() {
        assertEquals(false, canSubmitTyping(19, 20))
        assertEquals(true, canSubmitTyping(20, 20))
        assertEquals(true, canSubmitTyping(21, 20))
        assertEquals(false, canSubmitTyping(0, 0))
    }

    @Test
    fun countdownProgressIsRemainingFractionAndClamps() {
        assertEquals(1f, countdownProgress(60, 60), 0f)
        assertEquals(0.5f, countdownProgress(60, 30), 0f)
        assertEquals(0f, countdownProgress(60, 0), 0f)
        assertEquals(1f, countdownProgress(60, 90), 0f)
        assertEquals(0f, countdownProgress(60, -5), 0f)
        assertEquals(0f, countdownProgress(0, 0), 0f)
    }

    @Test
    fun clockFormatsMinutesAndZeroPaddedSeconds() {
        assertEquals("0:00", formatClock(0))
        assertEquals("0:47", formatClock(47))
        assertEquals("1:00", formatClock(60))
        assertEquals("2:05", formatClock(125))
        assertEquals("12:00", formatClock(720))
        assertEquals("0:00", formatClock(-3))
    }

    @Test
    fun ordinalsHandleTeensAndExceptions() {
        assertEquals("1st", ordinal(1))
        assertEquals("2nd", ordinal(2))
        assertEquals("3rd", ordinal(3))
        assertEquals("4th", ordinal(4))
        assertEquals("11th", ordinal(11))
        assertEquals("12th", ordinal(12))
        assertEquals("13th", ordinal(13))
        assertEquals("21st", ordinal(21))
        assertEquals("22nd", ordinal(22))
        assertEquals("23rd", ordinal(23))
        assertEquals("101st", ordinal(101))
        assertEquals("111th", ordinal(111))
        assertEquals("112th", ordinal(112))
    }

    // — copy selectors (screens 17–19 vs 22 share one layout) —

    @Test
    fun copySelectorsDistinguishPauseFromTurnOff() {
        assertEquals("The scroll pit", challengeTitle(ChallengePurpose.PAUSE, "The scroll pit"))
        assertEquals(
            "Turning off · The scroll pit",
            challengeTitle(ChallengePurpose.TURN_OFF, "The scroll pit"),
        )
        assertEquals("Never mind", typingEscapeLabel(ChallengePurpose.PAUSE))
        assertEquals("Leave it on", typingEscapeLabel(ChallengePurpose.TURN_OFF))
        assertEquals("Never mind", delayEscapeLabel(ChallengePurpose.PAUSE))
        assertEquals("Leave it on", delayEscapeLabel(ChallengePurpose.TURN_OFF))
    }
}

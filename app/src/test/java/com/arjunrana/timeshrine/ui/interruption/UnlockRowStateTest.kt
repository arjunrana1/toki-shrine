package com.arjunrana.timeshrine.ui.interruption

import androidx.compose.runtime.saveable.SaverScope
import com.arjunrana.timeshrine.config.AppConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// The gate's "Minutes to unlock" row (11 October §17 unlock-minutes addendum,
// 14 v2, TS-unlock-minutes UM-7): the open/pick rules and the chip stagger.
// The Compose layout and motion themselves are owner-checked (UM-O1).
class UnlockRowStateTest {

    private val saverScope = object : SaverScope { override fun canBeSaved(value: Any) = true }

    @Test
    fun theRowStartsClosedWithNothingPicked() {
        val row = UnlockRowState()
        assertFalse(row.open)
        assertNull(row.picked)
    }

    // "While the row is closed it doesn't respond to taps."
    @Test
    fun aClosedRowAcceptsNoPick() {
        val row = UnlockRowState()
        assertFalse(row.pick(10))
        assertNull(row.picked)
    }

    // Exactly one tap starts the challenge: a second chip, or the same chip
    // again, is ignored once a pick is made.
    @Test
    fun anOpenRowAcceptsExactlyOnePick() {
        val row = UnlockRowState()
        row.toggle()
        assertTrue(row.open)

        assertTrue(row.pick(15))
        assertEquals(15, row.picked)
        assertFalse(row.pick(15))
        assertFalse(row.pick(5))
        assertEquals(15, row.picked)
    }

    // "Tapping the button again folds the row and clears it."
    @Test
    fun foldingTheRowClearsThePick() {
        val row = UnlockRowState()
        row.toggle()
        row.pick(20)

        row.toggle()
        assertFalse(row.open)
        assertNull(row.picked)
        assertFalse(row.pick(5)) // closed again

        row.toggle() // reopened: a fresh pick is accepted, nothing preselected
        assertNull(row.picked)
        assertTrue(row.pick(5))
        assertEquals(5, row.picked)
    }

    // Rotation keeps the row open; a half-made pick is not carried across.
    @Test
    fun onlyTheOpenFlagSurvivesRecreation() {
        val row = UnlockRowState()
        row.toggle()
        row.pick(10)

        val saved = with(UnlockRowState.Saver) { saverScope.save(row) }!!
        val restored = UnlockRowState.Saver.restore(saved)!!

        assertTrue(restored.open)
        assertNull(restored.picked)

        val closed = UnlockRowState.Saver.restore(with(UnlockRowState.Saver) { saverScope.save(UnlockRowState()) }!!)!!
        assertFalse(closed.open)
    }

    // Handoff §1: opening runs left to right at 80/140/200/260 ms, closing
    // runs in reverse at 90/60/30/0 ms.
    @Test
    fun chipStaggerRunsLeftToRightOpeningAndReversedClosing() {
        val count = AppConfig.UNLOCK_MINUTE_CHOICES.size
        assertEquals(4, count)
        assertEquals(listOf(80, 140, 200, 260), (0 until count).map { gateChipDelayMs(it, count, opening = true) })
        assertEquals(listOf(90, 60, 30, 0), (0 until count).map { gateChipDelayMs(it, count, opening = false) })
    }

    // The four chips are the fixed choices the runtime and the repository
    // accept, in ascending order.
    @Test
    fun theChipsAreFiveTenFifteenAndTwentyMinutes() {
        assertEquals(listOf(5, 10, 15, 20), AppConfig.UNLOCK_MINUTE_CHOICES)
    }
}

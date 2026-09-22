package com.arjunrana.tokishrine.ui.screens

import com.arjunrana.tokishrine.data.entity.FrictionType
import com.arjunrana.tokishrine.data.repo.DISABLE_CHARS_CHOICES
import com.arjunrana.tokishrine.data.repo.DISABLE_WAIT_SECONDS_CHOICES
import com.arjunrana.tokishrine.data.repo.PAUSE_CHARS_MAX
import com.arjunrana.tokishrine.data.repo.PAUSE_CHARS_MIN
import com.arjunrana.tokishrine.data.repo.PAUSE_CHARS_STEP
import com.arjunrana.tokishrine.data.repo.PAUSE_MINUTES_MAX
import com.arjunrana.tokishrine.data.repo.PAUSE_MINUTES_MIN
import com.arjunrana.tokishrine.data.repo.PAUSE_MINUTES_STEP
import com.arjunrana.tokishrine.data.repo.PAUSE_WAIT_SECONDS_MAX
import com.arjunrana.tokishrine.data.repo.PAUSE_WAIT_SECONDS_MIN
import com.arjunrana.tokishrine.data.repo.PAUSE_WAIT_SECONDS_STEP
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// Wizard draft model (PRD §7, §17's 19 September amendment): defaults,
// bounds and steps, sheet Reset, per-method retention and the fixed disable
// ladders. Pure state logic — the Room round-trip and UI wiring live in the
// instrumented suite.
class CreateFlowStateTest {

    // — defaults —

    @Test
    fun freshDraftStartsOnApprovedDefaults() {
        val state = CreateFlowState(editBlockId = null)
        assertEquals(FrictionType.TYPING, state.frictionType)
        assertEquals(150, state.typingPauseChars)
        assertEquals(60, state.waitPauseSeconds)
        assertEquals(15, state.pauseMinutes)
        assertEquals(350, state.typingTurnoffChars)
        assertEquals(360, state.waitTurnoffSeconds)
    }

    @Test
    fun entryStepIsClampedToTheWizardRange() {
        assertEquals(1, CreateFlowState(null, 0).entryStep)
        assertEquals(3, CreateFlowState(null, 3).entryStep)
        assertEquals(5, CreateFlowState(null, 9).entryStep)
    }

    // — bounds and steps —

    @Test
    fun debugTypingPauseTwentyToTwoHundredStepTen() {
        val state = CreateFlowState(null)
        state.adjustTypingPause(-PAUSE_CHARS_STEP) // 140
        state.adjustTypingPause(-PAUSE_CHARS_STEP * 100) // debug floor at 20
        assertEquals(PAUSE_CHARS_MIN, state.typingPauseChars)
        state.adjustTypingPause(PAUSE_CHARS_STEP * 100) // ceiling at 200
        assertEquals(PAUSE_CHARS_MAX, state.typingPauseChars)
        state.adjustTypingPause(-PAUSE_CHARS_STEP) // 190: still on a 10 step
        assertEquals(190, state.typingPauseChars)
    }

    @Test
    fun debugWaitPauseTwentyToThreeHundredStepFive() {
        val state = CreateFlowState(null)
        state.adjustWaitPause(-PAUSE_WAIT_SECONDS_STEP * 100)
        assertEquals(PAUSE_WAIT_SECONDS_MIN, state.waitPauseSeconds)
        state.adjustWaitPause(PAUSE_WAIT_SECONDS_STEP * 100)
        assertEquals(PAUSE_WAIT_SECONDS_MAX, state.waitPauseSeconds)
        state.adjustWaitPause(-PAUSE_WAIT_SECONDS_STEP)
        assertEquals(295, state.waitPauseSeconds)
    }

    @Test
    fun pauseMinutesFiveToOneHundredStepFive() {
        val state = CreateFlowState(null)
        state.adjustPauseMinutes(-PAUSE_MINUTES_STEP) // 10
        state.adjustPauseMinutes(-PAUSE_MINUTES_STEP * 100) // floor at 5
        assertEquals(PAUSE_MINUTES_MIN, state.pauseMinutes)
        state.adjustPauseMinutes(PAUSE_MINUTES_STEP * 100)
        assertEquals(PAUSE_MINUTES_MAX, state.pauseMinutes)
        state.adjustPauseMinutes(-PAUSE_MINUTES_STEP)
        assertEquals(95, state.pauseMinutes)
    }

    // — sheet Reset —

    @Test
    fun resetOnTypingRestoresTypingFieldAndPauseOnly() {
        val state = CreateFlowState(null)
        state.adjustTypingPause(PAUSE_CHARS_STEP * 3) // 180
        state.waitPauseSeconds = 180 // waiting draft adjusted separately
        state.typingTurnoffChars = 700 // disable choices must survive Reset
        state.pauseMinutes = 40

        state.resetDetails()

        assertEquals(150, state.typingPauseChars)
        assertEquals(15, state.pauseMinutes)
        assertEquals(180, state.waitPauseSeconds)
        assertEquals(700, state.typingTurnoffChars)
    }

    @Test
    fun resetOnWaitingRestoresWaitFieldAndPauseOnly() {
        val state = CreateFlowState(null)
        state.frictionType = FrictionType.DELAY
        state.adjustWaitPause(PAUSE_WAIT_SECONDS_STEP * 4) // 80
        state.typingPauseChars = 190
        state.pauseMinutes = 40

        state.resetDetails()

        assertEquals(60, state.waitPauseSeconds)
        assertEquals(15, state.pauseMinutes)
        assertEquals(190, state.typingPauseChars)
    }

    // — method switching keeps each method's drafts —

    @Test
    fun switchingMethodsRetainsPerMethodPauseDraftsAndSharedPause() {
        val state = CreateFlowState(null)
        state.adjustTypingPause(PAUSE_CHARS_STEP * 3) // typing 180
        state.frictionType = FrictionType.DELAY
        state.adjustWaitPause(PAUSE_WAIT_SECONDS_STEP * 8) // waiting 100
        state.pauseMinutes = 45 // shared pause follows the single setting

        state.frictionType = FrictionType.TYPING
        assertEquals(180, state.typingPauseChars)
        state.frictionType = FrictionType.DELAY
        assertEquals(100, state.waitPauseSeconds)
        assertEquals(45, state.pauseMinutes)
    }

    // — fixed disable ladders —

    @Test
    fun disableChoicesFollowTheInheritedMethod() {
        val state = CreateFlowState(null)
        assertEquals(listOf(20, 350, 700), state.disableChoices)
        state.frictionType = FrictionType.DELAY
        assertEquals(listOf(20, 360, 720), state.disableChoices)
    }

    @Test
    fun disableMiddleChoiceIsPreselected() {
        val state = CreateFlowState(null)
        assertEquals(1, state.disableChoiceIndex)
        state.frictionType = FrictionType.DELAY
        assertEquals(1, state.disableChoiceIndex)
    }

    @Test
    fun disableSelectionIsPerMethodAndIndependentOfPauseSettings() {
        val state = CreateFlowState(null)
        state.disableChoiceIndex = 2 // typing: 700
        state.adjustPauseMinutes(PAUSE_MINUTES_STEP * 10) // pause 65: unrelated
        state.frictionType = FrictionType.DELAY
        assertEquals(1, state.disableChoiceIndex) // waiting keeps its own rung
        state.disableChoiceIndex = 0 // waiting: 20
        state.frictionType = FrictionType.TYPING
        assertEquals(2, state.disableChoiceIndex)
        assertEquals(700, state.typingTurnoffChars)
        assertEquals(20, state.waitTurnoffSeconds)
        assertEquals(65, state.pauseMinutes)
    }

    @Test
    fun disableLaddersMatchTheApprovedValues() {
        assertEquals(listOf(20, 350, 700), DISABLE_CHARS_CHOICES)
        assertEquals(listOf(20, 360, 720), DISABLE_WAIT_SECONDS_CHOICES)
    }

    // — draft mapping —

    @Test
    fun draftMapsPerMethodFieldsToTheirColumns() {
        val state = CreateFlowState(null)
        state.frictionType = FrictionType.DELAY
        state.adjustWaitPause(PAUSE_WAIT_SECONDS_STEP * 12) // 120
        state.disableChoiceIndex = 2 // waiting 720
        state.pauseMinutes = 25

        val draft = state.draft()
        assertEquals(FrictionType.DELAY, draft.frictionType)
        assertEquals(120, draft.countdownSeconds)
        assertEquals(720, draft.turnoffSeconds)
        // The inactive typing columns still carry the typing method's drafts.
        assertEquals(150, draft.pauseChars)
        assertEquals(350, draft.turnoffChars)
        assertEquals(25, draft.pauseMinutes)
    }

    // — recreation-stable ordered operation owner (WZ-F01 / WZ-F02) —

    @Test
    fun operationSessionSerializesStepsBeforeTerminalSave() = runBlocking {
        val firstStepEntered = CompletableDeferred<Unit>()
        val releaseFirstStep = CompletableDeferred<Unit>()
        val calls = mutableListOf<String>()
        val session = CreateFlowSession(
            scope = this,
            isCreate = true,
            onStart = { calls += "start" },
            onStep = { step ->
                if (step == 1) {
                    firstStepEntered.complete(Unit)
                    releaseFirstStep.await()
                }
                calls += "step:$step"
            },
            onSave = { calls += "save" },
            onAbandon = { calls += "abandon:$it" },
        )
        try {
            assertTrue(session.recordStep(1))
            assertTrue(session.recordStep(2))
            assertTrue(session.requestSave(CreateFlowState(null).draft()))
            firstStepEntered.await()

            assertEquals(listOf("start"), calls)
            releaseFirstStep.complete(Unit)
            assertEquals(
                CreateFlowOutcome.CLOSE,
                withTimeout(1_000) { session.outcome.filterNotNull().first() },
            )
            assertEquals(listOf("start", "step:1", "step:2", "save"), calls)
        } finally {
            session.close()
        }
    }

    @Test
    fun terminalGuardStaysOwnedBySessionWhileScreenIsRecreated() = runBlocking {
        val saveEntered = CompletableDeferred<Unit>()
        val releaseSave = CompletableDeferred<Unit>()
        var saves = 0
        val session = CreateFlowSession(
            scope = this,
            isCreate = false,
            onStart = {},
            onStep = {},
            onSave = {
                saves += 1
                saveEntered.complete(Unit)
                releaseSave.await()
            },
            onAbandon = {},
        )
        try {
            val draft = CreateFlowState(42).draft()
            assertTrue(session.requestSave(draft))
            saveEntered.await()

            // A new composition obtains this same retained session. Neither a
            // second Save nor Back/abandon can enter while its terminal write
            // is suspended.
            assertFalse(session.requestSave(draft))
            assertFalse(session.requestAbandon(step = 5))
            assertEquals(1, saves)

            releaseSave.complete(Unit)
            assertEquals(
                CreateFlowOutcome.CLOSE,
                withTimeout(1_000) { session.outcome.filterNotNull().first() },
            )
            assertEquals(1, saves)
        } finally {
            session.close()
        }
    }
}

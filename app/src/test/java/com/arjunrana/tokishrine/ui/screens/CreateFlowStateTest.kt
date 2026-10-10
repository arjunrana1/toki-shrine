package com.arjunrana.tokishrine.ui.screens

import com.arjunrana.tokishrine.config.AppConfig.PAUSE_CHARS_MAX
import com.arjunrana.tokishrine.config.AppConfig.PAUSE_CHARS_STEP
import com.arjunrana.tokishrine.config.AppConfig.PAUSE_MINUTES_MAX
import com.arjunrana.tokishrine.config.AppConfig.PAUSE_MINUTES_MIN
import com.arjunrana.tokishrine.config.AppConfig.PAUSE_MINUTES_STEP
import com.arjunrana.tokishrine.config.AppConfig.PAUSE_WAIT_SECONDS_MAX
import com.arjunrana.tokishrine.config.AppConfig.PAUSE_WAIT_SECONDS_STEP
import com.arjunrana.tokishrine.data.entity.FrictionType
import com.arjunrana.tokishrine.data.repo.DISABLE_CHARS_CHOICES
import com.arjunrana.tokishrine.data.repo.DISABLE_CHARS_DEFAULT
import com.arjunrana.tokishrine.data.repo.DISABLE_WAIT_SECONDS_CHOICES
import com.arjunrana.tokishrine.data.repo.DISABLE_WAIT_SECONDS_DEFAULT
import com.arjunrana.tokishrine.data.repo.PAUSE_CHARS_DEFAULT
import com.arjunrana.tokishrine.data.repo.PAUSE_CHARS_MIN
import com.arjunrana.tokishrine.data.repo.PAUSE_WAIT_SECONDS_DEFAULT
import com.arjunrana.tokishrine.data.repo.PAUSE_WAIT_SECONDS_MIN
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
// instrumented suite. Variant-specific values (debug owner-testing floors,
// defaults and ladders vs release production) are referenced by name here
// and pinned literally by DebugChallengeValuesTest / ProductionChallenge-
// ValuesTest, so this suite runs unchanged in either variant.
class CreateFlowStateTest {

    // — defaults —

    @Test
    fun freshDraftStartsOnApprovedDefaults() {
        val state = CreateFlowState(editBlockId = null)
        assertEquals(FrictionType.TYPING, state.frictionType)
        assertEquals(PAUSE_CHARS_DEFAULT, state.typingPauseChars)
        assertEquals(PAUSE_WAIT_SECONDS_DEFAULT, state.waitPauseSeconds)
        assertEquals(15, state.pauseMinutes)
        assertEquals(DISABLE_CHARS_DEFAULT, state.typingTurnoffChars)
        assertEquals(DISABLE_WAIT_SECONDS_DEFAULT, state.waitTurnoffSeconds)
    }

    @Test
    fun entryStepIsClampedToTheWizardRange() {
        assertEquals(1, CreateFlowState(null, 0).entryStep)
        assertEquals(3, CreateFlowState(null, 3).entryStep)
        assertEquals(5, CreateFlowState(null, 9).entryStep)
    }

    // — bounds and steps —

    @Test
    fun typingPauseFloorToTwoHundredStepTen() {
        val state = CreateFlowState(null)
        state.adjustTypingPause(-PAUSE_CHARS_STEP)
        state.adjustTypingPause(-PAUSE_CHARS_STEP * 100) // floor at the minimum
        assertEquals(PAUSE_CHARS_MIN, state.typingPauseChars)
        state.adjustTypingPause(PAUSE_CHARS_STEP * 100) // ceiling at 200
        assertEquals(PAUSE_CHARS_MAX, state.typingPauseChars)
        state.adjustTypingPause(-PAUSE_CHARS_STEP) // 190: still on a 10 step
        assertEquals(190, state.typingPauseChars)
    }

    @Test
    fun waitPauseFloorToThreeHundredStepFive() {
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
        state.adjustTypingPause(PAUSE_CHARS_STEP * 3)
        state.waitPauseSeconds = 180 // waiting draft adjusted separately
        state.typingTurnoffChars = 700 // disable choices must survive Reset
        state.pauseMinutes = 40

        state.resetDetails()

        assertEquals(PAUSE_CHARS_DEFAULT, state.typingPauseChars)
        assertEquals(15, state.pauseMinutes)
        assertEquals(180, state.waitPauseSeconds)
        assertEquals(700, state.typingTurnoffChars)
    }

    @Test
    fun resetOnWaitingRestoresWaitFieldAndPauseOnly() {
        val state = CreateFlowState(null)
        state.frictionType = FrictionType.DELAY
        state.adjustWaitPause(PAUSE_WAIT_SECONDS_STEP * 4)
        state.typingPauseChars = 190
        state.pauseMinutes = 40

        state.resetDetails()

        assertEquals(PAUSE_WAIT_SECONDS_DEFAULT, state.waitPauseSeconds)
        assertEquals(15, state.pauseMinutes)
        assertEquals(190, state.typingPauseChars)
    }

    // — method switching keeps each method's drafts —

    @Test
    fun switchingMethodsRetainsPerMethodPauseDraftsAndSharedPause() {
        val state = CreateFlowState(null)
        state.adjustTypingPause(PAUSE_CHARS_STEP * 3) // typing default + 30
        state.frictionType = FrictionType.DELAY
        state.adjustWaitPause(PAUSE_WAIT_SECONDS_STEP * 8) // waiting default + 40
        state.pauseMinutes = 45 // shared pause follows the single setting

        state.frictionType = FrictionType.TYPING
        assertEquals(PAUSE_CHARS_DEFAULT + 30, state.typingPauseChars)
        state.frictionType = FrictionType.DELAY
        assertEquals(PAUSE_WAIT_SECONDS_DEFAULT + 40, state.waitPauseSeconds)
        assertEquals(45, state.pauseMinutes)
    }

    // — fixed disable ladders —

    @Test
    fun disableChoicesFollowTheInheritedMethod() {
        val state = CreateFlowState(null)
        assertEquals(DISABLE_CHARS_CHOICES, state.disableChoices)
        state.frictionType = FrictionType.DELAY
        assertEquals(DISABLE_WAIT_SECONDS_CHOICES, state.disableChoices)
    }

    // Three fixed rungs per method; the variant's default rung is
    // preselected (release: the middle one; debug owner testing: the first).
    @Test
    fun disableDefaultChoiceIsPreselected() {
        assertEquals(3, DISABLE_CHARS_CHOICES.size)
        assertEquals(3, DISABLE_WAIT_SECONDS_CHOICES.size)
        val state = CreateFlowState(null)
        assertEquals(DISABLE_CHARS_CHOICES.indexOf(DISABLE_CHARS_DEFAULT), state.disableChoiceIndex)
        assertTrue(state.disableChoiceIndex >= 0)
        state.frictionType = FrictionType.DELAY
        assertEquals(DISABLE_WAIT_SECONDS_CHOICES.indexOf(DISABLE_WAIT_SECONDS_DEFAULT), state.disableChoiceIndex)
        assertTrue(state.disableChoiceIndex >= 0)
    }

    @Test
    fun disableSelectionIsPerMethodAndIndependentOfPauseSettings() {
        val waitDefaultIndex = DISABLE_WAIT_SECONDS_CHOICES.indexOf(DISABLE_WAIT_SECONDS_DEFAULT)
        // A waiting rung that is neither the typing pick nor the default.
        val waitPick = (0..2).first { it != 2 && it != waitDefaultIndex }
        val state = CreateFlowState(null)
        state.disableChoiceIndex = 2 // typing: 700
        state.adjustPauseMinutes(PAUSE_MINUTES_STEP * 10) // pause 65: unrelated
        state.frictionType = FrictionType.DELAY
        assertEquals(waitDefaultIndex, state.disableChoiceIndex) // waiting keeps its own rung
        state.disableChoiceIndex = waitPick
        state.frictionType = FrictionType.TYPING
        assertEquals(2, state.disableChoiceIndex)
        assertEquals(700, state.typingTurnoffChars)
        assertEquals(DISABLE_WAIT_SECONDS_CHOICES[waitPick], state.waitTurnoffSeconds)
        assertEquals(65, state.pauseMinutes)
    }

    // The literal ladder values are pinned per variant by the guard tests.
    @Test
    fun disableWaitCopyNamesSecondsBelowAMinute() {
        assertEquals("Wait for 20 seconds.", disableCardCopy(FrictionType.DELAY, 0, 20).second)
        assertEquals("Wait for 3 minutes.", disableCardCopy(FrictionType.DELAY, 0, 180).second)
        assertEquals("Wait for 6 minutes.", disableCardCopy(FrictionType.DELAY, 1, 360).second)
        assertEquals(
            "Wait for 12 minutes. For blocks you don't trust yourself with.",
            disableCardCopy(FrictionType.DELAY, 2, 720).second,
        )
        assertEquals("Type a bit" to "Type 20 characters. Random words.", disableCardCopy(FrictionType.TYPING, 0, 20))
    }

    // — draft mapping —

    @Test
    fun draftMapsPerMethodFieldsToTheirColumns() {
        val state = CreateFlowState(null)
        state.frictionType = FrictionType.DELAY
        state.adjustWaitPause(PAUSE_WAIT_SECONDS_STEP * 12) // default + 60
        state.disableChoiceIndex = 2 // waiting 720
        state.pauseMinutes = 25

        val draft = state.draft()
        assertEquals(FrictionType.DELAY, draft.frictionType)
        assertEquals(PAUSE_WAIT_SECONDS_DEFAULT + 60, draft.countdownSeconds)
        assertEquals(720, draft.turnoffSeconds)
        // The inactive typing columns still carry the typing method's drafts.
        assertEquals(PAUSE_CHARS_DEFAULT, draft.pauseChars)
        assertEquals(DISABLE_CHARS_DEFAULT, draft.turnoffChars)
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

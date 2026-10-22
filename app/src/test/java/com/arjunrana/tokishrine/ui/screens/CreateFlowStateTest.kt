package com.arjunrana.tokishrine.ui.screens

import androidx.compose.runtime.saveable.SaverScope
import com.arjunrana.tokishrine.config.AppConfig.PAUSE_CHARS_DEFAULT
import com.arjunrana.tokishrine.config.AppConfig.PAUSE_CHARS_MAX
import com.arjunrana.tokishrine.config.AppConfig.PAUSE_CHARS_MIN
import com.arjunrana.tokishrine.config.AppConfig.PAUSE_CHARS_STEP
import com.arjunrana.tokishrine.config.AppConfig.PAUSE_WAIT_SECONDS_DEFAULT
import com.arjunrana.tokishrine.config.AppConfig.PAUSE_WAIT_SECONDS_MAX
import com.arjunrana.tokishrine.config.AppConfig.PAUSE_WAIT_SECONDS_MIN
import com.arjunrana.tokishrine.config.AppConfig.PAUSE_WAIT_SECONDS_STEP
import com.arjunrana.tokishrine.data.apps.AppEntry
import com.arjunrana.tokishrine.data.entity.FrictionType
import com.arjunrana.tokishrine.data.repo.DISABLE_CHARS_CHOICES
import com.arjunrana.tokishrine.data.repo.DISABLE_CHARS_DEFAULT
import com.arjunrana.tokishrine.data.repo.DISABLE_WAIT_SECONDS_CHOICES
import com.arjunrana.tokishrine.data.repo.DISABLE_WAIT_SECONDS_DEFAULT
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// Wizard draft model (PRD §7, §17's 19 September amendment and the 11 October
// unlock-minutes addendum): defaults, bounds and steps, the method choice on
// step 3, per-method retention and the fixed disable ladders. Pure state logic — the Room round-trip and UI wiring live in the
// instrumented suite. Pause values are the same in every build (11 October
// unlock-minutes addendum) and pinned literally below; the variant disable
// ladders are referenced by name and pinned by DebugChallengeValuesTest /
// ProductionChallengeValuesTest, so this suite runs unchanged in either
// variant. The draft has no pause length any more.
class CreateFlowStateTest {

    // — defaults —

    @Test
    fun freshDraftStartsOnApprovedDefaults() {
        val state = CreateFlowState(editBlockId = null)
        assertEquals(FrictionType.TYPING, state.frictionType)
        assertEquals(PAUSE_CHARS_DEFAULT, state.typingPauseChars)
        assertEquals(PAUSE_WAIT_SECONDS_DEFAULT, state.waitPauseSeconds)
        // Supersedes 20 characters / 60 s (UM-A): every build now starts
        // typing at 40 characters and waiting at 30 s (CV-1, 23 October).
        assertEquals(40, state.typingPauseChars)
        assertEquals(30, state.waitPauseSeconds)
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

    // Supersedes typingPauseTwentyToOneFiftyStepTen (CV-1, 23 October): 30..150 / 10.
    @Test
    fun typingPauseThirtyToOneFiftyStepTen() {
        val state = CreateFlowState(null)
        state.adjustTypingPause(-PAUSE_CHARS_STEP)
        state.adjustTypingPause(-PAUSE_CHARS_STEP * 100) // floor at the minimum
        assertEquals(30, PAUSE_CHARS_MIN)
        assertEquals(PAUSE_CHARS_MIN, state.typingPauseChars)
        state.adjustTypingPause(PAUSE_CHARS_STEP * 100) // ceiling at 150
        assertEquals(150, PAUSE_CHARS_MAX)
        assertEquals(PAUSE_CHARS_MAX, state.typingPauseChars)
        state.adjustTypingPause(-PAUSE_CHARS_STEP) // 140: still on a 10 step
        assertEquals(140, state.typingPauseChars)
    }

    // Supersedes waitPauseTwentyToTwoFortyStepTen (CV-1, 23 October): 30..120 s / 10.
    @Test
    fun waitPauseThirtyToOneTwentyStepTen() {
        val state = CreateFlowState(null)
        state.adjustWaitPause(-PAUSE_WAIT_SECONDS_STEP * 100)
        assertEquals(30, PAUSE_WAIT_SECONDS_MIN)
        assertEquals(PAUSE_WAIT_SECONDS_MIN, state.waitPauseSeconds)
        state.adjustWaitPause(PAUSE_WAIT_SECONDS_STEP * 100)
        assertEquals(120, PAUSE_WAIT_SECONDS_MAX)
        assertEquals(PAUSE_WAIT_SECONDS_MAX, state.waitPauseSeconds)
        state.adjustWaitPause(-PAUSE_WAIT_SECONDS_STEP)
        assertEquals(110, state.waitPauseSeconds)
    }

    // Supersedes the old 5..100-minute pause-length test: the pause length
    // left the block (UM-A), and editing clamps stored values from older
    // ranges into the current ones, on the step grid, for both methods.
    @Test
    fun editPrefillClampsStoredPauseValuesIntoRangeAndOntoTheStep() {
        val state = CreateFlowState(editBlockId = 3)
        state.prefillPause(storedChars = 200, storedWaitSeconds = 240) // old maxima
        assertEquals(150, state.typingPauseChars)
        assertEquals(120, state.waitPauseSeconds)
        state.prefillPause(storedChars = 20, storedWaitSeconds = 20) // old floors, below the new
        assertEquals(30, state.typingPauseChars)
        assertEquals(30, state.waitPauseSeconds)
        state.prefillPause(storedChars = 120, storedWaitSeconds = 65) // old 5 s step
        assertEquals(120, state.typingPauseChars)
        assertEquals(60, state.waitPauseSeconds)
        state.prefillPause(storedChars = 100, storedWaitSeconds = 90) // in range: unchanged
        assertEquals(100, state.typingPauseChars)
        assertEquals(90, state.waitPauseSeconds)
        // The clamped draft is always a valid save.
        val draft = state.draft()
        assertTrue(draft.pauseChars in PAUSE_CHARS_MIN..PAUSE_CHARS_MAX && draft.pauseChars % PAUSE_CHARS_STEP == 0)
        assertTrue(
            draft.countdownSeconds in PAUSE_WAIT_SECONDS_MIN..PAUSE_WAIT_SECONDS_MAX &&
                draft.countdownSeconds % PAUSE_WAIT_SECONDS_STEP == 0,
        )
    }

    // CV-5: a stored disable value that is off the current ladder moves to
    // the nearest rung (a tie goes to the harder one), so editing an older
    // block always saves; a value on the ladder is kept.
    @Test
    fun editPrefillSnapsStoredDisableValuesOntoTheLadder() {
        val state = CreateFlowState(editBlockId = 3)
        state.prefillDisable(storedChars = DISABLE_CHARS_CHOICES[1], storedWaitSeconds = DISABLE_WAIT_SECONDS_CHOICES[1])
        assertEquals(DISABLE_CHARS_CHOICES[1], state.typingTurnoffChars)
        assertEquals(DISABLE_WAIT_SECONDS_CHOICES[1], state.waitTurnoffSeconds)
        state.prefillDisable(storedChars = 10_000, storedWaitSeconds = 10_000) // far above
        assertEquals(DISABLE_CHARS_CHOICES.last(), state.typingTurnoffChars)
        assertEquals(DISABLE_WAIT_SECONDS_CHOICES.last(), state.waitTurnoffSeconds)
        state.prefillDisable(storedChars = 1, storedWaitSeconds = 1) // below the first rung
        assertEquals(DISABLE_CHARS_CHOICES.first(), state.typingTurnoffChars)
        assertEquals(DISABLE_WAIT_SECONDS_CHOICES.first(), state.waitTurnoffSeconds)
        // Exactly between the two upper rungs: the harder one wins.
        val charsTie = (DISABLE_CHARS_CHOICES[1] + DISABLE_CHARS_CHOICES[2]) / 2
        val waitTie = (DISABLE_WAIT_SECONDS_CHOICES[1] + DISABLE_WAIT_SECONDS_CHOICES[2]) / 2
        state.prefillDisable(storedChars = charsTie, storedWaitSeconds = waitTie)
        assertEquals(DISABLE_CHARS_CHOICES[2], state.typingTurnoffChars)
        assertEquals(DISABLE_WAIT_SECONDS_CHOICES[2], state.waitTurnoffSeconds)
        // The snapped draft is always on the ladder, so it saves.
        val draft = state.draft()
        assertTrue(draft.turnoffChars in DISABLE_CHARS_CHOICES)
        assertTrue(draft.turnoffSeconds in DISABLE_WAIT_SECONDS_CHOICES)
    }

    // — inline method cards (09 v3) —

    // Supersedes the sheet Reset tests resetOnTypingRestoresTypingFieldOnly and
    // resetOnWaitingRestoresWaitFieldOnly (UM-B): the details sheet and its
    // Reset are gone, so there is nothing to reset.
    @Test
    fun aNewDraftHasNoMethodChosenAndCannotLeaveStepThree() {
        val state = CreateFlowState(editBlockId = null)
        state.apps.add(AppEntry("com.example.app", "Example", null))
        assertFalse(state.methodChosen)
        assertFalse(state.methodStepComplete)

        state.chooseMethod(FrictionType.DELAY)
        assertTrue(state.methodChosen)
        assertEquals(FrictionType.DELAY, state.frictionType)
        assertTrue(state.methodStepComplete)
    }

    @Test
    fun stepThreeStaysBlockedWithoutTargetsEvenAfterAPick() {
        val state = CreateFlowState(editBlockId = null)
        state.chooseMethod(FrictionType.TYPING)
        assertFalse(state.methodStepComplete) // PRD §17 R8: no targets, no advance
        state.sites.add("example.com")
        assertTrue(state.methodStepComplete)
    }

    // Tapping a card picks that method and nothing else: neither method's
    // value, nor the step-4 choices, move.
    @Test
    fun choosingAMethodKeepsEveryDraftValue() {
        val state = CreateFlowState(null)
        state.adjustTypingPause(PAUSE_CHARS_STEP * 3)
        state.adjustWaitPause(PAUSE_WAIT_SECONDS_STEP * 5)
        state.typingTurnoffChars = DISABLE_CHARS_CHOICES[2]

        state.chooseMethod(FrictionType.DELAY)
        state.chooseMethod(FrictionType.TYPING)
        state.chooseMethod(FrictionType.TYPING) // tapping the open card again is harmless

        assertEquals(PAUSE_CHARS_DEFAULT + 30, state.typingPauseChars)
        assertEquals(PAUSE_WAIT_SECONDS_DEFAULT + 50, state.waitPauseSeconds)
        assertEquals(DISABLE_CHARS_CHOICES[2], state.typingTurnoffChars)
        assertEquals(FrictionType.TYPING, state.frictionType)
        assertTrue(state.methodChosen)
    }

    // — method switching keeps each method's drafts —

    @Test
    fun switchingMethodsRetainsPerMethodPauseDrafts() {
        val state = CreateFlowState(null)
        state.adjustTypingPause(PAUSE_CHARS_STEP * 3) // typing default + 30
        state.frictionType = FrictionType.DELAY
        state.adjustWaitPause(PAUSE_WAIT_SECONDS_STEP * 8) // waiting default + 80

        state.frictionType = FrictionType.TYPING
        assertEquals(PAUSE_CHARS_DEFAULT + 30, state.typingPauseChars)
        state.frictionType = FrictionType.DELAY
        assertEquals(PAUSE_WAIT_SECONDS_DEFAULT + 80, state.waitPauseSeconds)
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
        state.disableChoiceIndex = 2 // typing: the hardest rung
        state.adjustTypingPause(PAUSE_CHARS_STEP * 3) // typing pause + 30: unrelated
        state.frictionType = FrictionType.DELAY
        assertEquals(waitDefaultIndex, state.disableChoiceIndex) // waiting keeps its own rung
        state.disableChoiceIndex = waitPick
        state.frictionType = FrictionType.TYPING
        assertEquals(2, state.disableChoiceIndex)
        assertEquals(DISABLE_CHARS_CHOICES[2], state.typingTurnoffChars)
        assertEquals(DISABLE_WAIT_SECONDS_CHOICES[waitPick], state.waitTurnoffSeconds)
        assertEquals(PAUSE_CHARS_DEFAULT + 30, state.typingPauseChars)
    }

    // The literal ladder values are pinned per variant by the guard tests.
    @Test
    fun disableWaitCopyNamesSecondsBelowAMinute() {
        assertEquals("Wait for 20 seconds.", disableCardCopy(FrictionType.DELAY, 0, 20).second)
        assertEquals("Wait for 1 minute.", disableCardCopy(FrictionType.DELAY, 0, 60).second) // CV-4: singular
        assertEquals("Wait for 2 minutes.", disableCardCopy(FrictionType.DELAY, 1, 120).second)
        assertEquals(
            "Wait for 4 minutes. For blocks you don't trust yourself with.",
            disableCardCopy(FrictionType.DELAY, 2, 240).second,
        )
        assertEquals("Type a bit" to "Type 20 characters. Random words.", disableCardCopy(FrictionType.TYPING, 0, 20))
    }

    // — draft mapping —

    @Test
    fun draftMapsPerMethodFieldsToTheirColumns() {
        val state = CreateFlowState(null)
        state.frictionType = FrictionType.DELAY
        state.adjustWaitPause(PAUSE_WAIT_SECONDS_STEP * 6) // default + 60
        state.disableChoiceIndex = 2 // waiting: the hardest rung

        val draft = state.draft()
        assertEquals(FrictionType.DELAY, draft.frictionType)
        assertEquals(PAUSE_WAIT_SECONDS_DEFAULT + 60, draft.countdownSeconds)
        assertEquals(DISABLE_WAIT_SECONDS_CHOICES[2], draft.turnoffSeconds)
        // The inactive typing columns still carry the typing method's drafts.
        assertEquals(PAUSE_CHARS_DEFAULT, draft.pauseChars)
        assertEquals(DISABLE_CHARS_DEFAULT, draft.turnoffChars)
    }

    // The position-indexed saver lost its pause-length slot (UM-A) and its
    // details-sheet slot (UM-B) and gained methodChosen: every field, flowId
    // last, must still come back in place. The slot count is pinned so a
    // future change has to touch this test.
    @Test
    fun saverRoundTripKeepsEveryDraftFieldAfterTheSlotRework() {
        val state = CreateFlowState(editBlockId = 9, entryStep = 3, flowId = "flow-1")
        state.step = 4
        state.showSearch = true
        state.appsTab = false
        state.name = "Social"
        state.frictionType = FrictionType.DELAY
        state.methodChosen = true
        state.typingPauseChars = 140
        state.waitPauseSeconds = 110 // in the 30..120 range (CV-1)
        state.typingTurnoffChars = DISABLE_CHARS_CHOICES[2]
        state.waitTurnoffSeconds = DISABLE_WAIT_SECONDS_CHOICES[0]
        state.prefilled = true
        state.saveRejected = true
        state.apps.add(AppEntry("com.example.app", "Example", null))
        state.sites.add("example.com")

        val scope = object : SaverScope { override fun canBeSaved(value: Any) = true }
        val saved = with(CreateFlowStateSaver) { scope.save(state) }!!
        assertEquals(17, saved.size)
        val restored = CreateFlowStateSaver.restore(saved)!!

        assertEquals(9L, restored.editBlockId)
        assertEquals(3, restored.entryStep)
        assertEquals("flow-1", restored.flowId)
        assertEquals(4, restored.step)
        assertTrue(restored.showSearch)
        assertFalse(restored.appsTab)
        assertEquals("Social", restored.name)
        assertEquals(FrictionType.DELAY, restored.frictionType)
        assertTrue(restored.methodChosen)
        assertEquals(140, restored.typingPauseChars)
        assertEquals(110, restored.waitPauseSeconds)
        assertEquals(DISABLE_CHARS_CHOICES[2], restored.typingTurnoffChars)
        assertEquals(DISABLE_WAIT_SECONDS_CHOICES[0], restored.waitTurnoffSeconds)
        assertTrue(restored.prefilled)
        assertTrue(restored.saveRejected)
        assertEquals(listOf("com.example.app" to "Example"), restored.apps.map { it.packageName to it.label })
        assertEquals(listOf("example.com"), restored.sites.toList())
    }

    // A fresh create draft saves and restores with no method chosen: the
    // recreated screen must not quietly select a card for the user.
    @Test
    fun saverRoundTripKeepsAnUnchosenMethodUnchosen() {
        val state = CreateFlowState(editBlockId = null, flowId = "flow-2")
        val scope = object : SaverScope { override fun canBeSaved(value: Any) = true }
        val saved = with(CreateFlowStateSaver) { scope.save(state) }!!
        val restored = CreateFlowStateSaver.restore(saved)!!
        assertFalse(restored.methodChosen)
        assertFalse(restored.methodStepComplete)
    }

    // The previous build's shape (17 slots: the sheet flag at slot 4, flowId
    // at 16, no methodChosen) shifts every later slot, so a cast fails and the
    // flow starts fresh instead of restoring scrambled fields.
    @Test
    fun saverFromTheDetailsSheetShapeFallsBackToAFreshState() {
        val staleShape = arrayListOf<Any>(
            -1L, 3, 1, false, false, true, "Social", "TYPING",
            140, 230, 350, 360, true, false,
            ArrayList<String>(), ArrayList<String>(), "flow-3",
        )
        assertNull(CreateFlowStateSaver.restore(staleShape))
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

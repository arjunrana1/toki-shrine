package com.arjunrana.tokishrine.challenge

import com.arjunrana.tokishrine.data.entity.FrictionType
import com.arjunrana.tokishrine.data.db.BlockWithContents
import com.arjunrana.tokishrine.data.entity.Block
import com.arjunrana.tokishrine.data.entity.BlockedApp
import com.arjunrana.tokishrine.data.entity.BlockedSite
import com.arjunrana.tokishrine.ui.interruption.ChallengePurpose
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChallengeRuntimeTest {

    @Test
    fun mismatchKeepsPassageAndTextAndAllowsUnlimitedRetries() {
        val runtime = typingRuntime()
        runtime.enterChallenge(0)
        repeat(20) { attempt ->
            val wrong = "wrong ${attempt.toString().padStart(4, '0')}"
            runtime.updateTypedText(wrong)
            val effect = runtime.submitTyping(attempt.toLong())
            assertTrue(effect is ChallengeEffect.TypingMismatch)
            assertEquals(wrong, runtime.view(attempt.toLong()).typedText)
            assertTrue(runtime.view(attempt.toLong()).showTypingMismatches)
            assertEquals("alpha beta", runtime.config.passage)
        }

        runtime.updateTypedText("alpha beta")
        val completion = runtime.submitTyping(25) as ChallengeEffect.PersistCompletion
        assertEquals(21, completion.request.attempts)
    }

    @Test
    fun exactTypingIsTheOnlyCompletionPath() {
        val runtime = typingRuntime()
        runtime.enterChallenge(10)
        runtime.updateTypedText("alpha bet")
        assertNull(runtime.submitTyping(20))
        runtime.updateTypedText("alpha betx")
        assertTrue(runtime.submitTyping(25) is ChallengeEffect.TypingMismatch)
        runtime.updateTypedText("alpha beta")
        val effect = runtime.submitTyping(30)
        assertTrue(effect is ChallengeEffect.PersistCompletion)
        assertEquals(ChallengePhase.COMMITTING, runtime.view(30).phase)
    }

    @Test
    fun countdownAccruesOnlyAcrossVisibleSegments() {
        val runtime = waitingRuntime(seconds = 10)
        runtime.beginTurnOff(0)
        runtime.onVisible(0)
        assertNull(runtime.tick(5_000))
        assertEquals(5, runtime.view(5_000).remainingSeconds)

        runtime.onConfigurationHidden(5_000)
        assertNull(runtime.tick(100_000))
        assertEquals(5, runtime.view(100_000).remainingSeconds)

        runtime.onVisible(100_000)
        assertTrue(runtime.tick(105_000) is ChallengeEffect.PersistCompletion)
    }

    @Test
    fun backgroundingResetsWaitingWithoutTerminalOutcomeAndInvalidatesOldTick() {
        val runtime = waitingRuntime(seconds = 10)
        runtime.beginTurnOff(0)
        runtime.onVisible(0)
        val oldGeneration = runtime.currentGeneration()

        runtime.onBackgrounded()

        assertEquals(ChallengePhase.ACTIVE, runtime.view(4_000).phase)
        assertEquals(10, runtime.view(4_000).remainingSeconds)
        assertNull(runtime.tick(20_000, oldGeneration))
        runtime.onVisible(20_000)
        assertTrue(runtime.tick(30_000) is ChallengeEffect.PersistCompletion)
    }

    @Test
    fun backgroundingPreservesTypingPassageAndEnteredText() {
        val runtime = typingRuntime()
        runtime.enterChallenge(0)
        runtime.updateTypedText("alpha")

        runtime.onBackgrounded()

        assertEquals(ChallengePhase.ACTIVE, runtime.view(1_000).phase)
        assertEquals("alpha beta", runtime.config.passage)
        assertEquals("alpha", runtime.view(1_000).typedText)
    }

    @Test
    fun pauseEscapeIsWalkAwayButTurnOffEscapeIsSeparate() {
        val pause = typingRuntime()
        pause.enterChallenge(0)
        val walkAway = pause.escape(1) as ChallengeEffect.WalkAway
        assertEquals("typing", walkAway.source)

        val turnOff = waitingRuntime(10)
        turnOff.beginTurnOff(0)
        turnOff.onVisible(0)
        assertTrue(turnOff.escape(2_000) is ChallengeEffect.TurnOffAbandoned)
    }

    @Test
    fun explicitEscapeFirstInvalidatesLateCompletionAndStaleTick() {
        val runtime = waitingRuntime(seconds = 1)
        runtime.beginTurnOff(0)
        runtime.onVisible(0)
        val generation = runtime.currentGeneration()
        assertNotNull(runtime.escape(500))
        assertNull(runtime.tick(2_000, generation))
        assertNull(runtime.commitSucceeded(generation))
    }

    @Test
    fun completionFirstOwnsRaceAndIgnoresBackgrounding() {
        val runtime = typingRuntime()
        runtime.enterChallenge(0)
        runtime.updateTypedText("alpha beta")
        val completion = runtime.submitTyping(10) as ChallengeEffect.PersistCompletion
        runtime.onBackgrounded()
        assertEquals(ChallengePhase.COMMITTING, runtime.view(11).phase)
        assertTrue(runtime.commitSucceeded(completion.request.token) is ChallengeTerminalResult.PauseRequested)
    }

    @Test
    fun failedWriteReturnsToActiveAndRetryGetsFreshToken() {
        val runtime = typingRuntime()
        runtime.enterChallenge(0)
        runtime.updateTypedText("alpha beta")
        val first = runtime.submitTyping(10) as ChallengeEffect.PersistCompletion
        assertTrue(runtime.commitFailed(first.request.token, 11))
        val second = runtime.submitTyping(12) as ChallengeEffect.PersistCompletion
        assertNotEquals(first.request.token, second.request.token)
        assertNull(runtime.commitSucceeded(first.request.token))
        assertNotNull(runtime.commitSucceeded(second.request.token))
    }

    @Test
    fun recreationRestoresLiveStateWithoutAnotherStartedEffect() {
        val original = typingRuntime()
        original.enterChallenge(0)
        original.updateTypedText("alpha")

        val restored = ChallengeRuntime(original.config, original.snapshot())

        assertEquals(ChallengePhase.ACTIVE, restored.view(10).phase)
        assertEquals("alpha", restored.view(10).typedText)
        assertNull(restored.enterChallenge(10))
    }

    @Test
    fun mismatchMarksAppearOnlyAfterSubmitAndHideOnNextEdit() {
        val runtime = typingRuntime()
        runtime.enterChallenge(0)
        runtime.updateTypedText("alpha betx")
        assertEquals(false, runtime.view(1).showTypingMismatches)

        assertTrue(runtime.submitTyping(2) is ChallengeEffect.TypingMismatch)
        assertTrue(runtime.view(2).showTypingMismatches)

        runtime.updateTypedText("alpha beta")
        assertEquals(false, runtime.view(3).showTypingMismatches)
    }

    @Test
    fun duplicateCompletionCallbackIsAtMostOnce() {
        val runtime = typingRuntime()
        runtime.enterChallenge(0)
        runtime.updateTypedText("alpha beta")
        val completion = runtime.submitTyping(10) as ChallengeEffect.PersistCompletion
        assertNotNull(runtime.commitSucceeded(completion.request.token))
        assertNull(runtime.commitSucceeded(completion.request.token))
    }

    @Test
    fun recreationDuringCommitReissuesSameStableCompletion() {
        val original = typingRuntime()
        original.enterChallenge(0)
        original.updateTypedText("alpha beta")
        val first = original.submitTyping(10) as ChallengeEffect.PersistCompletion

        val restored = ChallengeRuntime(original.config, original.snapshot())
        val retried = restored.resumePendingCompletion(20)!!

        assertEquals(first.request.token, retried.request.token)
        assertEquals(first.request.sessionId, retried.request.sessionId)
        assertNotNull(restored.commitSucceeded(retried.request.token))
    }

    @Test
    fun contentSelectorProducesExactRequestedLengthAndIndependentSelections() {
        val first = ChallengeContentSelector(kotlin.random.Random(1)).passage(20)
        val second = ChallengeContentSelector(kotlin.random.Random(2)).passage(20)
        assertEquals(20, first.length)
        assertEquals(20, second.length)
        assertNotEquals(first, second)
        assertTrue(first.split(' ').all { it in ChallengeContentSelector.WORDS })
    }

    @Test
    fun ownerApprovedCopyAndAssetPoolsAreComplete() {
        assertEquals(11, ChallengeContentSelector.HUMOUR_LINES.size)
        assertEquals("Naah bruh", ChallengeContentSelector.HUMOUR_LINES[6])
        assertEquals("Let's give it a rest", ChallengeContentSelector.HUMOUR_LINES.last())
        assertEquals(7, ChallengeContentSelector.HEADLINE_TEMPLATES.size)
        assertTrue(ChallengeContentSelector.HEADLINE_TEMPLATES.all { "{target}" in it })
        assertEquals(11, ChallengeContentSelector.BACKGROUNDS.size)
        assertTrue(ChallengeContentSelector(kotlin.random.Random(1)).headline("Ajio").contains("Ajio"))
    }

    @Test
    fun launchValidationRejectsStaleDisabledAndMismatchedInputs() {
        val block = BlockWithContents(
            block = Block(
                id = 7,
                name = "Social",
                frictionType = FrictionType.TYPING,
                pauseMinutes = 15,
                pauseChars = 150,
                turnoffChars = 350,
                countdownSeconds = 60,
                turnoffSeconds = 360,
                enabled = true,
            ),
            apps = listOf(BlockedApp(blockId = 7, packageName = "com.example.target")),
            sites = listOf(BlockedSite(blockId = 7, domain = "example.com")),
        )

        assertTrue(isValidDetectionLaunch(block, "app", "com.example.target"))
        assertTrue(isValidDetectionLaunch(block, "site", "news.example.com"))
        assertEquals(false, isValidDetectionLaunch(block, "app", "com.other"))
        assertEquals(false, isValidDetectionLaunch(block, "site", "notexample.com"))
        assertEquals(false, isValidDetectionLaunch(block.copy(block = block.block.copy(enabled = false)), "app", "com.example.target"))
    }

    private fun typingRuntime() = ChallengeRuntime(
        ChallengeConfig(
            sessionId = "session",
            blockId = 7,
            purpose = ChallengePurpose.PAUSE,
            method = FrictionType.TYPING,
            passage = "alpha beta",
            pauseMinutes = 15,
            target = "com.example.target",
            targetType = "app",
        ),
    )

    private fun waitingRuntime(seconds: Int) = ChallengeRuntime(
        ChallengeConfig(
            sessionId = "session",
            blockId = 7,
            purpose = ChallengePurpose.TURN_OFF,
            method = FrictionType.DELAY,
            totalSeconds = seconds,
        ),
    )
}

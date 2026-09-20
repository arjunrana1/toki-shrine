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
            runtime.updateTypedText("wrong $attempt")
            val effect = runtime.submitTyping(attempt.toLong())
            assertTrue(effect is ChallengeEffect.TypingMismatch)
            assertEquals("wrong $attempt", runtime.view(attempt.toLong()).typedText)
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
        assertTrue(runtime.submitTyping(20) is ChallengeEffect.TypingMismatch)
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
    fun appSwitchAndScreenOffAreAbandonmentsNotWalkAways() {
        listOf(AbandonReason.APP_SWITCH, AbandonReason.SCREEN_OFF).forEach { reason ->
            val runtime = waitingRuntime(seconds = 10)
            runtime.beginTurnOff(0)
            runtime.onVisible(0)
            val effect = runtime.abandon(reason, 4_000) as ChallengeEffect.Abandoned
            assertEquals(reason, effect.reason)
            assertEquals(40, effect.progressPct)
            assertEquals(ChallengePhase.TERMINAL, runtime.view(4_000).phase)
        }
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
    fun cancellationFirstInvalidatesLateCompletionAndStaleTick() {
        val runtime = waitingRuntime(seconds = 1)
        runtime.beginTurnOff(0)
        runtime.onVisible(0)
        val generation = runtime.currentGeneration()
        assertNotNull(runtime.abandon(AbandonReason.APP_SWITCH, 500))
        assertNull(runtime.tick(2_000, generation))
        assertNull(runtime.commitSucceeded(generation))
    }

    @Test
    fun completionFirstOwnsRaceAndIgnoresCancellation() {
        val runtime = typingRuntime()
        runtime.enterChallenge(0)
        runtime.updateTypedText("alpha beta")
        val completion = runtime.submitTyping(10) as ChallengeEffect.PersistCompletion
        assertNull(runtime.abandon(AbandonReason.APP_SWITCH, 11))
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
        val first = ChallengeContentSelector(kotlin.random.Random(1)).passage(220)
        val second = ChallengeContentSelector(kotlin.random.Random(2)).passage(220)
        assertEquals(220, first.length)
        assertEquals(220, second.length)
        assertNotEquals(first, second)
        assertTrue(first.split(' ').all { it in ChallengeContentSelector.WORDS })
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

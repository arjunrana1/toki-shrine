package com.arjunrana.tokishrine.challenge

import com.arjunrana.tokishrine.config.AppConfig
import com.arjunrana.tokishrine.data.entity.FrictionType
import com.arjunrana.tokishrine.data.db.BlockWithContents
import com.arjunrana.tokishrine.data.entity.Block
import com.arjunrana.tokishrine.data.entity.BlockedApp
import com.arjunrana.tokishrine.data.entity.BlockedSite
import com.arjunrana.tokishrine.data.repo.EventRepository
import com.arjunrana.tokishrine.ui.interruption.ChallengePurpose
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

        runtime.onBackgrounded(1_000)

        assertEquals(ChallengePhase.ACTIVE, runtime.view(4_000).phase)
        assertEquals(10, runtime.view(4_000).remainingSeconds)
        assertNull(runtime.tick(14_000, oldGeneration))
        assertNull(runtime.onReturned(14_000))
        runtime.onVisible(14_000)
        assertTrue(runtime.tick(24_000) is ChallengeEffect.PersistCompletion)
    }

    // P7-F4 supersedes the earlier "typed text survives backgrounding":
    // the passage stays, the text is cleared when the user comes back.
    @Test
    fun returnWithinLimitKeepsSessionAndPassageButClearsTypedText() {
        val runtime = typingRuntime()
        runtime.enterChallenge(0)
        runtime.updateTypedText("alpha betx")
        assertTrue(runtime.submitTyping(500) is ChallengeEffect.TypingMismatch)

        runtime.onBackgrounded(1_000)
        assertEquals("alpha betx", runtime.view(2_000).typedText)
        assertNull(runtime.onReturned(15_999))

        val view = runtime.view(16_000)
        assertEquals(ChallengePhase.ACTIVE, view.phase)
        assertEquals("alpha beta", runtime.config.passage)
        assertEquals("", view.typedText)
        assertEquals(false, view.showTypingMismatches)
        assertNull(runtime.awayRemainingMs(16_000))
        runtime.updateTypedText("alpha beta")
        val completion = runtime.submitTyping(17_000) as ChallengeEffect.PersistCompletion
        assertEquals(2, completion.request.attempts)
        assertEquals("session", completion.request.sessionId)
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
        // The button escape is not an auto-nope: it returns to the block (P7-F16).
        assertFalse((waitingRuntime(10).apply { beginTurnOff(0); onVisible(0) }.escape(2_000) as ChallengeEffect.TurnOffAbandoned).auto)
    }

    @Test
    fun siteSessionCarriesHostingBrowserIntoWalkAwayAndCompletion() {
        fun siteRuntime() = ChallengeRuntime(
            typingRuntime().config.copy(target = "reddit.com", targetType = "site", hostPackage = "com.android.chrome"),
        )
        val gate = siteRuntime()
        val walkAway = gate.gateWalkAway() as ChallengeEffect.WalkAway
        assertEquals("reddit.com", walkAway.target)
        assertEquals("com.android.chrome", walkAway.hostPackage)

        val pass = siteRuntime()
        pass.enterChallenge(0)
        pass.updateTypedText("alpha beta")
        val request = (pass.submitTyping(10) as ChallengeEffect.PersistCompletion).request
        assertEquals("site", request.targetType)
        assertEquals("com.android.chrome", request.hostPackage)
        assertEquals(request, (pass.resumePendingCompletion(20)!!).request.copy(durationMs = request.durationMs))
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
        runtime.onBackgrounded(11)
        assertNull(runtime.awayRemainingMs(11))
        assertNull(runtime.expireAway(60_000))
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

    // ---- P7-F3 auto-nope after 15 s away: one test per HANDBACK delta 7 row ----

    @Test
    fun gateReturnWithinLimitResumesTheSameGateWithoutOutcome() {
        val runtime = typingRuntime()
        runtime.onBackgrounded(0)
        assertEquals(15_000L, runtime.awayRemainingMs(0))
        assertNull(runtime.expireAway(14_999))
        assertNull(runtime.onReturned(14_999))
        assertEquals(ChallengePhase.GATE, runtime.view(15_000).phase)
        assertNull(runtime.expireAway(60_000)) // the clock stopped on return
        assertTrue(runtime.enterChallenge(60_000) is ChallengeEffect.Started)
    }

    @Test
    fun waitingReturnWithinLimitRestartsTheWait() {
        val runtime = pauseWaitingRuntime(seconds = 10)
        runtime.enterChallenge(0)
        runtime.onVisible(0)
        runtime.onBackgrounded(8_000)
        assertNull(runtime.onReturned(20_000))
        runtime.onVisible(20_000)
        assertEquals(10, runtime.view(20_000).remainingSeconds)
        assertNull(runtime.tick(29_999))
        assertTrue(runtime.tick(30_000) is ChallengeEffect.PersistCompletion)
    }

    @Test
    fun screenOffThenStopKeepsTheEarliestAwayStart() {
        val unlockedInTime = typingRuntime()
        unlockedInTime.enterChallenge(0)
        unlockedInTime.onBackgrounded(1_000) // SCREEN_OFF
        unlockedInTime.onBackgrounded(1_200) // onStop
        assertNull(unlockedInTime.onReturned(15_999))
        assertEquals(ChallengePhase.ACTIVE, unlockedInTime.view(16_000).phase)

        val unlockedLate = typingRuntime()
        unlockedLate.enterChallenge(0)
        unlockedLate.onBackgrounded(1_000)
        unlockedLate.onBackgrounded(10_000)
        assertEquals(6_000L, unlockedLate.awayRemainingMs(10_000))
        val effect = unlockedLate.onReturned(16_000) as ChallengeEffect.WalkAway
        assertEquals(EventRepository.WALK_AWAY_SOURCE_AUTO_AWAY, effect.source)
    }

    @Test
    fun pauseGateAndChallengeAutoNopeAfterFifteenSecondsWithAutoAwaySource() {
        val gate = typingRuntime()
        gate.onBackgrounded(0)
        val gateNope = gate.expireAway(15_000) as ChallengeEffect.WalkAway
        assertEquals(EventRepository.WALK_AWAY_SOURCE_AUTO_AWAY, gateNope.source)
        assertEquals("session", gateNope.sessionId)
        assertEquals("com.example.target", gateNope.target)
        assertEquals(ChallengePhase.WALK_AWAY, gate.view(15_000).phase)

        val typing = typingRuntime()
        typing.enterChallenge(0)
        typing.updateTypedText("alpha")
        typing.onBackgrounded(1_000)
        assertEquals(EventRepository.WALK_AWAY_SOURCE_AUTO_AWAY, (typing.expireAway(16_000) as ChallengeEffect.WalkAway).source)
        assertEquals(ChallengePhase.TERMINAL, typing.view(16_000).phase)

        val waiting = pauseWaitingRuntime(seconds = 10)
        waiting.enterChallenge(0)
        waiting.onVisible(0)
        waiting.onBackgrounded(5_000)
        // Evaluated on a late return instead of the timer: same outcome.
        assertEquals(EventRepository.WALK_AWAY_SOURCE_AUTO_AWAY, (waiting.onReturned(40_000) as ChallengeEffect.WalkAway).source)
    }

    @Test
    fun turnOffAutoNopeEqualsNeverMindAndNeverWalksAway() {
        val typing = ChallengeRuntime(typingRuntime().config.copy(purpose = ChallengePurpose.TURN_OFF, target = null, targetType = null))
        typing.beginTurnOff(0)
        typing.updateTypedText("alpha")
        typing.onBackgrounded(1_000)
        val abandoned = typing.expireAway(16_000) as ChallengeEffect.TurnOffAbandoned
        assertEquals(7, abandoned.blockId)
        assertEquals(50, abandoned.progressPct)
        assertTrue(abandoned.auto) // lands on the phone's home screen (P7-F16)
        assertEquals(ChallengePhase.TERMINAL, typing.view(16_000).phase)

        val waiting = waitingRuntime(seconds = 10)
        waiting.beginTurnOff(0)
        waiting.onVisible(0)
        waiting.onBackgrounded(4_000) // leaving resets the wait, as a visible-only wait always has
        val late = waiting.onReturned(19_000) as ChallengeEffect.TurnOffAbandoned
        assertEquals(0, late.progressPct)
        assertTrue(late.auto)
    }

    @Test
    fun visibleWaitStillCompletesAsPushThroughAndOnScreenTimeNeverExpires() {
        val runtime = pauseWaitingRuntime(seconds = 30)
        runtime.enterChallenge(0)
        runtime.onVisible(0)
        assertNull(runtime.expireAway(29_000))
        assertNull(runtime.awayRemainingMs(29_000))
        assertTrue(runtime.tick(30_000) is ChallengeEffect.PersistCompletion)
    }

    @Test
    fun configurationChangeIsNotAwayAndKeepsTypedText() {
        val runtime = typingRuntime()
        runtime.enterChallenge(0)
        runtime.updateTypedText("alpha")
        runtime.onConfigurationHidden(1_000)
        val recreated = ChallengeRuntime(runtime.config, runtime.snapshot())
        assertNull(recreated.snapshot().awayStartMs)
        assertNull(recreated.onReturned(60_000))
        assertNull(recreated.expireAway(60_000))
        assertEquals("alpha", recreated.view(60_000).typedText)
    }

    @Test
    fun processDeathRestoresTheAwayStartAndEvaluatesItOnRecreate() {
        val saved = typingRuntime().apply {
            enterChallenge(0)
            updateTypedText("alpha")
            onBackgrounded(1_000)
        }.snapshot()
        assertEquals(1_000L, saved.awayStartMs)

        // Recreated while still hidden: the remaining time runs on.
        val hidden = ChallengeRuntime(typingRuntime().config, saved)
        assertNull(hidden.expireAway(10_000))
        assertEquals(6_000L, hidden.awayRemainingMs(10_000))
        assertTrue(hidden.expireAway(16_000) is ChallengeEffect.WalkAway)

        // Recreated by the user's return in time: same session, text cleared.
        val back = ChallengeRuntime(typingRuntime().config, saved)
        assertNull(back.onReturned(12_000))
        assertEquals("", back.view(12_000).typedText)
        assertNull(back.enterChallenge(12_000))

        // Recreated late: quiet auto-nope.
        assertTrue(ChallengeRuntime(typingRuntime().config, saved).onReturned(30_000) is ChallengeEffect.WalkAway)

        // Elapsed clock reset (reboot) cannot leave the session away forever.
        val rebooted = ChallengeRuntime(typingRuntime().config, saved)
        assertEquals(0L, rebooted.awayRemainingMs(500))
        assertTrue(rebooted.expireAway(500) is ChallengeEffect.WalkAway)
    }

    @Test
    fun relaunchWhileAwayEndsAnExpiredSessionOnceOrKeepsALiveOne() {
        // onNewIntent asks expireAway first: an expired session yields its
        // outcome exactly once, so the replacement launch cannot double-count.
        val expired = typingRuntime()
        expired.enterChallenge(0)
        expired.onBackgrounded(1_000)
        assertTrue(expired.expireAway(20_000) is ChallengeEffect.WalkAway)
        assertNull(expired.expireAway(20_001))
        assertNull(expired.onReturned(20_001))
        assertNull(expired.escape(20_001))

        // In time: no effect, the challenge stays and the return clears text.
        val live = typingRuntime()
        live.enterChallenge(0)
        live.updateTypedText("alpha")
        live.onBackgrounded(1_000)
        assertNull(live.expireAway(10_000))
        assertEquals(ChallengePhase.ACTIVE, live.view(10_000).phase)
        assertNull(live.onReturned(10_000))
        assertEquals("", live.view(10_000).typedText)
    }

    @Test
    fun lateAwayTimerAfterCommitStartedLetsTheCommitWin() {
        val runtime = typingRuntime()
        runtime.enterChallenge(0)
        runtime.updateTypedText("alpha beta")
        runtime.onBackgrounded(1_000)
        val completion = runtime.submitTyping(2_000) as ChallengeEffect.PersistCompletion
        assertNull(runtime.expireAway(30_000))
        assertNull(runtime.onReturned(30_000))
        assertTrue(runtime.commitSucceeded(completion.request.token) is ChallengeTerminalResult.PauseRequested)

        // A failed commit returns to ACTIVE with no stale away-start.
        val retry = typingRuntime()
        retry.enterChallenge(0)
        retry.updateTypedText("alpha beta")
        retry.onBackgrounded(1_000)
        val first = retry.submitTyping(2_000) as ChallengeEffect.PersistCompletion
        assertTrue(retry.commitFailed(first.request.token, 3_000))
        assertNull(retry.expireAway(60_000))
        assertNull(retry.snapshot().awayStartMs)
    }

    // P7-F-A1 regression. Mirrors BlockActivity's call order: Begin calls
    // enterChallenge + onVisible; SCREEN_OFF arrives while still resumed;
    // the delayed recordStarted completion then calls onVisible (and would
    // start the ticker); the user unlocks within 15 s before any onStop, so
    // onPostResume calls onReturned then onVisible.
    @Test
    fun screenOffThenDelayedStartCompletionCountsNoOffscreenTimeAndReturnRestartsTheWait() {
        val runtime = pauseWaitingRuntime(seconds = 10)
        assertTrue(runtime.enterChallenge(0) is ChallengeEffect.Started)
        runtime.onVisible(0)

        runtime.onBackgrounded(3_000) // SCREEN_OFF, resumed still true
        runtime.onVisible(4_000) // late recordStarted callback
        assertTrue(runtime.isAway())
        assertEquals(10, runtime.view(13_000).remainingSeconds)
        assertNull(runtime.tick(13_000)) // a ticker started by that callback cannot complete

        assertNull(runtime.onReturned(14_000)) // unlock 11 s later, before onStop
        assertEquals(false, runtime.isAway())
        runtime.onVisible(14_000)
        assertEquals(10, runtime.view(14_000).remainingSeconds)
        assertNull(runtime.tick(23_999))
        val completion = runtime.tick(24_000) as ChallengeEffect.PersistCompletion
        assertEquals(10_000L, completion.request.countdownElapsedMs)
    }

    @Test
    fun turnOffWaitStartedLateAfterScreenOffAlsoWaitsForTheReturn() {
        val runtime = waitingRuntime(seconds = 10)
        assertTrue(runtime.beginTurnOff(0) is ChallengeEffect.Started)
        runtime.onBackgrounded(500) // SCREEN_OFF before recordStarted returns
        runtime.onVisible(1_000) // persistStarted completion while resumed
        assertNull(runtime.tick(11_500)) // without the guard: 10.5 s "visible" -> complete
        assertNull(runtime.onReturned(12_000)) // 11.5 s away, in time
        runtime.onVisible(12_000)
        assertNull(runtime.tick(21_999))
        assertTrue(runtime.tick(22_000) is ChallengeEffect.PersistCompletion)
    }

    // P7-F-A2 regression. Owner rule: a failed save never costs a completed
    // challenge. Mirrors BlockActivity: the wait completes on screen, SCREEN_OFF
    // arrives while COMMITTING (still resumed), the save throws before onStop
    // (onScreen = false), then onStop, then the user unlocks.
    @Test
    fun screenOffDuringCommitThenFailedSaveBeforeStopKeepsTheEarnedWaitForTheReturn() {
        val runtime = pauseWaitingRuntime(seconds = 10)
        runtime.enterChallenge(0)
        runtime.onVisible(0)
        val first = runtime.tick(10_000) as ChallengeEffect.PersistCompletion

        runtime.onBackgrounded(10_100) // SCREEN_OFF while committing: no away clock
        assertEquals(false, runtime.isAway())
        assertEquals(false, runtime.saveFailed(first.request.token, 10_200, onScreen = false))
        assertEquals(ChallengePhase.COMMITTING, runtime.view(10_200).phase)
        assertNull(runtime.tick(10_400)) // no retry or new completion while dark
        runtime.onBackgrounded(10_500) // onStop
        assertNull(runtime.awayRemainingMs(10_500))
        assertNull(runtime.expireAway(60_000)) // never an auto-nope

        // Process death in the meantime would restore the same pending completion.
        val restored = ChallengeRuntime(runtime.config, runtime.snapshot())
        assertEquals(first.request.token, restored.resumePendingCompletion(61_000)!!.request.token)

        assertNull(runtime.onReturned(61_000)) // unlock, long after 15 s
        val retry = runtime.resumePendingCompletion(61_000)!!
        assertEquals(first.request.token, retry.request.token)
        assertEquals(first.request.sessionId, retry.request.sessionId)
        assertEquals(10_000L, retry.request.countdownElapsedMs)
        assertTrue(runtime.commitSucceeded(retry.request.token) is ChallengeTerminalResult.PauseRequested)
    }

    @Test
    fun hiddenFailedSaveKeepsCompletedTypingWithoutClearingOrRecounting() {
        val runtime = typingRuntime()
        runtime.enterChallenge(0)
        runtime.updateTypedText("alpha beta")
        val first = runtime.submitTyping(5_000) as ChallengeEffect.PersistCompletion
        assertEquals(false, runtime.saveFailed(first.request.token, 5_100, onScreen = false))
        runtime.onBackgrounded(5_200)
        assertNull(runtime.onReturned(30_000))
        assertEquals("alpha beta", runtime.view(30_000).typedText)
        val retry = runtime.resumePendingCompletion(30_000)!!
        assertEquals(1, retry.request.attempts)
        assertNotNull(runtime.commitSucceeded(retry.request.token))
    }

    @Test
    fun onScreenFailedSaveKeepsThePhaseFiveRetryAndStaleTokensAreIgnored() {
        val runtime = pauseWaitingRuntime(seconds = 10)
        runtime.enterChallenge(0)
        runtime.onVisible(0)
        val first = runtime.tick(10_000) as ChallengeEffect.PersistCompletion
        assertEquals(false, runtime.saveFailed(first.request.token + 1, 10_100, onScreen = true))
        assertTrue(runtime.saveFailed(first.request.token, 10_100, onScreen = true))
        assertEquals(ChallengePhase.ACTIVE, runtime.view(10_100).phase)
        val retry = runtime.tick(10_300) as ChallengeEffect.PersistCompletion // the earned wait re-completes at once
        assertNotEquals(first.request.token, retry.request.token)
        assertEquals(false, runtime.saveFailed(first.request.token, 10_400, onScreen = true))
    }

    @Test
    fun explicitEscapeStopsTheAwayClock() {
        val runtime = typingRuntime()
        runtime.enterChallenge(0)
        runtime.onBackgrounded(1_000)
        assertEquals("typing", (runtime.escape(2_000) as ChallengeEffect.WalkAway).source)
        assertNull(runtime.expireAway(60_000))
        assertNull(runtime.awayRemainingMs(60_000))
    }

    @Test
    fun contentSelectorProducesExactRequestedLengthAndIndependentSelections() {
        val first = ChallengeContentSelector(kotlin.random.Random(1)).passage(20)
        val second = ChallengeContentSelector(kotlin.random.Random(2)).passage(20)
        assertEquals(20, first.length)
        assertEquals(20, second.length)
        assertNotEquals(first, second)
        assertTrue(first.split(' ').all { it in AppConfig.TYPING_WORDS })
    }

    @Test
    fun ownerApprovedCopyAndAssetPoolsAreComplete() {
        assertEquals(11, AppConfig.GATE_HUMOUR_LINES.size)
        assertEquals("Naah bruh", AppConfig.GATE_HUMOUR_LINES[6])
        assertEquals("Let's give it a rest", AppConfig.GATE_HUMOUR_LINES.last())
        assertEquals(7, AppConfig.GATE_HEADLINES.size)
        assertTrue(AppConfig.GATE_HEADLINES.all { "{target}" in it })
        assertEquals(15, AppConfig.GATE_BACKGROUNDS.size) // 11 + four added 9 October
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

    private fun pauseWaitingRuntime(seconds: Int) = ChallengeRuntime(
        ChallengeConfig(
            sessionId = "session",
            blockId = 7,
            purpose = ChallengePurpose.PAUSE,
            method = FrictionType.DELAY,
            totalSeconds = seconds,
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

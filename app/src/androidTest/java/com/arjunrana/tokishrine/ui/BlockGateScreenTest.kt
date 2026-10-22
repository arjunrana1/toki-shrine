package com.arjunrana.tokishrine.ui

import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.DpRect
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.arjunrana.tokishrine.R
import com.arjunrana.tokishrine.ui.interruption.BlockGateScreen
import com.arjunrana.tokishrine.ui.theme.NocturneTheme
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/*
 * Gate 14 v2 regressions (TS-unlock-minutes UM-7), compiled but not executed
 * in the delta that added them — no device/emulator run is authorized. The row
 * and chip motion themselves (stagger, overshoot) are owner-checked in UM-O1;
 * these cover what the semantics tree can show: the closed row is absent, a
 * chip reports its minutes exactly once, folding clears, the open row survives
 * recreation, and the buttons never move.
 */
@RunWith(AndroidJUnit4::class)
class BlockGateScreenTest {

    @get:Rule
    val compose = createAndroidComposeRule<CreateFlowTestActivity>()

    private val entered = CopyOnWriteArrayList<Int>()
    private val walkAways = AtomicInteger(0)

    @After
    fun tearDown() {
        CreateFlowTestActivity.content = null
    }

    private fun showGate() {
        CreateFlowTestActivity.content = {
            NocturneTheme {
                BlockGateScreen(
                    blockName = "Social",
                    headline = "Open Instagram?",
                    humourLine = "The feed will still be there.",
                    backgroundRes = R.drawable.sys_block_1,
                    onWalkAway = { walkAways.incrementAndGet() },
                    onEnterChallenge = { entered.add(it) },
                )
            }
        }
        // Reinstall the configured root from onCreate, as the other UI tests do.
        compose.activityRule.scenario.recreate()
    }

    private fun toggleRow() {
        compose.onNodeWithText("I'll do the challenge", substring = true).performClick()
    }

    private fun assertRowShown() {
        compose.onNodeWithText("Minutes to unlock").assertExists()
        listOf(5, 10, 15, 20).forEach { compose.onNodeWithContentDescription("$it minutes").assertExists() }
    }

    private fun assertRowAbsent() {
        compose.onNodeWithText("Minutes to unlock").assertDoesNotExist()
        listOf(5, 10, 15, 20).forEach { compose.onNodeWithContentDescription("$it minutes").assertDoesNotExist() }
    }

    private fun bounds(text: String, substring: Boolean = false): DpRect =
        compose.onNodeWithText(text, substring = substring).getUnclippedBoundsInRoot()

    // The closed row is not in the semantics tree, so it cannot be tapped or
    // read; tapping the way-in button shows the label and four chips, and
    // tapping it again folds them away without reporting anything.
    @Test
    fun theRowIsAbsentUntilTheChallengeButtonOpensItAndFoldsOnASecondTap() {
        showGate()
        assertRowAbsent()

        toggleRow()
        assertRowShown()

        toggleRow()
        compose.waitForIdle()
        assertRowAbsent()
        assertTrue(entered.isEmpty())
        assertEquals(0, walkAways.get())
    }

    // A chip tap shows its selected state and then reports its minutes once,
    // even when tapped again or when another chip is tapped straight after.
    @Test
    fun aChipReportsItsMinutesExactlyOnce() {
        showGate()
        toggleRow()

        compose.onNodeWithContentDescription("10 minutes").performClick()
        compose.onNodeWithContentDescription("10 minutes").performClick()
        compose.onNodeWithContentDescription("15 minutes").performClick()

        compose.waitUntil(timeoutMillis = 5_000) { entered.isNotEmpty() }
        compose.waitForIdle()
        assertEquals(listOf(10), entered.toList())
    }

    // "Tapping the button again folds the row back up ... and clears the
    // pick": folding right after a chip tap cancels the pending report.
    @Test
    fun foldingRightAfterAChipTapReportsNothing() {
        showGate()
        toggleRow()

        compose.onNodeWithContentDescription("5 minutes").performClick()
        toggleRow()
        compose.waitForIdle()
        Thread.sleep(500) // longer than the selected-state hold
        compose.waitForIdle()

        assertTrue(entered.isEmpty())
        assertRowAbsent()
    }

    // The main button only reports the walk-away; it never carries minutes.
    @Test
    fun theMainButtonStillWalksAway() {
        showGate()
        toggleRow()
        compose.onNodeWithText("Nope, not now").performClick()

        assertEquals(1, walkAways.get())
        assertTrue(entered.isEmpty())
    }

    // Open state survives recreation (rotation); a pick does not, and nothing
    // is reported by the recreation itself.
    @Test
    fun theOpenRowSurvivesRecreation() {
        showGate()
        toggleRow()
        assertRowShown()

        compose.activityRule.scenario.recreate()

        assertRowShown()
        assertTrue(entered.isEmpty())
    }

    // 14 v2: the row sits under the challenge button and never shifts either
    // button when it opens or folds.
    @Test
    fun openingTheRowDoesNotMoveEitherButton() {
        showGate()
        val mainClosed = bounds("Nope, not now")
        val challengeClosed = bounds("I'll do the challenge", substring = true)

        toggleRow()
        compose.waitForIdle()
        assertEquals(mainClosed, bounds("Nope, not now"))
        assertEquals(challengeClosed, bounds("I'll do the challenge", substring = true))
    }
}

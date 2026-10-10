package com.arjunrana.tokishrine.ui.interruption

import com.arjunrana.tokishrine.config.AppConfig
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/*
 * The 9 October celebration rules (P7-F17): the "FOUR TIMES" title only on
 * the day's fourth nope, the minutes line only for a nope that saved time
 * (with the real minutes), every title keeping its own image, and the same
 * seed reproducing the same pick (recreation).
 */
class CelebrationPickerTest {

    private val fourTimes = AppConfig.CELEBRATION_TITLES.single { it.onlyOnDailyCount == 4 }

    @Test
    fun fourTimesTitleIsEligibleOnlyOnTheFourthNope() {
        for (count in listOf(1, 2, 3, 5, 12)) {
            assertFalse(fourTimes in CelebrationPicker.eligibleTitles(count))
        }
        assertTrue(fourTimes in CelebrationPicker.eligibleTitles(4))
        assertEquals(AppConfig.CELEBRATION_TITLES.size - 1, CelebrationPicker.eligibleTitles(1).size)
        assertEquals(AppConfig.CELEBRATION_TITLES.size, CelebrationPicker.eligibleTitles(4).size)
    }

    @Test
    fun minutesLineUsesTheRealMinutesAndIsDroppedWithoutASaving() {
        val withMinutes = CelebrationPicker.eligibleSubtitles(12)
        assertTrue("That could've been 12 minutes of reels." in withMinutes)
        assertEquals(AppConfig.CELEBRATION_SUBTITLES.size, withMinutes.size)
        assertTrue("That could've been 1 minute of reels." in CelebrationPicker.eligibleSubtitles(1))

        val deduped = CelebrationPicker.eligibleSubtitles(null)
        assertEquals(AppConfig.CELEBRATION_SUBTITLES.size - 1, deduped.size)
        assertTrue(deduped.none { "reels" in it || AppConfig.CELEBRATION_MINUTES_TOKEN in it })
        assertEquals(deduped, CelebrationPicker.eligibleSubtitles(0))
    }

    @Test
    fun picksKeepTitleImagePairsAndReproduceFromTheSeed() {
        repeat(200) { seed ->
            val pick = CelebrationPicker.pick(dailyCount = 3, visitMinutes = 7, random = Random(seed.toLong()))
            val title = AppConfig.CELEBRATION_TITLES.single { it.text == pick.title }
            assertEquals(title.image, pick.image)
            assertFalse(pick.title == fourTimes.text) // not the 4th nope
            assertFalse(AppConfig.CELEBRATION_MINUTES_TOKEN in pick.subtitle)
            assertEquals("3 wins today", pick.winsLine)
            assertEquals(pick, CelebrationPicker.pick(3, 7, Random(seed.toLong())))
        }
        // Over many seeds every eligible title and subtitle shows up: the choice is random, not fixed.
        val picks = (0L until 400L).map { CelebrationPicker.pick(4, 9, Random(it)) }
        assertEquals(AppConfig.CELEBRATION_TITLES.size, picks.map { it.title }.distinct().size)
        assertEquals(AppConfig.CELEBRATION_SUBTITLES.size, picks.map { it.subtitle }.distinct().size)
        assertEquals("1 win today", CelebrationPicker.winsLine(1))
    }
}

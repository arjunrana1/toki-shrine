package com.arjunrana.tokishrine.challenge

import com.arjunrana.tokishrine.config.AppConfig
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/*
 * P7-F12 passage-variety contract (PRD §17 "Typing passages", 8 October
 * 2026): passages keep their exact requested length, never repeat a word
 * immediately, and no longer funnel into the same short closing words.
 * Seeds are fixed, so thresholds are deterministic; they are set with
 * margin below observed variety to stay meaningful, not brittle.
 */
class ChallengeContentSelectorTest {

    @Test
    fun passagesHaveTheExactRequestedLengthAcrossTheConfiguredRange() {
        val lengths = (3..200).toList() + listOf(220, 350, 700)
        lengths.forEach { length ->
            val passage = ChallengeContentSelector(Random(length)).passage(length)
            assertEquals("length $length", length, passage.length)
            val words = passage.split(' ')
            assertTrue("length $length", words.none { it.isEmpty() })
            assertTrue("length $length", words.all { it in AppConfig.TYPING_WORDS })
        }
    }

    @Test
    fun shorterThanTheSmallestWordIsRejected() {
        var thrown = false
        try {
            ChallengeContentSelector(Random(1)).passage(2)
        } catch (expected: IllegalArgumentException) {
            thrown = true
        }
        assertTrue(thrown)
    }

    @Test
    fun passagesNeverRepeatAWordImmediately() {
        val lengths = (3..60).toList() + (70..200 step 10) + listOf(220, 350, 700)
        lengths.forEach { length ->
            repeat(30) { seed ->
                val words = ChallengeContentSelector(Random(seed)).passage(length).split(' ')
                words.zipWithNext().forEach { (word, next) ->
                    assertTrue("length $length seed $seed: '$word $next'", word != next)
                }
            }
        }
    }

    @Test
    fun passageEndingsVaryAcrossManySeeds() {
        listOf(20, 60, 150, 200).forEach { length ->
            val endings = (0..99).map { seed ->
                ChallengeContentSelector(Random(seed)).passage(length).split(' ').last()
            }
            assertTrue(
                "length $length had ${endings.distinct().size} distinct endings",
                endings.distinct().size >= 15,
            )
            assertTrue(
                "length $length had ${endings.map { it.length }.distinct().size} ending lengths",
                endings.map { it.length }.distinct().size >= 4,
            )
        }
    }

    @Test
    fun typingWordsAreLowercaseDistinctAndNeverSoloPerLength() {
        val words = AppConfig.TYPING_WORDS
        assertTrue(words.size >= 100)
        assertTrue(words.all { it.matches(Regex("[a-z]+")) })
        assertEquals(words.size, words.distinct().size)
        // A 3-letter word keeps passage(length >= 3) satisfiable, and every
        // length class needs a second word so the no-immediate-repeat fill
        // can never dead-end (see ChallengeContentSelector.passage).
        assertTrue(words.any { it.length == 3 })
        words.groupBy { it.length }.forEach { (length, sameLength) ->
            assertTrue("length $length has ${sameLength.size} words", sameLength.size >= 2)
        }
    }
}

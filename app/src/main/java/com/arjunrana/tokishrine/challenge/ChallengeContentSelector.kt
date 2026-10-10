package com.arjunrana.tokishrine.challenge

import com.arjunrana.tokishrine.config.AppConfig
import kotlin.random.Random

/*
 * Randomized gate/challenge content. The editable pools (backgrounds,
 * headlines, humour lines, typing words) live in AppConfig (PRD §17
 * "Config", 8 October 2026 addendum / P7-F11); this class only draws from
 * them.
 */
class ChallengeContentSelector(private val random: Random = Random.Default) {
    fun humour(): String = AppConfig.GATE_HUMOUR_LINES.random(random)

    fun headline(target: String): String =
        AppConfig.GATE_HEADLINES.random(random).replace("{target}", target)

    fun backgroundRes(): Int = AppConfig.GATE_BACKGROUNDS.random(random)

    /*
     * P7-F12 variety rules, keeping the exact-length contract:
     * - a word length is chosen uniformly first, then one word of that
     *   length, so word-heavy short classes no longer dominate every fill;
     * - a word never directly follows itself;
     * - the exact-length fill no longer funnels every passage into the same
     *   short closing words, because the closing word's length varies with
     *   the walk instead of being forced by a uniform-over-words greedy pick.
     *
     * The word-list invariant pinned by ChallengeContentSelectorTest (at
     * least two words per length) guarantees the previous-word exclusion can
     * never dead-end; the fallback below keeps that true even if the list is
     * ever edited down to singletons.
     */
    fun passage(length: Int): String {
        require(length >= 3) { "Passage length must fit at least one word" }
        val wordsByLength = AppConfig.TYPING_WORDS.groupBy { it.length }
        val appendCosts = wordsByLength.keys.map { it + 1 }
        val fillable = BooleanArray(length + 1).also { possible ->
            possible[0] = true
            for (remaining in 1..length) {
                possible[remaining] = appendCosts.any { it <= remaining && possible[remaining - it] }
            }
        }
        fun pick(remaining: Int, first: Boolean, previous: String?): String {
            val lengths = wordsByLength.keys.filter { wordLength ->
                val cost = if (first) wordLength else wordLength + 1
                cost <= remaining && fillable[remaining - cost]
            }
            var pools = lengths
                .map { wordsByLength.getValue(it).filter { it != previous } }
                .filter { it.isNotEmpty() }
            if (pools.isEmpty()) {
                pools = lengths.map { wordsByLength.getValue(it) }.filter { it.isNotEmpty() }
            }
            return pools.random(random).random(random)
        }
        val first = pick(length, first = true, previous = null)
        return buildString(length) {
            append(first)
            var previous = first
            while (this.length < length) {
                val word = pick(length - this.length, first = false, previous = previous)
                append(' ')
                append(word)
                previous = word
            }
        }
    }
}

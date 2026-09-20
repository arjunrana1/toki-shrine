package com.arjunrana.tokishrine.challenge

import com.arjunrana.tokishrine.R
import kotlin.random.Random

class ChallengeContentSelector(private val random: Random = Random.Default) {
    fun humour(): String = HUMOUR_LINES.random(random)

    fun backgroundRes(): Int = BACKGROUNDS.random(random)

    fun passage(length: Int): String {
        require(length >= 3) { "Passage length must fit at least one word" }
        val appendCosts = WORDS.map { it.length + 1 }.distinct()
        val fillable = BooleanArray(length + 1).also { possible ->
            possible[0] = true
            for (remaining in 1..length) {
                possible[remaining] = appendCosts.any { it <= remaining && possible[remaining - it] }
            }
        }
        val first = WORDS.filter { it.length <= length && fillable[length - it.length] }.random(random)
        return buildString(length) {
            append(first)
            while (this.length < length) {
                val remaining = length - this.length
                val word = WORDS.filter {
                    val cost = it.length + 1
                    cost <= remaining && fillable[remaining - cost]
                }.random(random)
                append(' ')
                append(word)
            }
        }
    }

    companion object {
        val HUMOUR_LINES = listOf(
            "Touch grass.",
            "Go drink water.",
            "Nice try.",
            "Not today.",
            "Bro. No.",
            "Look who it is.",
            "Right on schedule.",
            "Bold of you.",
            "The algorithm can wait.",
            "We meet again.",
        )

        val BACKGROUNDS = listOf(
            R.drawable.sys_block_1,
            R.drawable.sys_block_2,
            R.drawable.sys_block_3,
            R.drawable.sys_block_4,
            R.drawable.sys_block_5,
            R.drawable.sys_block_6,
        )

        internal val WORDS = listOf(
            "amber", "anchor", "apple", "bamboo", "beacon", "birch", "breeze", "brook",
            "candle", "cedar", "cloud", "coral", "crane", "dawn", "drift", "ember",
            "fern", "field", "flint", "forest", "garden", "glade", "harbor", "hazel",
            "heron", "island", "juniper", "lake", "lantern", "maple", "meadow", "mist",
            "moon", "moss", "ocean", "olive", "orchard", "pebble", "pine", "pond",
            "rain", "reed", "river", "robin", "sage", "shore", "spruce", "stone",
            "sun", "thistle", "trail", "valley", "willow", "wind", "wood", "wren",
        )
    }
}

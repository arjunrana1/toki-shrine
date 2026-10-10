package com.arjunrana.tokishrine.config

import com.arjunrana.tokishrine.R

/*
 * One plain Kotlin config file for the editable lists and shared values
 * (PRD §17 "Config", 8 October 2026 addendum / P7-F11). Everything here is
 * identical in every build variant. Values that differ per variant — the
 * typing/wait floors, the fresh-draft defaults and the disable ladders —
 * stay in the per-variant data/repo/ChallengeValues.kt source sets, and
 * images stay in res/drawable.
 */
object AppConfig {

    // — Gate content (moved from ChallengeContentSelector, values unchanged) —

    val GATE_BACKGROUNDS = listOf(
        R.drawable.sys_block_1,
        R.drawable.sys_block_2,
        R.drawable.sys_block_3,
        R.drawable.sys_block_4,
        R.drawable.sys_block_5,
        R.drawable.sys_block_6,
        R.drawable.sys_block_7,
        R.drawable.sys_block_8,
        R.drawable.sys_block_9,
        R.drawable.sys_block_10,
        R.drawable.sys_block_11,
        // Added by Arjun, 9 October 2026.
        R.drawable.sys_block_12,
        R.drawable.sys_block_13,
        R.drawable.sys_block_14,
        R.drawable.sys_block_15,
    )

    val GATE_HEADLINES = listOf(
        "Trying to waste time on {target}?",
        "Back for {target} already?",
        "Someone tryna open {target}?",
        "Taking a detour to {target}?",
        "Is {target} calling again?",
        "{target}? Really?",
        "Is someone missing {target}?",
    )

    val GATE_HUMOUR_LINES = listOf(
        "Touch grass.",
        "Go drink water.",
        "Nice try.",
        "Not today.",
        "Bro. No.",
        "Look who it is.",
        "Naah bruh",
        "Bold of you.",
        "The algorithm can wait.",
        "We meet again.",
        "Let's give it a rest",
    )

    /**
     * Typing passage vocabulary (P7-F12): the original 56 calm-nature words
     * plus everyday vocabulary across length 3–10. Plain lowercase words,
     * all distinct, and at least two words of every length present, so the
     * exact-length fill can always avoid repeating the previous word.
     * Pinned by ChallengeContentSelectorTest.
     */
    internal val TYPING_WORDS = listOf(
        "adventure", "amber", "anchor", "apple",
        "apricot", "avocado", "bamboo", "beach",
        "beacon", "berry", "birch", "blackberry",
        "blanket", "bluebird", "bread", "breeze",
        "brook", "brush", "bubble", "butter",
        "butterfly", "cabinet", "cactus",
        "candle", "cedar", "cheese", "cherry", "chocolate",
        "cinnamon", "cloud", "copper", "coral",
        "cotton", "crane", "crayon", "crow",
        "dawn", "deer", "desert", "dolphin",
        "dragonfly", "drift", "dusk",
        "elephant", "ember", "fabric", "feather", "fern",
        "festival", "field", "finch", "flint",
        "fog", "forest", "fossil", "fox", "frost",
        "galaxy", "garden", "gem", "gemstone",
        "glade", "granite",
        "grape", "guitar", "harbor",
        "harvest", "hazel", "herb", "heron",
        "honey", "ink", "island", "ivy",
        "jacket", "journey", "joy", "juniper",
        "kelp", "kitchen", "kitten", "ladder",
        "lake", "lantern", "lavender", "leaf",
        "lemon", "light", "linen", "loam",
        "mango", "maple", "marble", "market",
        "meadow", "melon", "metal", "mirror",
        "mist", "moon", "moonbeam", "moonlight",
        "morning", "moss", "moth", "mountain",
        "mouse", "mustard", "necklace", "needle",
        "nest", "nightfall", "oak", "ocean",
        "olive", "orange", "orchard", "orchid",
        "owl", "paper", "parrot", "peach",
        "pear", "pearl", "pebble", "pencil",
        "penguin", "pepper", "petal", "piano",
        "picnic", "picture", "pillow", "pine",
        "planet", "pond", "popcorn", "pudding",
        "puddle", "quiet", "rabbit", "rain",
        "reed", "reef", "ribbon", "river",
        "robin", "rocket", "root", "rope",
        "rye", "saffron", "sage", "sail",
        "salad", "salmon", "sand", "seashell",
        "seed", "sheet", "shore", "shrimp",
        "silk", "silver", "skirt", "sky",
        "smoke", "snow", "soil", "song",
        "spice", "spinach", "spiral", "spruce",
        "star", "stone", "stream", "sugar",
        "summer", "sun", "sunbeam", "sunflower",
        "sunset", "sunshine", "tea", "thistle",
        "thunder", "tide", "toast", "towel",
        "trail", "treasure", "tulip", "twilight",
        "umbrella", "valley", "vanilla", "vase", "velvet",
        "violet", "walnut", "waterfall", "watermelon",
        "whisper", "wild", "wildflower", "willow",
        "wind", "window", "winter", "wood",
        "wool", "wren", "yarn"
    )

    // — Shared pause ranges/steps (moved from BlockRepository.kt, values
    //    unchanged; enforced at the persistence boundary) —

    const val PAUSE_MINUTES_MIN = 5
    const val PAUSE_MINUTES_MAX = 100
    const val PAUSE_MINUTES_STEP = 5
    const val PAUSE_CHARS_MAX = 200
    const val PAUSE_CHARS_STEP = 10
    const val PAUSE_WAIT_SECONDS_MAX = 300
    const val PAUSE_WAIT_SECONDS_STEP = 5
    const val PAUSE_MINUTES_DEFAULT = 15

    // — Auto-nope (8 October §17 addendum / P7-F3): continuous seconds away
    //    before a live gate/challenge auto-nopes. ChallengeRuntime derives
    //    AUTO_NOPE_AWAY_MS from this. —

    const val AUTO_NOPE_AWAY_SECONDS = 15

    // — Usual visit length (moved from the StatsLedger companion, values
    //    unchanged; see stats.md and StatsLedger.usualVisit) —

    const val VISIT_PERCENTILE = 75
    const val MIN_VISIT_MS = 30_000L
    const val MIN_VISITS = 3

    // — Celebration / Walk-Away moment (9 October §17 addendum / P7-F17) —
    //
    // One title is picked at random, always with its own image; one subtitle
    // is picked independently. [CelebrationTitle.onlyOnDailyCount] limits a
    // title to that nope of the day. A subtitle containing
    // [CELEBRATION_MINUTES_TOKEN] is offered only when this nope saved time,
    // and the token becomes its valued visit in minutes.
    //
    // Images: res/drawable-nodpi/celebrate_*.xml are placeholders. Replace one
    // by deleting the .xml and adding a .webp/.png/.jpg with the same name.

    data class CelebrationTitle(val text: String, val image: Int, val onlyOnDailyCount: Int? = null)

    val CELEBRATION_TITLES = listOf(
        CelebrationTitle("Look who's taking control 😎", R.drawable.celebrate_cat),
        CelebrationTitle("Okay, someone's on a roll!", R.drawable.celebrate_dicaprio_toast),
        CelebrationTitle("You absolute legend.", R.drawable.celebrate_michael_scott),
        CelebrationTitle("FOUR TIMES?! WHO ARE YOU? 🔥", R.drawable.celebrate_elmo_fire, onlyOnDailyCount = 4),
        CelebrationTitle("Your phone is losing this battle.", R.drawable.celebrate_victory_dance),
        CelebrationTitle("Touching grass: Professional level.", R.drawable.celebrate_touch_grass),
    )

    const val CELEBRATION_MINUTES_TOKEN = "{minutes}"

    val CELEBRATION_SUBTITLES = listOf(
        "Your future self says thanks.",
        "Doomscrolling took another L.",
        "Plot twist: You chose yourself.",
        "That could've been $CELEBRATION_MINUTES_TOKEN of reels.",
        "Another distraction defeated. Flawless victory.",
        "Your screen time is trembling.",
        "You're the main character today.",
    )
}

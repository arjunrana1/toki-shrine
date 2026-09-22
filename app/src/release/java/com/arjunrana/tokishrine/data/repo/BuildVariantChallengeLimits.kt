package com.arjunrana.tokishrine.data.repo

/** Production values retained while debug builds expose owner-test minima. */
internal object BuildVariantChallengeLimits {
    const val pauseCharsMin = 100
    const val pauseWaitSecondsMin = 60
    val disableCharsChoices = listOf(220, 350, 700)
    val disableWaitSecondsChoices = listOf(180, 360, 720)
}

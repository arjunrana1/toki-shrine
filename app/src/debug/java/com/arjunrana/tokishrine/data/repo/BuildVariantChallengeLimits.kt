package com.arjunrana.tokishrine.data.repo

/** Temporary low-cost values for owner testing; remove for Phase 7 approval. */
internal object BuildVariantChallengeLimits {
    const val pauseCharsMin = 20
    const val pauseWaitSecondsMin = 20
    val disableCharsChoices = listOf(20, 350, 700)
    val disableWaitSecondsChoices = listOf(20, 360, 720)
}

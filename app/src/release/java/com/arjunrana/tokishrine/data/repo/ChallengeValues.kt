package com.arjunrana.tokishrine.data.repo

// Production disable values (PRD §17 23 October pre-launch config values
// addendum) for the release variant. The debug variant's ChallengeValues.kt
// declares the same names; pause values are the same in every build and live
// in AppConfig.

// Fixed disable ladders: no custom stepper or second method selector. The
// middle entry is the recommended preselection.
val DISABLE_CHARS_CHOICES = listOf(150, 200, 300)
val DISABLE_WAIT_SECONDS_CHOICES = listOf(60, 120, 240)

const val DISABLE_CHARS_DEFAULT = 200
const val DISABLE_WAIT_SECONDS_DEFAULT = 120

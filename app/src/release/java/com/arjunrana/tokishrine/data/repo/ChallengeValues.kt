package com.arjunrana.tokishrine.data.repo

// Production disable values (PRD §7 / §17) for the release variant. The
// debug variant's ChallengeValues.kt declares the same names; pause values
// are the same in every build and live in AppConfig (11 October §17
// unlock-minutes addendum).

// Fixed disable ladders: no custom stepper or second method selector. The
// middle entry is the recommended preselection.
val DISABLE_CHARS_CHOICES = listOf(220, 350, 700)
val DISABLE_WAIT_SECONDS_CHOICES = listOf(180, 360, 720)

const val DISABLE_CHARS_DEFAULT = 350
const val DISABLE_WAIT_SECONDS_DEFAULT = 360

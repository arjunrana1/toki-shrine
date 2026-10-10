package com.arjunrana.tokishrine.data.repo

// Production challenge values (PRD §7 / §17) for the release variant. The
// debug variant's ChallengeValues.kt declares the same names; values shared
// by every variant stay in BlockRepository.kt.
const val PAUSE_CHARS_MIN = 100
const val PAUSE_WAIT_SECONDS_MIN = 60

// Fixed disable ladders: no custom stepper or second method selector. The
// middle entry is the recommended preselection.
val DISABLE_CHARS_CHOICES = listOf(220, 350, 700)
val DISABLE_WAIT_SECONDS_CHOICES = listOf(180, 360, 720)

// Fresh-draft defaults: typing 150 chars, waiting 60 s, and the middle rung
// of each disable ladder.
const val PAUSE_CHARS_DEFAULT = 150
const val PAUSE_WAIT_SECONDS_DEFAULT = 60
const val DISABLE_CHARS_DEFAULT = 350
const val DISABLE_WAIT_SECONDS_DEFAULT = 360

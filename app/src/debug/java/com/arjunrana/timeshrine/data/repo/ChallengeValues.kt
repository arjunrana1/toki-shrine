package com.arjunrana.timeshrine.data.repo

// Owner-testing values for the debug variant, so device testing is quick
// (Arjun, 7 October 2026; ladders updated in the 23 October §17 pre-launch
// config values addendum): the first disable rung ("Type a bit" / "Wait a
// bit") is 20 and preselected; the other rungs match release. Pause values
// are the same in every build and live in AppConfig. The release variant
// keeps production values. Revert: copy the values from
// src/release/.../ChallengeValues.kt into this file.

// The middle rung keeps its "Recommended" badge; only the first is lowered.
val DISABLE_CHARS_CHOICES = listOf(20, 200, 300)
val DISABLE_WAIT_SECONDS_CHOICES = listOf(20, 120, 240)

const val DISABLE_CHARS_DEFAULT = 20
const val DISABLE_WAIT_SECONDS_DEFAULT = 20

package com.arjunrana.tokishrine.data.repo

// TEMPORARY owner-testing values for the debug variant — Arjun's request,
// 7 October 2026, so device testing is quick: typing passages floor and
// start at 20 characters, waits floor and start at 20 seconds, and the first
// disable rung ("Type a bit" / "Wait a bit") is 20 and preselected. Maxima,
// steps and pause minutes are unchanged (BlockRepository.kt). The release
// variant keeps production values. Revert: copy the values from
// src/release/.../ChallengeValues.kt into this file.
const val PAUSE_CHARS_MIN = 20
const val PAUSE_WAIT_SECONDS_MIN = 20

// The middle rung keeps its "Recommended" badge; only the first is lowered.
val DISABLE_CHARS_CHOICES = listOf(20, 350, 700)
val DISABLE_WAIT_SECONDS_CHOICES = listOf(20, 360, 720)

const val PAUSE_CHARS_DEFAULT = 20
const val PAUSE_WAIT_SECONDS_DEFAULT = 20
const val DISABLE_CHARS_DEFAULT = 20
const val DISABLE_WAIT_SECONDS_DEFAULT = 20

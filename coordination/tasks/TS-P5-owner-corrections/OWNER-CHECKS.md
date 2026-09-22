# Owner checks — TS-P5-owner-corrections

Owner observations are from Arjun's 23 September 2026 testing of the Phase 5B APK installed from code commit `c5a8e9e` (with later records-only HEAD `59596a8`). Unmentioned behavior was reported working well; this record does not turn source/build checks into device evidence. Retest results for the correction submission remain open.

| ID | Owner observation on installed build | Required correction / retest |
|---|---|---|
| P5C-O1 | Gate displayed raw package `com.ril.ajio` | Installed apps show user-visible label such as Ajio; sites show domain |
| P5C-O2 | Fixed “Open target?” copy was wrong for the intent | Seven approved target-aware headlines rotate independently of eleven approved subtexts |
| P5C-O3 | Gate headline alignment/type did not match reference | Centered regular Inter treatment; no Anton headline |
| P5C-O4 | Background subject was stretched/cropped out of view | Cropped full-screen layer plus centered aspect-preserving copy of the same selected image; rotate all eleven assets |
| P5C-O5 | Gate CTA sizes differed and reference emojis were absent | Equal CTA geometry; challenge CTA ends `🚩🤨` |
| P5C-O6 | Walk-away screen disappeared too quickly, emoji was absent, and dismissal returned to Toki | Count line ends `🎉`; tap or 10 seconds routes to Android home |
| P5C-O7 | Waiting challenge disappeared on app-switch/lock | Session remains active in process, resets to the full duration, and resumes without a terminal event |
| P5C-O8 | Waiting screen did not match supplied design | Timer ring, heading/reset explanation, no Counting pill, full-width outlined Never mind |
| P5C-O9 | Testing costs could not be lowered enough | Debug only: pause 20-char/20-sec minima; disable first rungs 20-char/20-sec; release values unchanged |
| P5C-O10 | Typing exposed typos live, lacked Submit, used wrong escape copy, and disappeared on switch | Never mind; passage/text retained in process; Submit-only validation, disabled until required length; typos revealed after failed Submit |
| P5C-O11 | Android Back needed intentionality distinction | Back backgrounds/preserves; only explicit Never mind terminates and records a walk-away |
| P5C-O12 | Completing a pause challenge did not grant access | Expected Phase 5 boundary; verify no premature Phase 6 claim, then test access lifecycle in Phase 6 |

Typing paste suppression and autocorrect/prediction safeguards were explicitly reported working well and must remain unchanged.

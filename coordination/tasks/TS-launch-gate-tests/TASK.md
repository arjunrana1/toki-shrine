# TS-launch-gate-tests — repair the stale Android tests and re-run them

- **State:** `awaiting_owner`. D1 (all six rows, test code only) was submitted on 23 October 2026: [HANDBACK](HANDBACK.md#d1--lg-1lg-6-test-repairs-23-october-2026), [D1-SUBMISSION.sha256](D1-SUBMISSION.sha256). It compiles and the JVM suite is green, but the instrumented suite has not been re-run. That needs Arjun's fresh authorization for the isolated emulator (LG-7).
- **Goal:** clear the launch-gate item "Run the Android tests". The 23 October run ([TS-unlock-minutes OWNER-CHECKS](../TS-unlock-minutes/OWNER-CHECKS.md#launch-gate-android-test-run--23-october-2026-isolated-emulator-agent-executed)) failed 15 of 65 and left 39 tests unrun. Every failure was diagnosed as test-side.
- **Owner decisions (Arjun, 23 October 2026):**
  - LG-5: "I tested LG 5 IRL and its not getting reported unless it actually runs." A chip pick folded away is not reported on the phone, so this is not a product defect; the test is repaired.
  - LG-6: browser data stays when the site block is off. This is the existing P7-F10 rule (PRD §17, 8 October addendum), and the test is updated to it.
  - The other failures are outdated tests. Tests that did not run because the code changed significantly are acceptable as test-side.
- **Owner:** Claude Opus 5.5 as senior implementer; Arjun asked it to take over in the launch-gate session. Routine test fixes, except LG-5's Compose test-clock control.
- **Base:** `main` at `fa55fbd`. Arjun's unrelated uncommitted deletion of `design_handoff_stats_screen/` stays untouched and unstaged.

## Rows

| ID | Change | Location | Status |
|---|---|---|---|
| LG-1 | Two `= runBlocking` tests returned a non-`Unit` value, so JUnit rejected both classes (39 tests unrun). Declare `: Unit`. | `BlockRepositoryTest.kt:600`, `EventRepositoryTest.kt:252` | submitted D1 |
| LG-2 | `MailTo.parse` cut the body at an encoded `&` on API 36. Parse the URI in RFC 6068 order (split the encoded query, then decode) in the test. | `FeedbackMailTest.kt` | submitted D1 |
| LG-3 | `example.com` matched both the added row and the field hint. Wait for the row only (text without the set-text action). | `AddMoreScreenTest.kt` `addSite` | submitted D1 |
| LG-4 | Back control found by its glyph, which is cleared from the merged tree since the Phase 7 accessibility pass. Find it by its "Back" description (12 uses). | `CreateFlowScreenTest.kt` | submitted D1 |
| LG-5 | The click's idle sync ran the 200 ms hold before the fold. Drive the clock by hand: tap, advance half the hold, fold, advance past it, expect nothing. | `BlockGateScreenTest.foldingRightAfterAChipTapReportsNothing` | submitted D1 |
| LG-6 | Expect the browser row to stay after the site block is off (P7-F10, owner-confirmed). | `StatsRepositoryTest.kt:196–200` | submitted D1 |
| LG-7 | Re-run the full instrumented suite on AVD `toki-p7d-isolated` with fresh owner authorization. Record per-class results in [OWNER-CHECKS](OWNER-CHECKS.md). Repair further stale tests found in the 39 newly runnable ones under the same rules. | — | waiting for authorization |

## Invariants

- No app code, build file or dependency changes; the debug APK must stay `9d505bd8…`.
- Never weaken a test to pass. A repair keeps the test's intent and only updates a finder, an oracle or an expectation to an owner-confirmed rule. A failure that shows the app misbehaving goes to Arjun, not into the test.
- Device work only on the isolated emulator, with every adb command addressed `-s emulator-5554`; never the phone.

## Allowed files

`app/src/androidTest/**`, this task folder, the TS-unlock-minutes OWNER-CHECKS decision note, CURRENT and CONTEXT-MAP.

## Checks

`./build.sh assembleDebug assembleDebugAndroidTest testDebugUnitTest`, and confirm every compiled test method returns `void`. Then LG-7 when authorized.

## Stop

After D1: stop and wait for Arjun's emulator authorization. After LG-7: stop when the suite is green, or report the remaining failures with a diagnosis.

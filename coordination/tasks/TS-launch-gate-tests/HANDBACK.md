# TS-launch-gate-tests — handback

## D1 — LG-1–LG-6 test repairs, 23 October 2026

Implementer: Claude Opus 5.5 (senior implementer), taking over the launch-gate session at Arjun's request. Base: `main` at `fa55fbd` (the run record). Arjun's unrelated deletion of `design_handoff_stats_screen/` is untouched and unstaged. Identity: [D1-SUBMISSION.sha256](D1-SUBMISSION.sha256) (the seven changed test files).

### What changed (test code only)

- **LG-1:** `BlockRepositoryTest.addTargetsRespectsOwnershipAtomically()` and `EventRepositoryTest.existingRowsSurviveReopenWithoutDestructiveFallback()` now declare `: Unit = runBlocking { … }`, so both compile to `void` and JUnit accepts their classes. This makes 39 tests runnable again (26 + 13).
- **LG-2:** `FeedbackMailTest.mailtoUriParsesBackToRecipientSubjectAndBody` no longer uses `android.net.MailTo.parse` as its oracle. It splits the still-encoded query on `&` and `=`, then `Uri.decode`s each part (RFC 6068 order), and checks the recipient, subject and full body. The URI builder under test is unchanged.
- **LG-3:** `AddMoreScreenTest.addSite` waits for `hasText(domain) and !hasSetTextAction()`, the added row only, instead of any `example.com` text, which also matched the field hint. Both tests still add `example.com` and check the stored sites.
- **LG-4:** `CreateFlowScreenTest` finds the appbar back control with `onNodeWithContentDescription("Back")` (12 uses; `BACK_GLYPH` replaced by `BACK`). The glyph has been cleared from the merged tree since the Phase 7 accessibility pass (`NocturneUi.kt:291`). The step and telemetry assertions are unchanged.
- **LG-5:** `BlockGateScreenTest.foldingRightAfterAChipTapReportsNothing` turns `mainClock.autoAdvance` off. It taps 5, advances half of `GATE_SELECTION_HOLD_MS` and checks that nothing is reported yet. Then it folds, advances three times the hold, and checks again that nothing is reported and the row is absent. The old `Thread.sleep(500)` is gone. The fold now lands inside the hold, which the old test never reached. Arjun confirmed on the phone that a folded pick is not reported.
- **LG-6:** `StatsRepositoryTest.enabledSiteBlockShowsHostingBrowserRowWithSiteSavingsAndFirstBrowserVisitSpent` expects `["browser", "app"]` after the site block is turned off. This is P7-F10, owner-confirmed on 23 October. The retained-savings assertion is unchanged.

### Checks and results

- `./build.sh assembleDebug assembleDebugAndroidTest testDebugUnitTest` → BUILD SUCCESSFUL. JVM unit tests: 277, 0 failures, 0 errors, 0 skipped (result XML). No new compiler warnings in the changed code (the three remaining androidTest warnings are pre-existing unused variables).
- `javap` over every compiled androidTest class: no public no-argument method returns anything other than `void`. Both LG-1 methods now compile as `public final void`.
- APK SHA-256: `app-debug.apk` `9d505bd8ec2d8654ac798cc3e7438aa50186322494936290c3a5ef8d9bb3325f` (unchanged, so no app code changed); `app-debug-androidTest.apk` `def129a806fe6055ea77270c8814d02090b006d32c287d6f6b78b4d6d890541a`.

### Limits

- **Not executed on Android.** These repairs are compiled only. Whether they pass, and whether the 39 newly runnable tests pass, needs LG-7, the re-run on the isolated emulator with Arjun's authorization.
- The 39 tests in `BlockRepositoryTest` and `EventRepositoryTest` were never executed against the Phase 7 and unlock-minutes code, so more stale expectations are likely there. They will be handled under the TASK invariants.
- LG-5's manual-clock sequence is a reading of the Compose test API, not yet observed running.
- Self-verified only; no independent review.

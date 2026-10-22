# TS-unlock-minutes — owner checks

Arjun runs these on his phone after UM-14. Record each result with the date and the **installed APK hash**. **Results: all seven PASS (22 October 2026, build `9d505bd8…`)**, see [Owner results](#owner-results--22-october-2026).

## UM-14 install record — 22 October 2026

- **Installed build:** reviewed D3 debug APK `9d505bd8ec2d8654ac798cc3e7438aa50186322494936290c3a5ef8d9bb3325f` (built 11 October 01:54, versionName 0.1.0), at Arjun's explicit request. Run every check below against this hash.
- **Identity verified first:** all 35 entries of [D3-SUBMISSION.sha256](D3-SUBMISSION.sha256) match the working tree, `ui/util/Estimate.kt` is absent, and the APK hash matches D3 and [REVIEW](REVIEW.md). No rebuild.
- **Install:** `adb -s R5CW30ZBM2R install -r` (in place, no uninstall or clear) on Arjun's SM-S918B over USB → `Success`; package `lastUpdateTime` 2026-10-22 23:36:17 IST. Installed by Claude Opus 5.5.
- **Limits:** no app launch, screenshot, database read or other device query beyond the install and the package's version/update time. The v3–v5 data reset happens on first open and is unobserved (UM-O7). Replaces the Phase 7 build `8a7cbdb9…`.

The install is expected to reset the app's data (PRD §17 11 October addendum), so blocks need setting up again first. Debug builds still carry the delta 5 disable 20 rung.

| ID | What to test | Steps | Fail | Pass |
|---|---|---|---|---|
| UM-O1 | Gate layout and motion | Open a blocked app. Tap "I'll do the challenge. Let me in"; tap it again. | Buttons move when the row opens; the meme is covered by the buttons or chips; the row snaps in with no stagger; the background or dimming changed. | Row label and 5/10/15/20 drop in left to right with a slight bounce, and fold away right to left on the second tap. "Nope, not now" is the deeper accent and stays the dominant button. The clear meme sits about 76 dp higher and is fully visible. |
| UM-O2 | Chosen length honoured | For each of 5, 10, 15 and 20: tap the chip, complete the challenge, watch the bubble and notification; let one pause expire. | Wrong starting time; any extra confirm step; no re-arm. | A chip tap highlights the chip for a moment (about 0.2 s) and then goes straight to the challenge. The pause counts down from the chosen length and re-arms at expiry. |
| UM-O3 | Length survives interruptions | Pick 5, leave the challenge for under 15 s, return and finish. Rotate mid-challenge and finish. Pick 10 and tap "Nope, not now" on the challenge. | The pause length changes after leaving or rotating; a pause starts after the nope. | 5-minute and kept pauses as chosen; the nope starts no pause. |
| UM-O4 | Step 3 when creating | New block → step 3. | A card is preselected; Next works before a pick; a sheet or "Adjust the details" appears; the old subtitle or a time estimate shows. | "How do you want to pause the block?", nothing selected, Next dimmed. Tapping a card unfolds one stepper; the other card folds it away. Typing 20…150 in steps of 10 (starts at 20); wait 20…240 s in steps of 10 (starts at 60). Hint line under the cards. |
| UM-O5 | Step 3 when editing | Turn a block off, edit its friction. | The stored method isn't open or its value is lost; the save is rejected. | The stored method is open at its stored value, and saving works. Entering directly from the block's detail, the card may unfold as the screen opens; that is expected. |
| UM-O6 | Copy elsewhere | Check Review (5/5), Block detail and Turn-on confirmation. | Any "Each pause lasts"/"Pause lasts" row or "~Ns" estimate. | No pause-length rows and no time estimates; only character counts and waits. |
| UM-O7 | First open after install | Open the app after installing. | Crash or hang. | It opens (data reset as accepted). Database checks for `pause_started.minutes` and `block_created` wait for the launch-gate P7-D3/D4 audit. |

## Owner results — 22 October 2026

Arjun's report, 22 October 2026, on installed build `9d505bd8ec2d8654ac798cc3e7438aa50186322494936290c3a5ef8d9bb3325f`: "All the cases pass and everything works as expected."

| ID | Result |
|---|---|
| UM-O1 | PASS |
| UM-O2 | PASS |
| UM-O3 | PASS |
| UM-O4 | PASS |
| UM-O5 | PASS |
| UM-O6 | PASS |
| UM-O7 | PASS (opened without a crash; data reset as accepted) |

Recorded as one summary report; no per-check detail was given. Still not verified by these checks: the database assertions for `pause_started.minutes` and `block_created` (launch-gate P7-D3/D4 audit) and execution of the Android tests (UM-RN1, launch gate). The Android tests were then run on 23 October 2026: see the next section.

## Launch-gate Android test run — 23 October 2026, isolated emulator (agent-executed)

**Result: FAIL — the full instrumented suite reported `Tests run: 65, Failures: 15`. The launch-gate item "Run the Android tests" is not cleared.** Nothing was repaired; this section records the failures and a source-reading diagnosis, then stops, as instructed.

- **Authorization and executor:** Arjun's explicit instruction for this session — the isolated emulator (AVD `toki-p7d-isolated`) only, no other device access. Executed by Claude Sonnet 5.5 in the implementer role. No app code, test code, build file or dependency was changed.
- **Source/base:** `main` at `3ff1029`. `git diff 1060024 HEAD -- app` is empty, so the app and test sources are exactly code `1060024`. The only working-tree change is Arjun's unrelated, uncommitted deletion of `design_handoff_stats_screen/`, untouched and unstaged.
- **Build:** `./build.sh assembleDebug assembleDebugAndroidTest` → BUILD SUCCESSFUL (every task already up to date; no source had changed since the D3 build of 11 October 01:54). APK SHA-256: `app-debug.apk` `9d505bd8ec2d8654ac798cc3e7438aa50186322494936290c3a5ef8d9bb3325f` (byte-identical to the build installed on the phone on 22 October and recorded in the UM-14 install record above); `app-debug-androidTest.apk` `fc97c3bef554bf077774fdc18bb462949ac5fda3fca183d454188ee0ae7b6231`. Both installed on the emulator with `install -r` (streamed, Success ×2).
- **Environment (isolated):** AVD `toki-p7d-isolated` (pixel_6 profile, Android 16 / API 36, `default/arm64-v8a`), emulator 37.2.12.0, headless cold boot `-port 5554 -no-window -no-audio -no-boot-anim -no-snapshot`. Identity confirmed from the emulator itself: `ro.product.model` "Android SDK built for arm64", `ro.kernel.qemu` 1, `sys.boot_completed` 1. Shut down afterwards with `adb -s emulator-5554 emu kill`; no emulator process remained and the AVD stays on disk.
- **Device safety:** Arjun's phone `R5CW30ZBM2R` was plugged in over USB and was never addressed. Every adb command was `adb -s emulator-5554 …` (with `ANDROID_SERIAL=emulator-5554` also exported); no un-targeted adb command was run, including `adb devices`, so this run did not itself observe whether the phone was visible to adb. The Gradle commands were assemble tasks only (no install or connected tasks).
- **Command:** `adb -s emulator-5554 shell am instrument -w -r com.arjunrana.tokishrine.test/androidx.test.runner.AndroidJUnitRunner` — no class filter, so the whole suite. `-r` (raw status stream) was added to `-w` so the output carries a result per test. 23 October 2026, 00:09–00:12 IST (the runner reported `Time: 185.845` s). Raw output: [checks/launch-gate-instrumented-output.txt](checks/launch-gate-instrumented-output.txt) (1,605 lines, SHA-256 `0b21de6c874c9d894a1366c73dd16e8179e1ddf5ceafb9e994999aa81380ba09`).

### Results by test class

The runner's 65 counts the two classes that failed to initialise as one `initializationError` each; the suite declares 102 `@Test` methods, so 39 never executed. Of the 63 tests that did execute, 50 passed and 13 failed.

| Class | Declared | Passed | Failed | Not run |
|---|---|---|---|---|
| `data.BlockRepositoryTest` | 26 | 0 | 1 (class error) | 26 |
| `data.ChallengeRepositoryTest` | 12 | 12 | 0 | 0 |
| `data.EventRepositoryTest` | 13 | 0 | 1 (class error) | 13 |
| `data.StatsMigrationTest` | 4 | 4 | 0 | 0 |
| `data.StatsRepositoryTest` | 9 | 8 | 1 | 0 |
| `detection.AssetDetectionConfigLoaderTest` | 5 | 5 | 0 | 0 |
| `ui.AddMoreScreenTest` | 3 | 1 | 2 | 0 |
| `ui.BlockGateScreenTest` | 6 | 5 | 1 | 0 |
| `ui.CreateFlowScreenTest` | 21 | 13 | 8 | 0 |
| `ui.util.FeedbackMailTest` | 3 | 2 | 1 | 0 |
| **Total** | **102** | **50** | **15** | **39** |

### Failures and diagnosis

Diagnosis is from the raw traces and a read of the source and git history only. No test was re-run and no logcat or other device output was gathered, so each cause below is a reading of the evidence, not an experiment. None of the failures points at Room, the v6 schema, migration or pause-length persistence: `StatsMigrationTest` (4/4) and `ChallengeRepositoryTest` (12/12) passed.

| ID | Failing test(s) | Error | Diagnosis |
|---|---|---|---|
| LG-1 | all of `BlockRepositoryTest` and `EventRepositoryTest` (39 tests not run) | `InvalidTestClassError … Method addTargetsRespectsOwnershipAtomically() should be void` / `… existingRowsSurviveReopenWithoutDestructiveFallback() should be void` | Test-source defect. Both methods are written `= runBlocking { … }` and end in an expression that is not `Unit` — `assertThrows(…)` (returns the exception, `BlockRepositoryTest.kt:600`) and `…getDatabasePath(dbName).delete()` (returns Boolean, `EventRepositoryTest.kt:252`) — so Kotlin infers a non-void return type and JUnit 4 rejects the whole class. It compiles, which is why compile-only checks never showed it. Both signatures first appear in the Phase 7 commits `89ef9e3` and `c19b53f`. Consequence: the dedicated same-version-reopen test of UM-RN1 (`EventRepositoryTest.kt:252–288`) did not run. |
| LG-2 | `FeedbackMailTest.mailtoUriParsesBackToRecipientSubjectAndBody` | `ComparisonFailure: expected:<Slow [& odd? …]> but was:<Slow []>` | The parsed body stops at the first `&`. `FeedbackEmail.mailtoUri` percent-encodes the body (`&` → `%26`), so the URI itself is well formed; the truncation fits `android.net.MailTo.parse` on API 36 splitting on `&` after decoding. Not confirmed from the platform source. Probably the test's oracle rather than the product, but it is the only check of the full round trip. |
| LG-3 | `AddMoreScreenTest.confirmSavesOnlyAdditionsToTheOnBlockAndExistingTargetsStayLocked`, `neverMindAndBackReturnWithoutSavingAnything` | `assertExists`: expected 1 node but found 2 for text `example.com` | Finder ambiguity. After "Add", `awaitText("example.com")` matches both the newly added site row and the still-focused, empty website field, whose placeholder is also `example.com` (node text `[, example.com]`). The row was added; the test input collides with the placeholder. |
| LG-4 | 8 of `CreateFlowScreenTest`: `searchBackAndStepBackLogExactSequence`, `createDraftSurvivesActivityRecreation`, `repeatedBackWhileLoggingSuspendsLogsAbandonmentExactlyOnce`, `frictionEditOnAnEmptyBlockCannotAdvance`, `editCancellationLogsNothing`, `frictionEditEntersAtStepThreeAndBackClosesSilently`, `rejectedSaveRetainsDraftAndShowsInlineMessage`, `editDraftSurvivesRecreationWithoutPrefillOverwrite` | `Failed to inject touch input … could not find any node` for the back glyph `` (`BACK_GLYPH`); "the unmerged tree contains 1 node that matches" | Stale finder. Since `89ef9e3` (23 September, Phase 7 accessibility pass) the shared back control is `clearAndSetSemantics { contentDescription = "Back"; role = Button }` (`NocturneUi.kt:291`), which hides the glyph from the merged tree. The tests still click `onNodeWithText(BACK_GLYPH)`, a finder written in Phases 2–3 and not updated for that change. They would need to find the control by its "Back" description. |
| LG-5 | `BlockGateScreenTest.foldingRightAfterAChipTapReportsNothing` (`BlockGateScreenTest.kt:125`, `assertTrue(entered.isEmpty())`) | `AssertionError` | **The only failure on the unlock-minutes feature; cause not established.** The gate reports a pick from `LaunchedEffect(picked) { delay(GATE_SELECTION_HOLD_MS = 200); onEnterChallenge(picked) }` (`BlockGateScreen.kt:161–164`), and folding sets `picked = null`, which should cancel it. In the test, `performClick()` ends with Compose's idle sync, which by default fast-forwards virtual time, so the 200 ms hold most likely elapses and reports before the test folds the row; the test's `Thread.sleep(500)` is wall-clock time and does not help. If so, the test never exercises "fold inside the hold" and would need `mainClock.autoAdvance = false`. This run neither confirms nor rules out a real defect in that path (a pick reported after the fold). |
| LG-6 | `StatsRepositoryTest.enabledSiteBlockShowsHostingBrowserRowWithSiteSavingsAndFirstBrowserVisitSpent` (`StatsRepositoryTest.kt:200`) | `expected:<[app]> but was:<[browser, app]>` | Likely a stale test expectation. After the site block is turned off the test expects the browser row to disappear, but `leaderboardPackages` (`StatsRepository.kt:185–195`, added in `c19b53f` for the 8 October §17 addendum, P7-F10) keeps any package with a counted outcome this week listed even when its block is off, and the test records counted site outcomes for `browser` that week. Confirm against PRD §17 before changing either side. |

### What this run covers of UM-RN1 ([REVIEW](REVIEW.md))

- **Covered (executed, passed, emulator API 36):** first open of v3, v4 and v5 files as an empty v6 database through the shipped builder, with rows written at v6 surviving a reopen, and the fallback scoped to versions 3–5 (`StatsMigrationTest`, 4/4); the open pause row at the gate surviving `ActivityScenario.recreate()` (`BlockGateScreenTest.theOpenRowSurvivesRecreation`); Room-level pause/chosen-length transaction rules (`ChallengeRepositoryTest`, 12/12, including `pauseCompletionWithoutAValidChosenLengthWritesNothing` and `autoAwayNopeTakesTheChosenNopePathWithItsOwnSource`).
- **Not covered:** the dedicated same-version reopen test (`EventRepositoryTest.kt:252–288`, LG-1), partly overlapped by the v6 reopen inside the passing `StatsMigrationTest`; chosen minutes surviving Android recreation or process restoration in `BlockActivity.kt:702–765`, which no instrumented test exercises (only the JVM snapshot tests do), plus the failing fold-inside-the-hold test (LG-5). Those parts remain with Arjun's phone checks UM-O2, UM-O3 and UM-O7, which he reported PASS above.
- So UM-RN1 is **partly executed on Android, not closed**; do not report the Android tests as executed PASS.

### Limits

Stock emulator SQLite/Room (AOSP-like, API 36 arm64), not Arjun's Samsung OEM storage; single run; no re-run to separate deterministic from flaky failures; no diagnostics beyond the raw output. Only the instrumented process ran: no manual app launch, screenshot or UI session. The emulator's own app data was not preserved or inspected. P7-D3/D4 (the phone database and usage audit) are unaffected and remain open.

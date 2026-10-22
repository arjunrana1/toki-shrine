# TS-launch-gate-tests — owner checks

## LG-7 re-run — 23 October 2026, isolated emulator (agent-executed)

**Result: PASS. Final full suite `OK (102 tests)`: every declared instrumented test executed and passed.** The launch-gate item "Run the Android tests" is satisfied by this evidence.

- **Authorization and executor:** Arjun's explicit instruction this session: "I authorize the LG-7 re-run on the isolated emulator only (AVD toki-p7d-isolated, emulator-5554). Never address my phone." Executed by Claude Opus 5.5 as implementer.
- **Source:** `main` at `f10fe2f` (D1) plus the one-line D2 repair found by run 1. The final run used the D2 tree, [D2-SUBMISSION.sha256](D2-SUBMISSION.sha256). No app code changed.
- **APK SHA-256:** `app-debug.apk` `9d505bd8ec2d8654ac798cc3e7438aa50186322494936290c3a5ef8d9bb3325f` (unchanged; the build on Arjun's phone). `app-debug-androidTest.apk`: run 1 `def129a806fe6055ea77270c8814d02090b006d32c287d6f6b78b4d6d890541a` (D1); gate re-run and final run `9b98f9b798919a1bb86ab6b43d9c732fff391dbe1899ccbb8782345d4acc3485` (D2). Both installed on the emulator with `install -r`.
- **Environment:** AVD `toki-p7d-isolated` (pixel_6, API 36, `default/arm64-v8a`), emulator 37.2.12.0, headless cold boot `-port 5554 -no-window -no-audio -no-boot-anim -no-snapshot`. Identity confirmed on the device: model "Android SDK built for arm64", `ro.kernel.qemu` 1. Shut down with `adb -s emulator-5554 emu kill`; no emulator process remained.
- **Device safety:** every adb command was addressed `-s emulator-5554`, with `ANDROID_SERIAL=emulator-5554` exported. No un-targeted adb command was run, so the phone `R5CW30ZBM2R` was never addressed. Gradle ran assemble tasks only.

### Runs

| Run | Time (IST) | Command scope | Result | Raw output |
|---|---|---|---|---|
| 1 (D1) | 00:44–00:48 | full suite, `am instrument -w -r` | `Tests run: 102, Failures: 1` | [lg7-run1-output.txt](checks/lg7-run1-output.txt) (SHA-256 `18cce3c4…`) |
| Gate class (D2) | ~00:49 | `-e class …BlockGateScreenTest` | `OK (6 tests)` | [lg7-gate-class-output.txt](checks/lg7-gate-class-output.txt) (`36fa1a46…`) |
| Final (D2) | 00:49–00:52 | full suite, `am instrument -w -r` | `OK (102 tests)`, `Time: 160.587` | [lg7-final-output.txt](checks/lg7-final-output.txt) (`36267c30…`) |

Full command: `adb -s emulator-5554 shell am instrument -w -r com.arjunrana.tokishrine.test/androidx.test.runner.AndroidJUnitRunner`.

Run 1's only failure was in D1's own LG-5 repair. `foldingRightAfterAChipTapReportsNothing` turned off auto-advance before the just-opened row had settled, so the "5 minutes" chip was not yet in the merged tree ("Failed to inject touch input … the unmerged tree contains 1 node"). D2 adds `compose.waitForIdle()` before turning the clock off. All 39 tests that had never run before (`BlockRepositoryTest`, `EventRepositoryTest`) passed in run 1 with no repair needed.

### Final run by class

| Class | Passed / declared |
|---|---|
| `data.BlockRepositoryTest` | 26 / 26 |
| `data.ChallengeRepositoryTest` | 12 / 12 |
| `data.EventRepositoryTest` | 13 / 13 |
| `data.StatsMigrationTest` | 4 / 4 |
| `data.StatsRepositoryTest` | 9 / 9 |
| `detection.AssetDetectionConfigLoaderTest` | 5 / 5 |
| `ui.AddMoreScreenTest` | 3 / 3 |
| `ui.BlockGateScreenTest` | 6 / 6 |
| `ui.CreateFlowScreenTest` | 21 / 21 |
| `ui.util.FeedbackMailTest` | 3 / 3 |
| **Total** | **102 / 102** |

### Real app bugs

None found. No test failed in a way that pointed at app behaviour; the one run-1 failure was the test's own clock setup.

### UM-RN1

Now executed on Android: first open from v3/v4/v5 and the v6 reopen (`StatsMigrationTest`), the dedicated same-version reopen (`EventRepositoryTest.existingRowsSurviveReopenWithoutDestructiveFallback`), the gate row surviving recreation, and a fold inside the selection hold not reporting a pick. Still covered only by Arjun's phone checks UM-O2/O3/O7: chosen minutes surviving `BlockActivity` recreation and process restoration, which no instrumented test exercises.

### Limits

Stock emulator SQLite/Room (API 36 arm64), not Arjun's Samsung; one passing full run (no repeated runs for flakiness). No app launch, screenshot or UI session beyond the instrumented process. Test repairs are self-verified, not independently reviewed. P7-D3/D4 (the phone database and usage audit) remain open.

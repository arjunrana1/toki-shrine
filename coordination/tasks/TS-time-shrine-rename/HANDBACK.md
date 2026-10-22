# TS-time-shrine-rename — handback

## D1 — TR-1 to TR-6, 23 October 2026

- **Implementer:** Claude Opus 5.5 (senior implementer, same session as the architect; Arjun's choice). Self-verified only: no independent review.
- **Base:** `main` at `499caf3`. Identity: [D1-SUBMISSION.sha256](D1-SUBMISSION.sha256) (138 files: everything under `app/`, `settings.gradle.kts` and the component contracts). PRD, AGENTS, README, CONTEXT-MAP, the PostHog plan note and the task records are outside the identity.

### Changes

- **TR-1:** `namespace`/`applicationId` `com.arjunrana.timeshrine`; `rootProject.name` "Time Shrine".
- **TR-2:** every source set's package tree was moved with `git mv` (`main`, `debug`, `release`, `test`, `testDebug`, `testRelease`, `androidTest`). Package and import lines, package-bearing strings (such as the self-exclusion package in `ActiveBlockIndexTest`) and source paths in docs were rewritten. The diff is 669 changed lines in each direction, and git sees every file as a rename.
- **TR-3:** `TimeShrineApplication`, `TimeShrineDatabase` (file `time-shrine.db`), `TimeShrineAccessibilityService` (manifest, TAG, settings-component check), `Theme.TimeShrine`.
- **TR-4:** the schema folder moved to `app/schemas/com.arjunrana.timeshrine.data.db.TimeShrineDatabase/` (3–6.json). The clean build re-exported `6.json` byte-identical (git status shows pure renames). `StatsMigrationTest` reads the new asset path.
- **TR-5:** "Time Shrine" in `app_name`, the Accessibility description, `detection_config.json` (and its test mirror), Settings "About Time Shrine", the permission checklist line, the Stats sheet ("…your 7 days before Time Shrine…") and code comments. The product-name pass replaced the bare word "Toki" only when it wasn't part of a longer identifier or path.
- **TR-6:** PRD (outside the rename addendum), AGENTS, README, CONTEXT-MAP, all component contracts and the PostHog plan note now say Time Shrine and use the new paths. Kept as written: `toki-shrine-ui-mockups/` (also a comment in `Color.kt`), AVD `toki-p7d-isolated`, `design/regenerate-screens.py`, `.gitignore`, `docs/history`, `docs/phases`, `Verification Feedback/` and every closed task record. Their links to old `app/src/.../tokishrine/` paths are historical and now point at moved files.

### Checks

- `./build.sh clean assembleDebug testDebugUnitTest testReleaseUnitTest assembleDebugAndroidTest` → BUILD SUCCESSFUL. Result XML (all classes under `com.arjunrana.timeshrine`): **278 tests, 0 failures, 0 errors** in each of debug and release, the same count as TS-config-values D1.
- `aapt2 dump badging` on the debug APK: `package: name='com.arjunrana.timeshrine'`, `application-label:'Time Shrine'`, launchable `com.arjunrana.timeshrine.MainActivity`.
- Debug APK `453208bdbbdb5f2fbba0374bae9e13bb73882af9b2b3d8fdd707b7cbfa7d28e4`; androidTest APK `fe0036fdc8299d552a17816606eaedb36a0eac19a54bd59884452e63f220bd1d`. Not installed.
- `git grep -i toki -- app settings.gradle.kts` finds only the `toki-shrine-ui-mockups` path comment. `git diff --check` is clean.
- Android tests: **compiled only**. The last executed run (102/102) predates both this rename and the config values.

### Limits

- Unobserved on a device: launcher label and icon, the Accessibility list entry, notifications and the new-install onboarding.
- The isolated emulator still has the old test package installed from earlier runs. A future Android test run installs the new packages alongside it; uninstall the old ones on the emulator only if they get in the way.
- Release signing and the Play listing are not part of this task.

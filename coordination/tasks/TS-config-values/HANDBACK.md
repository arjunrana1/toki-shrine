# TS-config-values — handback

## D1 — CV-1 to CV-7, 23 October 2026

- **Implementer:** Claude Opus 5.5 (senior implementer, same session as the architect; Arjun's choice). Self-verified only: no independent review.
- **Base:** `main` at `92cf6ce`. Identity: [D1-SUBMISSION.sha256](D1-SUBMISSION.sha256) (16 files: app sources, tests and the blocks/targets and persistence/events contract notes). PRD and task records are outside the identity.

### Changes

- **CV-1** `AppConfig`: typing 30..150/10, default 40; wait 30..120/10 s, default 30.
- **CV-2/CV-3** `ChallengeValues.kt`: release 150/200/300 (default 200) and 60/120/240 s (default 120); debug 20/200/300 and 20/120/240 s, both defaulting to the 20 rung.
- **CV-4** `disableCardCopy`: a 60 s rung reads "Wait for 1 minute.".
- **CV-5** `CreateFlowState.prefillDisable`: edit prefill moves a stored turn-off value that is off the ladder to the nearest rung (tie → harder), for both methods' columns, because `BlockRepository` validates both. Without it, editing any block saved with the old ladders (for example 350 characters) could not be saved.
- **CV-6** `TYPING_WORDS`: 214 → 330 (116 new everyday words, none removed), now alphabetical. By length: 3:20, 4:52, 5:69, 6:76, 7:46, 8:34, 9:22, 10:11.
- **CV-7** tests:
  - Value guards updated: `ProductionChallengeValuesTest`, `DebugChallengeValuesTest`, `CreateFlowStateTest` bounds, defaults, prefill clamp, disable copy and saver fixtures. Superseded names and values are noted in comments.
  - New `editPrefillSnapsStoredDisableValuesOntoTheLadder`. `typingWordsAreLowercaseDistinctAndNeverSoloPerLength` now pins exactly 330. Passage-length tests include the 300 rung.
  - Android fixtures that the new validation would reject were moved to rungs present in both variants (200 characters, 120/240 s), with expectations updated in `BlockRepositoryTest`, `CreateFlowScreenTest`, `AddMoreScreenTest`, `ChallengeRepositoryTest`, `StatsRepositoryTest` and `StatsMigrationTest`.
  - Invalid-input cases that became valid under the new values now use old rungs (350 characters, 360 s) or the old floor (20 characters), so they still test rejection.
  - Left as is on purpose: raw SQL rows for old schema versions (`StatsMigrationTest`); the deliberate amount mismatch (220) in `ChallengeRepositoryTest`; and JVM `Block` entities built without validation (`ChallengeRuntimeTest`, `ActiveBlockIndexTest`, `StatsRedesignTest`).

### Checks

- `./build.sh assembleDebug testDebugUnitTest testReleaseUnitTest assembleDebugAndroidTest` → BUILD SUCCESSFUL. Result XML: **278 tests, 0 failures, 0 errors** in each of debug and release (277 before plus the new snapping test). The first run failed one literal default pin (`freshDraftStartsOnApprovedDefaults`, 20/60), which was updated to 40/30 before the passing run.
- Android tests: **compiled only**. The last executed run (102/102, 23 October) predates this change.
- Debug APK `c8073c4fec959f4a1a536d7d36de037412b6645667517d849c4f09ca15df86d3`; androidTest APK `0f7c0e948de9ce21cd40a76f52fd3f173177c7ea7f8a0201805197b741e7d129`. Not installed.
- `git diff --check` clean. A grep of the tests for the old values finds only the intentional cases listed above.

### Limits

- No device or emulator run. The new steppers, ladders and the "1 minute" copy are unobserved. The rename comes next and installs as a new app, so owner checks are best run on the renamed build.

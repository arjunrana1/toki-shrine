# Independent review — TS-P5-owner-corrections

- **Reviewed submission:** `7fbb692` (`Apply Phase 5 owner testing corrections`)
- **Base:** `59596a8`
- **Verdict:** **PASS WITH NOTES** — code approved for owner retesting; owner acceptance and device evidence remain separate.
- **Reviewer:** fresh independent Codex, 23 September 2026. No device, emulator, adb, installation, screenshot, or instrumented execution was performed.

## Scope and method

Reviewed the full `59596a8..7fbb692` diff (33 files) plus enough surrounding context to substantiate the correctness claims: `ChallengeRuntime`, `BlockActivity` lifecycle paths, the accessibility launch flags, `isValidDetectionLaunch`, `InstalledAppsRepository.labelFor`, the Nocturne `Type.kt` slot used by the gate headline, and the debug/release source sets. Confirmed `418b6dd` (routing commit) is records-only, so the review surface is exactly the submission.

## Verified correctness (required items)

- **Nonterminal backgrounding/lock/Back.** `onStop` and the `ACTION_SCREEN_OFF` receiver route to `ChallengeRuntime.onBackgrounded()`: DELAY progress resets to zero with a generation bump; TYPING state is untouched. No `walk_away`, `challenge_abandoned`, or `turnoff_abandoned` is emitted on any background path — `.abandon(` has no remaining main-source callers. Back during `ACTIVE` is `moveTaskToBack(true)`.
- **Stale waiting ticks.** The ticker is cancelled before the reset in both background paths, ticks carry the generation captured at scheduling (`startTickerIfNeeded`), and `tick` rejects stale generations; after reset, elapsed is zero. `ChallengeRuntimeTest.backgroundingResetsWaitingWithoutTerminalOutcomeAndInvalidatesOldTick` proves stale tick rejection and full-duration restart.
- **Typing preservation.** `onBackgrounded` is a no-op for TYPING; passage and entered text survive app-switch/lock in process (`backgroundingPreservesTypingPassageAndEnteredText`). Process-death/reboot survival is explicitly out of scope and not claimed.
- **Exactly-once explicit outcomes.** Escape and gate walk-away terminalize before returning the effect; the COMMITTING phase survives backgrounding (the `onStop` reset only fires for `ACTIVE`), and `commitSucceeded`/`commitFailed` remain token-gated. The Phase 6 seam is unchanged: completion still yields only `PauseRequested`/`BlockDisabled`; no access/pause/re-arm, bubble, or notification code was touched.
- **Raw target vs display label.** Detection validation (`isValidDetectionLaunch`), `ChallengeConfig.target`, block-shown and walk-away events all keep the raw package/domain. The resolved label exists only in the gate headline via `labelFor`, which falls back to the package name on lookup failure (the required safe fallback).
- **Debug/release bounds.** Debug source set: pause minima 20 chars/20 s, disable ladders 20/350/700 chars and 20/360/720 s. Release source set: 100/60 and 220/350/700 plus 180/360/720. Defaults 150/60, maxima 200/300, pause 5–100 min, and the middle-rung preselection are unchanged and variant-independent. Persistence validation reads the variant values on every write path; PRD §17 addendum, `blocks-targets.md`, and `phase-05.md` all carry the Phase 7 removal obligation.
- **Submission-gated mismatch state.** `canSubmitTyping` disables the button below passage length and `submitTyping` re-guards it in the runtime; Enter/IME action no longer submits (`ImeAction.None`, newline rejected by the existing edit filter); mismatch spans and the visual transformation render only when `showTypingMismatches` is set by a failed explicit Submit, and any edit clears it. Paste suppression (`acceptTypingEdit`, no-menu toolbar) and autocorrect/prediction flags are unchanged.
- **Presentation corrections.** Seven approved headline templates with literal `{target}` substitution rotating independently of the eleven humour lines and eleven backgrounds (new `sys_block_7–11` drawable SHA-256s match the owner-supplied assets); two-layer cropped-ground plus centered aspect-preserving gate image; centered regular Inter headline (`headlineMedium` is Inter, weight Normal override; no `Anton` usage remains in source); equal 58 dp CTAs with `🚩🤨`; walk-away count ends `🎉` with 10-second auto-dismiss and both dismissal paths routing to Android home; `singleTop` + recents retention + `onNewIntent` guard bring an unfinished challenge forward instead of replacing it; the waiting screen matches the reference structure (timer ring, "Stay on this screen", reset explanation, no Counting pill, full-width outlined *Never mind*).

## Evidence attribution

- `./build.sh testDebugUnitTest` — **PASS, 146/146** re-counted by the reviewer from the fresh XML in `app/build/test-results/testDebugUnitTest/` (16 suites, 0 failures/errors/skipped).
- `./build.sh assembleDebug` and `./build.sh assembleRelease` — APKs present at 23 Sep 00:44–00:45 (`app-debug.apk`, `app-release-unsigned.apk`), consistent with the handback; release includes the production-limit source set and lint-vital.
- `./build.sh assembleDebugAndroidTest` — compile-only per handback; no instrumented test executed.
- `git diff --check` — clean, re-run by the reviewer.
- Owner asset hashes — recomputed and matched by the reviewer.

## Notes (non-blocking)

- **RV-N1 — process-death restore can resurrect partial waiting progress.** `onSaveInstanceState` folds the pre-reset `visibleSinceMs` into `accruedVisibleMs` before `onStop` resets the runtime. If the backgrounded process is killed and the activity is later recreated from the retained task, the waiting challenge can resume from partial progress (and even complete shortly after return) rather than restarting at the full duration. The task and PRD addendum explicitly exclude process-death restoration, and no stale tick or terminal event is involved, so this is inside the declared undefined zone — listed so owner device testing of recents relaunch after process death is not mistaken for a required-behavior failure.
- **RV-N2 — release bounds have no executed test.** `testDebugUnitTest` runs the debug source set, so the release values are evidenced by source inspection of `app/src/release/.../BuildVariantChallengeLimits.kt` plus the successful `assembleRelease`, not by a test run. Accepted; the constants are trivially inspectable and variant source sets are not unit-testable in the debug run.
- **RV-N3 — retained abandon path is now unreachable.** `ChallengeRuntime.abandon(reason)`, `AbandonReason`, and the activity's `Abandoned` handler have no live callers; PRD §8 records the event as schema-history only. Deliberate retention, tested as a pure-runtime capability; no Phase 5 action.

## Limits

UI appearance on device, installed-app label resolution, Android-home routing, recents/relaunch and screen-lock handling, IME/keyboard behavior, and the 10-second walk-away timing remain owner-device evidence after this review. Compilation is not instrumented execution. No prior-phase records were reopened; P4-09 Samsung overnight survival remains an owner-accepted deferred risk.

## Next actor

Arjun: request installation of a fresh debug build at `7fbb692` and retest per [OWNER-CHECKS](OWNER-CHECKS.md) P5C-O1–O11, recording results with build attribution. Phase 6 remains unstarted.

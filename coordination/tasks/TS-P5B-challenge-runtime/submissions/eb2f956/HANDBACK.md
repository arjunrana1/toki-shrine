# Senior Codex handback — TS-P5B-challenge-runtime

- **Submission:** `eb2f956` on base `9e3a373`. The base contains independently approved P5A code submission `3ba8037` plus records-only commits.
- **Implementer:** senior Codex, 21 September 2026.
- **Scope:** Phase 5 challenge lifecycle, `BlockActivity` integration, walk-away accounting, events and atomic disable completion. No Phase 6 access/pause/re-arm behavior was added.

## Delivered

- Added a pure deterministic `ChallengeRuntime` with stable session identity, injected monotonic time inputs, explicit generations and one terminal owner. It covers fresh typing passages, retained mismatch text, unlimited retries, visible-only delay accrual, configuration restoration, stale callbacks, escape/abandonment separation, completion/cancellation ordering and safe persistence retry.
- Replaced the Phase 4 placeholder with a thin `BlockActivity` adapter over the P5A surfaces. It resolves the stored block and target defensively, keeps the screen awake only for a live challenge, distinguishes screen-off from app-switch abandonment, restores live state without repeating starts, preserves one-shot resumed `block_screen_shown`, and auto-dismisses the persisted walk-away moment after two seconds.
- Routed both list and detail OFF switches through the stored typing/waiting disable challenge. Successful disable atomically commits the OFF mutation with `challenge_completed`, optional `countdown_completed`, `turnoff_completed` and `block_turned_off`; failure rolls the whole operation back and remains retryable. Escape records `turnoff_abandoned` and never changes the block.
- Added a transactional challenge repository. Walk-away insertion precedes the inclusive local-day global count in the same transaction; internal `app_meta` session markers make started, completion and walk-away retries idempotent without adding noncanonical §10 event params.
- Pause completion records the Phase 5 completion/countdown outcome, then exposes `PauseRequested(blockId, pauseMinutes, sessionId)` through the Activity result seam. It does not grant access, launch the blocked target, begin a pause, re-arm, show a bubble or create a notification.

## Verification

- `./build.sh assembleDebug` — **PASS**.
- `./build.sh testDebugUnitTest` — **PASS, 142/142**; 13 new pure runtime/content/launch tests.
- `./build.sh assembleDebugAndroidTest` — **PASS, compile-only**; six new Room repository tests compile but were not executed.
- `git diff --check 9e3a373..eb2f956` — **PASS**.

The new JVM suite covers exact/mismatch typing, unlimited retry, segmented monotonic countdown, app-switch/screen-off/explicit escape, both terminal-race orders, stale generations, recreation, duplicate callbacks and failed-write retry. The compile-only Room suite covers walk-away ordering/count, disable events and mutation, forced-event rollback, retry, at-most-once completion, waiting completion, and missing/disabled/changed blocks.

No device, emulator, adb, installation, screenshot, database extraction or instrumented-test execution occurred. These results are implementer self-verification, not independent review or runtime/device proof.

## Remaining gates

- Independent review must inspect `eb2f956` against this task before owner acceptance.
- The six Room tests still require separately authorized instrumented execution if the reviewer/owner requires executed rollback evidence.
- Owner Phase 5 acceptance remains open, including lifecycle/lock/app-switch behavior, keep-awake, IME/input, visual surfaces, walk-away auto-dismiss/count and both disable ladders.
- Phase 6 alone owns consuming `PauseRequested`, temporary access, opening/returning to the target, pause timing, automatic re-arm, bubble and ongoing notification. None is claimed here.

Next prompt: `Read AGENTS.md and review TS-P5B-challenge-runtime submission eb2f956 against base 9e3a373. Write REVIEW.md, update CURRENT, and stop before owner testing or Phase 6.`

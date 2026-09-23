# TS-P6-pause-lifecycle — Phase 6 pause lifecycle

- **State:** `ready_for_review` — P6C-R1 repaired in `f7bb1f2` (on records-only base `69cd2be` over `7fb322e`): the dismissal decision now captures an immutable `BubbleDismissalSnapshot` (displayed block + live pause instances) at release, before the 140 ms exit animation, and applies it after the animation; a pause starting in that interval re-shows the bubble and `bubble_dismissed` keeps the release-time block attribution. Same commit carries direct owner correction P6C-O7 (Dismiss label dark chip). Focused JVM regression added (177/177). Route `7fb322e..f7bb1f2` for fresh review; see [HANDBACK](HANDBACK.md) and [REVIEW](REVIEW.md).
- **Goal:** consume the Phase 5 `PauseRequested` seam and implement the full pause lifecycle (PRD §§4/5/6/7.3/10/12/13, [phase 6](../../../docs/phases/phase-06.md)): per-block temporary access, monotonic pause timing, automatic immediate re-arm, simultaneous independent pauses, the overlay bubble (screen 20) and the ongoing notification (screen 21).
- **Implementation owner:** GLM, by Arjun's direct 23 September 2026 assignment. The phase doc's senior default for monotonic timing/concurrent pauses/re-arm stands superseded for this task only by that instruction; the compensating controls are (a) the pause state machine is a small pure component with focused JVM coverage for expiry/re-arm, simultaneous pauses, exact target isolation and clock-change resistance, and (b) if cancellation, concurrency, persistence ordering or re-arm correctness becomes unclear, the implementer stops and documents the exact issue for Codex review instead of guessing.
- **P6-R1 repair owner:** senior Codex, per the independent review's bounded concurrency/timing assignment; repair submission `062c71e`.
- **Base:** `6533dbd` (records-only HEAD at assignment; app source identical to reviewed Phase 5 `68fdcb3`).
- **Authority:** PRD §§4–8/10/12/13/17 (pause duration 5–100 min, no pause extension, immediate re-arm with no warning, monotonic elapsed time, bubble/notification behavior, §10 pause events), [phase 6 scope](../../../docs/phases/phase-06.md), [persistence/events](../../../docs/components/persistence-events.md), [navigation/permissions](../../../docs/components/navigation-permissions.md), [theme/UI](../../../docs/components/theme-ui.md), [build/validation](../../../docs/components/build-validation.md), and the closed Phase 5 task records (RV2-N1 note: `ChallengeEffect.Abandoned` has no detection-suppression release and must not be revived for PAUSE sessions).

## Scope

- Consume `ChallengeTerminalResult.PauseRequested` after the committed pause completion: start the pause, log `pause_started`, and keep the existing activity-result seam and all Phase 5 events/behavior unchanged.
- Per-block temporary access: while a block is paused, every app and site in that block — and no others — leaves detection; other blocks enforce independently.
- Monotonic pause timing via `SystemClock.elapsedRealtime` semantics only; a device clock change (forward or back) can neither shorten nor extend a pause.
- Automatic re-arm: at expiry the pause ends, `pause_expired` logs once, the block's targets return to detection immediately, and a currently-foreground blocked target is re-evaluated so the gate reappears without waiting for a new window event.
- Simultaneous pauses are independent: one per block, each with its own expiry and notification; the bubble shows the soonest expiry.
- Overlay bubble (PRD §6 screen 20): pill with hourglass glyph, the open block's name and the remaining time; draggable; tap returns to Toki Shrine and logs `bubble_tapped`; shown only while `Settings.canDrawOverlays` is granted and at least one pause exists; `bubble_shown`/`bubble_dragged` per §10.
- Ongoing notification (PRD §6 screen 21): one per active pause, ongoing (non-swipeable) for its duration, system chronometer countdown, progress bar, tap opens that block's detail screen; a foreground service hosts it so the countdown survives backgrounding.
- Pause state is process-local by design: process death/reboot drops the pause and re-arms (the safe direction), matching the phase's no-restoration requirement; document the residual deep-sleep expiry-delay risk.

## Exclusions

- No Phase 7 work (debug-limit removal, Stats UI, feedback, polish), no rename, no new dependencies or toolchain changes (manifest declarations required by the foreground service/notification are in scope), no device/emulator/adb operations, no schema changes, no weakening of existing tests. The existing `PauseRequested` seam, Phase 5 events and accepted owner corrections stay intact.

## Required correctness

- A pause can never outlive its monotonic deadline and can never be extended; a duplicate start for an already-paused block restarts that pause and is expected not to occur through detection.
- Expiry is exactly-once per pause: exactly one `pause_expired` event, one access removal, one re-arm; idempotent against duplicate/delayed evaluation callbacks.
- Only the paused block's targets leave detection; app/site isolation and per-key repeat suppression in `DetectionEngine` remain unchanged (filtering happens in the enabled-block index and a launch-time paused guard, not by editing engine rules).
- Blocks turned off or deleted mid-pause drop that pause silently (no `pause_expired`).
- Overlay permission denied → no bubble and nothing else changes; notification permission denied → no visible notification and nothing else changes.
- All timing decisions come from injectable monotonic clocks in pure components; wall clock is used only for event timestamps and notification chronometer anchoring.

## Checks and stop

- Focused JVM coverage for: expiry/re-arm exactly-once with idempotent re-evaluation; independent simultaneous pauses; exact per-block target isolation in the detection index; clock-change resistance (expiry follows only the monotonic clock); restart-not-extend; disabled/deleted block drop; pause formatting/notification anchoring helpers; manifest pause-service declarations. Keep Android/Room coverage compiling.
- Run `./build.sh assembleDebug`, `./build.sh testDebugUnitTest`, `./build.sh assembleDebugAndroidTest` (compile-only), and `git diff --check`.
- Save a handback, update CURRENT to `ready_for_review`, and stop for independent review. Device behavior (bubble drag/appearance, notification countdown/swipe resistance, re-arm timing, clock-change via `adb shell date`) is Arjun's acceptance evidence.

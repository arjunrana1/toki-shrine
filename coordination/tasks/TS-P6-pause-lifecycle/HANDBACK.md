# Handback — TS-P6-pause-lifecycle P6-R1 repair

- **Repair submission:** `062c71e` (`Expire paused access on wake enforcement`), 23 September 2026.
- **Repair base:** `d93162d` (records-only review verdict over GLM submission `1056fca`; original Phase 6 base `4f18bda`).
- **Implementer:** senior Codex. **State:** `ready_for_review`.
- **Finding addressed:** P6-R1 from [REVIEW](REVIEW.md). No other Phase 6 behavior was intentionally changed.

## Repair

- `PauseRegistry.evaluateForAccess(blockId, now)` now atomically advances every overdue monotonic deadline before answering whether the requested block is paused. Registry removal remains the single exactly-once gate; repeated timer, screen-on and enforcement evaluations return no second expiration.
- `PauseCoordinator.isPaused` uses that deadline-aware decision and publishes/logs any expirations on the main thread. A stale registry entry can no longer grant access after its deadline even if the scheduled Handler callback was delayed.
- `TokiAccessibilityService` evaluates pauses on service connection and before every accessibility enforcement event. An overdue expiry publishes the non-paused set; the existing combined index then restores the block's targets and re-feeds the foreground window. Re-feed was moved explicitly onto the main thread.
- `PauseService` registers a process-lifetime `ACTION_SCREEN_ON` receiver while pauses are hosted and evaluates on screen-on and service restart. This removes overdue bubble/notification state on the first wake boundary rather than waiting out Handler's slept uptime. Accessibility-event evaluation remains the independent access-enforcement backstop.
- Process-local state is unchanged: process death/reboot reconstructs an empty registry and therefore re-arms. `elapsedRealtime` remains the only deadline authority; wall clock and uptime are not used to decide access.

## Focused coverage

`PauseRegistryTest` adds two delayed-callback cases:

- elapsed time crosses the deadline without any scheduled `advance`; the first enforcement evaluation removes the pause and denies continued access;
- a later duplicate wake/enforcement evaluation emits no second expiration or re-arm.

The existing independent-pause, index restoration, restart-not-extend, clock-change and silent OFF/delete tests remain unchanged. Total JVM suite is now 170 tests.

## Checks run by the repair implementer on `062c71e`

- `./build.sh assembleDebug` — **PASS** (`BUILD SUCCESSFUL`).
- `./build.sh testDebugUnitTest` — **PASS, 170/170**; fresh XML count: 0 failures, 0 errors, 0 skipped.
- `./build.sh assembleDebugAndroidTest` — **PASS** (compile-only; no instrumented execution).
- `git diff --check` — clean.

An initial sandboxed targeted-test attempt could not create Gradle's external cache lock; the same targeted `PauseRegistryTest` command was rerun with approved Gradle-cache access and passed. This was infrastructure permission handling, not a product/test failure.

## Preserved interpretations and limits

- Bubble content/tap and notification navigation are unchanged.
- P6-N1 remains an owner-observed platform limit: source requests an ongoing notification, but Android 13+ controls foreground-notification dismissal behavior.
- P6-N2 remains unchanged: `bubble_shown` is keyed to the displayed block ID; duplicate same-block pause starts are not expected through detection.
- The screen-on receiver, accessibility adapter, service rendering and foreground-window re-feed are Android wiring. They compile but are not executed by JVM tests; owner device evidence remains separate after code PASS.
- No device/emulator/adb/install/screenshot/instrumented operation, Phase 7 work, dependency/toolchain change, schema change or unrelated cleanup was performed.

## Stop

Ready for a fresh independent re-review of only `d93162d..062c71e` plus the affected expiry/enforcement dependencies. Owner device acceptance remains after code PASS.

# Independent review — TS-P6-pause-lifecycle

- **Reviewed submission:** `1056fca` (`Implement Phase 6 pause lifecycle, bubble and countdown notification`).
- **Base:** `4f18bda`
- **Verdict:** **FAIL — CHANGES REQUESTED.** The process-local pause model is acceptable, but the submitted expiry scheduler can leave a block paused after its monotonic deadline, including after the device has already woken. P6-R1 must be repaired and independently re-reviewed before owner/device acceptance.
- **Reviewer:** fresh independent Codex, 23 September 2026. No device, emulator, adb, installation, screenshot or instrumented execution was performed.

## Scope and method

Reviewed the full `4f18bda..1056fca` app diff (18 files) plus the task, handback interpretations, PRD §§4/6/7.3/10/12/13, Phase 6 scope, the detection engine/event adapter, Phase 5 completion/return path, application scope, navigation behavior and manifest declarations. Confirmed `1056fca` is directly based on `4f18bda`; records-only HEAD `7b34956` does not change `app/`. The worktree was clean at review start.

## Blocking finding

### P6-R1 — expiry scheduling uses the wrong clock and can keep access open after wake

- **Locations:** `PauseCoordinator.kt:142-149`; `PauseService.kt:60-73,304-309`; `TokiAccessibilityService.kt:121-138,256-263`.
- **Required behavior:** timing decisions use `SystemClock.elapsedRealtime`; a pause never outlives its monotonic deadline; expiry removes access and re-arms immediately. The handback additionally interprets the admitted deep-sleep callback risk as having immediate wake-time re-evaluation.
- **Failing scenario:** start a 5-minute pause, then let the device spend four minutes in deep sleep and wake after the elapsed-realtime deadline. `scheduleNextExpiry()` and both service tickers use `Handler.postDelayed()`. Android Handlers schedule on `SystemClock.uptimeMillis()`, which stops in deep sleep, so their callbacks retain the slept portion as extra delay. On wake, accessibility events go directly through the still-paused detection index; the launch-time guard calls `isPaused()`, which only checks registry membership and does not evaluate the elapsed-realtime deadline. No wake/window path advances the registry. The block can therefore remain open for roughly the sleep duration after wake, not merely until wake.
- **Consequence:** the pause outlives its monotonic deadline; `pause_expired`, access removal and foreground-window re-feed are delayed; the handback's “wake-time re-evaluation is immediate” claim is false for this implementation. The pure registry tests cannot detect the adapter time-base mismatch.
- **Required correction:** preserve `elapsedRealtime` as deadline authority, but add a wake-capable or wake-time evaluation path that guarantees stale membership cannot grant access after the deadline. At minimum, every enforcement decision/launch guard must synchronously evaluate due expiries before treating a block as paused, and wake/foreground re-evaluation must publish the changed set exactly once; if exact expiry during deep sleep is required rather than expiry at first wake, use an appropriate elapsed-realtime wake mechanism within platform/policy constraints. Add focused coverage around a deadline advancing past while delayed callbacks have not fired, including exactly-once expiration/re-arm and immediate foreground re-feed. Do not solve this by switching deadline authority to wall clock or uptime.

Android's `SystemClock` and `Handler` API documentation explicitly states that `elapsedRealtime()` includes deep sleep, while Handler scheduling uses `uptimeMillis()` and deep sleep adds delivery delay.

## Confirmed interpretations and non-blocking notes

- **Process-local pause state:** accepted for this task. A process restart reconstructs an empty registry, so no persisted pause is restored and the safe-direction product decision is re-arm. This does not cure P6-R1 while the same process survives sleep.
- **Bubble content/tap:** showing the soonest paused block's name plus remaining time and tapping to the app matches the written PRD; notification tap to detail is separate and correct in intent. With `NEW_TASK`, an existing app task is brought forward in its last state; avoiding `CLEAR_TOP` preserves a challenge above `MainActivity`.
- **P6-N1 — foreground notification dismissal is platform-limited:** `setOngoing(true)` is the correct app request, but Android 13+ lets users dismiss foreground-service notifications by default and also stop the whole app from Active apps. The owner check should record observed Samsung/Android 16 behavior without treating source inspection as proof of literal non-dismissability.
- **P6-N2 — `bubble_shown` is keyed to displayed block ID, not pause instance:** `bubbleShownLoggedBlockId` logs once while the same block remains displayed. A duplicate restart of that block while it stays soonest does not log a new `bubble_shown`, despite the handback wording “once per pause.” Duplicate start is declared not to occur through detection and the PRD does not specify event multiplicity, so this is not a blocker; either narrow the claim or key telemetry to pause identity in a later repair.
- **Android adapter evidence gap:** service/overlay/notification wiring remains compile-only and needs owner device evidence after P6-R1 passes review. Bubble permission behavior, drag/tap, multi-notification presentation, notification permission denial, notification dismissal, exact expiry/re-feed and clock-change behavior are not established by the 168 JVM tests.

## Evidence attribution

- `./build.sh assembleDebug` — implementer-reported PASS on `1056fca`.
- `./build.sh testDebugUnitTest` — implementer-reported PASS, 168/168. The 18 focused tests cover pure registry/format/index/manifest behavior, not Android scheduling or wake-time re-arm.
- `./build.sh assembleDebugAndroidTest` — implementer-reported PASS, compile-only.
- `git diff --check 4f18bda..1056fca` — clean, re-run by the reviewer.
- No build/test suite was re-run by the reviewer because compilation and existing JVM tests cannot answer P6-R1's Android Handler/elapsed-realtime mismatch.

## Next actor

Senior Codex implementation is recommended for the bounded P6-R1 repair because it is the task's central timing/concurrency invariant. Repair only the expiry/enforcement seam and focused tests, preserve the accepted process-local decision and all Phase 5 behavior, save a replacement handback, move the task to `ready_for_review`, and stop. No device work or Phase 7.

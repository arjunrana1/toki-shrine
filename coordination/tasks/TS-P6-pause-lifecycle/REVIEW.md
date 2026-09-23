# Independent review — TS-P6-pause-lifecycle

## P6C-R1 repair re-review

- **Reviewed repair:** `f7bb1f2` (`Snapshot bubble dismissal at release and add Dismiss label chip`).
- **Repair base:** `7fb322e` via records-only `69cd2be`.
- **Verdict:** **PASS WITH NOTES.** The immutable release-time snapshot resolves the original late-read and `bubble_dismissed` attribution defect. The pure policy correctly leaves a pause started after that decision outside the dismissed set. P6C-O7 is a bounded token-based visibility correction with no code-review blocker. Phase 6 returns to owner acceptance; this is not device/runtime proof.
- **Reviewer:** independent Codex, 23 September 2026. Review was limited to `7fb322e..f7bb1f2` and affected bubble render/callback dependencies. No device, emulator, adb, installation, screenshot or instrumented execution was performed.

### Result and non-blocking note

The dismissal decision is now captured synchronously at `ACTION_UP`, before the 140 ms cosmetic exit animation. `BubbleDismissalSnapshot` defensively copies the release-time block ID and pause-instance keys; both policy state and event attribution consume that snapshot after animation completion. A pause added after release is therefore not classified as dismissed.

- **P6C-N1 — theoretical animation/render ordering edge, owner-accepted defer:** if another pause starts and its flow renders during the 140 ms exit animation, that render can precede the completion callback that detaches the shared bubble, with no guaranteed subsequent `renderBubble()` call. Reproduction requires the new pause activation and render to land inside that narrow interval; normal sequential user interaction requires completing another challenge and does not credibly reach it. Arjun explicitly accepted deferral on 23 September 2026. Treat this as a non-blocking observation unless device testing or real usage reproduces a missing bubble; do not expand Phase 6 for speculative repair.

No actionable code finding remains in `7fb322e..f7bb1f2`.

### Evidence

- Reviewer-run `./build.sh testDebugUnitTest --tests com.arjunrana.tokishrine.pause.BubbleDismissalPolicyTest` — **PASS**, task `UP-TO-DATE` (`BUILD SUCCESSFUL`), confirming the committed inputs match the attributed passing run.
- Reviewer-run `git diff --check 7fb322e..f7bb1f2` — clean.
- Implementer-attributed `./build.sh assembleDebug` — PASS; full JVM suite — PASS, 177/177; focused policy suite — PASS, 7/7.
- Gesture/animation ordering and P6C-O7 readability remain owner/device acceptance, not JVM evidence.

### Next actor

Arjun performs the affected owner retest on a freshly installed build: ordinary dismissal, bubble return for a subsequent pause, `bubble_dismissed` attribution and Dismiss-label readability. Unaffected Phase 6 results retain their original build attribution. After acceptance, Phase 6 may close and Phase 7 may be scoped; no Phase 7 implementation begins before that clearance.

---

## Owner-corrections verification

- **Reviewed corrections:** `da7f8d5..7fb322e` (P6C-O1–O3, P6C-O5 plus Dismiss-label refinement, and P6C-O6).
- **Verdict:** **FAIL — CHANGES REQUESTED.** The notification cadence, gate scrim, app-bar spacing and turn-off challenge corrections are coherent, and the pure dismissal policy has focused passing coverage. The Android dismissal adapter defers taking the dismissal snapshot until after its exit animation, however, so a newly started pause can be suppressed as though it were present when the owner dismissed the bubble.
- **Reviewer:** Codex, 23 September 2026. Review was limited to the corrections delta and affected pause-service, overlay, interruption-copy and callback dependencies. No device, emulator, adb, installation, screenshot or instrumented execution was performed. The prior **PASS WITH NOTES** for `d93162d..062c71e` remains unchanged and applies only to that submission.

### Blocking finding

#### P6C-R1 — exit animation can fold a newly started pause into the dismissal snapshot

- **Locations:** `PauseBubbleView.kt:217-220,272-278`; `PauseService.kt:332-337`.
- **Required behavior:** dismissal hides the bubble only for pause instances visible at dismissal; any new pause instance shows it again.
- **Failing scenario:** the user releases the pill in the discard zone, then another block starts or restarts a pause during the 140 ms exit animation. `PauseBubbleView` invokes `onBubbleDismissed` only from the animation end callback, and both `shownBlockId` and `PauseService.activePauseKeys()` are read at that later time. A flow/tick render can update the displayed block during the interval, and the service records the newly added pause in `BubbleDismissalPolicy` even though it did not exist at release.
- **Consequence:** the new pause does not bring the bubble back, contrary to the Phase 6 owner addendum; the `bubble_dismissed` block attribution can also identify the post-release displayed block rather than the pill the user discarded.
- **Required correction:** capture the displayed block ID and the active pause-instance set at release/dismissal decision time, before starting the animation, and use that immutable snapshot for policy state and event attribution. Keep the animation cosmetic and do not broaden into pause timing, enforcement, notification behavior or the owner-accepted P6C-O4/P6-O13 cases. Add focused coverage for a pause arriving between dismissal decision and animation completion.

No other actionable finding was identified in the corrections delta.

### Evidence

- Reviewer-run `./build.sh testDebugUnitTest --tests com.arjunrana.tokishrine.pause.BubbleDismissalPolicyTest --tests com.arjunrana.tokishrine.ui.interruption.InterruptionModelsTest` — **PASS** (`BUILD SUCCESSFUL`). The first sandboxed attempt was blocked only by Gradle cache permissions; the approved rerun passed.
- Reviewer-run `git diff --check da7f8d5..7fb322e` — clean.
- Implementer-attributed `./build.sh assembleDebug` — PASS.
- Implementer-attributed full `./build.sh testDebugUnitTest` — PASS, 175/175.
- The bubble gesture/animation and Android overlay callback ordering remain source-reviewed and compile-tested, not device-executed evidence.

### Next actor

Implementer repairs only P6C-R1, updates the handback and routes the repair delta for fresh review. The installed `7fb322e` build may still be used to observe the already-recorded visual corrections, but Phase 6 acceptance cannot close on it. No Phase 7 work.

---

## P6-R1 repair re-review

- **Reviewed repair:** `062c71e` (`Expire paused access on wake enforcement`).
- **Repair base:** `d93162d` (records-only commit containing the original review below; app source at the base is the failed submission `1056fca`).
- **Verdict:** **PASS WITH NOTES.** P6-R1 is resolved for code review. The repair prevents stale registry membership from granting access after an elapsed-realtime deadline, preserves exactly-once expiry, and adds wake/reconnect/enforcement evaluation that republishes the active index and re-feeds the foreground window. Phase 6 can proceed to Arjun's device acceptance; this is not device/runtime proof.
- **Reviewer:** fresh independent Codex, 23 September 2026. No device, emulator, adb, installation, screenshot or instrumented execution was performed.

### Review scope and result

Reviewed only `d93162d..062c71e` plus the affected pause registry/coordinator, accessibility detection/index and foreground-service dependencies. Confirmed the range contains five app/test files and no unrelated behavior.

P6-R1 is resolved because:

- `PauseRegistry.evaluateForAccess` removes all overdue pauses and answers the requested block's access state under one lock. A delayed uptime callback therefore cannot make the launch-time guard return paused after the monotonic deadline.
- Registry removal remains the exactly-once gate. Timer, screen-on, accessibility and launch-guard evaluations can race or repeat, but only the first receives a `PauseExpiration`; later evaluations cannot log another expiry or initiate another pause-set transition.
- Expirations are published on the main thread. The accessibility collector rebuilds the enabled-block index from the resulting pause set and re-feeds the current foreground window on the main thread when the paused IDs change. This restores app/site enforcement without requiring a new window transition.
- `ACTION_SCREEN_ON`, accessibility service connection/reconnection and each accessibility event evaluate elapsed-realtime expiry. The synchronous launch guard remains the final enforcement backstop. Deadline authority is still `SystemClock.elapsedRealtime`; Handler uptime and wall time do not decide expiry.
- Process death/reboot still reconstructs an empty registry, and Phase 5 behavior is unchanged.

No blocking or actionable code finding remains in the reviewed repair.

### Evidence

- Reviewer-run `./build.sh testDebugUnitTest --tests com.arjunrana.tokishrine.pause.PauseRegistryTest --tests com.arjunrana.tokishrine.detection.ActiveBlockIndexTest` — **PASS** (`BUILD SUCCESSFUL`). The first sandboxed attempt was blocked only by Gradle cache permissions; the approved rerun passed.
- Reviewer-run `git diff --check d93162d..062c71e` — clean.
- Implementer-attributed `./build.sh assembleDebug` — PASS.
- Implementer-attributed full `./build.sh testDebugUnitTest` — PASS, 170/170.
- Implementer-attributed `./build.sh assembleDebugAndroidTest` — PASS, compile-only; no instrumented execution.

### Notes and remaining acceptance boundary

- The screen-on receiver, accessibility callback/index refresh, active-window re-feed, overlay and notification service remain Android wiring established by source review and compilation, not executed device evidence. Arjun still owns expiry-after-sleep, already-foreground app/site re-arm, simultaneous pauses, clock changes, bubble and notification acceptance.
- P6-N1 and P6-N2 below remain non-blocking and unchanged. In particular, Android 13+ notification dismissal behavior is a platform observation, not something `setOngoing(true)` can prove from source.

### Next actor

Arjun performs Phase 6 owner/device acceptance against the reviewed `062c71e` app tree, with the installed APK/build identity recorded separately. Do not begin Phase 7 until Phase 6 owner acceptance is complete.

---

## Original submission review (preserved)

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

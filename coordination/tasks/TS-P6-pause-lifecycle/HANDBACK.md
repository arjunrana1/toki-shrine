# Handback — TS-P6-pause-lifecycle

- **Submission:** `1056fca` (`Implement Phase 6 pause lifecycle, bubble and countdown notification`), 23 September 2026.
- **Base:** `4f18bda` (records-only Phase 5 closure + Phase 6 scoping on `6533dbd`; app source identical to reviewed Phase 5 `68fdcb3`).
- **Implementer:** GLM. **State:** `ready_for_review`.

## What changed

**New `pause/` package — the lifecycle core:**

- `PauseRegistry.kt` — pure per-block pause state machine (JVM). Monotonic deadlines only (`elapsedRealtime` semantics, injected clock); expiry emitted exactly once via idempotent `advance(now)`; restart-not-extend on a duplicate start; `retainBlocks(liveBlockIds)` silently drops pauses of blocks turned off/deleted (no `pause_expired`); `soonest()` for the bubble. `PauseFormat` holds the pure presentation math: m:ss clock text, the notification chronometer anchor (`wall now + monotonic remaining`) and the elapsed per-mille progress.
- `PauseCoordinator.kt` — application-scoped process owner. `startPause` publishes the pause synchronously on the main thread (so detection access begins before the user is returned to the triggering app), logs `pause_started(block_id, minutes)` best-effort, schedules expiry at the soonest monotonic deadline on the main handler, and starts the service. Expiry logs `pause_expired(block_id)` once per pause and re-publishes. An enabled-blocks Room flow drops pauses when their block goes OFF/deleted.
- `PauseService.kt` — specialUse foreground service (manifest-declared, unexported, FGS subtype property) started with the first pause, stopping itself when the last ends. One ongoing (non-swipeable) notification per pause: system countdown chronometer (`setUsesChronometer` + `setChronometerCountDown`, anchored from the monotonic remaining time at every post), per-mille progress bar, tap → that block's detail screen; the soonest pause's notification doubles as the foreground notification. Per-minute re-post and per-second bubble tickers re-derive everything from monotonic deadlines. Always satisfies the `startForegroundService` obligation before stopping.
- `PauseBubbleView.kt` — the overlay pill (screen 20): Phosphor hourglass glyph, block name ("what is open"), remaining time, Nocturne tokens (92%-opaque bg, accent border, pill radius), draggable with clamping (slop-separated tap vs drag), `TYPE_APPLICATION_OVERLAY` gated on `Settings.canDrawOverlays`; a failed/absent overlay degrades to no bubble with everything else intact.

**Wiring:**

- `BlockActivity` — after a committed pause completion, `PauseRequested` now calls `pauseCoordinator.startPause` before the unchanged `setResult` seam and return-to-triggering-app. All Phase 5 events/behavior untouched.
- `TokiAccessibilityService` — the block index is now `combine(blocks, pauses)`: a paused block's apps **and** sites leave detection (exact per-block isolation via `ActiveBlockIndex.from(..., pausedBlockIds)`); when the pause set changes, the current foreground window is re-fed through the engine once, so re-arm is immediate even while a blocked app is already on screen; `launchBlockScreen` adds a synchronous `isPaused` guard closing the index-re-emission race at pause start (and correctly allowing gates at re-arm).
- `MainActivity` — consumes a new `EXTRA_OPEN_DETAIL_BLOCK_ID` on create/new-intent (notification tap → block detail route, pushed once after launch resolution; no CLEAR_TOP, so a live challenge is never destroyed).
- `EventRepository` — §10 pause/bubble event constants (`pause_started`, `pause_expired`, `bubble_shown`, `bubble_dragged`, `bubble_tapped`).
- Manifest — `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_SPECIAL_USE` permissions, PauseService declaration; strings for channel/notification; `ic_stat_pause` status icon. No new dependencies; toolchain unchanged.

**Coverage (JVM, focused):** `PauseRegistryTest` (10) — exactly-once expiry/re-arm with idempotent re-evaluation, simultaneous independent pauses, soonest handover, exact per-block isolation, restart-not-extend, monotonic-only timing (wall-clock jumps have no input surface; backward jumps cannot extend), silent drop of OFF/deleted blocks; `PauseFormatTest` (4) — clock format, chronometer anchoring including the forward-wall-jump repost scenario, clamped progress; `ActiveBlockIndexTest` (+2) — paused block's apps/sites excluded while a neighbour stays enforced, empty pause set restores targets; `AndroidManifestTest` (+2) — FGS permissions and unexported specialUse PauseService declaration. 168/168 total; the 150 Phase 5 tests are unchanged and still pass.

## Checks (all run by the implementer on `1056fca`)

- `./build.sh assembleDebug` — **PASS** (BUILD SUCCESSFUL).
- `./build.sh testDebugUnitTest` — **PASS, 168/168** (0 failures/errors/skipped, counted from fresh XML in `app/build/test-results/testDebugUnitTest/`).
- `./build.sh assembleDebugAndroidTest` — **PASS** (compile-only; instrumented sources unchanged and up to date).
- `git diff --check` — clean. Legacy hex-color check: no new hex literals outside `ui/theme` (bubble/notification colors derive from Nocturne tokens).

## Interpretations the reviewer/owner should confirm

1. **Bubble shows block name + remaining time** (PRD §6 screen 20 "shows remaining time and what is open"); the mock pill shows time only. Soonest expiry is shown with two pauses, per the acceptance list. Bubble **tap returns to Toki Shrine** (home), matching the mock's "tap to go back" and §6 "returns to the app"; the notification tap goes to the block detail, per §6 screen 21.
2. **Process-local pause state** (no Room persistence): process death/reboot drops pauses and re-arms — the safe direction; matches the phase's no-restoration requirement. Deep sleep can delay the expiry callback (handler uptime), but deadlines are monotonic, so pauses can only end at/after their true duration; wake-time re-evaluation is immediate (tickers, service render, detection re-feed).
3. **Notification tap uses NEW_TASK|SINGLE_TOP without CLEAR_TOP**: if a challenge activity of another block sits above MainActivity, the tap just brings the task forward (no detail push, no destroyed challenge).
4. `bubble_shown` is logged once per pause while it is the bubble's displayed (soonest) block.

## Known gaps (for review, not waived)

- PauseService/PauseBubbleView/coordinator Android wiring has compile + pure-logic coverage only; no JVM or instrumented test executes the service, notification or overlay paths (adapter code, same honest gap class as RV2-N2). Device acceptance (bubble appearance/drag, notification countdown/swipe resistance, re-arm timing, `adb shell date` clock test) belongs to Arjun.
- Pause events (`pause_started`/`pause_expired`/bubble) are best-effort telemetry writes (`runCatching`), consistent with the existing service-event pattern; a failed write never cancels a pause.

## Stop

Handed off for fresh independent review. No device work, no Phase 7, no dependencies/toolchain changes, no unrelated cleanup performed.

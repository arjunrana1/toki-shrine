# Codex review — TS-P4-detection-engine

- **Reviewed submission:** `0ca1424` on base `0ddd569`.
- **Reviewer:** Codex, 20 September 2026.
- **Verdict:** **FAIL — changes requested.** The pure matching/config work is sound in the inspected paths, but four stale-state/outcome gaps can produce an incorrect placeholder or incorrect `block_screen_shown` evidence.
- **Review boundary:** submitted diff plus Phase 4, PRD §§10/13/14, the affected persistence/navigation contracts, and focused callers/tests. No device, emulator, installation, screenshot, database extraction or instrumented execution was performed.

## Findings

### P4-F01 — A pending site settle can fire after the browser is no longer foreground

- **Location:** `TokiAccessibilityService.kt:83-100,167-174`; `DetectionEngine.kt:181-201`.
- **Scenario:** Chrome schedules a blocked-site settle, then the user switches within two seconds to the launcher, Toki Shrine, or any other package outside the runtime filter. The service deliberately receives events only from blocked apps plus supported browsers, so that foreground change need not reach `onWindowStateChanged`. The delayed callback calls `onSettleElapsed` without checking `rootInActiveWindow`; the engine's cached Chrome window remains current and emits the trigger.
- **Consequence:** the placeholder can open over an unrelated foreground app, violating the required stale-window cancellation and typing/navigation safety boundary.
- **Required correction:** before settling, revalidate the actual active root/window/package against the scheduled browser state (cancel/no-op when unavailable or changed) while preserving the missing-address-bar cache rule. Add focused coverage for leaving a supported browser for an unfiltered package during the delay.

### P4-F02 — Turning a block OFF does not invalidate its already scheduled site trigger

- **Location:** `DetectionEngine.kt:90-94,181-201`.
- **Scenario:** a blocked URL schedules a settle and the blocks flow then removes or changes that domain before the callback. `onBlocksChanged` replaces the live maps but leaves `pending` intact; `onSettleElapsed` triggers the captured `BlockRef` without checking the current map.
- **Consequence:** an OFF/deleted/reassigned block can still show the placeholder, contrary to enabled-block ownership and the claim that OFF blocks leave detection on the next emission.
- **Required correction:** cancel/invalidate pending work when its domain no longer maps to the same active block, or revalidate the current mapping before emitting. Cover removal and ownership/ref change in deterministic JVM tests.

### P4-F03 — Reusing a window ID for another package retains the old package/cache

- **Location:** `DetectionEngine.kt:97-103,241-250`.
- **Scenario:** while a Chrome URL is settling, `onWindowStateChanged` receives the same window ID with a different package. Cancellation checks only the window ID, and `touchWindow` reuses the immutable old `packageName` and cached URL instead of resetting the entry.
- **Consequence:** the explicit package-change invalidation invariant is false; the old Chrome settle can still pass validation and trigger for a different package occupying the reused ID.
- **Required correction:** treat `(windowId, packageName)` as the cache identity: reset cached state and invalidate pending work when the package changes. Add a same-window-ID/different-package JVM regression test.

### P4-F04 — `block_screen_shown` is recorded for an accepted launch request, not a shown screen

- **Location:** `TokiAccessibilityService.kt:182-209`.
- **Scenario:** `startActivity` returns without throwing, but presentation can still fail or be delayed after the request is accepted. The service immediately writes the fixed §10 `block_screen_shown` event and records engine-decision latency (`0`/settle time), before `BlockActivity` has reached a presented lifecycle state.
- **Consequence:** the event can claim an outcome that did not occur and its `latency_ms` omits launch-to-screen time, conflicting with the task's “without fabricating outcomes” boundary.
- **Required correction:** acknowledge/log from the placeholder when it is actually created/presented, carry the detection start timestamp and canonical trigger data across the intent, and prevent duplicate logging on recreation. Keep launch failure silent. Add the narrowest feasible lifecycle/unit coverage; final presentation/latency remains owner-device evidence.

## Evidence and limits

- Independently inspected the exact range and confirmed `0ca1424^ == 0ddd569`; `git diff --check 0ddd569..0ca1424` passed.
- Independently ran `./build.sh testDebugUnitTest`: **101 tests, 0 failures, 0 errors**. These tests pass but do not exercise P4-F01–P4-F04.
- Builder-attributed only: `assembleDebug` passed, `assembleDebugAndroidTest` passed compile-only, and the same 101 JVM tests passed in the implementation handback.
- Still pending after repair review: execution of `AssetDetectionConfigLoaderTest`, service binding and browser behavior, app/site timing, focus/search exclusions on-device, unsupported-browser silence, battery-screen rendering, and overnight survival. Compilation is not instrumented or owner acceptance evidence.

## Repair ownership and stop condition

The remaining work is cancellation/state-machine and event-outcome logic, so a senior Codex implementation session is the recommended owner under `WORKFLOW.md`. Repair only P4-F01–P4-F04 on base `0ca1424`; preserve the accepted browser map, matcher, config seam, placeholder scope, Phase 3 behavior and all exclusions. Run the three authorized non-device checks, update HANDBACK/CURRENT, and stop at `ready_for_review`. A fresh independent review PASS is required before owner/device acceptance.

## Paste-ready repair prompt

> Read AGENTS.md and resume `TS-P4-detection-engine` as the senior Codex repair implementer. Base is `0ca1424`; review findings P4-F01–P4-F04 in `REVIEW.md`. Repair only foreground settle revalidation, block-map invalidation, `(windowId, package)` cache identity, and truthful `block_screen_shown` attribution. Add focused regression coverage, run the authorized non-device checks, update HANDBACK/CURRENT, and stop at `ready_for_review`; do not run device or emulator operations.

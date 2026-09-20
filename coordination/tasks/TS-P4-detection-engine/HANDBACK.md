# Implementation handback — TS-P4-detection-engine

## Senior Codex repair — current submission

- **Submission:** `9b3da43` repairing findings P4-F01–P4-F04 on code base `0ca1424`. Documentation-only commits `d63a65c` and `7467965` sit between the implementation commits and do not alter the reviewed code base.
- **Implementer:** senior Codex, 20 September 2026. Self-verification only; a fresh independent review of `0ca1424..9b3da43` is required before owner/device acceptance.

### Finding resolution

- **P4-F01 — foreground settle truth:** the delayed callback now snapshots `rootInActiveWindow`, refreshes focus/text from that actual root when it is a supported browser, and supplies the live window/package identity to `DetectionEngine`. A missing root or foreground identity mismatch consumes the stale settle without triggering; a hidden address-bar node still preserves the cached URL.
- **P4-F02 — live block ownership:** `onBlocksChanged` now cancels the pending settle when its domain no longer maps to the same active `BlockRef`; `onSettleElapsed` also revalidates the current map before emitting.
- **P4-F03 — cache identity:** cached readings are keyed semantically by `(windowId, packageName)`. Reusing a numeric window ID for another package resets its URL/focus state and cancels pending work for the old identity.
- **P4-F04 — truthful shown event:** the service carries block identity and the original monotonic detection timestamp into `BlockActivity` but no longer logs `block_screen_shown`. The activity logs only from `onPostResume`, computes latency through that presentation point, and saves a synchronous one-shot guard so resume/recreation cannot schedule duplicates. Invalid launch data produces neither UI nor telemetry.

### Repair files

- Modified: `TokiAccessibilityService.kt`, `BlockActivity.kt`, `detection/DetectionEngine.kt`, `detection/DetectionEngineTest.kt`.
- Added: `detection/BlockShownEvent.kt`, `detection/BlockShownEventTest.kt`.

### Checks (senior Codex, non-device, this machine, 20 September 2026)

- `./build.sh testDebugUnitTest` — BUILD SUCCESSFUL; **109 tests, 0 failures, 0 errors**. `DetectionEngineTest` is 25/25 and `BlockShownEventTest` is 3/3. New cases cover unfiltered foreground departure, unavailable active roots, block removal/reassignment, same-ID package replacement, original detection timestamps, invalid event inputs and non-negative latency.
- `./build.sh assembleDebug` — BUILD SUCCESSFUL.
- `./build.sh assembleDebugAndroidTest` — BUILD SUCCESSFUL, compile-only.
- `git diff --check` — passed before submission.

### Remaining evidence and stop

- No adb, device/emulator, installation, screenshot, database extraction or instrumented execution was performed. The previously listed Phase 4 device/browser checks and execution of `AssetDetectionConfigLoaderTest` remain owner-authorized only after independent code PASS.
- In particular, the new live-root/window identity behavior and resumed `block_screen_shown` latency still require actual service/browser/device evidence; the compile and JVM results do not claim that acceptance.
- Stop at `ready_for_review`. Do not begin owner/device acceptance or Phase 5.

### Current next-role prompt

> Read AGENTS.md and resume `TS-P4-detection-engine` as an independent Codex reviewer. Review only repair range `0ca1424..9b3da43` and affected dependencies against P4-F01–P4-F04 in `REVIEW.md`. The three authorized non-device checks pass; no device execution occurred. Record PASS/FAIL for `9b3da43`, keep owner/device acceptance separate, and stop after updating REVIEW/CURRENT.

## Initial GLM submission — preserved evidence

- **Submission:** `0ca1424` on base `0ddd569` (repository HEAD when the task was handed over; worktree was clean before editing). First Phase 4 submission; no prior Phase 4 handback to preserve.
- **Implementer:** GLM 5.3 Pro, as assigned in TASK/CURRENT, 20 September 2026. Self-verification only; independent Codex review precedes owner/device acceptance.

## What was implemented

- **Detection engine (pure Kotlin, `detection/DetectionEngine.kt`):** all PRD §13 timing/state rules with an injectable clock, no Android types, `@Synchronized` inputs, declarative `DetectionAction` outputs. Immediate app triggers on a blocked app's window-state change; site triggers only after a 2000 ms settle (`SETTLE_DELAY_MS`, prototype reference, tunable) elapses with the reading unchanged — same window, same URL, address bar unfocused. A focused address bar or a spaced/blank value cancels any pending settle and never schedules; a missing address-bar node (scroll-hidden bar) changes nothing, so the per-window cached URL stands. Window/package/address changes cancel stale delayed work via a generation counter that also invalidates late handler callbacks. Repeat triggers are debounced per (type, target, block) for `REPEAT_TRIGGER_DEBOUNCE_MS` (10 s tunable placeholder; Phase 5's pause/re-arm replaces the loop-prevention role). A recreated engine starts clean — debounce memory and caches do not survive a service restart, so a kill/rebind can re-trigger (documented, tested).
- **Domain matching (`detection/DomainMatcher.kt`):** address-bar text → host (scheme/userinfo/path/query/fragment/port/trailing-dot/case handling) → whole-domain match with the dot-boundary suffix rule: `old.reddit.com` matches `reddit.com`, `notreddit.com` and `reddit.com.evil.com` do not. No path-level matching.
- **Service adapter (`TokiAccessibilityService.kt` rewritten):** subscribes `typeWindowStateChanged|typeWindowContentChanged` (XML + runtime), applies the runtime package filter via `setServiceInfo` = enabled blocked apps ∪ supported browsers (refreshed whenever the blocks flow or config emits); own-package events are dropped and own package is excluded from matchable apps. Address-bar reading uses `rootInActiveWindow` (package-checked) + `findAccessibilityNodeInfosByViewId`; `flagReportViewIds` added. Node text (`contentDescription` fallback) with `isFocused || isAccessibilityFocused`. §10 events: `accessibility_connected` on connect, best-effort `accessibility_disconnected` from `onUnbind` (detached write so the following `onDestroy` cannot cancel it), `url_read_failed(browser_package)` emitted only when a **window-state-changed** read from a supported browser finds no address-bar node — mid-scroll content-change absences stay quiet so the canary doesn't flood; `block_screen_shown(block_id, trigger_type, target, latency_ms)` logged only when `startActivity` was accepted. Latency definition: app ≈ 0 (decision is synchronous in the event callback), site ≈ settle delay (from the scheduling reading), matching PRD §14's attribution.
- **Bundled JSON loader:** `assets/detection_config.json` (nine PRD §13 browsers verbatim + the two authored OEM battery texts moved out of `OemBattery.kt` unchanged) behind `DetectionConfigLoader` (one interface, one implementation `AssetDetectionConfigLoader`, cached after first load). Parsing (org.json, Android platform) is separated from pure validation (`DetectionConfigFactory`), which fails loudly as `DetectionConfigException` on empty/blank/duplicate/mis-shaped entries. A failed load degrades the service to app-only detection (no crash); the battery screen degrades to its frame with the settings button functional — no Kotlin fallback copy, so a missing asset can't silently pass.
- **Placeholder (`BlockActivity.kt`):** unexported, `excludeFromRecents`, `noHistory`; plain Nocturne-dark text naming block name, target and app/site trigger; finishes on back. Launched with `NEW_TASK|EXCLUDE_FROM_RECENTS|CLEAR_TOP`; no overlay involvement (§13/§14 exemption).
- **OEM text migration:** `OemBattery` keeps only `detect()` + data classes; `MainActivity` loads config once for the battery route and passes the map into `BatteryInstructionsScreen`.
- Phase 3 permission/onboarding/activation behavior, Room ownership and event contracts untouched except the additions listed above.

## Files

New: `app/src/main/assets/detection_config.json`; `detection/{DetectionConfig,DetectionConfigLoader,DomainMatcher,ActiveBlocks,DetectionEngine}.kt`; `BlockActivity.kt`; JVM tests `detection/{DetectionEngineTest,DomainMatcherTest,ActiveBlockIndexTest,DetectionConfigFactoryTest,DetectionAssetsTest}.kt`; androidTest `detection/AssetDetectionConfigLoaderTest.kt`.
Modified: `TokiAccessibilityService.kt`, `MainActivity.kt`, `TokiApplication.kt`, `OemBattery.kt` (+`OemBatteryTest.kt` slimmed to detection-only), `EventRepository.kt` (three §10 constants), `BatteryInstructionsScreen.kt`, `AndroidManifest.xml`, `accessibility_service_config.xml`, `strings.xml`.

## Checks (GLM, non-device, this machine, 20 September 2026)

- `./build.sh assembleDebug` — BUILD SUCCESSFUL.
- `./build.sh testDebugUnitTest` — BUILD SUCCESSFUL; **101 tests, 0 failures, 0 errors** from `app/build/test-results/testDebugUnitTest/` (was 60; +44 detection tests, OemBattery 5→2). DetectionEngineTest 20, DomainMatcherTest 10, ActiveBlockIndexTest 4, DetectionConfigFactoryTest 6, DetectionAssetsTest 4.
- `./build.sh assembleDebugAndroidTest` — BUILD SUCCESSFUL (compile only; includes the new `AssetDetectionConfigLoaderTest`, which needs org.json on-device).
- Legacy hex-color grep outside `ui/theme/` — no matches.

## Test-evidence split (deliberate)

org.json is not on the JVM classpath and no test dependency may be added, so: the **asset contents** (nine exact browsers, OEM text, bold steps) and the **declarations** (event types, `flagReportViewIds`, unexported placeholder) are pinned by the file-reading `DetectionAssetsTest` (JVM, executed); full JSON parsing of the real asset plus parser failure paths run in `AssetDetectionConfigLoaderTest` (androidTest, compiled but **not executed** — awaits the owner-authorized instrumented run). Engine rules are fully covered in JVM.

## Limits and remaining device evidence

- No device execution of any kind was performed or claimed: service binding, `dumpsys accessibility` listing, sub-1-second app placeholder, ≤3 s site placeholder, Chrome typing/search non-triggers, subdomain vs lookalike behavior on-device, unsupported-browser silence, battery-screen rendering with JSON text, and the new androidTest suite all remain owner-authorized evidence.
- Adapter simplifications for review attention: URL reads use `rootInActiveWindow` (not per-window `getWindows`, which needs another flag), so an event from a non-active window isn't read — the package check drops it; `url_read_failed` only counts window-state-changed absences; `accessibility_disconnected` is best-effort (lost if the process dies between unbind and write).
- The 10 s repeat debounce is a Phase 4 stand-in for Phase 5 pause semantics; after it lapses, a still-foreground blocked target re-shows the placeholder. Settle (2000 ms) and debounce are single tunable constants.
- `block_screen_shown` is written when the launch call succeeds; actual on-screen timing is device evidence.
- Phase 3 owner checklist retests are unaffected by this change set except the battery screen (text now JSON-loaded).

## Initial next-role prompt (superseded by the repair prompt above)

> Read AGENTS.md and resume [TS-P4-detection-engine](TASK.md) as reviewer (Codex). Submission `0ca1424` on base `0ddd569` is ready for review; see HANDBACK.md for the state-machine invariants, the JVM/androidTest evidence split and the flagged adapter simplifications.

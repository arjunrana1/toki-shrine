# Handback — TS-P6-pause-lifecycle owner corrections P6C-O1–O3

- **Submission:** `ac78985` (`Apply Phase 6 owner corrections P6C-O1 to O3`), 23 September 2026.
- **Base:** `da7f8d5` (records-only; app source identical to reviewed `062c71e`). Direct owner → GLM correction path (WORKFLOW). The re-review PASS WITH NOTES applies to `d93162d..062c71e` only; this new diff is pending review, and Codex reviews the accumulated corrections at the next checkpoint.
- **Implementer:** GLM. **State:** `awaiting_owner` — owner retest of the corrections pending; the corrected APK is staged but not yet installed (phone absent at handoff).
- Prior repair handback preserved at [submissions/062c71e/HANDBACK.md](submissions/062c71e/HANDBACK.md). Owner results and finding rationale: [OWNER-CHECKS](OWNER-CHECKS.md).

## Changes

- **P6C-O1 — notification progress cadence.** `PauseService`: the 60 s notification re-post tick and the 1 s bubble tick merge into one per-second `secondTick` that evaluates, re-renders notifications and refreshes bubble text. Within a render the soonest pause is promoted via `startForeground` and the remaining pauses are `notify()`-ed — the soonest no longer gets a duplicate immediate post. Chronometer anchoring, IDs, channel, ongoing semantics and all pause/enforcement logic are unchanged.
- **P6C-O2 — gate photo dim.** `BlockGateScreen`: `GATE_SCRIM_ALPHA` 0.78 → 0.65, comment updated with the owner-correction date. Only the gate scrim; assets, layout and type untouched.
- **P6C-O3 — app bar trailing gap.** `NocturneAppbar`: title gains `padding(end = 12.dp)` so long titles wrap before the trailing action (block detail's delete icon). Shared component; screens without trailing content are visually unchanged for short titles.

## Deliberately not changed

- **P6C-O4 / P6-O13** — explicit owner do-not-fix instructions, recorded in OWNER-CHECKS with root-cause explanations (unfinished-challenge task ownership in `BlockActivity.onNewIntent`; per-tick notification re-post).
- **P6-O9 bubble-dismissal improvement** — changes PRD §6 screen 20 behavior; awaiting an owner product decision before any implementation.

## Checks run on `ac78985`

- `./build.sh assembleDebug` — **PASS**.
- `./build.sh testDebugUnitTest` — **PASS, 170/170**; fresh XML count: 170 tests, 0 failures, 0 errors, 0 skipped.
- `git diff --check` — clean.
- `assembleDebugAndroidTest` was not rerun: no test infrastructure or instrumented sources were touched (proportionate direct-correction checks per WORKFLOW; same precedent as the Phase 5 owner corrections).

## Install status

Corrected APK staged: `app/build/outputs/apk/debug/app-debug.apk`, SHA-256 `9083e321e63399140fb7c7e5c8a8de4decb82b410ba55c5be91b72e9b9e5f29d`, built from `ac78985`. `adb devices` shows no device at handoff (reported once, no polling per build/validation). Install remains the agreed setup only: `adb -s R5CW30ZBM2R install -r`, no launch, no uninstall/clear, accessibility re-binding and seeding stay owner setup.

## Stop

Owner retests P6C-O1–O3 on the corrected build (progress cadence, photo brightness, title/delete spacing) and continues any outstanding acceptance observation; unaffected checklist results stand with their original build attribution. No Phase 7 work. Bubble-dismissal decision requested from the owner.

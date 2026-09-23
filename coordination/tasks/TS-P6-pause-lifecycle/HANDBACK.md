# Handback — TS-P6-pause-lifecycle owner corrections P6C-O1–O5

- **Submission:** `7abb516` (bubble drag-to-dismiss, P6C-O5) accumulating `ac78985` (`Apply Phase 6 owner corrections P6C-O1 to O3`), both 23 September 2026.
- **Base:** `da7f8d5` (records-only; app source identical to reviewed `062c71e`). Direct owner → GLM correction path (WORKFLOW). The re-review PASS WITH NOTES applies to `d93162d..062c71e` only; these corrections are pending review, and Codex reviews the accumulated diff at the next checkpoint.
- **Implementer:** GLM. **State:** `awaiting_owner` — owner retest of the corrections pending; the corrected APK is staged but not yet installed (phone absent at handoff).
- Prior repair handback preserved at [submissions/062c71e/HANDBACK.md](submissions/062c71e/HANDBACK.md). Owner results and finding rationale: [OWNER-CHECKS](OWNER-CHECKS.md).

## Changes

- **P6C-O1 — notification progress cadence.** `PauseService`: the 60 s notification re-post tick and the 1 s bubble tick merge into one per-second `secondTick` that evaluates, re-renders notifications and refreshes bubble text. Within a render the soonest pause is promoted via `startForeground` and the remaining pauses are `notify()`-ed — the soonest no longer gets a duplicate immediate post. Chronometer anchoring, IDs, channel, ongoing semantics and all pause/enforcement logic are unchanged.
- **P6C-O2 — gate photo dim.** `BlockGateScreen`: `GATE_SCRIM_ALPHA` 0.78 → 0.65, comment updated with the owner-correction date. Only the gate scrim; assets, layout and type untouched.
- **P6C-O3 — app bar trailing gap.** `NocturneAppbar`: title gains `padding(end = 12.dp)` so long titles wrap before the trailing action (block detail's delete icon). Shared component; screens without trailing content are visually unchanged for short titles.
- **P6C-O5 — bubble drag-to-dismiss (owner-approved 23 September, PRD §17 Phase 6 addendum).** `PauseBubbleView`: releasing a drag over the bottom 72 dp strip discards the pill — dim/scale affordance while over the zone, 140 ms fade-out, then the host callback; releasing elsewhere keeps the old settle + `bubble_dragged`. `PauseService`: dismissal records the live pause instances (block id + monotonic deadline) in the new pure `BubbleDismissalPolicy`, detaches the bubble and logs the new §10 `bubble_dismissed` event; renders keep the bubble hidden while only dismissed instances remain live, and any new/restarted pause instance (new deadline) shows it again with a fresh `bubble_shown`. Timing, enforcement, notification behavior and `PauseRegistry`/`PauseCoordinator` are untouched. PRD §6 screen 20, §7 and §10 amended; `EventRepository` gains `EVENT_BUBBLE_DISMISSED`.

## Deliberately not changed

- **P6C-O4 / P6-O13** — explicit owner do-not-fix instructions, recorded in OWNER-CHECKS with root-cause explanations (unfinished-challenge task ownership in `BlockActivity.onNewIntent`; per-tick notification re-post) and in the PRD §17 Phase 6 addendum.

## Focused coverage

`BubbleDismissalPolicyTest` (5 JVM cases): dismissal hides while only dismissed instances stay live; a pause absent at dismissal shows the bubble; a restarted/fresh same-block pause (new monotonic deadline) shows it; an empty live set resets the memory; a non-empty set keeps it. Total JVM suite is now 175 tests.

## Checks run on `7abb516`

- `./build.sh assembleDebug` — **PASS**.
- `./build.sh testDebugUnitTest` — **PASS, 175/175**; fresh XML count: 175 tests, 0 failures, 0 errors, 0 skipped (170 prior + 5 new).
- `git diff --check` — clean.
- `assembleDebugAndroidTest` was not rerun: no test infrastructure or instrumented sources were touched (proportionate direct-correction checks per WORKFLOW; same precedent as the Phase 5 owner corrections).

## Install status

Corrected APK staged: `app/build/outputs/apk/debug/app-debug.apk`, SHA-256 `df50470ebc4432458b9ce8c9eaed64978de82cc75c6ed31a74579f24ee8af590`, built from `7abb516` (supersedes the earlier staged `9083e321…` APK from `ac78985`, which was never installed). `adb devices` shows no device at handoff (reported once, no polling per build/validation). Install remains the agreed setup only: `adb -s R5CW30ZBM2R install -r`, no launch, no uninstall/clear, accessibility re-binding and seeding stay owner setup.

## Stop

Owner retests P6C-O1–O3 and O5 on the corrected build (progress cadence, photo brightness, title/delete spacing; bubble drag-to-bottom-edge dismissal, hidden-until-new-pause behavior, `bubble_dismissed` event visibility is DB-level and not directly owner-observable). Unaffected checklist results stand with their original build attribution. No Phase 7 work.

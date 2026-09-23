# Handback — TS-P6-pause-lifecycle owner corrections P6C-O1–O6

- **Submission:** `7fb322e` (turn-off challenge visual corrections, P6C-O6) accumulating `b1d9ec8` (dismissal hint refinement), `7abb516` (bubble drag-to-dismiss, P6C-O5) and `ac78985` (P6C-O1–O3), all 23 September 2026.
- **Base:** `da7f8d5` (records-only; app source identical to reviewed `062c71e`). Direct owner → GLM correction path (WORKFLOW). The re-review PASS WITH NOTES applies to `d93162d..062c71e` only; the accumulated corrections diff `da7f8d5..7fb322e` is routed by Arjun to Codex for final verification.
- **Implementer:** GLM. **State:** `ready_for_review` — Codex verification of the accumulated corrections requested by the owner. The installed device build is `7fb322e` (see Install status).
- Prior repair handback preserved at [submissions/062c71e/HANDBACK.md](submissions/062c71e/HANDBACK.md). Owner results and finding rationale: [OWNER-CHECKS](OWNER-CHECKS.md).

## Changes

- **P6C-O1 — notification progress cadence.** `PauseService`: the 60 s notification re-post tick and the 1 s bubble tick merge into one per-second `secondTick` that evaluates, re-renders notifications and refreshes bubble text. Within a render the soonest pause is promoted via `startForeground` and the remaining pauses are `notify()`-ed — the soonest no longer gets a duplicate immediate post. Chronometer anchoring, IDs, channel, ongoing semantics and all pause/enforcement logic are unchanged.
- **P6C-O2 — gate photo dim.** `BlockGateScreen`: `GATE_SCRIM_ALPHA` 0.78 → 0.65, comment updated with the owner-correction date. Only the gate scrim; assets, layout and type untouched.
- **P6C-O3 — app bar trailing gap.** `NocturneAppbar`: title gains `padding(end = 12.dp)` so long titles wrap before the trailing action (block detail's delete icon). Shared component; screens without trailing content are visually unchanged for short titles.
- **P6C-O5 — bubble drag-to-dismiss (owner-approved 23 September, PRD §17 Phase 6 addendum).** `PauseBubbleView`: releasing a drag over the bottom 72 dp strip discards the pill, then the host callback; releasing elsewhere keeps the old settle + `bubble_dragged`. `PauseService`: dismissal records the live pause instances (block id + monotonic deadline) in the new pure `BubbleDismissalPolicy`, detaches the bubble and logs the new §10 `bubble_dismissed` event; renders keep the bubble hidden while only dismissed instances remain live, and any new/restarted pause instance (new deadline) shows it again with a fresh `bubble_shown`. Timing, enforcement, notification behavior and `PauseRegistry`/`PauseCoordinator` are untouched. PRD §6 screen 20, §7 and §10 amended; `EventRepository` gains `EVENT_BUBBLE_DISMISSED`.
- **P6C-O5 refinement (`b1d9ec8`, owner refinement same day).** While the pill is dragged, a centred **Dismiss** label appears at the bottom edge: a separate non-touchable overlay window (`R.string.pause_bubble_dismiss`), fading in on drag start and out on settle/cancel, tinting Nocturne accent when the pill is over the zone. Over the zone the pill now goes very transparent (alpha 0.2, scale 0.9) instead of the earlier 0.55 dim. Discard semantics, hidden-until-new-pause memory, events and all other behavior are unchanged.
- **P6C-O6 — turn-off challenge visual corrections (`7fb322e`, owner direction same day).** Typing turn-off screen: the header "Leave it on" ghost escape is removed and replaced by a full-width outlined **Never Mind** button under Submit (same `onWalkAway` escape semantics; delay screen's outlined-button style); the header eyebrow becomes the block name only — `challengeTitle` drops the "Turning off ·" prefix for both challenge screens; the disable info card reads "Type in X characters to disable the block" (was "…to turn this block off.") and its lock icon is now vertically centred (`verticalAlignment` on the card row, icon top-padding removed). `delayEscapeLabel` TURN_OFF becomes "Never Mind" so the waiting variant matches; PAUSE challenge headers and every other surface are unchanged. `typingEscapeLabel` was removed with its dead branch; `InterruptionModelsTest` copy assertions updated to the owner-approved copy (not weakened — the new copy is asserted). PRD §6 screen 22 and the §17 Phase 6 addendum amended.

## Deliberately not changed

- **P6C-O4 / P6-O13** — explicit owner do-not-fix instructions, recorded in OWNER-CHECKS with root-cause explanations (unfinished-challenge task ownership in `BlockActivity.onNewIntent`; per-tick notification re-post) and in the PRD §17 Phase 6 addendum.

## Focused coverage

`BubbleDismissalPolicyTest` (5 JVM cases): dismissal hides while only dismissed instances stay live; a pause absent at dismissal shows the bubble; a restarted/fresh same-block pause (new monotonic deadline) shows it; an empty live set resets the memory; a non-empty set keeps it. Total JVM suite is now 175 tests.

## Checks run on `7fb322e`

- `./build.sh assembleDebug` — **PASS**.
- `./build.sh testDebugUnitTest` — **PASS, 175/175**; fresh XML count: 175 tests, 0 failures, 0 errors, 0 skipped (copy-selector assertions updated to the owner-approved copy in place; suite count unchanged).
- `git diff --check` — clean.
- `assembleDebugAndroidTest` was not rerun: no test infrastructure or instrumented sources were touched (proportionate direct-correction checks per WORKFLOW; same precedent as the Phase 5 owner corrections).

## Install status

The device runs `7fb322e` (installed 23 September on the owner's request): staged APK hash re-verified (`67d09fa6e7d611d6409a028c224eb323fd84a329fad1be4ba7298b97b17448cf`), `adb -s R5CW30ZBM2R install -r` → **Success** (streamed install, data preserved, no uninstall/clear); `pm path` confirms the package; the accessibility grant is retained (re-binding is owner observation). Nothing was launched and no device testing was performed. Installation history this day: `062c71e` build (full checklist) → `7abb516` → `b1d9ec8` → `7fb322e` (current). Install remains agreed setup only: `install -r`, no launch, no uninstall/clear.

## Stop

Codex verifies the accumulated corrections `da7f8d5..7fb322e` (owner-routed final verification). The verified build is installed, so the owner correction retest (P6C-O1–O6: progress cadence, photo brightness, title/delete spacing, drag-to-dismiss with the Dismiss label, turn-off header/escape/copy changes and lock centring) can proceed directly. Unaffected checklist results stand with their original build attribution. No Phase 7 work.

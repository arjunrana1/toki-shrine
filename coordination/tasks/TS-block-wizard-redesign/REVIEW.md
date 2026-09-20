# TS-block-wizard-redesign — REVIEW

- **Latest verdict:** `PASS` — WZ-F01/WZ-F02 are resolved in the exact repair delta recorded below. The initial `FAIL` and finding text remain preserved as the diagnosis for that repair.
- **Reviewed submission:** staged working-tree snapshot on base `f92661d5ff0303fbdcba17a9dc2fa6af3790bb3a`; pre-review staged-diff SHA-256 `1ed13764df0cb125877d52b9f3103235b2f4475eba8a8eb760459f198e5e2e2b` (12 staged files recorded in [HANDBACK](HANDBACK.md)). No implementation commit exists.
- **Reviewer:** Codex, 19 September 2026.
- **Scope:** PRD §7 and the latest §17 wizard amendment; [blocks/targets](../../../docs/components/blocks-targets.md) and [persistence/events](../../../docs/components/persistence-events.md). Highest-risk inspection covered `CreateFlowStateSaver`, edit prefill, both per-method column families, five-step telemetry, terminal guards and their tests.
- **Checks:** source/diff/test inspection and `git diff --cached --check` (clean). Builder evidence was inspected, not rerun: assembleDebug PASS; 58 JVM tests PASS; assembleDebugAndroidTest PASS **compile-only**; hex check clean. No device, emulator or instrumented execution.

## Repair-delta review — PASS

- **Reviewed repair:** the four HANDBACK blob pairs, excluding the already-reviewed wizard delta: `EventRepository.kt` `465b9de` → `81a5b55f`; `CreateFlowScreen.kt` `bd06367` → `4f39e214`; `CreateFlowStateTest.kt` `fd45cc9` → `c7ec186b`; `CreateFlowScreenTest.kt` `71193e7` → `13d5337c`. Base remains `f92661d5ff0303fbdcba17a9dc2fa6af3790bb3a`; no implementation commit exists.
- **Reviewer:** independent Codex repair-delta review, 19 September 2026.
- **Verdict:** `PASS` for WZ-F01/WZ-F02. No blocking finding remains in the repair boundary.
- **Why:** the saved `flowId` reconnects the recreated screen to an Activity-retained `CreateFlowSession`; its single channel orders create-start, every traversed step and the terminal operation FIFO. The session-owned atomic terminal guard excludes duplicate Save/Back/abandon input until completion and is released only for the supported stale-ownership rejection. Outcome replay invokes the current composition's close callback once, then removes the completed session.
- **Transaction boundary:** create now nests block creation, step 5 and `block_created` in one Room transaction; edit nests the pre-update comparison, update and `block_edited` in one transaction. Because production supplies both repositories from the same `TokiDatabase`, the existing nested `withTransaction` calls share the outer transaction, preventing a committed block mutation without its terminal event.
- **Focused checks:** all blob anchors resolve; all four anchor diffs and `git diff --cached --check` are clean. Reviewer ran `./build.sh testDebugUnitTest --tests com.arjunrana.tokishrine.ui.screens.CreateFlowStateTest`: BUILD SUCCESSFUL, **15 tests, 0 failures, 0 errors, 0 skipped**, including the new suspended FIFO and retained terminal-exclusion tests. Builder evidence remains: assembleDebug PASS; full 60-test JVM suite PASS; assembleDebugAndroidTest PASS **compile-only**. No device, emulator or instrumented test ran.
- **Evidence limit:** the three new recreation/Room UI tests remain compile-only, so activity recreation, SQL rollback/commit behavior and one-close delivery are not independently executed runtime evidence. Owner BW checks remain pending; this code PASS does not clear Phase 3 or authorize Phase 4/5.

## Direct owner corrections — source verification

- **DC-01–DC-04:** source inspection matches the recorded owner requirements: welcome copy/app icon, the corrected sliders glyph, both method-card indicators, and disable-step explainer. The owner now reports their affected wizard checks pass.
- **DC-05:** source inspection confirms it only removed tonal elevation while retaining the lighter `surface` container and the neutral-900 scrim. The supplied post-fix screenshot establishes a remaining visual failure; this is not a passing correction.
- **DC-06:** Codex changed the sheet container/scrim token roles in response. That new three-file visual delta is pending an independent narrow review and owner visual retest; no self-review verdict is claimed here.

### DC-06 narrow review — PASS

- **Scope:** only the sheet container, tonal elevation and shared scrim-role delta plus the theme/UI contract dependency; no wizard behavior was reopened.
- **Verdict:** `PASS`. `DetailsSheet` uses the Nocturne `bg` token with `tonalElevation = 0.dp`, preventing Material tonal tint from lifting the sheet. `MaterialTheme.colorScheme.scrim` resolves to `NocturneScrim` (`0x80000000`), a 50% black overlay that darkens the underlying wizard instead of blending toward a lighter neutral.
- **Checks/evidence:** source and staged-diff inspection; `./build.sh testDebugUnitTest` passed **60 tests, 0 failures, 0 errors, 0 skipped**. Arjun separately reports the DC-06 manual visual retest passes. No device or instrumented execution was performed by the reviewer.
- **Disposition:** no DC-06 finding remains. Wizard code review and owner acceptance are complete; its still-unexecuted instrumented cases remain part of the explicit nonvisual test gap rather than a visual blocker.

### Instrumented closure — PASS

- The repaired recreation/FIFO/terminal-transaction tests now executed on SM-S918B / Android 16 as part of the final 51-test Android suite: **0 failures, 0 errors, 0 skipped**.
- The test harness was corrected without changing production wizard behavior: recreation uses a debug-only non-exported Activity whose content is installed from `onCreate`; rapid double Save invokes both semantics callbacks without waiting through the intentional suspension; stale display/event expectations were aligned with the existing formatter and traversed-step contract.
- WZ-F01/WZ-F02, wizard owner acceptance and the previously compile-only runtime evidence are closed. No wizard blocker remains.

## Initial review — FAIL (preserved finding record)

## Findings

### WZ-F01 — Blocker — recreation can strand a committed create or suppress its start event

**Location:** `CreateFlowScreen.kt:138–150, 222–270, 314–318, 348–398`.

`CreateFlowStateSaver` persists `createStartedLogged` but not the terminal in-flight state. The start flag is set before the suspending event write. Save commits the Room block before the step-5 and `block_created` writes, while its duplicate/abandon guard lives only in the composition-owned `terminalTaken` field and the coroutine uses a composition-owned scope.

Concrete failing windows:

1. Recreation after `createStartedLogged = true` but before `block_create_started` commits cancels the effect; the restored flag suppresses retry, so this logical flow has no start event.
2. Recreation after `createBlock` commits but before step 5 / `block_created` / `onClose` completes restores the review screen with a fresh terminal guard. The block exists, but telemetry and navigation may be missing. Retrying Save then collides with the target now owned by the already-created block and reports `Already added to a block`, leaving the user in an editor for a block that was actually saved.

The recreation tests change an idle draft only; they do not suspend a start or terminal write across recreation. The repeated-tap tests do not recreate the activity. This violates draft/recreation correctness, the exactly-once terminal guard, and the recorded creation-event contract.

**Required correction:** give start and terminal operations a recreation-stable owner and define retry/idempotency around durable commits. Add focused tests that gate each relevant suspension boundary, recreate, then prove exactly one block, one ordered event sequence and one close (or a safe retry before any commit). Do not fix this by saving a bare `terminalTaken = true`, which can permanently strand a canceled operation.

### WZ-F02 — Blocker — five-step event ordering is not enforced under suspension

**Location:** `CreateFlowScreen.kt:321–327, 348–366, 446–469`.

Steps 1–4 call `stepCompleted`, which launches an independent coroutine and immediately advances the UI. A user can advance again, go Back/Next, or Save while an earlier event write is suspended. Later step coroutines—and the save coroutine that writes the block, step 5 and `block_created`—can therefore overtake an earlier step event. The current tests assert the happy-path Room order only after ungated writes; none suspends `block_create_step_completed` while navigation continues.

This does not guarantee the required ordered history (`started`, traversal events in action order, step 5, `block_created`) and can persist the block before an earlier completion event. The new fifth step extends the same pre-existing launch pattern without resolving the task's explicit event-ordering invariant.

**Required correction:** serialize creation telemetry and terminal creation through one recreation-stable flow owner or equivalent ordered queue, with an explicit terminal boundary. Add a suspension test that advances/backtracks/saves while an earlier step write is gated and asserts the exact final sequence after release and recreation/cancellation cases.

## Verified areas and limits

- Repository validation is correct by source inspection: the shared `requireValidConfiguration` runs before mutation on both create and update and validates pause duration, typing passage, waiting duration and both disable ladders even when a method is inactive. Existing tests cover all create-side families plus selected update rollback paths; the HANDBACK overstates the update test matrix because invalid update coverage is explicit only for pause wait and disable wait, though the shared production path covers the remaining fields.
- The schema v3 reset, separate `turnoff_seconds`, unconditional typo behavior, method-specific summaries, five visible screens, per-method draft values, details-sheet scope and fixed ladders align with the written specification by source inspection.
- Instrumented sources compiling does not prove Room round trips, UI restoration, modal Back behavior, layout, event order or transaction/cancellation behavior. All BW owner checks remain pending.
- Phase 3 remains open with its existing build attribution. Phase 4 and Phase 5 runtime work remain unauthorized.

## Repair ownership and review boundary

This is the first unsuccessful GLM submission for WZ-F01/WZ-F02. Both findings involve cancellation, recreation and durable ordering, so the workflow assigns the repair to **Codex/senior ownership**, not another GLM repair cycle. Re-review only the repair delta and affected lifecycle/event dependencies; retain the validated UI and persistence work unless the repair requires a narrow interface change.

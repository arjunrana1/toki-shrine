# GLM builder handoff

Updated 9 September 2026 after Phase 2 repairs landed as `a0df32c` (builder session).

## Current responsibilities and boundary — latest owner update, 10 September 2026

Read AGENTS.md §§3a–3b: GLM implements, compiles and runs targeted non-device checks; Codex reviews code only; Arjun leads all device and experience checks after code clearance. Neither agent runs adb, installations, device/emulator operations or connected/instrumented tests without explicit owner authorization. These rules supersede historical hardware instructions and results below.

## Phase 2 repair finalized — 12 September 2026 (builder update)

Both 10 September blockers are repaired and committed on top of a0df32c (see DECISIONS.md "Phase 2 repair finalized"): synchronous terminal guard for Back/Save with captured exit step, ownership-load gating for app and site selection, and rejected-save draft retention with the inline "Already added to a block" message. Codex re-reviewed the diff: PASS WITH NOTES. Suite at this state, from the completed 10 September builder run (attributed, not rerun): 7 JVM + 25 instrumented (12 repository, 6 event, 7 UI incl. the new CreateFlowScreenTest) green on the USB S23 Ultra. Owner dependency approval recorded for ui-test-junit4/ui-test-manifest; the three repositories expose `open` seams for instrumented gating only. Remaining: Arjun's owner device validation — Phase 3 is not cleared. The 10 September harness run wiped app data; the pre-run backup at /tmp/toki-fixture-backup/toki-shrine.db was purged by macOS /tmp cleanup before restoration, so the fixtures (The scroll pit, News spiral) are unrecoverable. The approved build was installed fresh on 12 September and launches empty; the owner recreates checklist fixtures.

Follow §3b for narrow scope, progress updates, prompt explanations of delays and a final report of changed files/reasons, commit/base, actual checks/results and unverified items. Nonvisual device-only assertions remain explicitly pending; do not claim compilation proves they pass or introduce a large replacement harness to avoid device tests.

## Repairs in a0df32c (historical builder summary, not sign-off)

- Owner ownership decision implemented for apps and websites: other-block targets are visible, disabled, labelled exactly "Already added to a block" (no owner name), ON or OFF; the edited block's own targets stay editable while OFF. Conflict dialog/move flow removed for both types; conflict shown/resolved telemetry retired (historical rows kept).
- Repository boundary: `ConflictingOwnershipException` from `createBlock`/`updateBlock` — atomic abort with parent rollback; no write path reassigns a target. Move/transfer APIs removed; bulk `appOwnerships`/`siteOwnerships` feed the picker. In-draft duplicates collapse; only cross-block ownership rejects.
- Android Back inside the flow: search exits first, steps unwind, step-1 Back exits via the abandonment logger exactly once (create mode; edits silent). System Back dismisses the keyboard before the handler runs.
- DECISIONS.md corrected: 8f1ad1e shipped an in-place Box, not the Dialog the entry claimed; the One UI freeze finding is retained as guidance (clear focus before any modal; device-test keyboard up and down).

## Read in order

1. AGENTS.md in full: phase scope, build/device rules and acceptance gates.
2. PRD.md: behavioral authority, especially §4, Phase 2 screen descriptions and §10 events.
3. DECISIONS.md in full: prior decisions, owner decisions and known caveats.
4. Verification Feedback/phase-2-verification/REPORT.md: current actionable findings. Evidence files are supporting reproductions, not required reading unless diagnosing a finding.
5. The Nocturne readme/styles.css and relevant screen markup/renders before changing visuals.
6. Inspect git status, log and diff, then read the affected implementation/tests in full before editing.

## Important context

- Toolchain is already configured: Gradle 8.7, AGP 8.4.1, Kotlin 1.9.24, Compose compiler 1.5.14. Do not bump versions or add dependencies without owner approval.
- Original Phase 1 repairs: atomic failed saves, canonical domain identity, and target_type to exclude sites from the per-app leaderboard. Preserve those invariants. a0df32c restored meaningful failed-save rollback tests; preserve that coverage and address the latest targeted gaps.
- Phase 0 error palette has owner approval. Direct OFF is temporary until Phase 5; ON permission gating belongs to Phase 3. Stats/feedback belong to Phase 7.
- 0.3 seconds/character meets the pinned acceptance estimate; PRD's 300-character prose conflicts. The discrepancy is documented; do not silently rewrite product requirements.
- RESOLVED in a0df32c: the conflict flow (in-place Box) is removed; no modal paths remain in Phase 2 flows. DECISIONS.md now records the correction.
- Carry-forward (non-blocking, from the report): route and draft state use plain `remember` and die on activity recreation — saved-state preservation is wanted before release. Glyph-only clickable controls lack accessibility labels/roles — address as shared controls mature. Destructive migrations remain dev-only.

## Preserve unrelated work

At handoff, DECISIONS.md already has an owner branding update; old icon-concept files are deleted and design/icon-final is untracked. These are unrelated changes. Do not revert them or sweep them into a repair commit with git add -A.

The recorded owner decision is to rename the product to Time Shrine in a dedicated change; existing code/spec still say Toki Shrine and package com.arjunrana.tokishrine. The selected icon is the torii/hourglass reference under design/icon-final/master/. Do not combine the rename/icon implementation with Phase 2 repairs or start it without a scoped assignment.

## Keeping future sessions reliable

Treat repository documents and verified code as durable context. At every stop, record the current commit, uncommitted work, exact remaining task, tests actually run, decisions and unresolved issues. Keep this handoff short and current. Ask the outgoing builder to append any unrecorded discoveries before closing its session. A fresh session should first summarize its scope and findings back to Arjun, then carry out the authorized repair work. Do not claim access to the old conversation or assume every old completion claim was independently verified.

## Model and run-boundary reminder

Follow AGENTS.md §3c. At each phase/repair start, recommend the builder model with a reason and state scope, checks and stop condition. Codex remains the independent reviewer; switching builders does not trigger a whole-codebase review. Preserve the current repair and review its diff before requesting further implementation. After two failed attempts at the same check, report a bounded diagnosis instead of continuing speculative retries. Once planned checks pass, hand off. Phone preparation follows code approval and an owner setup request; it is not authorization to run device tests.

At phase/repair start, use the model/scope/checks/stop format in AGENTS.md §3c. Flash is the owner-reported image-reading choice for supplied visual references and bounded UI work; GLM 5.3 is preferred for complex state/lifecycle/persistence. Do not add a second whole-project review or require a model handoff for trivial work. On interruption, report from local records without launching another check or querying the phone.

# TS-block-wizard-redesign — five-screen block editor

- **State:** `closed` — WZ-F01/WZ-F02 and DC-06 have reviewer PASS; BW-01–BW-10 have owner PASS; the final Android suite executed 51/51 with no failures. See [HANDBACK](HANDBACK.md), [REVIEW](REVIEW.md) and [OWNER-CHECKS](OWNER-CHECKS.md).
- **Goal:** simplify create/edit comprehension using method cards, optional details and inherited disable difficulty.
- **Implementation ownership:** GLM implemented the wizard, summaries and bounded repository/schema wiring. After the first review failed on cancellation/recreation and durable event ordering, Codex took senior ownership of WZ-F01/WZ-F02 under WORKFLOW. The owner permits discarding all existing data, removing the need for compatibility migration logic. Arjun dispatches manually; one writer.
- **Base:** `f92661d` (verify HEAD before work). Existing uncommitted workflow/PRD/docs, design assets/deletions and verification reports are present. Approved working-tree docs are inputs, not disposable changes. Stage only task-owned changes; preserve unrelated work.
- **Authority:** [PRD](../../../PRD.md) §7, §10 and **Block wizard redesign — 19 September 2026** at the end of §17. This newest amendment supersedes old configuration copy/ranges. In particular pause minimum is **5 minutes**, not the image's 10. Wait default is 60 seconds; disable ladder is 3/6/12 minutes.
- **Design references:** [Friction selection](../../../design/screens/Friction-selection.png), [Disable block](../../../design/screens/Disable%20block.png). Use visible card layout/copy subject to the written spec; annotations are reference material, not implementation instructions.
- **Contracts:** [blocks/targets](../../../docs/components/blocks-targets.md), [persistence/events](../../../docs/components/persistence-events.md), [theme/UI](../../../docs/components/theme-ui.md), [build/validation](../../../docs/components/build-validation.md). Read navigation/permissions only for affected activation/editor routing dependencies.

## Scope and invariants

- Five screens and progress segments: contents, name, method, disable difficulty, review. Preserve existing contents/name validation and list behavior.
- Step 3 has two cards, typing initially selected, and outlined Adjust the details below them in normal layout. Details sheet only shows the selected method's field plus pause duration. Changes/Reset/Done/dismissal update draft and card summary; no database save. Sheet Back precedes step Back. Preserve each method's draft settings when toggling.
- Typing default 150, range 100..200, step 10; waiting default 60 seconds, range 60..300, step 5; pause default 15 minutes, range 5..100, step 5. Reset restores active method and pause defaults only.
- Step 4 inherits method; fixed typing 220/350/700 or waiting 180/360/720 seconds; middle preselected. Remember per-method draft choices. No custom disable stepper or second method selector.
- Review remains the existing layout with updated summaries, target-list bound, pinned Save and homescreen helper. Update detail and activation summaries too; remove misleading typing-only or phone-in-hand copy there. Preserve save OFF and existing permission-gated ON path.
- Edit starts at 1/5 or direct friction 3/5; direct-entry Back exits to detail. Load stored choices; canceled edits do not write. Draft values survive supported recreation, backtracking and rejected saves. Preserve target ownership, nonempty saves, terminal guards and transactional event boundaries.
- Store `turnoff_seconds` separately (default 360); no seconds in `turnoff_chars`. Update entity/draft/repository create/update, equality/change tracking, summaries, event payloads and fixtures. Validate ranges/increments and fixed ladders at the persistence boundary. Remove obsolete show_typos preference/column in the deliberate schema revision; highlighting remains unconditional.
- Owner explicitly allows all existing data to be discarded. A deliberate development schema version change/reset is allowed; no legacy mapping/migration required. Do not delete data on every startup. Report reset behavior and that old local block/event/onboarding fixtures may be lost. No device operation is part of this implementation assignment.
- Creation event steps become 1..5; the sheet emits no step-completed event. New disable field participates in block_created and fields_changed. Preserve existing save/event ordering and duplicate-action protections.

## Checks and delivery

- Meaningful focused tests for draft defaults/bounds/reset/method switching, fixed disable choices, recreation/back/cancellation, edit persistence and rejected saves. Reuse existing test infrastructure. Include new field validation and round-trip coverage at the repository layer, plus five-step events and unchanged-edit behavior.
- Run `./build.sh assembleDebug`, `./build.sh testDebugUnitTest`, and `./build.sh assembleDebugAndroidTest` (compile-only). Report fresh test counts from result XML. Apply scoped visual token checks; no new dependencies/toolchain changes.
- [OWNER-CHECKS](OWNER-CHECKS.md) holds new UX acceptance and affected regressions. Arjun performs device/experience checks; unexecuted instrumented cases remain explicit evidence gaps.
- Write HANDBACK with exact base/submission, scope, commands/results, schema reset consequences and limitations. Set CURRENT to ready_for_review for Codex. Independent review precedes owner acceptance; do not claim source/build checks prove visual or Room execution results.
- **Stop:** after scoped implementation and checks. No Phase 4/5/6 runtime work, services, hold detection, challenge timers, installations, rename or unrelated repairs. One unsuccessful GLM repair submission requires ownership reassessment.

## Phase continuity

[TS-P3-validation](../TS-P3-validation/TASK.md) is closed with owner and executed nonvisual PASS. [Phase 4](../TS-P4-detection-engine/TASK.md) is authorized. Waiting/disable challenge execution remains Phase 5, including visibility/unlocked cancellation and completion races under senior ownership.

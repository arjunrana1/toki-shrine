# Phase 2 — Block list and create flow

Original owner validation was recorded at `512dc36`; later refinements were code-approved at `6c3b962`. The five-screen redesign and affected preservation matrix are closed at `7dc5aa4` with reviewer, owner and executed nonvisual PASS. Preserve the evidence attribution in the linked task records.

Read only for this phase or an affected acceptance question. Scope/acceptance below was relocated from the original root instructions. PRD and applicable §17 amendments resolve stale copy/values; current WORKFLOW governs who performs checks. Historical shell/device commands describe evidence, not standing authorization; build through `./build.sh` and leave device operations to the authorized owner.

## Scope and acceptance

Screens 5–14: block list (empty and populated), the five create steps, app search with unavailable rows for apps owned by another block, block detail and edit. Screen 12 is removed for apps and websites by the owner decision recorded in PRD.md.

**Acceptance**

- A block created through the UI appears in the list with its toggle **off**.
- Implemented redesign: progress `1 / 5` through `5 / 5`; contents → name → method → disable difficulty → review. Details is a sheet within step 3.
- Method defaults: typing 150 characters or waiting 60 seconds, pause 15 minutes. Details bounds: 100–200 chars/10; 60–300 sec/5; 5–100 min/5. Reset and dismissal operate on draft only.
- Disable inherits method, with fixed 220/350/700-character or 3/6/12-minute choices; middle preselected. Review/detail/activation summaries reflect stored values.
- Full checks, design references and attributed closure evidence: [wizard redesign task](../../coordination/tasks/TS-block-wizard-redesign/TASK.md).
- An app owned by another block is visible in the picker with **Already added to a block**, without the block name, and cannot be selected. No app conflict dialog or move action appears, whether the owning block is ON or OFF. Websites already owned by another block show the same message beside the entry and cannot be added. No conflict dialog or move action exists for either type. Saving cannot reassign either target type; rejected saves leave existing data intact.
- With a block ON, the edit action is unavailable and the detail screen states the block must be off first.
- Toki Shrine itself does not appear in the app picker.

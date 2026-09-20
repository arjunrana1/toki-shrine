# Historical decision record

Historical evidence, not current workflow instructions. Later records may supersede this entry. Use current component contracts and PRD requirements for implementation.

Source: pre-migration DECISIONS.md, section 9.

## Phase 2 app ownership UX — owner decision, 9 September 2026

- Apps owned by another block stay visible in the picker with **Already added to a block**, no owning block name, and cannot be selected. Applies to ON and OFF owners. Remove app conflict dialog and app move action; this supersedes the original move-confirmation requirement and the verifier suggestion to retain confirmed app transfers. Current-block apps remain editable while that block is OFF.
- The repository must still reject ownership conflicts atomically; disabling a row alone does not protect stale drafts or alternate write paths. Restore actual failed-save rollback coverage. App dialog removal resolves that app-specific modal finding; Back handling and abandonment events still require repair.
- Website behavior was not specified in the owner's app-picker request; clarification requested. No website policy change recorded yet.
- Updated PRD.md, AGENTS.md, GLM_HANDOVER.md and the report addendum. These are requirements/handoff changes only; no production code changed or new sign-off issued.

- **Website clarification received:** owner confirmed the same rule for websites. Show **Already added to a block** beside an already-owned website entry and prevent adding it, without naming the block. Remove conflict dialogs and transfer actions for both apps and websites. Enforce ownership and atomic failure for both target types. This resolves the clarification above. Conflict shown/resolved telemetry is retired with the removed interaction; historical events remain intact.

- **Phase 2 repairs (9 September 2026).** All three blocking findings closed under the owner's ownership decision:
  - The app/site conflict-dialog and move flow is removed entirely. Targets owned by another block — ON or OFF — appear in the picker as a disabled row labelled exactly "Already added to a block" (no owner name); typed domains held elsewhere show the same message beside the entry with Add disabled. The edited block's own targets remain editable while OFF, and edit-mode search shows them as "Added".
  - The repository rejects conflicting ownership at the transaction boundary (`ConflictingOwnershipException`): createBlock/updateBlock abort atomically with the parent row rolling back, so a rejected save — including one from a stale draft whose target changed owners mid-edit — leaves the stored data untouched. Targets are never reassigned by any write path. The move APIs (moveApp/moveSite, AddTargetResult, single-target holding lookups) are gone; the picker reads bulk ownership maps (appOwnerships/siteOwnerships). In-draft duplicate targets collapse; only cross-block ownership rejects.
  - Android Back inside the create/edit flow now mirrors the appbar: app-search closes first, then steps unwind, and only a step-1 Back exits — through the abandonment logger exactly once (create mode; edits stay silent). With the IME open, system Back dismisses the keyboard first, so the handler only runs once the keyboard is down.
  - Conflict shown/resolved telemetry is retired with the removed flow; historical event rows are preserved untouched.
  - Coverage: the moved-API tests were replaced with rejection/rollback tests — app and site rejections (the site case via canonical case-variant identity), rejection from an ON source, mid-transaction parent+child rollback proving all-or-nothing, stale-draft update rejection leaving the block unchanged, own-target re-add as a canonical no-op, and the picker ownership maps. Suite after repairs: 7 JVM + 17 device tests green on the S23 Ultra; every fix exercised on hardware (back/abandonment event, held rows for two different owners, site label with Add disabled, edit-own "Added" state, free-site add).

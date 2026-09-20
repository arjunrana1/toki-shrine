# Historical decision record

Historical evidence, not current workflow instructions. Later records may supersede this entry. Use current component contracts and PRD requirements for implementation.

Source: pre-migration DECISIONS.md, section 26.

## Phase 3 independent code review — 14 September 2026

- **`9a88b41` on `6c3b962`: CHANGES REQUIRED; no device sign-off.** Nominal static paths satisfy the four-row checklist, real system-state reads/resume refresh, explainer-before-settings ordering, shared accessibility gate (home, detail and final confirmation), Settings entry and post-write haptic ordering. No Phase 2 regression was found in the affected ownership/validation/save paths.
- **Lifecycle blocker:** the hand-rolled route stack, onboarding display state and outstanding permission requests are held only with `remember`/an in-memory set. Activity or process recreation while onboarding or system Settings is active can return to Welcome/Block list instead of the checklist and loses the matching `permission_granted`/`permission_denied` event. Preserve the minimal route/mode/request state across recreation and add targeted restoration/settlement regression coverage.
- **Terminal-action blocker:** activation and onboarding completion have no synchronous single-flight guard, and their mutations run in composition scopes while Back/Not yet remains available. Rapid taps can duplicate events/haptics; leaving a screen can cancel between state persistence and event recording; two turn-on jobs can invoke the stack pop twice. Make terminal mutations exactly once and navigation-safe, including failure behavior, and add focused rapid-repeat/cancel regressions. Repair scope is Phase 3 only; owner device validation follows code clearance.
- **GLM handoff preference (owner, 14 September 2026).** Whenever a builder task or repair is needed, Codex provides Arjun a paste-ready, token-efficient prompt containing the exact model, commit boundary, concrete files/findings, implementation direction where known, required checks/evidence, exclusions, deliverables and stop condition. The builder should not spend tokens rediscovering an already-reviewed problem or guessing the work boundary.

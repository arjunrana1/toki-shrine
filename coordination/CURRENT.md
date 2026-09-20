# Current work

- **Active task:** [TS-P4-detection-engine](tasks/TS-P4-detection-engine/TASK.md) — Accessibility detection and supported-browser URL matching.
- **State:** `ready` — Phase 3 and the block-wizard redesign are closed with reviewer, owner and executed nonvisual PASS. Phase 4 implementation is authorized.
- **Next actor:** Arjun opens a GLM 5.3 Pro session as the Phase 4 implementer. One writer; no automatic dispatch. Flash is not recommended for the service/window/URL state machine.
- **Base:** the committed Phase 3/wizard closure plus repository-hygiene documentation/assets at repository HEAD when this handoff is delivered. The Phase 4 implementer must record `git rev-parse HEAD` before editing. The worktree is expected to be clean; if it is not, reconcile unexpected changes with Arjun before writing and touch only task-owned files.
- **Phase 4 boundary:** follow the [task](tasks/TS-P4-detection-engine/TASK.md) and [phase scope](../docs/phases/phase-04.md). Detection service, bundled JSON loader, per-window URL cache, focused-address/search exclusion, settle cancellation, domain matching and a plain trigger placeholder only. No Phase 5 challenge/pause runtime.
- **Device state:** final `./build.sh connectedDebugAndroidTest` passed **51/51** on SM-S918B (`R5CW30ZBM2R`, Android 16). Gradle then removed both app and test packages; `pm path` returned no installation and `enabled_accessibility_services` was `null`. Reinstall and re-grant before later device acceptance. The temporary USB stay-awake setting was restored.

## Closed Phase 3 and wizard evidence

- Arjun reports all P3-01–P3-15 and BW-01–BW-10 acceptance checks passing, including the revised ON haptic, overlay deny/grant/revoke flow and DC-06 sheet darkness.
- [Phase 3 REVIEW](tasks/TS-P3-validation/REVIEW.md) records final PASS. `./build.sh testDebugUnitTest` passed 60/60. Executed Android evidence passed 51/51: BlockRepositoryTest 23, EventRepositoryTest 9, CreateFlowScreenTest 19.
- P3-NV-02: enabled-state/event and onboarding marker/event commit, rollback and at-most-once behavior executed against Room, including forced event failures.
- P3-NV-03: owner callback/resume/recreation paths plus executed restored-pending Room failure/retry evidence establish one durable permission outcome.
- P3-NV-04: the complete revised ownership/save/UI/recreation suite executed successfully.
- [Wizard REVIEW](tasks/TS-block-wizard-redesign/REVIEW.md) records WZ-F01/WZ-F02 and DC-06 PASS plus instrumented closure. No prior-phase blocker remains.

Workflow migration completed 18 September 2026 is documentation-only; see [migration record](../docs/history/migration-2026-09-18/MIGRATION.md). Current-state changes belong here, not in AGENTS or global handoffs.

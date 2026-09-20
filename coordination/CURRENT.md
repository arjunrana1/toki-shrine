# Current work

- **Active task:** [TS-P4-detection-engine](tasks/TS-P4-detection-engine/TASK.md) — Accessibility detection and supported-browser URL matching.
- **State:** `closed` — Phase 4 is cleared. Independent Codex review PASSed `9b3da43`; owner reports P4-01–P4-05/P4-07/P4-08 passing; P4-06 is explicitly waived; and the authorized Android asset-loader suite passed **5/5**. P4-09 is an owner-accepted deferred Samsung-survival risk, not a pass; see [OWNER-CHECKS](tasks/TS-P4-detection-engine/OWNER-CHECKS.md). Phase 5 implementation may begin only under a new scoped task.
- **Next actor:** Arjun opens the Phase 5 task when ready. Preserve the Phase 4 deferred-risk record; do not represent overnight survival as validated.
- **Base for any Phase 4 repair:** `9b3da43`. Preserve documentation records and unrelated work.
- **Phase 4 implementation record:** settle callbacks require live active root/window/package; pending work invalidates on block-map and `(windowId, package)` replacement. `block_screen_shown` is emitted from `BlockActivity.onPostResume` with monotonic end-to-end latency and a saved one-shot guard. Browser/OEM JSON and the matcher/config seam remain unchanged.
- **Device state:** at Phase 3 closure, 51 Android tests passed on SM-S918B (`R5CW30ZBM2R`, Android 16) and packages were removed. Arjun later installed a build at records-only `aa6e91c` (code equivalent to `9b3da43`). On 20 September 2026, Codex ran the authorized targeted `AssetDetectionConfigLoaderTest` suite on that same device: **5/5 passed**. Gradle then removed both app and test packages (`pm path` empty); `enabled_accessibility_services` is `null`. Reinstall, re-grant and re-seed are required for future device work. See [build/validation](../docs/components/build-validation.md).

## Closed Phase 3 and wizard evidence

- Arjun reports all P3-01–P3-15 and BW-01–BW-10 acceptance checks passing, including the revised ON haptic, overlay deny/grant/revoke flow and DC-06 sheet darkness.
- [Phase 3 REVIEW](tasks/TS-P3-validation/REVIEW.md) records final PASS. `./build.sh testDebugUnitTest` passed 60/60 at that time. Executed Android evidence passed 51/51: BlockRepositoryTest 23, EventRepositoryTest 9, CreateFlowScreenTest 19.
- P3-NV-02: enabled-state/event and onboarding marker/event commit, rollback and at-most-once behavior executed against Room, including forced event failures.
- P3-NV-03: owner callback/resume/recreation paths plus executed restored-pending Room failure/retry evidence establish one durable permission outcome.
- P3-NV-04: the complete revised ownership/save/UI/recreation suite executed successfully.
- [Wizard REVIEW](tasks/TS-block-wizard-redesign/REVIEW.md) records WZ-F01/WZ-F02 and DC-06 PASS plus instrumented closure. No prior-phase blocker remains.

Workflow migration completed 18 September 2026 is documentation-only; see [migration record](../docs/history/migration-2026-09-18/MIGRATION.md). Current-state changes belong here, not in AGENTS or global handoffs.

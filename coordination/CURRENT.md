# Current work

- **Active task:** [TS-P4-detection-engine](tasks/TS-P4-detection-engine/TASK.md) — Accessibility detection and supported-browser URL matching.
- **State:** `awaiting_owner` — independent Codex review PASSed `9b3da43` for P4-F01–P4-F04; see [REVIEW](tasks/TS-P4-detection-engine/REVIEW.md). Implementation-attributed non-device checks passed (`assembleDebug`, `testDebugUnitTest` 109/109, `assembleDebugAndroidTest` compile-only). Owner/device validation and authorized instrumented execution remain open in [OWNER-CHECKS](tasks/TS-P4-detection-engine/OWNER-CHECKS.md).
- **Next actor:** Arjun performs or reports the Phase 4 owner checks. Record results by P4 ID; do not run the instrumented suite without explicit authorization and a data-preservation plan.
- **Base for any further repair:** `9b3da43`. Preserve documentation records and unrelated work.
- **Implementation notes:** settle callbacks re-read and require the live active root/window/package; pending work is invalidated by block-map changes and `(windowId, package)` replacement. `block_screen_shown` moved from launch acceptance to `BlockActivity.onPostResume` with monotonic end-to-end latency and a saved one-shot guard. Browser/OEM JSON and the accepted matcher/config seam are unchanged.
- **Device state:** unchanged from Phase 3 closure — final `./build.sh connectedDebugAndroidTest` passed **51/51** on SM-S918B (`R5CW30ZBM2R`, Android 16); app and test packages were then removed (`pm path` empty, `enabled_accessibility_services` `null`). Reinstall, re-grant accessibility and re-seed fixtures before any Phase 4 device acceptance; the new androidTest suite (56 instrumented tests incl. `AssetDetectionConfigLoaderTest`) also awaits an authorized run. Plan data preservation first — see [build/validation](../docs/components/build-validation.md).

## Closed Phase 3 and wizard evidence

- Arjun reports all P3-01–P3-15 and BW-01–BW-10 acceptance checks passing, including the revised ON haptic, overlay deny/grant/revoke flow and DC-06 sheet darkness.
- [Phase 3 REVIEW](tasks/TS-P3-validation/REVIEW.md) records final PASS. `./build.sh testDebugUnitTest` passed 60/60 at that time. Executed Android evidence passed 51/51: BlockRepositoryTest 23, EventRepositoryTest 9, CreateFlowScreenTest 19.
- P3-NV-02: enabled-state/event and onboarding marker/event commit, rollback and at-most-once behavior executed against Room, including forced event failures.
- P3-NV-03: owner callback/resume/recreation paths plus executed restored-pending Room failure/retry evidence establish one durable permission outcome.
- P3-NV-04: the complete revised ownership/save/UI/recreation suite executed successfully.
- [Wizard REVIEW](tasks/TS-block-wizard-redesign/REVIEW.md) records WZ-F01/WZ-F02 and DC-06 PASS plus instrumented closure. No prior-phase blocker remains.

Workflow migration completed 18 September 2026 is documentation-only; see [migration record](../docs/history/migration-2026-09-18/MIGRATION.md). Current-state changes belong here, not in AGENTS or global handoffs.

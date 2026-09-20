# Current work

- **Active task:** [TS-P4-detection-engine](tasks/TS-P4-detection-engine/TASK.md) — Accessibility detection and supported-browser URL matching.
- **State:** `ready_for_review` — Phase 4 implementation submitted by GLM as `0ca1424` on base `0ddd569`; see [HANDBACK](tasks/TS-P4-detection-engine/HANDBACK.md). Non-device checks passed (`assembleDebug`, `testDebugUnitTest` 101/101, `assembleDebugAndroidTest` compile-only). Independent Codex review precedes owner/device acceptance.
- **Next actor:** Arjun opens a Codex session as reviewer for submission `0ca1424`; paste-ready prompt at the end of the HANDBACK.
- **Base for any repair:** `0ca1424`. The worktree is expected to be clean; reconcile unexpected changes with Arjun before writing.
- **Implementation notes for review:** all §13 rules live in the pure `DetectionEngine` (JVM-tested); the service is a thin adapter (package filter, `rootInActiveWindow` address reads, §10 events, placeholder launch). Browser map + OEM battery text ship in `assets/detection_config.json` behind `DetectionConfigLoader`. Full asset parsing is covered by a new androidTest suite awaiting the owner-authorized instrumented run; no device execution has been performed or claimed.
- **Device state:** unchanged from Phase 3 closure — final `./build.sh connectedDebugAndroidTest` passed **51/51** on SM-S918B (`R5CW30ZBM2R`, Android 16); app and test packages were then removed (`pm path` empty, `enabled_accessibility_services` `null`). Reinstall, re-grant accessibility and re-seed fixtures before any Phase 4 device acceptance; the new androidTest suite (56 instrumented tests incl. `AssetDetectionConfigLoaderTest`) also awaits an authorized run. Plan data preservation first — see [build/validation](../docs/components/build-validation.md).

## Closed Phase 3 and wizard evidence

- Arjun reports all P3-01–P3-15 and BW-01–BW-10 acceptance checks passing, including the revised ON haptic, overlay deny/grant/revoke flow and DC-06 sheet darkness.
- [Phase 3 REVIEW](tasks/TS-P3-validation/REVIEW.md) records final PASS. `./build.sh testDebugUnitTest` passed 60/60 at that time. Executed Android evidence passed 51/51: BlockRepositoryTest 23, EventRepositoryTest 9, CreateFlowScreenTest 19.
- P3-NV-02: enabled-state/event and onboarding marker/event commit, rollback and at-most-once behavior executed against Room, including forced event failures.
- P3-NV-03: owner callback/resume/recreation paths plus executed restored-pending Room failure/retry evidence establish one durable permission outcome.
- P3-NV-04: the complete revised ownership/save/UI/recreation suite executed successfully.
- [Wizard REVIEW](tasks/TS-block-wizard-redesign/REVIEW.md) records WZ-F01/WZ-F02 and DC-06 PASS plus instrumented closure. No prior-phase blocker remains.

Workflow migration completed 18 September 2026 is documentation-only; see [migration record](../docs/history/migration-2026-09-18/MIGRATION.md). Current-state changes belong here, not in AGENTS or global handoffs.

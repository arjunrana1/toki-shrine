# Current work

- **Active task:** [TS-P5A-interruption-ui](tasks/TS-P5A-interruption-ui/TASK.md) — bounded stateless interruption screens and assets.
- **State:** `ready_for_review` — GLM 5.3 Pro submitted the initial P5A implementation `3ba8037` on the clean task-opening base `8523006` (records commit aside). Stateless Compose surfaces for screens 15–19/22, byte-identical humour image/font resources and 20 new JVM tests are in place; authorized non-device checks pass (129/129 unit tests, compile-only androidTest, `git diff --check`). Details, contract and flagged decisions: [HANDBACK](tasks/TS-P5A-interruption-ui/HANDBACK.md). No device operations occurred; owner visual/input acceptance remains pending after review.
- **Next actor:** Arjun dispatches an independent Codex reviewer for submission `3ba8037` against the P5A scope boundary and PRD §§6–8, 11, 17. On PASS, P5B ([TS-P5B-challenge-runtime](tasks/TS-P5B-challenge-runtime/TASK.md)) may be scheduled; owner visual/device acceptance is separate and follows review.
- **Implementation boundary:** P5A owns stateless Compose surfaces, supplied humour resources/font, paste/autocorrect suppression and pure presentation helpers. [P5B](tasks/TS-P5B-challenge-runtime/TASK.md) owns `BlockActivity` integration, random selection, lifecycle/lock handling, monotonic countdown, terminal races, events, walk-away count and block-disable persistence. Phase 6 retains the actual pause/access/re-arm lifecycle, bubble and notification.
- **Phase 4 retained risk:** P4-09 Samsung overnight survival remains owner-accepted deferred risk, not a pass. Do not represent it as validated; see the closed [Phase 4 checklist](tasks/TS-P4-detection-engine/OWNER-CHECKS.md).
- **Device state:** at Phase 3 closure, 51 Android tests passed on SM-S918B (`R5CW30ZBM2R`, Android 16) and packages were removed. Arjun later installed a build at records-only `aa6e91c` (code equivalent to `9b3da43`). On 20 September 2026, Codex ran the authorized targeted `AssetDetectionConfigLoaderTest` suite on that same device: **5/5 passed**. Gradle then removed both app and test packages (`pm path` empty); `enabled_accessibility_services` is `null`. Reinstall, re-grant and re-seed are required for future device work. See [build/validation](../docs/components/build-validation.md).

## Closed Phase 3 and wizard evidence

- Arjun reports all P3-01–P3-15 and BW-01–BW-10 acceptance checks passing, including the revised ON haptic, overlay deny/grant/revoke flow and DC-06 sheet darkness.
- [Phase 3 REVIEW](tasks/TS-P3-validation/REVIEW.md) records final PASS. `./build.sh testDebugUnitTest` passed 60/60 at that time. Executed Android evidence passed 51/51: BlockRepositoryTest 23, EventRepositoryTest 9, CreateFlowScreenTest 19.
- P3-NV-02: enabled-state/event and onboarding marker/event commit, rollback and at-most-once behavior executed against Room, including forced event failures.
- P3-NV-03: owner callback/resume/recreation paths plus executed restored-pending Room failure/retry evidence establish one durable permission outcome.
- P3-NV-04: the complete revised ownership/save/UI/recreation suite executed successfully.
- [Wizard REVIEW](tasks/TS-block-wizard-redesign/REVIEW.md) records WZ-F01/WZ-F02 and DC-06 PASS plus instrumented closure. No prior-phase blocker remains.

Workflow migration completed 18 September 2026 is documentation-only; see [migration record](../docs/history/migration-2026-09-18/MIGRATION.md). Current-state changes belong here, not in AGENTS or global handoffs.

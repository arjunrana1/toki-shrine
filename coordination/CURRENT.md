# Current work

- **Active task:** [TS-P5B-challenge-runtime](tasks/TS-P5B-challenge-runtime/TASK.md) — Phase 5 challenge lifecycle, outcomes and events.
- **State:** `ready_for_review` — senior Codex submitted P5B implementation `eb2f956` on base `9e3a373`. The pure runtime, thin Activity adapter, defensive launch resolution, monotonic visible-only wait, stable-session retry guards, transactional walk-away count/events and atomic disable completion are in place. Implementer checks pass: debug assembly, **142/142 JVM tests** (13 new), compile-only Android-test assembly (six new Room tests), and diff check. Details and limits: [HANDBACK](tasks/TS-P5B-challenge-runtime/HANDBACK.md). No device operations or Android-test execution occurred.
- **Next actor:** Arjun dispatches an independent Codex reviewer for `eb2f956` against base `9e3a373` and the P5B task/contracts. Review must PASS before Phase 5 owner acceptance. Do not begin Phase 6.
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

# Current work

- **Active task:** [TS-P5-owner-corrections](tasks/TS-P5-owner-corrections/TASK.md) — owner-tested interruption corrections.
- **State:** `ready_for_review` — senior Codex submitted owner corrections `7fbb692` on base `59596a8`; see [HANDBACK](tasks/TS-P5-owner-corrections/HANDBACK.md) and the recorded [owner observations](tasks/TS-P5-owner-corrections/OWNER-CHECKS.md). Debug/release assembly, 146/146 JVM tests, compile-only Android-test assembly, and structural checks pass as implementer evidence.
- **Next actor:** a fresh independent Codex reviewer inspects `59596a8..7fbb692`, records PASS/FAIL, and stops. No installation or owner retest begins before review PASS; do not begin Phase 6.
- **Implementation boundary:** the current correction submission spans the formerly split P5A/P5B surfaces and runtime only where owner testing required it. Phase 6 still retains the actual pause/access/re-arm lifecycle, bubble and notification.
- **Phase 4 retained risk:** P4-09 Samsung overnight survival remains owner-accepted deferred risk, not a pass. Do not represent it as validated; see the closed [Phase 4 checklist](tasks/TS-P4-detection-engine/OWNER-CHECKS.md).
- **Device state:** at Phase 3 closure, 51 Android tests passed on SM-S918B (`R5CW30ZBM2R`, Android 16) and packages were removed. Arjun later installed a build at records-only `aa6e91c` (code equivalent to `9b3da43`). On 20 September 2026, Codex ran the authorized targeted `AssetDetectionConfigLoaderTest` suite on that same device: **5/5 passed**. Gradle then removed both app and test packages (`pm path` empty); `enabled_accessibility_services` is `null`. On 21 September 2026, at Arjun's request, the Phase 5B debug APK built at `c5a8e9e` (records-only over reviewed repair `7a8fb00`) was installed on that device via `adb install -r` (Success). Accessibility re-grant and any seeding remain owner setup; the app was not launched and no device testing was performed. See [build/validation](../docs/components/build-validation.md).

## Closed Phase 3 and wizard evidence

- Arjun reports all P3-01–P3-15 and BW-01–BW-10 acceptance checks passing, including the revised ON haptic, overlay deny/grant/revoke flow and DC-06 sheet darkness.
- [Phase 3 REVIEW](tasks/TS-P3-validation/REVIEW.md) records final PASS. `./build.sh testDebugUnitTest` passed 60/60 at that time. Executed Android evidence passed 51/51: BlockRepositoryTest 23, EventRepositoryTest 9, CreateFlowScreenTest 19.
- P3-NV-02: enabled-state/event and onboarding marker/event commit, rollback and at-most-once behavior executed against Room, including forced event failures.
- P3-NV-03: owner callback/resume/recreation paths plus executed restored-pending Room failure/retry evidence establish one durable permission outcome.
- P3-NV-04: the complete revised ownership/save/UI/recreation suite executed successfully.
- [Wizard REVIEW](tasks/TS-block-wizard-redesign/REVIEW.md) records WZ-F01/WZ-F02 and DC-06 PASS plus instrumented closure. No prior-phase blocker remains.

Workflow migration completed 18 September 2026 is documentation-only; see [migration record](../docs/history/migration-2026-09-18/MIGRATION.md). Current-state changes belong here, not in AGENTS or global handoffs.

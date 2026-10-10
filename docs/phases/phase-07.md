# Phase 7 — Stats redesign, feedback and final pass

Reopened by Arjun on 4 October 2026. Continue from `89ef9e3` (earlier Phase 7 base `12ee572`); do not revert Phase 6 or discard useful Feedback, production-value, event-retirement or accessibility work. [Active task](../../coordination/tasks/TS-P7-stats-feedback-final/TASK.md) owns assignment/status. [Preserved submission](../../coordination/tasks/TS-P7-stats-feedback-final/submissions/89ef9e3-before-stats-redesign/TASK.md) keeps previous evidence; its review does not cover new code.

## Scope and sequence

1. Codex: revised PRD/data contract, v3→v4 migration, immutable baseline/savings, outcome deduplication, usage ingestion and post-challenge attribution, fixed UI model, focused tests.
2. GLM 5.3 Flash: screen 23 from [S1–S6 PNGs](../../design/screens/stats-redesign/README.md), five-row permission UI and routine local-event wiring against [Stats contract](../components/stats.md). Preserve completed Feedback screen 25 and unrelated accepted phases.
3. One scoped independent review of the combined critical changes and UI integration, plus separately authorized migration/Room/platform evidence and Arjun's experience acceptance. No automatic dispatch or device work.

## Acceptance

- PRD §9 metrics/states/copy, including app-only resolved attempts, fixed-anchor nope deduplication, immutable savings, fallback and explicit recalibration.
- Usage Access controls Stats only. Blocking and the Accessibility-only activation gate still work without it.
- Charts, sheets, app list, unknown values and zero dashboard match the updated contract and six references with Nocturne, scrolling, insets, large-text and accessible touch targets.
- v3 data preserved through explicit v4 migration; retired events retained, not re-emitted. Transaction/idempotency, usage measurement and real Android evidence reported separately.
- Existing Feedback handoff/draft/failure behavior and production challenge values remain valid. Complete outstanding Phase 7 final-screen/event/Phase 1–6 regression checks.
- Only local events; no PostHog, network permission, SDK, accounts, package/brand rename or dependency/toolchain changes.

Required non-device checks: `./build.sh assembleDebug testDebugUnitTest assembleDebugAndroidTest`, diff check, manifest/dependency and theme audits as applicable. Android-test compilation is not execution. [Owner checks](../../coordination/tasks/TS-P7-stats-feedback-final/OWNER-CHECKS.md) and [handback](../../coordination/tasks/TS-P7-stats-feedback-final/HANDBACK.md) distinguish evidence. [PostHog](../plans/posthog-analytics.md) remains later.

## Closure — 11 October 2026 (owner decision)

**Closed** with no code blockers. Owner device acceptance passed through the 11 October retest (APK `8a7cbdb9…`). Code: `c19b53f`; records: `5501509`. Arjun closed the phase with these obligations carried to the **launch gate**:

- **P7-D3/D4 database and usage audit:** a read-only pull of the app database plus `dumpsys usagestats`, checked against the code once real usage exists. Also explains the 7 nopes of unknown source from the 10 October retest.
- **Run the Android tests** (Room repository, migration and UI tests). Since the 4 October P7-D1/D2 run they have only compiled, across schema v5, auto-nope and the two-minute window. Run them on the isolated emulator (AVD `toki-p7d-isolated`) with owner authorization.
- **Debug testing values (delta 5)** stay until launch. Then decide on removal and confirm production values (P7-O14) as part of the pre-launch config review.
- **Not blocking, owner's option:** independent review of deltas 11, 12A and 13; the PRD wording "returns to the block" vs the Blocks list for the turn-off escape (retest row 4); Samsung overnight survival (P4-09, accepted deferred risk).

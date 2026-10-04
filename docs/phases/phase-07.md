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

Required non-device checks: `./build.sh assembleDebug testDebugUnitTest assembleDebugAndroidTest`, diff check, manifest/dependency and theme audits as applicable. Android-test compilation is not execution. [Owner checks](../../coordination/tasks/TS-P7-stats-feedback-final/OWNER-CHECKS.md) and [handback](../../coordination/tasks/TS-P7-stats-feedback-final/HANDBACK.md) distinguish evidence. Phase 7 stays open; [PostHog](../plans/posthog-analytics.md) remains later.

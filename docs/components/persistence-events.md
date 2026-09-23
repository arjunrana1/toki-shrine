# Persistence, events and Stats contract

Read for data mutations, analytics/Stats, schema or repository boundary changes. Current rules distilled from PRD §§4/8–10/17 and repaired code; historical chronology is [indexed separately](../history/INDEX.md). This is not new execution evidence.

## Implementation pointers

Under `app/src/main/java/com/arjunrana/tokishrine/`: `data/repo/BlockRepository.kt`, `EventRepository.kt`; `data/db/TokiDatabase.kt`, `BlockDao.kt`, `EventDao.kt`; `data/entity/*`; `data/stats/StatsCalculator.kt`; `TokiApplication.kt`.
Tests: matching `app/src/androidTest/.../data/{BlockRepositoryTest,EventRepositoryTest}.kt`; `app/src/test/.../data/stats/StatsCalculatorTest.kt` (expand ... to java/com/arjunrana/tokishrine).

## Current invariants

- App/site identity belongs to exactly one block through global unique constraints. Create/update reject cross-block ownership atomically; no move/transfer path. A stale draft cannot steal a target from an ON or OFF owner. In-draft duplicates collapse. Rejected writes preserve all prior data.
- New blocks save OFF. Both create/update require at least one target and the approved redesign values: pause countdown 60..300 seconds in increments of 5, typing 100..200 in increments of 10, pause 5..100 minutes in increments of 5, and fixed disable ladders. Implementation pending in the wizard task; prior 1..1200 allowance is superseded.
- Domain canonicalization at storage boundary trims, removes one trailing dot and lowercases; package identity is case-sensitive. UI full-domain validation is a separate layer; see [blocks/targets](blocks-targets.md).
- State transition plus block_turned_on/off event commit in `setEnabledRecordingTransition`; unchanged stored value emits no transition. Internal bare setEnabled is not a replacement public activation path. UI activation gating belongs to [navigation/permissions](navigation-permissions.md).
- `completeOnboarding` transaction pairs completion event and one-way app_meta marker at most once. Preserve first_launch_at independently through insert-if-absent semantics.
- Events survive block deletion: no cascading event FK. Child target rows cascade with their block. Historical retired conflict events remain; do not emit new conflict shown/resolved events for the removed UI.
- The active event taxonomy has three uses: product-critical outcomes, feature engagement and diagnostics. `bubble_dragged` and `challenge_abandoned` are retired alongside the existing conflict/stall events: preserve historical rows, but do not emit them or include them in the Phase 7 active-event audit. Bubble dragging and nonterminal challenge suspension remain product behavior; `countdown_started`/`countdown_completed` remain active.
- Event target_type distinguishes app/site; do not infer it from dotted strings. Per-app leaderboard excludes sites; global walk-away totals include both.
- Walk-away rate = walk_away / (walk_away + challenge_completed); zero denominator yields 0. Abandonment and turnoff_completed are excluded. Local calendar week begins at start of day six days ago; days-active starts at zero on launch day; best-day buckets are local. Preserve injectable clock/test fixtures.

## Open obligations and limits

- Migration posture resolved in Phase 7: `fallbackToDestructiveMigration()` is removed and schema export is enabled (`app/schemas`, v3 baseline checked in with the Phase 7 submission). Schema v3 stands — the wizard redesign's approved reset already dropped `show_typos` — so existing v3 blocks/events are preserved and no Phase 7 migration runs; any future version bump must ship an explicit migration against the exported schema. A pre-v3 install (none exists in the field) now fails loudly instead of wiping.
- Typo positions have no user preference and appear only after an explicit failed Submit. The `show_typos` column is gone as of schema v3; nothing writes or reads it.
- Phase 3 transactional pairings have executed Room evidence in [OWNER-CHECKS](../../coordination/tasks/TS-P3-validation/OWNER-CHECKS.md): transition/event and onboarding marker/event commit together, forced event failures roll back the paired mutation, and repetitions remain at most once.
- Edit/delete OFF gating is part of UI behavior; do not overstate it as universally enforced by every repository API. Inspect the relevant path if changing this boundary.

## Approved development reset and disable duration

[Wizard task](../../coordination/tasks/TS-block-wizard-redesign/TASK.md): owner explicitly permits deleting all existing app data for this redesign; no legacy migration/mapping is required. Add a separate disable-wait seconds field (default 360; choices 180/360/720), retain typing disable characters (220/350/700), and update create/edit events and fixtures. Use a deliberate schema version change; do not introduce recurring deletion on ordinary startup. No reset has yet been executed. Prior evidence keeps its build attribution; erased fixtures are not test results. Production migration policy still needs resolution before distribution.

# Persistence, events and Stats contract

Read for data mutations, analytics/Stats, schema or repository boundary changes. Current rules distilled from PRD §§4/8–10/17 and repaired code; historical chronology is [indexed separately](../history/INDEX.md). This is not new execution evidence.

## Implementation pointers

Under `app/src/main/java/com/arjunrana/tokishrine/`: `data/repo/{BlockRepository,ChallengeRepository,EventRepository,EventTaxonomy}.kt`; `data/db/{TokiDatabase,BlockDao,EventDao}.kt`; `data/entity/*`; `data/stats/` (new [Stats contract](stats.md); StatsCalculator/StatsFormat are legacy pending screen replacement); `TokiApplication.kt`; exported schemas `app/schemas/com.arjunrana.tokishrine.data.db.TokiDatabase/{3,4}.json`.
Tests: matching `app/src/androidTest/.../data/{BlockRepositoryTest,EventRepositoryTest}.kt`; JVM `data/stats/{StatsCalculatorTest,StatsFormatTest}.kt`, `data/repo/{EventTaxonomyTest,ProductionChallengeValuesTest}.kt`, and `challenge` tests (expand ... to java/com/arjunrana/tokishrine).

## Current invariants

- App/site identity belongs to exactly one block through global unique constraints. Create/update reject cross-block ownership atomically; no move/transfer path. A stale draft cannot steal a target from an ON or OFF owner. In-draft duplicates collapse. Rejected writes preserve all prior data.
- New blocks save OFF. Both create/update require at least one target and the implemented redesign values: pause countdown 60..300 seconds in increments of 5, typing 100..200 in increments of 10, pause 5..100 minutes in increments of 5, and fixed disable ladders. Phase 7 removed the temporary debug overrides; prior 1..1200 allowance is superseded. Existing stored test values are not rewritten merely by opening the database.
- Domain canonicalization at storage boundary trims, removes one trailing dot and lowercases; package identity is case-sensitive. UI full-domain validation is a separate layer; see [blocks/targets](blocks-targets.md).
- State transition plus block_turned_on/off event commit in `setEnabledRecordingTransition`; unchanged stored value emits no transition. Internal bare setEnabled is not a replacement public activation path. UI activation gating belongs to [navigation/permissions](navigation-permissions.md).
- `completeOnboarding` transaction pairs completion event and one-way app_meta marker at most once. Preserve first_launch_at independently through insert-if-absent semantics.
- Events survive block deletion: no cascading event FK. Child target rows cascade with their block. Historical retired conflict events remain; do not emit new conflict shown/resolved events for the removed UI.
- The active event taxonomy has three uses: product-critical outcomes, feature engagement and diagnostics. `bubble_dragged` and `challenge_abandoned` are retired alongside the existing conflict/stall events: preserve historical rows, but do not emit them or include them in the Phase 7 active-event audit. Bubble dragging and nonterminal challenge suspension remain product behavior; `countdown_started`/`countdown_completed` remain active.
- Event target_type distinguishes app/site; do not infer it from dotted strings. New Stats records site outcomes against the hosting browser package (4 October §17 addendum; the raw event keeps the domain target); the raw in-the-moment global walk-away count retains both.
- Redesigned Stats uses the transactionally paired outcome ledger, frozen baseline and local usage cache under [stats.md](stats.md), not the legacy raw-event denominator. Outcomes store their local date and immutable saved contribution; counted nopes + app pause successes form resolved attempts. Preserve injectable clocks/test fixtures.
- Pause completion records `challenge_completed`; turn-off completion records `turnoff_completed` with its OFF transition, without inflating the walk-away denominator. Challenge marker/event writes live in `ChallengeRepository` transactions; bare `EventRepository.log()` is not the only event-writing path.
- Analytics remain local Room events under PRD §10. [PostHog](../plans/posthog-analytics.md) is proposed follow-up work; local persistence/Stats must remain independent of any future upload result. The local event table includes target identity and is not an approved remote payload.

## Open obligations and limits

- Schema v4 adds Stats tables with explicit MIGRATION_3_4 and preserves v3 blocks/targets/events/meta. Export both versions. No destructive fallback; no pre-v3 installs are supported. Android upgrade tests are compiled, not executed. Do not confuse the earlier v3 no-op upgrade evidence with v3→v4 proof.
- Typo positions have no user preference and appear only after an explicit failed Submit. The `show_typos` column is gone as of schema v3; nothing writes or reads it.
- Phase 3 transactional pairings have executed Room evidence in [OWNER-CHECKS](../../coordination/tasks/TS-P3-validation/OWNER-CHECKS.md): transition/event and onboarding marker/event commit together, forced event failures roll back the paired mutation, and repetitions remain at most once.
- Edit/delete OFF gating is part of UI behavior; do not overstate it as universally enforced by every repository API. Inspect the relevant path if changing this boundary.
- Previous GLM PASS WITH NOTES applies only to 89ef9e3. Stats redesign data implementation and forthcoming GLM UI require a new scoped review; owner/device and migration execution remain pending in [the task](../../coordination/tasks/TS-P7-stats-feedback-final/TASK.md).

## Historical development reset and current disable duration

The closed [wizard task](../../coordination/tasks/TS-block-wizard-redesign/TASK.md) used the owner's one-time development reset approval for schema v3, which added separate `turnoff_seconds` (default 360; choices 180/360/720) and dropped `show_typos`. Typing disable characters remain 220/350/700; create/edit writes and events use the separate fields. That approval is historical and grants no new deletion authority. Phase 7 removed the destructive fallback and exported v3; future schema bumps require explicit migrations. Prior fixtures/evidence retain their original attribution.

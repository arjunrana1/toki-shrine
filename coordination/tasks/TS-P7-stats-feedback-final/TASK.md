# TS-P7-stats-feedback-final — revised Stats and final acceptance

- **State:** `awaiting_owner` — scoped re-review of the accumulated N4 (GLM) + N6 (Claude) delta (HANDBACK deltas 2–3) recorded **PASS WITH NOTES** (P7-N8–N10, no blockers) in [REVIEW](REVIEW.md); the isolated P7-D1/D2 re-run on the N6 tree is executed — **PASS, `OK (16 tests)`** (OWNER-CHECKS) — and P7-D3/D4, P7-O1 and owner acceptance remain. Earlier: the scoped combined review of the uncommitted working-tree submission (Codex Stats data + GLM UI/permission delta) recorded **PASS WITH NOTES** (P7-N3–P7-N7, no blockers), preserved in [submissions/combined-stats-redesign-review](submissions/combined-stats-redesign-review/REVIEW.md). Phase 7 remains open through separately authorized Android execution (P7-D1–D4, real upgrade install) and owner acceptance.
- **Owner authorization:** 4 October 2026: replace open Phase 7 Stats scope, approve intake defaults, Codex implements critical data work, GLM 5.3 Flash implements UI next. One writer, manually started by Arjun.
- **Base:** HEAD `89ef9e38ec893c2ff9537dc3cb79f3bd4a4ad106` plus the uncommitted Codex Stats data submission (identified by DATA-SUBMISSION.sha256, preserved with its records in [submissions/89ef9e3-codex-stats-data](submissions/89ef9e3-codex-stats-data/HANDBACK.md)) and the uncommitted GLM UI delta on top (eight files listed in HANDBACK). Do not reset/revert Phase 6, discard untracked files, or overwrite unrelated docs-freshness edits. Preserve [previous Phase 7 evidence](submissions/89ef9e3-before-stats-redesign/TASK.md).
- **Authority/startup:** AGENTS → CURRENT → WORKFLOW → this task → [Stats contract](../../../docs/components/stats.md), PRD §9/§12/utility events, [theme](../../../docs/components/theme-ui.md), [navigation](../../../docs/components/navigation-permissions.md), [build](../../../docs/components/build-validation.md). Load deeper data source only for a concrete interface question; do not repeat product discovery.

## Next implementer — GLM 5.3 Flash

1. Replace the legacy StatsScreen presentation with the approved Your time design. Inspect [all six PNGs](../../../design/screens/stats-redesign/README.md), especially full-scroll S1, and [source layout/spec](../../../toki-shrine-ui-mockups/stats-screen/INTAKE.md). S4 is partial, not a second layout. PRD corrected copy overrides PNG pre-install/abandonment language.
2. Consume `TokiApplication.statsRepository.state`, refresh at entry/resume through existing lifecycle patterns, and call repository recalibrate only after confirmation. Use StatsDashboard/StatsDurationFormat as specified. No metric formulas, Android usage reads or persistence logic in Compose. Resolve app labels/tie order in presentation. Implement loading/error/retry, permission/no-block/zero states, nullable usage/spent values, dynamic weekday labels, charts, nested list, both info sheets and recalibration feedback.
3. Add Usage Access as the fifth onboarding/Settings permission (Better experience), real system-state progress, settings intent and resume recheck. Use AndroidUsageSource.hasAccess. **Do not change Accessibility-only ON-toggle gating.** Existing manifest declaration/main-resume/service ingestion are already implemented.
4. Wire local UI events from the Stats contract. Repository alone emits baseline_recalibrated. Preserve accepted stats_viewed recreation semantics, log once per resolved entry rather than every StateFlow emission. Do not revive retired events.
5. Preserve current Feedback, production values and existing accepted challenge/pause behavior. Remove only obsolete Stats screen-specific presentation imports after replacing them; retain legacy raw-event helpers/tests unless removal is specifically necessary (they are historical and not new metric authority).

**Allowed implementation files:** `ui/screens/StatsScreen.kt`, small Stats presentation components/utilities if useful, `MainActivity.kt` route/permission wiring only (retain Codex resume refresh), `data/permissions/AppPermission.kt` and relevant permission helpers, `PermissionChecklistScreen.kt`, `SettingsScreen.kt`, local UI/event wiring and focused corresponding tests. Task/handback/current/checklist records. No dependencies/toolchain changes.

**Fixed Codex boundary:** `data/stats/` ledger/store/parser/repository/model, database/migration/schemas, ChallengeRepository/ChallengeRuntime outcome wiring, service ingestion. If an interface defect blocks UI, describe the failing scenario and hand it back; don't redesign critical logic or change approved requirements to make the UI work.

**Checks:** required build, JVM tests and Android-test compile; focused state/render/permission wiring coverage; theme literal and manifest/dependency checks. No emulator/adb/install/screenshots/DB extraction or browser-policy workaround. Arjun owns visual/device acceptance.

**Stop:** preserve Codex HANDBACK/REVIEW before replacing (RECORDS), write your UI delta and evidence, update CURRENT to ready_for_review, supply the scoped review prompt. Do not claim Phase 7 closed or self-verification independently reviewed. No PostHog, accounts, brand/package rename, deployment or Phase 8.

## Owner change requests from review notes — 4 October 2026 (after combined review)

Verdicts on P7-N3–N7 are in OWNER-CHECKS. Two became change requests; the PRD §17 "Phase 7 owner-review addendum" is the product authority for both.

### P7-N4 minutes-only chart bar labels — GLM, done 4 October 2026

`statsBarSpecs` bar value labels render whole minutes in every range (`140m`), replacing `StatsDurationFormat.duration` in the two chart cards only; wider figures keep §9 formatting. Files: `ui/screens/StatsPresentation.kt`, one new JVM case in `StatsPresentationTest.kt` (216/0 JVM run), PRD §9 Dates/format + §17 addendum, `docs/components/stats.md`. Pending review: the combined-review PASS covered the tree without this delta.

### P7-N6 site-block savings attribute to the hosting browser — implemented by Claude (owner's choice), 4 October 2026, re-review PASS WITH NOTES

Done as HANDBACK "Claude delta 3": no schema change, 220/0 JVM run, Android tests compiled only (P7-D2 re-run needed for the changed instrumented cases). Leaderboard interpretation (all installed supported browsers while a site block is enabled) is flagged there for review/owner confirmation.


Owner decision: when a site block triggers in a browser and the user nopes out, the Stats outcome is recorded against that browser's package, so the browser appears on the blocked-apps leaderboard and its time-saved row aggregates site-block savings. This reverses the sites-excluded rule in stats.md/§9 ("A browser is an app only when the interruption targeted its package, not a URL").

Scope (Codex-owned files; same transaction/idempotency/frozen-value rules):
1. Supply the hosting browser package with site walk-aways (service ingestion knows the foreground package); record the site nope outcome against that package through the existing `StatsLedger`/`ChallengeRepository` transaction path.
2. Leaderboard source: browsers hosting enabled site blocks must appear even when not app targets themselves (§9 "App rows" wording adjusts with implementation; zero rows included, historical totals retained).
3. Update `StatsRepositoryTest.sitesAreExcludedAndStatsWriteFailureRollsBackPairedEvent` and any `StatsRedesignTest`/contract lines to the new rule — behavior change, not test weakening.
4. Prefer no schema change (P7-D1 evidence covers v4). If a v5 migration is unavoidable, export the schema, add the migration and flag that P7-D1 needs a re-run.

Settled by Arjun, 4 October 2026 (recorded in the §17 addendum and incorporated into §9 — no open questions remain):
- Site pause **push-throughs** count as attempts attributed to the browser (uniform with app challenges).
- Post-challenge **spent measurement** for a site challenge is the first completed visit to the hosting browser, under the same conservative guards (first non-Toki visit, 30s, covered history, unique claim, else unknown).
- Site nope and app nope on the **same browser package** share one five-minute dedup sequence (per-package anchor unchanged).
- **No backfill**: past site outcomes stay uncounted; site savings count only from the build implementing this.

Stop: update HANDBACK/REVIEW cycle per WORKFLOW; Arjun routes the accumulated delta (Codex N6 + GLM N4) through one scoped re-review before the attributed final build.

Paste-ready prompt for the implementing session — **Claude**, chosen by Arjun as the senior implementer (recorded switch; Codex remains the natural independent reviewer since the implementer must not review their own delta):

> Read AGENTS.md and resume TS-P7-stats-feedback-final as the senior implementer for P7-N6: site-block outcomes attribute to the hosting browser. Then read CURRENT.md, WORKFLOW.md and this task before editing. Authority: the PRD §17 "Phase 7 owner-review addendum — 4 October 2026" and §9 as incorporated (all semantics settled — push-throughs count, spent time is the first browser visit, one dedup sequence per browser package, browsers hosting enabled site blocks appear on the leaderboard, no backfill), plus the scope list and constraints in this task's P7-N6 section. You are the one writer: do not modify the GLM N4 chart-label delta (StatsPresentation/StatsPresentationTest), do not reset/revert/rebase anything, stage only your intended files. Preserve the transaction/idempotency/frozen-value rules; prefer no schema change (a v5 migration invalidates the recorded P7-D1 evidence and needs an isolated re-run). Update the sites-excluded tests to the new rule rather than weakening them. Verify with `./build.sh assembleDebug testDebugUnitTest assembleDebugAndroidTest`; no adb/device/emulator/install work. When done, append your HANDBACK section with exact files, checks and limits, set CURRENT to ready_for_review, and supply a paste-ready reviewer prompt for a different model/session. Do not commit.

## Review and acceptance after GLM

Review the new critical data changes, migration, outcome/source integration and GLM API consumption against PRD. Retain the previously requested migration/event-retirement/transactional review for the earlier `12ee572..89ef9e3` only where still relevant; no need to repeat unrelated accepted phases. Android/Room upgrade execution remains NOT VERIFIED until separately authorized in an isolated environment; Arjun owns experience-only checks. Existing non-Stats Phase 7 obligations remain in OWNER-CHECKS.

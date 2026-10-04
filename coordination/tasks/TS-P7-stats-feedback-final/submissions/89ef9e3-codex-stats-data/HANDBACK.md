# Codex handback — Stats data submission, 4 October 2026

## Attribution / next actor

- Author/role: Codex, authorized senior implementer of the Stats data boundary. Self-verification only; independent review remains pending.
- Exact base: `89ef9e38ec893c2ff9537dc3cb79f3bd4a4ad106`. Changes are **uncommitted** in this checkout. The 22 changed/new app files are identified by [DATA-SUBMISSION.sha256](DATA-SUBMISSION.sha256), manifest SHA-256 `ea3464898b2182ef13c2ac0105018a0ae2c6ae981e201fc922db56a68bc4fae3`. Verify from repo root with `shasum -a 256 -c coordination/tasks/TS-P7-stats-feedback-final/DATA-SUBMISSION.sha256` before GLM edits. The manifest is attribution, not an instruction to reset other files.
- Pre-existing docs-freshness edits, original handoff and design assets retained. Previous TASK/HANDBACK/REVIEW/OWNER-CHECKS/CURRENT preserved under [submissions/89ef9e3-before-stats-redesign](submissions/89ef9e3-before-stats-redesign/TASK.md).
- Next: Arjun starts **GLM 5.3 Flash** for the bounded presentation/permission assignment in TASK. No automatic dispatch. Continue from current Phase 7; do not revert Phase 6.

## Completed

- PRD §9 replacement and related scope, permission, local-data/privacy, analytics and asset authority. Phase 7/task/contracts/acceptance now reflect approved decisions. [Stats contract](../../../docs/components/stats.md) is the executor's fixed API/reference packet. Six owner PNGs live in [design/screens/stats-redesign](../../../design/screens/stats-redesign/README.md); original HTML/product notes retained, with override routing.
- Room v4 additive migration; exported schemas, baseline state/table, immutable per-session outcome ledger, bounded cached usage events and coverage spans. No existing rows removed/backfilled and no destructive fallback.
- ChallengeRepository pairs raw event + ledger + marker atomically. Runtime carries target/type into app pause completion. Raw walk-away counter retains app/site and repeated-event semantics; new Stats dedup applies only to explicit app nopes. Passes always count and reset the dedup sequence; suspensions/turn-offs/sites remain excluded.
- Frozen recent-available baseline, <3-visit/unknown-package fallback, immutable saved contributions, confirmed recalibration API/success event, regrant preservation.
- Android usage adapter, package/activity visit reconstruction, durable overlap cache, conservative first-visit attribution with unique visit claims. Missing measurement remains null. Existing service ingests while alive, main resume refreshes, and no OS usage query enters the blocking transaction.
- Fixed StateFlow screen model, seven-date calculator, raw-duration aggregation/formatting, explicit permission/empty/loading/error/unknown states. Five new local Stats event names added to taxonomy; UI emitters belong to GLM. Existing screen is deliberately still the old UI until that assignment.

## Fresh evidence

| Check | Result | Limits |
|---|---|---|
| `./build.sh assembleDebug testDebugUnitTest assembleDebugAndroidTest` | PASS, final run 4 October; 207 tests, 0 failures/errors/skips | Build + JVM execution + Android-test **compilation only**; XML: `app/build/test-results/testDebugUnitTest/` |
| StatsRedesignTest (13 JVM cases) | PASS | Dedup boundary/pass reset, resolved denominator/history/filtering, null versus zero, format/delta thresholds, activity transitions, duplicate/orphan events, incomplete tail, first return, coverage gaps and DST |
| New StatsMigrationTest, StatsRepositoryTest; expanded ChallengeRepositoryTest | COMPILED | Android execution NOT VERIFIED. Exported v3 fixture/real migration/reopen, transactional rollback, baseline/fallback/regrant/recalibration, event target propagation and unique visit claims await isolated execution |
| `python3 coordination/tasks/TS-P7-stats-feedback-final/checks/verify-migration-sql.py` | PASS | Host SQLite compares migration table/index/FK metadata with exported Room v4; all exported v3 entities unchanged. **Not Android Room upgrade proof** |
| `git diff --check`; scoped local-link check | PASS | URL-encoded existing image links retained; no new broken local links |
| Manifest / dependency audit | PASS | Added only PACKAGE_USAGE_STATS permission; no INTERNET. Gradle adds schema assets for migration test only, no dependencies/toolchain change. No visual palette changes |

The first compile exposed a non-public UsageEvents instance-ID accessor; removed it and used public package/class/type/timestamp data. The initial Gradle cache write was sandbox-blocked; the approved build execution used the existing cache. Final checks above passed after repairs. No browser workaround, device operation, install or instrumentation run occurred.

## Remaining / limits

- GLM screen, five-row permission checklist/Settings, local UI event emitters and screen interaction tests are not implemented here. Old StatsCalculator/EventRepository.getStats remain legacy until GLM replaces the screen consumer; they are not the new metric contract.
- Android raw-event retention/OEM completeness is finite. Details and conservative one-second activity merge / 30-second return-attribution guard are in stats.md. Unknown visits are not charged as zero or guessed. Screen average needs full seven-date coverage; baseline may use fewer complete usable days. No promise of recovering seven pre-install days.
- Independent combined review, Android migration/transaction execution, real usage adapter behavior and Arjun's visual/experience acceptance remain open. Previous review applies only to 89ef9e3. Existing Feedback/final-pass obligations remain.
- No PostHog, network, accounts, package/name change, destructive reset or next-phase implementation.

Before GLM replaces this handback, preserve it, REVIEW and the manifest as the Codex data submission per RECORDS. Then hand back only the new UI delta and evidence, with CURRENT set ready_for_review.

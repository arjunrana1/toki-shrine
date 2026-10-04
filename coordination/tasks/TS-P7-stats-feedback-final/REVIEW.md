# REVIEW — accumulated N4 + N6 delta (scoped re-review, 4 October 2026)

## Verdict

**PASS WITH NOTES** for the exact accumulated uncommitted delta on top of the PASS-WITH-NOTES combined Stats tree (base HEAD `89ef9e3` + Codex data submission + GLM delta 1):

- **GLM delta 2 — P7-N4** minutes-only chart labels: `StatsPresentation.kt` (SHA-256 `54a8a443…`), `StatsPresentationTest.kt` (`034e665d…`), PRD §9/§17, `docs/components/stats.md`.
- **Claude delta 3 — P7-N6** site outcomes attribute to the hosting browser: the 15 files in [N6-SUBMISSION.sha256](N6-SUBMISSION.sha256).

No blockers. Code review plus one non-device JVM re-run only; **the changed/added instrumented cases, real device usage behavior, the attributed final build and owner acceptance remain NOT VERIFIED** (list at the end).

Reviewer: Claude (Opus 5.5), a different session from the N6 implementer but **the same model**. The N4 portion is fully independent (GLM author). For N6 the owner chose this session; if strict model independence matters, a short Codex cross-check of N6 can be added. This review does not cover that.

The previous combined review is preserved unchanged in [submissions/combined-stats-redesign-review/REVIEW.md](submissions/combined-stats-redesign-review/REVIEW.md). Notes P7-N3–N7 and the owner verdicts on them stand.

## Submission identity (reviewer-verified)

- `shasum -a 256 -c N6-SUBMISSION.sha256` → 15/15 OK.
- `shasum -c submissions/pre-n6-claude/PRE-N6-APP.sha256` → exactly 9 mismatches. These are the 9 N6 app files that were already modified before N6. The other 4 N6 app files (BlockActivity, DetectionEngine, DetectionEngineTest, ChallengeRuntimeTest) were clean at HEAD and are diffed against git. 33 modified/untracked `app/` paths = 29 pre-N6 + those 4. Nothing else in `app/` changed.
- No schema change: `TokiDatabase.kt` `ca6791e1…`, `StatsStore.kt` `3626a387…`, `schemas/…/4.json` `e8eae786…` all equal PRE-N6. Room stays at v4, so the recorded P7-D1 migration evidence still applies. No dependency, toolchain, manifest or raw-event payload change.
- N4 file hashes match the values recorded in HANDBACK delta 3 (N6 left them untouched).

## What was reviewed (authority: PRD §17 "Phase 7 owner-review addendum — 4 October 2026", §9 as incorporated)

**P7-N4.** `barValueLabel` gives whole minutes using the same `roundToLong` nearest-minute rounding as `StatsDurationFormat.duration`, so 99.5m → `100m` in both. Unknown values stay "—" and zero stays `0m`. Bar heights and stubs are unchanged, and only the two chart cards (`statsBarSpecs`) use it. The hero, tiles, totals and app rows keep the §9 format. `heavyDayBarLabelsStayMinutesOnly` pins 140m/100m and the rounding boundary. PRD §9/§17 and stats.md agree. Visual fit on a heavy day remains P7-S6/P7-O21.

**P7-N6: host propagation.**
- `DetectionTrigger.hostPackage` is a required parameter, so no construction site can omit it. An app trigger uses its own package. A site trigger uses `PendingSettle.packageName`, which `onSettleElapsed` has already validated equals the active window's package and the window's package at fire time. The service passes it as `EXTRA_HOST_PACKAGE`.
- `BlockActivity.resolveSession` reads the extra each time it resolves and passes it into `ChallengeConfig` and the deferred pending-walk-away `WalkAway`. `WalkAway` and `CompletionRequest` (including `resumePendingCompletion`) take it from the config, and `handle(WalkAway)` forwards it to `recordWalkAwayAndCount`.
- Recreation re-reads the system-retained intent, so the host survives. `onNewIntent` replaces the intent only when no unfinished challenge, pending walk-away or walk-away card owns the activity, so a relaunch cannot switch a live session's host. `completePause` and `recordWalkAwayAndCount` are the only ledger callers, and both pass the host.
- A legacy or hand-made intent without the extra records the raw event but no Stats outcome. This is conservative and consistent with no backfill.

**P7-N6: ledger, transaction, idempotency and frozen values.**
- `StatsLedger.statsPackage` maps: app → target, site → host, anything else or blank → no outcome. A site outcome is never recorded under its domain.
- The `record` body is otherwise unchanged. It runs inside the existing challenge Room transaction, after the raw event and before the walk-away marker or `markCompleted`. The session-ID outcome precheck plus the marker/`completionExists` guards keep duplicates as no-ops.
- `savedMs` and `baselineId` are frozen at resolution, and pre-capture rows keep the fallback value with a null baseline.

**P7-N6: shared dedup sequence.** `lastCounted(pkg)` uses the resolved browser package. Site and app nopes on one browser therefore share a single fixed five-minute anchor, and a pass ends the sequence, as §9 requires. This is pinned on the JVM (package rule), in StatsRepositoryTest (site nope then app nope on the same browser → counted/ignored) and in ChallengeRepositoryTest (real repository path; idempotent duplicate completion; raw counter stays inclusive at 1/2/3; raw events keep the domain target).

**P7-N6: leaderboard.**
- `leaderboardPackages` = app targets of enabled blocks, plus the bundled-config supported browsers while any enabled block has a site, then intersected with installed packages.
- The browser source is the same `DetectionConfigLoader` map the service uses for address-bar reading. Every site outcome therefore comes from a package in that set.
- It is resolved outside the Room transaction. A config failure degrades to app rows only, and cancellation is rethrown.
- Removing or disabling every site block drops the browser rows while the totals are kept, matching §9 ("browsers with no enabled site block disappear … historical totals remain"). A browser that is also an app target appears once.
- The "hosting browsers" interpretation is a reasonable reading of §9: site blocks are enforced in every supported browser, so each one hosts them. See P7-N8 for owner confirmation.

**P7-N6: first-browser-visit spent.** `firstPostChallenge` is unchanged and runs against `outcome.packageName` (now the browser). The guards still apply: first non-Toki completed visit, within 30s, covered history, unique claim, otherwise unknown. `enabledSiteBlockShowsHostingBrowserRow…` pins 7s attributed to the site pass. Toki's own visits are skipped, so Toki surfacing first does not by itself make the visit unknown; the 30s limit does. See P7-N9.

**No backfill.** There is no migration and no historical scan. Ledger rows are written only from live terminal paths.

**Replaced test was updated, not weakened.** `sitesAreExcluded…` became `siteNopesAttributeToHostingBrowserShareItsSequenceAndStatsWriteFailureRollsBackPairedEvent`:
- It now asserts the new positive behavior: browser package, counted/ignored order, fallback/zero saved values.
- It keeps the old negative guard: a host-less site writes no row and nothing is recorded under the domain.
- It keeps the forced-failure rollback that pairs the raw event with the Stats row, and re-checks after the trigger is dropped. The final count rising from 1 to 3 follows from the two new rows.

The contract docs (stats.md, persistence-events.md) replace the exclusion rule consistently.

## Independent check run by this review

- `./build.sh testDebugUnitTest --rerun-tasks` → BUILD SUCCESSFUL, **220 JVM tests, 0 failures/errors/skips** (XML under `app/build/test-results/testDebugUnitTest/`). This matches the builder's 220 on this exact tree.
- Checksum, scope and schema-identity verification as above.
- Android-test compilation was not re-run. The builder's `assembleDebugAndroidTest` success is the attributed evidence, and the changed sources are unchanged since (15/15 OK).

## Notes (non-blocking; IDs continue the task sequence)

- **P7-N8 — leaderboard interpretation (owner confirmation).** While any enabled block has a site, every installed supported browser shows a row, including a 0m row for an unused browser. The alternative, "only browsers with a site outcome", is a one-line change in `StatsRepository.leaderboardPackages`. Confirm during P7-S acceptance. No code change requested.
- **P7-N9 — site push-throughs can make spent unknown more often.**
  - A site pass now enters the spent measurement, but the site completion path still uses a plain `finish()`. That navigation was accepted in Phases 5/6. BlockActivity runs in Toki's task, so finishing can reveal Toki rather than the browser.
  - If the user does not reach the browser within 30s, the pass stays unmeasured. The day's and week's spent then shows "—" with "Some visits couldn't be measured". This is conservative and §9-correct (never 0), but could become a frequent owner-visible state.
  - Observe in P7-D3 and P7-S acceptance. A possible later fix is to return to `hostPackage` on site completion. That is a navigation change and needs an owner decision; it is not in N6 scope.
- **P7-N10 — site nope valuation.** A counted site nope is saved at the browser's whole-app usual visit length, or the 10-minute fallback when there are fewer than 3 visits or no baseline. This is not specific to the site. It follows directly from §9's per-package baseline now that the browser is the Stats app. Record only.

## Remaining Android-execution and owner evidence (NOT VERIFIED; do not close Phase 7 without them)

1. **P7-D2 re-run** in the isolated environment (AVD `toki-p7d-isolated` can be reused) for the N6-changed instrumented sources:
   - `StatsRepositoryTest`: 6 cases, including the replaced `siteNopesAttribute…` case and the new `enabledSiteBlockShowsHostingBrowserRow…`.
   - `ChallengeRepositoryTest`: 9 cases, including the new `siteWalkAwayAndPushThroughAttribute…`.

   The recorded `OK (14 tests)` predates N6. P7-D1 (`StatsMigrationTest`) does not need a re-run because there is no schema change, but running it together is cheap.
2. **P7-D3**: real `AndroidUsageSource`/OEM behavior, now also covering browser visits after a site challenge (P7-N9) and the browser row on the real device.
3. **P7-D4**: on-device active-event payload / once-per-entry audit. Raw site events must keep the domain target.
4. **P7-O1**: the separately attributed final build, installed over the existing v3 install without uninstall or clear.
5. **Arjun's experience/visual acceptance**: P7-S1–S10, including the heavy-day minutes-only labels (S6/O21) and the N6 browser-row behavior (P7-N8/N9 observations), plus the retained checks in [OWNER-CHECKS](OWNER-CHECKS.md).

## Next

No code changes requested. Arjun decides: authorize the isolated P7-D2 re-run (with D1) and the attributed final build for P7-O1/D3/D4 and experience acceptance, or optionally route a Codex cross-check of N6 first. Phase 7 stays open through the evidence above, then commit and close.

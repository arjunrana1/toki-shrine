# GLM handback — Stats UI redesign submission, 4 October 2026

## Attribution / next actor

- Author/role: GLM 5.3 Flash, the bounded Stats presentation/permission assignment in [TASK](TASK.md). Self-verification only; independent review remains pending. Arjun owns visual/device acceptance.
- Exact base: HEAD `89ef9e38ec893c2ff9537dc3cb79f3bd4a4ad106` **plus two uncommitted layers in this working tree**: (1) the Codex Stats data submission identified by [DATA-SUBMISSION.sha256](DATA-SUBMISSION.sha256) — re-verified OK immediately before GLM edits, and preserved with its REVIEW and manifest in [submissions/89ef9e3-codex-stats-data](submissions/89ef9e3-codex-stats-data/HANDBACK.md); (2) the GLM UI delta below, uncommitted on top. The GLM delta touches exactly these eight files and nothing else:
  - `app/src/main/java/com/arjunrana/tokishrine/ui/screens/StatsScreen.kt` (replaced presentation)
  - `app/src/main/java/com/arjunrana/tokishrine/ui/screens/StatsPresentation.kt` (new pure display mapping)
  - `app/src/main/java/com/arjunrana/tokishrine/ui/screens/PermissionChecklistScreen.kt`
  - `app/src/main/java/com/arjunrana/tokishrine/ui/screens/SettingsScreen.kt`
  - `app/src/main/java/com/arjunrana/tokishrine/data/permissions/AppPermission.kt`
  - `app/src/main/java/com/arjunrana/tokishrine/MainActivity.kt`
  - `app/src/test/java/com/arjunrana/tokishrine/AndroidManifestTest.kt`
  - `app/src/test/java/com/arjunrana/tokishrine/ui/screens/StatsPresentationTest.kt` (new)
- Codex-owned files (`data/stats/`, database/migration, Challenge wiring, service ingestion, `TokiDatabase`, schemas, manifest) were not modified. No `data/stats/` interface defect was hit; no escalation was needed. No dependency or toolchain change.
- Next: Arjun starts the **scoped combined review** (prompt below). No automatic dispatch; no device work authorized.

## What was implemented

1. **Screen 23 "Your time" replacement** (`StatsScreen.kt` + new `StatsPresentation.kt`): the approved S1–S6 design from the six owner PNGs and the HTML spec, with PRD §9 copy overriding the exports. Collects `TokiApplication.statsRepository.state`; renders Loading (blank content), Error (retry, no fake zeros), NoUsageAccess (S5 card + `ACTION_USAGE_ACCESS_SETTINGS` CTA), NoBlocks (S6) and Ready (hero with 68sp figure and `/ day`, saved-so-far, screen-time line with hidden ≤1%/unknown delta, attempts/nope-rate tiles with em-dash-and-empty-bar on zero attempts, nested ~300dp app list with overflow fade and display-label tie order, two seven-bar chart cards with stub bars for zero/unknown days, today bar + glow, recalibration card). Both info sheets (S2 with corrected §9 copy and formula chips, S3 with resolved counts and "No tries yet" on zero attempts) support Got it, scrim tap and swipe-down dismissal. Recalibration logs `stats_recalibrate_tapped`, confirms, disables repeat taps, calls repository `recalibrate()` and reports failure inline without touching the baseline; `baseline_recalibrated` is emitted by the repository only. Loading renders nothing and Error offers retry per §9; `usageAvailable == false` shows the neutral "Usage data isn't available right now" note; unknown spent values show "—" and "Some visits couldn't be measured"; screen percent 0 is hidden.
2. **Fifth permission row**: `USAGE_ACCESS` added to `AppPermission` ("Usage access — Measures app foreground time for Stats; never app contents", Better experience group) with `Permissions.isGranted` reading `AndroidUsageSource.hasAccess()`. Checklist and Settings show real-state n-of-5 progress; the checklist request dispatch opens Usage Access settings through the established mark-pending → record → launch → settle-on-resume path. Accessibility-only ON-toggle gating is untouched. Manifest declaration already present.
3. **Local UI events** per the Stats contract/PRD §10: `stats_viewed` (`state` = default/partial/no_access/no_blocks) logged once per entry after the state resolves, never per StateFlow emission, recreation allowed per the accepted `settings_viewed` convention; `stats_info_opened` (`which` = time_saved_hero/time_saved_list/nope_rate) from the three info targets; `stats_usage_access_cta_tapped` from S5; `stats_recalibrate_tapped` before confirmation; `usage_access_granted` logged once per false→true transition at MainActivity resume, covering both checklist and Stats-CTA grants. No retired events revived.
4. **Presentation mapping pinned on the JVM** (`StatsPresentation.kt`): app rows (saved desc, display-label ties, `~{n}m per visit` with the 1m floor), rolling narrow weekday letters with Today last (fixed English locale to match the app's fixed-English copy), hero screen-time line hide rules, `stats_viewed` partial/default classification and bar geometry (proportional to 74dp max, 4dp floor, 3dp stubs).

## Fresh evidence

| Check | Result | Limits |
|---|---|---|
| `./build.sh assembleDebug testDebugUnitTest assembleDebugAndroidTest` | PASS, final run 4 October after all edits; 215 JVM tests, 0 failures/errors/skips; XML under `app/build/test-results/testDebugUnitTest/` | Build + JVM execution + Android-test **compilation only**. 207 pre-existing (Codex) + 8 new (7 `StatsPresentationTest`, 1 manifest guard) |
| `StatsPresentationTest` (7 JVM cases) | PASS | Row order/tie/labels, weekday letters, screen-time hide rules, viewed-state classification, bar geometry incl. 4dp floor/3dp stubs/em dash |
| New `usageAccessPermissionIsDeclared` in `AndroidManifestTest` | PASS | Source-manifest guard only; runtime grant behavior is owner/device evidence |
| Theme literal check (hex outside `ui/theme`) | PASS | Only two pre-existing comment mentions in unrelated files; no new literals, no palette/typography/dependency changes |
| `git diff --check` | PASS | No whitespace errors |
| `shasum -a 256 -c DATA-SUBMISSION.sha256` (before GLM edits) | PASS all 22 files | Codex attribution intact; Codex files untouched since |

No device, emulator, adb, install, screenshot, DB extraction or instrumented execution occurred.

## Notes for review / known limits

- **Manifest hash prose discrepancy (Codex record):** the preserved Codex HANDBACK prose states manifest SHA-256 `ea346489…`, but the actual `DATA-SUBMISSION.sha256` manifest entry is `8b833784…` and that checksum file verified cleanly before GLM edits (the manifest carries Codex's uncommitted `PACKAGE_USAGE_STATS` addition). GLM did not modify the manifest; the snapshot under `submissions/89ef9e3-codex-stats-data/` is byte-identical to the working tree. Treat the checksum file as authoritative; the prose value appears stale.
- **`stats_viewed` partial/default classification** uses `baselineCapturedAt` against the seven-day window start: null or in-window → "partial", fully covered week → "default". A confirmed recalibration rewrites `capturedAt`, so a recalibrated week can read "partial" until the window rolls past it — a conservative approximation of the S4 notion, documented here rather than silently invented.
- **`stats_viewed` is not logged** when the state resolves to `Error` (§10 defines no error state); the next resolved state (e.g., Ready after retry) logs once.
- **Visual approximations to flag for owner acceptance:** info glyphs use the bundled `Ph.Info` (no `ph-question` glyph exists in the carried Phosphor subset) inside 44dp named button targets (accessibility convention; mock draws bare 13–15px glyphs); the hero figure renders Inter regular because the mock's 300 weight is not bundled (only regular/medium are carried); the today-bar glow uses a blurred accent underlay approximating the CSS `box-shadow`; sheet swipe dismissal is a plain drag threshold without fling velocity.
- **Site-only blocks** produce a Ready dashboard with an empty blocked-apps section (the §9 "include zero rows" rule taken literally); no special empty-rows state is defined in the contract.
- Legacy `EventRepository.getStats`/`StatsSnapshot`/`StatsFormat` and their tests are retained untouched as historical helpers, per TASK; they are not the new metric authority.
- Unchanged open obligations: Android/Room upgrade execution, real usage-adapter behavior and the combined independent review remain NOT VERIFIED; previous P7 obligations in OWNER-CHECKS stand; previous PASS applies only to `12ee572..89ef9e3`.

## Paste-ready next-role prompt

> Read AGENTS.md and resume TS-P7-stats-feedback-final as the scoped reviewer. Review the combined uncommitted delta on base `89ef9e3` recorded in HANDBACK.md — Codex's Stats data submission (DATA-SUBMISSION.sha256) plus the GLM Stats UI/permission delta — against PRD §9/§10/§12, docs/components/stats.md, theme-ui and navigation-permissions, focusing on schema migration/data preservation, transactional outcome accounting, dedup, baseline immutability, conservative usage parsing/attribution and the UI's consumption of null/zero/permission states and its §10 event wiring. Preserve the recorded snapshots under submissions/. Write REVIEW, set CURRENT, and list any Android-execution evidence still needed.

## GLM delta 2 — P7-N4 minutes-only chart bar labels, 4 October 2026 (later session)

Owner decision on review note P7-N4. On top of delta 1 (same base `89ef9e3` + Codex data submission + GLM delta 1), uncommitted:

- `ui/screens/StatsPresentation.kt`: `statsBarSpecs` bar value labels now use a private minutes-only formatter (`140m` in every range, same nearest-minute rounding); `StatsDurationFormat.duration` remains for hero/tiles/totals/app rows. Unknown stays "—", zero stays "0m", heights unchanged.
- `app/src/test/java/com/arjunrana/tokishrine/ui/screens/StatsPresentationTest.kt`: new `heavyDayBarLabelsStayMinutesOnly` case ("140m"/"100m"/"100m", including the 99.5m rounding boundary).
- `PRD.md`: §9 Dates/format incorporates the exception; new §17 "Phase 7 owner-review addendum — 4 October 2026" records all five note verdicts and both decisions.
- `docs/components/stats.md`: chart-label rule added to the GLM API bullet; pending N6 site-attribution change flagged in the Codex-owned paragraph.

Checks: `./build.sh assembleDebug testDebugUnitTest assembleDebugAndroidTest` → BUILD SUCCESSFUL, **216 JVM tests, 0 failures/errors/skips** (delta 1 was 215; +1 new). No device/emulator work in this delta. Limits: pending review (the combined-review PASS predates this delta); visual confirmation of label fit on a heavy day lands in S6/O21 on the final build. The same session also executed the authorized P7-D1/P7-D2 isolated-emulator run (recorded in OWNER-CHECKS) before this change; that evidence covers the pre-N4 tree's data layer, which this delta does not touch.

## Claude delta 3 — P7-N6 site-block outcomes attribute to the hosting browser, 4 October 2026

- **Author/role:** Claude (Opus 5.5), senior implementer by owner's choice (TASK P7-N6). Self-verification only; independent review pending — Codex (different model/session) is the natural reviewer.
- **Exact base:** HEAD `89ef9e3` + Codex data submission + GLM delta 1 + GLM delta 2 (N4), all uncommitted. Pre-edit state identified by [PRE-N6-APP.sha256](submissions/pre-n6-claude/PRE-N6-APP.sha256) (all 29 modified/untracked `app/` files) with byte copies of every pre-edit file touched under [submissions/pre-n6-claude/files/](submissions/pre-n6-claude/files/); `diff -u submissions/pre-n6-claude/files/<path> <path>` shows this delta exactly (BlockActivity, DetectionEngine, DetectionEngineTest and ChallengeRuntimeTest were clean at HEAD, so `git diff HEAD` also shows them). Post-delta state: [N6-SUBMISSION.sha256](N6-SUBMISSION.sha256) (15 files, verifies OK). GLM N4 files `StatsPresentation.kt`/`StatsPresentationTest.kt` untouched (SHA-256 `54a8a443…`/`034e665d…`, unchanged from session start). The only pre-existing doc line replaced outside the snapshot is `persistence-events.md` "New Stats excludes site outcomes entirely; …" (now rewritten for N6).
- **No schema change:** `TokiDatabase.kt`, `StatsStore.kt` and `schemas/…/4.json` hashes equal PRE-N6 (`ca6791e1…`, `3626a387…`, `e8eae786…`); v4 stands, so P7-D1 migration evidence still applies. No dependency/toolchain/manifest change; no raw-event payload change.

### Files (15)

| File | Change |
|---|---|
| `detection/DetectionEngine.kt` | `DetectionTrigger.hostPackage` (required): app trigger = its package; site trigger = `PendingSettle.packageName` (the settled, still-foreground browser window). |
| `TokiAccessibilityService.kt` | Passes `BlockActivity.EXTRA_HOST_PACKAGE`. |
| `BlockActivity.kt` | Reads the extra (recreation/`onNewIntent` re-read the intent as before) into `ChallengeConfig`, the deferred pending-walk-away effect, and `recordWalkAwayAndCount`; new constant; comment on site plain-finish corrected. Navigation unchanged. |
| `challenge/ChallengeRuntime.kt` | `hostPackage` on `ChallengeConfig`, `WalkAway`, `CompletionRequest` (also on `resumePendingCompletion`). |
| `data/repo/ChallengeRepository.kt` | `recordWalkAwayAndCount(…, hostPackage = null)` and `completePause` pass the host into the ledger inside the existing transaction, before the idempotency marker — ordering unchanged. |
| `data/stats/StatsLedger.kt` | `record(…, hostPackage, …)`; package from `statsPackage(target, type, host)`: app → target, site → host, else/blank → no outcome. Dedup/frozen saved value/baseline ID logic unchanged, so site and app outcomes on one browser share `lastCounted(pkg)`, i.e. one sequence; passes break it. |
| `data/stats/StatsRepository.kt` | Optional `siteHostPackages` source (resolved outside the Room transaction); `leaderboardPackages(enabledBlocks, siteHosts)` = enabled app targets + every supported browser while any enabled block has a site, then ∩ installed. |
| `TokiApplication.kt` | Supplies `detectionConfigLoader.load().browsers.keys`; config failure → empty set (cancellation rethrown), matching the service's app-only degradation. |
| `DetectionEngineTest.kt` | Helpers carry host; new `siteTriggerCarriesTheHostingBrowserPackage` (Firefox window). |
| `ChallengeRuntimeTest.kt` | New `siteSessionCarriesHostingBrowserIntoWalkAwayAndCompletion`. |
| `StatsRedesignTest.kt` | New `siteOutcomesBelongToTheHostingBrowserPackage`, `enabledSiteBlocksAddHostingBrowsersToTheLeaderboard`. |
| `StatsRepositoryTest.kt` (androidTest) | `sitesAreExcluded…` **replaced** by `siteNopesAttributeToHostingBrowserShareItsSequenceAndStatsWriteFailureRollsBackPairedEvent` (site nope → browser row with fallback saving; app nope on the same browser within 5 min ignored; host-less site → no row; forced-failure rollback retained, final count 1 → 3). New `enabledSiteBlockShowsHostingBrowserRowWithSiteSavingsAndFirstBrowserVisitSpent` (installed host browser row, uninstalled host absent, site pass spent = first browser visit 7s, disabling the site block drops the row while the 7-day total is retained). |
| `ChallengeRepositoryTest.kt` (androidTest) | New `siteWalkAwayAndPushThroughAttributeToHostingBrowserInTheChallengeTransaction` (real repository path, shared sequence, idempotent completion, inclusive raw counter 1/2/3, raw events keep the domain target). |
| `docs/components/stats.md`, `docs/components/persistence-events.md` | Site-exclusion rule replaced by the N6 contract; evidence-gate line. PRD §9/§17 already incorporated the decisions — not edited. |

### Checks

| Check | Result | Limits |
|---|---|---|
| `./build.sh assembleDebug testDebugUnitTest assembleDebugAndroidTest` (final run after all edits; `compileDebugKotlin` + `testDebugUnitTest` executed) | BUILD SUCCESSFUL; **220 JVM tests, 0 failures/errors/skips** (216 + 4 new) | Android-test **compilation only** |
| `git diff --check` | PASS | — |
| `shasum -c N6-SUBMISSION.sha256` | OK (15/15) | — |
| N4/schema file hashes vs session start / PRE-N6 | unchanged | — |

No adb, device, emulator, install, screenshot or DB extraction.

### Interpretation and limits for review

- **"Browsers hosting enabled site blocks"** is read as: every *installed supported* browser (bundled detection config) whenever any enabled block contains a site, zero rows included — site blocks are enforced in every supported browser, so each hosts them. An unused installed supported browser therefore shows a 0m row. Owner may prefer "only browsers with a site outcome"; that would be a one-line change in `leaderboardPackages`.
- **Spent measurement for site passes** reuses `firstPostChallenge` unchanged against the browser package. The site completion path still only `finish()`es (accepted Phase 5/6 navigation, not changed); if Toki's own task or another app surfaces first, or the browser returns after 30s, spent stays unknown (conservative, never 0).
- A site outcome with no host package (should not occur from detection; only a hand-crafted/legacy intent) records the raw event but no Stats outcome.
- **P7-D2 evidence is now partial:** the recorded `OK (14 tests)` covers the pre-N6 StatsRepositoryTest/ChallengeRepositoryTest. This delta changes one and adds three instrumented cases (now StatsRepositoryTest 6, ChallengeRepositoryTest 9) — compiled, **not executed**; an isolated re-run (reusing AVD `toki-p7d-isolated`) is needed if Arjun wants N6 instrumented evidence. P7-D1 (migration) is unaffected.
- Device behavior (real browser foreground/usage events, OEM) remains P7-D3/owner acceptance; the attributed final build is not yet produced.

### Paste-ready reviewer prompt (different model/session — Codex recommended)

> Read AGENTS.md and resume TS-P7-stats-feedback-final as the scoped reviewer. Review the accumulated uncommitted delta recorded in HANDBACK: GLM delta 2 (P7-N4 minutes-only chart labels: StatsPresentation/StatsPresentationTest, PRD §9/§17, stats.md) and Claude delta 3 (P7-N6 site outcomes attribute to the hosting browser; diff each file against submissions/pre-n6-claude/files/ or git HEAD; post-state in N6-SUBMISSION.sha256). Authority: PRD §17 "Phase 7 owner-review addendum — 4 October 2026" and §9. Focus: host-package propagation detection → BlockActivity → runtime → ChallengeRepository (including recreation/onNewIntent/pending walk-away), ledger transaction/idempotency/frozen-value preservation, the shared per-package dedup sequence, leaderboard source and its "hosting browsers" interpretation, first-browser-visit spent measurement, no schema change/no backfill, and that the replaced sites-excluded test was updated, not weakened. Do not re-review the PASS-WITH-NOTES tree beyond affected dependencies. Run non-device checks only for concrete open questions; no adb/device/emulator. Write REVIEW with a verdict for this exact submission, set CURRENT, and list remaining Android-execution evidence (P7-D2 re-run for the changed instrumented tests, P7-D3/D4, P7-O1).

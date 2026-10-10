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

## GLM delta 4 — temporary owner-test override (easy challenges), 7 October 2026

- **Author/role:** GLM 5.3 (ZCode), owner → GLM direct correction path (Arjun's instruction this session: "make testing easier… just a temporary change while we test the product out… log it, and rebuild and install"). Self-verification only; the delta is mechanical value replacement plus one copy fix, recorded for revert.
- **Exact base:** HEAD `70bd8e8` (Arjun's committed Phase 7 Stats redesign) — the working tree was clean under `app/` before this delta. Delta = `git diff 70bd8e8 -- app/`: exactly five files, no schema/migration/dependency change (v4 stands; P7-D evidence unaffected — these constants are not part of any migration).

### Values (temporary; revert restores §7/§17 production)

| Value | Production | Override |
|---|---|---|
| Pause typing min/default (max 200, step 10 unchanged) | 100 / 150 | **20 / 20** |
| Pause wait min/default (max 300, step 5 unchanged) | 60 / 60 | **10 / 10** |
| Disable typing ladder / default | 220/350/700 (middle) / 350 | **single rung 20** |
| Disable wait ladder / default | 180/360/720 (middle) / 360 | **single rung 10** |
| Pause minutes (unchanged) | 5–100 step 5, default 15 | same |

### Files (5)

- `data/repo/BlockRepository.kt` — the constants above; header comment records the override, its date and the revert target.
- `ui/screens/CreateFlowScreen.kt` — `disableCardCopy` wait body now seconds-aware (`Wait for 10 seconds.` instead of `value / 60` → "0 minutes"); correct for the production rungs too after revert.
- `data/repo/ProductionChallengeValuesTest.kt` (JVM) — guard test now pins the override values; header documents the exact production assertions to restore on revert (same loud-guard purpose, assertions equally strict).
- `ui/screens/CreateFlowStateTest.kt` (JVM) — defaults/bounds/Reset/switch/ladder/draft-mapping cases updated to the override; ladder cases adapted to the single rung (preselection index 0; per-method values still asserted); renames drop stale "HundredTo…"/"MiddleChoice" wording. 220 JVM tests total — same count, none deleted.
- `androidTest/data/BlockRepositoryTest.kt` — valid-draft fixtures and paired assertions moved to the override rungs (350/360/720 → 20/10/10) so the instrumented suite stays executable; rejection cases still reject (300 > 200; 30 ∉ {20}; 120/200/3600 ∉ {10}); stale 23-September override comment refreshed.

### Checks

| Check | Result | Limits |
|---|---|---|
| `./build.sh assembleDebug testDebugUnitTest assembleDebugAndroidTest` | BUILD SUCCESSFUL; **220 JVM tests, 0 failures/errors/skips** (XML re-read) | Android-test **compilation only** |
| `git diff 70bd8e8 --stat -- app/` | 5 files, +100/−91 | Attribution for this delta |

No adb/device work in the delta itself; the rebuild+install Arjun requested is recorded separately in OWNER-CHECKS/CURRENT.

### Limits and revert

- **Temporary by owner instruction.** Blocks created while this build is installed store the easy values (20 chars / 10 s) and keep them after revert until re-saved; blocks created before the override keep their stored (harder) values until re-saved — the disable editor shows their stored value unselected and requires picking a rung on save (established P7-O15 pattern, now in reverse).
- **P7-O14 (production bounds in debug) is temporarily superseded** on override builds; it re-applies on revert. P7-O15 keeps its meaning (stored blocks remain valid; re-saves move into the active ranges).
- Single-rung ladders remove the disable choice step's recommendation badge (index 0 ≠ 1) — cosmetic only.
- **Revert:** restore the five files' values from `70bd8e8` (or follow the revert block in `ProductionChallengeValuesTest`), re-run the same three build tasks (guard test fails loudly otherwise), and re-attribute any further owner acceptance to the reverted build. The N6 final build (`46877f25…`) remains the attribution for production-value acceptance.

### Revert — 7 October 2026, same day, at Arjun's instruction ("This is incorrect")

`git checkout HEAD -- app/` restored all five files to `70bd8e8` (`git diff HEAD -- app/` empty; N6-SUBMISSION.sha256 re-verified OK 15/15 — no source trace of the override remains). Re-run on the reverted tree: `./build.sh assembleDebug testDebugUnitTest assembleDebugAndroidTest` → BUILD SUCCESSFUL, **220 JVM tests, 0 failures** (production guard restored from HEAD). Fresh APK `a93899d4…` (source-identical to `70bd8e8`; hash differs from the 5 October `46877f25…` through packaging nondeterminism only — content identity pinned by the clean diff and checksum record) installed over the override build on `R5CW30ZBM2R` → Success, data preserved. The phone again enforces production §7/§17 values; P7-O14 applies again. Delta 4 above remains as history.

## Claude delta 5 — debug-only owner-testing challenge values, 7 October 2026 (evening)

- **Author/role:** Claude (Opus 5.5), acting as implementer at Arjun's direct request in this session. The plan was aligned with him before any edit. Self-verification only; Arjun decides whether to route it for review. This replaces the reverted delta 4 approach: it applies to debug builds only, uses 20 s rather than 10 s, and keeps the three-rung ladders.
- **Exact base:** HEAD `70bd8e8`; `app/` was clean before this delta (the delta 4 revert). Uncommitted, not staged except the `git mv` rename of the guard test.

### Values (debug variant only; release unchanged)

| Value | Release (production) | Debug |
|---|---|---|
| Pause typing min / default (max 200, step 10) | 100 / 150 | **20 / 20** |
| Pause wait min / default (max 300, step 5) | 60 / 60 | **20 / 20** |
| Disable typing ladder, preselected | 220 / 350 / 700, 350 | **20** / 350 / 700, **20** |
| Disable wait ladder, preselected | 180 / 360 / 720, 360 | **20** / 360 / 720, **20** |
| Pause minutes 5–100 / 15; "Recommended" badge on the middle card | unchanged | unchanged |

### Files (8)

- `data/repo/BlockRepository.kt`: keeps only the shared values. The variant values moved out under the same names, so callers and `requireValidConfiguration` are unchanged.
- **New** `app/src/release/java/.../data/repo/ChallengeValues.kt` (production values) and `app/src/debug/java/.../data/repo/ChallengeValues.kt` (testing values, with a revert note). These use the standard AGP variant source sets. No Gradle, BuildConfig, dependency, schema or manifest change.
- `ui/screens/CreateFlowScreen.kt`: the `disableCardCopy` wait text names rungs under a minute in seconds ("Wait for 20 seconds.", not "0 minutes"). Production copy is unchanged. The function is now `internal` so it can be tested. The step-4 comment is updated.
- `test/.../ui/screens/CreateFlowStateTest.kt` and `androidTest/.../ui/CreateFlowScreenTest.kt`: values that differ by variant are referenced by name, not literal, so the same suite runs in both variants.
  - The literal production-ladder case moved to the variant guards.
  - Preselection is asserted as the variant default's index.
  - New `disableWaitCopyNamesSecondsBelowAMinute`.
- `ProductionChallengeValuesTest` was moved to `src/testRelease` with its assertions unchanged. **New** `src/testDebug/.../DebugChallengeValuesTest` pins the debug table.
- `androidTest/.../BlockRepositoryTest.kt` is unchanged: its rejection values (10, 15, 155, 205, 230, 300, 120, 200, 3600) reject in both variants, and its 350/360/720 fixtures are valid in both.

### Checks

| Check | Result |
|---|---|
| `./build.sh assembleDebug testDebugUnitTest assembleDebugAndroidTest` | BUILD SUCCESSFUL. **220 JVM tests, 0 failures/errors/skips** (debug values). Android tests **compiled only**. |
| `./build.sh testReleaseUnitTest` | BUILD SUCCESSFUL. **220 JVM tests, 0 failures** against the production values, including `ProductionChallengeValuesTest`. |
| `git diff --check`; Gradle/schema files | Clean; no change. |

### Limits and revert

- **Existing blocks keep their stored values.** Edit and re-save a block to pick up 20. A block holding 220 chars / 180 s shows its disable rung unselected until a new rung is picked (P7-O15 pattern).
- **Debug builds now intentionally differ from release:** P7-O14 does not apply to debug builds while this is in place.
- **Revert:** copy the release `ChallengeValues.kt` values into the debug file, or fold both back into `BlockRepository.kt`. Then restore the production literals in `DebugChallengeValuesTest`, or delete it. Re-run both unit-test variants.

## Claude delta 6 — P7-F14 Feedback mailto repair and P7-F15 verification, 8 October 2026

- **Author/role:** Claude (Sonnet 5.5), implementer, owner-assigned in place of GLM for this bounded direct correction (WORKFLOW "Direct owner → GLM corrections"). No product ambiguity, state-machine or data logic was involved, so no escalation. Self-verification only; no independent review.
- **Exact base:** HEAD `70bd8e8` plus the uncommitted delta 5 and the 8 October documentation edits, all preserved. This delta touches none of delta 5's files. Nothing was staged, unstaged or committed by this session; the index entries shown by `git status` (`A`/`RM` for the variant source sets) are delta 5's, unchanged.
- **Defect (P7-F14):** the Feedback intent was `ACTION_SENDTO` with data `mailto:<address>` and the subject/body only as `EXTRA_SUBJECT`/`EXTRA_TEXT`. Gmail builds the draft from the URI, so it opened with no subject and no body (fails P7-O11 on the `8ca43e64…` build).

### Change

- `ui/util/FeedbackEmail.kt` — new pure `mailtoUri(body)`: `mailto:arjranaprep@gmail.com?subject=…&body=…` per RFC 6068. UTF-8 percent-encoding via `URLEncoder`, with `+` → `%20` so a space is never read as a literal plus; every line break (`\n`, `\r`, `\r\n`) becomes `%0D%0A`; the fixed recipient stays unencoded. JVM-testable (no `android.net.Uri`).
- `ui/util/FeedbackMail.kt` — the intent now uses `Uri.parse(FeedbackEmail.mailtoUri(body))` and keeps `EXTRA_SUBJECT` and `EXTRA_TEXT`, adding `EXTRA_EMAIL = [recipient]`. Still no stream/attachment extras. `MainActivity`'s handler resolution, launch and `feedback_sent` logic is unchanged (the manifest `<queries>` SENDTO/mailto entry already existed).
- **P7-F15:** no behavior change. The body is `rememberSaveable` instance state only. Nothing writes it to disk, Room, prefs or an event payload (checked `FeedbackScreen`, `MainActivity`, `EventRepository`, `EventTaxonomy`). It is dropped when the route is popped (Back or `onSent`). Only the stale `FeedbackScreen.kt` header comment ("process death keeps the typed text") was corrected to the F15 wording.

### Files (5)

| File | Change |
|---|---|
| `app/src/main/java/com/arjunrana/tokishrine/ui/util/FeedbackEmail.kt` | `mailtoUri` + private encoder |
| `app/src/main/java/com/arjunrana/tokishrine/ui/util/FeedbackMail.kt` | URI built from `mailtoUri`; adds `EXTRA_EMAIL` |
| `app/src/main/java/com/arjunrana/tokishrine/ui/screens/FeedbackScreen.kt` | comment only |
| `app/src/test/java/com/arjunrana/tokishrine/ui/util/FeedbackEmailTest.kt` | 7 new tests; the 4 existing tests are unchanged |
| `app/src/androidTest/java/com/arjunrana/tokishrine/ui/util/FeedbackMailTest.kt` | updated to the new contract: the old assertion that the data is the bare `mailto:<address>` was the defect. Now asserts data = `mailtoUri(body)`, `EXTRA_EMAIL`, and parses back with `android.net.MailTo` (to/subject/body incl. `&`, `?`, `=`, `%`, `#`, `+`, newline, non-ASCII) |

Plus the records: TASK (F14/F15 status rows), OWNER-CHECKS (P7-O10 re-baseline, P7-O11 note, direct-correction record), CURRENT.

### Checks

| Check | Result |
|---|---|
| `./build.sh testDebugUnitTest` | BUILD SUCCESSFUL. **227 JVM tests, 0 failures/errors/skips** (result XML read: 220 before + 7 new). `FeedbackEmailTest` 11/11. |
| `./build.sh assembleDebugAndroidTest` | BUILD SUCCESSFUL. Instrumented sources **compiled only, not executed**. |
| `testReleaseUnitTest` | Not run: the change is in shared `main` code and tests with no variant dependence. Delta 5's release run (220/0) is recorded above and is not re-attributed to this delta. |
| Device / Gmail behavior | **NOT VERIFIED.** No adb, device, emulator or install, per AGENTS. |

New JVM tests cover: exact URI for a plain body; `&`, `=`, `?`, `#`, `%`, `+` encoded so the query cannot be split (one `?`, one `&`); no `+` for spaces (including double spaces); `\n`, `\r\n`, `\r` and blank lines → `%0D%0A` without doubling; UTF-8 (`é`, emoji); empty body keeps the subject; and a decode round trip of the whole query against the typed text.

### Interpretation and limits for review/owner

- **Both URI and extras are sent** (as the architect specified). Gmail uses the URI. A mail client that merged both would show the body twice; none is known to, but this is unverified on Arjun's other mail apps. If P7-O11 shows duplicates, drop `EXTRA_TEXT` and keep the URI.
- **Line breaks are sent as CRLF** (RFC 6068). The typed text is otherwise exact; clients normalise CRLF themselves.
- **Very long text** now travels in both the URI and the extra (roughly double the Binder payload). A pathological multi-hundred-KB paste could make `startActivity` throw, which is shown as the existing "No email app could be reached" message with the text kept. No cap was added: not in scope, and a cap would alter "the typed text as the body".
- **P7-F15 interpretation to confirm:** a system process-death restore (not force-stop, not swipe-away from recents) still returns the saved text, because the route stack is also saveable. Nothing is persisted. If Arjun wants even that cleared, it is a separate change.
- **Not installed.** The phone still runs the `8ca43e64…` debug build; P7-O11 stays failed until a build containing this delta is installed and retested.

### Paste-ready next-role prompt

> Read AGENTS.md. P7-F14 is implemented and P7-F15 verified (HANDBACK delta 6, uncommitted with delta 5; nothing installed). No further code is assigned for them. Owner follow-up: install a build containing delta 6, then retest P7-O11 (Gmail shows recipient, subject and body once) and P7-O10 (rotation only). Next implementation sessions follow the TASK order: Codex (senior) for P7-F3/F4 auto-nope, P7-F6 schema/baseline, P7-F13/F10 data and the P7-F5 add-only rule; then GLM for the UI items.

## Claude delta 7 — submission F-A: P7-F3 auto-nope, P7-F4 cleared text, P7-F2 Back hook, 8 October 2026

- **Author/role:** Claude (Opus 5.5), senior implementer assigned by Arjun for submission F-A (in place of the suggested Codex session). Authority: PRD §17 "Phase 7 owner-experience addendum — 8 October 2026" and the TASK's 8 October rows P7-F2/F3/F4.
- **Exact base:** HEAD `70bd8e8` plus uncommitted deltas 5 and 6 and the 8 October documentation edits, all preserved. Nothing staged or committed.
- **Status:** implemented, self-verified, **ready for review**. The state table was written before any edit; the implementation follows it without deviation.

### State table (written before editing)

"Away" = the challenge or gate is not visible: `onStop` without a configuration change, or `ACTION_SCREEN_OFF`. The away-start is the earliest such signal (`SystemClock.elapsedRealtime`, which keeps counting through sleep and across process death). Away ends only on return (`onPostResume`). Limit: 15 s (`AUTO_NOPE_AWAY_MS`).

| Case | GATE (pause only) | ACTIVE | COMMITTING |
|---|---|---|---|
| Background (Home, app switch, recents), return < 15 s | Same gate, no outcome | Same challenge and passage; **typed text and mismatch marks cleared** on return; wait restarts (as today) | No away clock; the commit proceeds and wins as today |
| Screen off, unlock < 15 s | As background. SCREEN_OFF and `onStop` both start the clock; the earlier start is kept. A late SCREEN_OFF that arrives once resumed and interactive is ignored | As background | As background |
| Away ≥ 15 s (timer, or evaluated on return/recreate) | Pause nope via `recordWalkAwayAndCount`, source `auto_away`; no Walk-Away moment; finish quietly | **Pause:** nope, source `auto_away`, quiet finish. **Turn-off:** `turnoff_abandoned` exactly as Never Mind (progress at expiry), block stays on, finish | Nothing: the commit wins (no away clock is running; a late expiry returns no effect) |
| Configuration change | Not away; no clock starts | Not away; wait accrual pauses and resumes (as today); typed text kept | Not away; pending completion reissued on recreate (as today) |
| Process death / recreation | Away-start saved in instance state with the snapshot. On recreate: resumed → evaluate as a return (expired → quiet auto-nope; else continue); not resumed → expire now or schedule the remaining time. Clock went backwards (reboot) → treated as expired | Same; a non-expired return clears typed text and restarts the wait | Away cleared when commit starts; recreate reissues the pending completion (as today) |
| `onNewIntent` detection relaunch while away | Expired → old session's `auto_away` nope recorded quietly, then the new launch loads. Not expired → replaced by the new session (as today; the old gate records nothing) | Expired → old session's auto-nope (pause) or Never Mind (turn-off) recorded quietly, then the new launch loads. Not expired → the existing challenge is brought forward; return < 15 s clears typed text | Brought forward; commit wins |
| Timer fires after a commit started | — | Commit start clears the away-start, and expiry is a no-op outside GATE/ACTIVE. A timer belonging to a replaced runtime is ignored | Commit wins |
| Back | `finish()` (unchanged) | No `moveTaskToBack`: bumps a Never-mind nudge signal and fires a light haptic | Inert (unchanged) |

Purpose summary: **pause** auto-nope = the existing walk-away transaction (raw `walk_away` event, inclusive daily count, StatsLedger nope with the same valuation and five-minute dedup, idempotent per session) with source `auto_away`, and no moment screen. **Turn-off** auto-nope = Never Mind (`turnoff_abandoned`, no Stats outcome). A visible wait still completes as a push-through. On-screen time without progress never counts.

### Implementation

- **`ChallengeRuntime` (pure):** new `awayStartMs` in the snapshot. `onBackgrounded(nowMs)` now covers GATE as well as ACTIVE and keeps the earliest start. New `onReturned(nowMs)`: returns the auto-nope when expired; otherwise ends the away period and clears typed text and mismatch marks (P7-F4). New `expireAway(nowMs)`: GATE → `WALK_AWAY` + `auto_away`; ACTIVE pause → `auto_away`; ACTIVE turn-off → `TurnOffAbandoned`. It is a no-op outside GATE/ACTIVE (the commit wins), and a backwards clock counts as expired. New `awayRemainingMs`. Commit start and every terminal transition clear the away-start, so each session ends at most once. `AUTO_NOPE_AWAY_MS = 15_000` sits in the runtime companion for now; P7-F11 (GLM) moves it into the config file. Walk-away sources now use named constants.
- **`BlockActivity`:** `onStop` (not a configuration change) and SCREEN_OFF both call `goAway`: stop ticker, release detection suppression, `onBackgrounded`, schedule the timer. A late SCREEN_OFF is ignored while the activity is resumed and the screen is interactive. `onPostResume` cancels the timer and calls `onReturned` before `onVisible`. `resolveSession` evaluates a restored away-start: resumed → as a return; otherwise expire now or reschedule. A session that loads after the activity was already stopped starts its clock. `onNewIntent` first asks `expireAway`; an expired session is recorded quietly (`recordDetached`) and the new launch loads. Otherwise the previous bring-forward/replace rules apply. An `auto_away` walk-away records through the existing `recordWalkAwayAndCount` path (pending source kept for recreation), then `finish()`es without the moment. A commit failure while hidden goes away instead of restarting the wait. Back in ACTIVE bumps `escapeNudge` and calls `BlockHaptics.nudge` (`EFFECT_TICK`); `moveTaskToBack` is gone; COMMITTING stays inert. The away-start is saved as `challenge_away_start` with the snapshot.
- **Repository/taxonomy:** `EventRepository.WALK_AWAY_SOURCE_{BLOCK_SCREEN,TYPING,COUNTDOWN,AUTO_AWAY}`; `EventTaxonomy.WALK_AWAY_SOURCES`; `recordWalkAwayAndCount` requires a known source. No new event name (the active set stays 40). There is no Stats code change: the ledger was already source-agnostic, so `auto_away` is valued and deduped exactly like a chosen nope by construction. No schema change.
- **UI hook only:** `TypingChallengeScreen`/`DelayCountdownScreen` take `escapeNudge: Int = 0`. The new `ui/interruption/EscapeNudge.kt` `Modifier.escapeNudge` is a minimal ~0.2 s horizontal shake placeholder on the Never mind control: the pause header text, the turn-off button and the waiting escape. No copy, layout or Stats change.

### Files (16)

| File | Change |
|---|---|
| `app/src/main/java/com/arjunrana/tokishrine/challenge/ChallengeRuntime.kt` | away clock, return/expiry, source constants |
| `app/src/main/java/com/arjunrana/tokishrine/BlockActivity.kt` | lifecycle wiring, timer, relaunch, quiet finish, Back hook, saved state |
| `app/src/main/java/com/arjunrana/tokishrine/data/repo/ChallengeRepository.kt` | source guard + doc comment |
| `app/src/main/java/com/arjunrana/tokishrine/data/repo/EventRepository.kt` | walk-away source constants |
| `app/src/main/java/com/arjunrana/tokishrine/data/repo/EventTaxonomy.kt` | `WALK_AWAY_SOURCES` |
| `app/src/main/java/com/arjunrana/tokishrine/ui/util/Haptics.kt` | `BlockHaptics.nudge` |
| `app/src/main/java/com/arjunrana/tokishrine/ui/interruption/EscapeNudge.kt` | **new**, placeholder wiggle modifier |
| `app/src/main/java/com/arjunrana/tokishrine/ui/interruption/TypingChallengeScreen.kt` | `escapeNudge` param on both Never mind controls |
| `app/src/main/java/com/arjunrana/tokishrine/ui/interruption/DelayCountdownScreen.kt` | `escapeNudge` param on the escape button |
| `app/src/test/java/com/arjunrana/tokishrine/challenge/ChallengeRuntimeTest.kt` | 11 new tests; 3 updated (below) |
| `app/src/test/java/com/arjunrana/tokishrine/data/repo/EventTaxonomyTest.kt` | 1 new test |
| `app/src/androidTest/java/com/arjunrana/tokishrine/data/ChallengeRepositoryTest.kt` | 1 new instrumented test (compiled only) |
| `docs/components/persistence-events.md` | sources + auto-nope invariant; "nonterminal suspension" wording superseded |
| `docs/components/stats.md` | one sentence: auto-nope is an ordinary nope outcome |
| `PRD.md` | §10 `walk_away` source row gains `auto_away` (incorporates the 8 October addendum) |
| `coordination/…/OWNER-CHECKS.md`, `TASK.md`, `CURRENT.md`, this HANDBACK | P7-S5 re-baseline (original kept), F-A status, state |

**Tests changed on purpose (supersession, not weakening):** `backgroundingPreservesTypingPassageAndEnteredText` became `returnWithinLimitKeepsSessionAndPassageButClearsTypedText`. The P7-F4 rule now clears the text, and the passage, session and attempts are still asserted. `backgroundingResetsWaiting…` keeps its assertions, with the return moved inside 15 s (it previously returned after 20 s, which is now an auto-nope). `completionFirstOwnsRace…` gained the away-expiry assertions. No other test was touched.

**Row → JVM test mapping:** background/return <15 s → `gateReturnWithinLimit…`, `returnWithinLimit…ClearsTypedText`, `waitingReturnWithinLimitRestartsTheWait`, `backgroundingResetsWaiting…`; screen-off/unlock → `screenOffThenStopKeepsTheEarliestAwayStart`; ≥15 s by purpose → `pauseGateAndChallengeAutoNope…`, `turnOffAutoNopeEqualsNeverMind…`, `visibleWaitStillCompletesAsPushThrough…`; config change → `configurationChangeIsNotAway…`; process death → `processDeathRestoresTheAwayStart…` (includes the reboot clock); `onNewIntent` → `relaunchWhileAwayEndsAnExpiredSessionOnceOrKeepsALiveOne`; late timer vs commit → `lateAwayTimerAfterCommitStartedLetsTheCommitWin`, `completionFirstOwnsRace…`; escape stops the clock → `explicitEscapeStopsTheAwayClock`; taxonomy → `walkAwaySourcesIncludeAutoAwayWithoutANewEventName`. The repository row (same transaction, valuation and dedup as a chosen nope; unknown source rejected) is `ChallengeRepositoryTest.autoAwayNopeTakesTheChosenNopePathWithItsOwnSource`. It is instrumented because the repository needs Room: **compiled, not executed**. Activity wiring itself has no JVM harness; the runtime tests cover each decision it delegates.

### Checks

| Check | Result |
|---|---|
| `./build.sh assembleDebug testDebugUnitTest testReleaseUnitTest assembleDebugAndroidTest` | BUILD SUCCESSFUL. Result XML read: **testDebugUnitTest 239 tests, 0 failures/errors/skips; testReleaseUnitTest 239, 0/0/0** (227 before + 12 new). `ChallengeRuntimeTest` 28/28. |
| Instrumented tests | **Compiled only, not executed** (no device/emulator authorized). |
| Device behavior (lifecycle timing, haptic, wiggle, OEM screen-off) | **NOT VERIFIED.** No adb, device, emulator or install. |
| Delta 5 preserved | `app/src/{debug,release}/…/ChallengeValues.kt` and the variant tests untouched (mtimes 7 October; `git status` entries unchanged). Delta 6 files untouched. |

### Limits and points for review

1. **Process death while away, task never reopened → nothing is recorded.** This follows the brief ("evaluate on recreate"). A durable record would need an alarm or a Room-persisted away marker, which is out of scope.
2. **Deep sleep delays the timer.** `delay` runs on uptime, so with the screen off the 15 s timer can fire late, at the next CPU wake or on the user's return, whichever comes first. The decision always uses elapsed realtime, so the outcome is right, but the event/outcome timestamp (and so the Stats day and dedup anchor) is the recording time, not away-start + 15 s.
3. **The detached record on `onNewIntent`** is fire-and-forget in `applicationScope`. It survives the activity, but not a process kill at that instant. The normal timer/return path keeps the pending-source recreation safety.
4. **Turn-off auto-nope progress:** waiting reports 0 % (leaving already resets the wait, as before); typing reports the text length at expiry. Both go to `turnoff_abandoned.progress_pct` exactly as Never Mind would.
5. **Unchanged and out of scope:** Back on the GATE still `finish()`es with no outcome. A user who returns to an already-expired session can see the old challenge for a frame before it finishes. A non-expired GATE replaced by a detection relaunch still records nothing (existing rule).
6. **Late SCREEN_OFF guard** relies on `PowerManager.isInteractive`. If an OEM never stops the activity on screen-off, the return is still detected by `onPostResume` on the next resume.
7. **Wiggle is a placeholder** (P7-F2 animation is GLM's); the signal is the `escapeNudge` counter. P7-F1 (moving the pause Never mind under Submit) is not done; the nudge applies to whichever control is there.
8. **Not installed and not committed.** P7-S5 (re-baselined) and the challenge checks need the next installed build.

### Paste-ready reviewer prompt (different model/session — Codex recommended)

> Read AGENTS.md, then CURRENT, WORKFLOW and TS-P7-stats-feedback-final (TASK 8 October rows P7-F2/F3/F4; HANDBACK "Claude delta 7 — submission F-A"). Authority: PRD §17 "Phase 7 owner-experience addendum — 8 October 2026". Role: reviewer of submission F-A only (implemented by Claude Opus 5.5; you must be a different model/session). Scope: the delta-7 files listed in HANDBACK. Ignore delta 5 (debug ChallengeValues) and delta 6 (Feedback mailto), which are separate uncommitted work. Check the implementation against the HANDBACK state table: GATE/ACTIVE/COMMITTING × background/return, screen-off/unlock, ≥15 s by purpose (pause → `auto_away` through `recordWalkAwayAndCount`, no Walk-Away moment; turn-off → Never Mind), configuration change, process death/recreation (saved away-start), `onNewIntent` relaunch while away, late timer vs COMMITTING, and Back (wiggle signal + haptic, no `moveTaskToBack`, COMMITTING inert). Confirm the three updated tests are supersessions, not weakening, and that Stats valuation/dedup is unchanged for `auto_away`. Weigh the listed limits (process death never reopened, deep-sleep timer latency, detached record on relaunch). Run non-device checks only if a concrete question needs them (`./build.sh testDebugUnitTest testReleaseUnitTest`); no adb/device/emulator/install, no commit. Record PASS / PASS WITH NOTES / FAIL in REVIEW.md for this exact submission and update CURRENT.

### Repair F-A-R1 — P7-F-A1 (SCREEN_OFF race counted offscreen wait time), 8 October 2026

- **Author/role:** Claude (Opus 5.5), the F-A senior implementer, repairing the review blocker per [REVIEW](REVIEW.md) "submission F-A" (FAIL, P7-F-A1). Base: the F-A tree above, unchanged otherwise; deltas 5 and 6 untouched. Nothing staged or committed.
- **Defect:** SCREEN_OFF opened the away period while `resumed` stayed true until `onStop`. A delayed `recordStarted` (or other persistence) callback in that interval called `onVisible` and restarted the ticker. The runtime accepted it, so `visibleSinceMs` covered offscreen time; a return within 15 s then resumed a shortened wait instead of restarting it.
- **Fix:** the runtime is the guard. `ChallengeRuntime.onVisible` is ignored while `awayStartMs` is set, and `tick` returns null while away, so a ticker started in the gap cannot complete. Only `onReturned` closes the away period; `onPostResume` already calls it before `onVisible`. Defence in depth: `BlockActivity.startTickerIfNeeded` also refuses to start while `engine.isAway()` (new accessor). No other caller changed. Every late visibility callback (`persistStarted`, the commit-failure path, `resolveSession`) goes through the same guarded `onVisible`. The commit-failure path cannot be away, because commit start clears the away-start.

| File | Change |
|---|---|
| `app/src/main/java/com/arjunrana/tokishrine/challenge/ChallengeRuntime.kt` | `onVisible`/`tick` away guards; `isAway()` |
| `app/src/main/java/com/arjunrana/tokishrine/BlockActivity.kt` | ticker gate on `isAway()` |
| `app/src/test/java/com/arjunrana/tokishrine/challenge/ChallengeRuntimeTest.kt` | 2 new regressions |

**Regressions** (activity call order mirrored on the pure runtime; there is no Android-lifecycle JVM harness):
- `screenOffThenDelayedStartCompletionCountsNoOffscreenTimeAndReturnRestartsTheWait`: pause wait of 10 s, in this order:
  1. Begin at 0 (`enterChallenge` + `onVisible`).
  2. SCREEN_OFF at 3 s, still resumed.
  3. Late `recordStarted` → `onVisible` at 4 s.
  4. A tick at 13 s cannot complete and 10 s still remain.
  5. Unlock at 14 s, before any `onStop` → `onReturned` + `onVisible`.

  The wait then completes at exactly 24 s with `countdownElapsedMs = 10 000`. The pre-fix behaviour would have completed at the return.
- `turnOffWaitStartedLateAfterScreenOffAlsoWaitsForTheReturn`: the same race on the turn-off `beginTurnOff` → `persistStarted` path.

**Checks:**
- `./build.sh assembleDebug testDebugUnitTest testReleaseUnitTest assembleDebugAndroidTest` → BUILD SUCCESSFUL. Result XML: **241 JVM tests, 0 failures/errors/skips on debug and on release** (239 + 2). `ChallengeRuntimeTest` 30/30.
- **Mutation check:** with only the `onVisible` guard removed, both new tests fail (`30 tests completed, 2 failed`). The file was restored byte-for-byte, and debug was recompiled and re-run (241/0). Release was up to date from the pre-mutation run of the same sources.
- `git diff --check` is clean. Instrumented sources compiled only. No device, adb, emulator or install.

**Remaining limit (unchanged, now explicit):** if an OEM delivers SCREEN_OFF but the activity is neither paused nor stopped, no `onPostResume` signals the return. The wait then stays stopped, and the 15 s timer can auto-nope even though the screen came back on. The fix makes this case safe for the visible-time rule (no offscreen time counted), but the return still relies on the lifecycle. Detecting it would need a SCREEN_ON/USER_PRESENT receiver, which is a new behaviour for the reviewer/owner to decide, not part of this repair.

**Paste-ready scoped re-review prompt (different model/session — Codex recommended):**

> Read AGENTS.md, then CURRENT and TS-P7-stats-feedback-final REVIEW.md (submission F-A, P7-F-A1) and HANDBACK "Repair F-A-R1". Role: scoped re-reviewer of the P7-F-A1 repair only (implemented by Claude Opus 5.5; use a different model/session). Check that `ChallengeRuntime.onVisible`/`tick` cannot accrue or complete while an away period is open, that `onReturned` is the only path back to visible (`onPostResume` order, `resolveSession`, `persistStarted`, commit-failure path), that `startTickerIfNeeded`'s `isAway()` gate is consistent, and that the two new regressions express SCREEN_OFF → delayed start completion → return within 15 s. Re-inspect only the affected F-A lifecycle paths; the rest of F-A was confirmed in the earlier review. Weigh the stated no-pause OEM limit. Non-device checks only if a concrete question needs them; no adb/device/emulator/install, no commit. Deltas 5 and 6 are out of scope. Record the verdict for F-A + R1 in REVIEW.md and update CURRENT.

### Repair F-A-R2 — P7-F-A2 (failed save after SCREEN_OFF) under the owner's failed-save rule, 8 October 2026

- **Author/role:** Claude (Opus 5.5), F-A senior implementer. Base: the F-A + R1 tree; deltas 5 and 6 untouched; nothing staged or committed.
- **Finding** ([REVIEW](REVIEW.md) "F-A + repair R1"): SCREEN_OFF during COMMITTING opens no away period. If the save then throws before `onStop`, the failure branch saw `resumed == true`, returned to ACTIVE and restarted the ticker, so the earned wait was retried with the screen dark.
- **Owner decision (Arjun, 8 October), recorded in PRD §17's 8 October addendum:** *a failed save never costs a completed challenge.* On screen: retried as before. Off screen or hidden: kept and saved when the user is next on screen. The away clock never starts from a failed save. **This deliberately replaces the reviewer's suggested remedy** ("start an away period if still hidden"). That remedy would turn a completed wait into an auto-nope after 15 s, or force the wait to be redone. The implementer's own F-A hidden-branch `goAway` had the same flaw (introduced in F-A; before F-A the save was retried on return), so this repair removes it too.
- **Context for the reviewer:** the path needs a Room **exception** while saving (disk full, corruption, I/O error). The ordinary "not committed" result (block disabled or values changed) still finishes the screen as before. The failure is rare; the repair is about choosing a correct outcome, not about how often it happens.

**Change:**
- `ChallengeRuntime.saveFailed(token, nowMs, onScreen)`. On screen it is exactly `commitFailed` (Phase 5 behaviour unchanged). Hidden, it stays COMMITTING with the same token, so `onBackgrounded`, `expireAway`, `onReturned`, `tick` and Back have no effect on it, and recreation reissues it through the existing `resumePendingCompletion`.
- `BlockActivity`:
  - The save-exception branch calls `saveFailed` with `onScreen = resumed && screenInteractive()`. The F-A `goAway` there is gone.
  - `onPostResume`, after `onReturned`, retries a pending completion through `resumePendingCompletion`. A `commitInFlight` flag prevents a duplicate attempt while a save is still running. Even a duplicate would be harmless: the repository's completion marker and the token check make a second success a no-op.
  - `screenInteractive()` is shared with the R1 late-SCREEN_OFF guard.
- R1 turn-off test: the timestamps now increase monotonically (11.5 s tick before the 12 s return). It still fails without the R1 guard.

| File | Change |
|---|---|
| `app/src/main/java/com/arjunrana/tokishrine/challenge/ChallengeRuntime.kt` | `saveFailed` |
| `app/src/main/java/com/arjunrana/tokishrine/BlockActivity.kt` | failure branch, retry on return, `commitInFlight`, `screenInteractive()` |
| `app/src/test/java/com/arjunrana/tokishrine/challenge/ChallengeRuntimeTest.kt` | 3 new tests; R1 turn-off test timestamps made monotonic |
| `PRD.md` | one bullet in the 8 October addendum (owner rule) |

**New tests:**
- `screenOffDuringCommitThenFailedSaveBeforeStopKeepsTheEarnedWaitForTheReturn`: the reviewer's sequence, in this order:
  1. The wait completes on screen.
  2. SCREEN_OFF while committing: no away clock.
  3. The save throws before `onStop` (`onScreen = false`): still COMMITTING, no tick can retry in the dark.
  4. `onStop`: no away clock, and no auto-nope even at 60 s.
  5. A recreation at that point reissues the same token.
  6. Unlock: the retry carries the same token, session and `countdownElapsedMs = 10 000`, then succeeds.
- `hiddenFailedSaveKeepsCompletedTypingWithoutClearingOrRecounting`: the typed text survives a 25 s absence, and attempts stay 1.
- `onScreenFailedSaveKeepsThePhaseFiveRetryAndStaleTokensAreIgnored`: on screen, the challenge goes back to ACTIVE and the earned wait re-completes at once with a fresh token; stale tokens are ignored.

**Checks:**
- `./build.sh assembleDebug testDebugUnitTest testReleaseUnitTest assembleDebugAndroidTest` → BUILD SUCCESSFUL. **244 JVM tests, 0 failures, on debug and on release** (241 + 3). Android tests compiled only.
- **Mutation checks:**
  - Dropping the `onScreen` guard fails both hidden-failure tests.
  - Dropping the R1 `onVisible` guard fails both R1 tests, including the reordered turn-off test.
  - The source was restored (byte-compare OK) and debug re-ran 244/0.
- `git diff --check` clean. No device, adb, emulator or install.

**Limits:**
1. A persistent database failure while hidden leaves the session waiting in COMMITTING until the next return. That return retries once, and an on-screen failure then follows the Phase 5 path: back to ACTIVE, with the escape available.
2. The R1 no-pause OEM note is unchanged.
3. The earlier F-A limits stand.

**Paste-ready scoped re-review prompt (different model/session):**

> Read AGENTS.md, then CURRENT and TS-P7-stats-feedback-final REVIEW.md ("F-A + repair R1", P7-F-A2) and HANDBACK "Repair F-A-R2". Role: scoped re-reviewer of R2 only (implemented by Claude Opus 5.5; use a different model/session). Authority for the remedy is the owner's rule now in PRD §17's 8 October addendum: "a failed save never costs a completed challenge" — on screen retried as before; hidden or screen off kept and saved on the next return; the away clock never starts from a failed save. This intentionally replaces the earlier suggested away-period remedy; review against the rule, not that suggestion. Check `ChallengeRuntime.saveFailed`, the `BlockActivity` save-exception branch, the `onPostResume` retry and `commitInFlight`, interaction with SCREEN_OFF/`onStop`/`onNewIntent`/recreation while COMMITTING, and that the three new tests and the reordered R1 turn-off test express the sequences claimed. Re-inspect only these paths; non-device checks only if a concrete question needs them; no adb/device/emulator/install, no commit; deltas 5 and 6 out of scope. Record the verdict for F-A + R1 + R2 in REVIEW.md and update CURRENT.

## Claude delta 8 — submission F-B: P7-F6 baseline + override (schema v5), P7-F13/F10 data, P7-F5 add-only rule, 8 October 2026

- **Author/role:** Claude (Opus 5.5), senior implementer assigned by Arjun for submission F-B (Prompt B, after F-A's PASS WITH NOTES). Authority: PRD §17 "Phase 7 owner-experience addendum — 8 October 2026", TASK rows P7-F5/F6/F10/F13, the [Stats contract](../../../docs/components/stats.md) and [persistence/events](../../../docs/components/persistence-events.md).
- **Exact base:** HEAD `70bd8e8` + uncommitted deltas 5, 6 and F-A (delta 7 + R1 + R2), all preserved and untouched. Nothing staged or committed. Source identity: [F-B-SUBMISSION.sha256](F-B-SUBMISSION.sha256) (17 files; `shasum -a 256 -c` from the repo root).
- **Status:** implemented, self-verified, **ready for review**. Data/state and APIs only; the chips, sheet, Add more screen and Today/This week layout are GLM's.

### What changed

**P7-F6 usual visit length.**
- `StatsLedger.usualVisit(durations)`: drop visits < `MIN_VISIT_MS` (30 s), then the `VISIT_PERCENTILE` (75th) percentile by linear interpolation between closest ranks (numpy/Excel `PERCENTILE.INC`). Fewer than `MIN_VISITS` (3) qualifying visits → `FALLBACK_MS` (10 m). `samples` now stores the qualifying count. The constants sit in the `StatsLedger` companion beside `FALLBACK_MS`/`DEDUP_MS`; P7-F11 (GLM) moves them into the config file. Used at initial capture and Recalibrate (replaces the mean of all visits > 0).
- **Existing installs:** per Arjun's TASK-row decision (8 October), **no one-time recompute**. `MIGRATION_4_5` leaves `stats_baseline` alone; old mean baselines stay until the next Recalibrate, which re-measures only the last 7 days of Android history. Pinned by `StatsMigrationTest.v4Upgrade…`.

**P7-F6 per-app override (schema v4 → v5).**
- New entity `StatsVisitOverride` → table `stats_visit_override(packageName PK, visitMs, setAt)`. `MIGRATION_4_5` creates only this table and is registered in `TokiApplication` beside `MIGRATION_3_4`. Exported `5.json` added; `3.json`/`4.json` are byte-identical to the P7-D1 hashes (`bff02516…`, `e8eae786…`).
- `StatsRepository.setVisitOverride(packageName, minutes): Boolean` and `clearVisitOverride(packageName): Boolean` write under the repository mutex, then `refresh()`. Minutes are whole and `OVERRIDE_MINUTES_MIN..MAX` = **1..120 (provisional; see points 3)**; invalid input throws `IllegalArgumentException`. False = write failed, nothing changed.
- `StatsDashboardCalculator` re-values **every counted nope of an overridden package in the window**: week totals, `days` (chart), today, and the app row (`savedMs = nopes × override`). Ignored (deduped) nopes stay 0. Stored `savedMs` is never rewritten. The ledger still freezes the *measured* baseline at resolution, so "Use measured" restores the frozen values exactly. Overrides survive Recalibrate and revocation. Packages without an override keep frozen values.
- `StatsApp` gains `measuredVisitMs`, nullable `userVisitMs` and `savedWith(visitMs)` for the sheet's live "N nopes × Xm" line. `usualVisitMs` is now the effective value; `usesFallback` describes the measured baseline.

**P7-F13 data.** `StatsDashboard` gains today's fields from local midnight: `todaySavedMs`, `todayAttempts` (counted nopes + push-throughs, so one counted nope adds exactly 1), `todayNopes`, `todayNopeRatePercent`, and nullable `todayScreenMs`. The last was not in the prompt's list, but the PRD's "Screen time Xh Ym today" line needs it; it is null unless covered history spans midnight → now. The week keeps `savedPerDayMs` (Avg time saved = 7-day ÷ 7) and "N of M" = `nopes` of `attempts`. **`screenChangePercent` is removed** ("vs your usual" no longer exposed). `attemptsPerDay` and the 7-day `screenDailyMs` stay, documented as retired/unused by the new layout, so the current screen compiles until GLM replaces it.

**P7-F10.** `NoBlocks` (S6) only when there is no enabled block **and** no counted outcome in the 7-day window. Otherwise `Ready` with new `blocksOn = false`. `leaderboardPackages` adds every package with a counted outcome this week, still intersected with installed packages.

**P7-F5.** `BlockRepository.addTargets(id, apps, sites): Int` is one transaction allowed on an ON block. The lists are the full Add-more selection. Omitting any existing app/site throws the new `TargetRemovalException`; another block's target throws `ConflictingOwnershipException`. Both roll back everything. Domains are canonicalized as elsewhere. It changes nothing else on the block, writes no event and no enabled transition, and returns the number added. Detection: `TokiAccessibilityService` already collects `observeBlocksWithContents()` into `ActiveBlockIndex`, and Room invalidation re-emits on the target insert, so additions protect on the next emission. No detection code change.

**Minimal UI wiring (compile only):** `screenTimeLine(screenDailyMs)` drops its comparison suffix, and `StatsScreen` passes the one argument. No layout or copy change otherwise.

### Files (17 source/schema/test + docs)

| File | Change |
|---|---|
| `data/stats/StatsStore.kt` | `StatsVisitOverride` entity; DAO `overrides`/`override`/`clearOverride` |
| `data/stats/StatsLedger.kt` | p75/floor/minimum and override-bound constants; `usualVisit` |
| `data/stats/StatsDashboard.kt` | today fields, `blocksOn`, override re-valuing, `StatsApp` fields, comparison removed |
| `data/stats/StatsRepository.kt` | p75 capture, F10 rule, today screen time, override APIs, leaderboard week outcomes |
| `data/db/TokiDatabase.kt`, `TokiApplication.kt` | v5, `MIGRATION_4_5`, registration |
| `data/repo/BlockRepository.kt` | `TargetRemovalException`, `addTargets` |
| `ui/screens/StatsPresentation.kt`, `StatsScreen.kt` | comparison removed from `screenTimeLine` (wiring) |
| `app/schemas/…/5.json` | **new**, Room export |
| `test/…/data/stats/StatsRedesignTest.kt` | 3 new tests; leaderboard test extended; comparison test retired (below) |
| `test/…/ui/screens/StatsPresentationTest.kt` | `screenTimeLine` test now pins "never compares"; fixture fields |
| `androidTest/…/data/StatsRepositoryTest.kt` | 3 new tests; 2 tests' visit durations raised (below) |
| `androidTest/…/data/StatsMigrationTest.kt` | shared schema builder; v3 test now opens through v5; new v4 → v5 test |
| `androidTest/…/data/BlockRepositoryTest.kt` | 3 new add-only tests |
| `docs/components/{stats,persistence-events,blocks-targets}.md` | contracts updated |

**Tests changed on purpose (supersession, not weakening):**
- `screenComparisonSuppressesSubOnePercentAndUnknownBaseline` was removed with the field it tested (the PRD removes the comparison). `screenTimeLineHides…` now asserts the line never carries a comparison.
- In `StatsRepositoryTest`, `baselineFrozen…` used 5 s/20 s visits. These are now below the 30 s floor, so they were raised to 40 s/80 s. All assertions are kept, and the post-recalibration p75 (80 s) is now asserted explicitly. `insufficientHistoryFallsBack…` was raised from 5 s to 40 s, so it still tests the "fewer than three" rule rather than the floor. The floor has its own new test.

**Rule → test mapping:**
- p75/floor/fallback: JVM `usualVisitIsSeventyFifthPercentile…`; Android `visitsUnderThirtySecondsNeverFormABaseline`, `baselineFrozen…`.
- Override re-valuing: JVM `overrideRevaluesThatAppsCountedNopesForTheWeekShownOnly`; Android `overrideRevaluesTheWeekSurvivesRecalibrateAndClearingRestoresFrozenValues`.
- Today/week fields: JVM `todayFieldsCountFromLocalMidnight…`.
- F10: JVM `enabledSiteBlocksAddHostingBrowsers…` (extended); Android `emptyStateOnlyWithoutEnabledBlocksAndWithoutOutcomesThisWeek` (also covers `todayScreenMs`).
- Migration: Android `v3UpgradePreserves…` (now v3 → v5) and `v4UpgradeKeepsStatsBaselinesOutcomesAndBlocksAndAddsAnEmptyOverrideTable`.
- F5: Android `addTargetsAddsToAnOnBlockAndReachesDetectionImmediately`, `addTargetsRejectsAnyRemovalAndKeepsPriorData`, `addTargetsRespectsOwnershipAtomically`.

### Checks

| Check | Result |
|---|---|
| `./build.sh assembleDebug testDebugUnitTest testReleaseUnitTest assembleDebugAndroidTest` (same as F-A) | BUILD SUCCESSFUL. Result XML: **246 JVM tests, 0 failures/errors/skips on debug and on release** (244 + 3 new − 1 retired). `StatsRedesignTest` 17/17, `StatsPresentationTest` 8/8. The only compiler warnings are the existing unused `closeCount` in `CreateFlowScreenTest`. |
| Mutation | Removing the override lookup in the calculator fails `overrideRevalues…` (17 run, 1 failed). The source was restored from a copy and debug re-ran 246/0. |
| Schema | `5.json` = `4.json` + `stats_visit_override` only. Its Room `createSql`/PK match `MIGRATION_4_5`'s columns. `3.json`/`4.json` unchanged. |
| Instrumented | **Compiled only, not executed.** No adb, device, emulator or install. |
| `git diff --check` | Clean. Deltas 5/6/F-A files untouched. |

### Points for Arjun / review

1. **P7-D1 needs an isolated re-run.** The migration adds v5, so the 4 October P7-D1 evidence (v3 → v4) no longer covers the shipped path. Re-run `StatsMigrationTest` (v3 → v5 and v4 → v5), plus `StatsRepositoryTest` and `BlockRepositoryTest` for the new compiled cases, on the isolated AVD `toki-p7d-isolated` only with your authorization. Your phone's first open of this build will run v4 → v5 (P7-O1-style observation).
2. **Leaderboard rule applied generally (confirm).** "Apps with outcomes this week stay listed" now applies whether or not other blocks are on. A package that left its block or whose block is off keeps its row for the week. Applying it only when *all* blocks are off would make rows appear and vanish depending on unrelated blocks. This supersedes the older "removed app omitted" expectation in P7-S7 for the current week; P7-S7 is re-baselined accordingly.
3. **Choices made within scope (change if you disagree):**
   - The percentile method is linear interpolation. Nearest-rank would equal the longest visit when there are exactly 3.
   - The override range is 1–120 whole minutes; the PRD gives presets but no stepper bounds.
   - No event is logged for setting or clearing an override (§10 has none). Add one only if you want it in analytics.
   - The empty state looks at counted outcomes only. A lone deduped nope cannot exist without its counted one, except at the window edge.
4. **Add more during an active pause:** pause is per block, so additions to a block that is currently paused stay open until that pause re-arms. Every other case protects immediately.
5. **GLM's next steps:**
   - Remove the old Attempts/day tile and `attemptsPerDay` / `screenDailyMs` if unused.
   - Render `todayScreenMs` as "Screen time X today", `blocksOn == false` as the note, and "—" when `todayAttempts == 0`.
   - Build the chip/sheet on `StatsApp` + `set/clearVisitOverride`.
   - Build Add more on `addTargets`. Map `TargetRemovalException` to "can't happen in UI", since existing targets are locked, and `ConflictingOwnershipException` to "Already added to a block".
6. Not installed, not committed. P7-S3/S6/S7 retests need the GLM UI build.

### Paste-ready reviewer prompt (different model/session — Codex recommended)

> Read AGENTS.md, then CURRENT, WORKFLOW and TS-P7-stats-feedback-final (TASK 8 October rows P7-F5/F6/F10/F13; HANDBACK "Claude delta 8 — submission F-B"). Authority: PRD §17 "Phase 7 owner-experience addendum — 8 October 2026", docs/components/stats.md and persistence-events.md. Role: reviewer of submission F-B only (implemented by Claude Opus 5.5; you must be a different model/session). Verify the source identity with `shasum -a 256 -c coordination/tasks/TS-P7-stats-feedback-final/F-B-SUBMISSION.sha256` from the repo root. Check:
> - the p75/30 s/fallback math and where it applies;
> - schema v5: `MIGRATION_4_5` against `5.json`, registration in `TokiApplication`, v3/v4 untouched, and both migration tests;
> - override re-valuing: only counted nopes of that package in the window; stored values never rewritten; survives Recalibrate; clear restores frozen values; the mutex/refresh in `set/clearVisitOverride`;
> - today fields at local midnight, `todayScreenMs` coverage, and removal of `screenChangePercent`;
> - the P7-F10 empty-state/leaderboard rule;
> - `BlockRepository.addTargets`: add-only, transactional, ownership, canonical domains, no enabled/event side effects, detection via the blocks flow.
>
> Confirm the changed tests are supersessions, not weakening. Weigh points 2–4 of the handback (general leaderboard rule, chosen percentile/bounds, pause interaction). Deltas 5, 6 and F-A are out of scope. Run non-device checks only if a concrete question needs them (`./build.sh testDebugUnitTest testReleaseUnitTest assembleDebugAndroidTest`); no adb/device/emulator/install, no commit. Record PASS / PASS WITH NOTES / FAIL in REVIEW.md for this exact submission and update CURRENT. P7-D1 needs an isolated re-run because of the migration; that needs Arjun's authorization and is not part of the review.

## GLM delta 9 — P7-F1 Never mind button, P7-F2 wiggle, P7-F11 config file, P7-F12 passage variety, 8 October 2026

- **Author/role:** GLM 5.3 (ZCode), implementer for the GLM UI/config part 1 per Arjun's session prompt (WORKFLOW table: routine UI/config work). Authority: PRD §17 "Phase 7 owner-experience addendum — 8 October 2026" rows P7-F1/F2/F11/F12; visual values per [theme-ui](../../../docs/components/theme-ui.md).
- **Exact base:** HEAD `70bd8e8` plus the uncommitted deltas 5, 6, F-A (delta 7 + R1 + R2) and F-B (delta 8), all preserved untouched. Nothing staged or committed by this session.
- **Status:** implemented, self-verified, **ready for review**.

### What changed

**P7-F1.** `TypingChallengeScreen`: the pause header "Never mind" text control is gone; both purposes now escape through the same full-width outlined 50 dp button under Submit (the old `TurnOffEscapeButton`, renamed `EscapeButton` with a label parameter). Label comes from the existing `delayEscapeLabel(purpose)`: "Never Mind" for turn-off (unchanged), "Never mind" for pause — the exact copy the header carried, matching the pause waiting screen. Submit is untouched: primary, disabled until `canSubmitTyping`. The `escapeNudge` modifier rides the button for both purposes.

**P7-F2.** `EscapeNudge.kt`: the F-A placeholder stepped shake is replaced by a damped-sine wiggle — ~550 ms, 8 dp peak amplitude, 2.5 horizontal swings with quadratic decay to rest at the exact starting position. Same `Modifier.escapeNudge(nudge: Int)` API and counter; per-frame offsets are plain state read inside the `graphicsLayer` block, so the layer re-renders without recomposing the screen. `BlockActivity` Back/haptic wiring untouched (closes delta 7's limit 7; the modifier already sits on the waiting escape and, after F1, on the typing button for both purposes).

**P7-F11** (pure move; every value byte-identical). New `config/AppConfig.kt` (`com.arjunrana.tokishrine.config`) holds: gate backgrounds (11 drawables), headline templates (7), humour lines (11), the typing word list (see F12), the eight shared pause constants moved out of `BlockRepository.kt` (minutes min/max/step + default, chars max/step, wait-seconds max/step), `AUTO_NOPE_AWAY_SECONDS = 15` (the runtime's `AUTO_NOPE_AWAY_MS` is now derived as seconds × 1000, still 15 000 ms), and `VISIT_PERCENTILE = 75` / `MIN_VISIT_MS = 30 000` / `MIN_VISITS = 3` out of the `StatsLedger` companion. `ChallengeContentSelector` lost its companion and draws from `AppConfig`. Repointed consumers: `BlockRepository`, `ChallengeRuntime`, `StatsLedger`, `StatsDashboard` (its one `MIN_VISITS` read), `CreateFlowScreen`, and tests `CreateFlowStateTest`, `ChallengeRuntimeTest` (pool assertions), `DebugChallengeValuesTest` / `ProductionChallengeValuesTest` (imports only). `data/stats/` was touched only for those constant repoints. Variant ladders/defaults untouched in `src/{debug,release}/…/ChallengeValues.kt`; `stats.md` got the one-line name repoint. `FALLBACK_MS` / `DEDUP_MS` / override bounds stay in `StatsLedger`.

**P7-F12.** `ChallengeContentSelector.passage` keeps the exact-length contract (`require(length >= 3)`, exact character count, single spaces) and changes selection: the word list grew 56 → **214 words** (a superset of the original 56; everyday vocabulary across lengths 3–10, lowercase, distinct, at least 12 words in the smallest class); the fill picks a word **length** uniformly first and then a word of that length (uniform-over-words made word-heavy short classes dominate and forced the same short closings); and a word never directly follows itself — guaranteed by a pinned ≥2-words-per-length invariant, with a defensive fallback so a future singleton list cannot crash the challenge screen.

**Variety evidence** (temporary JVM probe against both algorithms over the 100 fixed seeds used by the test, deleted after the run; old = shipped 56-word list + old greedy, new = shipped): distinct endings at lengths 20/60/150/200: 26→77, 40→71, 38→70, 39→74; distinct ending word-lengths 5→7–8; passages containing an immediate word repeat 15/15/40/45 per 100 → 0/0/0/0.

### Files (15 code/test + docs and records)

| File | Change |
|---|---|
| `config/AppConfig.kt` | **new** — shared lists and constants (P7-F11) + the 214-word list (P7-F12) |
| `challenge/ChallengeContentSelector.kt` | companion removed (pools → AppConfig); length-uniform no-repeat fill (P7-F12) |
| `ui/interruption/TypingChallengeScreen.kt` | header escape removed; shared outlined escape button under Submit (P7-F1) |
| `ui/interruption/EscapeNudge.kt` | damped-sine wiggle replaces placeholder shake (P7-F2) |
| `data/repo/BlockRepository.kt` | 8 shared constants moved to AppConfig; imports; comment |
| `challenge/ChallengeRuntime.kt` | `AUTO_NOPE_AWAY_MS` derived from `AppConfig.AUTO_NOPE_AWAY_SECONDS` |
| `data/stats/StatsLedger.kt` | 3 usual-visit constants moved to AppConfig; imports |
| `data/stats/StatsDashboard.kt` | one `MIN_VISITS` reference repointed |
| `ui/screens/CreateFlowScreen.kt` | 8 imports repointed (use sites unchanged) |
| `test/…/ChallengeContentSelectorTest.kt` | **new** — 5 tests: exact length sweep 3–200+220/350/700, length-2 rejection, no immediate repeats (58 lengths × 30 seeds), varied endings (4 lengths × 100 seeds), word-list invariants |
| `test/…/ChallengeRuntimeTest.kt` | pool assertions repointed to AppConfig (values unchanged) |
| `test/…/CreateFlowStateTest.kt` | imports repointed |
| `testDebug/…/DebugChallengeValuesTest.kt`, `testRelease/…/ProductionChallengeValuesTest.kt` | imports repointed (assertions unchanged) |
| `docs/components/stats.md` | one line: the three usual-visit names now live in AppConfig |

### Checks

| Check | Result |
|---|---|
| `./build.sh assembleDebug testDebugUnitTest testReleaseUnitTest assembleDebugAndroidTest` | BUILD SUCCESSFUL. Result XML read: **251 JVM tests, 0 failures/errors/skips on debug and on release** (246 + 5 new). Only the pre-existing unused-`closeCount` warnings. Instrumented sources **compiled only, not executed**. |
| Mutation A (previous-word exclusion removed) | `passagesNeverRepeatAWordImmediately` FAILED (5 tests, 1 failed). Restored byte-identical. |
| Mutation B (deterministic first-fit fill) | `passageEndingsVaryAcrossManySeeds` FAILED. Restored byte-identical; full suite re-ran 251/0 on both variants. |
| `git diff --check` | Clean. No device, adb, emulator or install; nothing staged or committed. |

### Limits and points for review

1. **Wiggle feel is source-only** (amplitude/duration/swing count); Arjun's visual acceptance lands with the next installed build (P7-S5 challenge visuals).
2. **Pause typing copy reads "Never mind"** (lowercase, as the header did and as the pause waiting escape does); turn-off keeps "Never Mind". The PRD says "matching the turn-off layout" — layout and control are matched, copy stays purpose-based via `delayEscapeLabel`. Flag for Arjun if he wants identical capitalization.
3. The two variant `ChallengeValues.kt` header comments still name `BlockRepository.kt` as the home of the shared values (now `AppConfig.kt`); both files were left byte-untouched to preserve delta 5.
4. The endings thresholds (≥15 distinct endings, ≥4 ending lengths per 100 seeds) guard against severe collapse (a forced-ending regression fails them, per mutation B); measured 70–77 distinct endings. Word-list edits can shift absolute numbers; the thresholds leave wide margin.
5. No schema, migration or Stats behavior change: P7-D evidence is unaffected by this delta.
6. **Not installed and not committed.** Retests once installed: P7-S5 (wiggle + bottom Never mind) and the retained typing-challenge checks; debug-build passages now draw from the 214-word list.

### Owner-directed P7-F12-1 repair (Codex, 8 October 2026)

After the scoped delta-9 review found that six original words were absent, Arjun directed Codex to add them without additional tests. `AppConfig.TYPING_WORDS` now includes `cedar`, `cloud`, `fern`, `forest`, `valley` and `wren`, replacing six newly added words of matching lengths. The list remains 214 unique lowercase words with the same length-class counts; a direct source comparison against HEAD confirms the original 56 are a subset. No selector logic, tests or other app files changed. `AppConfig.kt` SHA-256: `dc09d528c4c757c68d6ec5919ac6ed9c63c3bcd7d22b35a4c4262f267fbbe4e4`. The 251/0 builder results above precede this repair; no tests or device work were run for it. REVIEW preserves the original FAIL and records the source-verified resolution separately.

### Paste-ready reviewer prompt (different model/session — Codex recommended)

> Read AGENTS.md, then CURRENT, WORKFLOW and TS-P7-stats-feedback-final (TASK 8 October rows P7-F1, F2, F11, F12; HANDBACK "GLM delta 9"). Authority: PRD §17 "Phase 7 owner-experience addendum — 8 October 2026" (Never mind at the bottom, Back no longer suspends/wiggle, Config, Typing passages). Role: scoped reviewer of GLM delta 9 only (different model/session from the implementer). Deltas 5, 6, F-A and F-B are separate uncommitted work: re-inspect only where delta 9 touches them (constant repoints in BlockRepository/ChallengeRuntime/StatsLedger/StatsDashboard, imports in the variant value tests, selector references in ChallengeRuntimeTest). Check: P7-F1 pause escape is the reused full-width outlined button under Submit with Submit still primary and disabled until length, header control removed; P7-F2 changes only EscapeNudge.kt internals (same modifier API, no BlockActivity/Back/haptic change); P7-F11 is a pure move — every moved value identical (backgrounds/headlines/humour, 8 pause constants, auto-nope 15 s, 75th percentile/30 s/3 visits), variant ChallengeValues.kt untouched, data/stats touched only for repoints; P7-F12 keeps the exact passage-length contract while adding the length-uniform no-immediate-repeat fill, the 214-word list is a superset of the old 56, and the five new tests are additions (the two repointed ChallengeRuntimeTest assertions keep their values). Weigh handback limits 1–5 (wiggle is source-only, pause copy capitalization, stale variant-file comments, endings thresholds). Run non-device checks only for concrete open questions (`./build.sh testDebugUnitTest testReleaseUnitTest`); no adb/device/emulator/install, no commit. Record PASS / PASS WITH NOTES / FAIL in REVIEW.md for this exact submission and update CURRENT.

## GLM delta 10 — P7-F13/F6/F7/F10 Stats UI and P7-F5 Add more, 8 October 2026

- **Author/role:** GLM 5.3 (ZCode), implementer for the GLM UI part 2 of 2 per Arjun's session prompt, on top of the F-B data API (unchanged apart from the two assigned field removals). Authority: PRD §17 "Phase 7 owner-experience addendum — 8 October 2026"; design references `design/screens/stats-redesign/` (S1 v2, S2 v2, S7, S8 8 October files) and [theme-ui](../../../docs/components/theme-ui.md); F-B REVIEW owner resolution (Add more during an active pause shares it).
- **Exact base:** the working tree after GLM delta 9 + the owner-directed P7-F12-1 word-list repair (CURRENT records the resolution; HEAD `70bd8e8` plus the preserved uncommitted deltas 5, 6, F-A, F-B and delta 9). Nothing staged or committed by this session.
- **Status:** implemented, self-verified, **ready for review**.

### What changed

**P7-F13 (S1 v2 layout).** `StatsScreen.ReadyContent` rebuilt: **Today hero** — "Time saved today" label + ⓘ, `todaySavedMs` at 68sp with a "today" suffix, "X saved so far" (week total), then "Screen time X today" from `todayScreenMs` (hidden when null; the `usageAvailable == false` neutral note is unchanged; no "vs your usual" anywhere). **Today tiles** — equal-size `todayAttempts` and "Nope rate today ⓘ" with its bar ("—" and an empty bar when `todayAttempts == 0`). **THIS WEEK card** — "Avg time saved `Xm / day`" (`savedPerDayMs`) and "Nope rate `N% · N of M`" (em dash on zero attempts). **App rows** — "N nopes this week" plus the S7 visit chip in a fixed-height 5×60dp box that scrolls on its own (bottom fade kept); both charts and the Recalibrate card are untouched. The old Attempts/day tile is removed together with `StatsDashboard.attemptsPerDay` and the dashboard's `screenDailyMs` (the persisted `stats_state.screenDailyMs` column stays; no schema change — v5 stands and P7-D evidence is unaffected).

**P7-F6 UI.** The row chip reads `~6m/visit ✎` (measured, pencil) or `12m/visit · yours` (user-set, accent, no pencil); tapping anywhere on the row opens the S8 sheet "<App> · time per visit": "How long does a typical visit last for you? We measured Xm." / "We assume 10m." on `usesFallback`, a ±stepper with a large value bounded to `StatsLedger.OVERRIDE_MINUTES_MIN..MAX` (1..120), 3/5/10/15/20m presets, the live "N nopes × Xm" … "X saved this week" pair re-computed as `nopes × minutes`, **Save** → `setVisitOverride`, **"Use measured (Xm)"** → `clearVisitOverride` (both dismiss on success; a false return keeps the sheet open with an inline note). The selection survives recreation as a package name and re-resolves against the latest dashboard. The pencil glyph is `.ph-pencil-simple` (0xe3b4) verified against the bundled font's cmap and the same Phosphor 2.1.1 release as the shared `Ph` table.

**P7-F7.** The Time saved sheet uses the S2 v2 copy with generous spacing (18dp rhythm): "Each nope skips a visit. We count the time that visit usually takes.", the Nopes × Usual visit length = Time saved chip row, "Usual visit length comes from your 7 days before Toki. You can set your own per app." The "Tried again within 5 minutes…" line and the old "How we get…" block are gone.

**P7-F10.** `blocksOn == false` renders a small "No blocks are on" note under the hero lines on the Ready dashboard; the S6 NoBlocks state is untouched.

**P7-F5.** The detail APPS & SITES row shows **Add more** while the block is ON (Edit while OFF). It opens the new standalone `AddMoreScreen` ("What should this cover?", no step indicators, no Next, `Route.AddMore` codec-encoded and round-trip-pinned): the create-flow contents-step pieces are reused — `SiteAddSection` (domain validation/ownership copy extracted verbatim), `AppSearchPane` (now parameterized, with `lockedPackages` showing existing targets as "Added" and inert), and `SelectedRow` (null `onRemove` renders a lock glyph). Existing targets are locked; additions persist across recreation as names (savers); **"Confirm and Save Block"** calls `BlockRepository.addTargets` with the full selection (disabled until the block's own targets load, so a pre-load tap can never send an incomplete selection), `ConflictingOwnershipException` maps to the inline "Already added to a block" copy, `TargetRemovalException` is unreachable by construction (defensively inside a generic failure branch that saves nothing), **"Never mind"** and Back return to detail with nothing saved. No §10 event exists for this flow.

### Files (13 code/test + docs and records)

| File | Change |
|---|---|
| `ui/screens/StatsScreen.kt` | S1 v2 ReadyContent, F10 note, S2 v2 sheet, S8 visit sheet, chip, stepper/presets |
| `ui/screens/StatsPresentation.kt` | row model + chip/sheet/today/N-of-M mapping; `screenTimeLine` retired |
| `data/stats/StatsDashboard.kt`, `StatsRepository.kt` | assigned removals only: `attemptsPerDay`, dashboard `screenDailyMs`, calculator param + call site |
| `ui/screens/AddMoreScreen.kt` | **new** — the add-only contents screen |
| `ui/screens/CreateFlowScreen.kt` | `SelectedRow` internal + locked variant; `AppSearchPane` parameterized (+`lockedPackages`); `SiteAddSection` extracted; call sites |
| `ui/screens/BlockDetailScreen.kt` | Add more action on the APPS & SITES row (ON state) |
| `ui/navigation/Routes.kt`, `MainActivity.kt` | `Route.AddMore` + codec + wiring |
| `test/…/StatsPresentationTest.kt` | 8 → 11 tests: chip text/pencil, measured/assumed line, sheet init minutes, live lines, N-of-M, today line (replacing the retired screen-time-per-day case) |
| `test/…/data/stats/StatsRedesignTest.kt` | calculator call sites lose the removed argument; the `screenDailyMs` assertion retires with the field (assertions otherwise unchanged) |
| `test/…/ui/navigation/RouteCodecTest.kt` | `AddMore` round-trip samples + damaged `addMore:` inputs |
| `androidTest/…/ui/AddMoreScreenTest.kt` | **new**, compiled only — locked rows, save-only-additions on an ON block, Never-mind-nothing, mapped ownership copy |
| `docs/components/stats.md`, `docs/components/blocks-targets.md` | contracts updated for the implemented layout/sheet/Add more |

### Checks

| Check | Result |
|---|---|
| `./build.sh assembleDebug testDebugUnitTest testReleaseUnitTest assembleDebugAndroidTest` | BUILD SUCCESSFUL. Result XML read: **254 JVM tests, 0 failures/errors/skips on debug and on release** (251 + 3 net new presentation tests). Instrumented sources **compiled only, not executed**. |
| `git diff --check` | Clean. No device, adb, emulator or install; nothing staged or committed. |
| F-B data API | unchanged apart from the two assigned field removals (`attemptsPerDay`, dashboard `screenDailyMs`); the persisted `stats_state.screenDailyMs` column and schema v5 are untouched. |

### Limits and points for review

1. **Visual styling nuances vs the exports** (owner visual pass pending): the 8 October exports set section labels in all-caps and show "1h 36m saved this week"; the implementation keeps the established sentence-case label typography and the §9 duration form ("96m" below 100 minutes) used everywhere else on the screen. Save uses the app's PRIMARY (accent-outlined) variant, which matches the S8 export's outlined-accent button.
2. **No new §10 events**: the visit sheet's open and the override set/clear log nothing (F-B handback point 3); `stats_info_opened` still fires only for the two ⓘ info sheets; Add more logs nothing (§10 has no event for it).
3. The AddMoreScreen helper line "Apps and sites already in this block stay in. You can only add." is new copy not in the PRD — flag for Arjun.
4. **AddMoreScreenTest is compiled, not executed** (no device/emulator authorized); it reuses the debug `CreateFlowTestActivity` host. Execution, like the F-B instrumented cases, needs Arjun's authorization.
5. The app box is exactly 5 × 60dp (the export's "about five rows"); with fewer apps the box keeps its height with empty space below the last row, as in the export.
6. Retests once installed: P7-S3/S6/S7 re-baselined checks plus the new chip/sheet/Add-more flows; no schema change, so P7-D evidence is unaffected by this delta.

### Correction after review (8 October 2026)

The test summary above omitted one removal: `StatsPresentationTest.weekdayLabelsAreRollingNarrowLettersWithTodayLast` was deleted (P7-G10-1), so the true count was 251 − 1 + 4. Owner-directed repair R1 (Claude) restored it and changed the nope-rate sheet to today's figures (P7-N12); suite now 256/0 per variant. See REVIEW.

### Paste-ready reviewer prompt (different model/session — Codex recommended)

> Read AGENTS.md, then CURRENT, WORKFLOW and TS-P7-stats-feedback-final (TASK 8 October rows P7-F5, F6, F7, F10, F13; HANDBACK "GLM delta 10"; for the data APIs see HANDBACK "Claude delta 8 — submission F-B" points 2–5 and the F-B owner resolution in REVIEW). Authority: PRD §17 "Phase 7 owner-experience addendum — 8 October 2026" (Stats layout update, usual-visit override, Blocks — Add more sections) with design references S1 v2/S2 v2/S7/S8 in design/screens/stats-redesign/. Role: scoped reviewer of GLM delta 10 only (different model/session from the implementer); deltas 5, 6, F-A, F-B and 9 are separate uncommitted work — re-inspect only where delta 10 touches them (the two assigned field removals in StatsDashboard/StatsRepository and the CreateFlowScreen refactors must not change wizard behavior). Check: the S1 v2 arrangement consumes exactly the F-B fields (today hero/tiles, THIS WEEK N-of-M, chip values, "—" rules, hidden null screen time, blocksOn note); the S8 sheet's bounds/presets/live line and set/clear override wiring with failure inline and no events; the S2 v2 copy replaces the removed five-minute line; Add more is add-only end to end (locked existing targets, full selection to addTargets, ownership copy mapping, unreachable removal, Back/Never mind save nothing, works while ON) and the shared StepContents pieces are unchanged for the wizard; the changed tests are supersessions/additions, not weakening (the retired screenDailyMs assertion left with its field). Weigh handback limits 1–5 (export styling vs §9 form, no events, new helper copy, compiled-only instrumented test, fixed box height). Run non-device checks only for concrete open questions (`./build.sh testDebugUnitTest testReleaseUnitTest`); no adb/device/emulator/install, no commit. Record PASS / PASS WITH NOTES / FAIL in REVIEW.md for this exact submission and update CURRENT.

## Claude delta 11 — 9 October Part 1: P7-F16 auto-nope → phone home, P7-F17 celebration, P7-F18 "Nope, not now", P7-F19 haptic, 9 October 2026

- **Author/role:** Claude (Opus 5.5), senior implementer assigned by Arjun (approved plan, 9 October). Authority: PRD §17 "Phase 7 owner-feedback addendum — 9 October 2026"; TASK rows P7-F16–F19.
- **Exact base:** `70bd8e8` + uncommitted deltas 5, 6, F-A, F-B, 9 (+P7-F12-1) and 10 (+R1), all preserved. Source identity: [D11-SUBMISSION.sha256](D11-SUBMISSION.sha256) (20 files). Nothing staged or committed; not installed.
- **Status:** implemented, self-verified, **ready for review**. GLM's Part 2 (P7-F20–F28) follows; prompt below.

### What changed

- **P7-F16:**
  - `ChallengeEffect.TurnOffAbandoned` gains `auto` (default false); `ChallengeRuntime.expireAway` sets it.
  - In `BlockActivity`, the pause auto-nope (quiet walk-away) and an `auto` turn-off now call the existing `returnToHome()` (HOME intent + finish) instead of `finish()`. This covers the timer path (screen off) and the late-return path. The button escape on turn-off still uses `finish()` and returns to the block.
- **P7-F17:**
  - `AppConfig` holds `CELEBRATION_TITLES` (text → image, with `onlyOnDailyCount = 4` for "FOUR TIMES") and `CELEBRATION_SUBTITLES` (one with a `{minutes}` token).
  - The new pure `CelebrationPicker` filters eligible entries and picks from a seeded `Random`. The minutes line is dropped without a saving; it uses "1 minute" / "N minutes" and "N wins today".
  - New read-only `ChallengeRepository.nopeVisitMs(sessionId)` (+ DAO `overrideFor`, a query only, no schema change). It returns the user's override for that app, else the frozen saving, or null for a deduped or unrecorded nope.
  - `BlockActivity` reads it after the walk-away write (a failure only drops the minutes line). It keeps the seed and minutes in saved state, so a recreation shows the same pick.
  - `WalkAwayMomentScreen` is rebuilt to the reference on Nocturne tokens: accent-100 background, a 208 dp circular image in an accent-200 disc, a 30 sp bold title, a grey subtitle and the 🏆 pill. Status and navigation icons turn dark while it is shown. Tap-to-continue and the 10 s auto-dismiss are unchanged.
  - Six placeholder vectors `res/drawable-nodpi/celebrate_{cat,dicaprio_toast,michael_scott,elmo_fire,victory_dance,touch_grass}.xml`.
- **P7-F18:** `delayEscapeLabel` returns "Nope, not now" for both purposes (typing and waiting). The gate's "Not now" is unchanged.
- **P7-F19:** `BlockHaptics.nudge` uses `EFFECT_CLICK` instead of `EFFECT_TICK`.

### Tests changed (supersession/addition)

- `InterruptionModelsTest.copySelectorsDistinguishPauseFromTurnOff`: the two label assertions move to the owner's new copy.
- `ChallengeRuntimeTest`: auto turn-off cases assert `auto == true`; the button escape asserts `auto == false`.
- **New:** JVM `CelebrationPickerTest` (3: fourth-nope-only title, real or absent minutes line, title–image pairing with seeded reproducibility and coverage). Android `ChallengeRepositoryTest.nopeVisitIsTheOverrideElseTheFrozenSavingAndNullForADedupedNope`, compiled only.

### Checks

| Check | Result |
|---|---|
| `./build.sh assembleDebug testDebugUnitTest testReleaseUnitTest assembleDebugAndroidTest` | BUILD SUCCESSFUL. **259 JVM tests, 0 failures/errors/skips on debug and on release** (256 + 3). Android tests compiled only. |
| `git diff --check` | Clean. No device, adb, emulator or install. |

### Limits and points for review

1. **HOME from the background:** starting the home screen while Toki is hidden (screen-off expiry) relies on Android's background-activity-start exemption for an app with a bound accessibility service. If the OEM refuses, `runCatching` swallows it and the old `finish()` behavior remains. Arjun's device check decides.
2. **Images are placeholders.** Arjun replaces each `celebrate_*.xml` with a same-named `.webp`/`.png`/`.jpg`. Images are cropped to a circle. Rights/licensing is his call.
3. **The light screen** is a deliberate dark-theme exception per the owner reference. Confetti decorations from the reference were not drawn. The bold title is synthesized because only Inter Regular/Medium are bundled.
4. **The "N minutes" value** follows the Stats valuation (override, else frozen measured/fallback). A site nope uses its browser's value.
5. No new events; the `walk_away` payloads are unchanged.

### Owner images added (9 October 2026)

- **Celebration images:** Arjun replaced the six `celebrate_*` placeholders with images (PNG data under `.jpg` names, 468–1066 px; decoded by content, like `sys_block_8`–`11`).
- **Gate backgrounds:** he added `sys_block_12`–`15`. Claude registered them in `AppConfig.GATE_BACKGROUNDS` (11 → 15) and updated the pinned count in `ChallengeRuntimeTest.ownerApprovedCopyAndAssetPoolsAreComplete`.
- **Checks:** rebuilt with all ten files in the APK; 259/0 JVM per variant. The [D11-SUBMISSION.sha256](D11-SUBMISSION.sha256) hashes for the placeholder `.xml` files are superseded by these images.

### Owner restyle of the celebration screen (9 October 2026, Claude at Arjun's direction)

- **Arjun's request:** a bigger meme image, a ground closer to the app theme (a lighter purple), consistent type and spacing, and no background behind "N wins today".
- **Change, `WalkAwayMomentScreen.kt` only:**
  - the ground is accent-300 `#D2CEFD` (was accent-100);
  - the image is the screen width, up to 300 dp, in a 6 dp accent-400 ring (was 208 dp);
  - the title uses bundled Inter Medium at 28/34 sp (was a synthesized bold 30 sp), matching the block screen's headline;
  - the subtitle is 15/23 sp in accent-700, and the wins line is a plain 15 sp Medium "🏆  N wins today";
  - "tap to continue" is 12 sp, and the spacing is 28/10/16 dp.
- **Unchanged:** tap-to-continue, the 10 s auto-dismiss, the picker and the minutes line.
- **Checks:** 262/0 JVM per variant; Android tests compiled; installed (APK `f8133036…`). The file's new SHA-256 is `e783ef6fea4283bbf3089d03be214b12ecabbdbed0ae44a0b471e6a1b83eb70a`, which supersedes its D11 line.

### Paste-ready reviewer prompt (different model/session — Codex recommended)

> Read AGENTS.md, CURRENT, WORKFLOW and TS-P7-stats-feedback-final (TASK rows P7-F16–F19; HANDBACK "Claude delta 11"). Authority: PRD §17 "Phase 7 owner-feedback addendum — 9 October 2026". Role: scoped reviewer of Claude delta 11 only (verify `shasum -a 256 -c coordination/tasks/TS-P7-stats-feedback-final/D11-SUBMISSION.sha256` from the repo root). Check:
> - auto-nope endings (pause quiet walk-away; turn-off `auto`) call `returnToHome()` on both the timer and late-return paths, while the button escape still finishes to the block;
> - `nopeVisitMs` valuation (override, else frozen saving; null for deduped/unrecorded) and that the DAO change is query-only;
> - `CelebrationPicker` eligibility and the recreation stability (seed + minutes in saved state);
> - the label/haptic changes and that the test edits are supersessions.
>
> Weigh limits 1–5. Run non-device checks only for concrete questions. No adb/device/install/commit. Record PASS / PASS WITH NOTES / FAIL in REVIEW.md and update CURRENT.

### Next — GLM Part 2 prompt (paste after this delta is reviewed, or alongside if Arjun prefers)

> Read AGENTS.md, then CURRENT, WORKFLOW and TS-P7-stats-feedback-final (TASK rows P7-F20–F28 from the 9 October section; REVIEW "GLM delta 10" + R1; HANDBACK "Claude delta 11" for context only). Authority: PRD §17 "Phase 7 owner-feedback addendum — 9 October 2026"; designs `design/screens/stats-redesign/` S1 v2, S7, S8 (8 October files); theme-ui contract. Role: GLM implementer, UI only. Preserve all other uncommitted work, including Claude delta 11. Do not touch `data/`, `BlockActivity`, the challenge runtime, `ui/interruption/` or `config/AppConfig.kt`.
>
> 1. **Stats app list (P7-F20, currently broken on device: subtitle clipped, chip text invisible):**
>    - Remove the fixed 60 dp row height; rows wrap their content.
>    - The box is `heightIn(max ≈ 5 rows)` and scrolls on its own, with no empty space when there are few apps.
>    - Row layout: name, then "N nopes this week ·" + chip, saved time on the right.
>    - The chip is outlined (1 dp border, 8 dp radius) and reads `~Xm/visit ✎` for every app.
>    - Remove "· yours" and any visual difference for user-set values.
>    - The whole row stays tappable.
> 2. **Visit sheet (P7-F21, S8):**
>    - The stepper sits inside a tinted rounded box, with a large number (~56 sp) + "min".
>    - Presets 3/5/10/15/20 fill the full width in equal widths (`weight(1f)`).
>    - The live line stays as now.
>    - Save is an accent-outlined CTA; "Use measured (Xm)" is a secondary CTA. Both follow item 6.
>    - Keep bounds, wiring, inline failure and no events.
> 3. **Today tiles (P7-F22):**
>    - Equal size: `Row(Modifier.height(IntrinsicSize.Min))`, each tile `weight(1f).fillMaxHeight()`.
>    - Number at the top, label below, and the nope-rate bar at the bottom of the right tile, as in S1 v2.
> 4. **Hero and week card (P7-F23):**
>    - Remove the "X saved so far" line.
>    - The THIS WEEK nope rate shows only "N%" ("—" at zero attempts). Update `weekNopeRateLine` and its test.
> 5. **Time saved info sheet (P7-F24):** the first sentence becomes "Each ‘nope’ saves you a visit." The rest is unchanged.
> 6. **CTA consistency (P7-F25):** paired CTAs match the challenge screens.
>    - Full width, 50 dp tall, 10 dp radius, 15 sp medium label.
>    - 12 dp gap between stacked CTAs and ≥16 dp from the content above.
>    - Apply to Add more (Confirm and Save Block / Never mind), the visit sheet, the Stats sheets' "Got it" and the Recalibrate card.
>    - Reuse `NocturneButton` with these values, or one small shared helper; don't restyle the challenge screens.
> 7. **Settings (P7-F26/F27):**
>    - Remove the Theme row.
>    - "Supported browsers" opens a new screen listing every supported browser by display name. Read them from the bundled `assets/detection_config.json` through the existing detection-config loader; no hardcoded list.
>    - Derive the row subtitle from the same list.
>    - Add `Route.SupportedBrowsers` with codec and a round-trip test.
> 8. **Permission checklist (P7-F28):** the content scrolls (`verticalScroll`), so every row, including Usage Access under "Better experience", is reachable on small screens and at large font. Bottom actions stay reachable.
>
> Add or adjust JVM tests for the changed presentation helpers (chip text, week line, browser list mapping); supersede, never weaken, and list every removed or changed test in the handback. Run `./build.sh assembleDebug testDebugUnitTest testReleaseUnitTest assembleDebugAndroidTest` (Part 1 baseline: 259/0 per variant). No adb, device, install or commit. Write HANDBACK "GLM delta 12" with files, checks and limits. Update TASK and CURRENT, with a paste-ready reviewer prompt (Claude reviews).

## GLM delta 12 — 9 October Part 2: P7-F20–F28 Stats/Settings/permission/CTA UI, 9 October 2026

- **Author/role:** GLM 5.3 (ZCode), implementer for Part 2 per Arjun's session prompt (the HANDBACK delta 11 "Next — GLM Part 2" list). Authority: PRD §17 "Phase 7 owner-feedback addendum — 9 October 2026"; designs S1 v2/S7/S8 (8 October files); theme-ui contract.
- **Exact base:** the working tree after Claude delta 11 (HEAD `70bd8e8` plus the preserved uncommitted deltas 5, 6, F-A, F-B, 9 (+P7-F12-1), 10 (+R1) and 11, all preserved and untouched outside the files below). Source identity: [D12-SUBMISSION.sha256](D12-SUBMISSION.sha256) (14 files, verified 14/14). Nothing staged or committed by this session.
- **Status:** implemented, self-verified, **ready for review**.

### What changed

- **P7-F20 (app list):** rows wrap their content (the fixed 60 dp height and the 44 dp inner constraint are gone); the box is `heightIn(max = 5 × 60dp)` and scrolls on its own, collapsing to the list when there are few apps; row = name, "N nopes this week ·" + chip (`appRowSubtitle` now carries the "·"), saved time right; the chip is outlined (1 dp `neutral.step700` border, 8 dp radius) and reads `~Xm/visit ✎` for **every** app (`visitChipLabel(effectiveVisitMs)`, one arg; "· yours", the accent tint and the `chipShowsPencil` field are gone); the whole row stays tappable; the bottom fade is kept.
- **P7-F21 (visit sheet, S8):** the stepper sits in a tinted (`accentRamp.step900`) rounded box with the minutes at 56 sp + "min"; presets 3/5/10/15/20 fill the width in equal widths (`weight(1f)`); the live line, `initialSheetMinutes` bounds, `setVisitOverride`/`clearVisitOverride` wiring, inline failure and no-events rule are unchanged; Save is the accent-outlined (PRIMARY) CTA and "Use measured (Xm)" the secondary CTA.
- **P7-F22 (Today tiles):** `Row(Modifier.height(IntrinsicSize.Min))`, each tile `weight(1f).fillMaxHeight()`; number first, label below, the nope-rate bar pinned to the right tile's bottom by a `weight(1f)` spacer.
- **P7-F23 (hero/week):** the "X saved so far" line is removed; `weekNopeRateLine(nopeRatePercent, attempts)` returns "N%" only ("—" at zero attempts; the `nopes` parameter left with the copy).
- **P7-F24:** the Time saved sheet opens "Each 'nope' saves you a visit." (curly quotes as in the addendum); the rest of the S2 v2 copy, formula chip row and spacing are unchanged.
- **P7-F25 (CTAs):** new `NocturneCtaButton` in `ui/components/NocturneUi.kt` (full width, 50 dp, 10 dp radius via a new optional `cornerRadius` on `NocturneButton` — null keeps the 8 dp token for every existing call site, 15 sp medium) applied to Add more (Confirm and Save Block / Never mind), the visit sheet (Save / Use measured), the Stats sheets' "Got it" and the Recalibrate card, with a 12 dp gap between stacked pairs and ≥16 dp below the content above. The challenge screens are untouched; variants keep their established PRIMARY/SECONDARY roles.
- **P7-F26/F27 (Settings):** the Theme row is removed; "Supported browsers" opens the new `Route.SupportedBrowsers` screen (`SupportedBrowsersScreen.kt`) listing every browser in the bundled `assets/detection_config.json` by display name, read through `app.detectionConfigLoader` (the existing loader; hoisted once in MainActivity, shared by the row and the screen); the Settings subtitle derives from the same list (`supportedBrowsersSubtitle`); `RouteCodec` encodes `supportedBrowsers` with a round-trip sample and a damaged-input case.
- **P7-F28:** the permission checklist body (heading, progress, both groups) scrolls (`verticalScroll`); Continue stays pinned above the navigation bar, so Usage Access is reachable on small screens and at large font.

### Files (12 code/test + 2 docs)

| File | Change |
|---|---|
| `ui/components/NocturneUi.kt` | `cornerRadius` param on `NocturneButton`; new `NocturneCtaButton` |
| `ui/screens/StatsScreen.kt` | P7-F20–F25: rows/box/chip, S8 stepper/presets/CTAs, intrinsic tiles, hero line, sheet copy, Got it, Recalibrate card |
| `ui/screens/StatsPresentation.kt` | `visitChipLabel` one-arg "~Xm/visit"; `appRowSubtitle` "·"; `weekNopeRateLine` percent-only; `chipShowsPencil` removed |
| `ui/screens/AddMoreScreen.kt` | CTA pair per spec (12 dp gap, ≥16 dp above) |
| `ui/screens/SettingsScreen.kt` | Theme row removed; browsers row opens the screen; derived subtitle |
| `ui/screens/SupportedBrowsersScreen.kt` | **new** — the config-driven browser list + pure `browserDisplayName`/`supportedBrowsersSubtitle` |
| `ui/screens/PermissionChecklistScreen.kt` | scrollable body, pinned Continue |
| `ui/navigation/Routes.kt`, `MainActivity.kt` | `Route.SupportedBrowsers` + codec + wiring (loader state hoisted once) |
| `test/…/StatsPresentationTest.kt` | 2 tests changed (see below); count unchanged at 13 |
| `test/…/SupportedBrowsersPresentationTest.kt` | **new**, 3 tests |
| `test/…/ui/navigation/RouteCodecTest.kt` | samples + damaged-input additions (no new method) |
| `docs/components/stats.md`, `docs/components/navigation-permissions.md` | contract lines updated for the 9 October copy and the Settings/checklist changes |

### Tests changed (no test deleted)

- `StatsPresentationTest.appRowsReadNopesThisWeekPlusTheS7VisitChip` — changed: subtitle expectations gain the "·" suffix; every chip asserts `~Xm/visit` including the override (`~12m/visit`); the two `chipShowsPencil` assertions left with the field.
- `StatsPresentationTest.weekNopeRateLineReadsPercentThenNOfMWithAnEmDashForNoTries` → renamed `weekNopeRateLineReadsPercentOnlyWithAnEmDashForNoTries` with percent-only expectations (supersedes the "N of M" form per the 9 October addendum; adds a `0%` non-zero-attempts case).
- **New** `SupportedBrowsersPresentationTest`: all nine bundled packages map to product names, the unknown-package fallback, and the subtitle derivation (3, ≤3, 1).
- `RouteCodecTest`: `Route.SupportedBrowsers` in the round-trip/re-encode samples; `"supportedbrowser"` in the damaged list (`"supportedBrowsers:"` decodes to the object route, like `settings:`, so it is not damaged).

### Checks

| Check | Result |
|---|---|
| `./build.sh assembleDebug testDebugUnitTest testReleaseUnitTest assembleDebugAndroidTest` | BUILD SUCCESSFUL. Result XML read: **262 JVM tests, 0 failures/errors/skips on debug and on release** (259 baseline + 3 new). Instrumented sources **compiled only, not executed**. |
| `git diff --check` | Clean. No device, adb, emulator or install; nothing staged or committed. |
| Scope | No change under `data/`, `BlockActivity`, the challenge runtime, `ui/interruption/` or `config/AppConfig.kt`; deltas 5, 6, F-A, F-B, 9, 10, 11 preserved. |

### Limits and points for review

1. **Browser display names are a presentation table.** The config declares only package names, and several are not derivable from the product (`com.microsoft.emmx` → Microsoft Edge, `com.sec.android.app.sbrowser` → Samsung Internet), so `BROWSER_DISPLAY_NAMES` dresses the nine current entries and unknown future entries fall back to the capitalized last segment (never a raw dotted ID). The *set* of browsers shown is 100% config-driven — adding a browser to the JSON lists it (with the fallback name) with no code change. Flag for Arjun: approve the nine names.
2. **Derived subtitle wording:** the row now reads "Chrome, Chrome Beta, Samsung Internet + 6 more" — the first three **config-order** names — replacing the old hardcoded "Chrome, Firefox, Samsung Internet + 6 more". Same shape, different (derived) names.
3. **Degrade paths:** a corrupt/unreadable asset leaves `supportedBrowsers` null — the Settings row hides its subtitle and the screen shows its frame without the list (the BatteryInstructions precedent). No error surface was added.
4. **Variants kept:** "Got it" and the Recalibrate card keep their neutral SECONDARY outline (the addendum fixes measurements, not variants; S2 v2 draws "Got it" neutral-outlined). Note: the S1 v2 export draws the Recalibrate button accent-outlined — Arjun can ask for PRIMARY if he prefers the export.
5. **Recalibrate confirm dialog untouched:** its side-by-side Recalibrate/Cancel pair is not one of the assignment's four CTA surfaces and keeps the 42 dp/8 dp dialog style.
6. **Visit sheet height:** the sheet grew (~+40 dp: 50+12+50 CTAs, bigger stepper); sheets are not scrollable, so very small screens or extreme font scale may crop it — covered by Arjun's P7-S7 device check.
7. **List cap at font scale:** the 300 dp cap is "about five rows" at default size; rows now wrap, so at large font scale five rows exceed the cap and the box scrolls (the assigned behavior).
8. Retests once installed: P7-S3/S7 (rows/chip/sheet), the Settings/checklist flows, and the retained P7-N13 clipping check on device. No schema change, so P7-D evidence is unaffected.

### Repair P7-G12-1 (+ P7-N16, P7-N17) — Claude (Opus 5.5), 9 October 2026, at Arjun's direction

- **Change, `StatsScreen.kt` list block only:**
  - the list's scroll state is hoisted, and the bottom fade is drawn only while `canScrollForward`, so it never covers the last row (P7-G12-1);
  - the row subtitle has `weight(1f, fill = false)`, so the chip keeps its width at large font and the subtitle wraps instead (P7-N16);
  - `APP_ROW_APPROX_HEIGHT` is 72 (the real wrapped-row height), so the cap shows about five rows (P7-N17).
- **Checks:** `./build.sh assembleDebug testDebugUnitTest testReleaseUnitTest assembleDebugAndroidTest` → BUILD SUCCESSFUL, **262/0 per variant** (results written after the edit). Android tests compiled only. `git diff --check` clean.
- **Identity:** the D12 `StatsScreen.kt` line is now `fbdf69e6…`. Self-verified by the reviewer; no separate re-review.
- **Installed** at Arjun's request (OWNER-CHECKS 9 October install record).

### Paste-ready reviewer prompt (Claude reviews)

> Read AGENTS.md, then CURRENT, WORKFLOW and TS-P7-stats-feedback-final (TASK rows P7-F20–F28 in the 9 October section; HANDBACK "GLM delta 12"; REVIEW "GLM delta 10" + R1 for the code being corrected; HANDBACK "Claude delta 11" is context only). Authority: PRD §17 "Phase 7 owner-feedback addendum — 9 October 2026"; designs `design/screens/stats-redesign/` S1 v2, S7, S8; theme-ui contract. Role: scoped reviewer of GLM delta 12 only (different session from the implementer); verify `shasum -a 256 -c coordination/tasks/TS-P7-stats-feedback-final/D12-SUBMISSION.sha256` from the repo root; deltas 5, 6, F-A, F-B, 9, 10 and 11 are separate preserved work — re-inspect only where delta 12 touches them. Check: the app list wraps (no fixed heights), the heightIn cap scrolls and collapses, every chip reads `~Xm/visit ✎` outlined with no user-set distinction, rows stay tappable; the S8 stepper/presets keep bounds, wiring, inline failure and no events, with the CTA pair; the intrinsic-size tiles and the removed hero line; `weekNopeRateLine` percent-only; the sheet's first sentence; `NocturneCtaButton` values (50 dp/10 dp/15 sp, 12 dp gap, ≥16 dp above) on all four assigned surfaces with `cornerRadius` leaving other `NocturneButton` call sites on the 8 dp token and the challenge screens untouched; Settings Theme row gone, the browser list/subtitle genuinely config-driven through `DetectionConfigLoader` (handback limit 1's name table is presentation, not a list), the route codec round trip; the checklist scroll with Continue pinned. Weigh handback limits 1–8. Confirm the test changes are supersessions/additions, not weakening. Run non-device checks only for concrete open questions (`./build.sh testDebugUnitTest testReleaseUnitTest`). No adb/device/emulator/install, no commit. Record PASS / PASS WITH NOTES / FAIL in REVIEW.md for this exact submission and update CURRENT.

## Delta 12A — 10 October owner changes P7-F32/F33 (installed 00:58; recorded retroactively 11 October by Claude Opus 5.5 as architect)

The original implementer session left no handback. This record is reconstructed from the source, file times and the OWNER-CHECKS install bullet.

### What changed (10 October, 00:58)
- **P7-F32:** `StatsLedger.DEDUP_MS` 300 000 → 120 000. The JVM boundary test (`StatsRedesignTest.nopeWindowHasFixedAnchorAndPassResetsIt`) moved to 119 999 / 120 000. Comments in `ChallengeRepository` (`nopeVisitMs`) and `ChallengeRepositoryTest` now say "two minutes".
- **P7-F33:** the `BlockGateScreen` primary button reads "Nope, not now" (was "Not now"). It has no JVM test; the challenge-escape label stays covered by `InterruptionModelsTest`.
- **Data effect:** dedup is decided when a nope is recorded and frozen in `stats_outcome.counted`. Rows written under the five-minute rule keep their flags, so the change affects only nopes recorded after the install. No schema or migration change.
- Files untouched since the install (all file times 10 October 00:58), so the D12A lines for the first five files match the installed source.

### Audit repair (11 October, architect)
- `StatsRepositoryTest.ignoredNopesNeverExtendWindowAndSuccessEndsSequence` used fixed 299 s/1 s gaps written for five minutes. Under two minutes the "ignored" nope counted and "two" was ignored, and the total (4) stayed the same, so the test passed while no longer checking its rule. It is now relative to `DEDUP_MS` (ignored at window − 1 s, "two" at the window), and it asserts each session's counted flag. A stale "five-minute" comment in the site-sequence test is fixed.
- `docs/components/stats.md` names the two-minute window and the frozen-flag rule.

### Checks and identity
`./build.sh testDebugUnitTest testReleaseUnitTest assembleDebug assembleDebugAndroidTest` → BUILD SUCCESSFUL, **262/0 per variant**; Android tests **compiled only** (the repaired test has not run; P7-D2-style execution needs separate authorization). The debug APK is unchanged by the repair (`8a7cbdb9…`, test-only change). Identity: [D12A-SUBMISSION.sha256](D12A-SUBMISSION.sha256) (6 files).

### Limits
- No independent review of P7-F32/F33 or this repair.
- The two-minute window and the friction label were **not** covered by the 10 October device retest. That ran on `f8133036…`, before this install (OWNER-CHECKS 10 October record).

## Claude delta 13 — 10 October retest fixes P7-F29–F31, 10 October 2026

Implementer: Claude (Opus 5.5), at Arjun's direction (routine work given directly to this session). Base: `70bd8e8` plus the uncommitted deltas 5–12 (+ repairs and delta 12A, the 10 October 2-minute/friction-label change), all preserved. Identity: [D13-SUBMISSION.sha256](D13-SUBMISSION.sha256) (8 files; PRD.md excluded because it carries other uncommitted owner edits).

### What changed
- **P7-F29:** `detection_config.json` lists seven browsers; the DuckDuckGo and Vivaldi rows are removed from both the config and `BROWSER_DISPLAY_NAMES`. The subtitle is still derived, so it now reads "Chrome, Chrome Beta, Samsung Internet + 4 more". The Supported browsers column is `verticalScroll`, so large fonts can't clip it. Site blocking no longer triggers in those two browsers.
- **P7-F30:** `DelayCountdownScreen` escape 56 dp/16 sp → 50 dp/15 sp, matching `TypingChallengeScreen.EscapeButton`.
- **P7-F31:** `visitSheetNopesLine` uses "nope" for 1.
- **PRD:** two bullets added to §17 "Owner changes — 10 October 2026"; the Supported browsers heading points to them. The five-minute lines in §9 are already superseded by that addendum and are unchanged.

### Tests changed (owner supersession/addition, none deleted)
- `DetectionAssetsTest`: the pinned map and count are 9 → 7 (renamed `…SevenSupportedBrowsers`).
- `SupportedBrowsersPresentationTest`: bundled list/names 9 → 7, subtitle "+ 4 more", and the three-name example uses Opera instead of Vivaldi.
- `AssetDetectionConfigLoaderTest` (instrumented, compiled only): size 7, DuckDuckGo is now asserted absent.
- `StatsPresentationTest`: added "1 nope × 5m".

### Checks
`./build.sh testDebugUnitTest testReleaseUnitTest assembleDebug assembleDebugAndroidTest` → BUILD SUCCESSFUL. **262 JVM tests, 0 failures/errors/skips per variant.** Android tests compiled only. `git diff --check` clean. Debug APK SHA-256 `8a7cbdb9073d73c9eafd97131d7dde3fe75415a856df029001bda845ed6c83fc`. Not installed or committed.

### Limits
- Self-verified only. The change is small and visual/config, so a separate review is optional (Arjun's call).
- Existing site blocks are unaffected in data; the two dropped browsers simply stop being watched.
- Row 19 measurements were in px. 50 dp is 94 px at the tester's density, matching the typing escape; the 92 px CTAs are the separate `NocturneCtaButton` (also 50 dp per P7-F25; the 2 px gap is probably rounding and was not investigated).
- **Identity overlap:** four D12 files (`StatsPresentation.kt`, `SupportedBrowsersScreen.kt` and their two JVM tests) now fail D12's checksum because delta 13 changes them; D13 holds their current hashes. D11's existing mismatches (AppConfig, ChallengeRepository, celebration drawables, two tests) predate this delta and come from the celebration restyle/images; they are untouched here.

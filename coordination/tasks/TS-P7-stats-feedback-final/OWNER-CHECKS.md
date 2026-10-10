# Owner checks — revised Phase 7

All redesigned Stats **owner experience** checks are **pending**. The only executed evidence is the authorized isolated-emulator P7-D1/P7-D2 runs recorded below — the original run on the pre-N6 tree and the 4 October re-run on the N6 tree — with no owner device touched in either. Record the final GLM build/source hash before testing. The installed 4 October `89ef9e3` build is the old design and cannot accept the redesign. Earlier checklist/evidence is preserved in [the snapshot](submissions/89ef9e3-before-stats-redesign/OWNER-CHECKS.md); old P7-O3–O7 are superseded by P7-S1–S10 below.

## Owner decisions on review notes — 4 October 2026

Arjun's verdicts on P7-N3–N7 (recorded in [REVIEW](REVIEW.md)): **P7-N3** (recalibrated week reads "partial"), **P7-N5** (`usage_access_granted` scope) and **P7-N7** (stale prose hash) — accepted as reviewed, no change. **P7-N4** — chart bar labels must be minutes-only (`140m`, never `1h 40m`): implemented by GLM (HANDBACK delta 2), pending re-review. **P7-N6** — site-block savings attribute to the hosting browser on the app leaderboard: owner-decided behavior change, PRD §17 addendum recorded with all semantics settled, implemented by Claude 4 October (HANDBACK delta 3), pending review. Acceptance note: with a site block enabled, installed supported browsers appear on the app list (0m until used); a site nope adds a saving to that browser's row and an app nope on the same browser within 5 minutes counts once; a completed site challenge's spent time is the next browser visit or "—". Acceptance note for S6/S7: bar value labels render whole minutes in every range; the heavy-day clipping check (former N4 concern) stays part of S6/O21.

Re-review notes (REVIEW, N4 + N6 re-review PASS WITH NOTES): **P7-N8** (every installed supported browser listed while a site block is enabled), **P7-N9** (site passes may leave spent "—" more often) and **P7-N10** (site nope valued at the browser's baseline) — accepted by Arjun 4 October 2026, no change.

## Arjun — experience checks after GLM

| ID | What to test | Steps to follow | Fail conditions | Pass conditions |
|---|---|---|---|---|
| P7-O1 | Upgrade preserves your setup | Install the agreed final build over the existing install without uninstall/clear; open existing blocks | Missing blocks/targets/settings or startup crash | Existing setup retained; schema v3→v4 migration happens transparently |
| P7-S1 | Usage Access is Stats-only | Revoke Usage Access, open Stats, then try an enabled block with Accessibility granted; grant Usage Access from the Stats card | Blocking requires Usage Access; grant doesn't refresh Stats | S5 prompt, blocking works, returning from Settings refreshes Stats |
| P7-S2 | Five permission rows | Visit onboarding checklist and Settings, grant/revoke Usage Access | Wrong progress, Essential categorization or activation gate change | Five real-state rows, Usage Access under Better experience, Accessibility-only gate |
| P7-S3 | Empty versus zero | Open Stats with no enabled block, then enable a block before attempting it | Same empty state in both cases, fake activity or NaN | S6 with none; full zero dashboard with an enabled block; rate is “—” |
| P7-S4 | Nope/push-through sequence | Nope an app, repeat within five minutes, then complete its challenge and nope again | Repeat adds saved time; completion merges into earlier attempt | Repeated nope adds no Stats outcome count/saving; pass is separate; next nope counts. Raw walk-away screen counter can still increase |
| P7-S5 | Suspension | Begin challenge then Home/Back/lock; resume | Suspension adds a nope or loses accepted challenge behavior | No terminal Stats outcome from suspension; existing resume behavior retained. **Superseded 8 October by P7-F3/F4 (HANDBACK delta 7, original expectation preserved here); retest on the build containing it:** Back during a challenge does not leave it (Never mind wiggles, light haptic). Home/app switch/lock and return **within 15 s** → same challenge, typed text cleared, wait restarted, no outcome. Away **15 s or more** → pause: one counted nope (`walk_away` source `auto_away`, valued like a chosen nope, five-minute dedup unchanged), no Walk-Away moment, the challenge is gone on return; turn-off: same as Never Mind, block stays on. A visible wait still completes as a push-through. Rotation is never "away". |
| P7-S6 | New screen and sheets | Compare S1–S6 references; open all info buttons and dismiss sheets each way | Old headline/pre-install/abandonment copy; clipped layout, dead Got it | Your time, both charts, correct current dates, accessible sheets and corrected explanation |
| P7-S7 | App list | Use several enabled blocked apps; disable/remove one | Icons/taps added, wrong sort/fade, vanished historical totals | Rows sorted by saved time then label, zero rows included, bounded scroll; removed app omitted with historical total retained |
| P7-S8 | Recalibration | Cancel then confirm recalibration; revoke/regrant access afterward | Cancel changes values; historical savings jump; regrant silently recalibrates | Confirmation controls replacement; existing saved contributions stay fixed; later nopes use new baseline |
| P7-S9 | Post-challenge visit | Complete challenge, spend time in triggering app, leave it and reopen Stats; later visit again or open another app in same paused block | All pause duration or unrelated/later app usage added; unknown shown as measured 0 | Only observable first target visit contributes; unavailable visit shows “—” and neutral explanation |
| P7-S10 | Real-world readability | Use large font/TalkBack, scroll full dashboard, switch dates/timezone, return after an app restart | Cut-off controls, inaccessible info actions, weekday sequence wrong, old outcomes move dates | Usable screen, actual seven local date labels with Today last; historical outcome dates retained |

## Separate technical evidence — isolated environment, authorization required

These are **NOT VERIFIED on Android** except P7-D1/P7-D2, executed and recorded below. Arjun need not extract databases as part of experience testing. An explicitly assigned technical session should execute the compiled fixtures and record fixture/build/command/result/limits separately:

- P7-D1 (replaces old P7-O2 technical work): StatsMigrationTest exported v3 fixture → actual Room v4 open → reopen, blocks/targets/events/markers preserved, empty new ledger/no backfill. Include real data-preserving upgrade evidence separately from synthetic fixture. **Invalidated for the current tree by F-B (8 October 2026, schema v5):** the 4 October PASS covers v3→v4 only. Needs an isolated re-run (authorization required) of `StatsMigrationTest` — now v3→v5 and v4→v5 (`MIGRATION_4_5` adds only `stats_visit_override`; existing baselines are not recomputed) — together with the new compiled F-B cases in `StatsRepositoryTest` and `BlockRepositoryTest`.
- P7-D2 (executed 4 October 2026 on the N6 tree — **PASS, `OK (16 tests)`**, re-run record below; the earlier 14-test PASS predates P7-N6): ChallengeRepositoryTest + StatsRepositoryTest: failure rollback, duplicate callbacks, session attribution, fixed dedup anchor, pass reset, frozen baseline/recalibration, fallback, retained data on revocation, overlap reads/unique visit claims.
- P7-D3: real UsageEvents adapter on target Android/OEM: activity transitions, first target visit, screen lock, service/process restart, retention gaps and multi-window limitations. Confirm missing data stays unknown and blocking still works without Usage Access.
- P7-D4: active local-event payload/once-per-entry audit, including new Stats/permission events and preserved retired rows. Prior GLM PASS WITH NOTES is not a review of this revision.

### P7-D1/P7-D2 execution record — 4 October 2026, isolated emulator (agent-executed)

- **Authorization/scope:** Arjun's explicit instruction this session — isolated Android execution of `StatsMigrationTest`, `StatsRepositoryTest` and the expanded `ChallengeRepositoryTest` only. No device belonging to Arjun, no exploratory testing. Executed by GLM as the technical session; no source files were modified.
- **Environment (isolated):** newly created headless AVD `toki-p7d-isolated` (pixel_6 profile, Android 16 / API 36, `default/arm64-v8a`), cold boot `-no-window -no-audio -no-boot-anim -no-snapshot`, serial `emulator-5554`. Environment setup installed into the pre-existing `~/dev-tools/android-sdk`: emulator 37.2.12 and `system-images;android-36;default;arm64-v8a` (SDK components only — no project build file, dependency or toolchain change). `adb devices` listed only the emulator; no owner hardware was ever attached or addressed. The emulator was shut down after the run; the AVD remains on disk for reuse.
- **Source/build:** the unmodified reviewed combined working-tree submission on base `89ef9e3` (Codex Stats data per DATA-SUBMISSION.sha256 + GLM UI delta per HANDBACK). `./build.sh assembleDebug assembleDebugAndroidTest` → BUILD SUCCESSFUL. APK SHA-256: app-debug.apk `73fd576333a6303b904bbc7daa361886bc32c9c301de4714ce45767c259f4b6f`, app-debug-androidTest.apk `40e341af93163a7a9143f6ce02659b6d2a2d848266b0510063c1c2d9bb9c5670`. Both installed on `emulator-5554` with `install -r` (streamed, Success ×2).
- **Fixture (P7-D1):** the test rebuilds a real v3 database from the exported Room schema `app/schemas/com.arjunrana.tokishrine.data.db.TokiDatabase/3.json` (SHA-256 `bff0251628cc77f6e657152c415a4412afc797c08135356c9e45165fdf09bfcb`, shipped as an androidTest asset), inserts a block/target/retired-name `challenge_abandoned` event and an `app_meta` marker, sets `version=3`, then opens through the real `MIGRATION_3_4` under Room v4 schema validation, asserts preservation and an empty new ledger, closes and reopens (×2). Exported v4 baseline `4.json` (SHA-256 `e8eae7860117b35f959f941a1f2456147f48a07dabaa7a02c23271e2a6b2c5c7`) is part of the Codex submission; Room's open-time identity validation makes the run a real DDL match, not just SQL text comparison.
- **Command:** `adb -s emulator-5554 shell am instrument -w -e class com.arjunrana.tokishrine.data.StatsMigrationTest,com.arjunrana.tokishrine.data.StatsRepositoryTest,com.arjunrana.tokishrine.data.ChallengeRepositoryTest com.arjunrana.tokishrine.test/androidx.test.runner.AndroidJUnitRunner`. Raw output preserved in [checks/p7-d1-d2-instrumented-output.txt](checks/p7-d1-d2-instrumented-output.txt).
- **Result: PASS — `OK (14 tests)`, 0.7s.** StatsMigrationTest 1/1 (P7-D1: v3→v4 upgrade plus reopen; blocks/targets/events/markers preserved; empty stats ledger, no backfill). StatsRepositoryTest 5/5 and ChallengeRepositoryTest 8/8 (P7-D2: forced-failure transaction rollback incl. stats-ledger-shared challenge transactions, duplicate-callback idempotency, session attribution, fixed dedup anchor and pass reset, frozen baseline/recalibration/fallback, data retained on revocation, overlap read dedup and unique visit claims).
- **Limits:** stock emulator SQLite/Room execution (AOSP-like, API 36 arm64), not Arjun's Samsung OEM storage; single run; synthetic exported-schema fixture only — the real data-preserving upgrade on the owner's actual install stays with P7-O1; P7-D3 and P7-D4 remain NOT VERIFIED; no app activity launch, screenshot or UI interaction occurred beyond the instrumented process.

### P7-D1/P7-D2 re-run record — 4 October 2026, isolated emulator, N6 tree (agent-executed)

- **Authorization/scope:** Arjun's explicit instruction this session — the isolated P7-D2 re-run (with P7-D1) for the N6 delta on AVD `toki-p7d-isolated`: execute `StatsMigrationTest`, `StatsRepositoryTest` and `ChallengeRepositoryTest` only. No owner device, no app launch. Executed by GLM 5.3 (ZCode) as the technical session; no source files were modified.
- **Source/build:** the current uncommitted N6 tree on base `89ef9e3` — [N6-SUBMISSION.sha256](N6-SUBMISSION.sha256) re-verified immediately before the run (**OK 15/15**); schema fixtures unchanged since the first run (3.json `bff02516…`, 4.json `e8eae786…` — no schema change in N6, so v4 stands). `./build.sh assembleDebug assembleDebugAndroidTest` → BUILD SUCCESSFUL (outputs up-to-date: no source changed since the N6 session's final build of this exact tree). APK SHA-256: app-debug.apk `46877f25558eed4eefc141db3edf22847304f11a9b88f93a36c2577c90627a89`, app-debug-androidTest.apk `c33433092edaebb324ef696a9f1a99b698be0e18d19d33e96409ae26f3ade861`. Both installed on `emulator-5554` with `install -r` (streamed, Success ×2).
- **Environment (isolated):** reused AVD `toki-p7d-isolated` (pixel_6, API 36, `default/arm64-v8a`), headless cold boot `-no-window -no-audio -no-boot-anim -no-snapshot`, serial `emulator-5554`. `adb devices` was empty before boot and listed only the emulator during the run — no owner hardware was attached at any point. The emulator was shut down cleanly after the run (`adb emu kill`).
- **Fixture (P7-D1):** unchanged from the first run — `StatsMigrationTest` rebuilds a real v3 database from the exported schema asset `3.json` and upgrades through the real `MIGRATION_3_4` under Room v4 identity validation, then closes and reopens; re-confirmed on the N6 tree.
- **Command:** `adb -s emulator-5554 shell am instrument -w -e class com.arjunrana.tokishrine.data.StatsMigrationTest,com.arjunrana.tokishrine.data.StatsRepositoryTest,com.arjunrana.tokishrine.data.ChallengeRepositoryTest com.arjunrana.tokishrine.test/androidx.test.runner.AndroidJUnitRunner`. Raw output preserved in [checks/p7-d2-rerun-n6-instrumented-output.txt](checks/p7-d2-rerun-n6-instrumented-output.txt).
- **Result: PASS — `OK (16 tests)`, 0.351s.** StatsMigrationTest 1/1 (P7-D1 re-confirmed on the N6 tree). StatsRepositoryTest 6/6 and ChallengeRepositoryTest 9/9 (P7-D2 now executing the N6 cases: site nope → hosting-browser row with fallback saving sharing the browser's five-minute sequence with an app nope, host-less site → raw event only/no outcome, forced-failure rollback inside the stats-ledger-shared challenge transaction, hosting-browser leaderboard rows with site savings and first-browser-visit spent with disable retaining totals, and site walk-away/push-through attribution through the real repository transaction with idempotent completion, inclusive raw counters and domain-target raw events — plus all first-run coverage).
- **Limits:** same class as the first run — stock emulator SQLite/Room (API 36 arm64), not Arjun's Samsung OEM storage; single run; real UsageEvents/OEM adapter behavior stays with P7-D3 and the payload audit with P7-D4; the real data-preserving upgrade on the owner's install stays with P7-O1; no app activity launch, screenshot or UI interaction occurred beyond the instrumented process. Instrumented evidence now matches the N6 tree; the attributed final build is recorded below.

### Attributed final build — installed 5 October 2026 at Arjun's request (agent-executed setup)

- **Source/build:** the N6 tree, committed by Arjun between sessions as `70bd8e8` on top of `89ef9e3` (GLM delta 1+2 / Codex data / Claude N6) — content identity re-verified at install time: N6-SUBMISSION.sha256 **OK 15/15**; `./build.sh assembleDebug` → BUILD SUCCESSFUL (up-to-date against HEAD, no source changed since the re-run session). This is the same APK the P7-D1/D2 re-run instrumented: `app/build/outputs/apk/debug/app-debug.apk`, SHA-256 `46877f25558eed4eefc141db3edf22847304f11a9b88f93a36c2577c90627a89`, re-verified unchanged immediately before install.
- **Install:** `adb -s R5CW30ZBM2R install -r` → **Success** (streamed install, data preserved, no uninstall/clear) on Arjun's SM-S918B over the previous `89ef9e3` install (old Stats design, hash `394ad7e9…`, preserved in the [status snapshot](submissions/89ef9e3-before-stats-redesign/CURRENT.md)). One transient USB transport drop aborted the first attempt (`device not found`; device re-appeared on a single status check); the retry succeeded — no polling. `pm path` confirms `com.arjunrana.tokishrine` versionName 0.1.0 / versionCode 1 (debug, targetSdk 36).
- **Scope/limits:** install only, at Arjun's request ("install the latest build") — the app was **not launched** and no device testing, screenshot or DB extraction was performed. P7-O1's open-the-app verification (existing blocks/targets/settings retained, v3→v4 migration on first open) and all P7-S experience checks are owner observations on this build; accessibility re-binding after reinstall is owner observation. This build is the attribution for every pending check below (P7-O1, P7-S1–S10, P7-O8–O23).

### Temporary testing override — 7 October 2026, owner request ("make testing easier")

- **What changed (GLM delta 4, HANDBACK):** typing challenges demand **20 characters** (pause and disable alike) and timer challenges wait **10 seconds** (pause and disable alike); everything else unchanged (pause 5–100 min default 15, typing max 200 step 10, wait max 300 step 5). Source: `git diff 70bd8e8 -- app/` (5 files; constants, one copy fix, tests updated to the override — revert values documented in `ProductionChallengeValuesTest`). Checks: `./build.sh assembleDebug testDebugUnitTest assembleDebugAndroidTest` → BUILD SUCCESSFUL, **220 JVM tests, 0 failures**, androidTest compiled. Override APK SHA-256 `6a02b60fad78b68863cc9f6cdc75e50483674da22cd75f1f1af3cbb9283493bb`.
- **Acceptance notes:** new/re-saved blocks get the easy values; **existing blocks keep their stored values until re-saved** (their disable editor shows the stored rung unselected; picking the 20-char/10-s rung on save stores the easy values). The disable step shows one choice card per method (no Recommended badge). Production-value acceptance: **P7-O14 is temporarily superseded on override builds** and re-applies on revert; P7-O15 keeps its stored-blocks meaning in both directions. Any result recorded on the override build is valid only for the temporary behavior — revert and re-check before production acceptance.
- **Install: SUCCESS, 7 October 2026 (after reconnect).** The first install attempt failed mid-transfer when the phone dropped off USB (empty installer error, then device absent — stopped after two attempts + one status check per the no-polling contract, recorded above). After Arjun reconnected the phone, the retry `adb -s R5CW30ZBM2R install -r` → **Success** (streamed, data preserved, no uninstall/clear) over the 5 October N6 final build; `pm path` confirms `com.arjunrana.tokishrine` versionName 0.1.0 / versionCode 1. The app was not launched; no device testing performed.

### Override REVERTED — 7 October 2026, later the same day, at Arjun's instruction ("This is incorrect")

- **Revert:** `git checkout HEAD -- app/` restored all five override files to `70bd8e8` — `git diff HEAD -- app/` is empty and N6-SUBMISSION.sha256 verifies OK 15/15, so the tree is content-identical to the committed Phase 7 submission. Checks re-run on the reverted tree: `./build.sh assembleDebug testDebugUnitTest assembleDebugAndroidTest` → BUILD SUCCESSFUL, **220 JVM tests, 0 failures** (production assertions from HEAD, including the restored ProductionChallengeValuesTest guard).
- **Reinstall:** fresh APK SHA-256 `a93899d460db86efaf9edd82ee37d11567a25cb8dba455b5f9aa968fdb038433` (source-identical to `70bd8e8`; the hash differs from the 5 October `46877f25…` only through packaging nondeterminism — content identity is pinned by the clean git diff and the checksum record, not the APK hash). `adb -s R5CW30ZBM2R install -r` → **Success** (streamed, data preserved), `pm path` confirms 0.1.0/1. **The phone again runs production values.**
- **Supersession ended:** P7-O14 (production bounds) applies again; P7-O15 keeps its standing meaning. Blocks created or re-saved during the override window (roughly an hour on 7 October) store 20-char/10-second values and remain as stored — editing one shows its rung unselected and a re-save must move values into the production ranges (the P7-O15 pattern in reverse).
- The override records above and HANDBACK delta 4 are preserved as history of what shipped briefly; no source trace of the override remains.

### Debug-only testing values — 7 October 2026 (evening), installed at Arjun's request

- **What changed (Claude delta 5, HANDBACK):** on the **debug variant only**:
  - Typing passages: minimum and default 20 characters.
  - Waits: minimum and default 20 s.
  - Disable step: the first card becomes 20 chars / "Wait for 20 seconds." and is preselected (ladders 20/350/700 and 20 s/6/12 min). "Recommended" stays on the middle card.
  - Unchanged: maxima, steps and pause 5–100 min (default 15).
  - Release builds keep production values. Checks: 220/0 JVM on debug and on release, Android tests compiled only.
- **Build/install:** working tree = `70bd8e8` + delta 5 (uncommitted). Debug APK SHA-256 `8ca43e6439710fae6e8c45c16c829581910ff789901e58b0edea27cdf808decb`. `adb -s R5CW30ZBM2R install -r` → **Success** (streamed, data preserved, no uninstall/clear). `dumpsys` confirms 0.1.0 / versionCode 1, updated 2026-10-07 23:52. The app was **not launched**; no device testing.
- **Acceptance notes:**
  - **P7-O14 does not apply to this debug build**; production bounds are guarded for release by `ProductionChallengeValuesTest`.
  - **Existing blocks keep their stored values.** Edit and re-save one to use 20, and pick a disable rung if it held 220 / 180, which are no longer offered in debug (P7-O15 pattern).
  - Results recorded on this build are valid for every check except production challenge values.


### Owner experience feedback — 8 October 2026 (debug testing-values build `8ca43e64…`)

Arjun's report, summarised. Change requests P7-F1–F15 are in [TASK](TASK.md), with product authority in the PRD §17 8 October addendum.
- **Challenges:**
  - Pause typing "Never mind" belongs at the bottom under Submit.
  - Back currently suspends the challenge, so the user can return 30 min later and continue. Wanted instead: Back wiggles Never mind, and after 15 s away the challenge auto-nopes quietly.
  - Typed text resets if the user leaves and returns early.
- **Blocks:** adding an app/site needs a disable first, which is painful. Wanted: an add-only "Add more".
- **Stats:**
  - The per-visit estimate looks too low. Wanted: p75 plus a per-app override (designs S7/S8).
  - The Time saved sheet is cramped. Wanted: the new copy (S2 v2), without the 5-minute line.
  - Attempts/day tile is a different size from Nope rate, and doesn't rise on a nope.
  - With all blocks off, Stats hides history.
  - New Today/This week layout (S1 v2), with "vs your usual" removed.
- **Config:** challenge copy, images, ladders and options should be editable in one config file.
- **Typing words** don't feel random.
- **Feedback:** Gmail opens with no subject and no text. **This fails P7-O11 on Arjun's phone** (repair P7-F14). No draft is wanted, so **P7-O10's process-death expectation is withdrawn** (P7-F15).

Status: recorded only. The checks affected by these changes (P7-S3/S5/S6/S7, P7-O10/O11) are re-baselined and await the implementing build. Results Arjun records on the current build stay valid for the unaffected checks.

### F-B re-baseline — 8 October 2026 (data only; checks wait for the GLM UI build)

Submission F-B ([HANDBACK delta 8](HANDBACK.md)) changes the expected results below. The original rows in the table above are kept for continuity.
- **P7-S3 (re-baselined for P7-F10):** S6 appears only with no enabled block **and** no nope/push-through in the last 7 days. With every block off but some activity this week, the dashboard stays, with a "No blocks are on" note and this week's apps still listed. With an enabled block and no activity, the zero dashboard and the "—" rate are unchanged.
- **P7-S7 (re-baselined for P7-F10/F6):** an app whose block is turned off, or that is removed from its block, **stays listed for the week** while it has a nope or push-through in the 7-day window. Once that activity leaves the window, it is omitted and the historical totals remain in the week figures until they age out. Rows show "N nopes this week" and a visit chip; an app with your own visit length shows that value, and its saved time = nopes × your value for the week.
- **P7-D1:** invalidated by the v5 migration; see the technical section above.

### Direct correction — P7-F14 Feedback mailto, 8 October 2026 (Claude Sonnet 5.5; not installed)

- **Reproduction / expected / observed:** see the P7-F14 row in [TASK](TASK.md). Observed on the `8ca43e64…` debug build: Gmail opened with no subject and no body. The subject and body were passed only as `EXTRA_SUBJECT`/`EXTRA_TEXT` on an `ACTION_SENDTO` intent whose data was a bare `mailto:<address>`; Gmail reads the URI.
- **Repair:** the subject and body are now percent-encoded into the `mailto:` URI, and the same values still go as extras together with `EXTRA_EMAIL`.
- **Evidence (builder self-verification, no device):** `./build.sh testDebugUnitTest` → 227 JVM tests, 0 failures (220 before + 7 new in `FeedbackEmailTest`); `assembleDebugAndroidTest` compiled only. The instrumented `FeedbackMailTest` (parses the intent with `android.net.MailTo`) was updated to the new contract and **not executed** (NOT VERIFIED on a device).
- **Affected checks:** P7-O11 (retest: Gmail must show the recipient, subject "Feedback from user" and the typed text, once, with no duplicate body, from both Home and Settings entry points, including a message with a line break, `&` and `?`); P7-O10 (re-baselined above). P7-O8/O9/O12 are unaffected. Results recorded on earlier builds stay valid for unaffected checks.
- **Not yet in an installed build.** Arjun's phone still has the `8ca43e64…` debug build; no installation was done.

### Install — all 8 October changes, 8 October 2026 (owner request; emulator P7-D1 skipped by Arjun)

- **Source:** `70bd8e8` + uncommitted deltas 5, 6, F-A, F-B, 9 (+P7-F12-1) and 10 (+owner-directed R1); [D10-R1-SUBMISSION.sha256](D10-R1-SUBMISSION.sha256) verified 13/13 immediately before the build. 256/0 JVM per variant; Android tests compiled only.
- **Build/install:** `./build.sh assembleDebug` → BUILD SUCCESSFUL; debug APK SHA-256 `654602445b2e34d5d11c3dc26ab51ced5972c7534d6b0513b7c899932917fe3e`. `adb -s R5CW30ZBM2R install -r` → **Success** (streamed, data preserved, no uninstall/clear); `dumpsys` 0.1.0 / versionCode 1, updated 2026-10-08 23:11. App **not launched**; no device testing.
- **Notes:** Arjun chose to skip the isolated P7-D1 v5 re-run, so the **first open runs the v4→v5 migration on his real data** with no prior Android execution of it (P7-O1 observation now covers v4→v5). Debug testing values (delta 5) are active, so P7-O14 does not apply. P7-N13 (fixed row height) not changed. This build is the attribution for all pending P7-S/P7-O checks and the new 8 October flows.

### Owner device results — 9 October 2026 (build `65460244…`)

Arjun's results by check (his notes summarized). Change requests arising are P7-F16–F28 in TASK.
- **Pass:**
  - P7-O1 (upgrade, history kept; v4→v5 on his data, the first Android run of it);
  - P7-F1 bottom escape; P7-F2 Back wiggle (wants a slightly stronger buzz → P7-F19); auto-nope behavior;
  - P7-F12 passages;
  - hero and "Screen time X today" lines; charts/Recalibrate; visit-sheet measured/assumed line, stepper bounds and presets; Recalibrate keeps the override;
  - all of Add more (E);
  - P7-S1, P7-S4, P7-S9, P7-S10/O21;
  - all of Feedback (P7-O8–O13, **P7-O11 now passes**);
  - all of H (P7-O15, O18, O20, O22, O23).
- **Fail / change:**
  - After an auto-nope with the screen off, unlocking showed Toki home → P7-F16.
  - Today tiles not equal → P7-F22.
  - App list rows too narrow to test (clipped subtitle, invisible chip text; former P7-N13 rated too low by the delta-10 review) → P7-F20.
  - "· yours" not wanted → P7-F20.
  - Skinny CTAs → P7-F25.
  - Visit sheet presets not edge to edge → P7-F21.
  - "X saved so far" removed (P7-N11 decided) → P7-F23.
  - THIS WEEK "N of M" not wanted → P7-F23.
  - Time saved copy → P7-F24.
  - P7-S2 permission screen can't scroll to Usage Access → P7-F28.
  - Settings Supported browsers doesn't open, Theme row unwanted → P7-F26/F27.
- **New requests:** "Nope, not now" (P7-F18) and the celebration screen (P7-F17).
- **Not a bug:** the Stats "reset to zero" was the midnight reset of the today figures (owner-confirmed).
- **Not yet reported:** D (empty states), P7-S8, the nope-rate today sheet and the live-line/save re-valuing items. Retest on the next build.

### Install — all 9 October changes, 9 October 2026 (owner request)

- **Source:** `70bd8e8` + uncommitted deltas 5, 6, F-A, F-B, 9 (+P7-F12-1), 10 (+R1), 11 (+ owner images) and 12 (+ P7-G12-1 repair). [D12-SUBMISSION.sha256](D12-SUBMISSION.sha256) verified 14/14 (the `StatsScreen.kt` line refreshed for the repair). 262/0 JVM per variant; Android tests compiled only.
- **Build/install:** `./build.sh assembleDebug testDebugUnitTest testReleaseUnitTest assembleDebugAndroidTest` → BUILD SUCCESSFUL. Debug APK SHA-256 `acac08b7b438647309dee3f49dd42949ef5e694bd74afe1cd1dc9627d95f452a`. `adb -s R5CW30ZBM2R install -r` → **Success** (streamed, data preserved, no uninstall/clear); `dumpsys` 0.1.0 / versionCode 1, updated 2026-10-09 01:31. App **not launched**; no device testing.
- **Review state:** delta 11 has had no independent review yet. The delta 12 review failed on P7-G12-1 only; the reviewer repaired it at Arjun's direction (self-verified, no separate re-review).
- **Retest on this build:**
  - delta 11: the celebration screen, "Nope, not now", the stronger Back buzz, and auto-nope landing on the phone's home screen (including screen off);
  - delta 12: P7-S3/S7 rows/chip/sheet (including the last row's chip with few apps, and at a large font size), the today tiles, hero and week lines, the Time saved sheet's first sentence, the CTAs, Settings (no Theme row, Supported browsers screen) and the permission checklist scroll;
  - the items not yet reported: D (empty states), P7-S8, the today nope-rate sheet, and the live-line/save re-valuing items.
- Debug testing values (delta 5) are still active, so P7-O14 does not apply. This build is the attribution for all pending P7-S/P7-O checks.
- **Reinstalled 9 October, 01:38** with Arjun's celebration-screen restyle (HANDBACK delta 11, "Owner restyle"): debug APK `f8133036476b422e1a1cf23e4a9eb8a0eae7872eec881db6041ecf5e9934ff6a`, `install -r` → Success, data preserved, app not launched. **This build supersedes `acac08b7…` as the attribution.**
- **Reinstalled 10 October, 00:58** (wireless adb, same phone) with the owner changes: the repeat-nope window is now **2 minutes** (`StatsLedger.DEDUP_MS` 120 000; JVM boundary test superseded to 119 999/120 000), and the friction screen button reads **"Nope, not now"**. 262/0 JVM per variant; debug APK `a14565296d5d2d4c5844773a917db92d38cc5ff2f98bfc3c7c66350600f55697`; `install -r` → Success, data preserved, app not launched. **This build is now the attribution**; retest row 5 (the friction screen now also says "Nope, not now") and P7-S4 with a 2-minute window.

### Owner retest results — 10 October 2026 (tester report relayed by Arjun)

- **Build attribution: `f8133036476b422e1a1cf23e4a9eb8a0eae7872eec881db6041ecf5e9934ff6a`** (9 October 01:38 install), confirmed on 11 October by the testing session. The phone was updated to `a1456529…` about 37 minutes after the last test. These results therefore come from the **five-minute** window and the friction screen's old "Not now". Row 5 covers the challenge escapes only (P7-F18, delta 11). P7-F32 (two-minute window) and P7-F33 (friction label) are **untested**.
- **Method (tester):** wireless adb only; taps by coordinates (`uiautomator dump` failed); pixel measurements from screenshots. No installs, data clears, settings changes or code edits. "Pass (owner)" means Arjun's own observation.
- **Pass:** row 1 auto-nope screen on (owner), 2 auto-nope screen off, 3 early return (text cleared, pause timer restarted), 4 turn-off escape (returns to the Blocks list with the block still on; PRD says "returns to the block", noted, not raised), 5 escape label (owner; pause-typing not reached), 6 Back keeps the challenge (owner; wiggle/buzz not adb-verifiable), 7 celebration, 8 Stats hero/week ("86%", no "saved so far"), 9 Today tiles (measured equal), 10 app list with few apps (owner; the 1–4 app case not tested), 11 app list with many apps (~4.5 rows, inner scroll), 12 large font (owner), 13–14 visit sheet and save/Use measured, 15 Time saved sheet sentence, 16 nope-rate sheet ("7 of 7 tries today = 100%"), 18 empty states, 21 permission checklist (owner; large font/onboarding not run), 22 regression smoke.
- **Pass, not fully verified:** row 17 Recalibrate (owner): Cancel is a no-op and confirm keeps savings. Later-nope valuation and Usage Access revoke not verified.
- **Partial → P7-F30:** row 19 buttons: sheet/card/Add more 92 px with 25 px gaps; typing escape 94 px; waiting escape 104 px.
- **Fail → P7-F29:** row 20 Supported browsers: subtitle "+ 6 more", list shows 7, DuckDuckGo/Vivaldi unreachable. Arjun: support seven, not nine.
- **Minor → P7-F31:** visit sheet "1 nopes × 5m".
- **Observations for Arjun (no code change yet):** `chess`, `rando`, `instag` and `rowtest22` turned ON during the session without tester action (expected if they were paused and re-armed; a bug if they were fully off). Stats showed 7 nopes today / 0 push-throughs with no tester nope after about 23:42 on 9 October (possibly auto-nopes; source unknown, a P7-D3/D4 database read would settle it). Test block `rowtest22` (Calendar, Type 20) left in place. Calendar visit value set to 5m then reset to measured 10m; Recalibrate moved Blinkit from about 3m to about 2m.
- **Retest on the next install:** rows 19 and 20, row 13's singular line, the friction screen's "Nope, not now" (P7-F33) and P7-S4 with the two-minute window (P7-F32: a second nope under 2 minutes after a counted one adds nothing; at 2 minutes or later it counts). Other rows keep this report's `f8133036…` attribution.

### Install — deltas 12A + 13, 11 October 2026 (owner request)

- **Source:** all uncommitted deltas through 13. D12A (6) and D13 (8) identities verified 14/14. 262/0 JVM per variant; Android tests compiled only.
- **Install:** debug APK `8a7cbdb9073d73c9eafd97131d7dde3fe75415a856df029001bda845ed6c83fc`, `adb -s R5CW30ZBM2R install -r` (USB) → **Success**, data preserved, no uninstall or clear. `dumpsys` shows 0.1.0 / versionCode 1, updated 2026-10-11 00:25:09. App not launched; no device testing.
- **This build is now the attribution.** Retest the 10 October "Retest on the next install" list above.

### Owner retest results — 11 October 2026 (build `8a7cbdb9…`)

Arjun: "all work. Verified them now." **Pass:** row 19 (waiting escape matches typing), row 20 (subtitle "+ 4 more", 7 browsers listed), visit sheet "1 nope", block screen "Nope, not now" (P7-F33), the two-minute window (P7-F32), and the optional check that a fully turned-off block stays off. This closes the 10 October retest list. Earlier rows keep their `f8133036…` attribution.

## Retained non-Stats acceptance

The following existing checks remain pending and must use the new final build. Their older labels/DB annotations are retained for continuity; route database assertions to the technical session above. Old P7-N1/P7-N2 references are in the preserved review snapshot.

## Feedback — screen 25

| ID | What to test | Steps to follow | Fail conditions | Pass conditions |
|---|---|---|---|---|
| P7-O8 | Both entry points | (a) Home with ≥1 block: tap the bottom **Feedback** button. (b) Settings: tap **Send feedback**. | Either entry dead or opening the wrong screen. | Both open the "Send feedback" screen with headline "How's it going?" and body copy. |
| P7-O9 | No diagnostic-log surface | Inspect the whole screen and the app. | Any "Attach a diagnostic log" toggle/copy anywhere; any attachment UI. | Only the text field and *Send it*; no log/attachment control exists. |
| P7-O10 | Draft preservation | Type text, rotate/turn the screen off and back on, and (if convenient) force-stop and reopen the app into the screen. | Typed text lost on recreation/process death. | Draft text preserved. **Re-baselined 8 October (P7-F15):** text survives rotation/recreation only; Back and re-entry shows an empty field; force-stop/process death is not expected to keep it (original expectation withdrawn, preserved here). |
| P7-O11 | Successful handoff (mail app present) | Type a distinctive line, tap **Send it** from each entry origin (home and Settings). | No mail app opens; wrong recipient/subject/body; an attachment added; `feedback_sent` logged without a handoff (DB); wrong return origin. | The mail client opens addressed to arjranaprep@gmail.com, subject "Feedback from user", body exactly the typed text, no attachment; one `feedback_sent` row (DB) means handoff succeeded — it is not delivery confirmation. Returning to Toki lands on home for the home entry, or Settings for the Settings entry (REVIEW P7-N2). **8 October: failed on the `8ca43e64…` build (Gmail opened with empty subject and body); repaired by P7-F14 (HANDBACK delta 6) — retest on the next installed build.** |
| P7-O12 | Graceful failure (no mail app) | Disable all mail-capable apps (or use a profile without any), tap **Send it**. | Crash; silent nothing; `feedback_sent` row written (DB); draft lost. | Inline message ("No email app could be reached…"), draft and screen intact. |
| P7-O13 | Keyboard behavior | Focus the field and type several lines. | *Send it* unreachable behind the keyboard; text field clipped; screen does not scroll. | Field grows/scrolls; *Send it* stays reachable with the keyboard open. |

## Production configuration values (all variants)

| ID | What to test | Steps to follow | Fail conditions | Pass conditions |
|---|---|---|---|---|
| P7-O14 | Debug build uses production bounds | In the create flow's details sheet: step the typing passage down repeatedly; step the wait down repeatedly; open the disable step for both methods. | Any 20-char/20-second floor or 20/… disable rung still present in debug. | Typing floors at 100 (default 150, max 200, step 10); wait floors at 60 (default 60, max 300, step 5); disable ladders 220/350/700 chars and 180/360/720 seconds with the middle preselected; pause duration 5–100 min (default 15). |
| P7-O15 | Existing owner-test blocks still valid | Blocks created with the temporary debug minima (e.g., 20-char pause typing) remain as stored; try editing one. | Data wiped or rejected on open; enforcement treats stored values as invalid for display. | Stored blocks open/edit normally; new writes enforce production values only (a re-save must move values into the production ranges). |

## Event taxonomy audit

| ID | What to test | Steps to follow | Fail conditions | Pass conditions |
|---|---|---|---|---|
| P7-O16 | Full active-event run-through (DB) | Perform one full manual pass: onboarding steps (fresh install if acceptable — otherwise the subset still reachable), create/abandon/edit/delete a block, turn on/off, trigger the block screen for an app and a site, complete typing and delay pause challenges, walk away from each surface (block screen, typing, countdown), turn a block off via challenge and abandon one turn-off, pause to expiry, bubble shown/tapped/dismissed, open Stats/Settings/Feedback, send feedback. Query the event table afterwards. | Any **active** event from PRD §10 missing entirely; unknown event names. | Every active event fired at least once: onboarding_started, permission_requested/granted/denied, onboarding_completed, block_create_started/step_completed/abandoned, block_created/edited/deleted/turned_on/turned_off, block_screen_shown, walk_away, challenge_started/completed, typing_mismatch, countdown_started/completed, pause_started/expired, bubble_shown/tapped/dismissed, turnoff_started/completed/abandoned, stats_viewed, settings_viewed, feedback_opened, feedback_sent, accessibility_connected/disconnected. Conditional: `url_read_failed` fires only on a real address-bar read failure — acceptable to leave unfired with a note; `permission_denied` needs one denied permission request. |
| P7-O17 | Retired events stay retired (DB) | After the run-through (including deliberate bubble drags without dismissal, app-switches/locks/Back mid-challenge), query for new `challenge_abandoned` / `bubble_dragged` / `block_conflict_shown` / `block_conflict_resolved` / `countdown_stalled` / `countdown_resumed` rows newer than the Phase 7 install. | Any new row with a retired name. | No new retired-name rows; pre-existing historical rows untouched. |
| P7-O18 | Bubble drag behavior unchanged | During a pause, drag the bubble around and release away from the bottom edge; then drag to the bottom edge and release. | Drag broken; dismissal broken; dismissal no longer logs `bubble_dismissed` (DB). | Dragging still works with no event; drag-to-dismiss still discards the pill and logs `bubble_dismissed`; a new pause re-shows the bubble. |
| P7-O19 | Challenge suspension still nonterminal | Mid typing and mid delay challenge: switch apps, lock the screen, and press Back; return each time. | Challenge terminated; any terminal event written (DB); typing text lost. | Session preserved (typing text retained, waiting reset to zero); no walk-away/abandonment recorded. |

## Accessibility and final UI

| ID | What to test | Steps to follow | Fail conditions | Pass conditions |
|---|---|---|---|---|
| P7-O20 | Semantic labels/roles | With TalkBack: navigate the home header (Stats/Settings buttons), any screen's app bar (Back), Stats leaderboard, Feedback field and button. | Unlabeled glyph buttons ("button" only or glyph noise); focus trapped. | Home header buttons announce "Stats"/"Settings"; app bar announces "Back"; all controls have names/roles; focus order sensible. |
| P7-O21 | Large text / small screen | Enable largest font size (and smallest display size if available); visit Stats with a long leaderboard and Feedback with a long draft. | Clipped/cut-off figures; unreachable *Send it*; hero number overlapping cards. | Both screens scroll; all figures and controls remain readable and reachable. |
| P7-O22 | Insets and tokens | Inspect Stats and Feedback against the mock renders (23/25). | Status/nav bar overlap; colors off-token (e.g., pure black/white, wrong accent on the hero); outlined-button rules violated. | Both screens sit inside the system bars; Nocturne tokens only; copy matches §6 screens 23/25 (including removed diagnostic-log row). |

## Phase 1–6 regression smoke

| ID | What to test | Steps to follow | Fail conditions | Pass conditions |
|---|---|---|---|---|
| P7-O23 | Core loop still intact | Onboarding checklist states; create a block (apps + a site); turn on (permission gate if accessibility missing); launch a blocked app → block screen → walk away → walk-away moment; complete a typing and a delay pause; bubble + countdown notification; automatic re-arm; turn off via disable challenge; delete an OFF block via the Are-you-sure dialog. | Any Phase 1–6 behavior changed by the Phase 7 edits (event retirement touched the runtime/repo; variant-value collapse touched the persistence boundary). | All listed behaviors unchanged from the accepted Phase 6 state. |

# TS-time-shrine-rename — rename Toki Shrine to Time Shrine

- **State:** `awaiting_owner`. **D1 submitted 23 October 2026** ([HANDBACK](HANDBACK.md), [D1-SUBMISSION.sha256](D1-SUBMISSION.sha256)): clean build, 278/0 JVM tests per variant, APK `com.arjunrana.timeshrine` / "Time Shrine", Android tests compiled only, self-verified (no independent review). Owner checks [TR-O1–TR-O4](OWNER-CHECKS.md) with the config checks after install. Recorded on 23 October 2026 by Claude Opus 5.5 (architect) from Arjun's answers in the same session. Arjun asked Claude Opus 5.5 to implement it there too, as senior implementer, after [TS-config-values](../TS-config-values/TASK.md) was committed. **Owner decision (Arjun, 23 October 2026):** no independent review and no further Android test run for this change ("we don't need another review and we don't need more android tests").
- **Authority:** [PRD §17 "Time Shrine rename addendum — 23 October 2026"](../../../PRD.md).
- **Base:** `main` at `499caf3` (config code `ce2b236`). Arjun's unrelated uncommitted deletion of `design_handoff_stats_screen/` stays untouched and unstaged.

## Rows

| ID | Change | Status |
|---|---|---|
| TR-1 | `namespace` and `applicationId` become `com.arjunrana.timeshrine`; `rootProject.name` becomes "Time Shrine". | awaiting_owner (D1) |
| TR-2 | Move every source set's `com/arjunrana/tokishrine` tree to `com/arjunrana/timeshrine`, using `git mv` so history follows. Rewrite package and import lines, and string literals carrying the package (for example the self-exclusion package in tests). | awaiting_owner (D1) |
| TR-3 | Classes: `TokiApplication` → `TimeShrineApplication`, `TokiDatabase` → `TimeShrineDatabase`, `TokiAccessibilityService` → `TimeShrineAccessibilityService`; `Theme.TokiShrine` → `Theme.TimeShrine`; database file `toki-shrine.db` → `time-shrine.db`; manifest references. | awaiting_owner (D1) |
| TR-4 | Room schemas: `git mv` `app/schemas/com.arjunrana.tokishrine.data.db.TokiDatabase/` to `app/schemas/com.arjunrana.timeshrine.data.db.TimeShrineDatabase/`, contents unchanged; the build must re-export an identical `6.json`. | awaiting_owner (D1) |
| TR-5 | User-visible copy: `app_name`, the Accessibility service description, `detection_config.json`, Settings "About Time Shrine", the permission checklist and Stats copy, and code comments that name the product. Test strings mirror the copy. | awaiting_owner (D1) |
| TR-6 | Current docs: AGENTS, README, the component contracts, CONTEXT-MAP, and CURRENT name Time Shrine and the new package. Old paths (`toki-shrine-ui-mockups/`, AVD `toki-p7d-isolated`) and historical records stay as written. | awaiting_owner (D1) |

## Invariants

- Behaviour is unchanged: names only. No schema version change (the schema JSON identity hashes stay the same).
- Keep the closed tasks, history and phase records verbatim.
- No new dependencies or toolchain changes.

## Checks

- `./build.sh clean assembleDebug testDebugUnitTest testReleaseUnitTest assembleDebugAndroidTest`: 0 failures per variant, with counts matching TS-config-values (278).
- `git grep -i toki` in `app/` and the build files finds nothing; elsewhere only the kept paths, the AVD name and historical records.
- The re-exported `6.json` is byte-identical to the moved file; the APK's manifest package is `com.arjunrana.timeshrine` and its label is "Time Shrine" (checked with `aapt`, no device).
- `git diff --check`; commit using renames, staging only this task's files.

## Stop

Records (HANDBACK, `D1-SUBMISSION.sha256`, OWNER-CHECKS, CURRENT last), then commit. No install until Arjun asks; the install is a new app, so his phone setup starts fresh.

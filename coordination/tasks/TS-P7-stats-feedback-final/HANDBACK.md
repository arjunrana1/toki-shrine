# HANDBACK — TS-P7-stats-feedback-final (GLM 5.3 implementer)

- **Submission:** working tree at base `12ee572` plus the owner-approved uncommitted planning edits that were present at assignment (PRD §8/§10/phase-07/persistence-events taxonomy wording, P6 task closure records, CURRENT). Those planning edits are preserved — PRD/coordination/phase-07 untouched; `persistence-events.md` keeps the owner's taxonomy line and additionally records the discharged migration obligations (noted under "What was implemented" #5). All other modified/new files are the Phase 7 implementation. Nothing is committed; the reviewer should diff the working tree against `12ee572` and treat the planning-edit files as pre-approved context, not submission content.
- **State:** `ready_for_review`. No device operation, no installation, no PostHog/rename/dependency work.

## What was implemented

1. **Stats screen 23** (`ui/screens/StatsScreen.kt`, `Route.Stats`): the six §9 figures from `EventRepository.getStats()` — hero total with "since you started, …" days line, this-week/best-day/walk-away-rate cards, per-app leaderboard (labels via `InstalledAppsRepository.labelFor`, descending, sites excluded by the existing DAO query). Logs `stats_viewed` once per entry (established `settings_viewed` pattern). New pure formatter `data/stats/StatsFormat.kt` (percent + since-started copy), JVM-tested.
2. **Feedback screen 25** (`ui/screens/FeedbackScreen.kt`, `Route.Feedback`, both entry points wired — home bottom Feedback button and Settings › Send feedback): mock copy minus the removed diagnostic-log control; saveable draft; multiline top-anchored field (new `multiline` param on `NocturneTextField`, default behavior unchanged); IME inset + scrolling. Send builds the §13 intent (`ui/util/FeedbackMail.kt`: ACTION_SENDTO mailto, fixed recipient/subject, typed body, no attachments), checks handler visibility (manifest `<queries>` mailto block added — package visibility, **not** a permission), launches, and logs `feedback_sent` **only** on successful handoff (`ui/util/FeedbackEmail.kt`, JVM-tested). Handler absence/launch failure keeps the screen and draft with an inline message. Successful handoff pops to home (mock's `data-go="06"` reading).
3. **Production values in every variant**: the temporary debug overrides (20-char/20-second minima, 20-first-rung disable ladders from the §17 Phase 5 addendum) are removed with their seam — both `BuildVariantChallengeLimits.kt` variant files deleted; `BlockRepository` now holds single main-source constants (100–200/10, 60–300/5, 220/350/700, 180/360/720, pause 5–100/5). `CreateFlowStateTest`'s override-value assertions updated to production values (structure unchanged); new `ProductionChallengeValuesTest` pins them.
4. **Event retirement**: `challenge_abandoned` (runtime `abandon()`/`AbandonReason`/`ChallengeEffect.Abandoned`, `ChallengeRepository.recordAbandoned`, BlockActivity branch, repository constant) and `bubble_dragged` (Host callback, `PauseService.onBubbleDragged`, repository constant) removed with no behavioral change — dragging, drag-to-dismiss, and nonterminal suspension are untouched; historical rows are never deleted. New `EventTaxonomy` object + reflection-swept JVM test keep the 35 active names exactly equal to the emittable `EVENT_*` constants and pin the 6 retired names.
5. **Migration/schema**: `fallbackToDestructiveMigration()` removed from `TokiApplication` (v3→v3 opens preserve data; pre-v3 would fail loudly — none exists in the field); `exportSchema = true` with KSP `room.schemaLocation`, baseline `app/schemas/com.arjunrana.tokishrine.data.db.TokiDatabase/3.json` generated and checked in (no `show_typos` — dropped at v3 by the approved reset). Contract updated (`docs/components/persistence-events.md` open obligations).
6. **Accessibility/final UI**: shared `NocturneAppbar` back control gains "Back" label/role/44dp target (benefits every screen); home Stats/Settings glyph buttons gain labels/roles/44dp targets; Stats/Feedback respect status/nav/IME insets and scroll.

## Checks run (all non-device, this submission)

- `./build.sh assembleDebug` — **BUILD SUCCESSFUL**.
- `./build.sh testDebugUnitTest` — **194/194** (0 failures/errors/skipped; counted from result XML).
- `./build.sh assembleDebugAndroidTest` — **BUILD SUCCESSFUL** (compile-only; execution not authorized, left for owner acceptance).
- `./build.sh assembleRelease` — **BUILD SUCCESSFUL** (extra one-off after deleting the release source-set file, proportionate non-device compile check).
- `git diff --check` — clean.
- Hex-colour grep — two matches, both pre-existing comment lines (`TypingChallengeScreen.kt` `#8B0000`, `WalkAwayMomentScreen.kt` `#0b0d17`), identical at HEAD; zero new matches, all new files clean.
- Manifest audit — no `INTERNET`/network permission; permission inventory unchanged (VIBRATE, POST_NOTIFICATIONS, SYSTEM_ALERT_WINDOW, REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, FOREGROUND_SERVICE, FOREGROUND_SERVICE_SPECIAL_USE). Added element is a `<queries>` intent filter (mailto SENDTO) for handler visibility.
- Dependency audit — `dependencies {}` block unchanged; only a KSP `arg` added (existing toolchain configuration).

## New/changed tests

- New JVM: `StatsFormatTest`, `FeedbackEmailTest`, `ProductionChallengeValuesTest`, `EventTaxonomyTest`; `RouteCodecTest` gains Stats/Feedback round trips; `CreateFlowStateTest` re-pinned to production values.
- androidTest (compile-verified only): `EventRepositoryTest` seeds retired-name rows as literals (preserved + excluded from stats), empty-stats zeros, Phase 7 engagement-event writes, reopen-without-destructive-fallback preservation; new `FeedbackMailTest` pins intent action/data/extras/no-stream.

## Decisions and interpretations (routine, within scope — flag to reviewer if unwelcome)

- Rate displays as a rounded whole percent (`0.63 → 63%`); "since you started" reads "today" on day 0, "1 day ago", then plural.
- Empty leaderboard: the "MOST WALKED AWAY FROM" section is hidden when there are no app walk-aways (mock has no empty state).
- Draft persistence uses `rememberSaveable` (rotation/process death), consistent with the app's saveable-state idiom; navigating away from the screen intentionally discards it.
- Successful send pops back to home; a failed handoff keeps the screen.
- The mailto `<queries>` block was required for the no-handler check to see mail apps under API 30+ package visibility; it grants no permission.

## Files in this submission (beyond preserved planning edits)

- Code: `MainActivity.kt`, `BlockActivity.kt`, `TokiApplication.kt`, `challenge/ChallengeRuntime.kt`, `data/db/TokiDatabase.kt`, `data/repo/{BlockRepository,ChallengeRepository,EventRepository,EventTaxonomy}.kt`, `data/stats/{StatsCalculator,StatsFormat}.kt`, `pause/{PauseBubbleView,PauseService}.kt`, `ui/components/NocturneUi.kt`, `ui/navigation/Routes.kt`, `ui/screens/{BlockListScreen,SettingsScreen,StatsScreen,FeedbackScreen}.kt`, `ui/util/{FeedbackEmail,FeedbackMail}.kt`, `app/build.gradle.kts`, `app/src/main/AndroidManifest.xml`; deleted `app/src/{debug,release}/java/.../BuildVariantChallengeLimits.kt`; new `app/schemas/.../3.json`.
- Tests: listed above.
- Records: this HANDBACK, `OWNER-CHECKS.md`, `docs/components/persistence-events.md` (obligation bullets), CURRENT updated last.

## Suggested next-role prompt (for Arjun)

> Role: reviewer (Codex). Task: TS-P7-stats-feedback-final, state ready_for_review. Review the working-tree diff against `12ee572` (owner planning edits in PRD/coordination/docs are pre-approved context). Focus: Stats screen correctness against PRD §9, feedback intent/feedback_sent semantics, event-retirement completeness (no remaining emitters), the collapsed variant seam and its test re-pinning, and the migration-posture change (destructive fallback removal + schema export). Evidence: 194/194 JVM, assembleDebug/AndroidTest/Release compile, greps in HANDBACK. Owner checks are prepared in OWNER-CHECKS.md; no device work has run.

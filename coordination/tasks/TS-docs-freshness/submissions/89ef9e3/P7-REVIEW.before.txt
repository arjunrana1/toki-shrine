# Independent review — TS-P7-stats-feedback-final

- **Reviewed submission:** uncommitted working tree over base `12ee572` (Phase 7 implementation). The PRD §8/§10, coordination, phase-07 and P6-closure diffs present in the tree are Arjun's pre-approved planning edits, not submission content; `docs/components/persistence-events.md` carries the owner's taxonomy line plus the implementer's discharged-migration bullets, which accurately describe the code.
- **Verdict:** **PASS WITH NOTES** (P7-N1, P7-N2 — expectation-setting observations, no repair requested). Stats §9 correctness, feedback handoff semantics, event-retirement completeness, the variant-seam collapse, the migration posture and the accessibility touches all check out against PRD §§6/8–10/13/17 and the persistence-events contract. This is code review plus non-device verification only; instrumented tests are compile-verified and every device/DB assertion remains owner acceptance (P7-O1 onward).
- **Reviewer:** GLM 5.3, 23 September 2026, assigned by Arjun. **Model-diversity caveat:** this is a GLM implementation reviewed by GLM; per Arjun's assignment, model-diverse (Codex) confirmation is still pending on the migration-posture, event-retirement and transactional-Stats areas before Phase 7 closure. No device, emulator, adb, installation, screenshot or instrumented execution was performed.

## Area findings

### 1. Stats screen vs PRD §9 and the persistence-events contract — pass

- All six §9 figures come from the pre-existing `EventRepository.getStats()` (unchanged this phase except its comment); the formulas were re-verified against the contract: week window opens at start of day six days ago local (`calendarWeekStart`), best-day buckets local (`strftime … 'localtime'`), days-active day 0 on launch day, rate `walk_away / (walk_away + challenge_completed)` with 0.0 on zero denominator, `turnoff_completed` and retired `challenge_abandoned` out of the denominator (an instrumented test seeds literal retired rows and asserts exclusion — compile-verified).
- Leaderboard: `countsByTarget` filters `target_type = 'app'`, so sites are excluded per contract while `countByName`/`countByNameSince` stay inclusive; rows render `InstalledAppsRepository.labelFor` (never raw packages) and descending counts. Tie order within equal counts is unspecified (PRD requires only descending) — no finding.
- `stats_viewed` logs once per intentional entry via `LaunchedEffect(Unit)`, the established `settings_viewed` idiom. Screen copy/layout matches the `23-stats.png` render (hero total, "since you started, …" line, three cards, "MOST WALKED AWAY FROM" rows); the 63% mock figure is reproduced by `StatsFormat.walkAwayRatePercent`. Null-snapshot handling (blank frame below the app bar) matches the app's established first-load idiom in `BlockListScreen`; empty data renders zeros with the leaderboard section hidden (documented decision; the mock defines no empty state).

### 2. Feedback intent and handoff-only `feedback_sent` — pass

- `feedbackMailIntent` builds ACTION_SENDTO with `mailto:arjranaprep@gmail.com` data, `EXTRA_SUBJECT` "Feedback from user", typed body as `EXTRA_TEXT`, no stream/attachment extras — exactly §13; pinned by `FeedbackMailTest` (compile-verified).
- `feedback_sent` is logged only when `FeedbackEmail.handedOff(hasHandler, launchSucceeded)` is true, i.e. a handler resolved **and** `startActivity` succeeded — handoff-only, never delivery; the compound predicate is pinned on the JVM (`FeedbackEmailTest`). Handler absence or launch failure keeps the screen and saveable draft with the inline message; the manifest `<queries>` SENDTO/mailto block grants package visibility, not a permission. Screen copy matches the `25-feedback.png` render minus the PRD-removed diagnostic-log toggle; draft persistence via `rememberSaveable`; `NocturneTextField`'s new `multiline` param defaults off, so existing callers are unchanged.

### 3. Retirement of `bubble_dragged` / `challenge_abandoned` — pass

- Greps over `app/src` find no remaining production reference to either name, their former constants, `recordAbandoned`, `AbandonReason`, `ChallengeEffect.Abandoned` or `onBubbleDragged`; the only occurrences are comments, the `EventTaxonomy` retired set, and tests seeding literal historical rows.
- Behavior is preserved: the bubble's drag branch still repositions/stays dropped and drag-to-dismiss is untouched (only the `onBubbleDragged` call was removed); `onBackgrounded` and `BackHandler` (ACTIVE → `moveTaskToBack`) keep app-switch/lock/Back nonterminal; `escape()`/`gateWalkAway()` remain the only intentional escapes, emitting `walk_away`/`turnoff_abandoned` as before. No deletion path for historical rows exists anywhere in the app.
- `EventTaxonomy` (35 active / 6 retired) matches PRD §10 exactly, and `EventTaxonomyTest`'s reflection sweep over `EventRepository`'s `EVENT_*` String constants guarantees the active set cannot drift from what is emittable.

### 4. BuildVariantChallengeLimits seam collapse and test re-pinning — pass

- Both variant files are deleted; `BlockRepository` now holds single main-source production constants: typing 100–200 step 10 (default 150), wait 60–300 step 5 (default 60), disable ladders 220/350/700 and 180/360/720 (middle preselected), pause 5–100 step 5 (default 15) — matching the task scope and §7/§17.
- `CreateFlowStateTest` keeps its structure with values re-pinned to production (not weakened), and the new `ProductionChallengeValuesTest` pins constants and defaults so a reintroduced variant override fails loudly.

### 5. Migration posture — pass

- `fallbackToDestructiveMigration()` is removed; `TokiApplication` documents the loud pre-v3 failure. `exportSchema = true` plus the KSP `room.schemaLocation` arg (existing toolchain, no new dependency) produced the checked-in `app/schemas/…/3.json`: version 3, no `show_typos`, entity set and `event` columns matching §10. No Phase 7 entity changed, so the exported identity hash matches the in-field v3 and existing installs open without a migration — the compile-verified `existingRowsSurviveReopenWithoutDestructiveFallback` instrumented test mirrors the shipped open path. The persistence-events doc records the discharged obligation accurately.

### 6. Accessibility touches — pass (code level; TalkBack/large-text remain P7-O20/O21)

- `NocturneAppbar`'s back control is now a 44 dp target with `clearAndSetSemantics` label "Back" and `Role.Button`, benefiting every screen; home's Stats/Settings header buttons are 44 dp labeled buttons; Stats and Feedback apply status/navigation/IME insets and scroll. Send remains a 46 dp full-width button.

## Notes (non-blocking)

- **P7-N1 — engagement events re-log on activity recreation.** `stats_viewed`/`feedback_opened` use `LaunchedEffect(Unit)`, which restarts when the activity is recreated (e.g. rotation), adding a row for a single intentional entry. This is the established, previously accepted `settings_viewed` semantics, so it is consistent, not a defect — but P7-O3's "exactly one `stats_viewed` row per intentional entry" pass condition will surface it if the owner rotates mid-visit. Test without rotating, or accept rotation rows as the same established semantics.
- **P7-N2 — Feedback entered from Settings returns to Settings after a successful send.** `onSent` pops one stack entry, so the home entry lands on home (the mock's `data-go="06"` reading, as the HANDBACK states) while the Settings entry lands back on Settings. PRD pins no pop target for the Settings path and per-origin return is standard back-stack behavior; recorded so the owner expects it during P7-O8/P7-O11.

## Evidence

- Reviewer-run `./build.sh testDebugUnitTest` — task `UP-TO-DATE` against the submission tree, and the result XMLs count **194/194** (0 failures/errors/skipped over 24 classes), matching the HANDBACK.
- Reviewer-run `git diff --check 12ee572` — clean. Hex-colour grep over `app/src/main/java` — only the two pre-existing comment lines (`TypingChallengeScreen` `#8B0000`, `WalkAwayMomentScreen` `#0b0d17`), both outside this diff. Manifest audit — permission inventory unchanged (VIBRATE, POST_NOTIFICATIONS, SYSTEM_ALERT_WINDOW, REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, FOREGROUND_SERVICE, FOREGROUND_SERVICE_SPECIAL_USE); no network permission; only manifest addition is the `<queries>` mailto/SENDTO block. `dependencies {}` block is byte-identical to base; only the KSP schema arg was added.
- Reviewer inspection: retired-name greps (no production emitters), schema 3.json contents, both mock renders (23/25), Back/drag/suspension paths, OWNER-CHECKS prepared with the required columns including the Phase 1–6 smoke (P7-O23).
- Implementer-attributed and not re-run: `assembleDebug`, `assembleDebugAndroidTest` (compile-only), `assembleRelease`. All instrumented assertions and device behavior remain owner acceptance.

## Next actor

Arjun: optionally route this verdict to Codex for the pending model-diverse confirmation on the migration/event-retirement/transactional areas, then run owner acceptance from P7-O1 (`install -r` data preservation) through the checklist. Code review is complete; no repair is requested from the implementer.

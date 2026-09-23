# TS-P7-stats-feedback-final — Stats, feedback and final pass

- **State:** `ready`
- **Goal:** deliver Phase 7 screens 23/25, production configuration cleanup, active-event verification, accessibility/final UI review and release data/distribution hygiene without beginning remote analytics or the Time Shrine rename.
- **Implementation owner:** GLM 5.3, selected by Arjun on 23 September 2026. Escalate migration ambiguity, transactional Stats/event correctness or unresolved product behavior to Codex/Arjun rather than guessing.
- **Base:** `12ee572` plus the owner-approved Phase 6 closure and Phase 7 planning edits present in the working tree at assignment. Preserve them; reconcile and record the exact implementation base before code work.
- **Authority:** [Phase 7](../../../docs/phases/phase-07.md), PRD §§6/8–10/13/17, [persistence/events](../../../docs/components/persistence-events.md), [theme/UI](../../../docs/components/theme-ui.md), [navigation/permissions](../../../docs/components/navigation-permissions.md), [build/validation](../../../docs/components/build-validation.md), and the approved event-taxonomy decision below.

## Scope

- Implement Stats screen 23 from the existing Room event store: total walk-aways, days active, last seven local calendar days, best local day, walk-away rate, and descending per-app walk-away leaderboard. Preserve §9 formulas and exclusions; log `stats_viewed` once per intentional entry.
- Implement Feedback screen 25 and wire both home and Settings entry points. Compose an email to `arjranaprep@gmail.com`, subject `Feedback from user`, body equal to the typed text; no logs/attachments. `feedback_sent` means the populated external email intent was successfully handed off, not confirmed delivery. Preserve the draft and fail gracefully when no handler exists.
- Restore production configuration in all variants: pause typing 100–200 (default 150), pause waiting 60–300 seconds (default 60), disable typing 220/350/700, disable waiting 180/360/720 seconds; pause duration remains 5–100 minutes.
- Retire `bubble_dragged` instrumentation while preserving bubble dragging. Retire/remove the unused `challenge_abandoned` application instrumentation while preserving historical Room rows and the nonterminal background/lock/Back behavior. Keep `countdown_started` and `countdown_completed`.
- Active taxonomy remains: product-critical (onboarding, creation funnel, gate/challenge, pause and turn-off outcomes), feature engagement (bubble, Stats, Settings, Feedback), and diagnostic (accessibility lifecycle, URL-read failures). Verify active events only; do not resurrect retired events.
- Resolve release migration/schema obligations deliberately: replace dev-only destructive fallback, enable an appropriate schema-export/migration path, and address the obsolete `show_typos` column only through the approved deliberate schema revision while preserving existing blocks/events.
- Perform the Phase 7 accessibility/final UI pass on touched/all required screens: semantic labels/roles, focus order, touch targets, large text, scrolling, status/navigation/IME insets, current §17 copy and Nocturne tokens.
- Confirm no manifest network permission, no unapproved third-party dependency, and no Kotlin hex colours outside the theme exception path.

## Exclusions and decisions

- No PostHog, remote analytics, network permission, account/login, SDK, upload queue or telemetry identity. A future remote integration is a separate task.
- No Time Shrine rename, package/application ID change, themed/vector launcher redraw, Phase 2 features, dependency/toolchain upgrade or unrelated cleanup.
- Do not delete historical event rows when retiring an event name. Do not alter Stats to depend on any future upload path.
- Do not count `turnoff_completed`, retired `challenge_abandoned`, suspension, or abandonment in the walk-away denominator. Sites count in global walk-away figures but not the per-app leaderboard.

## Required checks

- Focused JVM tests for every Stats formula/boundary, empty data, feedback intent construction/handler absence, production configuration values, and active/retired taxonomy helpers.
- Room/instrumented coverage for Stats queries, historical-event preservation across migration, schema migration, event parameter/target typing and active-event writes; compile and execute only under owner-authorized device/emulator policy. If execution is not authorized, label compilation separately and leave execution for owner acceptance.
- `./build.sh assembleDebug`, `./build.sh testDebugUnitTest`, `./build.sh assembleDebugAndroidTest` (compile-only unless separately authorized), `git diff --check`, Kotlin hex-colour grep, manifest permission audit and dependency audit.
- Prepare `OWNER-CHECKS.md` using `ID | what to test | steps to follow | fail conditions | pass conditions`, including Stats fixtures/results, Feedback handoff/failure, production values, accessibility/large-text/small-screen behavior, full active-event audit and Phase 1–6 regression smoke tests.

## Stop

Save `HANDBACK.md`, update CURRENT to `ready_for_review`, and stop. No device installation/testing and no PostHog work unless Arjun separately authorizes it.

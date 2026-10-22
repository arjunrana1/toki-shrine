# TS-config-values — final pre-launch config values

- **State:** `awaiting_owner`. **D1 submitted 23 October 2026** ([HANDBACK](HANDBACK.md), [D1-SUBMISSION.sha256](D1-SUBMISSION.sha256)): 278/0 JVM tests per variant, Android tests compiled only, self-verified (no independent review). Owner checks [CV-O1–CV-O4](OWNER-CHECKS.md) run on the first installed build, expected to be the renamed one. Recorded on 23 October 2026 by Claude Opus 5.5 (architect) from Arjun's values given in the same session, which he confirmed (typing default 40, range 30–150). Arjun asked Claude Opus 5.5 to implement it in the same session as senior implementer. The Time Shrine rename follows as a separate task once this one is committed.
- **Goal:** ship Arjun's final pause and disable values and a 330-word typing list. Authority: [PRD §17 "Pre-launch config values addendum — 23 October 2026"](../../../PRD.md).
- **Base:** `main` at `92cf6ce`. Arjun's unrelated uncommitted deletion of `design_handoff_stats_screen/` stays untouched and unstaged.
- **Contracts:** [blocks/targets](../../../docs/components/blocks-targets.md) (values, edit prefill), [build/validation](../../../docs/components/build-validation.md).

## Rows

| ID | Change | Files | Status |
|---|---|---|---|
| CV-1 | Pause values in `AppConfig`: typing 30–150/10, default 40; wait 30–120 s/10, default 30. | `config/AppConfig.kt` | awaiting_owner (D1) |
| CV-2 | Release disable ladders 150/200/300 (default 200) and 60/120/240 s (default 120). | `src/release/.../ChallengeValues.kt` | awaiting_owner (D1) |
| CV-3 | Debug disable ladders 20/200/300 (default 20) and 20/120/240 s (default 20). | `src/debug/.../ChallengeValues.kt` | awaiting_owner (D1) |
| CV-4 | Disable wait copy: a 60 s rung reads "1 minute", not "1 minutes". | `ui/screens/CreateFlowScreen.kt` `disableCardCopy` | awaiting_owner (D1) |
| CV-5 | Edit prefill moves a stored disable value that is off the new ladder to the nearest rung (tie → harder rung), so editing an older block always saves. Unedited stored blocks keep their values. | `ui/screens/CreateFlowScreen.kt` draft state | awaiting_owner (D1) |
| CV-6 | `TYPING_WORDS` grows from 214 to exactly 330 words, keeping every existing word and the list rules. | `config/AppConfig.kt` | awaiting_owner (D1) |
| CV-7 | Tests: update every pinned value and fixture that the new values invalidate (per the every-change rule, instrumented tests included), add coverage for CV-4/CV-5 and pin 330 words. | JVM, testDebug, testRelease, androidTest | awaiting_owner (D1) |

## Invariants

- Unlock choices 5/10/15/20, auto-nope 15 s, Stats visit settings, gate and celebration content are unchanged.
- No schema change: stored values are not rewritten. Validation applies only to new and re-saved blocks.
- Never weaken a test to get a pass; when a fixture value becomes invalid, replace it with a valid one in place and say so in HANDBACK.

## Checks

- `./build.sh assembleDebug testDebugUnitTest testReleaseUnitTest assembleDebugAndroidTest`: 0 failures per variant. Android tests are compiled only (the last executed run, 102/102, predates this change).
- A grep of the tests for the old values (20–150 / 20–240 pause bounds and defaults 20/60; 220/350/700 and 180/360/720 ladders) finds only intentional out-of-range or history cases.
- `git diff --check`; stage only this task's files.

## Stop

Records (HANDBACK, `D1-SUBMISSION.sha256`, OWNER-CHECKS, CURRENT last), then commit with Arjun's approval of the two-commit plan. No install until Arjun asks.

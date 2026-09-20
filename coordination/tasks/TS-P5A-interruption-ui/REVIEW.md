# Codex review — TS-P5A-interruption-ui

- **Reviewed submission:** `3ba8037` on base `8523006`; `3ba8037^` resolves exactly to `8523006`. The later `455750b` commit changes task records only.
- **Reviewer:** independent Codex reviewer, 20 September 2026.
- **Verdict:** **PASS.** No blocking or required code finding remains in the P5A boundary.
- **Review boundary:** the exact implementation diff, P5A task/handback, PRD §§6–8, 11 and applicable §17 amendments, Phase 5 scope, and the theme/UI contract. The worktree was clean at review start. P5B Activity integration, selection, lifecycle/lock handling, monotonic time, terminal races, event/persistence outcomes and pause execution were deliberately excluded.

## Contract review

- **Scope and ownership:** all thirteen submitted paths are allowed P5A additions. The surfaces keep state hoisted and do not read repositories, select resources/passages, launch timers or coroutines, observe lifecycle, decide completion, write events, mutate blocks or grant a pause. Every rendered action reports through a callback; the purpose model carries pause versus turn-off copy without duplicate screens.
- **Block gate and walk-away:** the gate receives its image and humour line from the caller, renders the image full-bleed behind a strong scrim, preserves the filled walk-away versus outlined challenge hierarchy and uses Anton only for the gate headline. The walk-away screen receives the global daily count, formats its ordinal and reports tap-to-dismiss without owning the two-second timer.
- **Typing:** mismatch styling is unconditional and derived per typed character; overflow is marked and typed text is never cleared internally. The empty `TextToolbar`, single-keystroke edit filter and `autoCorrect = false`/no-capitalization IME options implement the scoped paste/autocorrect defenses. Enter reports the current text through `onSubmit`; it does not decide success. Pause and turn-off escape labels are purpose-specific, while terminal meaning remains P5B-owned.
- **Delay:** total and remaining seconds are caller inputs; clock/progress formatting is pure and clamped. The surface owns no timer or visibility state, shows no removed calming/hold message, and exposes the correct pause or turn-off escape action.
- **Resources and focused tests:** the six drawable copies and Anton font are byte-identical to the supplied assets by SHA-256 comparison. The 20 focused JVM tests meaningfully cover mismatch spans, paste-filter acceptance/rejection, progress bounds, clock/ordinal formatting and mode-specific copy. No out-of-bound dependency, manifest, Gradle, prior-phase or runtime file changed.

## Evidence and remaining acceptance

- Independently inspected the exact diff and affected contracts/tests, confirmed the base/parent and allowed-file boundary, compared all seven resource hashes, and ran `git diff --check 8523006..3ba8037` successfully.
- Builder-attributed non-device evidence remains: `assembleDebug` PASS; `testDebugUnitTest` PASS at **129/129** including **20/20** new tests; `assembleDebugAndroidTest` PASS **compile-only** with a forced Kotlin test-source compile; legacy-hex check clean. The reviewer did not rerun builds/tests and does not relabel these results as runtime proof.
- No device, emulator, adb, installation, screenshot, database extraction or instrumented execution occurred during review.

Arjun's separate visual/input acceptance remains open:

1. Check gate legibility and layout over all six supplied photos, including Anton rendering, humour-line placement and filled-versus-outlined prominence.
2. Check the walk-away count copy and immediate tap dismissal; the automatic two-second dismissal belongs to P5B runtime acceptance.
3. On a real IME, verify long-press exposes no Paste action, keyboard clipboard insertion is rejected, autocorrect/predictive suggestions are absent and Enter reports submit.
4. Verify wrong characters are visibly marked, typed text is retained and correction remains editable in both pause and turn-off typing modes, including the 700-character surface.
5. Verify pause and turn-off waiting layouts, draining ring, clock/gradient treatment and their distinct escape placement/copy.
6. Check status/navigation/IME insets, short-height scrolling, contrast, labels and touch behavior on device.

This PASS clears the independent P5A code-review gate only. P5B may now be scheduled, but is not started by this review; owner visual/device acceptance remains separate.

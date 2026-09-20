# Implementation handback — TS-P5A-interruption-ui

## Initial GLM submission

- **Submission:** `3ba8037` on base `8523006` (task-opening HEAD, "Open Phase 5 interruption tasks"; worktree verified clean before editing). Records commit follows separately.
- **Implementer:** GLM 5.3 Pro, as assigned in TASK/CURRENT, 20 September 2026. Self-verification only; independent code review precedes owner/device acceptance. No P5A code preceded this submission (the previous placeholder here carried no evidence).

### Files added (nothing existing modified)

- `app/src/main/java/com/arjunrana/tokishrine/ui/interruption/InterruptionModels.kt` — pure model/helpers.
- `app/src/main/java/com/arjunrana/tokishrine/ui/interruption/BlockGateScreen.kt` — screen 15.
- `app/src/main/java/com/arjunrana/tokishrine/ui/interruption/WalkAwayMomentScreen.kt` — screen 16.
- `app/src/main/java/com/arjunrana/tokishrine/ui/interruption/TypingChallengeScreen.kt` — screens 17/18 and 22-typing.
- `app/src/main/java/com/arjunrana/tokishrine/ui/interruption/DelayCountdownScreen.kt` — screen 19 and 22-waiting.
- `app/src/main/res/drawable-nodpi/sys_block_1..6.jpg` and `app/src/main/res/font/anton.ttf`.
- `app/src/test/java/com/arjunrana/tokishrine/ui/interruption/InterruptionModelsTest.kt`.
- No `strings.xml` additions were required: all screen copy follows the codebase convention of Kotlin literals inside the composables (as every existing screen does).

### UI model and callback contract (state hoisted; screens own nothing)

- `ChallengePurpose { PAUSE, TURN_OFF }` selects copy on the typing and delay surfaces (one layout each, PRD §6 screens 17–19 vs 22); pure selectors `challengeTitle`, `typingEscapeLabel`, `delayEscapeLabel` are JVM-tested.
- `BlockGateScreen(blockName, targetName, humourLine, backgroundRes, onWalkAway, onEnterChallenge)` — renders "Open <targetName>?" and the supplied humour line over the supplied drawable; selection of image/line is the caller's (P5B randomizes).
- `WalkAwayMomentScreen(dailyCount, onDismiss)` — formats "That's the Nth time today" via tested `ordinal`; the 2-second auto-dismiss is deliberately **not** here (runtime-owned); any tap reports `onDismiss`.
- `TypingChallengeScreen(purpose, blockName, passage, typedText, onTypedTextChanged, onSubmit, onWalkAway, turnOffChars)` — live count "n / total" and progress bar via tested `typingProgressChars`; `onSubmit(text)` reports the current text only (completion is P5B's; duplicate suppression likewise); `onWalkAway` fires for both "I'll walk away" and turn-off's "Leave it on".
- `DelayCountdownScreen(purpose, blockName, totalSeconds, remainingSeconds, onEscape)` — m:ss via tested `formatClock`, ring drains with tested `countdownProgress` (remaining fraction); no timer, lifecycle, lock or keep-awake ownership.
- Pure helpers additionally: `mismatchSpans` (per-character, consecutive merged, overflow marked) and `acceptTypingEdit` (paste-blocking edit filter).

### Input hardening (PRD §7.1 / task scope)

- Paste/context-menu: an empty `TextToolbar` (`TextToolbarStatus.Hidden`, no-op `showMenu`) is provided around the field, so long-press offers no Paste option; and `acceptTypingEdit` drops any edit larger than one keystroke (multi-char insertion, paste-over-selection, ≥2-char replacement) while accepting single insert/substitute and any contiguous deletion. A newline is never accepted into the text; a single-newline insertion is interpreted as Enter = submit and only reports `onSubmit(typedText)`.
- IME: `KeyboardType.Text`, `autoCorrect = false`, `capitalization = None` (no predictive/autocorrect help for random words).
- Mismatch styling is unconditional (no `show_typos` read anywhere): a `VisualTransformation` paints each mismatching character with the approved error-family container pairing (`errorContainer`/`onErrorContainer`); offsets are identity so editing semantics are untouched. Typed text is never cleared by the screen; the input border switches to the error token while mismatches exist. Hints row swaps to "Typo positions shown / Kept what you typed" when mismatches exist, matching screens 17 vs 18.
- The reference's keyboard art is not drawn; the screen scrolls (`verticalScroll` + `imePadding`) so 700-char disable passages stay reachable.

### Resource handling

- The six `sys_block_*.jpg` files and `anton.ttf` were copied with `cp` and verified byte-identical (`cmp`) — no recompression or redesign. Images live in `drawable-nodpi` (loaded via `painterResource`, `ContentScale.Crop`); launcher assets and naming untouched.
- Anton is bundled as `res/font/anton.ttf`, exposed as a local `Anton` FontFamily used **only** on the block gate headline ("Open …?"), per PRD §11's "available for the block screen only". Inter remains everywhere else.
- New Phosphor glyph constants (clipboard `0xe196`, magic-wand `0xe6b6`, arrows-counter-clockwise `0xe096`, lock-key `0xe2fe`) live privately in `TypingChallengeScreen.kt`, codepoint-verified against `@phosphor-icons/web@2.1.1/src/regular/style.css` (the same release `Ph.Eye`/`Ph.Check` come from); `Phosphor.kt` was not modified per file boundaries.

### Routine implementation decisions within scope (flag for owner visual pass)

- **Gate visual values:** filled walk-away = `accentRamp.step300` ground with near-black text, translucent-dark outlined way-in with white text/50% border — the screen-15 reference's explicit exceptions to the no-pure-black/white rule, expressed from `Color.Black/White` constants (no new hex tokens; legacy hex check stays green). The reference's 600-weight labels render as Inter Medium (only regular/medium are bundled). Photo scrim is `Color.Black` at 0.78 alpha.
- **Humour line slot:** rendered as the muted line between the question and the buttons — the reference markup's empty `p.muted` slot (PRD §11: "the line beneath the block name", i.e. in the name's text stack, not inside the button as the old mock art showed).
- **Mismatch marks:** the mock's `#8B0000` underline is approximated with the owner-approved error family as a container highlight (SpanStyle cannot draw a colored underline in this Compose version); the mistyped input border uses the error token in place of the mock's untokenized `#e6c98a`.
- **Delay screen:** the radial indigo-to-black ground is rebuilt from `accentRamp.step900`/`neutral.step900`/shade-black at the reference's 50%/12% center; no status message is shown (PRD §6 removes the mock's "keep holding" line with hold detection); turn-off mode adds the "Turning off · <name>" header with a "Leave it on" ghost action and drops the bottom escape (pause keeps the mock's bottom-centered "Never mind").
- **Walk-away ground:** shade black stands in for the reference's `#0b0d17`.

### Checks (GLM 5.3 Pro, non-device, this machine, 20 September 2026)

- `./build.sh assembleDebug` — BUILD SUCCESSFUL.
- `./build.sh testDebugUnitTest` — BUILD SUCCESSFUL; **129 tests, 0 failures, 0 errors** read from the result XML, including the new `InterruptionModelsTest` at **20/20** (mismatch spans incl. transposition/overflow/case, paste-blocker accept/reject matrix, progress clamps, clock, ordinals with teen exceptions, copy selectors).
- `./build.sh assembleDebugAndroidTest` — BUILD SUCCESSFUL (compile-only); additionally `:app:compileDebugAndroidTestKotlin --rerun-tasks` forced a fresh compile of instrumented sources against the new code — BUILD SUCCESSFUL.
- `git diff --check` — clean.
- Legacy hex-literal check — no hits outside `ui/theme/Color.kt`.

### Unexecuted owner checks (compilation is not visual or input proof)

- All of phase-05's visual acceptance: gate legibility over each of the six photos, filled-vs-outlined prominence, Anton headline look, humour line placement; walk-away count line; mistyped marks/border/hints; delay ring, gradient ground and m:ss clock.
- Input behavior on a real IME: long-press offers no Paste; no autocorrect/predictive suggestions while typing random words; Enter submits; paste attempts (including keyboard clipboard chips) are dropped; typed text retained on mismatch.
- Safe-area/inset and contrast inspection on device.
- No adb, device/emulator, installation, screenshot, database extraction or instrumented execution was performed. No `BlockActivity`/service/Room/navigation/Gradle file was touched.

### Current next-role prompt

> Read AGENTS.md and review `TS-P5A-interruption-ui` as an independent Codex reviewer. Submission is `3ba8037` on base `8523006` (records commit aside); HANDBACK.md in the task folder records the contract, decisions and passing non-device checks (129/129 JVM, 20 new). Review the interruption surfaces against the task's scope boundary and PRD §§6–8, 11, 17; keep P5B runtime concerns out of scope. Record PASS/FAIL in REVIEW.md, update CURRENT, and stop — owner visual/device acceptance follows separately, and P5B starts only after PASS.

# REVIEW — owner-directed repair of P7-G12-1 (9 October 2026)

At Arjun's direction the reviewer (Claude Opus 5.5) fixed P7-G12-1 directly and applied P7-N16 and P7-N17 in the same `StatsScreen.kt` list block. This is **self-verified, not an independent re-review** (precedents: P7-F12-1, delta-10 R1).

- **P7-G12-1 resolved:** the fade is drawn only while `listScroll.canScrollForward`. A list that fits, or one scrolled to the end, shows its last row unfaded.
- **P7-N16 applied:** the subtitle `Text` has `weight(1f, fill = false)`. The chip is measured first and keeps its natural width, and the subtitle wraps at large font.
- **P7-N17 applied:** `APP_ROW_APPROX_HEIGHT` is 72, about five rows at default size.
- **Checks:** 262 JVM tests, 0 failures/errors/skips on debug and release, written after the edit. Android tests compiled only.
- **Identity:** D12 re-verified 14/14 with `StatsScreen.kt` `fbdf69e6085024f3862881a0faf218d40858d49a1701d1b2d7b6afd917e0278e`.
- **Install:** done at Arjun's request (APK `acac08b7…`, see OWNER-CHECKS).

Delta 12 has no remaining code blockers. Device confirmation of the list (few apps, many apps, large font) is part of Arjun's P7-S3/S7 retest.

---

# REVIEW — GLM delta 12, 9 October Part 2: P7-F20–F28 (9 October 2026)

## Verdict and exact submission

**FAIL — one P7-F20 blocker (P7-G12-1). Everything else passes.**

- **Reviewer:** Claude (Opus 5.5), a different model and session from the GLM 5.3 implementer.
- **Scope:** HANDBACK "GLM delta 12" only. The base is HEAD `70bd8e88d3fec8d7028b52693eeb4af757d7e33e` plus the preserved uncommitted deltas 5, 6, F-A, F-B, 9 (+P7-F12-1), 10 (+R1) and 11 (+ owner images). Earlier deltas were re-inspected only where delta 12 touches them.
- **No changes by this review:** no app code, device, emulator, install, staging or commit.

**Source identity.** `shasum -a 256 -c D12-SUBMISSION.sha256` from the repo root: **14/14 OK**.

| SHA-256 | File |
|---|---|
| `802cad771f82afb68894840dfcf4049cc69c17bfd9b1db51abf9cf4c08d294ec` | `app/src/main/java/com/arjunrana/tokishrine/MainActivity.kt` |
| `3dff62ff3001273a9acbd4ce835242dff043112f40da960ed203e488f1701f78` | `app/src/main/java/com/arjunrana/tokishrine/ui/components/NocturneUi.kt` |
| `e0b875a8550cbbfbe6b366de2dd39bfdd34f56971d899607dce0b84c76a6ca53` | `app/src/main/java/com/arjunrana/tokishrine/ui/navigation/Routes.kt` |
| `73d718b8d2e07e070746e1705bcc93800aa16f2a5de464d2bee2bc1201dbd50a` | `app/src/main/java/com/arjunrana/tokishrine/ui/screens/AddMoreScreen.kt` |
| `3cbad4fe07af6042062b1ef2367ddf8b6b8fde4625ab5ffe7a132c6371aebb64` | `app/src/main/java/com/arjunrana/tokishrine/ui/screens/PermissionChecklistScreen.kt` |
| `65e5173d8b07c022584337003be3da05fa4602559590dae323b7815a8777e203` | `app/src/main/java/com/arjunrana/tokishrine/ui/screens/SettingsScreen.kt` |
| `c98e56829d743688ad2dc34be29f5fbd9e6a37536c8a0bda55a9a5be8f78826f` | `app/src/main/java/com/arjunrana/tokishrine/ui/screens/StatsPresentation.kt` |
| `473c9b4a0e1450560b53d7cd1c90b4a241ef2a098c6fda2473e14b8f3b7828f6` | `app/src/main/java/com/arjunrana/tokishrine/ui/screens/StatsScreen.kt` |
| `553fd64e5b97d6f1d80897bbb05e9843ce625656e31c38eedc283b06a0b5e304` | `app/src/main/java/com/arjunrana/tokishrine/ui/screens/SupportedBrowsersScreen.kt` |
| `9d0b958411b1145737d22623d37dd0c4d41a865eac7c9eb304f47e2bf0ddf426` | `app/src/test/java/com/arjunrana/tokishrine/ui/navigation/RouteCodecTest.kt` |
| `40ff96cf02db8bac0cc2fcddb1b7d53a2f9cbc3d5454200ef77f38412caedcc7` | `app/src/test/java/com/arjunrana/tokishrine/ui/screens/StatsPresentationTest.kt` |
| `760473fb0fc0bcd1119b7f1841bfffbab070d58bdbcad3829408995799afd6d9` | `app/src/test/java/com/arjunrana/tokishrine/ui/screens/SupportedBrowsersPresentationTest.kt` |
| `c135e10ab254c755a1d22408c78d56479a20ae4fa300a806756246651467053f` | `docs/components/navigation-permissions.md` |
| `904ea78558f0d90caa490dde3aadf369f73597c2c2647cecd5d33ac0f368aa3f` | `docs/components/stats.md` |

**Pre-delta base, recovered exactly.** The GLM session's edit snapshots (`~/.zcode/cli/artifacts/sess_49ffe377…`, `beforeContent`, read as data only) give the content of each file before its first edit.
- These match the recorded [D10-R1-SUBMISSION.sha256](D10-R1-SUBMISSION.sha256) hashes for `MainActivity`, `Routes`, `AddMoreScreen`, `StatsPresentation`, `StatsScreen`, `StatsPresentationTest` and `RouteCodecTest`.
- They match HEAD for `NocturneUi`, `SettingsScreen` and `PermissionChecklistScreen`.
- So every diff below is against the true pre-delta-12 tree, not HEAD.

**Untouched areas.**
- Delta 12 changed nothing under `data/`, `BlockActivity`, the challenge runtime, `ui/interruption/` or `config/AppConfig.kt`. Every uncommitted file modified at or after the session's first edit (01:12) is in the D12 list or the records.
- [D11-SUBMISSION.sha256](D11-SUBMISSION.sha256) still verifies `BlockActivity`, `ChallengeRuntime`, `ChallengeRepository`, `StatsStore`, `Celebration`, `WalkAwayMomentScreen`, `InterruptionModels`, `TypingChallengeScreen`, `Haptics` and their tests.
- Its three non-matches all predate delta 12 (00:46–00:58) and are recorded in HANDBACK delta 11, "Owner images added":
  - `AppConfig.kt` (gate pool 11 → 15);
  - `ChallengeRuntimeTest.kt` (pinned count);
  - the six `celebrate_*.xml` placeholders, replaced by `.jpg` files.

## Blocker

**P7-G12-1 — the overflow fade now covers the last app row.**

- **Location:** `StatsScreen.kt:555–565`. This is the 34 dp `bg`-coloured gradient drawn at `Alignment.BottomCenter` over the list box.
- **What changed:** before delta 12 the box was a fixed 300 dp. With few apps, the fade sat over empty space. Delta 12 correctly makes the box collapse to its content (`heightIn(max = …)`), but the fade is still always drawn. It now lands on the last row.
- **Row geometry:** each row is 8 + 23.25 (name) + 4 + 27.25 (chip line) + 8 = 70.5 dp, plus a 1 dp divider. The chip line therefore sits about 9–36 dp above the box bottom.
- **Failing scenario:** any user with one to four blocked apps, so the list does not fill the cap. Most users are in this group, and a single-app user's only row is affected.
  - The last row's subtitle and chip are blended roughly 20–50% toward the background across the text.
  - About 75% blending at the chip's lower border, which mostly disappears along with the divider.
  - The same happens to the last row of a long list scrolled to the end. That part already existed but stays unfixed.
- **Consequence:** P7-F20 requires every chip to be "`~Xm/visit ✎` in the same outlined style". The last chip looks faded and half-outlined. This is the exact item Arjun failed on device ("invisible chip text"), so installing this build would likely cost a retest cycle on the same check.
- **Edge case?** No. It is the common case and appears on every Stats visit.
- **Fix (localized):**
  - Hoist the scroll state (`val listScroll = rememberScrollState()`) and pass it to `verticalScroll`.
  - Draw the fade only while `listScroll.canScrollForward` (or `value < maxValue`).
  - Nothing else changes, and no test is needed (layout only).
  - A source check of that block is enough to re-review it.

## Notes (non-blocking)

**P7-N16 — at large accessibility font sizes the chip can be squeezed sideways.**
- **Location:** `StatsScreen.kt:526–539`. The subtitle `Text` and the `VisitChip` share an unweighted `Row`.
- **Mechanism:** Compose measures unweighted children in order. The subtitle takes the width it needs first, and the chip gets whatever remains.
- **Estimate** (Inter metrics, 360 dp-wide phone, 20 dp side padding, saved time on the right):
  - At default size, "22 nopes this week ·" plus the chip needs about 200 dp of the roughly 275 dp available, so it fits.
  - Font scale 1.15 (Pixel "Large") fits.
  - From about 1.3× on narrow phones, the pencil gets 0 width and the chip text wraps inside the chip.
  - From about 1.5× up to Android's 2.0×, the chip can collapse to nearly nothing. That is the reported symptom again, by a different mechanism.
- **Vertical clipping is fixed:** rows no longer have a fixed height. The old 60 dp row left 44 dp for about 56.5 dp of content, which is why the subtitle and chip clipped even at default size.
- **Edge case?** Moderate. It needs a large accessibility font on a narrow screen.
- **Fix:** put `Modifier.weight(1f, fill = false)` on the subtitle `Text`. Weighted children are measured last, so the chip keeps its natural width and the subtitle wraps instead. Alternatively, let the chip drop to its own line with `FlowRow`. Recommended in the same repair, because it touches the same lines.

**P7-N17 — the cap shows about four rows, not five.**
- **Location:** `StatsScreen.kt:1078`. `APP_ROW_APPROX_HEIGHT = 60` is the old clipped row height.
- **Effect:** wrapped rows are about 71.5 dp, so `5 × 60 = 300 dp` shows about 4.2 rows at default size. S1 v2 itself draws about four, so this is cosmetic.
- **Fix:** set the constant to 72 if Arjun wants a true five-row window.
- **Edge case?** It applies always, but is minor.

## Verified (passes)

- **P7-F20, apart from the above:**
  - No fixed row height or inner constraint remains.
  - The box is `heightIn(max)` + `verticalScroll`: it scrolls on its own and leaves no reserved space when there are few apps.
  - `visitChipLabel(effectiveVisitMs)` gives `~Xm/visit` for every app, override included.
  - `VisitChip` has a 1 dp `neutral.step700` border, 8 dp radius, one tint and the pencil always.
  - `chipShowsPencil`, "· yours" and the accent tint are gone.
  - The whole row is still `clickable { onOpenVisitSheet }`.
  - It matches S7 apart from the dropped "yours" variant.
- **P7-F21:**
  - The stepper sits in an `accentRamp.step900` 12 dp box at 56 sp tabular plus "min".
  - The presets use `weight(1f)` with 8 dp gaps and fill the width.
  - Unchanged from before: the `initialSheetMinutes` coercion into 1–120 and both stepper bounds; the live `nopes × minutes` line; `setVisitOverride` and `clearVisitOverride`; closing only on `true`; and "Couldn't save that. Try again." on `false` or an exception.
  - No event is logged.
  - The CTA pair is PRIMARY Save and SECONDARY "Use measured", 12 dp apart, 18 dp below the content.
- **P7-F22:** the tiles use `Row(height(IntrinsicSize.Min))` with `weight(1f).fillMaxHeight()`. No intrinsic-incompatible child is used (the 44 dp info `Box`, `Spacer.weight` and the bar `Box` are all safe). The rate bar is pinned to the bottom of the right tile. The order and layout match S1 v2.
- **P7-F23:**
  - The "saved so far" `Text` is removed.
  - `weekNopeRateLine(percent, attempts)` returns "N%", or "—" at zero attempts.
  - P7-N12 is intact: `todayNopeRateSheetFigures` and `NopeRateSheetContent` are untouched, and `nopeRateSheetExplainsTodaysFiguresNotTheWeeks` survives.
- **P7-F24:** the sheet opens "Each ‘nope’ saves you a visit. We count the time that visit usually takes." The rest is unchanged.
- **P7-F25:**
  - `NocturneCtaButton` is `block = true` (full width), 50 dp tall, `RoundedCornerShape(10.dp)`, with a 15 sp `FontWeight.Medium` label.
  - The new `cornerRadius` parameter defaults to `null`, which keeps `MaterialTheme.shapes.medium`. No other call site passes it, so the wizard and the remaining buttons are unchanged.
  - It is applied to Add more (16 dp spacer plus the "selected" line's 10 dp above it, then a 12 dp gap), the visit sheet, "Got it" (16 dp above) and the Recalibrate card (16 dp above).
  - The challenge screens are not touched (D11 hashes OK).
  - Add more's save, enabled and Never-mind logic is unchanged.
- **P7-F26:** the Theme row and its divider are removed.
- **P7-F27:**
  - `MainActivity` loads the list once through `app.detectionConfigLoader.load()` (suspend, IO, cached) and reads `browsers.keys`. That map is a `LinkedHashMap` built from the JSON array, so asset order is kept. The `runCatching` → null pattern follows the BatteryInstructions precedent.
  - The screen lists exactly that list. The subtitle (`supportedBrowsersSubtitle`) derives from the same list.
  - `BROWSER_DISPLAY_NAMES` only dresses package names and cannot add or remove a browser. A new config entry appears with its capitalized last segment.
  - `Route.SupportedBrowsers` ↔ `"supportedBrowsers"` is in the encoder, decoder and the `RouteCodecTest` round-trip and re-encode samples.
- **P7-F28:** heading, progress and both groups sit in a `weight(1f).verticalScroll` column. Continue stays outside it, pinned at the bottom, so Usage Access is reachable. The request wiring is moved verbatim.
- **Handback limits 1–8:** accepted as stated.
  - Display names (1) and the derived subtitle (2) are for Arjun to approve.
  - The degrade path (3) follows precedent.
  - On variants (4), S1 v2 as exported draws Recalibrate neutral-outlined, so SECONDARY matches it.
  - Leaving the confirm dialog (5) is in scope.
  - Visit-sheet height at extreme font (6) is for Arjun's P7-S7 check.
  - Limits 7 and 8 are correct; see P7-N17 for the cap.

## Tests and evidence

- **Test changes, diffed against the recovered pre-delta files:**
  - `StatsPresentationTest`: 13 → 13 `@Test`.
    - `weekNopeRateLineReadsPercentThenNOfM…` is renamed to `…PercentOnly…` and superseded by the addendum. It adds a `0%` case.
    - `appRowsReadNopesThisWeekPlusTheS7VisitChip` drops only the `chipShowsPencil` assertions, which left with the field. The override assertion is kept as `~12m/visit`.
    - `weekdayLabelsAreRollingNarrowLettersWithTodayLast` and `nopeRateSheetExplainsTodaysFiguresNotTheWeeks` survive unchanged.
  - `RouteCodecTest`: 4 → 4, with one sample and one damaged input added.
  - `SupportedBrowsersPresentationTest`: new, 3 tests.
  - **No `@Test` was removed. Every change is a supersession or an addition, with no weakening.**
- **Builder's result XML:** **262 tests, 0 failures/errors/skips on debug and on release** (29 classes each). It was written at 01:17:05, after the last changed source (01:16:02).
- **Reviewer check:** `./build.sh testDebugUnitTest testReleaseUnitTest assembleDebugAndroidTest` → BUILD SUCCESSFUL.
  - Run because the androidTest APK (00:40) predates delta 12.
  - Every task was UP-TO-DATE against the current tree. So the 262/0 results and the instrumented-source compile both stand for this exact submission.
  - Instrumented tests are **compiled only, not executed**.
- **P7-G12-1, P7-N16 and P7-N17** are layout reasoning. No JVM test covers Compose layout here, and device confirmation is Arjun's.

## Next

1. Repair P7-G12-1, ideally with P7-N16 and P7-N17 in the same small edit. It is a localized change to the Stats list block. GLM (first repair submission for this finding) or an owner-directed direct repair both fit.
2. Re-review with a source check of the list block, then run `./build.sh assembleDebug testDebugUnitTest testReleaseUnitTest` (expect 262/0).
3. Install on Arjun's phone (at his instruction) and retest the OWNER-CHECKS 9 October list:
   - delta 12: P7-S3/S7 rows, chip and sheet; Settings, Supported browsers and the checklist; the tiles; the hero and week lines; and the CTAs;
   - Claude delta 11's items: the celebration screen, "Nope, not now" and auto-nope landing on the phone's home screen;
   - the items not yet reported: D (empty states), P7-S8, the today nope-rate sheet, and the live-line/save re-valuing items.
4. Claude delta 11's own scoped review is still separate and pending.

No device work, install or commit is authorized by this review.

Paste-ready next-role prompt:

> Read AGENTS.md, CURRENT and TS-P7-stats-feedback-final REVIEW.md ("GLM delta 12"). Repair P7-G12-1 in `ui/screens/StatsScreen.kt` (the Stats app-list block) only:
> - hoist the list's `rememberScrollState()` and draw the bottom overflow fade only while `canScrollForward`;
> - in the same block, give the row subtitle `Text` `Modifier.weight(1f, fill = false)` so the chip keeps its width at large font (P7-N16);
> - set `APP_ROW_APPROX_HEIGHT` to 72 (P7-N17).
>
> No other file, test or behavior change. Run `./build.sh assembleDebug testDebugUnitTest testReleaseUnitTest assembleDebugAndroidTest` (expect 262/0 per variant), refresh the `StatsScreen.kt` line in D12-SUBMISSION.sha256, add a short repair note under HANDBACK delta 12, and update TASK and CURRENT. Preserve deltas 5, 6, F-A, F-B, 9, 10, 11 and 12. No device, install or commit.

---

# REVIEW — owner-directed delta-10 repair R1: P7-G10-1 and P7-N12 (8 October 2026)

Arjun accepted P7-G10-1 and ruled on the notes: **P7-N12 — the nope-rate sheet should show today's figures**; P7-N11 deferred to his device testing; P7-N14 and P7-N15 are non-issues (closed, no change); P7-N13 is an open question, not a change request. At his direction the reviewer (Claude Opus 5.5) made both changes directly. This is **self-verified, not an independent re-review** (precedent: P7-F12-1).

- **P7-G10-1 resolved:** `StatsPresentationTest.weekdayLabelsAreRollingNarrowLettersWithTodayLast` is restored verbatim from HEAD (lines 48–52).
- **P7-N12 implemented:** new pure `todayNopeRateSheetFigures(dashboard)` in `StatsPresentation.kt` (nopes, push-throughs = today attempts − today nopes, attempts, rate). `NopeRateSheetContent` now draws its bar, "N nopes / N pushed through" and the sentence "N of M tries today = X%. Higher is better." / "No tries yet today." from it. The THIS WEEK card keeps the week's N-of-M. The `nope_rate` info event is unchanged. New JVM test `nopeRateSheetExplainsTodaysFiguresNotTheWeeks`.
- **Checks (implementer's own):** `./build.sh testDebugUnitTest testReleaseUnitTest` → BUILD SUCCESSFUL, **256 JVM tests, 0 failures/errors/skips on debug and release** (254 + restored + new; StatsPresentationTest 13). `git diff --check` clean. Not installed, not committed, no device work.
- **Source identity after R1:** [D10-R1-SUBMISSION.sha256](D10-R1-SUBMISSION.sha256) — same 13 files; only `StatsScreen.kt`, `StatsPresentation.kt` and `StatsPresentationTest.kt` differ from the reviewed [D10-SUBMISSION.sha256](D10-SUBMISSION.sha256).

With P7-G10-1 resolved, delta 10 has no remaining code blockers. The P7-N12 sheet change is small owner-directed UI with no independent review; Arjun's device check of the sheet (P7-S3) covers it. Open: P7-N11 (hero "saved so far" line, owner on device), P7-N13 (row clipping at large font sizes, owner's option), the Add-more helper copy, `AddMoreScreenTest` and F-B Android execution, and the P7-D1 v5 re-run.

---

# REVIEW — GLM delta 10, P7-F5/F6/F7/F10/F13 UI (8 October 2026)

## Verdict and exact submission

**FAIL — one test-evidence blocker (P7-G10-1); no app-code blockers.** Reviewer: Claude (Opus 5.5), a different model/session from the GLM 5.3 implementer. Scope: HANDBACK "GLM delta 10" only, on HEAD `70bd8e88d3fec8d7028b52693eeb4af757d7e33e` plus the preserved uncommitted deltas 5, 6, F-A, F-B, 9 and the P7-F12-1 repair; earlier deltas were re-inspected only where delta 10 touches them. Source identity: [D10-SUBMISSION.sha256](D10-SUBMISSION.sha256) (13 files, verified 13/13 at review time; `shasum -a 256 -c` from the repo root). The F-B schema/migration files still match F-B-SUBMISSION.sha256; its other mismatches are the expected delta-9/10 edits. No app code, device/emulator, installation, staging or commit was changed by this review.

## Blocker

- **P7-G10-1 — a still-valid Stats test was deleted without record.** `StatsPresentationTest.weekdayLabelsAreRollingNarrowLettersWithTodayLast` exists at HEAD and in F-B (8/8 tests) but is gone from delta 10 (11 tests = 5 untouched + 6 new; the subtitle and screen-time tests were legitimately superseded). It was the only test of `weekdayLabels` (rolling narrow weekday letters, "Today" last, fixed English locale — PRD §9), which the unchanged charts still use. The handback lists the test changes without mentioning this removal, so the "251 + 3 net new" figure hides it. **No current user impact**: `weekdayLabels` is unchanged and correct. The consequence is a lost regression guard on a shipped §9 rule, against the "never weaken tests" rule. **Repair:** restore the HEAD test verbatim (`git show HEAD:app/src/test/java/com/arjunrana/tokishrine/ui/screens/StatsPresentationTest.kt`, lines 48–52), run `./build.sh testDebugUnitTest testReleaseUnitTest` (expect 255/0 per variant) and correct the handback count. No app code changes. A source check of the restored test is enough to resolve it.

## Verified scope

- **F-B field removals:** only `StatsDashboard.attemptsPerDay`, the dashboard `screenDailyMs`, the calculator parameter and the repository's 7-day screen average were removed. The persisted `stats_state.screenDailyMs` column, schema v5, migration and every other F-B field and rule are unchanged. `StatsRedesignTest` changes are call-site arity and the `assertNull(model.screenDailyMs)` line that left with its field.
- **P7-F13 (S1 v2):** the hero renders `todaySavedMs` with the "today" suffix. "Screen time X today" comes from `todayScreenMs` and is hidden when it is null. It is replaced by the existing neutral note when usage is unavailable, and no "vs your usual" text remains. The tiles show `todayAttempts` and `todayNopeRatePercent`, with "—" and an empty bar when there are no attempts today. THIS WEEK shows `savedPerDayMs` "/ day" and `weekNopeRateLine` ("N% · N of M", "—" at zero attempts). Rows read "N nopes this week" plus the chip in a fixed 5 × 60 dp scrolling box. The charts and Recalibrate are untouched. `blocksOn == false` shows "No blocks are on", and S6 is unchanged.
- **P7-F6 UI:** chips are `~Xm/visit` plus a pencil when measured and `Xm/visit · yours` in accent when set by the user. This follows the PRD text; the S7 export also draws a pencil on the user-set chip. The sheet opens at the override or the measured value, coerced into 1..120. Stepper buttons are disabled at both bounds, and the presets are 3/5/10/15/20. The live line `nopes × minutes` equals what the calculator will show after Save, because both use counted nopes only. Save calls `setVisitOverride` and "Use measured (Xm)" calls `clearVisitOverride`. Both close only on `true`; `false` or an exception keeps the sheet open with "Couldn't save that. Try again." No event is logged. The selection is saved as a package name, and refreshes go Ready → Ready, so the sheet survives a background refresh.
- **P7-F7:** the S2 v2 copy and formula are used with 18 dp spacing. The five-minute line and the "How we get…" block are gone.
- **P7-F5 end to end:** "Add more" appears only while ON (Edit while OFF). `Route.AddMore` is codec-encoded and round-trip tested. Existing targets load from the database and render locked (lock glyph, no remove); in search they show as selected and inert. Confirm stays disabled until the stored targets load. It then sends existing + added to `addTargets`, so `TargetRemovalException` cannot occur through the UI and would land in the generic failure branch. `ConflictingOwnershipException` shows "Already added to a block". Never mind, the appbar back and system Back all pop the route without writing. `addTargets` deduplicates input and skips existing rows, so a duplicate selection cannot corrupt data.
- **Wizard refactor:** `AppSearchPane` with empty `lockedPackages` and `ownerBlockId = state.editBlockId` behaves as before. `SiteAddSection` keeps the validation, ownership and copy word for word. `SelectedRow` with a non-null `onRemove` is unchanged. One small difference is in P7-N14.
- **Evidence:** the builder's result XML shows **254 JVM tests, 0 failures/errors/skips on debug and release** (StatsPresentationTest 11, RouteCodecTest 4). It was written after every file under `app/src` was last modified, so it covers this exact tree. The reviewer ran no tests. `AddMoreScreenTest` (3 cases) is **compiled only**; executing it, like the F-B Android cases and the P7-D1 v5 re-run, needs Arjun's authorization.

## Notes (non-blocking; owner decisions or polish)

- **P7-N11 — extra hero line.** "X saved so far" (the rolling 7-day total) still sits under "45m today". Neither the S1 v2 export nor the PRD bullet has it, and next to "today" its meaning is unclear (it is not all-time). Owner: keep it, relabel it "this week", or drop it.
- **P7-N12 — nope-rate info sheet mismatch.** The ⓘ is now on "Nope rate today", but the sheet still shows the week's bar and "N of M tries this week". On most days the sheet's percentage differs from the tile it explains. Owner: should the sheet show today's figures, or both?
- **P7-N13 — fixed row height.** Rows are 60 dp with 44 dp inside. At default font size the two lines take about 40 dp, so they fit. From about 1.15× system font size the chip line is clipped. With few or no apps the 300 dp box shows mostly empty space (handback limit 5). Owner visual check; a `heightIn(min = 60.dp)` row would avoid clipping.
- **P7-N14 — wizard site field.** Because the field's state moved into the extracted `SiteAddSection`, a domain typed but not yet added is now cleared when switching to Apps and back. It used to be kept. Nothing saved is affected.
- **P7-N15 — Add more locked set.** `lockedPackages` is computed once per search-pane composition, keyed on the list instance. If the search pane is opened before the block's targets load, or the phone is rotated while it is open, existing apps show as unlocked and selectable. Selecting one shows it twice in the list; saving is a no-op for it. Cosmetic only.
- **Visual nuances for the owner pass (handback limit 1):**
  - THIS WEEK is two stacked label/value lines rather than the export's two big-number columns.
  - The chip is filled rather than outlined.
  - Section labels use sentence case.
  - "96m" uses the §9 duration form.
- **Handback limits 2–4:** no events matches §10 and F-B point 3. The new helper line "Apps and sites already in this block stay in. You can only add." is new copy for Arjun to approve. The compiled-only instrumented test is an evidence gap, not a defect.

## Next

Restore the deleted test (P7-G10-1), rerun the two JVM tasks, and update the handback count. This is a test-only change, small enough for GLM or an owner-directed direct repair. A source check of the restored test can resolve the finding. Then Arjun decides P7-N11/N12 and the new helper copy, and the install/acceptance timing (P7-S3/S6/S7, the chip/sheet/Add-more flows) and the authorized P7-D1 v5 re-run. No device work, install or commit is authorized by this review.

Paste-ready next-role prompt:

> Read AGENTS.md, CURRENT and TS-P7-stats-feedback-final REVIEW.md (GLM delta 10). Repair P7-G10-1 only: restore `weekdayLabelsAreRollingNarrowLettersWithTodayLast` verbatim from `git show HEAD:app/src/test/java/com/arjunrana/tokishrine/ui/screens/StatsPresentationTest.kt` into the current test file without changing any other test or app code. Run `./build.sh testDebugUnitTest testReleaseUnitTest` (expect 255/0 per variant), correct the delta-10 test count in HANDBACK, refresh D10-SUBMISSION.sha256 for the test file, and update CURRENT. Preserve deltas 5, 6, F-A, F-B, 9 and 10; no device, install or commit.

---

# REVIEW — owner-directed P7-F12-1 repair (8 October 2026)

Arjun directed Codex to add the six missing original words without adding or running tests. Codex replaced six newly added words of the same lengths in `AppConfig.TYPING_WORDS`: `cabin` → `cedar`, `cocoa` → `cloud`, `dove` → `fern`, `ginger` → `forest`, `hammer` → `valley`, and `glow` → `wren`. Source comparison with HEAD confirms **all 56 original words are present**, the list still has **214 unique lowercase words**, and the count in every length class is unchanged. `git diff --check` for the edited config file is clean. The repaired file's SHA-256 is `dc09d528c4c757c68d6ec5919ac6ed9c63c3bcd7d22b35a4c4262f267fbbe4e4`.

**P7-F12-1 is resolved by this source check.** The original independent FAIL and its source hashes remain below as the record of GLM delta 9 before repair. This narrow repair was made by the reviewer at Arjun's request; it is self-verified and has no separate independent re-review. The 251/0 JVM results below precede the repair and are not presented as a test run on the repaired file. No tests, device work, installation, staging or commit were performed.

---

# REVIEW — GLM delta 9, P7-F1/F2/F11/F12 (8 October 2026)

## Verdict and exact submission

**FAIL — one P7-F12 blocker.** Reviewer: Codex (GPT-6), a different model/session from GLM 5.3. This review covers HANDBACK "GLM delta 9" only, on HEAD `70bd8e88d3fec8d7028b52693eeb4af757d7e33e` plus the preserved separate uncommitted deltas 5, 6, F-A and F-B. Scope was the delta-9 UI, config, selector and five new tests, with earlier files inspected only at the delta-9 repoints. The source snapshot inspected has SHA-256 `41632113a3e66ee25a10cb516651a0d3d29ce9ecadb71ea98720cd8d73e80adc` for `AppConfig.kt` and `301d0719a37533c9f5412d5d329a6ae6f622964ac3fdf5d97d2d794df4ce2dc8` for `ChallengeContentSelector.kt`. No app code, device/emulator, installation, staging or commit was changed by this review.

## Blocker

- **P7-F12-1 — the new word list is not a superset of the original 56.** `AppConfig.kt`'s `TYPING_WORDS` contains 214 distinct words, but a direct set comparison against HEAD `ChallengeContentSelector.WORDS` finds six omissions: `cedar`, `cloud`, `fern`, `forest`, `valley`, `wren`. Only 50 original words remain. This contradicts the delta-9 handback's explicit superset claim and the requested P7-F12 review criterion; those six established passage words can no longer appear. Restore all six while keeping the intended 214-word count by replacing six new words of matching lengths, then add an assertion pinning the old 56 as a subset. Re-review that repair and its affected selector/test dependency only.

## Verified scope and notes

- **P7-F1:** the pause header escape was removed. Both purposes use the existing full-width, 50 dp outlined escape control under Submit; the pause label is `Never mind`, turn-off stays `Never Mind`. Submit remains the primary control and `canSubmitTyping` still gates it by exact passage length. The capitalization difference is inherited purpose-based copy and is a nonblocking owner visual/copy check.
- **P7-F2:** the replacement `EscapeNudge.kt` retains `Modifier.escapeNudge(nudge: Int)` and computes a decaying horizontal offset back to zero. The existing Back/haptic hook in `BlockActivity` and waiting-screen modifier were inspected only for reachability; delta 9 did not change those paths. Timing, amplitude and feel are source-level only until Arjun's device check.
- **P7-F11:** the 11 backgrounds, seven headlines and 11 humour lines compare in the same order and text with HEAD. All eight moved pause constants compare exactly with HEAD. The new config keeps auto-nope at 15 seconds (`ChallengeRuntime` derives 15,000 ms) and the F-B usual-visit values at 75th percentile, 30,000 ms and three visits. `BlockRepository`, `ChallengeRuntime`, `StatsLedger`, `StatsDashboard`, `CreateFlowScreen` and the relevant test imports only repoint those values; variant `ChallengeValues.kt` files remain untouched. Their header comments still point to `BlockRepository.kt` and can be corrected with the eventual delta-5 cleanup.
- **P7-F12 apart from the blocker:** the selector still rejects lengths below three, constructs exact-length, single-spaced passages, first chooses an eligible word length uniformly, and excludes the immediately previous word. The current list has at least two words in each length class; the five new tests are additions. `ChallengeRuntimeTest`'s two pool assertions retain their values. The fixed-seed endings thresholds (15 distinct words and four lengths) are looser than the reported 70–77 observed endings; they catch severe collapse but would not by themselves prove the claimed improvement over the old algorithm.
- **Evidence:** existing builder XML contains 251 JVM tests with zero failures, errors or skips on each of debug and release, including the five new selector tests. Android tests were compiled only by the builder. Source/set comparison conclusively identifies P7-F12-1, so this reviewer did not rerun the suite. This delta has no schema or Stats behavior change; F-B's separate v5 migration execution and owner/device acceptance remain open.

## Next

Repair P7-F12-1 within `AppConfig.kt` and the selector test, run proportionate non-device checks, then request a scoped different-model/session re-review. Preserve the other uncommitted deltas. No device work or commit is authorized by this review.

Paste-ready next-role prompt:

> Read AGENTS.md, CURRENT and TS-P7-stats-feedback-final REVIEW.md (GLM delta 9). As a bounded implementer, repair P7-F12-1 only: the 214-word list omits `cedar`, `cloud`, `fern`, `forest`, `valley` and `wren` from the old 56. Keep 214 by replacing six newly added words of matching lengths, pin the old-word superset in `ChallengeContentSelectorTest`, run the debug/release JVM tests, and update HANDBACK/CURRENT. Preserve deltas 5, 6, F-A and F-B; no device, install or commit. If one GLM repair submission fails review, reassess ownership before another GLM repair.

---

# REVIEW — submission F-B, owner resolution of P7-F-B1 (8 October 2026)

## Verdict

**PASS WITH NOTES — no remaining F-B code blocker.** Arjun confirmed that a target added to an ON block during that block's active pause shares the remaining pause and starts being blocked when the pause ends. The earlier F-B review below marked this as P7-F-B1 against the then-current “protect immediately” wording. That finding is **resolved by the owner's rule**, now recorded in PRD §17 and TASK P7-F5; no app-code repair or new test is needed. The source-level detection path described below already implements the chosen behavior.

This verdict covers the exact F-B submission identified by [F-B-SUBMISSION.sha256](F-B-SUBMISSION.sha256), verified 17/17 in the earlier review, over HEAD `70bd8e88d3fec8d7028b52693eeb4af757d7e33e` plus the preserved separate deltas 5, 6 and F-A. The earlier inspection, test comparison and builder attribution below remain the evidence; this owner-rule resolution changed only product/task/review records. The F-B code-review gate is cleared. P7-D1's v5 migration tests still need a separately authorized isolated execution; Android source compilation and owner visual/device acceptance are not execution proof. HANDBACK points 2–3 remain provisional product choices rather than correctness blockers.

## Next

GLM may take the assigned F-B-dependent UI work from TASK P7-F5/F6/F10/F13 with this pause rule. Preserve the exact F-B source attribution and the other uncommitted deltas. Do not perform P7-D1 or owner-device work without Arjun's separate authorization; no commit was made.

Paste-ready next-role prompt:

> Read AGENTS.md, CURRENT and TS-P7-stats-feedback-final TASK/REVIEW (F-B owner resolution). F-B data/API review is PASS WITH NOTES. Implement the assigned GLM UI for Add more, the visit-length chip/sheet, the no-blocks note and Today/This week layout. A target added during an active block pause shares that pause and becomes blocked when it ends. Preserve deltas 5, 6, F-A and F-B; run proportionate non-device checks, then hand back for a scoped UI review. Do not run P7-D1, use a device or commit.

---

# REVIEW — submission F-B (initial review, 8 October 2026)

## Verdict and exact submission

**FAIL — P7-F-B1 needs an owner ruling or a repair.** Reviewer: Codex (GPT-6), a different model/session from Claude Opus 5.5. Scope: HANDBACK delta 8 only, P7-F5/F6/F10/F13, over HEAD `70bd8e88d3fec8d7028b52693eeb4af757d7e33e` and the preserved uncommitted deltas 5, 6 and F-A. `shasum -a 256 -c coordination/tasks/TS-P7-stats-feedback-final/F-B-SUBMISSION.sha256` passed **17/17**. No app code, device, emulator, installation, staging or commit was changed in this review.

## Finding

- **P7-F-B1 — a target added during an active pause remains open until that pause ends.** A user can finish a pause challenge, then use Add more to put another distracting app or site into that same ON block. `BlockRepository.addTargets` inserts the target, but `ActiveBlockIndex.from` excludes every target of a paused block (`detection/ActiveBlocks.kt:27–28`); the service builds its index from the blocks flow combined with the pause set (`TokiAccessibilityService.kt:98–108`). The newly added target therefore has no detection entry until re-arm, potentially for the remaining 5–100 minute pause. This conflicts with PRD §17's “Additions protect immediately.” HANDBACK delta 8 point 4 explicitly proposes sharing the pause, so Arjun was asked to choose. If he confirms sharing, amend that requirement and close this finding without code change. If he expects immediate protection, the pause/detection rule must distinguish targets added after the pause began, with a focused regression. No ruling had arrived when this review was recorded.

## Verified F-B paths

- **Visit length and schema:** `usualVisit` filters durations below 30,000 ms, requires three qualifying visits, and takes the inclusive 75th percentile by linear interpolation; otherwise it returns 600,000 ms. Initial capture and Recalibrate call it on completed recent visits. Existing baselines are not recomputed by the migration, as Arjun chose. `MIGRATION_4_5` creates only `stats_visit_override` with the same columns and primary key as exported `5.json`; every v4 entity is unchanged. `3.json`/`4.json` remain byte-identical to HEAD, and `TokiApplication` registers both 3→4 and 4→5. The instrumented tests seed each exported old schema and cover v3→v5 and v4→v5, including retained rows and reopen, but have only compiled.
- **Override accounting:** the calculator substitutes the override only for counted nopes of that package within the seven local dates. Week total, daily/chart, today and app row all use that value; deduped nopes do not. `StatsLedger.record` continues to freeze the measured contribution in `stats_outcome.savedMs`; set/clear do not update those rows. Recalibrate replaces baselines but not overrides; clearing the override reads the frozen contributions again. Set and clear write under the repository mutex and refresh the state after a successful write. Whole-minute 1–120 bounds are enforced, and no override event is emitted.
- **Today and empty state:** the calculator filters counted outcomes by stored local date, including today, for saved time, attempts and nope rate. `todayScreenMs` requires usage coverage from current local midnight through the read time and otherwise stays null; the existing reconstruction splits foreground segments at local midnight. `screenChangePercent` has no remaining source reference, and the current screen-time helper no longer appends “vs your usual.” With no enabled block, counted activity in the seven-date window gives `Ready(blocksOn=false)`; packages with such activity remain on the installed-app leaderboard even if their block is off or removed. No activity gives `NoBlocks`.
- **Add-only write:** `addTargets` reads and inserts in one Room transaction, rejects omitted existing apps/sites before inserts, rejects other-block ownership with rollback, canonicalizes domains using the existing repository rule, leaves the block's enabled/settings row alone and writes no event. The service observes the blocks flow, so new targets of an ON, unpaused block reach the detection index on its next emission.

The changed tests are supersessions, not weakened checks: the removed comparison test covered a retired field; the replacement asserts no comparison. The old frozen-baseline tests now use visits above the new floor while retaining the freeze/revocation assertions and adding a p75 assertion. New JVM tests cover p75/floor/fallback, override re-valuing, today values and outcome rows; new Android sources cover override persistence, v5 migrations and add-only rollback. The builder's existing XML has **246 tests, zero failures/errors/skips in each of debug and release**. HANDBACK reports `assembleDebugAndroidTest` compiled; no Android test ran. Source inspection resolved the review questions, so this reviewer did not run another build.

HANDBACK point 2's general leaderboard rule is consistent with keeping week outcomes visible while other blocks change. Point 3's interpolation and 1–120 minute bounds are reasonable provisional choices; no override event follows the current event list. Arjun may refine those product choices without changing this finding. P7-D1's isolated v5 migration re-run still needs separate authorization; owner visual/device acceptance remains open.

## Next

Arjun: decide whether Add more during an active pause should protect the new target immediately or let it share the current pause. If immediate, assign a bounded P7-F-B1 repair and test; if shared, amend the PRD wording and request a short re-review of that decision. Keep GLM UI work and isolated P7-D1 evidence separately attributed. No device action is authorized by this review.

Paste-ready next-role prompt:

> Read AGENTS.md, CURRENT and TS-P7-stats-feedback-final REVIEW.md (F-B, P7-F-B1). Confirm Arjun's Add-more-during-pause rule. If immediate protection is required, repair only the pause/detection handling for targets added to an already paused ON block, add a focused regression, and run proportionate non-device checks. Preserve F-B and the separate deltas 5, 6 and F-A; no device work or commit.

---

# REVIEW — F-A + repairs R1 + R2 (8 October 2026)

## Verdict and exact submission

**PASS WITH NOTES — P7-F-A1 and P7-F-A2 resolved; no remaining code blocker found in this scoped re-review.** Reviewer: Codex (GPT-6), a different model/session from Claude Opus 5.5, the implementer. Reviewed the uncommitted R2 repair on F-A + R1 over HEAD `70bd8e88d3fec8d7028b52693eeb4af757d7e33e`. The earlier F-A review remains the evidence for unaffected paths. Deltas 5 and 6 were excluded and preserved. Authority is PRD §17's 8 October owner rule: **a failed save never costs a completed challenge**; the earlier review's proposed away-period remedy is superseded, not an outstanding requirement.

Reviewed R2 source identity (SHA-256, repository-relative paths):

| File | SHA-256 |
|---|---|
| `app/src/main/java/com/arjunrana/tokishrine/BlockActivity.kt` | `bd491a7ffa68b1b6e3bdc5e8999b8c7e6acd89ea8a7751b0c9cba7b3f5bfaa1e` |
| `app/src/main/java/com/arjunrana/tokishrine/challenge/ChallengeRuntime.kt` | `4fa13e8af6c2b689b2c1b78409dece0e6cfc62170d3a3bab26fc822ef3e48acc` |
| `app/src/test/java/com/arjunrana/tokishrine/challenge/ChallengeRuntimeTest.kt` | `d5af72e587938cfdac3d2c206b1c6131553c56ff7906b7e4572d1bd6aa2a7842` |

## Repair findings

- **P7-F-A2 resolved under the owner's remedy.** `ChallengeRuntime.saveFailed` (lines 375–387) leaves an offscreen failure COMMITTING with the earned progress and token intact. `BlockActivity`'s exception branch (505–519) uses `resumed && screenInteractive()`, covering SCREEN_OFF before `onStop`, and no longer calls `goAway` on failure. On-screen failure retains the explicitly requested prior retry behavior: wait progress remains earned; typing retains its text for Submit. Stale tokens do not change the current completion.
- **Return and lifecycle interaction checked.** `onPostResume` (283–304) processes the return and reissues a pending completion only when `commitInFlight` is false. The flag is set before dispatch and cleared in both result and exception callbacks. If a return occurs during the write, that write retains ownership; a subsequent visible failure takes the normal retry path. SCREEN_OFF/`onStop` cannot open an away period for COMMITTING; expiry, ticks and Back cannot cancel it. `onNewIntent` preserves the pending session for the return callback instead of replacing it.
- **Recreation checked at the source level.** Saved phase, generation, text, attempts and accrued wait survive the Bundle mapping; `resolveSession` reissues COMMITTING with the same session/token. `commitInFlight` is per activity, so recreation can overlap an old application-scope write. The existing repository transaction checks the completion marker before writing, and callbacks to a destroyed activity do not deliver the terminal UI result. Inspection of `completePause`/`completeTurnOff` was limited to this retry/idempotency dependency. A normal `false` result for an invalidated block/config still closes the screen; it is distinct from a save exception.
- **P7-F-A1 remains resolved.** The away guards in `onVisible`, `tick` and the activity ticker remain intact. The reordered turn-off regression now advances through 0, 0.5, 1, 11.5, 12, 21.999 and 22 seconds: the late callback cannot accrue time, and the full wait completes after return.

## Test evidence and limits

The three R2 tests express the claimed pure-runtime sequences: completed wait → SCREEN_OFF → failure before stop → stop → no expiry at 60 s → same-token retry with 10,000 ms earned; hidden typing failure → return after about 25 s with text and one attempt preserved; visible failure → immediate earned-wait retry with a fresh token and stale-token rejection. The recreation assertion checks the restored token; completion after return is asserted on the original runtime. These are runtime tests, not Android callback or process-death execution tests.

Existing builder XML under `app/build/test-results/testDebugUnitTest` and `testReleaseUnitTest` was inspected: **244 tests, zero failures/errors/skips per variant**, including **33 ChallengeRuntimeTest cases** and all four reviewed tests. The build, Android-test compilation and mutation checks remain attributed to HANDBACK "Repair F-A-R2"; this reviewer did not rerun them. Source inspection resolved the scoped questions without additional execution. Android tests were **compiled only**.

**Retained notes, not new blockers:** return still relies on lifecycle callbacks. The previously disclosed no-pause OEM case has no automatic return signal; a hidden failed save stays pending until an actual resume. Earlier F-A process-death/deep-sleep/detached-record limits remain attached to that submission. Device/OEM behavior and owner experience acceptance remain **NOT VERIFIED**. No speculative edge-case repair is requested.

## Next

F-A is `awaiting_owner` with its code-review gate cleared. Arjun chooses the next session; part two is not started by this review. Only REVIEW and CURRENT were edited; no app-code change, device/emulator/install, staging or commit.

Paste-ready next-role prompt:

> Read AGENTS.md, CURRENT and TS-P7-stats-feedback-final REVIEW.md (F-A + R1 + R2). F-A has PASS WITH NOTES; retain its source attribution and separate owner/device acceptance. As planner, outline the next bounded part-two submission from TASK (P7-F6, P7-F13/F10, P7-F5), preserving deltas 5 and 6. No implementation or device work in this planning session.

Earlier review rounds are preserved verbatim below; their verdicts and suggested remedies describe those earlier submissions.

---

# REVIEW — F-A + repair R1 (8 October 2026)

## Verdict and scope

**FAIL — P7-F-A1 is fixed, but one affected lifecycle blocker remains (P7-F-A2).** This different-model/session re-review covers only Claude Opus 5.5's uncommitted repair R1 on F-A, base HEAD `70bd8e8` plus the separate uncommitted deltas 5 and 6. I inspected `ChallengeRuntime`, the affected `BlockActivity` lifecycle/callback paths, and the two new runtime tests. The rest of F-A retains the prior review's findings; deltas 5 and 6 were not reviewed or changed. No device work, app-code edit or commit was done.

## P7-F-A1 repair result

- **Resolved for an open away period.** `onBackgrounded` clears wait accrual and opens `awayStartMs`; `onVisible` cannot set `visibleSinceMs` until `onReturned` clears that start, and `tick` cannot complete while away. `startTickerIfNeeded` also gates on `isAway()`. Thus a late `persistStarted` callback after SCREEN_OFF cannot restart the visible wait even if `resumed` has not yet become false. `onPostResume` calls `onReturned` before `onVisible`; `resolveSession` evaluates a restored away start before starting a wait. The hidden commit-failure branch calls `goAway` when `resumed` is false.
- **Regression evidence.** The pause test mirrors SCREEN_OFF → late start callback → ticker attempt → return within 15 s and asserts the full wait begins on return. The turn-off test covers the same delayed-start path. The implementer's 241/0 debug and release JVM results, Android-test compilation, and mutation check (removing the `onVisible` guard makes both regressions fail) are attributed to R1; this review did not rerun them. The turn-off test calls `tick(11_000)` before `onReturned(10_000)`, so its clock sequence should be made monotonic in the next edit; the pause regression already establishes the guard with a valid sequence.

## Blocker

- **P7-F-A2 — a commit failure after SCREEN_OFF can restart a wait offscreen.** In `BlockActivity.kt:513–533`, SCREEN_OFF while COMMITTING does not open an away period. Commit start cleared the old away start (`ChallengeRuntime.kt:332–337`). If persistence fails before `onStop`, `BlockActivity.kt:498–505` sees `resumed == true`, calls `commitFailed` and starts the ticker; `isAway()` is false, so R1's guards do not apply. For a waiting challenge, `commitFailed` also sets `visibleSinceMs` (`ChallengeRuntime.kt:375–380`); the next tick may retry the already-complete wait and record a push-through while the screen is off. An unlock before `onStop` calls `onReturned` with no away start, so it cannot restore the intended wait/away behavior. This contradicts P7-F3's visible-time and 15-second-away rules. Reconcile screen state when a commit fails after SCREEN_OFF, start an away period if still hidden, and cover that callback order without relying on `onStop`.

The handback's **no-pause OEM limit** remains a note for an ACTIVE challenge whose away period was opened by SCREEN_OFF: without pause/stop or a return callback, it can auto-nope after screen-on. P7-F-A2 is distinct: COMMITTING opened no away period, and its failed write can restart completion while the screen is dark. Device/OEM behavior and owner acceptance remain **NOT VERIFIED**.

## Next

Senior implementer: repair P7-F-A2 within the F-A lifecycle paths, make the turn-off regression's clock sequence monotonic, run proportional non-device checks, and update HANDBACK/CURRENT. Then request scoped independent re-review. Preserve deltas 5 and 6; no install or device work in this review.

---

# REVIEW — submission F-A (8 October 2026)

## Verdict and submission

**FAIL — one blocker (P7-F-A1).** Reviewed Claude Opus 5.5's uncommitted HANDBACK delta 7, P7-F3/F4 and the P7-F2 Back hook, on HEAD `70bd8e8` plus the separate uncommitted deltas 5 and 6. This is a different-model, different-session review. Scope was the delta-7 files listed in HANDBACK and the affected ledger/lifecycle callers; deltas 5 and 6 were excluded. No code was changed, no device work or commit was done. The earlier N4 + N6 verdict below remains attached only to that earlier submission.

## Blocker

- **P7-F-A1 — a SCREEN_OFF race can count offscreen time in a waiting challenge.** `BlockActivity.kt:511–531` starts the away clock and clears wait progress on SCREEN_OFF, but leaves `resumed` true until `onStop`. If `recordStarted` finishes in that interval, its callback at `BlockActivity.kt:433–436` calls `engine.onVisible(now())` and restarts the ticker. `ChallengeRuntime.onVisible` (`ChallengeRuntime.kt:189–193`) does not reject an active away clock. If the activity receives SCREEN_OFF without `onStop` before unlock (a case the HANDBACK explicitly allows), `onPostResume` clears the away clock but keeps that offscreen `visibleSinceMs`, so a return within 15 s resumes a shortened wait. This violates P7-F3/F4's visible-time-only and wait-restarts-on-return rules. Keep the runtime non-visible while `awayStartMs` exists, including late persistence callbacks; then add focused coverage of SCREEN_OFF → delayed `recordStarted` completion → return before `onStop`/within 15 s.

## Verified behavior and limits

- The runtime's GATE/ACTIVE/COMMITTING branches, 15 s boundary, earliest screen-off/onStop start, saved away-start and backwards-clock expiry, configuration-change exclusion, `onNewIntent` expiration, and commit-wins guard agree with the HANDBACK state table at the pure-runtime level. `onPostResume` evaluates expiry before resuming. The activity's pending walk-away source retains the quiet `auto_away` path across recreation. Back in ACTIVE bumps `escapeNudge` and calls `BlockHaptics.nudge`; there is no `moveTaskToBack`, and COMMITTING Back is inert. The placeholder modifier reaches the pause typing, turn-off typing and waiting escape controls.
- Pause expiry routes through `recordWalkAwayAndCount` with `source=auto_away` and no Walk-Away moment; turn-off expiry takes `TurnOffAbandoned`. `StatsLedger.record` is unchanged and does not receive `source`; it uses the same package, fallback/baseline, session ID and five-minute dedup as a chosen nope. The new Room test asserts this, but it was **compiled, not executed** by the builder.
- The three updated runtime tests are supersessions, not weakened assertions: the typing test now checks cleared text/mismatch marks while retaining passage/session/attempts; the wait test moves return inside the new 15 s limit while retaining reset/stale-generation/completion checks; the commit-race test adds expiry no-op checks. The 11 new runtime tests and taxonomy test cover the other pure-state rows. None exercises the Android callback ordering in P7-F-A1.
- HANDBACK limits remain: process death while away and never reopened records nothing; deep sleep can delay the timer and shift the event/Stats day to recording time; detached `onNewIntent` recording can be lost if the process dies immediately. These are disclosed limits, not additional blockers for this scoped submission. Device lifecycle, haptic and wiggle behavior, and owner acceptance remain **NOT VERIFIED**.

Builder evidence only: `./build.sh assembleDebug testDebugUnitTest testReleaseUnitTest assembleDebugAndroidTest` succeeded with 239 JVM tests and 0 failures per variant; Android tests compiled only. This review did not rerun builds because the blocker follows from callback ordering in source. `git diff --check` was clean.

## Next

Senior implementer: repair P7-F-A1 within F-A, add the focused regression, rerun proportional non-device checks, and update HANDBACK/CURRENT. Then request a scoped re-review of that repair and affected lifecycle paths. Keep deltas 5 and 6 untouched. No install or device work in this review.

---

# REVIEW — accumulated N4 + N6 delta (scoped re-review, 4 October 2026)

## Verdict

**PASS WITH NOTES** for the exact accumulated uncommitted delta on top of the PASS-WITH-NOTES combined Stats tree (base HEAD `89ef9e3` + Codex data submission + GLM delta 1):

- **GLM delta 2 — P7-N4** minutes-only chart labels: `StatsPresentation.kt` (SHA-256 `54a8a443…`), `StatsPresentationTest.kt` (`034e665d…`), PRD §9/§17, `docs/components/stats.md`.
- **Claude delta 3 — P7-N6** site outcomes attribute to the hosting browser: the 15 files in [N6-SUBMISSION.sha256](N6-SUBMISSION.sha256).

No blockers. Code review plus one non-device JVM re-run only; **the changed/added instrumented cases, real device usage behavior, the attributed final build and owner acceptance remain NOT VERIFIED** (list at the end).

Reviewer: Claude (Opus 5.5), a different session from the N6 implementer but **the same model**. The N4 portion is fully independent (GLM author). For N6 the owner chose this session; if strict model independence matters, a short Codex cross-check of N6 can be added. This review does not cover that.

The previous combined review is preserved unchanged in [submissions/combined-stats-redesign-review/REVIEW.md](submissions/combined-stats-redesign-review/REVIEW.md). Notes P7-N3–N7 and the owner verdicts on them stand.

## Submission identity (reviewer-verified)

- `shasum -a 256 -c N6-SUBMISSION.sha256` → 15/15 OK.
- `shasum -c submissions/pre-n6-claude/PRE-N6-APP.sha256` → exactly 9 mismatches. These are the 9 N6 app files that were already modified before N6. The other 4 N6 app files (BlockActivity, DetectionEngine, DetectionEngineTest, ChallengeRuntimeTest) were clean at HEAD and are diffed against git. 33 modified/untracked `app/` paths = 29 pre-N6 + those 4. Nothing else in `app/` changed.
- No schema change: `TokiDatabase.kt` `ca6791e1…`, `StatsStore.kt` `3626a387…`, `schemas/…/4.json` `e8eae786…` all equal PRE-N6. Room stays at v4, so the recorded P7-D1 migration evidence still applies. No dependency, toolchain, manifest or raw-event payload change.
- N4 file hashes match the values recorded in HANDBACK delta 3 (N6 left them untouched).

## What was reviewed (authority: PRD §17 "Phase 7 owner-review addendum — 4 October 2026", §9 as incorporated)

**P7-N4.** `barValueLabel` gives whole minutes using the same `roundToLong` nearest-minute rounding as `StatsDurationFormat.duration`, so 99.5m → `100m` in both. Unknown values stay "—" and zero stays `0m`. Bar heights and stubs are unchanged, and only the two chart cards (`statsBarSpecs`) use it. The hero, tiles, totals and app rows keep the §9 format. `heavyDayBarLabelsStayMinutesOnly` pins 140m/100m and the rounding boundary. PRD §9/§17 and stats.md agree. Visual fit on a heavy day remains P7-S6/P7-O21.

**P7-N6: host propagation.**
- `DetectionTrigger.hostPackage` is a required parameter, so no construction site can omit it. An app trigger uses its own package. A site trigger uses `PendingSettle.packageName`, which `onSettleElapsed` has already validated equals the active window's package and the window's package at fire time. The service passes it as `EXTRA_HOST_PACKAGE`.
- `BlockActivity.resolveSession` reads the extra each time it resolves and passes it into `ChallengeConfig` and the deferred pending-walk-away `WalkAway`. `WalkAway` and `CompletionRequest` (including `resumePendingCompletion`) take it from the config, and `handle(WalkAway)` forwards it to `recordWalkAwayAndCount`.
- Recreation re-reads the system-retained intent, so the host survives. `onNewIntent` replaces the intent only when no unfinished challenge, pending walk-away or walk-away card owns the activity, so a relaunch cannot switch a live session's host. `completePause` and `recordWalkAwayAndCount` are the only ledger callers, and both pass the host.
- A legacy or hand-made intent without the extra records the raw event but no Stats outcome. This is conservative and consistent with no backfill.

**P7-N6: ledger, transaction, idempotency and frozen values.**
- `StatsLedger.statsPackage` maps: app → target, site → host, anything else or blank → no outcome. A site outcome is never recorded under its domain.
- The `record` body is otherwise unchanged. It runs inside the existing challenge Room transaction, after the raw event and before the walk-away marker or `markCompleted`. The session-ID outcome precheck plus the marker/`completionExists` guards keep duplicates as no-ops.
- `savedMs` and `baselineId` are frozen at resolution, and pre-capture rows keep the fallback value with a null baseline.

**P7-N6: shared dedup sequence.** `lastCounted(pkg)` uses the resolved browser package. Site and app nopes on one browser therefore share a single fixed five-minute anchor, and a pass ends the sequence, as §9 requires. This is pinned on the JVM (package rule), in StatsRepositoryTest (site nope then app nope on the same browser → counted/ignored) and in ChallengeRepositoryTest (real repository path; idempotent duplicate completion; raw counter stays inclusive at 1/2/3; raw events keep the domain target).

**P7-N6: leaderboard.**
- `leaderboardPackages` = app targets of enabled blocks, plus the bundled-config supported browsers while any enabled block has a site, then intersected with installed packages.
- The browser source is the same `DetectionConfigLoader` map the service uses for address-bar reading. Every site outcome therefore comes from a package in that set.
- It is resolved outside the Room transaction. A config failure degrades to app rows only, and cancellation is rethrown.
- Removing or disabling every site block drops the browser rows while the totals are kept, matching §9 ("browsers with no enabled site block disappear … historical totals remain"). A browser that is also an app target appears once.
- The "hosting browsers" interpretation is a reasonable reading of §9: site blocks are enforced in every supported browser, so each one hosts them. See P7-N8 for owner confirmation.

**P7-N6: first-browser-visit spent.** `firstPostChallenge` is unchanged and runs against `outcome.packageName` (now the browser). The guards still apply: first non-Toki completed visit, within 30s, covered history, unique claim, otherwise unknown. `enabledSiteBlockShowsHostingBrowserRow…` pins 7s attributed to the site pass. Toki's own visits are skipped, so Toki surfacing first does not by itself make the visit unknown; the 30s limit does. See P7-N9.

**No backfill.** There is no migration and no historical scan. Ledger rows are written only from live terminal paths.

**Replaced test was updated, not weakened.** `sitesAreExcluded…` became `siteNopesAttributeToHostingBrowserShareItsSequenceAndStatsWriteFailureRollsBackPairedEvent`:
- It now asserts the new positive behavior: browser package, counted/ignored order, fallback/zero saved values.
- It keeps the old negative guard: a host-less site writes no row and nothing is recorded under the domain.
- It keeps the forced-failure rollback that pairs the raw event with the Stats row, and re-checks after the trigger is dropped. The final count rising from 1 to 3 follows from the two new rows.

The contract docs (stats.md, persistence-events.md) replace the exclusion rule consistently.

## Independent check run by this review

- `./build.sh testDebugUnitTest --rerun-tasks` → BUILD SUCCESSFUL, **220 JVM tests, 0 failures/errors/skips** (XML under `app/build/test-results/testDebugUnitTest/`). This matches the builder's 220 on this exact tree.
- Checksum, scope and schema-identity verification as above.
- Android-test compilation was not re-run. The builder's `assembleDebugAndroidTest` success is the attributed evidence, and the changed sources are unchanged since (15/15 OK).

## Notes (non-blocking; IDs continue the task sequence)

- **P7-N8 — leaderboard interpretation (owner confirmation).** While any enabled block has a site, every installed supported browser shows a row, including a 0m row for an unused browser. The alternative, "only browsers with a site outcome", is a one-line change in `StatsRepository.leaderboardPackages`. Confirm during P7-S acceptance. No code change requested.
- **P7-N9 — site push-throughs can make spent unknown more often.**
  - A site pass now enters the spent measurement, but the site completion path still uses a plain `finish()`. That navigation was accepted in Phases 5/6. BlockActivity runs in Toki's task, so finishing can reveal Toki rather than the browser.
  - If the user does not reach the browser within 30s, the pass stays unmeasured. The day's and week's spent then shows "—" with "Some visits couldn't be measured". This is conservative and §9-correct (never 0), but could become a frequent owner-visible state.
  - Observe in P7-D3 and P7-S acceptance. A possible later fix is to return to `hostPackage` on site completion. That is a navigation change and needs an owner decision; it is not in N6 scope.
- **P7-N10 — site nope valuation.** A counted site nope is saved at the browser's whole-app usual visit length, or the 10-minute fallback when there are fewer than 3 visits or no baseline. This is not specific to the site. It follows directly from §9's per-package baseline now that the browser is the Stats app. Record only.

## Remaining Android-execution and owner evidence (NOT VERIFIED; do not close Phase 7 without them)

1. **P7-D2 re-run** in the isolated environment (AVD `toki-p7d-isolated` can be reused) for the N6-changed instrumented sources:
   - `StatsRepositoryTest`: 6 cases, including the replaced `siteNopesAttribute…` case and the new `enabledSiteBlockShowsHostingBrowserRow…`.
   - `ChallengeRepositoryTest`: 9 cases, including the new `siteWalkAwayAndPushThroughAttribute…`.

   The recorded `OK (14 tests)` predates N6. P7-D1 (`StatsMigrationTest`) does not need a re-run because there is no schema change, but running it together is cheap.
2. **P7-D3**: real `AndroidUsageSource`/OEM behavior, now also covering browser visits after a site challenge (P7-N9) and the browser row on the real device.
3. **P7-D4**: on-device active-event payload / once-per-entry audit. Raw site events must keep the domain target.
4. **P7-O1**: the separately attributed final build, installed over the existing v3 install without uninstall or clear.
5. **Arjun's experience/visual acceptance**: P7-S1–S10, including the heavy-day minutes-only labels (S6/O21) and the N6 browser-row behavior (P7-N8/N9 observations), plus the retained checks in [OWNER-CHECKS](OWNER-CHECKS.md).

## Next

No code changes requested. Arjun decides: authorize the isolated P7-D2 re-run (with D1) and the attributed final build for P7-O1/D3/D4 and experience acceptance, or optionally route a Codex cross-check of N6 first. Phase 7 stays open through the evidence above, then commit and close.

# TS-block-wizard-redesign — HANDBACK

- **State:** `ready_for_review`. Initial implementation by GLM (Flash session), senior repair by Codex for WZ-F01/WZ-F02, and Codex direct visual correction DC-06 on 19 September 2026. DC-06 requires an independent narrow review and one owner visual retest; this record is implementation evidence, not review.
- **Base:** `f92661d` (HEAD verified before work). No commit was made — the submission is the **staged working-tree diff on top of `f92661d`** (see file list below). Unrelated uncommitted docs/design/branding/report work was preserved unstaged.
- **Owner checklists:** Arjun now reports BW-01–BW-10 passing, except for the DC-05 sheet-tone observation recorded below; [OWNER-CHECKS](OWNER-CHECKS.md) preserves the detailed owner evidence. Arjun owns device/experience checks.

## Codex repair — WZ-F01 / WZ-F02

- `CreateFlowState` now saves a stable logical `flowId`; asynchronous ownership moved out of the composition into an Activity-retained `CreateFlowOperationStore`/`CreateFlowSession`. One FIFO owns `block_create_started`, step completions, Save and abandonment across activity recreation. The terminal guard lives with that retained session, so a recreated screen cannot issue a second Save/Back while the first terminal operation is suspended.
- Outcomes are replayed to the current composition. A completed operation closes through the recreated screen's current callback; a stale-target rejection releases the retained guard and restores the inline error without losing the draft. Completed sessions are removed, avoiding retained-flow accumulation.
- Creation now commits the block, step 5 and `block_created` in one Room transaction through `EventRepository.atomically`; editing likewise commits the update with `block_edited`. Cancellation cannot expose a saved/edited row without its terminal event. Steps 1–4 must drain in FIFO order before this terminal transaction begins.
- Added two executed JVM tests for FIFO ordering and retained terminal exclusion. Added three compile-only instrumented cases that gate `block_create_started`, an earlier step, and `block_created` across navigation/recreation, asserting one ordered history, one block and one close when executed.
- Repair files: `EventRepository.kt`, `CreateFlowScreen.kt`, `CreateFlowStateTest.kt`, and `CreateFlowScreenTest.kt`. The accepted wizard visuals/schema/boundary validation were otherwise preserved.
- Exact repair-delta blob anchors (pre-repair → staged repair), retained in Git for reviewer inspection: `EventRepository.kt` `465b9de` → `81a5b55f`; `CreateFlowScreen.kt` `bd06367` → `4f39e214`; `CreateFlowStateTest.kt` `fd45cc9` → `c7ec186b`; `CreateFlowScreenTest.kt` `71193e7` → `13d5337c`. Use `git diff <old> <new>` for each named file; these anchors exclude the already-reviewed GLM delta.

### Repair checks (fresh)

| Command | Result |
|---|---|
| `./build.sh assembleDebug` | BUILD SUCCESSFUL |
| `./build.sh testDebugUnitTest` | BUILD SUCCESSFUL — result XML: **60 tests, 0 failures, 0 errors, 0 skipped** (`CreateFlowStateTest` 15; +2 repair tests) |
| `./build.sh assembleDebugAndroidTest` | BUILD SUCCESSFUL — **compile-only**; no instrumented test executed |
| hex-literal grep (`0xFF…` outside `ui/theme/`) | 0 matches |

The three new recreation/Room scenarios remain compile-only evidence until separately authorized execution. No device operation was performed. Production process-death recovery beyond Room's new atomic terminal boundary is not claimed; activity recreation is the supported draft/operation restoration case under this task.

## Direct owner corrections — 19 September, round two

Reported by Arjun against the installed 21:45 build; bounded corrections under the direct owner→GLM path (WORKFLOW). Expected = owner message; observed = behavior on the installed repair build.

| ID | Expected | Observed on 21:45 build | Correction |
|---|---|---|---|
| DC-01 | Welcome headline `your time, your rules`; body lines `Doomscrolling? Time-blindness? We got you covered`, `This isn't a block.`, `You choose what gets paused, for how long, and how you get back.`; the screen's mark is the app icon | Headline `Your time, your rules`; body per the earlier Phase 3 welcome corrections ("Yeah, we got you." / vibe-check line); mark was the Hourglass glyph | `WelcomeScreen` copy replaced verbatim; the glyph box replaced by the app's own launcher icon — `getApplicationIcon` rasterized at 256 px in the same 76 dp rounded treatment |
| DC-02 | **Adjust the details** shows the sliders-horizontal glyph (owner attachment) | The GLM-added 0xe6c2 rendered an unrelated glyph ("H₄"-like): the codepoint exists in the bundled font's cmap but is not the sliders glyph | `Ph.SlidersHorizontal` corrected to **0xe434** per the @phosphor-icons/web 2.1.1 stylesheet (the project's documented codepoint source); the stylesheet was cross-validated against the bundled font via eight known-good glyphs before use |
| DC-03 | Both method cards carry the tick circle — selected filled+checked, unselected an empty ring (reference Friction-selection.png) | Circle appeared only on the selected card | Circle always rendered: accent-filled checked circle when selected, 1 dp neutral-500 empty ring otherwise |
| DC-04 | Disable step explainer reads `Disable the block to edit or delete the block. It needs to be a little inconvenient!` | Read `Tell us how you prefer to disable the block. It needs to be a little inconvenient!` | Copy replaced; the owner message supersedes that §17 amendment sentence |
| DC-05 | The details sheet reads as the flat Nocturne surface like the reference (mock selection image 4) | Sheet rendered washed-out lavender: material3's default tonal elevation composites the dark theme's light-purple surface tint over the container color | `ModalBottomSheet` given `tonalElevation = 0.dp` so `containerColor = surface` (#232532 token) renders exactly; no new color introduced |

Affected owner checks: BW-02 (DC-02/DC-03/DC-05 card, sheet and sheet-color visuals), BW-05 (DC-04 disable-step copy). DC-01 sits in the Phase 3 welcome-presentation scope — this owner message supersedes that Phase 3 correction entry; no BW ID.

### Correction checks (fresh, this session)

| Command | Result |
|---|---|
| `./build.sh assembleDebug` | BUILD SUCCESSFUL (latest: 22:36, includes DC-05) |
| `./build.sh testDebugUnitTest` | BUILD SUCCESSFUL — result XML: **60 tests, 0 failures, 0 errors, 0 skipped** |
| `./build.sh assembleDebugAndroidTest` | BUILD SUCCESSFUL — **compile-only** |
| hex-literal grep (`0xFF…` outside `ui/theme/`) | 0 matches |

Repair files touched this round: `WelcomeScreen.kt`, `CreateFlowScreen.kt` (MethodCard circle, disable copy, sheet elevation), `Phosphor.kt` (codepoint). The corrected build was keep-data installed on Arjun's SM-S918B (`R5CW30ZBM2R`) on 19 September 2026 at 22:36 (DC-01–DC-04 installed 22:30 on the owner's explicit request; DC-05 installed 22:40 on the owner's explicit request) and launched once — setup only; visual outcomes remain owner-side evidence.

## Codex direct correction — DC-06 details-sheet darkness

- **Reported issue:** the owner reports BW-01–BW-10 passing, but the supplied post-DC-05 screenshot shows the modal lighter than the approved reference. DC-05 removed tonal elevation, but retained the lighter `surface` container and a semi-transparent neutral-900 scrim that lightens `bg` rather than dimming it.
- **Correction:** `DetailsSheet` now uses the existing darker Nocturne `bg` token as its container, still with zero tonal elevation. The canonical `scrim` token is a 50% black scrim, so the underlying wizard darkens rather than lifting toward neutral-900. No layout, draft, navigation, persistence, event or test logic changed.
- **GLM correction verification:** DC-01 welcome copy/icon, DC-02 sliders glyph, DC-03 selected/unselected method indicators and DC-04 disable copy match their recorded source requirements; the owner reports their affected cases pass. DC-05 is confirmed insufficient by the supplied screenshot, not accepted as a visual pass.
- **Fresh checks:** Codex ran `./build.sh assembleDebug` and `./build.sh testDebugUnitTest`: both BUILD SUCCESSFUL. Result XML reports **60 tests, 0 failures, 0 errors, 0 skipped**. Working-tree and staged diff whitespace checks are clean; no out-of-theme Kotlin hex literals were found. No device, emulator or instrumented test ran.
- **Owner retest:** inspect only the details sheet after DC-06: its container should match the dark reference surface and the underlying method screen should be visibly dimmed. This is visual/device evidence and cannot be inferred from the build.

## Submission (staged files)

Main sources:
- `app/src/main/java/com/arjunrana/tokishrine/data/entity/Block.kt` — `show_typos` column removed; `turnoff_seconds` added; per-method column contract documented.
- `app/src/main/java/com/arjunrana/tokishrine/data/db/TokiDatabase.kt` — schema v2 → v3, deliberate development reset (see consequences).
- `app/src/main/java/com/arjunrana/tokishrine/data/repo/BlockRepository.kt` — `BlockDraft` gains `turnoffSeconds`, loses `showTypos`; shared `requireValidConfiguration` enforces pause 5–100 min/step 5, passage 100–200 chars/step 10, pause wait 60–300 s/step 5, and fixed ladders 220/350/700 chars + 180/360/720 s on create **and** update; canonical range/step/ladder/default constants live here (single source shared with UI and tests). `MAX_STORED_COUNTDOWN_SECONDS` (1..1200 allowance) removed as superseded. Ownership/rollback/transaction guards untouched.
- `app/src/main/java/com/arjunrana/tokishrine/ui/screens/CreateFlowScreen.kt` — five-step wizard (contents, name, method, disable, review) with 1/5–5/5 labels and 5-segment dots; method cards (typing preselected) with live plain-words summaries; outlined in-layout **Adjust the details** opening a material3 `ModalBottomSheet` (selected method's field + pause only, Reset/Done, min-5-minutes footnote, live draft updates, no save, no step event); step 4 disable cards inherited from the method (fixed ladders, middle preselected+Recommended, per-method memory); review updated to per-method pause/disable summaries with phone-in-hand copy removed; save logs step 5 then `block_created` (payload adds `turnoff_seconds`); `fields_changed` tracks `turnoff_seconds`, no `show_typos`; `CreateFlowStateSaver` (rememberSaveable) preserves the full draft across activity recreation (BW-06) with edit-prefill guarded by a saveable `prefilled` flag; `block_create_started` fires once per logical flow across recreation; sheet Back dismisses the sheet before wizard Back; step/terminal guards, abandonment-once, edit-silence and rejected-save handling unchanged.
- `app/src/main/java/com/arjunrana/tokishrine/ui/screens/BlockDetailScreen.kt` — "To turn off" line is method-specific (typing chars + estimate / waiting minutes).
- `app/src/main/java/com/arjunrana/tokishrine/ui/screens/TurnOnScreen.kt` — turn-off summary method-specific; "Phone in hand, every time." removed (PRD §7.2: the phone may be put down).
- `app/src/main/java/com/arjunrana/tokishrine/ui/icons/Phosphor.kt` — `SlidersHorizontal` corrected to 0xe434 for the Adjust the details button after the prior 0xe6c2 rendered the wrong glyph; no dependency added.

Tests:
- `app/src/test/java/com/arjunrana/tokishrine/ui/screens/CreateFlowStateTest.kt` — **new** JVM suite (13 tests): defaults, entry-step clamp, bounds/steps for all three steppers, sheet Reset scope, per-method retention on method switching, fixed ladders, middle preselection, per-method disable selection, draft→column mapping.
- `app/src/androidTest/java/com/arjunrana/tokishrine/ui/CreateFlowScreenTest.kt` — five-step navigation/events (steps 1..5, back-re-logs, save logs 5), created-payload assertions for the new defaults/fields, waiting-method end-to-end round trip (card switch → sheet contents → ladder rung → review summaries → stored row → event payload), sheet-Back-before-wizard-Back, create-draft recreation survival, edit-draft recreation survival without prefill overwrite; legacy `show_typos` rewrite test removed with the feature; all prior regressions retained on the new step count.
- `app/src/androidTest/java/com/arjunrana/tokishrine/data/BlockRepositoryTest.kt` — fixtures on the new draft shape; `turnoff_seconds` round-trip; new boundary tests: pause-wait range (create+update), pause-minutes range, passage range/step, both ladders off-rung rejected (create+update), every approved rung round-trips. Prior ownership/rollback/cascade/empty-draft coverage unchanged.

## Commands and results (fresh, this session, JDK/SDK env via `./build.sh`)

| Command | Result |
|---|---|
| `./build.sh assembleDebug` | BUILD SUCCESSFUL |
| `./build.sh testDebugUnitTest` | BUILD SUCCESSFUL — result XML: **58 tests, 0 failures, 0 errors, 0 skipped** (was 45 at `f92661d`; +13 `CreateFlowStateTest`) |
| `./build.sh assembleDebugAndroidTest` | BUILD SUCCESSFUL — **compile-only**; no instrumented test was executed |
| hex-literal grep (`0xFF…` outside `ui/theme/`) | 0 matches |

Per-class JVM counts: AndroidManifestTest 1, OemBatteryTest 5, PermissionEventLogicTest 18, StatsCalculatorTest 7, RouteCodecTest 4, **CreateFlowStateTest 13 (new)**, DomainTest 5, TerminalActionTest 5.

## Schema reset consequences (owner-approved, 19 September)

- v2 → v3 with the existing dev-phase `fallbackToDestructiveMigration()` in `TokiApplication` is the deliberate reset: on first launch after installing this build, **all existing local blocks, targets, events and onboarding markers are erased and rebuilt**. No legacy mapping or data-preserving migration exists; no recurring deletion was added (normal startups never wipe).
- `show_typos` no longer exists; typo highlighting is unconditional everywhere and Phase 5 must not consult any stored preference.
- Old block/event/onboarding test fixtures based on v2 rows are invalid; instrumented fixtures in the suite were rebuilt for v3. No device data was touched in this assignment — no device operation was performed.

## Limitations and evidence gaps (do not waive)

- Instrumented suites are **compile-only** evidence; Room round-trips, transaction rollback, event payloads, step numbering, sheet behavior and recreation survival on hardware remain unproven until run/observed (BW-01–BW-10, and instrumented execution itself is a separate authorization).
- JVM self-verification does not prove SQL atomicity; the Phase 3 transactional-evidence gap is untouched.
- Visual/layout conformance (cards, sheet, disable step, insets, large-font/short-height per BW-08) is source- and token-level only; owner visual pass pending. The sheet uses material3's built-in drag handle; the mock's exact sheet metrics were approximated with existing Nocturne tokens.
- The sheet's exit uses direct dismissal (no hide-animation choreography); functionally complete, visually to be confirmed by the owner.
- An edit prefill of a directly dao-seeded block with off-range values would be rejected at save (IllegalArgumentException, not the ownership inline path); unreachable through the app and post-reset data, noted for fixture authors.
- Production migration policy remains unresolved (pre-existing contract obligation), and Phase 3 acceptance remains open with its own evidence gaps. Waiting/typing challenge **execution** is untouched and stays Phase 5.

## Review pointers

Contracts: [blocks/targets](../../../docs/components/blocks-targets.md), [persistence/events](../../../docs/components/persistence-events.md), PRD §7 + §17 wizard amendment. Highest-risk areas: the new `CreateFlowStateSaver` (draft/recreation, entry-step floor, prefill guard), validation strictness at the boundary (both per-method columns always ladder-valid), and the five-step event ordering in `CreateFlowScreen`. Component-contract updates are deferred to review/closure per RECORDS.

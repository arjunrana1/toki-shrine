# Phase 2 verification — FAIL

Reviewed commit `8f1ad1ecbec78ea3abfed396028902fa3ac977f6` on 9 September 2026. **Do not begin Phase 3 until the blocking findings below are fixed and independently re-verified.** No production code changed by the verifier.

## Owner update after this review

On 9 September 2026 the owner replaced the app conflict/move UX: keep apps owned by another block visible, label them **Already added to a block**, omit the owning block name, and make them unselectable regardless of the owner's ON/OFF state. Remove the app conflict dialog and move action. Repository saves must reject conflicting app ownership atomically instead of transferring it. Apps belonging to the block currently being edited are not “another block.”

This supersedes the original fix suggestions below to retain confirmed app moves or repair the app sheet. Removing that flow resolves its modal issue; the Android Back finding still requires repair. The owner subsequently confirmed the same rule for websites: show the same inline message and prevent adding an already-owned domain. Remove the conflict/move flow for both types and reject conflicting saves atomically. The observations below remain the historical findings against 8f1ad1e, not a sign-off on the new behavior.

## Checks completed

Fresh `./build.sh testDebugUnitTest connectedDebugAndroidTest --rerun-tasks`: exit 0; 7 JVM and 16 instrumented tests passed on the USB Samsung S23 Ultra / Android 16. Debug APK reinstall succeeded and launched. The prescribed hex grep returned no matches.

Independently drove all seven Phase 2 acceptance scenarios: UI-created block saved OFF; counters 1/4–4/4 and advancing segments; typing controls defaulted to 15 minutes / 100 / 300 characters; estimate changed from 30 to 33 seconds at 110 characters; conflict named the owner and Leave returned with zero selected apps; ON detail hid edit/delete and displayed the off-first message; searching toki returned no apps. These happy-path checks pass, but the protection is bypassable as below.

Compared the empty list, typing configuration, review and conflict screenshots against the supplied renders. Broad visual structure is consistent, allowing for phone dimensions and the required third control. This is not an independent endorsement of GLM's entire 11/11 visual claim or every edit/delete/event path. Further exhaustive visual work is deferred until the blocking behavior is repaired.

## Blocking findings

### 1. P1 — Conflict moves can remove targets from an ON block

**Code:** `app/src/main/java/com/arjunrana/tokishrine/data/repo/BlockRepository.kt:61–75`, also `:83–115`, `:159–165` and the conflict move action in `ui/screens/CreateFlowScreen.kt`.

**Device reproduction:** Created Review% with Instagram and reddit.com, turned it ON, confirmed detail says it must be off to edit. Created MovedTarget, selected Instagram, chose Move it here and saved. Review% remained enabled but lost Instagram; MovedTarget was OFF and owned Instagram. This is the precise free removal bypass PRD §4 forbids. Direct OFF being temporarily ungated until Phase 5 does not justify a second removal bypass that survives that gate.

**Fix:** Check source and destination block state at the transaction boundary. Refuse to remove/reassign targets from an ON source or edit an ON destination; give the UI a clear off-first result. Carry explicit move consent/expected source ownership with the draft, so an unconfirmed or changed ownership conflict cannot be silently moved by create/update. Current BlockDraft has no consent information and every held target is automatically reassigned.

**Regression:** Cover app and site moves from ON sources, normal confirmed moves from OFF sources, ownership/state changing before save, abandonment before save, and all-or-nothing failure. The Phase 1 failed-save rollback regression was replaced by successful normalization/move coverage (`BlockRepositoryTest.kt:176`); a successful transaction does not test rollback. Restore a real failure-path assertion without undoing legitimate confirmed moves.

**Evidence:** `evidence/09-on-detail.png`, `evidence/13-on-source-stripped.png`, and `evidence/device-probes.txt`: block 1 enabled=1, block 2 enabled=0, Instagram belongs to block 2.

### 2. P2 — Conflict sheet allows interaction through its backdrop

**Code:** `app/src/main/java/com/arjunrana/tokishrine/ui/screens/CreateFlowScreen.kt:905–919`.

**Device reproduction:** Search instagram, open its ownership conflict, then tap the dimmed Layout row above the sheet. Layout becomes Added while the Instagram conflict remains open. The backdrop is only a background-painted Box and does not intercept input.

**Fix:** Make the sheet actually modal, prevent interaction with underlying controls, and route dismissal consistently through the keep/decline path. Retest with the keyboard focused as well as hidden. DECISIONS.md currently claims a separate Compose Dialog window and scrim-tap dismissal, but the committed implementation uses neither; correct the record to match the final tested implementation. Do not carry that inaccurate implementation claim into Phase 5.

**Evidence:** `evidence/12-conflict-clickthrough.png` shows Layout Added underneath the still-open Instagram conflict.

### 3. P2 — Android Back skips the flow's back handling and abandonment event

**Code:** `app/src/main/java/com/arjunrana/tokishrine/MainActivity.kt:45–46`; `ui/screens/CreateFlowScreen.kt:173–185`.

**Device reproduction:** Start a new block, advance to step 2 without opening the keyboard, press Android Back. It returns directly to the block list, unlike the appbar Back which returns to step 1. The draft is discarded and no block_create_abandoned event is recorded. The final two rows in device-probes.txt are create_started and step_completed(step=1), with no abandonment after exiting.

**Fix:** Give the flow its own Back handling, prioritizing conflict dismissal, app-search exit, previous step, then actual flow exit. Route genuine exit through the abandonment logger exactly once, with the correct step. Check create and edit routes and keyboard behavior on the device.

## Non-blocking notes / carry-forward

- Route and draft state use plain remember (`MainActivity.kt:45`, `CreateFlowScreen.kt:129`), so activity recreation loses them. Preserve navigation and draft state with an appropriate saved-state mechanism. This was identified by code inspection; recreation was not exercised in this run.
- Direct OFF until Phase 5, permission gating in Phase 3, and inactive future destinations are accepted phase boundaries. They are not findings.
- The 0.3 seconds/character estimate satisfies the explicit 100 ≈ 30 acceptance point. Keep the conflicting PRD prose visible as a documentation discrepancy; this report does not invent a new owner decision on the rate.
- Destructive DB migrations remain a development-only decision to revisit before distribution.
- Glyph-only clickable controls lack useful accessibility labels/roles; add semantic labels and toggle states as the shared controls mature.

## Evidence and workspace hygiene

Kept one build/test log, one small database query transcript and three defect screenshots. Successful-path screenshots were discarded after inspection. The original app database was backed up before the harness wiped it, then restored; The scroll pit and News spiral are back on the phone. No verification fixture blocks remain in the restored data.

Existing branding edits in DECISIONS.md, deleted icon concepts, and untracked design/icon-final were left untouched. No commit or push performed by the verifier.

## Code-only repair review — 10 September 2026

**FAIL — a0df32c against 8f1ad1e. Owner device validation pending; Phase 3 not cleared.** App source matches HEAD. Existing documentation and branding changes were preserved. No builds, tests or device operations performed. Existing builder XML reports show 7 JVM and 17 instrumented tests, zero failures/errors; these cover repository/Stats behavior, not create-flow navigation.

Ownership writes now reject atomically for both target types, move APIs/dialogs are removed, and ordinary Back follows search/step navigation. Two remaining P2 blockers:

- `CreateFlowScreen.kt:150–158`: abandonment launches an asynchronous event write without a synchronous exit guard. Repeated Back/appbar actions while logging is suspended can enqueue multiple abandonment events and close callbacks. Save also remains available during this interval. Guard the terminal transition before launching work, share it with save, capture the exit step, and prove repeated input produces exactly one terminal event/navigation action.
- `CreateFlowScreen.kt:194–204`, `:285–296`, `:444–448`: ownership starts as an empty map, allowing selection before loading completes (apps are published before their ownership read). A resulting conflict or stale draft is caught and silently closes the editor, discarding the draft. Keep selection disabled until ownership is known; on rejected save retain the draft and show the inline unavailable explanation without naming an owner or restoring a conflict dialog. Verify with delayed ownership reads and forced save rejection.

Builder follow-up: deterministic regression evidence for both paths; event-table assertions for create success/cancel, step/search Back, edit cancellation and retired conflict events; site-conflict update rollback after earlier app mutations, checking the complete before/after block state. Existing rollback tests are meaningful but do not cover these UI/event paths. Owner checklist is supplied in the review response. Prior saved-state, accessibility-semantics and development-only migration notes remain open.

## Latest Phase 2 checkpoint — 13 September 2026

**Current checkpoint — 13 September 2026: code PASS, `6c3b962`.** Codex reviewed `084b5eb..6c3b962`: unchanged-edit fixture corrected, legacy typo normalization explicitly covered, enabled-state assertions corrected, helper copy updated, and interior multiline website paste rejected without merging. No blocking findings in the corrective diff. Phase 3 implementation is cleared from `6c3b962`; verify git state. GLM reports debug build and 12 JVM tests passing, instrumented sources compiled only. Codex ran no builds/tests/device operations. Latest UI regressions and paste behavior remain pending owner device confirmation; this is implementation clearance, not a new device pass. Haptic tuning remains pending; Phase 3 must emit activation success feedback after permission checks and successful persistence.

Reviewed `fe836dc..512dc36`, building on prior reviewed refinements. No app-source modifications outside HEAD. This historical clearance is superseded by the latest checkpoint and correction below.

## Codex review of round-two/three refinements — 13 September 2026

Reviewed `512dc36..084b5eb`, including affected input/save/event/haptic paths. Source unchanged by reviewer. Required: `CreateFlowScreenTest.kt:470` expects `none` despite its `showTypos=false` fixture becoming true on save (`CreateFlowScreen.kt:133,334`). Use an always-on fixture for the unchanged-edit scenario and retain explicit normalization coverage if testing legacy false values. At test line 505, wait for enabled semantics rather than a click action before Add. Compilation cannot establish these runtime assertions. No broad test rerun or feature rewrite requested.

Notes: newline stripping merges `reddit.com\nabdes` into syntactically valid `reddit.comabdes`; raw validator tests do not demonstrate rejection of that UI input. Owner should choose rejection of multiline paste if silent merging is unwanted; do not assume registered-TLD validation. Current haptics fire before the persistence coroutine (TurnOnScreen.kt:174; BlockListScreen.kt:117; BlockDetailScreen.kt:88). Phase 3 activation gating must emit success feedback only after permission checks and successful activation. Owner finds haptics weak and floated +30% duration; no exact tuning change approved. Current preset effects contain no app-defined duration. Disable helper uses “the block” where owner requested “this block”; match the requested copy in the bounded correction.

Phase 3 remains onboarding/permissions only, using GLM 5.3 for lifecycle/permission logic. Start after corrective diff approval; retain owner-led device validation of the newer changes as a separate gate. No device checks were executed by Codex.

## Corrective diff closure — 13 September 2026

**Current checkpoint — 13 September 2026: code PASS, `6c3b962`.** Codex reviewed `084b5eb..6c3b962`: unchanged-edit fixture corrected, legacy typo normalization explicitly covered, enabled-state assertions corrected, helper copy updated, and interior multiline website paste rejected without merging. No blocking findings in the corrective diff. Phase 3 implementation is cleared from `6c3b962`; verify git state. GLM reports debug build and 12 JVM tests passing, instrumented sources compiled only. Codex ran no builds/tests/device operations. Latest UI regressions and paste behavior remain pending owner device confirmation; this is implementation clearance, not a new device pass. Haptic tuning remains pending; Phase 3 must emit activation success feedback after permission checks and successful persistence.

The preceding round-two/three regression findings are resolved by `72de89c` and `6c3b962`. Do not reopen that repair or rerun the full Phase 2 audit without a concrete regression. Use GLM 5.3 for Phase 3 implementation and Sol High for the initial independent lifecycle/permission review; Medium is suitable for a later small corrective diff. This is task-fit guidance, not a vendor benchmark ranking.

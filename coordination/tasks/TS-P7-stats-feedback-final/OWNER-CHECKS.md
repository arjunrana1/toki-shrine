# Owner checks — TS-P7-stats-feedback-final

- **Status:** prepared by the GLM 5.3 implementer, 23 September 2026. Not executed. Arjun owns all device/experience testing; no device operation was performed while preparing this list.
- **Build under test:** to be filled at installation (fresh debug APK from the Phase 7 submission; record SHA-256 and tree attribution when installed). Install with `adb -s R5CW30ZBM2R install -r` only (data preserved) — the P7-O1 row depends on it.
- **Verification method note:** assertions marked **(DB)** need the event table read by the owner's chosen authorized method (previous phases accepted source/instrumented evidence where no extraction was performed). Visual surrogates are stated where they exist. Executed non-device evidence for this submission: 194/194 JVM tests, `assembleDebug`/`assembleDebugAndroidTest`/`assembleRelease` compile, instrumented sources compile-only.

## Migration and data preservation (run first)

| ID | What to test | Steps to follow | Fail conditions | Pass conditions |
|---|---|---|---|---|
| P7-O1 | Upgrade preserves existing data (destructive fallback removed) | Install the Phase 7 build with `install -r` over the current Phase 6 install (no uninstall, no clear). Launch the app. Open the block list. | Any block, target, or onboarding state lost; crash on launch; database error. | All previously existing blocks appear with names/contents/toggles intact; app opens normally (schema stays v3; no migration runs). |
| P7-O2 | Historical event rows survive (DB) | After P7-O1, query the event table for `challenge_abandoned` / `bubble_dragged` rows that existed before the upgrade. | Previously present historical rows gone. | Historical rows still present and untouched (they are simply never re-emitted). |

## Stats — screen 23

| ID | What to test | Steps to follow | Fail conditions | Pass conditions |
|---|---|---|---|---|
| P7-O3 | Stats entry point and `stats_viewed` (DB) | From the populated home screen tap the bar-chart header icon (top right). Re-enter twice more. | Screen does not open; more `stats_viewed` rows than intentional entries (one per entry). | "Your walk-aways" screen opens; exactly one `stats_viewed` row per intentional entry. |
| P7-O4 | Figures against a known fixture | Perform a known set of interruptions (e.g., 3 walk-aways today via *Not now*, 1 completed typing challenge) then open Stats. | Totals/this week/best day/rate disagree with the performed actions; rate shows NaN/blank; rate counted against turn-offs or abandonments. | Total walk-aways, this week, best day match the actions; walk-away rate = walk_aways ÷ (walk_aways + completed challenges) (e.g., 3 walk-aways + 1 completion → 75%). |
| P7-O5 | Empty data zeros | On an install with no walk-aways yet (or before any interruption), open Stats. | Crash; NaN; negative days; leaderboard header with no rows. | Hero 0, "since you started, today", cards 0 / 0 / 0%, leaderboard section absent. |
| P7-O6 | Leaderboard correctness | Trigger walk-aways on two different apps (and a site if available), then open Stats. | Site domains appear in the leaderboard; raw package names shown (e.g., `com.instagram.android`); order not descending; apps tied to deleted blocks missing (events survive deletion). | Only apps listed with user-visible labels (Instagram, not the package); counts descending; site walk-aways still counted in global totals (hero/this week). |
| P7-O7 | Days active line | Compare the "since you started, …" line with the real first-launch day. | Wrong day count; "0 days ago" on launch day. | N days after first launch; "today" on the launch day itself. |

## Feedback — screen 25

| ID | What to test | Steps to follow | Fail conditions | Pass conditions |
|---|---|---|---|---|
| P7-O8 | Both entry points | (a) Home with ≥1 block: tap the bottom **Feedback** button. (b) Settings: tap **Send feedback**. | Either entry dead or opening the wrong screen. | Both open the "Send feedback" screen with headline "How's it going?" and body copy. |
| P7-O9 | No diagnostic-log surface | Inspect the whole screen and the app. | Any "Attach a diagnostic log" toggle/copy anywhere; any attachment UI. | Only the text field and *Send it*; no log/attachment control exists. |
| P7-O10 | Draft preservation | Type text, rotate/turn the screen off and back on, and (if convenient) force-stop and reopen the app into the screen. | Typed text lost on recreation/process death. | Draft text preserved. |
| P7-O11 | Successful handoff (mail app present) | Type a distinctive line, tap **Send it**. | No mail app opens; wrong recipient/subject/body; an attachment added; `feedback_sent` logged without a handoff (DB). | The mail client opens addressed to arjranaprep@gmail.com, subject "Feedback from user", body exactly the typed text, no attachment; one `feedback_sent` row (DB) means handoff succeeded — it is not delivery confirmation. Returning to Toki lands on the home screen. |
| P7-O12 | Graceful failure (no mail app) | Disable all mail-capable apps (or use a profile without any), tap **Send it**. | Crash; silent nothing; `feedback_sent` row written (DB); draft lost. | Inline message ("No email app could be reached…"), draft and screen intact. |
| P7-O13 | Keyboard behavior | Focus the field and type several lines. | *Send it* unreachable behind the keyboard; text field clipped; screen does not scroll. | Field grows/scrolls; *Send it* stays reachable with the keyboard open. |

## Production configuration values (all variants)

| ID | What to test | Steps to follow | Fail conditions | Pass conditions |
|---|---|---|---|---|
| P7-O14 | Debug build uses production bounds | In the create flow's details sheet: step the typing passage down repeatedly; step the wait down repeatedly; open the disable step for both methods. | Any 20-char/20-second floor or 20/… disable rung still present in debug. | Typing floors at 100 (default 150, max 200, step 10); wait floors at 60 (default 60, max 300, step 5); disable ladders 220/350/700 chars and 180/360/720 seconds with the middle preselected; pause duration 5–100 min (default 15). |
| P7-O15 | Existing owner-test blocks still valid | Blocks created with the temporary debug minima (e.g., 20-char pause typing) remain as stored; try editing one. | Data wiped or rejected on open; enforcement treats stored values as invalid for display. | Stored blocks open/edit normally; new writes enforce production values only (a re-save must move values into the production ranges). |

## Event taxonomy audit

| ID | What to test | Steps to follow | Fail conditions | Pass conditions |
|---|---|---|---|---|
| P7-O16 | Full active-event run-through (DB) | Perform one full manual pass: onboarding steps (fresh install if acceptable — otherwise the subset still reachable), create/abandon/edit/delete a block, turn on/off, trigger the block screen for an app and a site, complete typing and delay pause challenges, walk away from each surface (block screen, typing, countdown), turn a block off via challenge and abandon one turn-off, pause to expiry, bubble shown/tapped/dismissed, open Stats/Settings/Feedback, send feedback. Query the event table afterwards. | Any **active** event from PRD §10 missing entirely; unknown event names. | Every active event fired at least once: onboarding_started, permission_requested/granted/denied, onboarding_completed, block_create_started/step_completed/abandoned, block_created/edited/deleted/turned_on/turned_off, block_screen_shown, walk_away, challenge_started/completed, typing_mismatch, countdown_started/completed, pause_started/expired, bubble_shown/tapped/dismissed, turnoff_started/completed/abandoned, stats_viewed, settings_viewed, feedback_opened, feedback_sent, accessibility_connected/disconnected. Conditional: `url_read_failed` fires only on a real address-bar read failure — acceptable to leave unfired with a note; `permission_denied` needs one denied permission request. |
| P7-O17 | Retired events stay retired (DB) | After the run-through (including deliberate bubble drags without dismissal, app-switches/locks/Back mid-challenge), query for new `challenge_abandoned` / `bubble_dragged` / `block_conflict_shown` / `block_conflict_resolved` / `countdown_stalled` / `countdown_resumed` rows newer than the Phase 7 install. | Any new row with a retired name. | No new retired-name rows; pre-existing historical rows untouched. |
| P7-O18 | Bubble drag behavior unchanged | During a pause, drag the bubble around and release away from the bottom edge; then drag to the bottom edge and release. | Drag broken; dismissal broken; dismissal no longer logs `bubble_dismissed` (DB). | Dragging still works with no event; drag-to-dismiss still discards the pill and logs `bubble_dismissed`; a new pause re-shows the bubble. |
| P7-O19 | Challenge suspension still nonterminal | Mid typing and mid delay challenge: switch apps, lock the screen, and press Back; return each time. | Challenge terminated; any terminal event written (DB); typing text lost. | Session preserved (typing text retained, waiting reset to zero); no walk-away/abandonment recorded. |

## Accessibility and final UI

| ID | What to test | Steps to follow | Fail conditions | Pass conditions |
|---|---|---|---|---|
| P7-O20 | Semantic labels/roles | With TalkBack: navigate the home header (Stats/Settings buttons), any screen's app bar (Back), Stats leaderboard, Feedback field and button. | Unlabeled glyph buttons ("button" only or glyph noise); focus trapped. | Home header buttons announce "Stats"/"Settings"; app bar announces "Back"; all controls have names/roles; focus order sensible. |
| P7-O21 | Large text / small screen | Enable largest font size (and smallest display size if available); visit Stats with a long leaderboard and Feedback with a long draft. | Clipped/cut-off figures; unreachable *Send it*; hero number overlapping cards. | Both screens scroll; all figures and controls remain readable and reachable. |
| P7-O22 | Insets and tokens | Inspect Stats and Feedback against the mock renders (23/25). | Status/nav bar overlap; colors off-token (e.g., pure black/white, wrong accent on the hero); outlined-button rules violated. | Both screens sit inside the system bars; Nocturne tokens only; copy matches §6 screens 23/25 (including removed diagnostic-log row). |

## Phase 1–6 regression smoke

| ID | What to test | Steps to follow | Fail conditions | Pass conditions |
|---|---|---|---|---|
| P7-O23 | Core loop still intact | Onboarding checklist states; create a block (apps + a site); turn on (permission gate if accessibility missing); launch a blocked app → block screen → walk away → walk-away moment; complete a typing and a delay pause; bubble + countdown notification; automatic re-arm; turn off via disable challenge; delete an OFF block via the Are-you-sure dialog. | Any Phase 1–6 behavior changed by the Phase 7 edits (event retirement touched the runtime/repo; variant-value collapse touched the persistence boundary). | All listed behaviors unchanged from the accepted Phase 6 state. |

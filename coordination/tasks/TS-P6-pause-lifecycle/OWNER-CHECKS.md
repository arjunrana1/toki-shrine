# Owner device checks — TS-P6-pause-lifecycle

- **Source:** Arjun's completed checklist ("Toki Shrine | Owner device checklist | Phase 6") plus his accompanying correction message, both 23 September 2026. Agent-recorded from the owner's reports; the agent performed no device execution and invented no results.
- **Build under test:** installed review-attributed `062c71e` build (records-only HEAD `5f31511`; `app-debug.apk` SHA-256 `f637c82e883137e99031ce11712347a0154c254f34aa92b49fd11c263e47346c`, installed 23 September 2026 via `adb -s R5CW30ZBM2R install -r`).
- **Outcome:** no fail verdicts. P6-O1–O8, O10–O12, O14, O16, O18–O20 **Pass**. P6-O9 pass with an improvement note. P6-O13 recorded observation (owner: nothing to fix). P6-O15 conditional pass (owner declined the clock-change steps as a narrow edge case). P6-O17 not applicable as written (owner: stopping a block removes its pause; delete requires disable first, so pause-plus-delete is unreachable through product flow).

## Results by checklist ID

- **P6-O1–O8:** Pass (pause start, per-block temporary access isolation, website pause/re-arm, foreground re-arm at expiry [P6-R1 acceptance], sleep/wake expiry, exactly-once expiry, independent simultaneous pauses, soonest-deadline bubble selection).
- **P6-O9:** Pass. **Improvement note (owner):** needs a way to dismiss the bubble — suggested drag to the bottom screen edge and discard, per common floating-button convention. Owner approved the feature later the same day and it is implemented as P6C-O5 (PRD §6/§7/§10/§17 amended); retest of the dismissal behavior is pending on the corrected build.
- **P6-O10:** Pass (bubble tap opens Toki Shrine; pause neither cancelled nor extended).
- **P6-O11:** Pass (overlay denied: no bubble, pause/enforcement unaffected).
- **P6-O12:** Pass (one notification per pause, correct block and countdown, per-block navigation and removal).
- **P6-O13:** Recorded observation. The ongoing notification can be dismissed, but it returns after a while (owner does not know what triggers the return; owner: "nothing to fix here"). Mechanism identified in source (owner asked for the reason): `PauseService` re-posts pause notifications on every render tick (60 s cadence at test time) plus data-change renders, so any platform-permitted dismissal is undone by the next re-post; the pause itself keeps expiring and re-arming correctly. **Explicit owner instruction: do not attempt to improve this.**
- **P6-O14:** Pass (notification permission denied: pause starts, expires and re-arms; bubble independent).
- **P6-O15:** Conditional pass — owner chose not to run the manual clock-change steps ("very narrow edge case"). Monotonic `elapsedRealtime` remains the only deadline authority in source; JVM clock-change coverage stands, device evidence intentionally absent.
- **P6-O16:** Pass (block OFF during pause silently drops the pause; no auto re-arm of the disabled block).
- **P6-O17:** Not applicable as written (see outcome note above); underlying silent-pause-removal behavior is covered by the passing O16.
- **P6-O18:** Pass (process death: pause not restored, enforcement returns, no stale surfaces).
- **P6-O19:** Pass (no second challenge or extension path for an already-paused block).
- **P6-O20:** Pass (Phase 5 behavior intact: abandonment/"Not now" grants no pause; turn-off path unchanged).

## Owner corrections raised this round (P6C)

- **P6C-O1 — notification progress bar advances only in large steps (owner observed ~10 s; source cadence was a 60 s re-post tick).** Fix: `PauseService` now re-derives and re-posts pause notifications every second (single per-second tick merged with the former 1 s bubble tick; the soonest pause is promoted via `startForeground` instead of a duplicate `notify`). Side effect to know: a dismissed notification is now restored within about a second instead of a minute — that is the P6-O13 re-post mechanism, which the owner directed not to change.
- **P6C-O2 — gate humour photos too dark.** Fix: `BlockGateScreen` scrim `GATE_SCRIM_ALPHA` 0.78 → 0.65 (PRD §11 legibility constraint retained; no contract pins the old value).
- **P6C-O3 — long block name wraps flush against the delete icon on block detail.** Fix: `NocturneAppbar` title gains 12 dp end padding (mirrors the gap after the back control; shared app bar benefits any trailing action).
- **P6C-O4 — cross-block stale challenge precedence (Block B's unfinished challenge reopens for a Block A trigger until completed or dismissed).** Owner verdict: acceptable narrow edge case — document, do **not** fix, revisit only with real user feedback. Root cause (owner asked): `BlockActivity.onNewIntent` deliberately lets one unfinished challenge own the task — a new detection launch while a challenge is ACTIVE/COMMITTING brings the existing session forward instead of replacing it (protects retained typing text and the delay challenge's reset-on-background rule); only completion or explicit dismissal terminalizes it, and app-switch/lock is nonterminal by design. Both this and P6-O13 carry the same do-not-improve instruction.
- **P6C-O5 — bubble drag-to-dismiss (from the P6-O9 improvement note; owner-approved 23 September: "we need to give the ability to dismiss the bubble if the user wants it").** Implemented in `7abb516`: release a drag over the bottom screen-edge strip to discard the pill; the bubble stays hidden while the pauses visible at dismissal remain live, and any new or restarted pause shows it again. Timing, enforcement and notifications unaffected; logs `bubble_dismissed` (new §10 event). PRD §6 screen 20, §7, §10 and the §17 Phase 6 addendum record the decision. Owner retest pending on the corrected build.

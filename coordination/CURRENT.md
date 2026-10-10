# Current work

- **Active task:** none. Phase 7 ([TS-P7-stats-feedback-final](tasks/TS-P7-stats-feedback-final/TASK.md)) was **closed on 11 October 2026** by owner decision. Code `c19b53f`, records `5501509`, pushed to `origin/main`. The CURRENT before closure is preserved at commit `6cc6efe`.
- **Installed build:** debug APK `8a7cbdb9073d73c9eafd97131d7dde3fe75415a856df029001bda845ed6c83fc` (11 October 00:25, USB `R5CW30ZBM2R`). It carries the debug testing values (delta 5). Owner retest PASS ([OWNER-CHECKS](tasks/TS-P7-stats-feedback-final/OWNER-CHECKS.md)).
- **Launch gate (carried from Phase 7):**
  - **P7-D3/D4 database and usage audit:** a read-only pull of the app database plus `dumpsys usagestats`, checked against the code once real usage exists. Also explains the 7 nopes of unknown source from the 10 October retest.
  - **Run the Android tests** (Room repository, migration and UI tests). Since the 4 October P7-D1/D2 run they have only compiled, across schema v5, auto-nope and the two-minute window. Run them on the isolated emulator (AVD `toki-p7d-isolated`) with owner authorization.
  - **Debug testing values (delta 5)** stay until launch. Then decide on removal and confirm production values (P7-O14) as part of the pre-launch config review.
  - **Not blocking, owner's option:** independent review of deltas 11, 12A and 13; the PRD wording "returns to the block" vs the Blocks list for the turn-off escape (retest row 4); Samsung overnight survival (P4-09, accepted deferred risk).
  - Also before launch: the full config-value review (owner, 9 October).
- **Later, not scheduled:** the Time Shrine rename (applicationId `com.arjunrana.timeshrine`; new install, no carried history), release signing (Arjun creates the key; Gradle reads an untracked file; Play App Signing), the Play testing track (new personal account: 12 testers × 14 days; accessibility prominent-disclosure check). [PostHog](../docs/plans/posthog-analytics.md) comes only after a full public release.
- **Retained limits:** P4-09 Samsung overnight survival is an owner-accepted deferred risk. Phase 6 keeps P6C-N1 and the P6C-O4/P6-O13 do-not-fix decisions.
- **Ignored for now (owner):** the untracked `design_handoff_unlock_minutes/`.
- **Next actor:** Arjun. He brings a new batch of improvements to a fresh architect session, which clarifies the whole batch before planning, then records the changes in PRD §17 and a new task before any implementation.

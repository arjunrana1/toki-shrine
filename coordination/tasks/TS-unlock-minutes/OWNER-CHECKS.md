# TS-unlock-minutes — owner checks

Arjun runs these on his phone after UM-14. Record each result with the date and the **installed APK hash**. **Results: all seven PASS (22 October 2026, build `9d505bd8…`)**, see [Owner results](#owner-results--22-october-2026).

## UM-14 install record — 22 October 2026

- **Installed build:** reviewed D3 debug APK `9d505bd8ec2d8654ac798cc3e7438aa50186322494936290c3a5ef8d9bb3325f` (built 11 October 01:54, versionName 0.1.0), at Arjun's explicit request. Run every check below against this hash.
- **Identity verified first:** all 35 entries of [D3-SUBMISSION.sha256](D3-SUBMISSION.sha256) match the working tree, `ui/util/Estimate.kt` is absent, and the APK hash matches D3 and [REVIEW](REVIEW.md). No rebuild.
- **Install:** `adb -s R5CW30ZBM2R install -r` (in place, no uninstall or clear) on Arjun's SM-S918B over USB → `Success`; package `lastUpdateTime` 2026-10-22 23:36:17 IST. Installed by Claude Opus 5.5.
- **Limits:** no app launch, screenshot, database read or other device query beyond the install and the package's version/update time. The v3–v5 data reset happens on first open and is unobserved (UM-O7). Replaces the Phase 7 build `8a7cbdb9…`.

The install is expected to reset the app's data (PRD §17 11 October addendum), so blocks need setting up again first. Debug builds still carry the delta 5 disable 20 rung.

| ID | What to test | Steps | Fail | Pass |
|---|---|---|---|---|
| UM-O1 | Gate layout and motion | Open a blocked app. Tap "I'll do the challenge. Let me in"; tap it again. | Buttons move when the row opens; the meme is covered by the buttons or chips; the row snaps in with no stagger; the background or dimming changed. | Row label and 5/10/15/20 drop in left to right with a slight bounce, and fold away right to left on the second tap. "Nope, not now" is the deeper accent and stays the dominant button. The clear meme sits about 76 dp higher and is fully visible. |
| UM-O2 | Chosen length honoured | For each of 5, 10, 15 and 20: tap the chip, complete the challenge, watch the bubble and notification; let one pause expire. | Wrong starting time; any extra confirm step; no re-arm. | A chip tap highlights the chip for a moment (about 0.2 s) and then goes straight to the challenge. The pause counts down from the chosen length and re-arms at expiry. |
| UM-O3 | Length survives interruptions | Pick 5, leave the challenge for under 15 s, return and finish. Rotate mid-challenge and finish. Pick 10 and tap "Nope, not now" on the challenge. | The pause length changes after leaving or rotating; a pause starts after the nope. | 5-minute and kept pauses as chosen; the nope starts no pause. |
| UM-O4 | Step 3 when creating | New block → step 3. | A card is preselected; Next works before a pick; a sheet or "Adjust the details" appears; the old subtitle or a time estimate shows. | "How do you want to pause the block?", nothing selected, Next dimmed. Tapping a card unfolds one stepper; the other card folds it away. Typing 20…150 in steps of 10 (starts at 20); wait 20…240 s in steps of 10 (starts at 60). Hint line under the cards. |
| UM-O5 | Step 3 when editing | Turn a block off, edit its friction. | The stored method isn't open or its value is lost; the save is rejected. | The stored method is open at its stored value, and saving works. Entering directly from the block's detail, the card may unfold as the screen opens; that is expected. |
| UM-O6 | Copy elsewhere | Check Review (5/5), Block detail and Turn-on confirmation. | Any "Each pause lasts"/"Pause lasts" row or "~Ns" estimate. | No pause-length rows and no time estimates; only character counts and waits. |
| UM-O7 | First open after install | Open the app after installing. | Crash or hang. | It opens (data reset as accepted). Database checks for `pause_started.minutes` and `block_created` wait for the launch-gate P7-D3/D4 audit. |

## Owner results — 22 October 2026

Arjun's report, 22 October 2026, on installed build `9d505bd8ec2d8654ac798cc3e7438aa50186322494936290c3a5ef8d9bb3325f`: "All the cases pass and everything works as expected."

| ID | Result |
|---|---|
| UM-O1 | PASS |
| UM-O2 | PASS |
| UM-O3 | PASS |
| UM-O4 | PASS |
| UM-O5 | PASS |
| UM-O6 | PASS |
| UM-O7 | PASS (opened without a crash; data reset as accepted) |

Recorded as one summary report; no per-check detail was given. Still not verified by these checks: the database assertions for `pause_started.minutes` and `block_created` (launch-gate P7-D3/D4 audit) and execution of the Android tests (UM-RN1, launch gate).

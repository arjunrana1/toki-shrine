> **Read with [PRD §17 "Unlock minutes addendum — 11 October 2026"](../../../PRD.md), which wins where this handoff differs.** Owner decisions of 11 October: the main gate button stays "Nope, not now"; the central meme moves up with the buttons; build 09 v3 (09b v2 is not built); step 3 is titled "How do you want to pause the block?" with no subtitle; the typing sub-line is "Random words." with no estimate; pause typing is 20–150 chars, step 10, default 20; wait is 20–240 s, step 10, default 60; a chip tap goes straight into the challenge; the review and detail pause rows are removed entirely. Task: [TS-unlock-minutes](../../../coordination/tasks/TS-unlock-minutes/TASK.md).

# Handoff: Pause-length moves to unlock time (14 v2 · 09b v2 · 09 v3)

## The decision (read first)
Until now, each block stored a fixed **"Block stays off for"** duration, which the user set once during creation on screen 09/09b. Every unlock lasted that same time.

**New model:** the user chooses how long a pause lasts **at the moment of unlocking**, on the block screen. Block creation therefore no longer asks for pause length. Creation also gets simpler: you pick the friction type and tune it inline, with no separate "Adjust the details" sheet.

Why:
- The user knows in the moment whether they need a 5-minute check or 20 minutes of real use.
- Naming a duration is a small commitment, so they're more likely to respect it.
- Creation has one fewer abstract decision.

## About the design files
`TimeShrine Mocks.dc.html` is a **high-fidelity HTML design reference, not production code**. Recreate it in the app's own stack.
- Open it in a browser and scroll to **"Proposed changes"** at the bottom.
- Three screens there are interactive: 14 v2, 09b v2 and 09 v3.
- The screens above that section are the current prototype and are unchanged.
- Visual tokens come from the Nocturne design system (`_ds/.../styles.css`). Map them to the codebase's equivalents.

---

## 1. Screen 14 v2 — Block screen with "Minutes to unlock"
**Replaces:** screen 14.

**Experience**
- The user opens a blocked app and sees "Open Instagram?" with two buttons.
- "Not now" stays the dominant, filled default.
- Tapping "I'll do the challenge. Let me in" drops a "Minutes to unlock" row out from under that button: **5 / 10 / 15 / 20**. The chips fall in one after another with a slight bounce.
- Picking a chip sets this pause's length. In production, the user then goes to the configured challenge, and the pause starts once it's completed.
- Tapping the challenge button again folds the row back up and clears the pick.

**Changes from 14**
- "Not now" background is now `--color-accent-400`, one step deeper. The previous purple read oddly.
- Both buttons sit about 76px higher (bottom padding 100px) to make room for the row.
- About 36px of space separates the chips from the bottom edge.

**Specs**
- Ground `#08090f`. Centered column, padding `24px 28px 100px`.
- Kicker: "The scroll pit", 12px, uppercase, letter-spacing .1em, neutral-600.
- Title: "Open Instagram?", 26px.
- **Not now:** 58px tall, 17.5px/600, accent-400 fill, text `#0b0b0f`, shadow `0 6px 22px` accent at 40%.
- **Challenge button:** 50px tall, 14.5px/500, 12px below "Not now". Background `rgba(12,12,16,.72)`, 1.5px border `rgba(255,255,255,.5)`, blur(6px).
- **Dropdown:** absolutely positioned under the challenge button (top 100%, 8px gap, 6px internal gap). The buttons never shift when it opens.
  - Label: "Minutes to unlock", 11px, uppercase, letter-spacing .08em, neutral-400.
  - Chips: a 4-up row with 8px gaps. Each chip is 36px tall, radius 10px, 15px/500, tabular numbers.
    - Unselected: background `rgba(12,12,16,.72)`, 1px border `rgba(255,255,255,.3)`, text neutral-100.
    - Selected: background accent-900, 1.5px border accent-400, text accent-100.

**Animation**
- Label: opacity 0→1 over .25s ease; translateY −10px→0 over .35s `cubic-bezier(.2,.8,.2,1)`.
- Chips: opacity 0→1 over .26s ease; translateY −18px→0 over .42s `cubic-bezier(.2,.9,.25,1.15)`. The overshoot gives the drop.
- Stagger: opening delays are 80/140/200/260ms (left to right). Closing runs in reverse (right to left) at 90/60/30/0ms.
- Selection: background and border change over .15s.
- While closed, the row doesn't respond to taps (pointer-events: none).

---

## 2. Screen 09b v2 — Adjust details (interim step)
**Replaces:** 09b. Superseded by 09 v3 below. It's kept so the step-by-step reasoning is visible.
- The "Block stays off for" stepper is removed.
- New hint line: Phosphor `clock-countdown` icon plus "You'll pick how long each pause lasts when you unlock." (11.5px, neutral-500).

---

## 3. Screen 09 v3 — Pick friction, tune inline ✅ recommended
**Replaces:** both 09 (friction choice) and 09b (the "Adjust the details" sheet).

**Experience**
- Step 3/5, "What should getting in cost?", shows two cards: **Type a passage** and **Wait it out**.
- There's no "Adjust the details" button and no sheet.
- Tapping a card selects it: accent border, tinted fill, check mark. Its single control unfolds beneath it, already at the default.
- The user can tweak the value or just tap **Next**.
- Tapping the other card folds the first one away and opens the second. Only one card is open at a time.
- **Next** is dimmed (45%) until a card is picked.
- The pause-length hint line sits under the cards.

**Controls**
- **Type a passage:** "Passage length" stepper.
  - Default 150 characters, step 50, range 50–500.
  - Sub-line: "Random words · about {chars÷4} sec" (shown in minutes at 60s and up).
- **Wait it out:** "Wait before you're in" stepper.
  - Default 60 sec, step 15, range 15–300.
  - Sub-line: "A countdown you can't skip."
- Steppers use the existing `.stepper` style: surface background, 1px divider border, radius 10px, square ±30px buttons.

**Card states**
- Unselected: 1px divider border, surface background, neutral-400 icons, `ph-circle` mark.
- Selected: 1.5px accent border, accent-900 background, accent-200 icons, `ph-fill ph-check-circle` mark.
- Radius 14px. The header row has 16px padding.

**Animation**
- Expand and collapse: a grid-rows 0fr→1fr transition over .38s `cubic-bezier(.2,.8,.2,1)`. Use height auto-animation or the platform equivalent.
- Inner content: opacity 0→1 over .25s; translateY −12px→0 over .38s `cubic-bezier(.2,.9,.25,1.15)`, delayed 80ms.
- Card colors change over .2s.

---

## State and data model
- **Block:** `friction: 'type' | 'wait'`, `passageChars: number` (default 150), `waitSecs: number` (default 60).
- The **`pauseMinutes` field is removed from Block.**
- **Unlock flow:** `dropOpen: boolean`, `unlockMinutes: 5|10|15|20|null`. The unlock passes this value to the pause timer.

## Still to reconcile (copy elsewhere in the prototype)
These spots still mention a fixed 15-minute pause. Remove or reword them:
- 09 card body: "Then 15 minutes before the block comes back."
- Screen 10 review: "Each pause lasts".
- Screen 13: "Pause lasts".

## Files
- `TimeShrine Mocks.dc.html` — the full prototype plus the "Proposed changes" section. Open it in a browser.
- `support.js` — the runtime that the HTML file needs.
- `_ds/` — Nocturne tokens and Phosphor icon setup.

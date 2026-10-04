# Handoff: Toki Shrine — "Your time" stats screen (Screen 21)

## Overview
This replaces the old walk-away stats screen with a time-focused dashboard. It shows how much time Toki saved the user on blocked apps over the last 7 days, plus supporting metrics. Android only (v1).

**Read `PRODUCT_NOTES.md` first.** It is the source of truth for data sources, metric definitions, formulas, states, edge cases and analytics. This README covers the visual and UI spec.

## About the design files
The files in this bundle are **design references built in HTML**. They show the intended look and behavior; they are not production code to copy. Recreate them in the app's existing Android environment (Jetpack Compose / Views, whichever the codebase uses), following its established patterns and components.

`Stats Hi-fi.dc.html` opens directly in a browser (keep `support.js` and `_ds/` next to it). All data in it is static example data.

## Fidelity
**High-fidelity.** Final colors, type, spacing and copy. Match them closely using the app's existing theme. The color tokens below map to the app's Nocturne theme.

## Screens

The screen is designed at **330 dp wide content inside a 340 dp frame**. Horizontal screen padding is 20 dp. Dark theme only.

### S1 — Default (scrolling screen)
Vertical stack with **24 dp gaps** between sections. The whole screen scrolls.

1. **App bar.** Row, 12 dp gap.
   - Back arrow icon: 20 dp, `neutral-300`.
   - Title "Your time": 15 sp / 500, `text`, fills the row.
   - "Weekly info": 12 sp, `neutral-500`. Plain text, not tappable.
2. **Hero.** Column, 6 dp gap.
   - Label row: "Time saved per day" (12.5 sp, `neutral-400`), then a (?) info icon (15 dp, `neutral-500`, 6 dp gap). Tapping the (?) opens S2.
   - Value row (baseline-aligned, 8 dp gap):
     - "48m": 68 sp, weight 300, line-height 1.0, letter-spacing −0.03em, `accent-200`, tabular numbers.
     - "/ day": 15 sp, `neutral-500`.
   - "5h 36m saved so far": 14 sp, `neutral-200`.
   - "Screen time 3h 40m/day · ↓ 22% vs your usual": 12 sp, `neutral-500`, 2 dp top margin. Same color whether screen time is up (↑) or down (↓).
3. **Tiles.** 2 equal columns, 10 dp gap.
   - Each tile: `surface` background, radius 12, padding 14, column with 4 dp gap.
   - Tile 1: "12" (24 sp / 500), then "Attempts / day" (11.5 sp, `neutral-500`).
   - Tile 2: "62%" (24 sp / 500), then "Nope rate" + (?) icon (13 dp; opens S3), then a progress bar.
     - Bar track: 3 dp tall, radius 2, `neutral-800`, 4 dp top margin.
     - Bar fill: width = nope rate, `accent`.
4. **"Time saved on blocked apps" list.**
   - Header: 11 sp, uppercase, letter-spacing 0.08em, `neutral-500`, followed by a (?) icon (14 dp; opens S2, the same sheet as the hero).
   - List container: max height 300 dp (about 5 rows), scrolls internally. A 34 dp gradient at the bottom fades from transparent to `bg`.
   - Row: padding 11 dp top and bottom, 1 dp bottom divider (`divider`). **No app icons.**
     - Left column (2 dp gap):
       - App name: 14 sp / 500.
       - Sub-line "{n} nopes this week · ~{x}m per visit": 11.5 sp, `neutral-500`.
     - Right: time saved, 14 sp, `accent-200`, tabular numbers.
   - Rows are not tappable.
5. **Chart card: "Time saved".**
   - Card: `surface` background, radius 14, padding 16/14/12, column with 12 dp gap.
   - Header row: "Time saved" (13 sp / 500) on the left; 7-day total on the right (12 sp, `accent-200`).
   - Plot area: 108 dp tall, 7 columns, 7 dp gap, bars bottom-aligned.
     - Each column: the value label (10 sp, no wrapping), 5 dp gap, then the bar.
     - Bar height: `value ÷ max × 74 dp`, minimum 4 dp. Zero values show a 3 dp stub.
     - Bar radius: 4.
   - Bar colors:
     - Past days: `accent-700`.
     - Today: `accent`, with a glow (shadow 0 0 14dp, `accent` at 45% opacity).
     - Zero values: `neutral-800`.
   - Value label color: `accent-200` for today, `neutral-400` for other days.
   - Day labels: below a 1 dp top divider with 8 dp top padding. Labels are "M T W T F S Today". 10 sp; `neutral-100` for today, `neutral-500` for other days.
   - Not interactive.
6. **Chart card: "Time spent after the challenge".**
   - Identical to the time-saved chart, except:
     - Header total: `neutral-400`.
     - Bars: `neutral-700`; today `neutral-400`; zero values `neutral-800`.
     - Value labels: `neutral-400`.
     - No glow.
   - Has its own scale.
7. **Recalibrate card.**
   - Card: 1 dp `neutral-800` border, radius 14, padding 16, column with 10 dp gap. No icon.
   - Title "Recalibrate time spent on apps": 14 sp / 500.
   - Body "Usual time per app seems off? Hit recalibrate and we'll re-measure it from your last 7 days.": 12.5 sp, line-height 1.55, `neutral-400`.
   - Secondary (outlined) button "Recalibrate": full width, 40 dp tall.

### S2 — "Time saved" info bottom sheet
- **Scrim:** `#08090F` at 70% opacity.
- **Sheet:**
  - Background `surface`, top corners radius 22, padding 10/22/28, column with 16 dp gap.
  - Shadow: `shadow-lg`.
  - Drag handle: 36×4 dp, `neutral-700`, centered.
- **Content, top to bottom:**
  - Title "Time saved": 18 sp / 500.
  - "Every time you nope out, you skip a visit. We count the time that visit would usually have taken.": 13.5 sp / 1.55, `neutral-300`.
  - **Formula row:** background `neutral-900`, radius 12, padding 12, centered, wrapping, 6 dp gap, 12 sp.
    - Chips (no wrapping, padding 4×8, radius 7): "Nopes" and "Usual visit length" with a 1 dp `neutral-700` border; "Time saved" with a 1 dp `accent` border and `accent-200` text.
    - "×" and "=" between chips in `neutral-500`.
  - "How we get "usual visit length"": 13 sp / 500. Below it: "We look at your last 7 days before you installed Toki. Total time in an app ÷ number of times you opened it. In the absence of data, we assume 10 mins per visit." 12.5 sp / 1.55, `neutral-400`.
  - "Tried again within 5 minutes? That counts as one nope.": 12 sp, `neutral-500`.
  - Secondary button "Got it": full width, 42 dp tall.
- **Dismiss:** "Got it", tapping the scrim, or swiping down.
- Opened from both the hero (?) and the list (?).

### S3 — "Nope rate" info bottom sheet
Same sheet shell as S2.
- Title "Nope rate".
- Body "How often you walked away instead of pushing through the challenge."
- **Split bar block:** background `neutral-900`, radius 12, padding 14.
  - Bar: 8 dp tall, radius 4, 2 dp gap between segments. Nopes segment in `accent`; the rest in `neutral-700`.
  - Labels below: "52 nopes" (`accent-200`) on the left, "32 pushed through" (`neutral-400`) on the right, 12 sp.
- "52 of 84 tries this week = 62%. Higher is better.": 13 sp, `neutral-300`. The percentage is in `accent-200`.
- "Got it" button.

### S4 — Partial or tough week
Same layout as S1, no changes. Days with no data show "0m" and a stub bar. Screen time up shows "↑ 9%" in the same neutral color. No warning styling anywhere.

### S5 — No Usage Access
- App bar: back arrow and title only.
- **Card:**
  - Background `surface`, radius 16, padding 20, inset 1 dp `neutral-800` border, column with 12 dp gap.
  - Title "See how much time you're saving": 18 sp / 500, line-height 1.3.
  - Body "Allow usage access so we can see how long you usually spend in each app. We only read app time, never what you do inside them.": 13 sp / 1.55, `neutral-400`.
  - Primary (outlined accent) button "Allow usage access": full width, 44 dp tall. Opens `Settings.ACTION_USAGE_ACCESS_SETTINGS`.
- No other data is shown.

### S6 — No blocks
- App bar: back arrow and title only.
- Text block, vertically centered, column with 10 dp gap:
  - "Nothing to brag about… yet.": 24 sp / 500, line-height 1.25.
  - "Create a block to see how much time you save.": 14 sp / 1.55, `neutral-400`.
- No icons and no CTA.

## Interactions & behavior
- **(?) icons:** open bottom sheets. Standard Material bottom-sheet enter and exit, with a 200 ms scrim fade.
- **App list:** scrolls independently inside the page scroll (nested scroll). Show the bottom fade only when the list overflows.
- **Not interactive:** chart bars and app rows.
- **Recalibrate:** see PRODUCT_NOTES §4.7. Whether it needs a confirmation step is an open question.
- **States:**
  - No Usage Access: S5.
  - No active blocks: S6.
  - Otherwise: S1 (S4 is the same UI with different data).
- **Loading:** data is computed locally, so no spinners are expected. If computation is async, show the S1 layout with values blank, not a spinner.

## State / data
Expose a single UI model to the screen:
```
StatsUiState =
  | NoUsageAccess
  | NoBlocks
  | Ready(
      savedPerDayMin, savedTotalMin,
      screenTimePerDayMin, screenTimeDeltaPct,   // vs frozen baseline
      attemptsPerDay, nopeRatePct, nopes, attempts,
      apps: [{ name, nopesThisWeek, usualVisitMin, savedMin }],  // sorted desc by savedMin
      savedByDay: [7 × minutes], spentByDay: [7 × minutes]       // oldest → today
    )
```
All formulas, the 5-minute deduplication rule, the baseline capture and the 10-minute fallback are in PRODUCT_NOTES §3–4.

## Number formatting
- Under 100 min: "48m", "61m".
- 100 min or more: "2h 12m" (drop "0m": "2h").
- Per-visit length: "~6m", rounded, minimum 1m.
- Sum raw seconds and round only at display.

## Design tokens (Nocturne theme)
| Token | Hex |
|---|---|
| bg | #161826 |
| surface | #232532 |
| text | #E9E9ED |
| divider | #E9E9ED @ 16% |
| accent | #9184D9 |
| accent-200 | #E7E5FE |
| accent-300 | #D2CEFD |
| accent-700 | #5D5294 |
| accent-900 | #2B2741 |
| neutral-100 | #F3F5FE |
| neutral-200 | #E4E7F5 |
| neutral-300 | #CFD3E5 |
| neutral-400 | #B2B6CA |
| neutral-500 | #9397AB |
| neutral-600 | #75798C |
| neutral-700 | #595D6C |
| neutral-800 | #3F424D |
| neutral-900 | #292B31 |
| shadow-lg | 0 0 0 1px #9397AB, 0 16px 40px rgba(0,0,0,.65) |

- **Typography:** Inter. Headings never go above weight 500. Numbers use tabular figures.
- **Radii:** 4 (bars), 7 (chips), 12 (tiles, formula block), 14 (chart and recalibrate cards), 16 (permission card), 22 (sheet top corners).
- **Buttons:** follow the Nocturne rule that primary buttons are an accent outline, never filled. Secondary buttons are a neutral outline.

## Assets
None. The only icons are the back arrow and the (?) info icon, both from Phosphor (`arrow-left`, `question`). Use the app's equivalents.

## Files
- `PRODUCT_NOTES.md`: product requirements (logic, data, states, edge cases, analytics).
- `Stats Hi-fi.dc.html`: hi-fi design reference for S1–S6. Open it in a browser.
- `support.js`, `_ds/…`: runtime and theme files needed to open the HTML reference.

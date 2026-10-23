# Toki Shrine — "Your time" stats screen (Screen 21)

Platform: Android only for v1. Designs: `Stats Hi-fi.dc.html` (S1–S6).

## 1. Goal
Show users how much time Toki saves them on blocked apps. This replaces the old walk-away count screen.

## 2. Permissions & data sources
| Data | Source | Notes |
|---|---|---|
| App foreground time (daily) | `UsageStatsManager` (Usage Access / `PACKAGE_USAGE_STATS`) | Used for the baseline and for screen time |
| App open counts | `UsageEvents` (ACTIVITY_RESUMED / MOVE_TO_FOREGROUND) | Used for per-visit length |
| Attempts, nopes, challenge passes | Toki's own event log (Accessibility + overlay) | Works without Usage Access |

- Sites are out of scope. Browsers (Chrome, Samsung Internet, Firefox) count as regular apps.
- Usage Access is a new permission. Add it to the onboarding checklist (Screen 02) under "Essential" and to Settings.

## 3. Definitions
**Attempt.** The user opens a blocked app, so the Toki challenge appears.

**Nope.** An attempt where the user does not get through. Either:
- they tap "Skip for now", or
- they abandon the challenge (leave the app, go Home, or lock the screen) before completing it.

**Push-through.** An attempt where the user completes the challenge and enters the app.

**Deduplication (5-minute rule).** Attempts on the same app within 5 minutes of the previous nope count as one nope (and one attempt). The window resets after each counted nope.

**Baseline.** Captured once, at the moment Usage Access is first granted. Frozen until the user recalibrates.
- Window: the 7 days before install (use whatever is available, up to 7 days).
- Per app:
  - `usual_visit_length = total foreground time in window ÷ number of opens in window`
  - `usual_daily_time = total foreground time in window ÷ days of data`
- Overall: `usual_screen_time = total foreground time (all apps) ÷ days of data`
- **No data fallback:** new or reset phone, the app has no opens in the window, or opens < 3. Then `usual_visit_length = 10 min`.
- Store the per-app baseline when an app is added to a block later. Compute it at that moment from the same pre-install window if it's still available; otherwise use the 10-min default.

**Time saved (per app, per day).** `deduped nopes × usual_visit_length`

**Time spent after the challenge (per app, per day).** Actual foreground time in the blocked app that follows a push-through, until the app leaves the foreground.

## 4. Screen structure (S1)
Data window: rolling last 7 days including today ("Weekly info"). Days before install count as 0.

1. **App bar.** Back arrow, title "Your time", static label "Weekly info" (not tappable).
2. **Hero.**
   - "Time saved per day" + ⓘ (opens S2).
   - Big number = `sum(time saved, all blocked apps, 7 days) ÷ 7`.
   - Sub-line: "{total} saved so far" = the 7-day total.
   - Context line: "Screen time {avg}/day · ↓/↑ {x}% vs your usual".
     - `avg` = 7-day average of all-app foreground time.
     - `x = (avg − usual_screen_time) ÷ usual_screen_time`, rounded to the nearest whole %.
     - Neutral color both ways. Hide the % if the change is under 1%.
3. **Tiles.**
   - Attempts / day = `deduped attempts (7 days) ÷ 7`, rounded to a whole number.
   - Nope rate = `nopes ÷ attempts`, whole %, + ⓘ (opens S3). Show "—" if attempts = 0.
4. **Time saved on blocked apps** + ⓘ (opens S2).
   - One row per app in any active block. Sorted by time saved, descending; ties broken alphabetically.
   - Row (no app icons): name, sub-line "{n} nopes this week · ~{usual_visit_length} per visit", time saved (right).
   - About 5 rows visible. The list scrolls inside its container, with a bottom fade when more rows exist.
   - Rows are not tappable.
   - Apps with 0 nopes still appear (showing 0m).
5. **Chart: Time saved.** 7 bars (M–S, last bar labelled "Today"). The value is printed above each bar, and the header shows the 7-day total. Today is highlighted in the accent. Days with no data show "0m" and a stub bar. Bars are not interactive.
6. **Chart: Time spent after the challenge.** Same format, neutral colors, with its own scale.
7. **Recalibrate.**
   - Copy: "Usual time per app seems off? Hit recalibrate and we'll re-measure it from your last 7 days."
   - Recalibrate recomputes the baseline from the most recent 7 days of usage and replaces the old one. Historical "time saved" is not recomputed; new values apply from that point.

## 5. Number formatting
- Under 100 min: "48m", "61m". 100 min or more: "2h 12m" (drop "0m": "2h"). Chart bar labels use the same rule so they fit the bar width.
- Per-visit length: "~6m". Round to the nearest minute, minimum 1m.
- Durations are rounded at display time only. Sum the raw seconds.

## 5a. Visual notes
- No decorative icons anywhere on the screen. The only icons are the back arrow and the (?) info buttons.
- Accent color is used only for the time-saved values, the saved chart bars (today highlighted), and the nope-rate bar.
- Spent chart uses the neutral greys.

## 6. States
| State | Trigger | Behavior |
|---|---|---|
| S1 Default | Access granted, at least 1 block | Full screen as above |
| S4 Partial / tough week | Fewer than 7 days of data, or screen time up | Same layout. Missing days = 0m. The average still divides by 7. No warning tone. |
| S5 No Usage Access | Permission missing or revoked | Show only the prompt card. The CTA opens `Settings.ACTION_USAGE_ACCESS_SETTINGS`. On return with access granted: compute the baseline, then show S1. |
| S6 No blocks | No active blocks (also covers blocks with no attempts ever) | Text only: "Nothing to brag about… yet." / "Create a block to see how much time you save." |
| S2 / S3 | ⓘ tapped | Bottom sheet; dismiss by "Got it", swipe down, or tapping the scrim |

## 7. Explainer copy
**S2 Time saved.**
- "Every time you nope out, you skip a visit. We count the time that visit would usually have taken."
- Formula: Nopes × Usual visit length = Time saved.
- "We look at your last 7 days before you installed Toki. Total time in an app ÷ number of times you opened it. In the absence of data, we assume 10 mins per visit."
- "Tried again within 5 minutes? That counts as one nope."

**S3 Nope rate.**
- "How often you walked away instead of pushing through the challenge."
- Split bar + "{nopes} of {attempts} tries this week = {x}%. Higher is better."

## 8. Edge cases
- **App removed from all blocks:** remove it from the list. Its past saved time still counts in the hero and the saved chart for the days it was blocked.
- **App uninstalled:** same as removed.
- **Usage Access revoked later:** go to S5. Keep the stored baseline and event history; resume when access is granted again.
- **Clock or timezone change:** bucket days by the device's local date at event time.
- **OEM battery killers** (Xiaomi, Huawei, Oppo): attempts can be missed if the service is killed. This is covered by the battery-exemption permission.
- **Work profile apps:** out of scope for v1.

## 9. Analytics
- `stats_viewed` (state: default / partial / no_access / no_blocks)
- `stats_info_opened` (which: time_saved_hero / time_saved_list / nope_rate)
- `stats_usage_access_cta_tapped`, `usage_access_granted`
- `stats_recalibrate_tapped`, `baseline_recalibrated` (old vs new usual_screen_time)

## 10. Out of scope / v2
- iOS. The Screen Time API doesn't expose raw per-app durations to app logic; it needs a feasibility spike.
- Per-site (URL) time.
- Tappable chart bars or per-day drill-down.
- Longer ranges (30 days, all time).

## 11. Open questions
1. Should Recalibrate ask for confirmation first ("This replaces your usual times")?
2. What's the minimum number of opens before we trust measured visit length over the 10-min default? Proposed: 3.
3. If an app was opened in the baseline window but has very short visits (under 1 min), should we floor visit length at 1 min?

# Toki Shrine — PRD 1.0

**Current design authority:** §17 records the owner’s refinements and later addenda. For those items it supersedes older screen descriptions, mockup copy/layout and historical decisions. Unchanged behavior and design tokens retain their existing authority. Dated checkpoints preserve evidence at that time; [CURRENT](coordination/CURRENT.md) identifies live task and acceptance status.

Product and build specification. Supersedes `PRD-phase-0.md` and `DESIGN-BRIEF-phase-1.md`, which are retained as history.

Android only. Phone, portrait, dark theme only.

---

## 1. What it is

An Android app that puts a deliberate, self-chosen pause in front of the apps and websites a person doomscrolls.

It never blocks anyone out. The user picks what to slow down, picks what it should cost to get in, and then pays that cost — either typing out a passage of random words or waiting with the Toki Shrine waiting screen visible and the device unlocked. If they still want in afterwards, that is a fine answer.

## 2. Positioning and principles

Anti-brainrot, not productivity. The subject is endless scrolling and endless watching — not time management, not focus sessions, not parental control.

**P1 — This app never hard-blocks.** The user can always get in. The job is to make that a conscious decision rather than an unconscious one. A pure hard-block product is out of scope and would be a separate app.

**P2 — Backing out is the easier path.** On every interruption screen the walk-away is the visually prominent action and the way in is the quieter one. Getting in is offered plainly, never hidden.

**P3 — Never shame.** Nothing in the product tells the user they have failed, disappointed anyone, or broken a promise. Dry humour is welcome; guilt is not. This principle governs all copy, including the humour pool in §11.

## 3. Scope

**In**

Blocks (create, edit, delete, on/off) · Android app blocking · whole-domain website blocking in nine browsers · two friction mechanisms (typing, delay) · visible-and-unlocked countdown · timed pause with automatic re-arm · turn-off via the chosen method · floating bubble · ongoing notification · walk-away counter · estimated time saved and app usage Stats · permission onboarding · feedback by email.

**Out**

Uninstall protection · block schedules · escalating difficulty · extending a pause · blocking surfaces inside an app (Shorts, Reels) · URL/path-level blocking · in-app browser (webview) coverage · shareable achievements · NFC · monetisation · any backend · light theme · iOS.

## 4. Vocabulary and lifecycle

| Term | Meaning |
|---|---|
| **Block** | A named container of apps and websites with its own friction settings |
| **ON** | The block is enforcing |
| **OFF** | The block exists but is not enforcing |
| **Pause** | Temporarily open after the user passed the friction challenge |
| **Re-arm** | The pause expires and the block returns to ON, automatically |
| **Friction mechanism** | The challenge required to pause a block |

Two distinct gates: **pausing** costs the block's chosen friction challenge; **turning off** uses the same method, with a separately selected fixed difficulty (§7).

### Rules

- An app or website belongs to **exactly one block**. Apps owned by another block remain visible in the app picker with the exact label **Already added to a block**, without the owning block name, and cannot be selected. This applies whether the other block is ON or OFF. There is no app conflict dialog or app move action. Apps already in the block currently being edited remain editable under the OFF-before-edit rule. The same no-transfer rule applies to websites: when an entered website belongs to another block, show **Already added to a block** beside the entry, without naming the block, and prevent adding it. No conflict dialog or move action is used for either target type.
- **Toki Shrine cannot be added to a block.** It is filtered out of the picker.
- Blocks are independent. Any number may be ON, paused, or OFF simultaneously.
- A block **must be OFF before it can be edited or deleted**. Otherwise removing an app from a block would be a free bypass.
- A new block is saved **OFF** and does nothing until explicitly turned on.
- Turning a block OFF leaves it off until the user turns it back on. It never re-arms itself.
- Pauses cannot be extended. When the timer expires the block re-arms immediately, with no prior warning — the notification stays visible throughout, and so does the bubble unless the user dismissed it (§6 screen 20, Phase 6 owner addendum).

## 5. Core flows

**Onboarding** → Welcome → permission checklist (4 items) → per-permission explainers as needed → block list.

**Create a block** → add apps and sites → name it → choose friction (optional details sheet) → choose disable difficulty → review and save → lands in the list, OFF.

**Turn a block on** → confirmation stating exactly what pausing and turning off will cost → ON. If permissions are missing, this is where the app gates and points at the checklist. The user is never gated before this point.

**Hit a block** → block screen → *Not now* (walk away) or *I'll do the challenge* → challenge → pass → pause begins → bubble and notification count down → automatic re-arm.

**Turn a block off** → complete the selected typing or waiting disable challenge → OFF until manually re-enabled. Challenge execution is Phase 5 work.

## 6. Screens

27 reference images in `design/screens/`, rendered from the Claude Design export. Screens 07 and 10 have two variants each because they carry a two-way toggle.

| # | Screen | File |
|---|---|---|
| 1 | Welcome | `01-welcome.png` |
| 2 | Permission checklist | `02-permission-checklist.png` |
| 3 | Accessibility explainer | `03-accessibility-explainer.png` |
| 4 | Battery settings | `04-battery-settings.png` |
| 5 | Block list — empty | `05-block-list-empty.png` |
| 6 | Block list — populated | `06-block-list-populated.png` |
| 7 | Create · add apps & sites | `07-create-add-apps-sites-apps.png`, `…-sites.png` |
| 8 | Create · app search | `08-create-app-search.png` |
| 9 | Create · name it | `09-create-name-it.png` |
| 10 | Create · configure friction | `10-create-configure-friction-type.png`, `…-wait.png` |
| 11 | Create · review & save | `11-create-review-save.png` |
| 12 | Historical conflict dialog (removed) | `12-conflict-dialog.png` |
| 13 | Turn-on confirmation | `13-turn-on-confirmation.png` |
| 14 | Block detail & edit | `14-block-detail-edit.png` |
| 15 | Block screen | `15-block-screen.png` |
| 16 | Walk-away moment | `16-walk-away-moment.png` |
| 17 | Typing challenge | `17-typing-challenge.png` |
| 18 | Typing — mistyped | `18-typing-mistyped.png` |
| 19 | Delay countdown | `19-delay-countdown.png` |
| 20 | Floating bubble | `20-floating-bubble.png` |
| 21 | Ongoing notification | `21-ongoing-notification.png` |
| 22 | Turn-off typing | `22-turn-off-typing.png` |
| 23 | Stats | `23-stats.png` |
| 24 | Settings | `24-settings.png` |
| 25 | Feedback | `25-feedback.png` |

### Notes per screen

**1 Welcome.** Product name, one-line description. Single CTA *Get started*. The "How it works" secondary CTA visible in the mock is **removed**.

**2 Permission checklist.** Five rows, two states each — pending and granted. Progress indicator ("1 of 5"). No other states. Persistent and resumable; the same component is reused on the Settings permission-health row.

**3 Accessibility explainer.** Shown before Android's own permission screen. States plainly what the app does and does not do. Content is load-bearing — if this screen fails, users abandon here.

**4 Battery settings.** Instructions vary by manufacturer, detected from `Build.MANUFACTURER`. Samsung is the authored default. A quiet "Not a Samsung? Pick your phone" link lets the user override detection. The universal Android battery-optimisation dialog is the baseline path on every device.

**5–6 Block list.** The app's home screen. Header carries two icon buttons, top right: **Stats** (bar chart) and **Settings** (gear). A floating action button creates a block. A *Share feedback* button sits at the bottom. Each row shows the block name, a summary line (contents · friction), and an on/off toggle.

**7 Create · add apps & sites.** Segmented Apps / Websites. Apps opens a searchable installed-app list; websites take a typed full domain. Selected items are listed below with a running count.

**8 Create · app search.** Live filtering with a match count and an *Added* state per row.

**9 Create · name it.** Free text. The suggested-name chips in the mock are **removed**.

**10 Create · configure friction.** Two method cards and an optional details bottom sheet, followed by a separate disable-difficulty screen. See §7 and the 19 September wizard amendment in §17; older configuration mockups are superseded.

**11 Create · review & save.** Reads back the deal: contents, cost to pause, cost to turn off, pause duration. Saves switched off, without the removed explanatory sentence (§17).

**12 Conflict dialog — superseded (owner decision, 9 September 2026).** Do not implement this screen for app selection. Show **Already added to a block** on an unselectable app row instead, without naming the other block. The old render is historical. Websites use the same inline unavailable message and cannot be added while owned by another block.

**13 Turn-on confirmation.** The commitment moment. States both costs using the replacement subtitle in §17; the old closing sentence is removed.

**14 Block detail & edit.** Shows on/off state, contents, and friction summary. Editing is only reachable while OFF, and the screen says so.

**15 Block screen.** Appears over the offending app. Block name, one randomly selected target-aware headline from §11, then *Not now* as the prominent filled action and *I'll do the challenge. Let me in 🚩🤨* as the quieter outlined one. Both actions have equal dimensions. Installed apps use their user-visible label, never a raw package identifier; sites use their domain. Carries the independently selected humour line and two-layer background treatment from §11. Must be comprehensible in under two seconds — no scrolling.

**16 Walk-away moment.** Confirmation, the running count ("That's the 4th time today 🎉"), then **auto-dismiss after 10 seconds** to the Android home screen. Tapping anywhere continues to the Android home screen immediately.

**17–18 Typing challenge.** Passage displayed above, input below, live character count and an explicit *Submit* CTA. Submit is disabled until the configured character count is reached and is the only submit path; keyboard Enter/Done does not submit. Typo positions appear only after a failed Submit, with the typed text kept for correction. Escape action: *Never mind*.

**19 Delay countdown.** Timer, *Stay on this screen*, the direct reset explanation from the owner reference, and a full-width outlined *Never mind*. The old Counting pill is absent.

**20 Floating bubble.** Draggable, shows remaining time and what is open. Tap returns to the app. The user may also dismiss it: dragging it to the bottom screen edge and releasing discards it; the bubble stays hidden while the pauses visible at dismissal remain, and any new pause shows it again. Requires overlay permission; absent it, the bubble is simply not shown and everything else still works.

**21 Ongoing notification.** Non-dismissable for the duration of the pause, with a system countdown. Tapping opens the block's detail screen.

**22 Turn-off challenge.** Inherits the block’s typing or waiting method with its selected disable difficulty. Escape action: *Never Mind* — on the typing variant a full-width outlined button under Submit, on the waiting variant the existing outlined bottom button (owner correction, 23 September 2026, superseding the former header *Leave it on* and the "Turning off ·" header eyebrow). The typing-only mock is incomplete for the waiting variant.

**23 Stats.** See §9.

**24 Settings.** Permission health · supported browsers · theme (static, Dark) · send feedback · about and version. The "Countdown messages — updated remotely" row is **removed**.

**25 Feedback.** Free-text field and *Send it*. Opens an email intent (§13). The "Attach a diagnostic log" toggle and its supporting copy are **removed** — no logs are collected.

## 7. Friction mechanisms

The user picks one method per block. It applies to both pausing and disabling. Configuration follows the five-screen wizard in §17's 19 September amendment; the details sheet is not a sixth screen.

### 7.1 Typing

A fresh passage of random words is displayed; the user types it correctly. Length is measured in characters, not words.

| Control | Default | Range | Step |
|---|---|---|---|
| Block stays off for (temporary pause) | 15 min | 5–100 min | 5 min |
| Passage length to pause | 150 chars | 100–200 chars | 10 chars |
| Passage length to disable | 350 chars (recommended) | 220 / 350 / 700 chars | Fixed choices |

Typo positions are highlighted only after a failed explicit Submit; there is no setting. Where other surfaces display typing estimates, use 0.4 seconds per character. The method cards state actual characters and pause duration in plain words, replacing the old configuration estimate paragraph.

Hard requirements:

- **Paste is blocked** on the input field.
- **Autocorrect and predictive text are disabled.** Mobile keyboards would otherwise complete the random words and defeat the mechanism entirely.
- The passage is generated fresh for each new challenge session. Ordinary app-switch/lock retains that session, passage and entered text.
- Attempts are unlimited. The user corrects and resubmits; there is no lockout or penalty.

### 7.2 Delay

A countdown runs; the user waits.

| Control | Default | Range | Step |
|---|---|---|---|
| Block stays off for (temporary pause) | 15 min | 5–100 min | 5 min |
| Wait to pause | 60 sec | 60–300 sec | 5 sec |
| Wait to disable | 6 min (recommended) | 3 / 6 / 12 min | Fixed choices |

A waiting challenge counts down only while the Toki Shrine waiting screen is visible and the device remains unlocked. The phone may be held or placed down. No hold detection, accelerometer variance or tilt condition is required. Leaving the app or locking the device retains the in-process challenge but resets progress to zero; returning restarts the full configured countdown. Choosing the explicit escape action terminates the challenge. These rules apply to both pause and disable waits; process-death/reboot restoration is not required.

### 7.3 Interruption

Both mechanisms require the app foreground with the screen on.

| What the user does | What happens |
|---|---|
| Sets the phone down mid-countdown | Countdown continues while the waiting screen is visible and device unlocked |
| Switches to another app | Challenge remains active in process. Waiting resets to zero; typing retains its passage and text. No terminal event is written. |
| Taps the escape action | Challenge ends. Pause-challenge escape is a walk-away; disable-challenge escape records turnoff_abandoned and leaves the block ON. |
| Screen turns off | Same suspension behavior as app-switch: waiting resets, typing is retained, and no terminal event is written. |
| Uses Android Back during a live challenge | The challenge is backgrounded and preserved using the same method-specific behavior; Back is not an intentional escape. |

The screen is kept awake for the duration of a challenge, so a routine display timeout never destroys progress the user was legitimately earning. Leaving the challenge or locking the device still cancels it, regardless of how the device was locked.

Elapsed time is measured **monotonically**, not against the wall clock, so changing the device clock cannot shorten a wait or a pause.

## 8. Walk-away accounting

A **walk-away** is recorded when the user declines to proceed by an explicit action:

- *Not now* on the block screen
- *Never mind* on the typing challenge
- *Never mind* on the delay countdown

**Suspension is not a walk-away or abandonment.** Switching apps, locking the screen, or using Android Back during a live challenge preserves the in-process challenge and records no terminal event. Only the explicit escape actions above intentionally end a live challenge. `challenge_abandoned` is retired: historical rows may remain, but the app no longer emits it and Phase 7 removes its unused instrumentation surface.

The counter shown to the user is **global** across all blocks. Individual events are still stored **per app and per block**, for event history and in-the-moment context. The Stats ledger applies the separate app-only rules in §9.

## 9. Stats — Your time (approved replacement, 4 October 2026)

This replaces the former walk-away dashboard in open Phase 7. Continue from the current implementation; do not revert Phase 6. Design screen number is **23** (the supplied handoff calls it 21). [S1–S6 PNGs](design/screens/stats-redesign/README.md) and the [HTML/spec](toki-shrine-ui-mockups/stats-screen/INTAKE.md) define layout; this section overrides their older behavior/copy. [Data/UI contract](docs/components/stats.md) defines the implementation boundary.

**Outcomes.** Explicit *Not now* / *Never mind* actions are nopes, whether the interruption targeted an app or a site. Home, Back, lock and switching apps suspend and produce no outcome. A successful pause challenge is a push-through, app- or site-triggered alike. Attempts = counted nopes + push-throughs; pending and turn-off challenges contribute neither. A site outcome attributes to the hosting browser's package: for site interruptions the browser is the Stats app, appearing on the app leaderboard with savings aggregated across its site and app triggers (4 October §17 addendum). The global in-the-moment walk-away counter retains raw app + site behavior from §8.

Repeated nopes for one package within five minutes of the **last counted** nope collapse into one Stats nope and attempt. Ignored repeats do not extend the window. At five minutes a new nope counts. Site-triggered and app-triggered nopes on the same browser package share that package's single sequence. A push-through always counts separately and ends the sequence. Deduplication is across blocks, local dates and process restarts. A backwards clock starts a new sequence rather than suppressing indefinitely.

**Baseline.** On first successful Usage Access read, capture recent usable history, up to seven days before that capture (not seven pre-install days). Reconstruct foreground visits; do not equate each activity resume with an app open. Per-app usual visit duration = measured duration of complete usable visits / number of those visits. Fewer than three usable visits means **10 minutes**. Store all observed packages so an app added later can use that frozen capture; unobserved packages use the fallback. Keep actual durations for arithmetic; minimum one minute applies only to the displayed visit label. Freeze the baseline until confirmed recalibration. Overall usual screen time uses fully covered local days from that history; if none exist it is unavailable.

**Storage/history.** Record new outcomes starting with this feature version; do not reinterpret old events as deduped attempts or invent old savings/post-challenge measurements. Past site nopes are not backfilled — site savings count only from the build implementing the 4 October attribution change. Before initial baseline capture, new nopes use the 10-minute fallback. Save each counted nope's duration contribution and baseline identity at outcome time. Recalibration atomically replaces the baseline for future nopes only; old savings never change. Retain outcomes and baseline through access revocation. Regrant resumes reading without implicit recalibration. Never clear existing blocks, events or markers on upgrade.

| Figure | Definition over seven local calendar dates, including today |
|---|---|
| Time saved | Sum of immutable saved-duration contributions from counted app nopes; remains an estimate with the headline **Time saved** |
| Time saved per day | Seven-day saved sum / 7, including pre-feature days as zero |
| “{total} saved so far” | Seven-day total, not all-time |
| Attempts / day | (Counted nopes + app push-throughs) / 7, rounded to a whole number for display |
| Nope rate | Counted nopes / resolved attempts, rounded whole percent; show an em dash and empty rate bar when there are no attempts |
| Screen time | Available all-app foreground time over the seven-date window / 7; hide unavailable average/comparison rather than inventing zeros. Delta against frozen usual screen time; no delta for unavailable/zero baseline, hide changes below 1%; neutral tone both ways |
| App rows | Installed apps in currently enabled blocks, plus browsers hosting enabled site blocks; include zero rows, sort by saved duration descending then display name. Removed/uninstalled apps, and browsers with no enabled site block, disappear from the list while historical totals remain |
| Time spent after the challenge | Measured first foreground visit to the triggering app after a successful pause challenge, never all usage during the block pause; for a site challenge the triggering app is the hosting browser. Attribute its duration to the completion's stored local date. Exclude later visits, other apps in the paused block and turn-off challenges |

A completed visit must be observable and uniquely attributable. Interrupted/missing history, an unclosed visit, an unrelated first app or an ambiguous return means **unavailable**, not 0m. A no-push-through day is a real zero. Unknown spent values use an em dash/stub and a neutral “Some visits couldn't be measured” note; do not present a partial sum as the full total. The conservative platform matching/coverage rules are fixed in the data contract.

**Dates/format.** Store outcome local dates at event time so later timezone changes do not move outcomes. Usage segments keep the zone captured on ingestion (Android does not supply historical zone changes), split at local midnight with DST-aware boundaries. Render actual rolling weekday labels, with Today last. Sum raw durations before rounding to nearest minute: under 100m as `48m`; otherwise `2h 12m`, dropping `0m` — except the two chart cards' bar labels, which always render whole minutes (`140m`; 4 October §17 addendum). Per-visit labels are `~6m`, rounded with minimum 1m.

**States and interaction.** Usage Access missing/revoked takes precedence: S5 permission card only. Granted with no enabled block: S6. At least one enabled block with no outcomes: full zero dashboard. S4 uses the same layout with zero saved bars for pre-feature dates and neutral tone. Loading is a blank content area (no spinner); load failure gets a retry state, never fake zero metrics. S1 contains the hero, two tiles, nested app list (~300dp max with overflow fade), both seven-bar charts and recalibration card. “Weekly info” is a static label; rows/bars are not tappable. S2 opens from hero/list info; S3 from nope rate; each supports Got it, scrim and swipe dismissal. Recalibrate asks confirmation and reports failure without changing the baseline.

**Corrected S2 copy:** “Every time you explicitly nope out, we estimate the time a usual visit would have taken.” Formula: Nopes × Usual visit length = Time saved. “We use recent available app history when you allow usage access, up to 7 days. If we have fewer than 3 usable visits, we use 10 minutes per visit. Recalibrating updates future estimates.” Dedup note: “Repeated nopes within 5 minutes count once. Completing a challenge counts as a separate try.” Do not copy the old pre-install or abandonment claims from PNGs. S3 uses resolved attempt counts; for zero attempts show “No tries yet” instead of a fabricated percentage.

## 10. Analytics

**Local only. No SDK, no network, no third party.** Events are written to a Room table and never leave the device.

Arjun named PostHog as the follow-up after Phase 7 on 27 September 2026. The [planning proposal](docs/plans/posthog-analytics.md) records scope and unresolved choices; this paragraph does not change the current local-only contract. An approved analytics/privacy amendment and separate implementation task are required before remote event collection.

The raw event store remains the local audit history. Redesigned Stats combines a transactionally paired outcome ledger, frozen baseline and local Android usage-event cache. UI reads the Stats repository; it must not derive the new metrics from old raw event counts. No local usage or target data is an approved remote payload.

Schema: `event(id, name, timestamp_utc, block_id?, target?, params_json?)`

### Event list

Active events are intentionally limited to three product uses:

1. **Product-critical:** onboarding, block-creation funnel, gate/challenge outcomes, pause outcomes and turn-off outcomes.
2. **Feature engagement:** bubble, Stats, Settings and Feedback interactions.
3. **Diagnostic:** accessibility-service lifecycle and URL-read failures.

Rows explicitly marked **retired** are historical compatibility only: they are not emitted, are excluded from the Phase 7 active-event audit and need not retain unused application constants. Existing stored rows are not deleted merely because an event is retired. Remote analytics and any SDK integration are outside Phase 7.

**Onboarding**

| Event | Params |
|---|---|
| `onboarding_started` | — |
| `permission_requested` | `permission` |
| `permission_granted` | `permission` |
| `permission_denied` | `permission` |
| `onboarding_completed` | `granted_count` |

**Block management**

| Event | Params |
|---|---|
| `block_create_started` | — |
| `block_create_step_completed` | `step` (1–5; details sheet is not a step) |
| `block_create_abandoned` | `step` |
| `block_created` | `app_count`, `site_count`, `friction_type`, `pause_minutes`, `pause_chars`, `turnoff_chars`, `countdown_seconds`, `turnoff_seconds` |
| `block_edited` | `block_id`, `fields_changed` |
| `block_deleted` | `block_id` |
| `block_conflict_shown` (retired; no longer emitted) | Historical: `target`, `existing_block_id` |
| `block_conflict_resolved` (retired; no longer emitted) | Historical: `resolution` (`moved` \| `kept`) |
| `block_turned_on` | `block_id` |
| `block_turned_off` | `block_id` |

**The interruption**

| Event | Params |
|---|---|
| `block_screen_shown` | `block_id`, `trigger_type` (`app` \| `site`), `target`, `latency_ms` |
| `walk_away` | `block_id`, `target`, `source` (`block_screen` \| `typing` \| `countdown` \| `auto_away`) |
| `challenge_started` | `block_id`, `type` (`typing` \| `delay`) |
| `challenge_completed` | `block_id`, `type`, `duration_ms`, `attempts` |
| `challenge_abandoned` (retired; no longer emitted) | Historical: `block_id`, `type`, `progress_pct`, `reason` (`app_switch` \| `screen_off`) |
| `typing_mismatch` | `block_id`, `chars_typed` |
| `countdown_started` | `block_id`, `seconds` |
| `countdown_stalled` (retired; hold detection removed) | Historical only |
| `countdown_resumed` (retired; hold detection removed) | Historical only |
| `countdown_completed` | `block_id`, `wall_ms`, `elapsed_ms` |

**Pause**

| Event | Params |
|---|---|
| `pause_started` | `block_id`, `minutes` |
| `pause_expired` | `block_id` |
| `bubble_shown` | `block_id` |
| `bubble_dragged` (retired; no longer emitted) | Historical only |
| `bubble_tapped` | `block_id` |
| `bubble_dismissed` | `block_id` |

**Turn off**

| Event | Params |
|---|---|
| `turnoff_started` | `block_id`, `type`, `chars` (typing only), `seconds` (waiting only) |
| `turnoff_completed` | `block_id`, `duration_ms` |
| `turnoff_abandoned` | `block_id`, `progress_pct` |

New app `walk_away` and `challenge_completed` outcomes carry `session_id`; successful completions also carry the original target/type. Their paired Stats rows are transactional, not asynchronous analytics-derived counters.

**Utilities**

| Event | Params |
|---|---|
| `stats_viewed` | `state`: default / partial / no_access / no_blocks; once per intentional entry after state resolves |
| `stats_info_opened` | `which`: time_saved_hero / time_saved_list / nope_rate |
| `stats_usage_access_cta_tapped` | — |
| `usage_access_granted` | Granted transition observed after a user request; not every refresh |
| `stats_recalibrate_tapped` | — |
| `baseline_recalibrated` | `old_screen_daily_ms`, `new_screen_daily_ms` (nullable); only on successful atomic replacement |
| `settings_viewed` | — |
| `feedback_opened` | — |
| `feedback_sent` | — |

**Service health**

| Event | Params |
|---|---|
| `accessibility_connected` | — |
| `accessibility_disconnected` | — |
| `url_read_failed` | `browser_package` |

`url_read_failed` is the silent-failure canary: if a browser changes its address-bar view ID, detection stops with no visible error. Counting nulls per browser surfaces it.

## 11. Humour assets

Carried over from the earlier CaffyBlock prototype to keep the experience light.

### Strings

Shown at random as the line on the block screen (§6, screen 15). Eleven lines:

```
Touch grass.
Go drink water.
Nice try.
Not today.
Bro. No.
Look who it is.
Naah bruh
Bold of you.
The algorithm can wait.
We meet again.
Let's give it a rest
```

The target-aware headline is selected independently from these seven templates:

```
Trying to waste time on {target}?
Back for {target} already?
Someone tryna open {target}?
Taking a detour to {target}?
Is {target} calling again?
{target}? Really?
Is someone missing {target}?
```

The first six remain from the earlier pool; the owner replaced *Right on schedule.* with *Naah bruh* and added *Let's give it a rest* during Phase 5 validation.

**Thirteen of the original eighteen were deliberately dropped** for violating P3 — they shame the user. Recorded here so they are not reintroduced: *How could you? · We talked about this. · Future you is watching. · You promised. · No. · That's a no from you. · Go do the thing. · This isn't the plan. · You're better than this. · Back to what matters. · Lmao, no. · Your goals called. They're disappointed. · This you?*

### Images and font

In `design/humor-assets/`:

- `sys_block_1.jpg` … `sys_block_11.jpg` — eleven images, sampled at random per block screen
- `anton.ttf` — condensed display face

Original source: `app/src/main/res/drawable/` and `res/font/` of the CaffyBlock project. In CaffyBlock these carried a full-bleed photo with "BLOCKED" in large red Anton over the top.

### How they combine with Nocturne

These two visual languages are opposed, so the merge is specified rather than left to judgement:

- **Nocturne owns structure, typography and controls.** Layout, buttons and hierarchy follow screen 15 exactly.
- The selected humour image is drawn twice: a full-screen cropped ground, then a centered aspect-preserving copy so the complete subject remains visible. On tall screens the cropped layer remains visible above and below the centered copy. The combined image is dimmed so foreground text stays legible.
- The humour string sits as the **line beneath the block name**.
- The headline follows the centered regular-weight Inter reference. The carried Anton asset is no longer used by the interruption headline.

## 12. Permissions and onboarding

| Permission | Stated purpose | Required? |
|---|---|---|
| Accessibility | Tells us which app or site is open | **Yes** — nothing works without it |
| Display over other apps | Shows the timer bubble | **No** — only the bubble is lost |
| Battery exemption | Stops the phone shutting us down in the background | Strongly recommended |
| Notifications | Shows the pause countdown | Recommended |
| Usage Access | Measures app foreground time for Stats; never app contents | **Stats only** — blocking still works without it |

Presented as a persistent, resumable **five-row** checklist with two states per row and a progress indicator. Usage Access belongs under Better experience, not Essential; reuse it in Settings. Open Android Usage Access settings and recheck on resume. It must not change the Accessibility-only ON-toggle gate. Reused later as the Settings permission-health view.

The app is **gated at block activation, not at app entry.** Users may install, explore and build blocks with zero permissions granted; the gate falls on the ON toggle. A permission wall on first launch kills activation; at the ON toggle the user wants it to work.

Sideloaded builds additionally require Android's "restricted settings" to be unlocked before accessibility can be granted. This is **not** an in-app step — it is delivered as text sent to testers, and it disappears if the app is ever distributed through Play.

## 13. Technical decisions

Recorded with rationale so future changes are informed rather than re-litigated.

| Decision | Rationale |
|---|---|
| **Native Kotlin + Jetpack Compose** | The core is entirely platform API — accessibility service, overlay windows, foreground service, sensors. A cross-platform framework would only help with the easy part and would add a bridge over the hardest code. |
| **minSdk 33 (Android 13), targetSdk 36** | Notification permission only exists from 13. A lower floor would need a conditional fourth checklist row appearing on some devices and not others — explicitly rejected. Cost is phones older than roughly four years. |
| **`applicationId` `com.arjunrana.tokishrine`** | — |
| **AccessibilityService for detection** | Verified working (§14). `packageNames` filtered at runtime to blocked apps plus supported browsers, so the service is idle almost always and battery cost is negligible outside active browsing. |
| **No overlay permission required for the block screen** | Accessibility services are exempt from background-activity-launch restrictions. Verified on Android 16 (§14). Overlay is needed only for the floating bubble. |
| **No Firebase, no third-party SDK** | Zero external dependencies in phase 1. |
| **Local Room event store** | Raw audit events plus transactionally paired Stats outcomes, frozen baseline, and a bounded local usage-event cache. No network or SDK. Package/activity identifiers and usage timestamps are sensitive local data; never attach them to feedback or export them. |
| **Browser map and OEM text as a bundled JSON asset** | Remote Config was considered and deferred. It earns its place only when there are store users who cannot be reached; with a handful of testers on App Distribution, pushing a build does the same job sooner. **The loader must be abstracted so switching to Remote Config later is a loader change and nothing else.** |
| **Feedback by email intent** | No backend exists. Opens the device mail client to `arjranaprep@gmail.com`, subject "Feedback from user", body prefilled with the typed text. No logs, no attachments, no collection. |
| **Dark theme only** | Nocturne ships no light tokens, and the product is used at night more than at any other time. |
| **Firebase App Distribution for test builds** | Console upload only — no SDK in the app, so it carries no code dependency. |

### Supported browsers

Website blocking reads the address bar via the accessibility node tree. Nine browsers, shipped as bundled JSON (**seven since the 10 October 2026 addendum**: DuckDuckGo and Vivaldi dropped):

| Package | Address-bar view ID |
|---|---|
| `com.android.chrome` | `com.android.chrome:id/url_bar` |
| `com.chrome.beta` | `com.chrome.beta:id/url_bar` |
| `com.sec.android.app.sbrowser` | `com.sec.android.app.sbrowser:id/location_bar_edit_text` |
| `org.mozilla.firefox` | `org.mozilla.firefox:id/mozac_browser_toolbar_url_view` |
| `com.brave.browser` | `com.brave.browser:id/url_bar` |
| `com.microsoft.emmx` | `com.microsoft.emmx:id/url_bar` |
| `com.opera.browser` | `com.opera.browser:id/url_field` |
| `com.duckduckgo.mobile.android` | `com.duckduckgo.mobile.android:id/omnibarTextInput` |
| `com.vivaldi.browser` | `com.vivaldi.browser:id/url_bar` |

Matching is **whole-domain including subdomains** — `reddit.com` matches `old.reddit.com`. No URL or path-level matching.

Required behaviours, all proven in the prototype:

- Do not act while the address bar is **focused** — the user is typing or seeing suggestions.
- Ignore values containing spaces — that is a search query, not a URL.
- Apply a **settle delay** before blocking a site, so the block does not fire mid-typing. The prototype used 2000 ms; this is a tunable constant, not a platform limit.
- **Cache the last known URL per window.** Chrome hides the address bar on scroll, so a missing node must not be read as "no site here."
- Debounce repeat triggers.

**Known limitation, stated plainly to users:** websites are blocked only in these browsers. Unsupported browsers and in-app webviews are not covered.

## 14. Verified platform findings

Tested 6 September 2026 on a Samsung Galaxy S23 Ultra (SM-S918B) running **Android 16**, using the CaffyBlock prototype.

- **App detection works.** The service binds, receives `TYPE_WINDOW_STATE_CHANGED` and `TYPE_WINDOW_CONTENT_CHANGED`, and the block screen appears **instantly** on launching a blocked app.
- **Website detection works.** `reddit.com` in Chrome was caught by address-bar reading and correctly identified. Observed latency 1.5–2 s, attributable to the 2000 ms settle constant rather than any platform limit.
- **Overlay permission is not required to launch the block screen.** Verified with the permission neither declared nor granted. Accessibility services carry a background-activity-launch exemption.
- **Still unverified:** overnight survival against Samsung's battery manager. This remains the largest open platform risk and is the reason the battery-exemption onboarding step exists.

## 15. Design assets

Stats screen 23 now uses [six supplied PNGs](design/screens/stats-redesign/README.md); retain old `23-stats.png` as historical only. PRD §9 overrides old copy in the raw handoff. All other visual authority remains Nocturne.

| Asset | Location | Authority |
|---|---|---|
| Design tokens | `toki-shrine-ui-mockups/project/_ds/nocturne-*/styles.css` | **Canonical.** Every colour, font, spacing, radius and shadow comes from these variables. Never hard-code a value the tokens carry. |
| Design language rules | `toki-shrine-ui-mockups/project/_ds/nocturne-*/readme.md` | Rules to follow — outlined buttons never filled, no pure black or white, Phosphor icons, 0.7× density |
| Screen renders | `design/screens/` | Layout and arrangement reference, 27 PNGs |
| Source markup | `toki-shrine-ui-mockups/project/TimeShrine Mocks.dc.html` | Exact values, structure and copy |
| Humour assets | `design/humor-assets/` | Eleven images plus the retained Anton source asset |
| Screen regeneration | `design/regenerate-screens.py` | Rebuilds the PNGs if the design export changes |

Nocturne is a **web** design system. Its tokens translate to a Compose theme; its CSS component layer (`.btn`, `.card`, `mix-blend-mode`) does not and should be ignored. Icons are Phosphor.

The `.dc.html` is a prototype rendering, not production code. It is the source of truth for values, copy and layout — never for implementation structure.

## 16. Deferred

**Phase 2:** uninstall protection (gated on user feedback) · escalating friction difficulty · re-arm prompt after turning a block off · block schedules · shareable achievements screen · NFC tag challenge · Remote Config · remote analytics export · pay-per-unlock.

**Later:** in-app surface blocking (Shorts, Reels) · iOS · the separate hard-block product.


## 17. Phase 2 owner refinements — 12 September 2026

Source: Arjun's owner device feedback in the Codex session. These are approved product inputs unless explicitly marked pending; several intentionally change earlier acceptance criteria. Old mockups will not be updated and must not override this section. Historical recording base: `9d02a28`. Refinements are implemented through `512dc36`; see the latest checkpoint below. Exact installed commit was not independently checked by Codex.

**Historical refinement status:** The 19 September wizard amendment below supersedes R1's maximum, R2/R4 configuration layout, R9's step denominator, old configuration helper/estimate copy, and the old UI wait bounds. Unaffected ownership, input, target-list, activation and save rules remain in force. Prior implementation checkpoints describe earlier code, not implementation of the redesign.

### Confirmed requirements

| ID | Area | Required change |
|---|---|---|
| R1 | Pause duration | Both typing and delay: default 15 minutes, minimum 5. Existing maximum 120 and step 5 remain. |
| R2 | Configuration order | Typing headings/order: **Type to pause → Pause duration → Disable this block** (owner clarified 13 September). Delay headings/order: **Wait for this duration → Pause duration → Disable this block**. |
| R3 | Typing estimates | 0.4 seconds/character consistently; 100 → 40 seconds, 300 → 120 seconds. Do not reuse the reference image's old numeric examples. |
| R4 | Delay explanatory box | Remove the additional box saying “A XXX sec wait each time you want in · no typing…”; retain the individual control explanations below. |
| R5 | Create review copy | Remove the entire “It’ll be saved switched off. Nothing happens…” sentence. Saving OFF behavior stays unchanged. |
| R6 | Create review target list | Replace horizontal target chips with a vertical app/site list like evidence image 02: icon, label and row dividers. Bound the list and allow scrolling with larger selections; it must not overflow horizontally or push the friction summary/Save out of reach. Show up to **four rows**, then scroll within the target list (owner confirmed 13 September). |
| R7 | Block names | Name input must be single-line and at most **30 characters** (raised from 20 after device testing, 13 September; supersedes the earlier 40 and 20). Display names may wrap to two lines without ellipsis. Owner confirms this is an early test build with no existing longer names; no legacy-name migration or compatibility path is needed. |
| R8 | Empty blocks | In both create and edit, users may temporarily remove all targets, but cannot leave step 1 or save until at least one app or website is selected (owner confirmed 13 September). Enforce nonempty saves at the persistence boundary too, so direct friction editing/stale drafts cannot bypass this rule. Owner authorizes one-time removal of empty blocks from the testing app; preserve every nonempty block. Perform this as explicit test-data cleanup during the builder phone handoff, not a production migration. |
| R9 | Detail editing destination | The detail screen’s **THE FRICTION → Edit** opens the existing editor directly at configuration (3/4), with stored values loaded. Contents editing still starts at 1/4. Back from direct friction entry at 3/4 returns to block detail (owner confirmed 13 September). Preserve cancellation and save semantics. |
| R10 | Detail activation | Remove redundant separate “Turn on” CTA. Keep the existing on/off switch as the sole activation control (owner confirmed 13 September). |
| R11 | Home actions | Bottom row: smaller **Feedback** button (no icon) on the left and wider **+ New Block** button on the right (owner confirmed 13 September). No overlap; both remain reachable. |
| R12 | Turn-on confirmation | Subtitle: **From now on, wasting your time on whatever's in this block won't be easy! Block Summary:** Remove the “This is the last easy moment…” sentence. |
| R13 | Switch appearance | Improve shared on/off switch geometry: thumb must remain inside the track with visible inset, including ON at the right edge. Preserve operation and theme consistency. |
| R14 | Delete confirmation | Attempting to delete an OFF block shows **Are you sure?** with **Confirm** and **Go back**. Only Confirm deletes; Go back leaves data unchanged. ON blocks remain unavailable for deletion. |

Exact helper copy, under the relevant control:

- Typing passage: **You'll need to type this number of characters each time you want to pause the block.**
- Delay wait (heading **Wait for this duration**): **You'll need to wait for this long each time you want to pause the block.**
- Pause duration, both modes: **Stay unblocked for this long. Then the block will be activated again.**
- Disable this block, both modes: **Type these many characters to disable the block completely. You can edit or delete it once it's off.**

### Clarification status

All listed owner choices are resolved as of 13 September 2026. R1–R14 and the post-validation addenda are implemented; no legacy-name migration is needed.

### Evidence and validation status

Evidence in `Verification Feedback/phase-2-verification/owner-feedback-2026-09-12/`:

- `01-review-overflow.png`: observed create-review overflow; website chip stretches at right, summary pushed down.
- `02-review-list-reference.png`: desired vertical target-list arrangement only. Old helper copy, estimates and saved-OFF sentence in this reference are superseded above.
- `03-home-actions.png`: overlapping home actions.
- `04-switch-on.png`: ON thumb touching track edge.

Historical owner-reported original checklist (superseded by the checkpoint below): 1 issues/refinements; 2 pass under old estimate, revised requirement now pending; 3 issues/refinements; 4–9 reported fine (including 8, with revised settings pending); 10 owner-reported PASS (confirmed 13 September); 11 delete-confirmation refinement pending. Preserve these as historical evidence; retest changed paths after repair. Neither screenshots nor source inspection establish database/event assertions. No Phase 3 clearance.

### Post-validation addenda — 13 September 2026 (owner device feedback on the R1–R14 build)

These supersede conflicting values above the same way §17 supersedes older sections. R7's cap is updated in place (30 characters). The remaining items extend §17:

- **Step 1 scroll and pinned CTA:** with a long selection the selected-list area scrolls on its own; the bottom CTA row ("N selected" + Next) stays pinned and floating at the bottom with a little padding above and below, mirroring the friction step (owner: previously Next fell below the fold with no scrolling at all).
- **Block detail target list:** the APPS & SITES section uses the same vertical icon/label/divider list as the create review, with the same four-row bound. A small scrollbar indicator appears at the right edge of either bounded list whenever its content overflows the four-row cap.
- **Launcher icon:** the app uses the generated torii+hourglass artwork from `design/icon-final/` as the launcher icon, integrated per that pack's README (mipmap rasters + adaptive icon with the brand background `#161826`; no monochrome layer until a vector redraw exists).
- **Home bottom actions:** the left button reads **Feedback** (label only, no icon); the Feedback + New Block row keeps padding above and below at the bottom of the home screen.

### Historical Phase 2 validation checkpoint

**Checkpoint at 13 September 2026: code PASS, `6c3b962`.** Codex reviewed `084b5eb..6c3b962`: unchanged-edit fixture corrected, legacy typo normalization explicitly covered, enabled-state assertions corrected, helper copy updated, and interior multiline website paste rejected without merging. No blocking findings in the corrective diff. Phase 3 implementation is cleared from `6c3b962`; verify git state. GLM reports debug build and 12 JVM tests passing, instrumented sources compiled only. Codex ran no builds/tests/device operations. Latest UI regressions and paste behavior remain pending owner device confirmation; this is implementation clearance, not a new device pass. Haptic tuning remains pending; Phase 3 must emit activation success feedback after permission checks and successful persistence.

Those pending Phase 2/3 items were subsequently accepted in the closed [Phase 3 checklist](coordination/tasks/TS-P3-validation/OWNER-CHECKS.md); the paragraph above retains its original evidence boundary.


### Post-validation addenda — 13 September 2026, second round (owner device feedback)

- **Website input (step 1):** the field is single-line (Enter inserts nothing) and pasted line breaks are **preserved, not stripped** — an interior break keeps the input invalid, so a multiline paste like `reddit.com↵abdes` is rejected outright and never silently merged into an addable domain (owner clarification, 13 September). Add requires a complete domain — two or more dot-separated labels of letters/digits/hyphens (no leading/trailing hyphen, 1–63 chars each), ending in a letters-only TLD of ≥ 2 characters, whole-domain only, canonical form (trimmed/lowercased/trailing dot removed; surrounding whitespace alone does not invalidate). "reddit" and "abdes" are rejected with the helper "Enter a complete domain like reddit.com"; no IDN/punycode support yet. `isValidFullDomain` in `ui/util/Domain.kt` is the single validator (JVM-tested).
- **Helper copy supersession (step 3):** Type to pause: "You'll need to type random words of this length to pause the block." Disable this block: "You'll need to type random words of this length to disable this block. You can edit or delete this block once it's disabled." (Codex-corrected 13 September: both Disable sentences read "this block", matching the owner's requested copy.) These replace the §17 sentences for those two controls.
- **Typos are always shown:** the "Show where the typos are" control is removed; every save writes `show_typos = true`. The column is retained this dev phase only to avoid a destructive wipe mid-testing; Phase 5 must always show typos regardless of any stored value, and the column should be dropped in the next schema revision.
- **Wait duration bound:** the configuration stepper's maximum stays 5 minutes (300 s, never mentioned on the UI); the persistence boundary additionally rejects any draft storing a wait above 20 minutes (`MAX_STORED_COUNTDOWN_SECONDS = 1200`) on both create and update.

### Post-validation addenda — 13 September 2026, third round (owner feedback)

- **Friction-screen estimate sentence (typing mode):** "You'll take X seconds to type random words each time you want to pause this block." — X is the live 0.4 s/char estimate in plain seconds. Replaces the previous "About X seconds to type each time you want in" box text; other surfaces (review, detail, activation) keep their existing estimate phrasing.
- **Haptic confirmation on activation changes:** a stronger vibration whenever a block is turned on (the turn-on confirmation's Turn it on, the single path through which every ON flows) and a smaller vibration whenever a block is turned off (detail switch and home-row switch). Implemented with predefined effects (heavy click on, click off); requires only the normal install-time VIBRATE permission.

Owner haptic tuning feedback: current pulses feel weak; +30% duration was suggested, not finalized. Keep tuning pending. Interior multiline website paste is rejected without merging as of `6c3b962`; surrounding whitespace alone is trimmed. Owner device confirmation remains pending.

### Phase 3 direct-correction addenda — 19 September 2026 (owner device feedback)

These owner decisions supersede conflicting onboarding/battery presentation text above without changing the then-current four permission rows or activation gates. The 4 October Stats redesign adds Usage Access as a fifth Better experience row; activation gates remain unchanged:

- **Welcome:** headline `Your time, your rules`; body lines `Doomscrolling? Time-blindness? Yeah, we got you.`, `We don't block you! We just do a vibe check before you fall in.`, and `You choose what gets paused, for how long, and how you get back.`
- **Battery guidance:** presentation is manufacturer-neutral and says Android is aggressive. Remove the owner-facing manufacturer picker and its `Not a Samsung? Pick your phone` CTA. Automatic Samsung/generic step selection and real-state refresh remain.
- **Accessibility handoff:** the app cannot decorate or highlight Android/One UI's system-owned **Installed apps** row. Keep the generic Accessibility Settings route; any extra direction belongs in Toki Shrine's preceding explainer and requires a separate owner copy decision.
- **Insets and spacing:** all screen roots respect top and bottom system insets; top navigation, the Blocks title and bottom-most CTAs retain consistent safe separation. The new-block review helper has visible space below `Turn it on from the “Blocks” homescreen.`
- **App loading:** while installed apps are being fetched, show `loading...` immediately below search and remove it when loading completes.

Checkpoint `f92661d`: Codex reviewer PASS for accumulated range `7d779a6..f92661d`; owner reports the correction set passes on the installed SM-S918B / Android 16 build. This is targeted acceptance, not Phase 3 clearance; the active task retains the unreported behavioral and nonvisual checks.


### Block wizard redesign — 19 September 2026 (approved requirements)

Source: owner redesign and follow-up decisions. This amendment supersedes conflicting older mock annotations and §17 configuration requirements. References: [Friction selection](design/screens/Friction-selection.png) and [Disable block](design/screens/Disable%20block.png). Written values here prevail, including the **5-minute** pause minimum (the image says 10), **60-second** default wait and **3 / 6 / 12-minute** disable ladder (ignore the image's old 1 / 3 / 10 note).

Implementation status, 20 September 2026: completed and closed in `7dc5aa4` with reviewer, owner and executed nonvisual PASS. Live phase authorization remains in [CURRENT](coordination/CURRENT.md).

1. **What should this cover?** Existing Apps / Websites segmented selection, validation and ownership behavior.
2. **Give it a name.** Existing name entry and limits.
3. **What should getting in cost?** Two tappable cards: **Type a passage** (selected initially; 150 characters, random words, then 15 minutes before the block returns) and **Wait it out** (60-second wait, then 15 minutes). Display actual draft values in plain words. Selecting a card does not move other controls. An outlined **Adjust the details** button occupies normal layout space under the cards, not a floating overlay.
   - Optional **The details** bottom sheet over the dimmed selection; only the selected method's passage length or wait duration, plus **Block stays off for**, Reset and Done. Bounds/defaults are in §7. Helper text states minimum 5 minutes.
   - Changes update the draft and card summary immediately. Done, Back, swipe or outside dismissal retain draft changes; none saves a block. Reset restores the displayed method's default and pause duration 15 minutes. Switching methods retains each method's draft adjustments.
4. **Disabling the block.** Copy: “Tell us how you prefer to disable the block. It needs to be a little inconvenient!” Inherit the method; never ask it again. Three tappable choices: typing 220 / 350 / 700 characters, or waiting 3 / 6 / 12 minutes. Middle choice is recommended and preselected, so Next alone is valid. Choices are fixed, independent of pause settings. Retain each method's draft disable choice when switching methods. Use the visible card copy in the disable reference; ignore contradictory annotation notes.
5. **Review.** Preserve layout, target list and pinned Save with the current homescreen helper. Update summaries to actual chosen pause and disable method/values. Saving persists once and creates an OFF block; activation remains separate and permission-gated.

Progress is 1/5 through 5/5. Contents editing starts at 1/5; direct friction editing starts at 3/5, then disable and review. Back from direct-entry 3/5 returns to detail. Sheet Back dismisses the sheet before wizard navigation. Preserve draft values through supported recreation, save rejection and navigation. Canceling editing never writes changes. Detail and turn-on confirmation must describe the chosen disable method accurately.

Persist disable waiting duration separately from pause countdown (proposed field `turnoff_seconds`, default 360); do not overload `turnoff_chars` with seconds. Save/edit events include the new field and the fifth step. Enforce §7 ranges and fixed ladders for new create/update writes. The old 1..1200-second storage allowance is superseded for the pause countdown by 60..300 seconds; disable wait validation uses 180/360/720 seconds separately.

Owner permits deletion of **all existing app data** because there are no users; no legacy value mapping or data-preserving migration is required for this redesign. Use a deliberate development schema reset/version change as needed, report its consequences, and rebuild test fixtures. This is not a general production destructive-migration policy. No device data was deleted during requirements work; device operations still follow AGENTS and the explicit setup scope.

The redesign delivery covers wizard/editor, persistence, summaries, events and tests. Actual typing/waiting challenge execution and cancel/reset behavior remain Phase 5, with no hold detection. At approval time this amendment did not itself clear Phase 3 or authorize Phase 4; the later evidence-backed clearance is recorded in CURRENT.

### Phase 5 owner-validation addendum — 23 September 2026

Arjun's installed-build testing supersedes the older interruption copy, two-second walk-away dismissal, live typo highlighting, and terminal app-switch/lock rules in §§6–8/11. The corrected behavior is incorporated directly into those sections.

For ongoing owner testing only, **debug builds** expose pause typing down to 20 characters and pause waiting down to 20 seconds while retaining defaults 150/60 and maxima 200/300. Debug disable ladders temporarily replace their first rungs with 20: typing 20/350/700 characters and waiting 20/360/720 seconds. Release builds retain the production §7 values: 100–200, 60–300, 220/350/700, and 180/360/720. Pause duration remains 5–100 minutes. Phase 7 final approval must remove the temporary debug overrides and retain the production values.

**Current implementation:** Phase 7 commit `89ef9e3` removed these temporary debug overrides; every variant now uses production §7 values. Existing stored test blocks remain intact. The Phase 7 owner/device checks for that change are still pending.

### Phase 6 owner-validation addendum — 23 September 2026

Arjun's installed-build testing of the reviewed `062c71e` tree recorded no failing checks (owner verdicts live in the Phase 6 task's OWNER-CHECKS; O15 is a conditional pass without the manual clock-change steps, O17 is not applicable as written). The screen-20 and §7 text above already incorporates the decisions below.

- **Bubble dismissal (screen 20, owner-approved: "we need to give the ability to dismiss the bubble if the user wants it").** Dragging the bubble to the bottom screen edge and releasing discards it. Dismissal hides only the bubble surface for the pause instances visible at dismissal; any new pause start shows the bubble again with a fresh `bubble_shown`. Timing, enforcement, the ongoing notification and automatic re-arm are unaffected. §10 gains `bubble_dismissed | block_id`.
- **Cross-block unfinished-challenge precedence — accepted, do not fix.** While a challenge is unfinished, a detection launch for a different block brings the existing session forward instead of starting that block's challenge; completing or explicitly dismissing it is the only way to clear it. Owner verdict: acceptable narrow edge case; do not "improve" this or the notification behavior below without real user feedback.
- **Dismissed pause notification may return — accepted, do not fix.** Android may permit swiping the ongoing countdown notification away; the service's periodic render re-post then recreates it. Owner verdict: recorded platform observation, nothing to fix.
- **Turn-off challenge visual round (owner corrections, 23 September 2026, incorporated into §6 screen 22).** The header shows the block name only (no "Turning off ·" eyebrow); "Leave it on" is retired — the typing variant escapes via a full-width outlined *Never Mind* button under Submit and the waiting variant's outlined bottom button reads *Never Mind*; the disable info card reads "Type in X characters to disable the block" with its lock icon vertically centred. Nothing else about either challenge changed.

### Phase 7 owner-review addendum — 4 October 2026

Arjun's decisions on the combined-review notes P7-N3–P7-N7 (verdicts recorded in the TS-P7 task's OWNER-CHECKS): P7-N3 (a recalibrated week reading "partial" until the window rolls past), P7-N5 (`usage_access_granted` logging any observed false→true grant) and P7-N7 (stale Codex prose hash, record-keeping only) are accepted as reviewed — no behavior change. Two decisions change §9 for the affected items and supersede conflicting text above:

- **Chart bar labels are minutes-only.** The two seven-bar chart cards' per-day value labels always render whole minutes (`140m`), never §9's `2h 12m` hours+minutes form, which clips in the ~35dp bar column. Wider figures (hero, tiles, chart totals, app rows) keep the §9 duration format. Incorporated into §9's Dates/format paragraph.
- **Site-block savings attribute to the hosting browser.** §9's former rule that "a browser is an app only when the interruption targeted its package, not a URL" is superseded: when a site block triggers in a browser and the user nopes out, the Stats outcome is recorded against that browser's package, so the browser appears on the blocked-apps leaderboard and its time-saved row aggregates site-block savings. Arjun settled the remaining semantics the same day (incorporated into §9): site pause push-throughs count as attempts attributed to the browser; post-challenge spent time for a site challenge is the first completed visit to the hosting browser under the same conservative attribution guards; site- and app-triggered nopes on one browser package share that package's single five-minute dedup sequence; browsers hosting enabled site blocks appear on the app leaderboard; and past site outcomes are never backfilled — site savings count only from the build implementing this. The addendum does not authorize recording site outcomes before that implementation lands.

### Phase 7 owner-experience addendum — 8 October 2026

Arjun's device feedback on the debug testing-values build (`70bd8e8` + Claude delta 5), with his answers to the clarifying questions in the same session. **Recorded, not yet implemented**: it supersedes conflicting text above, including §6 screens 21/22, §8's nonterminal app-switch/lock rule and §9, only from the build that implements it. Change requests P7-F1–P7-F15 in the TS-P7 task carry ownership and scope.

**Challenge screens (pause and turn-off)**
- **Never mind at the bottom.** The pause typing challenge's header "Never mind" moves under Submit as a full-width outlined secondary button, matching the turn-off layout. Submit stays the primary button and stays disabled until the passage length is reached. The waiting challenge keeps its bottom button.
- **Back no longer suspends.** While a challenge is active, Back/back-gesture does not send it to the background. The Never mind button wiggles (with a light haptic) to call attention to itself. This applies to typing and waiting, for both pause and turn-off.
- **Auto-nope after 15 s away.** "Away" means the challenge or its gate screen is not visible: app switch, Home, or a locked or switched-off screen.
  - **What happens:** after 15 continuous seconds away, a pause challenge is recorded as a nope. On a turn-off challenge it's a Never Mind; the block stays on.
  - **What doesn't count:** time on screen without progress never counts. A waiting challenge left visible still completes and counts as a push-through.
  - **How it's valued:** an auto-nope is valued and counted exactly like a chosen nope: an attempt, saving the usual visit length or 10 m, with the five-minute dedup unchanged. It carries a distinguishable walk-away source (`auto_away`).
  - **No Walk-Away moment:** it's recorded quietly and no counter screen is shown.
  - **Coming back in time:** returning within 15 s resumes the same challenge, but typed text is cleared and the wait restarts (as today).
  - **A failed save never costs a completed challenge** (owner decision, 8 October, F-A review P7-F-A2). If saving a completed challenge fails while the screen is on, it is retried as before. If the screen is off or Toki is hidden, the completion is kept and saved when the user is next on screen. The away clock never starts from a failed save.
  - Auto-nope also stops suspended challenges from different blocks clashing later.

**Blocks — Add more.** The block detail APPS & SITES label row gains an **Add more** action, available while the block is ON.
- It opens a standalone "What should this cover" screen with no step indicators and no Next. The primary action is **Confirm and Save Block**; the secondary is **Never mind**. Never mind and Back return to the block.
- **Add-only:** existing apps and sites show as included but can't be removed there. Removing still requires turning the block off first.
- Ownership rules are unchanged ("Already added to a block").
- Additions to an ON block protect on the next detection update. If that block already has an active pause, newly added targets share the remaining pause and start being blocked when it ends (Arjun, 8 October F-B review; pause is per block).

**Stats — layout update** (supersedes the §9 hero/tile arrangement; references in `design/screens/stats-redesign/`, files dated 2026-10-08).
- **Today** (local midnight): "Time saved today ⓘ" hero with a `today` suffix, then the line "Screen time Xh Ym today". The comparison to your usual is **removed**. Two equal-size tiles follow: **Attempts today** (counted nopes + push-throughs) and **Nope rate today ⓘ** with its bar.
- **This week** card: **Avg time saved** (`48m / day`, 7-day saved ÷ 7) and **Nope rate · N of M**.
- **Time saved on blocked apps ⓘ**: rows read "N nopes this week" plus a visit-length chip (`~6m /visit ✎`, or `12m/visit · yours` when set by the user). Saved time sits on the right. The list is a fixed-height box of about five rows that scrolls on its own.
- The two seven-day charts and the Recalibrate card are unchanged. Attempts/day is retired.
- **Time saved sheet:** generous spacing with the copy "Each nope skips a visit. We count the time that visit usually takes.", the Nopes × Usual visit length = Time saved formula, and "Usual visit length comes from your 7 days before Toki. You can set your own per app." The "Tried again within 5 minutes…" line is removed.
- **No enabled blocks:** the S6 empty state shows only when there is also no outcome in the seven-day window. Otherwise the dashboard stays visible with a small "No blocks are on" note, and apps with outcomes this week stay listed. Owner to confirm on implementation review.

**Stats — usual visit length.**
- **Measured value:** ignore visits shorter than 30 s, then take the 75th-percentile visit length. Fewer than three such visits falls back to 10 m. The percentile and the 30 s floor are config values.
- **Per-app override:** tapping a row's chip opens the "<App> · time per visit" sheet. It has a minutes stepper, 3/5/10/15/20m presets, the live "N nopes × Xm = saved this week" line, Save, and "Use measured (Xm)". The override always wins.
- **Re-valuing:** an override **re-values that app's counted nopes for the week shown**, not only future ones. This supersedes §9's frozen per-outcome valuation for apps with an override.

**Config.** One plain Kotlin config file holds the editable lists and values, checked at compile time. Images stay in `res/drawable`, and values that differ by variant stay in the variant `ChallengeValues.kt`. Contents:
- gate background images, headlines and humour/subtext lines;
- the typing word list;
- the disable ladders ("a bit", "a bit more", "make it hurt") for characters and time;
- the pause ranges;
- the auto-nope seconds;
- the visit-length percentile and minimum visit.

**Typing passages.** Increase variety: a larger word list, no immediate repeats, and the exact-length fill must no longer force the same short closing words.

**Feedback (screen 25 / §13).**
- **Expected:** Send it opens the mail app to the fixed recipient, with subject "Feedback from user" and the typed text as the body. The user sends it from the mail app.
- **Bug:** Gmail opened with no subject and no body because both were passed only as intent extras. They must also be encoded in the `mailto:` URI.
- **No draft:** typed text survives only rotation or recreation. It's cleared on leaving the screen and after a successful handoff, and never persisted. This supersedes P7-O10's process-death expectation.

### Phase 7 owner-feedback addendum — 9 October 2026

Arjun's device test of the 8 October build (debug APK `65460244…`). It supersedes conflicting text above, including the 8 October addendum, only from the build that implements it. TASK rows P7-F16–P7-F28 carry ownership.

**Challenges**
- **Escape label:** the challenge escape button reads **"Nope, not now"** on typing and waiting challenges, for pause and turn-off alike. The block (gate) screen keeps "Not now".
- **Back nudge:** the haptic is one step stronger (a click instead of a tick).
- **After an auto-nope, land on the phone's home screen** (pause and turn-off), including when it happens while the screen is off. Toki is not shown. A turn-off challenge the user abandons with the button still returns to the block.

**Celebration (Walk-Away moment, screen 16)**
- **Layout:** follows the 9 October reference (image, bold title, grey subtitle, "N wins today" pill) on a light lavender background. This is a deliberate exception to the dark theme for this screen only. Tap-to-continue and the automatic dismissal stay.
- **Titles:** one title is picked at random from these pairs, each with its own image:
  - "Look who's taking control 😎" (cat)
  - "Okay, someone's on a roll!" (DiCaprio toast)
  - "You absolute legend." (Michael Scott)
  - "FOUR TIMES?! WHO ARE YOU? 🔥" (Elmo fire). Eligible **only on the day's fourth nope**.
  - "Your phone is losing this battle." (victory dance)
  - "Touching grass: Professional level." (a suitable meme)
- **Subtitles:** one is picked at random:
  - "Your future self says thanks."
  - "Doomscrolling took another L."
  - "Plot twist: You chose yourself."
  - "That could've been N minutes of reels." N is this nope's valued visit length: the app's own setting if the user set one, else the measured or fallback value. It is only offered when the nope saved time (not a repeat within five minutes).
  - "Another distraction defeated. Flawless victory."
  - "Your screen time is trembling."
  - "You're the main character today."
- **Images:** supplied by Arjun. The rights and licensing decision is his before any public release.

**Stats**
- **Hero:** remove the "X saved so far" line.
- **THIS WEEK:** the nope rate shows only "N%" ("—" with no attempts), without "N of M".
- **Today tiles:** Attempts today and Nope rate today are equal in size and aligned as in S1 v2.
- **App list:**
  - Rows size to their content (no clipping). The box is at most about five rows and scrolls on its own.
  - Every chip reads `~Xm/visit ✎` in the same outlined style. A user-set value is **not** marked ("· yours" is removed).
- **Visit-length sheet:** as S8. The presets fill the full width in equal widths, and the stepper is shown large inside a tinted box.
- **Time saved info sheet:** the first sentence becomes "Each ‘nope’ saves you a visit."
- **Not a bug:** today's figures reset at local midnight. Stats history is never removed by turning blocks off or deleting them.

**CTAs.** Paired full-width buttons outside the challenge screens match the challenge screens:
- 50 dp tall, 10 dp radius, a 15 sp medium label;
- a 12 dp gap between stacked buttons.

This covers Add more, the visit-length sheet, the Stats info sheets and the Recalibrate card.

**Settings and permissions**
- **Settings:**
  - The Theme row is removed.
  - "Supported browsers" opens a list of every supported browser from the bundled detection configuration.
- **Permission checklist:** the screen scrolls, so every row (including Usage Access under "Better experience") is reachable.

**Owner changes — 10 October 2026** (supersede conflicting text above, including §9's five-minute rule and the escape-label bullet):
- **Repeat-nope window is two minutes.** A nope within two minutes of the last counted nope for the same package (a site's browser included) does not count again. At two minutes a new nope counts. The fixed anchor, push-through reset and backwards-clock rules are unchanged.
- **The friction (block) screen's main button reads "Nope, not now"**, matching the challenge escape.
- **Seven supported browsers.** DuckDuckGo (`com.duckduckgo.mobile.android`) and Vivaldi (`com.vivaldi.browser`) are dropped from website blocking and the bundled detection config; the Settings subtitle and Supported browsers screen show the remaining seven. Supersedes "nine browsers" in §2 and the Supported browsers table.
- **The waiting challenge's escape matches the typing escape:** 50 dp tall, 15 sp label.

# Toki Shrine — PRD 1.0

Build specification for phase 1. Supersedes `PRD-phase-0.md` and `DESIGN-BRIEF-phase-1.md`, which are retained as history.

Android only. Phone, portrait, dark theme only.

---

## 1. What it is

An Android app that puts a deliberate, self-chosen pause in front of the apps and websites a person doomscrolls.

It never blocks anyone out. The user picks what to slow down, picks what it should cost to get in, and then pays that cost — either typing out a passage of random words or sitting through a countdown while holding the phone. If they still want in afterwards, that is a fine answer.

## 2. Positioning and principles

Anti-brainrot, not productivity. The subject is endless scrolling and endless watching — not time management, not focus sessions, not parental control.

**P1 — This app never hard-blocks.** The user can always get in. The job is to make that a conscious decision rather than an unconscious one. A pure hard-block product is out of scope and would be a separate app.

**P2 — Backing out is the easier path.** On every interruption screen the walk-away is the visually prominent action and the way in is the quieter one. Getting in is offered plainly, never hidden.

**P3 — Never shame.** Nothing in the product tells the user they have failed, disappointed anyone, or broken a promise. Dry humour is welcome; guilt is not. This principle governs all copy, including the humour pool in §11.

## 3. Scope

**In**

Blocks (create, edit, delete, on/off) · Android app blocking · whole-domain website blocking in nine browsers · two friction mechanisms (typing, delay) · hold-to-advance countdown · timed pause with automatic re-arm · turn-off via typing · floating bubble · ongoing notification · walk-away counter and stats · permission onboarding · feedback by email.

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

Two distinct gates: **pausing** costs the block's chosen friction challenge; **turning off** always costs a typed passage, deliberately longer.

### Rules

- An app or website belongs to **exactly one block**. Adding one already held elsewhere prompts a move confirmation; declining returns the user to the editor unresolved.
- **Toki Shrine cannot be added to a block.** It is filtered out of the picker.
- Blocks are independent. Any number may be ON, paused, or OFF simultaneously.
- A block **must be OFF before it can be edited or deleted**. Otherwise removing an app from a block would be a free bypass.
- A new block is saved **OFF** and does nothing until explicitly turned on.
- Turning a block OFF leaves it off until the user turns it back on. It never re-arms itself.
- Pauses cannot be extended. When the timer expires the block re-arms immediately, with no prior warning — the bubble and notification have been visible throughout.

## 5. Core flows

**Onboarding** → Welcome → permission checklist (4 items) → per-permission explainers as needed → block list.

**Create a block** → add apps and sites → name it → configure friction → review and save → lands in the list, OFF.

**Turn a block on** → confirmation stating exactly what pausing and turning off will cost → ON. If permissions are missing, this is where the app gates and points at the checklist. The user is never gated before this point.

**Hit a block** → block screen → *Not now* (walk away) or *I'll do the challenge* → challenge → pass → pause begins → bubble and notification count down → automatic re-arm.

**Turn a block off** → type the turn-off passage → OFF until manually re-enabled.

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
| 12 | Conflict dialog | `12-conflict-dialog.png` |
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

**2 Permission checklist.** Four rows, two states each — pending and granted. Progress indicator ("1 of 4"). No other states. Persistent and resumable; the same component is reused on the Settings permission-health row.

**3 Accessibility explainer.** Shown before Android's own permission screen. States plainly what the app does and does not do. Content is load-bearing — if this screen fails, users abandon here.

**4 Battery settings.** Instructions vary by manufacturer, detected from `Build.MANUFACTURER`. Samsung is the authored default. A quiet "Not a Samsung? Pick your phone" link lets the user override detection. The universal Android battery-optimisation dialog is the baseline path on every device.

**5–6 Block list.** The app's home screen. Header carries two icon buttons, top right: **Stats** (bar chart) and **Settings** (gear). A floating action button creates a block. A *Share feedback* button sits at the bottom. Each row shows the block name, a summary line (contents · friction), and an on/off toggle.

**7 Create · add apps & sites.** Segmented Apps / Websites. Apps opens a searchable installed-app list; websites take a typed full domain. Selected items are listed below with a running count.

**8 Create · app search.** Live filtering with a match count and an *Added* state per row.

**9 Create · name it.** Free text. The suggested-name chips in the mock are **removed**.

**10 Create · configure friction.** The densest screen. See §7 — note it must carry **three** numeric controls, one more than the mock shows.

**11 Create · review & save.** Reads back the deal: contents, cost to pause, cost to turn off, pause duration. States that the block saves switched off.

**12 Conflict dialog.** Raised when an app already belongs to another block. Names the other block. Actions: *Move it here* / *Leave it where it is*.

**13 Turn-on confirmation.** The commitment moment. States both costs in plain language and closes with "This is the last easy moment."

**14 Block detail & edit.** Shows on/off state, contents, and friction summary. Editing is only reachable while OFF, and the screen says so.

**15 Block screen.** Appears over the offending app. Block name, "Open <target>?", then *Not now* as the prominent filled action and *I'll do the challenge. Let me in* as the quieter outlined one. Carries the humour line and background image per §11. Must be comprehensible in under two seconds — no scrolling.

**16 Walk-away moment.** Confirmation, the running count ("That's the 4th time today"), then **auto-dismiss after 2 seconds** returning the user where they came from. Tappable to continue immediately.

**17–18 Typing challenge.** Passage displayed above, input below, live character count. Screen 18 shows the mistyped state with typo positions marked and the typed text kept. Escape action: *I'll walk away*.

**19 Delay countdown.** Timer, hold state, escape action *Never mind*. **No message is shown** — the calming line in the mock is removed.

**20 Floating bubble.** Draggable, shows remaining time and what is open. Tap returns to the app. Requires overlay permission; absent it, the bubble is simply not shown and everything else still works.

**21 Ongoing notification.** Non-dismissable for the duration of the pause, with a system countdown. Tapping opens the block's detail screen.

**22 Turn-off typing.** Same mechanism as 17, longer passage, framed as turning the block off. Escape action: *Leave it on*.

**23 Stats.** See §9.

**24 Settings.** Permission health · supported browsers · theme (static, Dark) · send feedback · about and version. The "Countdown messages — updated remotely" row is **removed**.

**25 Feedback.** Free-text field and *Send it*. Opens an email intent (§13). The "Attach a diagnostic log" toggle and its supporting copy are **removed** — no logs are collected.

## 7. Friction mechanisms

The user picks one per block at creation. **Minimum pause duration is 15 minutes** on both — a deliberate parity choice against the iOS constraint should that platform ever be built, not an Android limit.

### 7.1 Typing

A passage of random words is displayed; the user types it correctly.

| Control | Default | Range | Step |
|---|---|---|---|
| Pause duration | 15 min | 15–120 min | 5 min |
| Passage length — to pause | **100 chars** | 50–200 | 10 |
| Passage length — to edit or turn off | **300 chars** | 100–350 | 10 |

The third control is **not in the mock and must be added.** The mock shows a single "Passage length" of 150; the authoritative values are the table above.

Toggle: **Show where the typos are** (on by default). When on, mismatched positions are marked and the typed text is kept.

Length is measured in **characters, not words.** With autocorrect off and paste blocked, random words type at roughly 20 wpm on mobile — so 100 characters is around 30 seconds and 300 around two and a half minutes. A live estimate is shown as the user adjusts the control.

Hard requirements:

- **Paste is blocked** on the input field.
- **Autocorrect and predictive text are disabled.** Mobile keyboards would otherwise complete the random words and defeat the mechanism entirely.
- The passage is regenerated fresh on every attempt.
- Attempts are unlimited. The user corrects and resubmits; there is no lockout or penalty.

### 7.2 Delay

A countdown runs; the user waits.

| Control | Default | Range | Step |
|---|---|---|---|
| Pause duration | 15 min | 15–120 min | 5 min |
| Countdown length | 30 sec | 10 sec – 5 min | 5 sec |

**The phone must be held for the countdown to advance.** Detected from accelerometer variance plus tilt angle — a held phone carries constant micro-tremor, a phone on a table is still. Setting the phone down **pauses** the countdown; it does not reset it. The user accumulates the full duration of actual holding.

The requirement is stated on screen before the wait begins, and the screen makes clear at all times whether the timer is advancing. A stalled timer that looks broken is the highest-risk state in the app.

Hold detection applies to the countdown only, never to typing.

### 7.3 Interruption

Both mechanisms require the app foreground with the screen on.

| What the user does | What happens |
|---|---|
| Sets the phone down mid-countdown | Countdown **pauses**, resumes on pick-up. Delay only |
| Switches to another app | Challenge ends, **all progress resets to zero** |
| Taps the escape action | Challenge ends, progress resets, **counted as a walk-away** |
| Screen turns off | Treated as leaving; progress resets |

The screen is kept awake for the duration of a challenge, so a routine display timeout never destroys progress the user was legitimately earning. Only a deliberate power-button press ends it.

Elapsed time is measured **monotonically**, not against the wall clock, so changing the device clock cannot shorten a wait or a pause.

## 8. Walk-away accounting

A **walk-away** is recorded when the user declines to proceed by an explicit action:

- *Not now* on the block screen
- *I'll walk away* on the typing challenge
- *Never mind* on the delay countdown

**Abandonment is not a walk-away.** Switching apps, letting the screen sleep, or otherwise leaving without an explicit action records `challenge_abandoned` and nothing else.

The counter shown to the user is **global** across all blocks. Individual events are still stored **per app and per block**, because the in-the-moment context line ("you've opened this 11 times since 9am") and the Stats leaderboard both need that granularity.

## 9. Stats definitions

| Figure | Definition |
|---|---|
| Total walk-aways | All-time count of walk-aways as defined in §8 |
| Days active | Days since first launch |
| This week | Walk-aways in the last 7 calendar days |
| Best day | Highest single-day walk-away count, all time |
| Walk-away rate | `walk_aways ÷ (walk_aways + completed_challenges)` |
| Most walked away from | Per-app walk-away counts, descending |

## 10. Analytics

**Local only. No SDK, no network, no third party.** Events are written to a Room table and never leave the device.

The event store *is* the Stats data source — Stats is a query over it, not a parallel system. Event names are fixed now so a future export or upload is a loader change rather than a retrofit.

Schema: `event(id, name, timestamp_utc, block_id?, target?, params_json?)`

### Event list

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
| `block_create_step_completed` | `step` (1–4) |
| `block_create_abandoned` | `step` |
| `block_created` | `app_count`, `site_count`, `friction_type`, `pause_minutes`, `pause_chars`, `turnoff_chars`, `countdown_seconds` |
| `block_edited` | `block_id`, `fields_changed` |
| `block_deleted` | `block_id` |
| `block_conflict_shown` | `target`, `existing_block_id` |
| `block_conflict_resolved` | `resolution` (`moved` \| `kept`) |
| `block_turned_on` | `block_id` |
| `block_turned_off` | `block_id` |

**The interruption**

| Event | Params |
|---|---|
| `block_screen_shown` | `block_id`, `trigger_type` (`app` \| `site`), `target`, `latency_ms` |
| `walk_away` | `block_id`, `target`, `source` (`block_screen` \| `typing` \| `countdown`) |
| `challenge_started` | `block_id`, `type` (`typing` \| `delay`) |
| `challenge_completed` | `block_id`, `type`, `duration_ms`, `attempts` |
| `challenge_abandoned` | `block_id`, `type`, `progress_pct`, `reason` (`app_switch` \| `screen_off`) |
| `typing_mismatch` | `block_id`, `chars_typed` |
| `countdown_started` | `block_id`, `seconds` |
| `countdown_stalled` | `block_id`, `elapsed_ms` |
| `countdown_resumed` | `block_id` |
| `countdown_completed` | `block_id`, `wall_ms`, `held_ms` |

**Pause**

| Event | Params |
|---|---|
| `pause_started` | `block_id`, `minutes` |
| `pause_expired` | `block_id` |
| `bubble_shown` | `block_id` |
| `bubble_dragged` | — |
| `bubble_tapped` | `block_id` |

**Turn off**

| Event | Params |
|---|---|
| `turnoff_started` | `block_id`, `chars` |
| `turnoff_completed` | `block_id`, `duration_ms` |
| `turnoff_abandoned` | `block_id`, `progress_pct` |

**Utilities**

| Event | Params |
|---|---|
| `stats_viewed` | — |
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

Shown at random as the line on the block screen (§6, screen 15). Ten lines:

```
Touch grass.
Go drink water.
Nice try.
Not today.
Bro. No.
Look who it is.
Right on schedule.
Bold of you.
The algorithm can wait.
We meet again.
```

The first five are retained from CaffyBlock's `Store.TEXT_PRESETS`; the last five are new, written in the same dry register.

**Thirteen of the original eighteen were deliberately dropped** for violating P3 — they shame the user. Recorded here so they are not reintroduced: *How could you? · We talked about this. · Future you is watching. · You promised. · No. · That's a no from you. · Go do the thing. · This isn't the plan. · You're better than this. · Back to what matters. · Lmao, no. · Your goals called. They're disappointed. · This you?*

### Images and font

In `design/humor-assets/`:

- `sys_block_1.jpg` … `sys_block_6.jpg` — six images, sampled at random per block screen
- `anton.ttf` — condensed display face

Original source: `app/src/main/res/drawable/` and `res/font/` of the CaffyBlock project. In CaffyBlock these carried a full-bleed photo with "BLOCKED" in large red Anton over the top.

### How they combine with Nocturne

These two visual languages are opposed, so the merge is specified rather than left to judgement:

- **Nocturne owns structure, typography and controls.** Layout, buttons and hierarchy follow screen 15 exactly.
- The humour image is a **full-bleed background**, dimmed so the foreground text stays legible and the photo recedes into the dark ground — the effect Nocturne's `.lighten` treatment is built for.
- The humour string sits as the **line beneath the block name**.
- Anton is available for the block screen only. It is **not** used elsewhere in the app.

## 12. Permissions and onboarding

| Permission | Stated purpose | Required? |
|---|---|---|
| Accessibility | Tells us which app or site is open | **Yes** — nothing works without it |
| Display over other apps | Shows the timer bubble | **No** — only the bubble is lost |
| Battery exemption | Stops the phone shutting us down in the background | Strongly recommended |
| Notifications | Shows the pause countdown | Recommended |

Presented as a persistent, resumable checklist with two states per row and a progress indicator. Reused later as the Settings permission-health view.

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
| **Local Room event store** | Powers Stats and analytics from one table. No network, no privacy surface, no SDK — and no extra cost, because Stats needs the data anyway. |
| **Browser map and OEM text as a bundled JSON asset** | Remote Config was considered and deferred. It earns its place only when there are store users who cannot be reached; with a handful of testers on App Distribution, pushing a build does the same job sooner. **The loader must be abstracted so switching to Remote Config later is a loader change and nothing else.** |
| **Feedback by email intent** | No backend exists. Opens the device mail client to `arjranaprep@gmail.com`, subject "Feedback from user", body prefilled with the typed text. No logs, no attachments, no collection. |
| **Dark theme only** | Nocturne ships no light tokens, and the product is used at night more than at any other time. |
| **Firebase App Distribution for test builds** | Console upload only — no SDK in the app, so it carries no code dependency. |

### Supported browsers

Website blocking reads the address bar via the accessibility node tree. Nine browsers, shipped as bundled JSON:

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

| Asset | Location | Authority |
|---|---|---|
| Design tokens | `toki-shrine-ui-mockups/project/_ds/nocturne-*/styles.css` | **Canonical.** Every colour, font, spacing, radius and shadow comes from these variables. Never hard-code a value the tokens carry. |
| Design language rules | `toki-shrine-ui-mockups/project/_ds/nocturne-*/readme.md` | Rules to follow — outlined buttons never filled, no pure black or white, Phosphor icons, 0.7× density |
| Screen renders | `design/screens/` | Layout and arrangement reference, 27 PNGs |
| Source markup | `toki-shrine-ui-mockups/project/TimeShrine Mocks.dc.html` | Exact values, structure and copy |
| Humour assets | `design/humor-assets/` | Six images plus Anton |
| Screen regeneration | `design/regenerate-screens.py` | Rebuilds the PNGs if the design export changes |

Nocturne is a **web** design system. Its tokens translate to a Compose theme; its CSS component layer (`.btn`, `.card`, `mix-blend-mode`) does not and should be ignored. Icons are Phosphor.

The `.dc.html` is a prototype rendering, not production code. It is the source of truth for values, copy and layout — never for implementation structure.

## 16. Deferred

**Phase 2:** uninstall protection (gated on user feedback) · escalating friction difficulty · re-arm prompt after turning a block off · block schedules · shareable achievements screen · NFC tag challenge · Remote Config · remote analytics export · pay-per-unlock.

**Later:** in-app surface blocking (Shorts, Reels) · iOS · the separate hard-block product.

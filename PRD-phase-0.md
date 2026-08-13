# TimeShrine — Phase 0 PRD

Early working document. Expected to be revised after design mockups. Text only, no design decisions.

---

## 1. What this is

An Android app that puts deliberate friction in front of the apps and websites a person doomscrolls. It does not stop them. It makes them pay a small, self-chosen cost each time they want in.

Built first for personal use and a small group of friends. App store release is a later goal.

## 2. Positioning

Anti-brainrot, not productivity. The product is about endless scrolling and endless watching — not time management, not focus sessions, not parental control.

**Core principle: this app never hard-blocks.** The user can always get in. The product's job is to make that a conscious decision rather than an unconscious one. A hard-block product is explicitly out of scope and would be a separate app.

**Secondary principle: backing out should be the easier, better-feeling path.** The two seconds where the user decides is the whole product.

## 3. Scope

**In, phase 1**

- Creating and managing blocks
- Blocking Android apps
- Blocking websites at whole-domain level, in a fixed set of supported browsers
- Two friction mechanisms: typing and delay
- Timed pause, auto re-arm
- Turning a block off via typing
- Floating bubble + ongoing notification during a pause
- Walk-away counter and in-the-moment context
- Permission onboarding

**Explicitly out, phase 1**

Uninstall protection · block schedules · escalating friction difficulty · extending a pause · blocking specific surfaces inside an app (Shorts, Reels) · URL/path-level blocking · in-app browser (webview) coverage · shareable achievements screen · NFC challenges · monetisation · backend · iOS

## 4. Vocabulary

| Term | Meaning |
|---|---|
| **Block** | A named container of apps and websites, with its own friction settings |
| **ON** | The block is enforcing |
| **OFF** | The block exists but is not enforcing |
| **Pause** | The block is temporarily open after the user passed its friction challenge |
| **Re-arm** | The pause expires and the block returns to ON, automatically |
| **Friction mechanism** | The challenge required to pause a block |

## 5. The block model

A block contains apps and websites. An app or website belongs to **exactly one block**. Adding something already in another block prompts the user to confirm the move; if they decline, they stay in the editor until it's resolved.

TimeShrine itself cannot be added to a block. It is filtered out of the picker, since blocking it would lock the user out of the only screen that can undo it.

Blocks are independent of one another. Any number can be paused, or turned off, at the same time.

Pausing a block opens **everything inside it**. There is no per-app pause.

### Creating a block

1. Add apps and websites
2. Name it
3. Configure the friction mechanism, including the pause duration
4. Save

A newly created block lands in the block list in the **OFF** state. It does nothing until the user turns it on.

### Turning a block ON

The user turns it on manually. Before it takes effect, they are shown a confirmation that states what they are agreeing to: what it will take to pause the block, and what it will take to turn it off. This is the commitment moment.

If the required permissions have not been granted, this is where the app blocks and points at the permission checklist. The user is never gated before this point — they can install, explore, and build blocks freely.

### Editing and deleting

**A block must be OFF before it can be edited or deleted.** Otherwise removing an app from a block is a free bypass.

### Turning a block OFF

Requires typing random words (see 6.1), defaulting to 350 characters. Configured per block at creation, and deliberately heavier than that block's pause gate.

Once OFF, the block **stays OFF until the user turns it back on**. It does not re-arm on its own.

## 6. Friction mechanisms

Both are modelled on Cold Turkey. The user picks one per block at creation.

**Minimum pause duration is 15 minutes** on both mechanisms. Android has no such limit; this is set deliberately for parity with the iOS constraint if that platform is built later.

### 6.1 Typing

The user is shown a passage of random words and must type it correctly.

Configurable:

- **Pause duration** — how long the block stays open after success
- **Length of the passage, in characters** — default 150 for pausing a block, 350 for turning one off
- **Show or hide typo positions** when the text doesn't match
- **Keep or clear** what they typed when it doesn't match

Length is measured in **characters, not words**. On a phone, with autocorrect off and paste blocked, random words type at roughly 20 WPM — so 150 characters is around 40 seconds and 350 characters around two and a half minutes. Word counts in the same range would run to ten minutes or more.

While configuring, show a live estimate of how long the passage will take to type. Users have no intuition for this on mobile, and the estimate is what makes the commitment informed.

Functional requirements:

- **Paste must be blocked** on the input field
- **Autocorrect and predictive text must be disabled** — mobile keyboards will otherwise complete random words and defeat the mechanism
- Text is regenerated fresh each attempt
- Attempts are unlimited. The user corrects and resubmits; there is no lockout or penalty for getting it wrong

The same mechanism, separately configured per block, is used for turning a block OFF.

### 6.2 Delay

A countdown runs and the user waits for it to finish. Nothing to do but wait and watch.

Configurable:

- **Pause duration**
- **Countdown length** — a number plus a unit (seconds or minutes)

**The phone must be held for the countdown to advance.** Detected from accelerometer variance plus tilt angle — a held phone carries constant micro-tremor, a phone on a table is dead still. Setting the phone down **pauses** the countdown; it does not reset it. The user has to accumulate the full duration of actual holding.

The waiting screen states this requirement plainly before the wait begins, and makes clear at all times whether the timer is advancing. The user should never be confused about why a countdown has stalled.

A short message is shown while the countdown runs, drawn at random from a set. Tone is non-judgmental and leaves the choice open — this is a friction app, not a guilt app. Starting set:

- No rush. The feed will still be there.
- Nothing to do here. That's the point.
- Past you set this up. Present you gets to decide.
- If you still want in when this ends, that's a fine answer.
- Urges usually pass quicker than you'd think.
- Notice what you came for. It's okay if it's nothing.
- Take a breath. Then choose.

These live in remote config so the set can be edited without an app release. Register is deliberately varied — a single tone becomes wallpaper when seen several times a day.

### 6.3 Interruption

Both mechanisms require the app to be in the foreground with the screen on.

| What the user does | What happens |
|---|---|
| Sets the phone down, still on the challenge screen | Countdown **pauses**. Resumes when picked up again. Delay mechanism only |
| Switches to another app | Challenge ends. **All progress resets to zero** |
| Taps the walk-away CTA | Challenge ends, screen closes, progress resets to zero. Counts as a walk-away |
| Screen turns off | Treated as leaving. Progress resets to zero |

Being able to background a countdown and wait it out elsewhere would remove the friction entirely, which is why leaving is unforgiving while setting the phone down is not.

The screen is kept awake for the duration of a challenge, so that a routine display timeout never destroys progress the user was legitimately earning. Only a deliberate power-button press ends it.

Elapsed time is measured monotonically rather than against the wall clock, so changing the device clock cannot shorten a wait or a pause.

## 7. Hitting a block

When the user opens a blocked app, or navigates to a blocked domain in a supported browser, the app interrupts and presents two choices:

- **Back out** — returns them to where they were. The prominent, easy path.
- **Continue** — starts the friction challenge

On backing out: a short animation and haptic, a plain congratulatory message, and honest in-the-moment context ("4th time today", "you've opened this 11 times since 9am").

The **walk-away counter shown to the user is global** — every walk-away counts towards one number, regardless of which block or app it came from. Individual walk-away events are still recorded per app, because the in-the-moment context needs per-app data. Global counter, per-app events.

On passing the challenge: the block enters **Pause** for its configured duration.

## 8. During a pause

Everything in the block is accessible. Two indicators run simultaneously:

- A **draggable floating bubble** with the remaining time, on top of whatever the user is doing
- An **ongoing notification** with a countdown, which cannot be swiped away

When the timer expires, the block re-arms automatically and immediately, with no warning beforehand. The bubble and the notification have been counting down in plain sight the whole time. The pause cannot be extended.

More than one block can be paused simultaneously. The bubble shows the soonest expiry, with an indication when others are running.

## 9. Permissions

Four, presented as a persistent checklist with two states per item — pending and granted — plus a progress indicator. Each item states in plain language what it's for and why. The same component is reusable later as a health screen.

| Permission | Stated purpose |
|---|---|
| Accessibility | Tell which app or website is open |
| Display over other apps | Show the block screen and the floating bubble |
| Battery exemption | Stop the phone shutting the app down in the background |
| Notifications | Show the pause timer |

Before the accessibility request, a short screen pre-empts Android's alarming system warning and states honestly what the app does and does not read: it checks which app is in front and reads the address bar in supported browsers. It does not read page contents, does not log browsing, and nothing leaves the phone.

The battery item needs manufacturer-specific instructions. The universal Android battery-exemption dialog is the baseline for every device; brand-specific extra instructions are added only for brands actually owned and tested by the team.

Sideloaded builds additionally require Android's "restricted settings" to be unlocked before accessibility can be granted. This is delivered as text shared with testers, not as an in-app step, and disappears if the app is later distributed via Play.

## 10. Technical approach

- **Native Kotlin.** The core is entirely platform API; a cross-platform framework would only help with the easy part.
- **AccessibilityService** detects the foreground app and reads the browser address bar. Filtered at runtime to only blocked apps and supported browsers.
- The **browser→address-bar mapping** and the **OEM battery instructions** both live in remote config, so either can be fixed without an app release.
- All data local. No backend.
- No VPN, no usage-stats polling.

**Known limitation:** websites are only blocked in a fixed set of supported browsers. Unsupported browsers and in-app browsers are not covered. This is stated plainly to users rather than hidden.

## 11. Distribution

Debug builds over USB during development. Firebase App Distribution for the friends group. No Play Store release in phase 1.

---

*Nothing open. Phase 1 scope is settled.*

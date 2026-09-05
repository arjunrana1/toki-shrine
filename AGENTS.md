# AGENTS.md — Toki Shrine build instructions

**Read this file first, in full, before doing anything.** It is the entry point. Everything else is referenced from here.

You are building **Toki Shrine**, a native Android app. This document tells you what to read, what to build, in what order, and how each phase is verified.

---

## 1. Read these before writing code

| File | What it is | How to treat it |
|---|---|---|
| `PRD.md` | Full product specification — flows, rules, screens, event list | **The authority on behaviour.** If anything here contradicts it, `PRD.md` wins. |
| `toki-shrine-ui-mockups/project/_ds/nocturne-*/styles.css` | The Nocturne design tokens | **The authority on every visual value.** Colours, fonts, spacing, radii, shadows. |
| `toki-shrine-ui-mockups/project/_ds/nocturne-*/readme.md` | The design-language rules | Read it. It defines rules you must follow — outlined buttons never filled, no pure black or white, Phosphor icons, 0.7× density. |
| `design/screens/*.png` | 27 rendered screens | Layout, arrangement and visual hierarchy reference. |
| `toki-shrine-ui-mockups/project/TimeShrine Mocks.dc.html` | The source markup behind those renders | Exact values, copy and structure. Read the section for a screen before building it. |
| `design/humor-assets/` | Six images and `anton.ttf` | Block-screen assets. See `PRD.md` §11. |
| `DECISIONS.md` | Running log of decisions made during the build | Read at the start of every session. Append to it. |

**Naming:** the product is **Toki Shrine**. The design export filename still says "TimeShrine" and a few strings inside it do too — those are stale. Use Toki Shrine everywhere.

## 2. Stack

| | |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose |
| minSdk | 33 (Android 13) |
| targetSdk | 36 |
| applicationId | `com.arjunrana.tokishrine` |
| Persistence | Room |
| Dependencies | **None beyond AndroidX and Compose.** No Firebase, no analytics SDK, no networking library. |
| Theme | Dark only |
| Icons | Phosphor |

## 2a. Build and device

The toolchain is already installed and wired up. **Android Studio is not installed and is not needed.**

Always build with `./build.sh` — it sets `JAVA_HOME` and the SDK path for you:

```bash
./build.sh assembleDebug
```

For `adb` and other SDK tools, source the environment first:

```bash
source tools/env.sh
```

The Gradle wrapper (8.7), `local.properties` and `gradle.properties` are already committed and correct. Do not regenerate them and do not change the Gradle or Kotlin version without asking.

The test device is a Samsung Galaxy S23 Ultra on Android 16, connected over USB.

**Three device facts that will otherwise waste your time:**

1. **`adb` fails with "more than one device/emulator" if wireless debugging is also on.** If you see that, stop and ask the user to turn wireless debugging off rather than guessing at a serial.
2. **Reinstalling the app disables its accessibility service.** After every `adb install`, re-enable it before running any detection test:
   ```bash
   adb shell settings put secure enabled_accessibility_services com.arjunrana.tokishrine/com.arjunrana.tokishrine.TokiAccessibilityService
   adb shell settings put secure accessibility_enabled 1
   ```
   Confirm it actually bound with `adb shell dumpsys accessibility | grep "label=Toki Shrine"` — the setting can read as enabled while the service is not yet bound, and binding is asynchronous. Wait for the bind before asserting a detection test failed.
3. **Any other blocker app installed on the device will fight yours.** If a detection test behaves strangely, check `adb shell settings get secure enabled_accessibility_services` for a second blocking service before debugging your own code.

## 3. Workflow rules

These are not suggestions.

1. **Build only what the current phase scopes.** Do not run ahead, do not "while I'm here" adjacent code, do not stub future phases.
2. **Stop at each phase boundary.** Run the acceptance tests, commit, and report before starting the next phase.
3. **Commit after every phase** with a descriptive message naming the phase and what landed. Never bundle two phases into one commit.
4. **Take every visual value from the Nocturne tokens.** Never hard-code a colour, font, spacing, radius or shadow that `styles.css` already carries. Translate the tokens once into the Compose theme, then reference the theme everywhere.
5. **If anything is ambiguous, stop and ask.** Do not guess and do not invent product behaviour. A wrong guess costs more than a question.
6. **Append every decision you make mid-build to `DECISIONS.md`** — what you decided, why, and which phase. This includes anything you resolved without asking. Decisions must survive across sessions.
7. **Never weaken an acceptance test to make it pass.** If a test cannot pass, stop and report why.
8. Do not add dependencies without asking.

## 4. Phases

Each phase ends with a commit and a verified test checkpoint. Every pass condition below is objectively checkable — a specific command output or a specific observable result.

---

### Phase 0 — Project skeleton and Nocturne theme

Gradle project, Compose, Room dependency, and the Nocturne tokens translated into a Compose theme (colours, type scale, spacing, shapes, elevation). App launches to an empty themed screen.

**Acceptance**

- `./gradlew assembleDebug` exits 0.
- `adb install -r app/build/outputs/apk/debug/app-debug.apk` prints `Success`.
- Launching the app shows a screen whose background is `#161826` (verify by screenshot).
- `grep -rE "#[0-9a-fA-F]{6}" app/src/main/java --include=*.kt | grep -v ui/theme/` returns **no matches**.
- The theme defines every token in `styles.css` `:root`: `--color-bg`, `--color-surface`, `--color-text`, `--color-accent`, all nine steps of `--color-neutral-*` and `--color-accent-*`, six `--space-*`, three `--radius-*`, three `--shadow-*`.

---

### Phase 1 — Data layer

Room entities, DAOs and repositories for blocks and events. No UI.

Blocks carry: id, name, apps, sites, friction type, pause minutes, pause chars, turn-off chars, countdown seconds, show-typos flag, enabled state. Events per `PRD.md` §10 schema.

**Acceptance**

- Instrumented test: insert a block, read it back, all fields match.
- Instrumented test: inserting an app already present in another block is rejected or flagged by the repository — it never silently duplicates.
- Instrumented test: insert 100 events, query walk-aways for the last 7 days, count is correct.
- Instrumented test: the Stats formulas in `PRD.md` §9 return correct values against a seeded fixture, including walk-away rate.
- `./gradlew testDebugUnitTest connectedDebugAndroidTest` exits 0.

---

### Phase 2 — Block list and create flow

Screens 5–14: block list (empty and populated), the four create steps, app search, conflict dialog, block detail and edit.

**Acceptance**

- A block created through the UI appears in the list with its toggle **off**.
- The create flow shows `1 / 4` through `4 / 4` and the progress bar advances one segment per step.
- Screen 10 renders **three** numeric controls: pause duration, pause passage length, turn-off passage length. Defaults 15 min, 100 chars, 300 chars.
- Adjusting pause passage length updates the live time estimate; at 100 characters it reads approximately 30 seconds.
- Adding an app that already belongs to another block opens the conflict dialog naming that block. *Leave it where it is* returns to the editor with the app not added.
- With a block ON, the edit action is unavailable and the detail screen states the block must be off first.
- Toki Shrine itself does not appear in the app picker.

---

### Phase 3 — Onboarding and permissions

Screens 1–4 and 24. Welcome, the four-row checklist, accessibility explainer, manufacturer battery instructions, settings.

**Acceptance**

- The checklist shows exactly four rows, each in one of two states only: pending or granted.
- Granting a permission and returning to the app flips that row to granted without a manual refresh.
- The progress indicator reads `n of 4` and matches the number of granted rows.
- The accessibility explainer appears **before** the system permission screen is opened.
- On a Samsung device the battery screen shows Samsung instructions; the "Not a Samsung? Pick your phone" control opens a manufacturer picker.
- With accessibility not granted, tapping a block's ON toggle does **not** enable the block and routes to the checklist.
- With permissions absent, the app still opens, navigates and creates blocks — no gate before the ON toggle.

---

### Phase 4 — Detection engine

`AccessibilityService`, app detection, browser address-bar reading, and the bundled JSON loader for the browser map and OEM text. Block screen is a **placeholder** in this phase — plain text naming the trigger.

Implement the four behaviours in `PRD.md` §13: never act while the address bar is focused, ignore values containing spaces, apply a settle delay before blocking a site, and cache the last known URL per window.

**Acceptance**

- `adb shell dumpsys accessibility | grep "label=Toki Shrine"` shows the service under `Bound services`.
- With a block containing Instagram switched ON, launching Instagram shows the placeholder. `adb shell dumpsys window | grep mCurrentFocus` reports the app's block activity **within 1 second**.
- With `reddit.com` blocked, opening it in Chrome shows the placeholder within 3 seconds.
- Typing into Chrome's address bar without navigating does **not** trigger the placeholder.
- Entering a search query containing spaces does not trigger it.
- `old.reddit.com` triggers a block on `reddit.com`; `notreddit.com` does not.
- Opening a blocked site in a browser absent from the map does not trigger it.
- The browser map is loaded from a JSON asset, not hard-coded in Kotlin. Behind an interface with one implementation, so a remote loader can replace it later.

---

### Phase 5 — The interruption

Screens 15–19 and 22. Real block screen with humour assets, walk-away moment, typing challenge and mistyped state, delay countdown with hold detection, turn-off typing.

**Acceptance**

- Block screen: the walk-away is the filled prominent button; the way in is the outlined secondary. Verify against `design/screens/15-block-screen.png`.
- The humour line is drawn from the ten strings in `PRD.md` §11 and changes between showings.
- The background image is one of `sys_block_1–6.jpg`, dimmed such that foreground text remains legible.
- Long-pressing the typing input offers **no Paste option**.
- The typing field reports no autocorrect or predictive suggestions while typing random words.
- Typing a wrong character with show-typos on marks the position and keeps the typed text.
- Completing the passage exactly advances; a mismatch does not, and can be retried without limit.
- Tapping *I'll walk away* or *Never mind* records a `walk_away` event; switching apps mid-challenge records `challenge_abandoned` and **no** `walk_away`.
- Switching away mid-countdown and returning restarts the countdown at zero.
- Placing the phone flat and still stalls the countdown; the screen indicates it is not advancing; picking it up resumes from where it stalled, not from zero.
- The screen does not sleep during a challenge — leave it untouched for twice the device display timeout and the challenge is still live.
- Walk-away screen auto-dismisses after 2 seconds and shows the correct daily count.

---

### Phase 6 — Pause lifecycle

Pause timer, monotonic timing, automatic re-arm, floating bubble, ongoing notification.

**Acceptance**

- Completing a challenge opens every app and site in that block, and no others.
- The bubble appears when overlay permission is granted, is draggable, and shows remaining time.
- With overlay permission **denied**, the pause still works and everything except the bubble functions.
- The notification shows a live countdown and cannot be swiped away for the pause duration.
- Setting the device clock forward during a pause does **not** shorten it (`adb shell date` to verify).
- When the timer expires the block re-arms immediately with no warning, and reopening the app shows the block screen again.
- Two blocks paused simultaneously both work; the bubble shows the soonest expiry.

---

### Phase 7 — Stats, feedback and final pass

Screens 23 and 25, plus a full pass over every screen against its render.

**Acceptance**

- Stats figures match the definitions in `PRD.md` §9 against real recorded events.
- Walk-away rate equals `walk_aways ÷ (walk_aways + completed_challenges)`.
- The per-app leaderboard is ordered descending by walk-away count.
- *Send it* opens the mail client to `arjranaprep@gmail.com` with subject `Feedback from user` and the typed text in the body.
- No "attach diagnostic log" control exists anywhere.
- Every event in `PRD.md` §10 fires at least once across a full manual run-through, verified by querying the event table.
- `grep -rE "#[0-9a-fA-F]{6}" app/src/main/java --include=*.kt | grep -v ui/theme/` still returns no matches.
- No network permission in the manifest, and no third-party dependency in `app/build.gradle.kts` beyond AndroidX, Compose and Room.

---

## 5. If you get stuck

Stop. Write the question in `DECISIONS.md` under an **Open questions** heading, report it, and wait. Do not guess at product behaviour, do not silently skip an acceptance test, and do not implement a phase you have not been asked for.

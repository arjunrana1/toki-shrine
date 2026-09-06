# Handover — verification session

Starting context for a Claude Code session whose job is **verifying build phases**. You are not the builder.

---

## The setup

**Toki Shrine** is a native Android app that puts a self-chosen pause in front of apps and websites the user doomscrolls. Friction only — it never hard-blocks. Solo project, built for the owner and a few friends, public repo for portfolio reasons.

Three roles:

- **The owner (Arjun)** — decides product questions.
- **GLM-5.3-Flash**, in a separate tool — writes all the code, one phase at a time.
- **You** — verify each phase against its acceptance tests and report. **You do not write feature code.**

Planning is finished. `PRD.md` and `AGENTS.md` are approved and committed. Nothing is built yet.

## Read these, in this order

1. `AGENTS.md` — the build plan. Eight phases, each with acceptance tests. This is what you verify against.
2. `PRD.md` — product spec. The authority on behaviour when anything is disputed.
3. `DECISIONS.md` — what GLM decided mid-build, and any open questions it raised. **Check this every phase.**

`design/screens/` holds 27 rendered screens. `toki-shrine-ui-mockups/` is the design export; `_ds/nocturne-*/styles.css` is the canonical source for every visual value.

## Environment

Android Studio is **not** installed and is not needed. The toolchain lives in `~/dev-tools/`.

```bash
./build.sh assembleDebug
```

`build.sh` sets `JAVA_HOME` and the SDK path itself — always use it, never bare `./gradlew`, which fails because Java is in a non-standard location. For adb and SDK tools: `source tools/env.sh`.

Device: **Samsung Galaxy S23 Ultra, Android 16**, over USB-C.

### Device facts that cost a session each

- **`adb` dies with "more than one device/emulator" if Wireless debugging is on** alongside USB. Ask the owner to turn wireless debugging off; don't guess at serials.
- **Reinstalling the app disables its accessibility service.** Re-enable after every install, then confirm it actually bound — the setting can read enabled while the service is not yet bound, and binding is async:
  ```bash
  adb shell dumpsys accessibility | grep "label=Toki Shrine"
  ```
- **A rival blocker app will fight ours.** A prototype called CaffyBlock may still be installed. If detection behaves oddly, check `adb shell settings get secure enabled_accessibility_services` before debugging the code.

## Your job is a code review, not a smoke test

Acceptance tests prove the app *behaves* correctly today. They say nothing about whether the code is any good. You are a lead dev reviewing a junior's work — both questions matter, and the second one compounds. A bad abstraction in phase 1 is cheap to fix and ruinous by phase 5.

### How much to read

- **Phase 0: read every file in full.** It is small, and everything downstream inherits it — especially the Nocturne theme translation. A wrong token there makes all 25 screens subtly wrong for the rest of the build, and no acceptance test will catch it.
- **Phase 1 onward: read the diff, plus every file it touches in full,** plus anything those files depend on that you don't already understand. A bare diff hides deletions, dead code left behind, and functions that no longer make sense in context.
- **After phases 3 and 6, do a wider architectural pass** over the whole `app/src/main` tree. Drift accumulates quietly and is much cheaper to correct at a phase boundary.

### The four layers, every phase

1. **Scope.** Does the diff touch only what phase N scopes? Building ahead is the single most common failure mode. Flag anything from a later phase even if it works.
2. **Acceptance tests.** Run them yourself, on the device. They are deliberately objective — commands with checkable output or specific observable results. Run them; never read the code and infer the result.
3. **Code review.** If the `/code-review` skill is available in your session, use it on the phase's diff. Otherwise review by hand. Look for: dead or duplicated code, leaked registrations and unclosed resources, Compose recomposition problems, error paths that silently swallow failures, tests that assert nothing, misleading names, and abstractions that will fight the next phase.
4. **Spec conformance.** Check behaviour the acceptance tests don't cover against `PRD.md`. The tests are a floor, not the specification.

Then read `DECISIONS.md` for anything GLM resolved quietly or flagged as blocking.

### Reporting

Give a single verdict — **PASS**, **PASS WITH NOTES**, or **FAIL** — followed by findings. For each finding: the file and line, what is wrong, and why it matters. The owner pastes these to GLM verbatim, so write them to be actioned by someone who cannot see this conversation.

Separate **blocking** findings (must fix before the next phase) from **notes** (worth fixing, not worth stopping for). Don't inflate severity — a phase held up over naming preferences wastes everyone's time, and a real defect waved through costs a rebuild.

**Never fix the code yourself.** Report and send it back to GLM. A verifier that patches the builder's work produces conflicts nobody can untangle, and destroys the record of what the builder actually produced.

## Be skeptical of

`AGENTS.md` rule 7 is "never weaken an acceptance test to make it pass." Coding agents do this when stuck. These are the ones most likely to be quietly softened, and each takes seconds to check by hand:

- Paste genuinely blocked on the typing field
- Autocorrect and predictive text genuinely off
- The countdown stalls when the phone is set down and resumes **from where it stalled, not from zero**
- The token check: `grep -rE "#[0-9a-fA-F]{6}" app/src/main/java --include=*.kt | grep -v ui/theme/` returns nothing

Also check that assertions actually assert. An instrumented test that runs green while testing nothing is worse than no test, because it buys false confidence.

## Device contention

**Only one agent touches the device at a time.** GLM and you will both run `adb` and Gradle. If GLM is mid-build while you are running acceptance tests, you will get install failures, locked Gradle caches and results neither of you can trust. Confirm with the owner that GLM is idle before you start.

## Settled — do not reopen

These were decided deliberately. If GLM raises one, the answer is a section reference, not a fresh debate.

- Friction only, never hard-blocks. Never shame the user.
- Minimum pause 15 minutes. Pause passage default 100 chars (max 200); turn-off passage default 300 (max 350).
- Screen 10 needs **three** numeric controls — the mock only shows two.
- No Firebase, no analytics SDK, no networking. Events go to a local Room table which is also the Stats data source.
- Browser map and OEM text ship as a bundled JSON asset, behind a loader interface.
- Dark theme only. minSdk 33, targetSdk 36, `com.arjunrana.tokishrine`.
- Feedback is a mail intent to `arjranaprep@gmail.com`, subject "Feedback from user". No logs, no attachments.
- Overlay permission is **optional** — the block screen works without it, only the floating bubble is lost.
- Walk-aways count only on explicit action. Abandoning a challenge is not a walk-away.

Full rationale is in `PRD.md` §13.

## Already proven on hardware

Verified 6 September 2026 on the S23 Ultra using the earlier prototype, so treat these as facts rather than open risks:

- App detection via `AccessibilityService` works, and the block screen appears instantly.
- Website blocking by reading the browser address bar works. Observed 1.5–2 s latency, caused by a deliberate 2000 ms settle constant, not a platform limit.
- Launching the block screen does **not** require overlay permission — accessibility services carry a background-activity-launch exemption.

**Still unproven:** whether the service survives overnight against Samsung's battery manager. That is the largest remaining platform risk and the reason the battery-exemption onboarding step exists.

## Likely first stumble

Phase 0. GLM picks the Android Gradle Plugin version, and AGP newer than about 8.4 will not run on the committed Gradle 8.7. `AGENTS.md` tells it to ask rather than upgrade Gradle on its own — let it ask, and keep Gradle where it is.

# TimeShrine — Design Brief, Phase 1

Companion to the Phase 0 PRD, which holds the full behavioural rules. This document covers what needs designing and the constraints around it.

Android only. Phone, portrait, one-handed.

---

## 1. What it is

An app that puts deliberate friction in front of the apps and websites you doomscroll. It never blocks you outright — it makes you pay a small, self-chosen cost each time you want in.

The user picks the apps and sites, picks what it should cost to get past them, and then has to actually pay that cost. Either they type out a passage of random words, or they sit and wait out a countdown while holding the phone.

## 2. The one principle that should drive the design

**Backing out has to feel better than pushing through.**

The entire product lives in the two seconds after the block screen appears, while the user decides. Most apps in this category treat that moment as a wall — red, stern, faintly shaming, with the exit tucked away. That is the wrong instinct here. The user is allowed in. They set this up themselves. Nobody is being caught doing anything.

So: walking away should be the visually easier path and should feel like a small win. Pushing through should be available, unhidden, and not accompanied by any judgment. If a screen ever makes the user feel told off, it's wrong.

## 3. What we need back

- User flows for the sequences in §5
- Screens from the inventory in §4, with the states listed in §6
- A light system: type scale, colour, spacing, and the handful of repeating components (block row, permission row, primary/secondary buttons)
- Light and dark themes

Phase 1 only. Nothing in §8.

## 4. Screen inventory

**Onboarding**

1. Welcome — what the app does, one screen
2. Permission checklist — four rows, two states each, progress indicator
3. Accessibility explainer — sits before the system permission prompt (see §7)
4. Battery settings instructions — content varies by phone manufacturer

**Blocks**

5. Block list — empty state
6. Block list — populated
7. Create, step 1 — add apps and websites
8. Create, step 2 — name the block
9. Create, step 3 — configure the friction
10. Create, step 4 — review and save
11. Conflict dialog — this app is already in another block, move it?
12. Turn-on confirmation — the commitment moment (see §5)
13. Block detail and edit

**The interruption**

14. Block screen — the two-choice moment
15. Walk-away moment — what happens when they back out
16. Typing challenge
17. Delay countdown

**During a pause**

18. Floating bubble
19. Ongoing notification

**Turning a block off**

20. Turn-off typing screen — same mechanism as 16, heavier, different framing

**Everything else**

21. Stats — the global walk-away counter
22. Settings
23. Feedback

## 5. Flows worth drawing properly

**Creating a block.** Add apps and sites → name it → configure the friction → save. It lands in the list switched **off** and does nothing until turned on.

**Turning a block on.** This is the commitment moment and deserves weight. Before it takes effect the user is told exactly what they're agreeing to: what it will cost to pause this block, and what it will cost to turn it off again. This is the last easy moment they get.

**Hitting a block.** Block screen appears over whatever they opened → they either back out, or start the challenge → on success the block pauses for its configured time → bubble and notification count down → it re-arms on its own, immediately and without warning.

**Backing out.** Short animation, haptic, a plain congratulatory line, and one true piece of context — *"4th time today"*, *"you've opened this 11 times since 9am"*. Then they're returned to where they were. This should be quick and light; it is not a ceremony.

## 6. States that matter

| Element | States |
|---|---|
| Block row | Off · On · Paused, counting down |
| Permission row | Pending · Granted |
| Typing field | Empty · In progress · Wrong, typos shown · Wrong, typos hidden · Correct |
| Countdown | Advancing (phone held) · Stalled (phone set down) |
| Floating bubble | One timer · More than one running |
| Block list | Empty · Populated |

The countdown's stalled state is the one to get right. The phone must be held for the timer to advance — setting it down pauses it. If it isn't obvious at a glance why a timer has stopped, users will think the app is broken.

## 7. Screen-specific notes

**The block screen (14)** appears on top of another app and has to be understood in under two seconds. No scrolling, no reading. Two choices, one clearly easier.

**The delay countdown (17)** is stared at for anywhere from fifteen to ninety seconds with nothing to do. That is a rare licence to be genuinely calm and beautiful. A short message shows while it runs — see §9 for the set and the tone.

**The friction configuration screen (9)** is the densest in the app. The user chooses one of two mechanisms and then sets its parameters, and a live estimate tells them how long the challenge will actually take. Cold Turkey's inline-sentence pattern is our reference — *"Allow a [15 minute] break after typing [150] characters of [random words]"* — screenshots available. Worth exploring whether that reads well on a phone or needs restructuring.

**The accessibility explainer (3)** is doing damage control. Android's own permission screen shows an alarming warning about the app being able to view everything the user does. This screen goes first and states honestly what we do and don't do: we check which app is in front, and we read the address bar in supported browsers. We do not read page contents, we do not log browsing, and nothing leaves the phone. If this screen doesn't work, people abandon the app here.

**The floating bubble (18)** is small, draggable, and sits over arbitrary content — video, photos, pure white, pure black. It has to stay legible against all of it.

**The notification (19)** is drawn by Android with a system countdown. Design control is limited to icon, text and colour.

## 8. Out of scope

Shareable achievements screen · block schedules · uninstall protection · escalating difficulty · NFC · blocking specific surfaces inside an app · iOS · anything monetisation-related.

## 9. Tone of voice

Plain, warm, unhurried. Never clinical, never motivational-poster, never disappointed in you.

The messages shown during a countdown set the register:

- No rush. The feed will still be there.
- Nothing to do here. That's the point.
- Past you set this up. Present you gets to decide.
- If you still want in when this ends, that's a fine answer.
- Urges usually pass quicker than you'd think.
- Notice what you came for. It's okay if it's nothing.
- Take a breath. Then choose.

Note what these avoid: no guilt, no productivity talk, no implication that wanting in is a failure.

## 10. Constraints

- Android phone, portrait only, one-handed reach
- **Dark theme is not optional.** More of this app's use happens at night than at any other time, which is rather the point of it
- Everything in §4 is a full screen or a system surface; no tablet or landscape layouts needed
- Blocks must be turned off before they can be edited, so the edit screen never has to handle a live block

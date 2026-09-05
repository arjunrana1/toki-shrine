# Toki Shrine

An Android app that puts a deliberate, self-chosen pause in front of the apps and websites you doomscroll.

It never blocks you out. You pick what to slow down, you pick what it should cost to get in, and then you pay that cost — either typing out a passage of random words or sitting through a countdown while holding the phone. If you still want in afterwards, that's a fine answer.

Deliberately friction-only. A hard blocker is a different product.

## Documents

| File | What's in it |
|---|---|
| [AGENTS.md](AGENTS.md) | Build instructions — read first. Phases, acceptance tests, workflow rules |
| [PRD.md](PRD.md) | Product specification 1.0 — flows, screens, rules, analytics events |
| [DECISIONS.md](DECISIONS.md) | Running log of decisions made during the build |

## Design

| Path | What's there |
|---|---|
| `design/screens/` | 27 rendered screens |
| `design/humor-assets/` | Block-screen images and display font |
| `design/regenerate-screens.py` | Rebuilds the renders from the design export |
| `toki-shrine-ui-mockups/` | Claude Design export — Nocturne tokens and source markup |

Nocturne, dark theme only. Every visual value comes from `_ds/nocturne-*/styles.css`.

## Status

Pre-build. Phase 1 scope is settled and feasibility is proven on hardware — app detection and browser URL blocking both verified on a Galaxy S23 Ultra running Android 16 (see PRD §14).

Android first. iOS is a separate architecture built on Apple's Screen Time APIs and is not committed to.

## History

`PRD-phase-0.md` and `DESIGN-BRIEF-phase-1.md` are the earlier documents that fed into PRD 1.0, retained for reference.

# Toki Shrine

An Android app that puts a deliberate, self-chosen pause in front of the apps and websites you doomscroll.

It never blocks you out. You pick what to slow down, you pick what it should cost to get in, and then you pay that cost — either typing out a passage of random words or waiting through a countdown while the challenge stays visible and the device remains unlocked. If you still want in afterwards, that's a fine answer.

Deliberately friction-only. A hard blocker is a different product.

## Documents

Start with [AGENTS.md](AGENTS.md), then [CURRENT](coordination/CURRENT.md). The active task points to its assignment, handback, review and owner checklist.

| Document | Purpose |
|---|---|
| [WORKFLOW](coordination/WORKFLOW.md) | Manual role routing, direct owner-to-GLM corrections, bounded review and stopping rules |
| [CONTEXT-MAP](docs/CONTEXT-MAP.md) | Load relevant component contracts, source/tests and phase requirements |
| [PRD](PRD.md) | Product requirements; applicable §17 addenda supersede older values/copy |
| [RECORDS](coordination/RECORDS.md) | How tasks, evidence and decisions evolve without growing startup context |
| [History](docs/history/INDEX.md) | Preserved prior records, consulted for specific questions only |

## Design

| Path | What's there |
|---|---|
| `design/screens/` | Numbered screen renders plus current owner-supplied references |
| `design/humor-assets/` | Block-screen images and display font |
| `design/regenerate-screens.py` | Rebuilds the renders from the design export |
| `toki-shrine-ui-mockups/` | Claude Design export — Nocturne tokens and source markup |

Nocturne, dark theme only. Every visual value comes from `_ds/nocturne-*/styles.css`.

## Status

See [CURRENT](coordination/CURRENT.md) for live status and next actor. Code approval, owner device acceptance and phase clearance are separate. Workflow is human-orchestrated; there is no automatic agent handoff.

Android first. iOS is a separate architecture built on Apple's Screen Time APIs and is not committed to.

## History

`PRD-phase-0.md` and `DESIGN-BRIEF-phase-1.md` are the earlier documents that fed into PRD 1.0, retained for reference.

# Time Shrine

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
| [PostHog plan](docs/plans/posthog-analytics.md) | Proposed follow-up after Phase 7; analytics/privacy decisions pending |
| [RECORDS](coordination/RECORDS.md) | How tasks, evidence and decisions evolve without growing startup context |
| [History](docs/history/INDEX.md) | Preserved prior records, consulted for specific questions only |

## Design

| Path | What's there |
|---|---|
All design material is in [`design/`](design/README.md): the Nocturne design system and source mocks (`design/mockups/`), screen renders and handoffs (`design/screens/`), the app icon (`design/icon-final/`) and block-screen assets (`design/humor-assets/`).

Nocturne, dark theme only. Every visual value comes from `design/mockups/project/_ds/nocturne-*/styles.css`.

## Status

See [CURRENT](coordination/CURRENT.md) for live status and next actor. Code approval, owner device acceptance and phase clearance are separate. Workflow is human-orchestrated; there is no automatic agent handoff.

Android first. iOS is a separate architecture built on Apple's Screen Time APIs and is not committed to.

## History

The earlier documents that fed into PRD 1.0 ([Phase 0 PRD](docs/history/inputs/PRD-phase-0.md), [Phase 1 design brief](docs/history/inputs/DESIGN-BRIEF-phase-1.md)) and the phase verification reports ([verification feedback](docs/history/verification-feedback/)) are kept in [docs/history](docs/history/INDEX.md).

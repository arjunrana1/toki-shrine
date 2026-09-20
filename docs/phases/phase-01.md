# Phase 1 — Data layer

Historical closure recorded at a78f877; preserve current persistence contracts.

Read only for this phase or an affected acceptance question. Scope/acceptance below was relocated from the original root instructions. PRD and applicable §17 amendments resolve stale copy/values; current WORKFLOW governs who performs checks. Historical shell/device commands describe evidence, not standing authorization; build through `./build.sh` and leave device operations to the authorized owner.

## Scope and acceptance

Room entities, DAOs and repositories for blocks and events. No UI.

Blocks carry: id, name, apps, sites, friction type, pause minutes, pause chars, turn-off chars, countdown seconds, show-typos flag, enabled state. Events per `PRD.md` §10 schema.

**Acceptance**

- Instrumented test: insert a block, read it back, all fields match.
- Instrumented test: inserting an app already present in another block is rejected or flagged by the repository — it never silently duplicates.
- Instrumented test: insert 100 events, query walk-aways for the last 7 days, count is correct.
- Instrumented test: the Stats formulas in `PRD.md` §9 return correct values against a seeded fixture, including walk-away rate.
- `./gradlew testDebugUnitTest connectedDebugAndroidTest` exits 0.

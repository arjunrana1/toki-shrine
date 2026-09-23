# Phase 7 — Stats, feedback and final pass

Planned only. Requires prior phase clearance and a scoped task before implementation.

Read only for this phase or an affected acceptance question. Scope/acceptance below was relocated from the original root instructions. PRD and applicable §17 amendments resolve stale copy/values; current WORKFLOW governs who performs checks. Historical shell/device commands describe evidence, not standing authorization; build through `./build.sh` and leave device operations to the authorized owner.

Resolve distribution/migration and accessibility obligations from the component map before release. Stats formulas are in the persistence/events contract.

## Scope and acceptance

Screens 23 and 25, plus a full pass over every screen against its render.

**Acceptance**

- Stats figures match the definitions in `PRD.md` §9 against real recorded events.
- Walk-away rate equals `walk_aways ÷ (walk_aways + completed_challenges)`.
- The per-app leaderboard is ordered descending by walk-away count.
- *Send it* opens the mail client to `arjranaprep@gmail.com` with subject `Feedback from user` and the typed text in the body.
- No "attach diagnostic log" control exists anywhere.
- Every **active** event in `PRD.md` §10 fires at least once across a full manual run-through, verified by querying the event table. Retired/historical events are excluded. Remove the unused `bubble_dragged` and `challenge_abandoned` instrumentation surfaces without deleting historical Room rows; dragging and challenge suspension behavior remain unchanged.
- `grep -rE "#[0-9a-fA-F]{6}" app/src/main/java --include=*.kt | grep -v ui/theme/` still returns no matches.
- No network permission in the manifest, and no third-party dependency in `app/build.gradle.kts` beyond AndroidX, Compose and Room.

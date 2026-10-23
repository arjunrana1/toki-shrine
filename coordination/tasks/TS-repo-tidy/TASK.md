# TS-repo-tidy — one design folder and a clean root

- **State:** `closed` (23 October 2026). Arjun asked for a clean, up-to-date repository before starting the iOS port in a separate folder. He chose: all designs under `design/`, the old root files into `docs/history`, and to keep his ignored original design zip on disk untouched. Done by Claude Opus 5.5 in the same session. Layout and docs only: no behaviour change.
- **Base:** `main` at `091ee4f`.

## Changes

| ID | Change |
|---|---|
| RT-1 | Committed Arjun's deletion of the root `design_handoff_stats_screen/`. Every file was byte-identical to `toki-shrine-ui-mockups/stats-screen/` (same git blobs), so nothing is lost. |
| RT-2 | `toki-shrine-ui-mockups/` → `design/mockups/` (Nocturne design system, source mocks, Stats hi-fi handoff). New [design/README.md](../../../design/README.md) indexes every design source. `.gitignore`, `design/regenerate-screens.py` (now a path relative to the script), `Color.kt`'s comment, PRD §15, README and the theme/UI contract point to the new path. |
| RT-3 | `PRD-phase-0.md` and `DESIGN-BRIEF-phase-1.md` → `docs/history/inputs/`; `Verification Feedback/` → `docs/history/verification-feedback/`. The redirect stubs `DECISIONS.md`, `HANDOVER.md` and `GLM_HANDOVER.md` were removed: nothing linked to them, AGENTS.md is the entry point, and their original texts stay in `docs/history/migration-2026-09-18/`. Two app comments that cited `DECISIONS.md` now cite docs/history. |
| RT-4 | Every relative Markdown link in the repository, history included, was rewritten for the moves by resolving it from its old location. This also fixed 6 links in the TS-unlock-minutes handback that pointed at pre-rename `tokishrine` source paths. Plain-text path mentions in history are left as written. |
| RT-5 | Local only: removed ignored `.DS_Store` files. The ignored `Toki Shrine UI mockups-handoff.zip` stays. |

## Checks

- Broken relative Markdown links: 37 before, **31 after**. The 31 are the long-standing archived `submissions/` snapshot copies, whose links were relative to their original folders; they are left verbatim.
- `./build.sh assembleDebug testDebugUnitTest` → BUILD SUCCESSFUL (comment-only app edits). `regenerate-screens.py` parses; it was not run.
- `git` records the moves as renames.

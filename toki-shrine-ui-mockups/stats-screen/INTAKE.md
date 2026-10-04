# Stats redesign intake

This is a byte-for-byte copy of the owner-supplied `design_handoff_stats_screen/` bundle, kept with the project's design references. The existing `project/TimeShrine Mocks.dc.html` board and `design/screens/23-stats.png` still represent the implemented Phase 7 Stats screen. This new reference is pending product decisions and has not yet replaced them.

Open `Stats Hi-fi.dc.html` with `support.js` and `_ds/` alongside it. Read `PRODUCT_NOTES.md` for the proposed data and behavior, then `README.md` for the visual specification. The link to "Stats Product Notes.md" inside the original HTML points to a filename that is not in the bundle; use `PRODUCT_NOTES.md`.

| Design state | Reference |
|---|---|
| S1 | Default, full scrolling dashboard |
| S2 | Time saved explanation sheet |
| S3 | Nope rate explanation sheet |
| S4 | Partial or tough week |
| S5 | No Usage Access |
| S6 | No blocks |

The handoff calls this Screen 21 because it uses the original mock board numbering. The app PRD and `design/screens/` call Stats Screen 23.

Owner-supplied PNG exports of S1–S6 are indexed in [`design/screens/stats-redesign/`](../../design/screens/stats-redesign/README.md). S4 is a top-half export; the HTML and visual README specify its full behavior. The PNGs show proposed visuals, while the current PRD and Phase 7 records await the redesign amendment.

## Approved implementation authority — 4 October 2026

[PRD §9](../../PRD.md) and the [Stats data/UI contract](../../docs/components/stats.md) now supersede behavior/copy in the untouched source handoff. Follow the [active Phase 7 task](../../coordination/tasks/TS-P7-stats-feedback-final/TASK.md) for the bounded GLM assignment; PNGs are available and no browser-policy workaround is needed.

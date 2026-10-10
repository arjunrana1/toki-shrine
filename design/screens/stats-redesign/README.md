# Stats redesign PNG references

Owner-supplied exports for the approved "Your time" screen. For metric definitions and exact UI specifications, use the [source handoff](../../../toki-shrine-ui-mockups/stats-screen/INTAKE.md). The existing `../23-stats.png` is the older implemented Stats design.

| State | PNG | Coverage |
|---|---|---|
| S1 Default | [Full scroll](23-stats-s1-default-full-scroll.png) | Full scrolling dashboard |
| S2 Time saved | [Info sheet](23-stats-s2-time-saved-sheet.png) | Full sheet |
| S3 Nope rate | [Info sheet](23-stats-s3-nope-rate-sheet.png) | Full sheet |
| S4 Tough week | [Top half](23-stats-s4-tough-week-top-half.png) | Hero, tiles and charts; lower sections follow S1 |
| S5 No Usage Access | [Permission prompt](23-stats-s5-no-usage-access.png) | Full state |
| S6 No blocks | [Empty state](23-stats-s6-no-blocks.png) | Full state |
| S1 v2 (8 Oct) | [Today + This week layout](23-stats-s1-v2-today-week-2026-10-08.webp) | **Supersedes S1's hero/tile arrangement**: today hero, Attempts/Nope rate today, This week card, ~5-row scrolling app box (drop the "vs your usual" line) |
| S2 v2 (8 Oct) | [Time saved sheet](23-stats-s2-v2-time-saved-sheet-2026-10-08.png) | **Supersedes S2 copy/spacing** |
| S7 (8 Oct) | [App rows with visit chips](23-stats-s7-app-visit-chips-2026-10-08.png) | Visit-length chip, measured `~6m/visit` vs `12m/visit · yours` |
| S8 (8 Oct) | [Visit length sheet](23-stats-s8-visit-length-sheet-2026-10-08.png) | Per-app override: stepper, presets, Save, Use measured |

These images preserve the handoff's original copy. Product decisions made after the export (explicit nopes only, recent available baseline, active-block zero state) are now authoritative in [PRD §9](../../../PRD.md#9-stats--your-time-approved-replacement-4-october-2026) and the [Stats contract](../../../docs/components/stats.md).

The 8 October references are governed by the [PRD §17 8 October addendum](../../../PRD.md#phase-7-owner-experience-addendum--8-october-2026); where they conflict with S1/S2, the 8 October files win.

# Wizard redesign — owner acceptance

Arjun owns device and experience testing; compilation does not execute these checks. Data reset is owner-approved, but none was performed during specification work. Use newly created fixtures after an intentional reset; never relabel prior results as fresh.

| ID | Actions | Expected |
|---|---|---|
| BW-01 | Create app and website blocks through all screens | 1/5–5/5; unchanged selection/name behavior; sheet is not a sixth step; new block OFF |
| BW-02 | Inspect and switch method cards without adjustments | Typing 150 chars / waiting 60 sec; both pause 15 min; selection does not shift controls |
| BW-03 | Open each details sheet; adjust through bounds | Only relevant field plus pause; 100..200/10 chars, 60..300/5 sec, 5..100/5 min; accurate live summaries |
| BW-04 | Adjust, Done/Back/swipe/outside dismiss, reopen, then Reset | Draft retained for each dismissal; Reset active method and pause to defaults; no block saved |
| BW-05 | Switch methods after adjustments; exercise every disable choice | Each method retains draft values; disable inherits method; 220/350/700 chars or 3/6/12 min; middle initially selected |
| BW-06 | Backtrack, recreate the screen, edit from contents and direct friction, cancel | Draft retained through supported recreation; sheet Back dismisses first; direct 3/5 Back returns to detail; canceled edits unchanged |
| BW-07 | Save/reopen/edit both methods; inspect review, detail, activation | Values round-trip, method-specific disable summary, no phone-in-hand copy; saves OFF; activation alone gated by permissions |
| BW-08 | Long target lists, long names, large system text and small available height | Cards/sheet/Next/Save remain readable and reachable, correct insets and scrolling, no overlap |
| BW-09 | Run P3-14 matrix and P3-01 create-without-permissions scenario | Ownership/nonempty/name/domain protections preserved; no permission wall on creation. Record cross-linked results in Phase 3 checklist |
| BW-10 | Ask an early tester to create a block without coaching, then explain access, pause and disable | Record misunderstandings and observed completion, not just a visual pass; tester can distinguish temporary pause from indefinite OFF |

## Owner report — 19 September 2026

The owner reports that every requested wizard case passed on the latest installed direct-correction build recorded in CURRENT. The report did not restate a build identifier. DC-05's supplied screenshot identified an overly light sheet; the owner now reports that DC-06's darker sheet works in manual verification. This is owner visual evidence, not an independent code-review verdict.

| ID | Owner result | Recorded observation / remaining action |
|---|---|---|
| BW-01 | Pass | Five-step app and website creation paths passed; new blocks remained OFF. |
| BW-02 | Pass | Both card behaviors passed; DC-06 manual retest confirms the details-sheet darkness now works. |
| BW-03 | Pass | Details-sheet fields, bounds and live summaries passed. |
| BW-04 | Pass | Dismissal, Reset and draft-retention behavior passed. |
| BW-05 | Pass | Method retention and both disable ladders passed. |
| BW-06 | Pass | Back, supported recreation, direct-entry cancellation and no-write behavior passed. |
| BW-07 | Pass | Saved values/summaries, OFF save and permission-gated activation passed. |
| BW-08 | Pass | Readability, reachability, insets and scrolling under constrained layouts passed. |
| BW-09 | Pass | P3-14 preservation matrix and the P3-01 create-without-permissions path passed; cross-linked Phase 3 results updated below. |
| BW-10 | Pass | Owner reports the uncoached comprehension check passed. |

## Nonvisual evidence

Track build/JVM results and instrumented compile separately in HANDBACK. Actual repository/schema validation, field round-trips, event payloads, step numbering, unchanged edits, rejected writes and duplicate-save protection need appropriate test evidence; source review or screenshots alone do not prove Room behavior. Record unexecuted cases explicitly. Challenge countdown/cancellation execution is Phase 5 acceptance, not a pass/fail requirement for this wizard build.

# Historical decision record

Historical evidence, not current workflow instructions. Later records may supersede this entry. Use current component contracts and PRD requirements for implementation.

Source: pre-migration DECISIONS.md, section 14.

## Phase 2 owner experience feedback — 12 September 2026

- Recorded current requirements in PRD.md §17, with stable R1–R14 identifiers and pending choices explicitly separated. This section overrides stale mockup layout/copy and historical decisions for the listed refinements. In particular pause minimum is now 5 minutes (default 15), estimate is 0.4 s/character, and new empty blocks are disallowed; the former 15-minute minimum, 0.3 s/character and zero-target-create decisions are superseded.
- Saved the four owner-provided images under the existing phase verification folder. Image 02 supplies target-list layout only; its old estimates and saved-OFF sentence are not reinstated.
- Owner reports original checks 4–9 fine, with configuration changes still needing retest; 10 unreported. Findings/refinements in 1/3/11 remain open. Full status is in PRD §17. No feature code, build/test/device operations or phase sign-off in this documentation pass.
- Open questions: exact typing headings, row cap, existing names, home action placement, detail control style, existing/edit-to-empty behavior and direct friction-editor Back. Answers must be incorporated into §17 before the dependent work is assigned.

- **Heading clarification, 13 September 2026:** owner selected “Type to pause → Pause duration → Disable this block.” Incorporated into PRD §17 R2; that choice is resolved. Other listed questions remain pending.

- **Owner clarifications, 13 September 2026:** PRD §17 R10/R11 now specify the existing switch alone on detail and a bottom row with smaller Share feedback left / wider + New Block right. Latest requested name cap is 20, superseding the recorded 40; existing-name policy remains pending. Owner explicitly confirmed R8: temporary removal of all targets is allowed, but both create and edit must block leaving step 1 and saving while empty. This also requires guarding direct-friction-edit saves. Row cap, existing names/empty-block activation and direct-friction Back remain pending. Existing screenshots/requirements are reused, no new handoff file or feature code.

- **Further owner clarification, 13 September 2026:** review target list shows four rows before internal scrolling; direct THE FRICTION edit enters 3/4 and Back returns to detail. Owner authorizes removing empty blocks from the testing app so legacy empty-block behavior need not be expanded. This is one-time test-data cleanup only, preserving all nonempty blocks; not performed by Codex and not a migration. Name interpretation/existing-name handling remains pending. PRD §17 updated.

- **Final owner clarification, 13 September 2026:** there are no existing longer block names; the app is in early build/testing, not live. R7 uses single-line input capped at 20 characters and up to two display lines without ellipsis, with no legacy-name migration work. Owner confirms checklist test 10 passed. All PRD §17 pending choices are now resolved; R1–R14 are ready for the bounded GLM 5.3 refinement pass. This is requirement/evidence documentation, not implementation or phase sign-off.

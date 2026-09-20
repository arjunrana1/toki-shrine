# Historical decision record

Historical evidence, not current workflow instructions. Later records may supersede this entry. Use current component contracts and PRD requirements for implementation.

Source: pre-migration DECISIONS.md, section 22.

## Codex corrective pass on 512dc36..084b5eb — 13 September 2026

- **Scoped corrections after the CHANGES REQUIRED checkpoint (regression coverage only; base 084b5eb).**
  - **Unchanged-edit fixture:** `rejectedSaveRetainsDraftAndShowsInlineMessage` seeded `show_typos = false` but asserted a recovery save with `fields_changed = none` — impossible now that every UI save forces true. The fixture seeds `showTypos = true` (draft helper grew a parameter), so "none" is genuine. Explicit legacy coverage added instead of lost: new `editRewritesLegacyShowTyposFalseToTrue` asserts a stored false value is rewritten to true and `block_edited.fields_changed` reports exactly `show_typos`.
  - **Website test waits for enabled semantics:** the Add gate in `siteAddRequiresACompleteSingleLineDomain` (and the same pattern in `siteAddIsDisabledUntilOwnershipLoads` plus the friction-entry `Next` check) now uses `assertIsEnabled()`, not `assertHasClickAction()` — a click action can exist while a control is disabled.
  - **Disable-this-block helper copy:** both sentences now read "this block" ("You'll need to type random words of this length to disable this block. You can edit or delete this block once it's disabled."), matching the owner's requested copy; PRD §17 round-two addendum corrected to match.
- **Explicitly not changed (per review):** multiline-paste behavior — pasting `reddit.com\nabdes` still merges into `reddit.comabdes`, which is syntactically valid; Codex flagged that silent merging is an owner decision, not a builder one. Haptic tuning untouched — owner floated +30% duration but approved no exact change; preset effects remain heavy-click/click. Phase 3 note recorded: activation haptics must fire only after permission checks and successful activation.
- **Checks:** assembleDebug, testDebugUnitTest (12 JVM green), assembleDebugAndroidTest compile — instrumented compilation only, execution still pending. No device operations.

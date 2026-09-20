# Historical decision record

Historical evidence, not current workflow instructions. Later records may supersede this entry. Use current component contracts and PRD requirements for implementation.

Source: pre-migration DECISIONS.md, section 19.

## Phase 2 post-validation fixes, round two — 13 September 2026

- **Owner device feedback on 512dc36 (four fixes, one pass; PRD §17 second-round addenda records the requirements).**
  - **Website input hardened.** The site field is now `singleLine`, pastes are filtered for `\n`/`\r` at `onValueChange`, and Add requires `isValidFullDomain(canonical)` — two or more dot-separated labels (letters/digits/hyphens, no edge hyphens, 1–63 chars), letters-only TLD ≥ 2 chars, whole domain ≤ 253 chars. "reddit"/"abdes"/multiline entries are rejected; the helper slot gained an invalid-domain message ("Enter a complete domain like reddit.com") between the held-elsewhere and default states. No IDN/punycode (validator is locale-free lowercase ASCII for now). Validator lives in `ui/util/Domain.kt` with a 5-case JVM suite (DomainTest) — executed green, not compile-only.
  - **Helper copy updated** to the owner's new sentences for Type to pause and Disable this block (supersedes §17's first-round copy for those two controls only).
  - **Show-typos toggle removed; typos always shown.** `CreateFlowState.showTypos` and the toggle row are gone; the draft always writes `showTypos = true`. The entity column is kept this dev phase to avoid a destructive DB wipe while the owner is mid-testing (fallbackToDestructiveMigration would erase their fixtures); every save rewrites the column to true and Phase 5 must ignore stored values. Column removal belongs to the next deliberate schema revision.
  - **Wait bound at persistence.** The UI stepper already capped at 5:00 (COUNTDOWN_MAX = 300, unchanged); what was missing was a storage bound — `MAX_STORED_COUNTDOWN_SECONDS = 1200` (20 min) now `require`d in createBlock and updateBlock, ahead of any DAO write, with two new repository rejection tests. Nothing about the bound appears in the UI.
- **Tests:** new DomainTest (5 cases), repository countdown rejections (create + update), UI test `siteAddRequiresACompleteSingleLineDomain` (dot-less rejected with message, pasted newlines stripped, valid domain adds). Existing suites untouched otherwise.
- **Checks:** assembleDebug, testDebugUnitTest (12 JVM green: 7 Stats + 5 Domain), assembleDebugAndroidTest compile. Instrumented execution still pending; owner reinstall follows this commit.

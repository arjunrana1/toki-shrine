# Blocks, targets and editor contract

Read for create/edit/input/ownership/target list changes. PRD §§4/6/7 plus **all applicable §17 addenda** govern current behavior; earlier 20/40-character caps, 0.3-second estimates and target transfers are superseded.

## Source and tests

`app/src/main/java/com/arjunrana/tokishrine/ui/screens/{CreateFlowScreen,BlockListScreen,BlockDetailScreen,TurnOnScreen}.kt`; `ui/components/TargetList.kt`; `ui/util/{Domain,Estimate}.kt`; `data/apps/InstalledAppsRepository.kt`; `data/repo/BlockRepository.kt`.
Tests: `app/src/test/java/com/arjunrana/tokishrine/ui/util/DomainTest.kt`; `app/src/androidTest/java/com/arjunrana/tokishrine/ui/CreateFlowScreenTest.kt` and data/BlockRepositoryTest.kt.

## Current rules

- Apps/sites owned by another ON or OFF block remain unavailable with “Already added to a block”, no owner name/dialog/transfer. Wait for ownership maps before enabling selection. Failed stale-draft save retains the editor/draft and inline conflict message. Storage protection: [persistence/events](persistence-events.md).
- Create/edit cannot leave contents step or save without targets, including direct friction edit; temporary empty selection is allowed while editing. Never introduce a legacy migration for the prior one-time empty-test-block cleanup.
- Name input is single-line, capped at 30; displays may wrap without ellipsis. No legacy-long-name migration required by owner.
- Website Add requires a complete domain: ASCII dot-separated labels, no edge hyphens, letters-only TLD of at least two characters; whole domain only, no IDN/punycode. Canonical surrounding whitespace/trailing dot/case handling remains. Preserve interior pasted newlines so invalid multiline input is rejected, never merged; Enter inserts nothing.
- Implemented five-screen redesign: pause minimum 5/default 15/maximum 100 minutes, step 5; typing estimate 0.4 seconds/character. Exact helper copy and post-validation estimate sentence live in PRD §17; do not copy stale mock values.
- Target lists: shared vertical icon/label/divider rows, four-row internal scroll bound and overflow indicator. Contents selection scrolls with bottom Next pinned. Save/summary must remain reachable.
- Detail has one activation switch; edit/delete unavailable while ON. Delete OFF asks Confirm/Go back. Redesigned friction Edit opens 3/5 and Back returns to detail; content Edit opens 1/5.
- Create Back closes search first, then unwinds steps, then records abandonment once on exit. Edits exit silently; successful edit logs fields_changed from before the write. Terminal guards prevent duplicate save/abandon actions and release on rejected save.
- Home bottom actions are Feedback (no icon) and wider New Block. Future feedback behavior remains Phase 7. Direct OFF remains temporary until Phase 5's inherited-method gate.
- New-block review keeps Save pinned and now shows the later owner-approved helper `Turn it on from the "Blocks" homescreen.` beneath it; the block still saves OFF and activation remains a separate gated action. This 19 September owner correction supersedes only the older no-helper presentation, not save behavior.
- App search shows `loading...` directly below the search field until installed apps finish loading, then replaces it with the applicable result state/count. Keep target rows inert until ownership data is available. The owner accepted the loading and review-helper presentation on `f92661d`.

## Carry-forwards

Phase 3 route restoration and the wizard's supported draft/terminal recreation paths are closed by owner evidence and the executed Android suite. This does not claim arbitrary process-death survival beyond the supported saved-state contract. The paste/input and other Phase 2 preservation matrix passed under P3-14. See the [closed checklist](../../coordination/tasks/TS-P3-validation/OWNER-CHECKS.md).

## Implemented wizard redesign

See PRD §7 and §17's 19/23 September amendments and the [closed implementation task](../../coordination/tasks/TS-block-wizard-redesign/TASK.md). Five screens: contents, name, method, disable difficulty, review. Details is an optional sheet inside step 3. Production values remain typing 100..200/10, default 150; pause wait 60..300/5 seconds, default 60; pause 5..100/5 minutes, default 15; disable 220/350/700 characters or 3/6/12 minutes. Debug owner testing temporarily lowers the two pause minima to 20 and replaces the first disable rungs with 20 characters/seconds; Phase 7 removes those overrides. Sheet dismissal retains draft only; summaries update immediately. Preserve edit cancellation, direct entry, draft restoration and save guards. Challenge typo highlighting is submission-gated and has no preference. Closure evidence is separated in task records; challenge execution remains Phase 5.

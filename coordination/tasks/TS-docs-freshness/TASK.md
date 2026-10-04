# TS-docs-freshness — Current-document review and cleanup

- **State:** `closed` — documentation cleanup and checks complete; see [HANDBACK](HANDBACK.md). [CURRENT](../../CURRENT.md) continues to point to Phase 7 owner acceptance and its pending Codex confirmation.
- **Owner/request:** Arjun, 27 September 2026: review document freshness, share the plan, then clean the repository. Phase 7 device testing is pending on Arjun; he reports the rest complete. PostHog is the intended follow-up.
- **Owner clarification:** on 27 September Arjun explicitly chose to keep the previously recorded Codex confirmation pending. The cleanup retains it alongside Phase 7 device acceptance; it does not assign or perform that review.
- **Role/author:** Codex documentation maintainer; one writer.
- **Base:** `89ef9e3`, clean `main` synchronized with `origin/main` at startup. Phase 7 code submission is `12ee572..89ef9e3`.
- **Scope:** live status, context routing, component/phase descriptions, Phase 7 submission metadata and review-note checklist clarifications, preserved evidence, and a proposed PostHog planning record.
- **Exclusions:** application/build/test changes, device work, new acceptance claims, Phase 7 closure, SDK/network/dependency changes, analytics implementation and rename.

## Findings and plan shared before editing

1. CURRENT duplicates the device-installation history and closed Phase 3 evidence; shorten it and preserve the original behind a history link.
2. Phase 4–7 documents and component/map descriptions still present implemented detection, challenges, pause lifecycle, Feedback and production bounds as future work.
3. Phase 7 TASK remains `ready`, HANDBACK remains `ready_for_review`, and submission records say uncommitted although Git contains `89ef9e3`; align metadata without inventing another review.
4. OWNER-CHECKS P7-O3/P7-O11 contradict the recorded P7-N1/P7-N2 observations; state the reviewed recreation and per-origin return behavior.
5. PostHog is next in the owner's intended sequence, but PRD §10 still defines local-only analytics; add a proposed planning record with open consent/data decisions and explicit implementation prerequisites.
6. Follow-up inspection found P5A still marked `awaiting_owner` and closed P5 task headers with obsolete phase-transition claims. Attribute completion to the existing combined Phase 5 owner closure. Four inherited archive-relative Markdown links remain immutable; document their original task bases and resolved destinations in the history index.

## Preservation

Original bytes from the base are retained in [CURRENT](submissions/89ef9e3/CURRENT.before.txt), [Phase 7 HANDBACK](submissions/89ef9e3/P7-HANDBACK.before.txt) and [Phase 7 REVIEW](submissions/89ef9e3/P7-REVIEW.before.txt). These are historical evidence, not fresh-session instructions.

## Checks and stop

Verify local Markdown links, Git submission references, byte-identical snapshots, live-state consistency and `git diff --check`. Confirm there is no application/toolchain change. Save HANDBACK and close this maintenance task after those checks; Phase 7 remains `awaiting_owner`. No build/device checks are justified by documentation edits.

# Historical decision record

Historical evidence, not current workflow instructions. Later records may supersede this entry. Use current component contracts and PRD requirements for implementation.

Source: pre-migration DECISIONS.md, section 23.

## Multiline website paste: reject, never merge — owner clarification, 13 September 2026

- Resolves the choice Codex left pending: pasting `reddit.com↵abdes` must not be accepted, and must not silently merge either. The field no longer strips `\n`/`\r` from input — the raw pasted value stays visible and the domain validator keeps Add disabled (an interior break fails the label rules), so the only way forward is removing the break. Rationale for preserve-and-reject over strip-on-paste or reject-the-update: stripping produced the forbidden merged addable domain, and silently rejecting a whole update desyncs a value-based BasicTextField's displayed text from state (no recomposition is triggered); preserving the raw value keeps display, state and validation in agreement with zero reconciliation hacks. Enter still inserts nothing (`singleLine`). Surrounding whitespace alone does not invalidate — canonical trim upstream means a domain copied with a trailing newline still adds (pinned in the UI test); the validator itself rejects raw untrimmed forms (JVM-pinned). Name-field paste handling is unchanged (truncate-and-strip, reviewed). PRD §17 website addendum corrected.

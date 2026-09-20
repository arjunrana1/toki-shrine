# Historical decision record

Historical evidence, not current workflow instructions. Later records may supersede this entry. Use current component contracts and PRD requirements for implementation.

Source: pre-migration DECISIONS.md, section 16.

## R7 name-input correction — 13 September 2026

- **Codex finding on 72ea3ad (bounded correction, code-only commit).** The R7 test submitted a 26-character string while the implementation rejected any update over 20 characters outright — incompatible expectations — and the length-only callback let pasted line breaks reach the stored name (singleLine does not filter SetText/paste input). Corrected the input path to always write back a sanitized value: strip `\n`/`\r`, then truncate to 20 characters (`InputFilter.LengthFilter` semantics). Truncation replaced silent rejection because a value-based BasicTextField only reconciles the displayed text when the value param changes — a rejected update leaves nothing to recompose with, risking a field showing 26 characters while state holds 20. Requirement unchanged: single-line, ≤ 20 stored characters, display wrapping without ellipsis, no legacy migration.
- **Test realigned and extended** (`nameInputStripsLineBreaksAndTruncatesAtTwentyCharacters`, replacing `nameInputCapsAtTwentyCharacters`): normal input, exactly-20 boundary, over-limit truncation (cap assertion retained), pure line-break paste, and strip-then-cap paste. Compile-verified only; instrumented execution still pending owner authorization. Record kept uncommitted with the owner's §17 documentation pass (its tail entries interleave with builder records, so committing the file would sweep owner content); commit covers code only.

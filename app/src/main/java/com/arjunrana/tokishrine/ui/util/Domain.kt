package com.arjunrana.tokishrine.ui.util

/*
 * Website input validation (owner addendum + clarification, 13 September
 * 2026): the field accepts a complete, whole domain like reddit.com — two
 * or more dot-separated labels, each 1–63 characters of letters/digits/
 * hyphens (no leading or trailing hyphen), ending in a letters-only TLD of
 * at least two characters. Validate the canonical form (trimmed,
 * lowercased, trailing root dot removed) — the same identity the
 * repository stores. The field itself is single-line, and pasted line
 * breaks are preserved rather than stripped: an interior break fails the
 * label rules below, so a multiline paste like "reddit.com\nabdes" stays
 * invalid and can never be added (owner decision — no silent merging).
 * Trimming means leading/trailing breaks alone do not invalidate. No
 * IDN/punycode support yet; "reddit" (no dot) is rejected.
 */

private val LABEL = Regex("[a-z0-9]([a-z0-9-]{0,61}[a-z0-9])?")
private val LETTERS_ONLY_TLD = Regex("[a-z]{2,63}")

fun isValidFullDomain(domain: String): Boolean {
    if (domain.isEmpty() || domain.length > 253) return false
    val labels = domain.split('.')
    if (labels.size < 2) return false
    if (!LETTERS_ONLY_TLD.matches(labels.last())) return false
    return labels.all { LABEL.matches(it) }
}

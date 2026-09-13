package com.arjunrana.tokishrine.ui.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// Website-field validation (owner addendum, 13 September 2026). The input
// is validated in its canonical form: trimmed, lowercased, single line,
// trailing root dot removed.
class DomainTest {

    @Test
    fun acceptsCompleteDomains() {
        assertTrue(isValidFullDomain("reddit.com"))
        assertTrue(isValidFullDomain("old.reddit.com"))
        assertTrue(isValidFullDomain("news.bbc.co.uk"))
        assertTrue(isValidFullDomain("a.co"))
        assertTrue(isValidFullDomain("example-site.com"))
        assertTrue(isValidFullDomain("x-y.multi-part.example.org"))
    }

    @Test
    fun rejectsDomainsWithoutADot() {
        // The owner's reported cases: bare words must never pass.
        assertFalse(isValidFullDomain("reddit"))
        assertFalse(isValidFullDomain("abdes"))
        assertFalse(isValidFullDomain("localhost"))
        assertFalse(isValidFullDomain(""))
    }

    @Test
    fun rejectsSpacesAndLineBreaks() {
        // The field strips line breaks and the repository would never see a
        // space, but the validator must hold the line on its own too.
        assertFalse(isValidFullDomain("abc def.com"))
        assertFalse(isValidFullDomain("abc\ndef"))
        assertFalse(isValidFullDomain("reddit.com\nabdes"))
    }

    @Test
    fun rejectsMalformedLabels() {
        assertFalse(isValidFullDomain("-bad.com")) // leading hyphen
        assertFalse(isValidFullDomain("bad-.com")) // trailing hyphen
        assertFalse(isValidFullDomain("a..com")) // empty label
        assertFalse(isValidFullDomain(".com")) // empty first label
        assertFalse(isValidFullDomain("a.-.com")) // hyphen-only label
        assertFalse(isValidFullDomain("under_score.com")) // underscore
        assertFalse(isValidFullDomain("a${"b".repeat(63)}.com")) // 64-char label
    }

    @Test
    fun rejectsWeakTlds() {
        assertFalse(isValidFullDomain("a.c")) // single-letter TLD
        assertFalse(isValidFullDomain("a.c0m")) // digits in TLD
        assertFalse(isValidFullDomain("a.123")) // numeric TLD
        assertFalse(isValidFullDomain("a.-")) // hyphen TLD
    }
}

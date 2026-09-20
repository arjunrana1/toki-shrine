package com.arjunrana.tokishrine.detection

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

// Whole-domain matching (PRD §13): exact and subdomain matches only, dot
// boundary enforced, lookalikes and path-level near-misses rejected.
class DomainMatcherTest {

    private val blocked = setOf("reddit.com", "instagram.com")

    @Test
    fun extractsHostFromBareDomain() {
        assertEquals("reddit.com", DomainMatcher.extractHost("reddit.com"))
    }

    @Test
    fun extractsHostWithSchemeAndPath() {
        assertEquals("old.reddit.com", DomainMatcher.extractHost("https://old.reddit.com/r/all"))
        assertEquals("reddit.com", DomainMatcher.extractHost("http://reddit.com#anchor"))
        assertEquals("reddit.com", DomainMatcher.extractHost("https://reddit.com/search?q=x"))
    }

    @Test
    fun extractsHostWithoutSchemeButWithPath() {
        assertEquals("reddit.com", DomainMatcher.extractHost("reddit.com/r/all"))
    }

    @Test
    fun lowercasesAndDropsTrailingDotAndPort() {
        assertEquals("reddit.com", DomainMatcher.extractHost("Reddit.com."))
        assertEquals("reddit.com", DomainMatcher.extractHost("https://Reddit.COM:443/"))
    }

    @Test
    fun stripsUserInfo() {
        assertEquals("reddit.com", DomainMatcher.extractHost("https://user:pass@reddit.com/"))
    }

    @Test
    fun rejectsNonHostText() {
        assertNull(DomainMatcher.extractHost("how do i reddit"))
        assertNull(DomainMatcher.extractHost("reddit"))
        assertNull(DomainMatcher.extractHost(""))
        assertNull(DomainMatcher.extractHost("   "))
    }

    @Test
    fun exactDomainMatches() {
        assertEquals("reddit.com", DomainMatcher.matchDomain("reddit.com", blocked))
        assertEquals("instagram.com", DomainMatcher.matchDomain("instagram.com", blocked))
    }

    @Test
    fun subdomainsMatchTheirBlockedDomain() {
        assertEquals("reddit.com", DomainMatcher.matchDomain("old.reddit.com", blocked))
        assertEquals("reddit.com", DomainMatcher.matchDomain("www.reddit.com", blocked))
        assertEquals("reddit.com", DomainMatcher.matchDomain("a.b.reddit.com", blocked))
    }

    @Test
    fun lookalikesDoNotMatch() {
        assertNull(DomainMatcher.matchDomain("notreddit.com", blocked))
        assertNull(DomainMatcher.matchDomain("reddit.com.evil.com", blocked))
        assertNull(DomainMatcher.matchDomain("redditco.com", blocked))
        assertNull(DomainMatcher.matchDomain("reddit.org", blocked))
    }

    @Test
    fun unrelatedDomainsDoNotMatch() {
        assertNull(DomainMatcher.matchDomain("example.com", blocked))
        assertNull(DomainMatcher.matchDomain("", blocked))
    }
}

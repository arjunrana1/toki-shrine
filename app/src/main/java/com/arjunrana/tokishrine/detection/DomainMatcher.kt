package com.arjunrana.tokishrine.detection

/*
 * Whole-domain matching for blocked sites (PRD §13): a host matches a
 * blocked domain only when it equals it or is a subdomain of it —
 * old.reddit.com matches reddit.com, notreddit.com and
 * reddit.com.evil.com do not. No path, query or fragment is ever
 * considered. The input is the raw address-bar text, which browsers
 * expose as whatever the user typed or navigated to (scheme optional,
 * path optional), so extraction is defensive rather than strict.
 */
object DomainMatcher {

    // Reduces address-bar text to a bare lowercase host, or null when the
    // text does not look like a URL host at all (callers already filter
    // search queries by the space rule; this catches the remaining junk).
    fun extractHost(addressBarText: String): String? {
        var text = addressBarText.trim()
        if (text.isEmpty() || text.contains(' ') || text.contains('\t')) return null
        val schemeSeparator = text.indexOf("://")
        if (schemeSeparator >= 0) text = text.substring(schemeSeparator + 3)
        // Cut path, query and fragment; strip any userinfo prefix; drop a
        // trailing port. Address-bar text carries at most a simple
        // host[:port]/path shape, so a last-colon split is sufficient and
        // IPv6 literals (bracketed) never match a blocked domain anyway.
        text = text.substringBefore('/').substringBefore('?').substringBefore('#')
        text = text.substringAfterLast('@')
        val portColon = text.lastIndexOf(':')
        if (portColon >= 0 && text.substring(portColon + 1).all { it.isDigit() } && portColon > 0) {
            text = text.substring(0, portColon)
        }
        text = text.lowercase().trimEnd('.')
        // A single label ("reddit") or an empty remainder is not a domain.
        return text.takeIf { it.contains('.') }
    }

    // Returns the canonical blocked domain the host belongs to, or null.
    // The dot boundary in the suffix check is what keeps lookalikes out:
    // "notreddit.com".endsWith(".reddit.com") is false.
    fun matchDomain(host: String, blockedDomains: Set<String>): String? =
        blockedDomains.firstOrNull { domain -> host == domain || host.endsWith(".$domain") }
}

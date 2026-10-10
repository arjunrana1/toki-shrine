package com.arjunrana.tokishrine.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Test

/*
 * Pure mapping for the Settings supported-browsers row and screen (P7-F27,
 * 9 October §17 addendum): the browser SET is owned by the bundled
 * detection configuration; these helpers only dress packages in display
 * names and derive the Settings subtitle. Pinned here on the JVM so a
 * config addition without a display-name table row visibly falls back
 * rather than showing a raw dotted package ID.
 */
class SupportedBrowsersPresentationTest {

    // Today's bundled detection_config.json browser list, in asset order.
    private val bundledPackages = listOf(
        "com.android.chrome",
        "com.chrome.beta",
        "com.sec.android.app.sbrowser",
        "org.mozilla.firefox",
        "com.brave.browser",
        "com.microsoft.emmx",
        "com.opera.browser",
    )

    @Test
    fun everyBundledBrowserHasAProductName() {
        assertEquals(
            listOf(
                "Chrome",
                "Chrome Beta",
                "Samsung Internet",
                "Firefox",
                "Brave",
                "Microsoft Edge",
                "Opera",
            ),
            bundledPackages.map(::browserDisplayName),
        )
    }

    @Test
    fun unknownPackagesFallBackToTheLastSegmentCapitalized() {
        // A future config entry without a table row degrades to a readable
        // single word, never a raw dotted ID.
        assertEquals("Newbrowser", browserDisplayName("com.example.newbrowser"))
        assertEquals("Q", browserDisplayName("org.example.q"))
    }

    @Test
    fun settingsSubtitleNamesThreeThenCountsTheRest() {
        assertEquals(
            "Chrome, Chrome Beta, Samsung Internet + 4 more",
            supportedBrowsersSubtitle(bundledPackages),
        )
        assertEquals(
            "Chrome, Firefox, Opera",
            supportedBrowsersSubtitle(listOf("com.android.chrome", "org.mozilla.firefox", "com.opera.browser")),
        )
        assertEquals("Chrome", supportedBrowsersSubtitle(listOf("com.android.chrome")))
    }
}

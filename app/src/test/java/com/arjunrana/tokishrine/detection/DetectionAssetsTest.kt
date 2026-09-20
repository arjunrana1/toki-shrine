package com.arjunrana.tokishrine.detection

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/*
 * Guards the bundled detection document and the service declarations
 * against drift from PRD §13/§14 without needing a JSON parser on the
 * JVM classpath: the asset is read as text and checked whitespace-blind,
 * the same source-guard pattern as AndroidManifestTest. Full parsing of
 * the real asset runs on-device in AssetDetectionConfigLoaderTest.
 * Gradle runs JVM tests with the module directory as the working
 * directory.
 */
class DetectionAssetsTest {

    private val asset: String =
        File("src/main/assets/${AssetDetectionConfigLoader.DEFAULT_ASSET}").readText()

    // Whitespace-blind view so JSON reformatting cannot break the checks.
    private val normalized: String = asset.filterNot { it.isWhitespace() }

    private val prdBrowsers = listOf(
        "com.android.chrome" to "com.android.chrome:id/url_bar",
        "com.chrome.beta" to "com.chrome.beta:id/url_bar",
        "com.sec.android.app.sbrowser" to "com.sec.android.app.sbrowser:id/location_bar_edit_text",
        "org.mozilla.firefox" to "org.mozilla.firefox:id/mozac_browser_toolbar_url_view",
        "com.brave.browser" to "com.brave.browser:id/url_bar",
        "com.microsoft.emmx" to "com.microsoft.emmx:id/url_bar",
        "com.opera.browser" to "com.opera.browser:id/url_field",
        "com.duckduckgo.mobile.android" to "com.duckduckgo.mobile.android:id/omnibarTextInput",
        "com.vivaldi.browser" to "com.vivaldi.browser:id/url_bar",
    )

    @Test
    fun browserMapCarriesExactlyTheNinePrdBrowsers() {
        prdBrowsers.forEach { (packageName, viewId) ->
            assertTrue(
                "Missing browser entry $packageName → $viewId",
                normalized.contains("\"package\":\"$packageName\",\"urlViewId\":\"$viewId\""),
            )
        }
        // Nine entries, no extras smuggled in.
        val urlViewIdOccurrences = Regex("\"urlViewId\":").findAll(asset).count()
        assertEquals(9, urlViewIdOccurrences)
    }

    @Test
    fun oemBatteryTextShipsInTheAsset() {
        assertTrue(normalized.contains("\"samsung\":"))
        assertTrue(normalized.contains("\"generic\":"))
        assertTrue(
            normalized.contains("\"intro\":\"Androidisaggressiveaboutclosingbackgroundapps."),
        )
        assertTrue(
            normalized.contains("\"intro\":\"Mostphonesonlycloseappsinthebackgroundtosavebattery."),
        )
        // The three authored Samsung steps keep their bold highlights.
        assertTrue(normalized.contains("\"text\":\"Battery\",\"bold\":true"))
        assertTrue(normalized.contains("\"text\":\"Unrestricted\",\"bold\":true"))
        assertTrue(normalized.contains("\"text\":\"Putapptosleep\",\"bold\":true"))
    }

    @Test
    fun serviceConfigSubscribesToWindowEventsAndReportsViewIds() {
        val config = File("src/main/res/xml/accessibility_service_config.xml").readText()
        assertTrue("typeWindowStateChanged must be subscribed", config.contains("typeWindowStateChanged"))
        assertTrue("typeWindowContentChanged must be subscribed", config.contains("typeWindowContentChanged"))
        // findAccessibilityNodeInfosByViewId returns nothing without it.
        assertTrue("flagReportViewIds is required for address-bar lookup", config.contains("flagReportViewIds"))
        assertTrue(config.contains("canRetrieveWindowContent"))
    }

    @Test
    fun placeholderBlockActivityIsDeclaredUnexportedAndRecentsFree() {
        val manifest = File("src/main/AndroidManifest.xml").readText()
        assertTrue(manifest.contains("android:name=\".BlockActivity\""))
        assertTrue(manifest.contains("android:excludeFromRecents=\"true\""))
        val blockActivityBlock = manifest.substringAfter(".BlockActivity").substringBefore("</activity>")
        assertTrue(
            "BlockActivity must not be exported",
            blockActivityBlock.contains("android:exported=\"false\""),
        )
    }
}

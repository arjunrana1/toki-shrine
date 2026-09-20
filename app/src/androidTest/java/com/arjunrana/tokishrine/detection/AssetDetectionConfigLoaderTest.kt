package com.arjunrana.tokishrine.detection

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.arjunrana.tokishrine.data.permissions.BatteryOem
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/*
 * On-device loading of the real bundled detection JSON (PRD §13), plus
 * the parser's failure paths — these need org.json, which the JVM
 * unit-test classpath does not carry. Written in Phase 4; executed at
 * the owner-authorized instrumented run.
 */
@RunWith(AndroidJUnit4::class)
class AssetDetectionConfigLoaderTest {

    private fun loadConfig(): DetectionConfig =
        runBlocking {
            AssetDetectionConfigLoader(ApplicationProvider.getApplicationContext()).load()
        }

    @Test
    fun bundledAssetLoadsTheNineSupportedBrowsers() = runBlocking {
        val config = AssetDetectionConfigLoader(ApplicationProvider.getApplicationContext()).load()
        assertEquals(9, config.browsers.size)
        assertEquals("com.android.chrome:id/url_bar", config.browsers["com.android.chrome"])
        assertEquals(
            "com.sec.android.app.sbrowser:id/location_bar_edit_text",
            config.browsers["com.sec.android.app.sbrowser"],
        )
        assertEquals("com.opera.browser:id/url_field", config.browsers["com.opera.browser"])
        assertEquals(
            "com.duckduckgo.mobile.android:id/omnibarTextInput",
            config.browsers["com.duckduckgo.mobile.android"],
        )
    }

    @Test
    fun bundledOemTextMatchesTheAuthoredCopy() {
        val samsung = loadConfig().batteryInstructions.getValue(BatteryOem.SAMSUNG)
        assertEquals(
            "Android is aggressive about closing background apps. Three quick taps stop it from killing Toki Shrine.",
            samsung.intro,
        )
        assertEquals(3, samsung.steps.size)
        assertTrue(samsung.steps[0].any { it.bold && it.text == "Battery" })
        assertTrue(samsung.steps[1].any { it.bold && it.text == "Unrestricted" })
        assertTrue(samsung.steps[2].any { it.bold && it.text == "Put app to sleep" })

        val generic = loadConfig().batteryInstructions.getValue(BatteryOem.GENERIC)
        assertEquals(
            "Most phones only close apps in the background to save battery. The next screen asks to keep Toki Shrine awake — allow it.",
            generic.intro,
        )
        assertTrue(generic.steps.isEmpty())
    }

    // — parser failure paths —

    @Test
    fun malformedJsonFailsAsDetectionConfigException() {
        assertThrows(DetectionConfigException::class.java) {
            DetectionConfigJsonParser.parse("{ not json ")
        }
    }

    @Test
    fun missingBatterySectionIsRejected() {
        val json = """
            { "browsers": [ { "package": "com.android.chrome", "urlViewId": "com.android.chrome:id/url_bar" } ] }
        """.trimIndent()
        assertThrows(DetectionConfigException::class.java) {
            DetectionConfigJsonParser.parse(json)
        }
    }

    @Test
    fun duplicateBrowserPackageIsRejected() {
        val json = """
            {
              "browsers": [
                { "package": "com.android.chrome", "urlViewId": "com.android.chrome:id/url_bar" },
                { "package": "com.android.chrome", "urlViewId": "com.android.chrome:id/other" }
              ],
              "batteryInstructions": {
                "samsung": { "intro": "i", "steps": [] },
                "generic": { "intro": "i", "steps": [] }
              }
            }
        """.trimIndent()
        assertThrows(DetectionConfigException::class.java) {
            DetectionConfigJsonParser.parse(json)
        }
    }
}

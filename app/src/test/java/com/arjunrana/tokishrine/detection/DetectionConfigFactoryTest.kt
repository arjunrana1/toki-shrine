package com.arjunrana.tokishrine.detection

import com.arjunrana.tokishrine.data.permissions.BatteryInstructions
import com.arjunrana.tokishrine.data.permissions.BatteryOem
import com.arjunrana.tokishrine.data.permissions.BatteryStepPart
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

// Config validation between JSON extraction and use: a partial browser
// map or missing OEM text is a loud build defect, never a silent gap in
// blocking coverage. All failures surface as DetectionConfigException.
class DetectionConfigFactoryTest {

    private val battery: Map<BatteryOem, BatteryInstructions> = mapOf(
        BatteryOem.SAMSUNG to BatteryInstructions("samsung intro", listOf(listOf(BatteryStepPart("tap "), BatteryStepPart("Battery", bold = true)))),
        BatteryOem.GENERIC to BatteryInstructions("generic intro", emptyList()),
    )

    private fun browsers(vararg pairs: Pair<String, String>) =
        pairs.map { RawBrowserEntry(it.first, it.second) }

    @Test
    fun validDocumentProducesTheBrowserMap() {
        val config = DetectionConfigFactory.create(
            browsers(
                "com.android.chrome" to "com.android.chrome:id/url_bar",
                "org.mozilla.firefox" to "org.mozilla.firefox:id/mozac_browser_toolbar_url_view",
            ),
            battery,
        )
        assertEquals(
            mapOf(
                "com.android.chrome" to "com.android.chrome:id/url_bar",
                "org.mozilla.firefox" to "org.mozilla.firefox:id/mozac_browser_toolbar_url_view",
            ),
            config.browsers,
        )
        assertEquals("samsung intro", config.batteryInstructions[BatteryOem.SAMSUNG]?.intro)
        assertEquals("generic intro", config.batteryInstructions[BatteryOem.GENERIC]?.intro)
    }

    @Test
    fun emptyBrowserMapIsRejected() {
        assertThrows(DetectionConfigException::class.java) {
            DetectionConfigFactory.create(emptyList(), battery)
        }
    }

    @Test
    fun blankPackageOrViewIdIsRejected() {
        assertThrows(DetectionConfigException::class.java) {
            DetectionConfigFactory.create(browsers(" " to "com.android.chrome:id/url_bar"), battery)
        }
        assertThrows(DetectionConfigException::class.java) {
            DetectionConfigFactory.create(browsers("com.android.chrome" to "url_bar"), battery)
        }
    }

    @Test
    fun duplicateBrowserPackageIsRejected() {
        assertThrows(DetectionConfigException::class.java) {
            DetectionConfigFactory.create(
                browsers(
                    "com.android.chrome" to "com.android.chrome:id/url_bar",
                    "com.android.chrome" to "com.android.chrome:id/url",
                ),
                battery,
            )
        }
    }

    @Test
    fun missingOemInstructionsAreRejected() {
        assertThrows(DetectionConfigException::class.java) {
            DetectionConfigFactory.create(browsers("com.android.chrome" to "com.android.chrome:id/url_bar"), emptyMap())
        }
    }

    @Test
    fun blankOemTextIsRejected() {
        val blankIntro = battery + (BatteryOem.GENERIC to BatteryInstructions(" ", emptyList()))
        assertThrows(DetectionConfigException::class.java) {
            DetectionConfigFactory.create(browsers("com.android.chrome" to "com.android.chrome:id/url_bar"), blankIntro)
        }
        val blankStepPart = battery + (
            BatteryOem.SAMSUNG to BatteryInstructions("intro", listOf(listOf(BatteryStepPart(" "))))
            )
        assertThrows(DetectionConfigException::class.java) {
            DetectionConfigFactory.create(browsers("com.android.chrome" to "com.android.chrome:id/url_bar"), blankStepPart)
        }
    }
}

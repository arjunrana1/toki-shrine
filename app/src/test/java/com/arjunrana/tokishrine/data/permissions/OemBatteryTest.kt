package com.arjunrana.tokishrine.data.permissions

import org.junit.Assert.assertEquals
import org.junit.Test

// Screen 4's manufacturer logic: Samsung detection is the authored
// default and everything else falls to the universal dialog path. The
// in-app picker override was removed by owner correction (19 September),
// so detection alone decides. Since Phase 4 the authored step content
// lives in the bundled detection JSON; it is pinned by
// DetectionAssetsTest (asset contents) and the on-device
// AssetDetectionConfigLoaderTest, not here.
class OemBatteryTest {

    @Test
    fun samsungIsDetectedCaseInsensitively() {
        assertEquals(BatteryOem.SAMSUNG, OemBattery.detect("samsung"))
        assertEquals(BatteryOem.SAMSUNG, OemBattery.detect("Samsung"))
        assertEquals(BatteryOem.SAMSUNG, OemBattery.detect("SAMSUNG"))
    }

    @Test
    fun otherManufacturersFallToTheGenericPath() {
        assertEquals(BatteryOem.GENERIC, OemBattery.detect("Google"))
        assertEquals(BatteryOem.GENERIC, OemBattery.detect("Xiaomi"))
        assertEquals(BatteryOem.GENERIC, OemBattery.detect(null))
        assertEquals(BatteryOem.GENERIC, OemBattery.detect(""))
    }
}

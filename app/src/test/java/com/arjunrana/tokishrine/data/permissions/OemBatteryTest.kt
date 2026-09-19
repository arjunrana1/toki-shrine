package com.arjunrana.tokishrine.data.permissions

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// Screen 4's manufacturer logic: Samsung detection is the authored
// default and everything else falls to the universal dialog path. The
// in-app picker override was removed by owner correction (19 September),
// so detection alone decides — its case-insensitive Samsung matching and
// the authored step content stay pinned here.
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

    @Test
    fun samsungInstructionsCarryTheThreeAuthoredSteps() {
        val instructions = OemBattery.instructionsFor(BatteryOem.SAMSUNG)

        assertEquals(3, instructions.steps.size)
        assertTrue(instructions.steps[0].any { it.bold && it.text == "Battery" })
        assertTrue(instructions.steps[1].any { it.bold && it.text == "Unrestricted" })
        assertTrue(instructions.steps[2].any { it.bold && it.text == "Put app to sleep" })
    }

    @Test
    fun genericInstructionsHaveNoOemSpecificSteps() {
        val instructions = OemBattery.instructionsFor(BatteryOem.GENERIC)

        assertTrue(instructions.intro.isNotEmpty())
        assertTrue(instructions.steps.isEmpty())
    }

    @Test
    fun everyOemHasInstructions() {
        BatteryOem.values().forEach { oem ->
            assertTrue(OemBattery.instructionsFor(oem).intro.isNotEmpty())
        }
    }
}

package com.arjunrana.tokishrine.data.permissions

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// Screen 4's manufacturer logic: Samsung detection is the authored default,
// and the in-app picker overrides whatever the hardware reports.
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
    fun everyOemHasAPickerLabelAndInstructions() {
        BatteryOem.values().forEach { oem ->
            assertTrue(oem.pickerLabel.isNotEmpty())
            assertTrue(OemBattery.instructionsFor(oem).intro.isNotEmpty())
        }
    }
}

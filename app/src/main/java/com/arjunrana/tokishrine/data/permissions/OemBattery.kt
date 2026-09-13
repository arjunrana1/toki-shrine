package com.arjunrana.tokishrine.data.permissions

/*
 * Battery-instruction content per manufacturer (screen 4, PRD §6/§12).
 * Samsung is the authored default from Build.MANUFACTURER; the in-app
 * "Not a Samsung? Pick your phone" control overrides detection. The
 * generic variant is the universal Android battery-optimisation dialog —
 * the baseline path on every device — so it carries no OEM-specific steps.
 *
 * Phase 4 moves OEM text into the bundled JSON asset alongside the browser
 * map (PRD §13); until then the two authored variants live here.
 */
enum class BatteryOem(val pickerLabel: String) {
    SAMSUNG("Samsung"),
    GENERIC("Another manufacturer"),
}

data class BatteryStepPart(val text: String, val bold: Boolean = false)

data class BatteryInstructions(
    val intro: String,
    val steps: List<List<BatteryStepPart>>,
)

object OemBattery {

    fun detect(manufacturer: String?): BatteryOem =
        if (manufacturer != null && manufacturer.equals("samsung", ignoreCase = true)) {
            BatteryOem.SAMSUNG
        } else {
            BatteryOem.GENERIC
        }

    fun instructionsFor(oem: BatteryOem): BatteryInstructions = when (oem) {
        BatteryOem.SAMSUNG -> BatteryInstructions(
            intro = "Samsung is aggressive about closing background apps. Three quick taps stop it from killing Toki Shrine.",
            steps = listOf(
                listOf(BatteryStepPart("On the next screen, tap "), BatteryStepPart("Battery", bold = true)),
                listOf(BatteryStepPart("Choose "), BatteryStepPart("Unrestricted", bold = true), BatteryStepPart(" for Toki Shrine")),
                listOf(BatteryStepPart("Turn off "), BatteryStepPart("Put app to sleep", bold = true), BatteryStepPart(" if it's on")),
            ),
        )
        BatteryOem.GENERIC -> BatteryInstructions(
            intro = "Most phones only close apps in the background to save battery. The next screen asks to keep Toki Shrine awake — allow it.",
            steps = emptyList(),
        )
    }
}

package com.arjunrana.tokishrine.data.permissions

/*
 * Battery-instruction content per manufacturer (screen 4, PRD §6/§12).
 * Handling is automatic and user-invisible (owner correction, 19
 * September): Samsung hardware from Build.MANUFACTURER gets the authored
 * One UI steps — introduced by a manufacturer-neutral "Android is
 * aggressive…" line — and every other device gets the universal Android
 * battery-optimisation dialog, the baseline path that carries no
 * OEM-specific steps. The former in-app override picker is removed.
 *
 * Phase 4 moves OEM text into the bundled JSON asset alongside the browser
 * map (PRD §13); until then the two authored variants live here.
 */
enum class BatteryOem {
    SAMSUNG,
    GENERIC,
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
            intro = "Android is aggressive about closing background apps. Three quick taps stop it from killing Toki Shrine.",
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

package com.arjunrana.tokishrine.data.permissions

/*
 * Battery-instruction types and manufacturer detection (screen 4, PRD
 * §6/§12). Handling is automatic and user-invisible (owner correction,
 * 19 September): Samsung hardware from Build.MANUFACTURER gets the
 * authored One UI steps and every other device gets the universal Android
 * battery-optimisation dialog. Since Phase 4 the authored text itself
 * lives in the bundled detection JSON (assets/detection_config.json) and
 * reaches the UI through DetectionConfigLoader; only detection remains
 * here.
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
}

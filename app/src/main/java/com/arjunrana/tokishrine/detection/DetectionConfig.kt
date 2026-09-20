package com.arjunrana.tokishrine.detection

import com.arjunrana.tokishrine.data.permissions.BatteryInstructions
import com.arjunrana.tokishrine.data.permissions.BatteryOem

// The bundled detection configuration (PRD §13): the supported-browser
// package → address-bar view-ID map, plus the per-manufacturer battery
// text that used to live in OemBattery. Both ship as JSON in
// assets/detection_config.json and reach the app only through
// DetectionConfigLoader, so a future remote loader is a loader change and
// nothing else.
data class DetectionConfig(
    val browsers: Map<String, String>,
    val batteryInstructions: Map<BatteryOem, BatteryInstructions>,
)

// Raised for any structurally loaded but invalid document — a browser map
// that could silently miss entries is worse than a loud failure, because
// §10's url_read_failed canary can only surface browsers the map declares.
class DetectionConfigException(message: String) : IllegalStateException(message)

// One raw browser entry between JSON extraction and validation. Keeping
// extraction (org.json, Android-only) separate from validation (pure
// Kotlin, JVM-testable) lets the failure modes be pinned down without a
// JSON parser on the unit-test classpath.
data class RawBrowserEntry(val packageName: String, val urlViewId: String)

object DetectionConfigFactory {

    // Validates the extracted document and builds the immutable config.
    // Every rule fails loudly rather than dropping the offending entry: a
    // partial browser map would block some browsers and not others with no
    // visible difference to the user. All failures surface as
    // DetectionConfigException.
    fun create(
        browsers: List<RawBrowserEntry>,
        batteryInstructions: Map<BatteryOem, BatteryInstructions>,
    ): DetectionConfig = try {
        createValidated(browsers, batteryInstructions)
    } catch (e: IllegalArgumentException) {
        throw DetectionConfigException(e.message ?: "Invalid detection configuration")
    }

    private fun createValidated(
        browsers: List<RawBrowserEntry>,
        batteryInstructions: Map<BatteryOem, BatteryInstructions>,
    ): DetectionConfig {
        require(browsers.isNotEmpty()) { "Browser map must not be empty" }
        val browserMap = LinkedHashMap<String, String>()
        browsers.forEach { entry ->
            require(entry.packageName.isNotBlank()) {
                "Browser package name must not be blank"
            }
            require(entry.urlViewId.contains(":id/")) {
                "Address-bar view ID '${entry.urlViewId}' must be a fully qualified res/id (package:id/name)"
            }
            require(browserMap.put(entry.packageName, entry.urlViewId) == null) {
                "Browser package '${entry.packageName}' appears twice in the map"
            }
        }
        BatteryOem.values().forEach { oem ->
            val instructions = batteryInstructions[oem]
                ?: throw DetectionConfigException("Battery instructions for '${oem.name.lowercase()}' are missing")
            validateInstructions(oem, instructions)
        }
        return DetectionConfig(browserMap, batteryInstructions)
    }

    private fun validateInstructions(oem: BatteryOem, instructions: BatteryInstructions) {
        val key = oem.name.lowercase()
        require(instructions.intro.isNotBlank()) { "Battery intro for '$key' must not be blank" }
        instructions.steps.forEachIndexed { stepIndex, step ->
            require(step.isNotEmpty()) { "Battery step ${stepIndex + 1} for '$key' must have at least one part" }
            step.forEachIndexed { partIndex, part ->
                require(part.text.isNotBlank()) {
                    "Battery step ${stepIndex + 1} part ${partIndex + 1} for '$key' must not be blank"
                }
            }
        }
    }

}

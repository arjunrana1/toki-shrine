package com.arjunrana.tokishrine.detection

import android.content.Context
import com.arjunrana.tokishrine.data.permissions.BatteryInstructions
import com.arjunrana.tokishrine.data.permissions.BatteryStepPart
import com.arjunrana.tokishrine.data.permissions.BatteryOem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

// The one replaceable seam over the bundled detection configuration
// (PRD §13): callers depend on this interface, not on the asset. Today it
// is backed by assets/detection_config.json; a Remote Config loader later
// is a new implementation and nothing else.
interface DetectionConfigLoader {
    suspend fun load(): DetectionConfig
}

// Reads and caches the bundled asset. Parsing happens once on first load;
// the result is immutable, so every later load returns the same instance.
class AssetDetectionConfigLoader(
    context: Context,
    private val assetName: String = DEFAULT_ASSET,
) : DetectionConfigLoader {

    private val assets = context.applicationContext.assets

    @Volatile
    private var cached: DetectionConfig? = null

    override suspend fun load(): DetectionConfig =
        cached ?: withContext(Dispatchers.IO) {
            cached ?: DetectionConfigJsonParser.parse(assets.open(assetName).bufferedReader().use { it.readText() })
                .also { cached = it }
        }

    companion object {
        const val DEFAULT_ASSET = "detection_config.json"
    }
}

// org.json extraction from the asset document into the raw Kotlin shapes
// DetectionConfigFactory validates. Malformed JSON surfaces as
// DetectionConfigException so a corrupted asset cannot crash the service;
// callers degrade to app-only detection instead. Lives here (not in JVM
// unit tests) because org.json is an Android platform artifact.
object DetectionConfigJsonParser {

    fun parse(json: String): DetectionConfig {
        val root = try {
            JSONObject(json)
        } catch (e: JSONException) {
            throw DetectionConfigException("Detection config is not valid JSON: ${e.message}")
        }
        return try {
            val browsers = parseBrowsers(root.getJSONArray("browsers"))
            val battery = parseBattery(root.getJSONObject("batteryInstructions"))
            DetectionConfigFactory.create(browsers, battery)
        } catch (e: JSONException) {
            // Missing keys and type mismatches land here; the factory's own
            // validation failures already carry their own messages.
            throw DetectionConfigException("Detection config is missing or mis-shaped: ${e.message}")
        }
    }

    private fun parseBrowsers(array: JSONArray): List<RawBrowserEntry> =
        (0 until array.length()).map { i ->
            val entry = array.getJSONObject(i)
            RawBrowserEntry(
                packageName = entry.getString("package"),
                urlViewId = entry.getString("urlViewId"),
            )
        }

    private fun parseBattery(root: JSONObject): Map<BatteryOem, BatteryInstructions> =
        BatteryOem.values().associateWith { oem ->
            val oemJson = root.getJSONObject(oem.name.lowercase())
            val stepsJson = oemJson.getJSONArray("steps")
            BatteryInstructions(
                intro = oemJson.getString("intro"),
                steps = (0 until stepsJson.length()).map { stepIndex ->
                    val step = stepsJson.getJSONArray(stepIndex)
                    (0 until step.length()).map { partIndex ->
                        parseStepPart(step.get(partIndex))
                    }
                },
            )
        }

    // A step part is either a plain string or an object with text/bold.
    private fun parseStepPart(raw: Any): BatteryStepPart = when (raw) {
        is String -> BatteryStepPart(raw)
        is JSONObject -> BatteryStepPart(
            text = raw.getString("text"),
            bold = raw.optBoolean("bold", false),
        )
        else -> throw DetectionConfigException("Battery step part must be a string or an object with text/bold")
    }
}

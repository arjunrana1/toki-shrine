package com.arjunrana.tokishrine.ui.navigation

import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.snapshots.SnapshotStateList

// How the permission checklist was reached, which drives chrome and
// onboarding events: ONBOARDING — first run, from the welcome screen;
// GATE — an ON toggle tapped without accessibility (PRD §12: the gate falls
// on activation, never before); SETTINGS — the Settings permission-health
// row reuses the checklist component.
enum class ChecklistMode { ONBOARDING, GATE, SETTINGS }

// Hand-rolled navigation routes (AGENTS.md §2: no navigation dependency).
sealed interface Route {
    data object BlockList : Route
    // startStep: the create flow's entry step (PRD §17 R9) — 1 for the
    // contents editor, 3 when detail's THE FRICTION → Edit opens it
    // directly at configuration.
    data class Create(val editBlockId: Long? = null, val startStep: Int = 1) : Route
    data class TurnOn(val blockId: Long) : Route
    data class Detail(val blockId: Long) : Route
    data class Checklist(val mode: ChecklistMode) : Route
    data object AccessibilityExplainer : Route
    data object BatteryInstructions : Route
    data object Settings : Route
    data object Welcome : Route
}

// Minimal string codec so the route stack survives activity recreation
// through rememberSaveable (review blocker 1) without persisting objects.
// The encodings are private shapes; round trips are pinned in
// RouteCodecTest. Garbage decodes to null so a damaged stack falls back to
// the initial screen instead of crashing.
object RouteCodec {

    fun encode(route: Route): String = when (route) {
        Route.BlockList -> "blockList"
        Route.Welcome -> "welcome"
        Route.Settings -> "settings"
        Route.AccessibilityExplainer -> "explainer"
        Route.BatteryInstructions -> "battery"
        is Route.Checklist -> "checklist:${route.mode.name}"
        is Route.TurnOn -> "turnOn:${route.blockId}"
        is Route.Detail -> "detail:${route.blockId}"
        is Route.Create -> "create:${route.editBlockId ?: -1L}:${route.startStep}"
    }

    fun decode(encoded: String): Route? {
        val parts = encoded.split(":")
        return when (parts.firstOrNull()) {
            "blockList" -> Route.BlockList
            "welcome" -> Route.Welcome
            "settings" -> Route.Settings
            "explainer" -> Route.AccessibilityExplainer
            "battery" -> Route.BatteryInstructions
            "checklist" -> parts.getOrNull(1)?.let { mode ->
                ChecklistMode.values().firstOrNull { it.name == mode }?.let { Route.Checklist(it) }
            }
            "turnOn" -> parts.getOrNull(1)?.toLongOrNull()?.let { Route.TurnOn(it) }
            "detail" -> parts.getOrNull(1)?.toLongOrNull()?.let { Route.Detail(it) }
            "create" -> {
                val editBlockId = parts.getOrNull(1)?.toLongOrNull() ?: return null
                val startStep = parts.getOrNull(2)?.toIntOrNull() ?: return null
                Route.Create(if (editBlockId == -1L) null else editBlockId, startStep)
            }
            else -> null
        }
    }
}

// The saveable form of the whole route stack: one encoded string per route,
// restored in order. An entirely undecodable restore yields null so
// rememberSaveable falls back to the initial stack. ArrayList<String> is the
// Bundle-storable shape.
val RouteStackSaver: Saver<SnapshotStateList<Route>, ArrayList<String>> = Saver(
    save = { stack ->
        ArrayList<String>().apply { stack.forEach { add(RouteCodec.encode(it)) } }
    },
    restore = { saved ->
        val routes = saved.mapNotNull { RouteCodec.decode(it) }
        if (routes.isEmpty()) null else SnapshotStateList<Route>().apply { addAll(routes) }
    },
)

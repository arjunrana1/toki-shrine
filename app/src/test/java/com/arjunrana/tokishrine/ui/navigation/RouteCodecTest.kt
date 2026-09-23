package com.arjunrana.tokishrine.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

// Route encode/restore round trips for the saveable navigation stack
// (review blocker 1): every Phase 3 flow position — checklist mode, block
// id, explainer/battery routes, onboarding welcome — must survive
// recreation exactly, and damaged input must fall back to null rather than
// crash or invent a route.
class RouteCodecTest {

    private val samples = listOf(
        Route.BlockList,
        Route.Welcome,
        Route.Settings,
        Route.Stats,
        Route.Feedback,
        Route.AccessibilityExplainer,
        Route.BatteryInstructions,
        Route.Checklist(ChecklistMode.ONBOARDING),
        Route.Checklist(ChecklistMode.GATE),
        Route.Checklist(ChecklistMode.SETTINGS),
        Route.TurnOn(42L),
        Route.TurnOn(Long.MAX_VALUE),
        Route.Detail(7L),
        Route.Create(null, 1),
        Route.Create(5L, 3),
    )

    @Test
    fun everyRouteRoundTripsExactly() {
        samples.forEach { route ->
            assertEquals(route, RouteCodec.decode(RouteCodec.encode(route)))
        }
    }

    @Test
    fun reencodingARestoredRouteIsStable() {
        samples.forEach { route ->
            val restored = RouteCodec.decode(RouteCodec.encode(route))
            assertEquals(route, RouteCodec.decode(RouteCodec.encode(restored!!)))
        }
    }

    @Test
    fun damagedInputDecodesToNull() {
        listOf(
            "",
            "nope",
            "blocklist",
            "turnOn",
            "turnOn:",
            "turnOn:abc",
            "detail:",
            "detail:x",
            "checklist",
            "checklist:",
            "checklist:NOPE",
            "create",
            "create:1",
            "create:a:3",
            "create:1:b",
        ).forEach { damaged ->
            assertNull("expected null for \"$damaged\"", RouteCodec.decode(damaged))
        }
    }

    @Test
    fun createEncodesNullEditBlockIdViaTheSentinel() {
        assertEquals(
            Route.Create(editBlockId = null, startStep = 1),
            RouteCodec.decode(RouteCodec.encode(Route.Create(editBlockId = null, startStep = 1))),
        )
        assertEquals(
            Route.Create(editBlockId = 1L, startStep = 1),
            RouteCodec.decode(RouteCodec.encode(Route.Create(editBlockId = 1L, startStep = 1))),
        )
    }
}

package com.arjunrana.tokishrine.data.permissions

import com.arjunrana.tokishrine.data.repo.EventRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

// Pins the §10 permission event sequences driven by the Phase 3 checklist:
// requested → granted/denied exactly once per outstanding request.
class PermissionEventLogicTest {

    private data class Emitted(val name: String, val params: Map<String, Any?>)

    private fun logic(events: MutableList<Emitted>) = PermissionEventLogic { name, params ->
        events.add(Emitted(name, params))
    }

    @Test
    fun requestThenResumeGrantedEmitsRequestedThenGranted() = runBlocking {
        val events = mutableListOf<Emitted>()
        val logic = logic(events)

        logic.requested(AppPermission.ACCESSIBILITY)
        logic.settle { it == AppPermission.ACCESSIBILITY }

        assertEquals(
            listOf(
                Emitted(EventRepository.EVENT_PERMISSION_REQUESTED, mapOf("permission" to "accessibility")),
                Emitted(EventRepository.EVENT_PERMISSION_GRANTED, mapOf("permission" to "accessibility")),
            ),
            events,
        )
    }

    @Test
    fun requestThenResumeStillMissingEmitsDenied() = runBlocking {
        val events = mutableListOf<Emitted>()
        val logic = logic(events)

        logic.requested(AppPermission.OVERLAY)
        logic.settle { false }

        assertEquals(
            listOf(
                Emitted(EventRepository.EVENT_PERMISSION_REQUESTED, mapOf("permission" to "overlay")),
                Emitted(EventRepository.EVENT_PERMISSION_DENIED, mapOf("permission" to "overlay")),
            ),
            events,
        )
    }

    @Test
    fun resumeWithoutOutstandingRequestsEmitsNothing() = runBlocking {
        val events = mutableListOf<Emitted>()
        val logic = logic(events)

        logic.settle { true }

        assertEquals(emptyList<Emitted>(), events)
    }

    @Test
    fun notificationsResolveFromDialogCallbackExactlyOnce() = runBlocking {
        val events = mutableListOf<Emitted>()
        val logic = logic(events)

        logic.requested(AppPermission.NOTIFICATIONS)
        logic.resolve(AppPermission.NOTIFICATIONS, granted = true)
        // The activity also resumes after the dialog; the request is
        // already settled and must not emit a second outcome.
        logic.settle { true }

        assertEquals(
            listOf(
                Emitted(EventRepository.EVENT_PERMISSION_REQUESTED, mapOf("permission" to "notifications")),
                Emitted(EventRepository.EVENT_PERMISSION_GRANTED, mapOf("permission" to "notifications")),
            ),
            events,
        )
    }

    @Test
    fun resolveWithoutAnOutstandingRequestEmitsNothing() = runBlocking {
        val events = mutableListOf<Emitted>()
        val logic = logic(events)

        logic.resolve(AppPermission.BATTERY, granted = true)

        assertEquals(emptyList<Emitted>(), events)
    }

    @Test
    fun multipleOutstandingRequestsSettleInRequestOrder() = runBlocking {
        val events = mutableListOf<Emitted>()
        val logic = logic(events)

        logic.requested(AppPermission.ACCESSIBILITY)
        logic.requested(AppPermission.BATTERY)
        logic.settle { it == AppPermission.ACCESSIBILITY }

        assertEquals(
            listOf(
                Emitted(EventRepository.EVENT_PERMISSION_REQUESTED, mapOf("permission" to "accessibility")),
                Emitted(EventRepository.EVENT_PERMISSION_REQUESTED, mapOf("permission" to "battery")),
                Emitted(EventRepository.EVENT_PERMISSION_GRANTED, mapOf("permission" to "accessibility")),
                Emitted(EventRepository.EVENT_PERMISSION_DENIED, mapOf("permission" to "battery")),
            ),
            events,
        )
    }

    @Test
    fun aRequestSettlesOnceAndCanOnlyBeSettledAgainByANewRequest() = runBlocking {
        val events = mutableListOf<Emitted>()
        val logic = logic(events)

        logic.requested(AppPermission.OVERLAY)
        logic.settle { false }
        logic.settle { true }

        logic.requested(AppPermission.OVERLAY)
        logic.settle { true }

        assertEquals(
            listOf(
                Emitted(EventRepository.EVENT_PERMISSION_REQUESTED, mapOf("permission" to "overlay")),
                Emitted(EventRepository.EVENT_PERMISSION_DENIED, mapOf("permission" to "overlay")),
                Emitted(EventRepository.EVENT_PERMISSION_REQUESTED, mapOf("permission" to "overlay")),
                Emitted(EventRepository.EVENT_PERMISSION_GRANTED, mapOf("permission" to "overlay")),
            ),
            events,
        )
    }
}

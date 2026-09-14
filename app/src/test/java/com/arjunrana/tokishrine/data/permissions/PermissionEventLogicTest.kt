package com.arjunrana.tokishrine.data.permissions

import com.arjunrana.tokishrine.data.repo.EventRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

// Pins the §10 permission event sequences driven by the Phase 3 checklist:
// requested → granted/denied exactly once per outstanding request —
// including requests restored after activity recreation (review blocker 1),
// where the pending list is rebuilt from its saved values.
class PermissionEventLogicTest {

    private data class Emitted(val name: String, val params: Map<String, Any?>)

    // The pending list is injected in production as a saveable snapshot
    // list; tests drive both the fresh and the restored (pre-populated)
    // shapes through the same constructor.
    private fun logic(
        events: MutableList<Emitted>,
        pending: MutableList<AppPermission> = mutableListOf(),
    ) = PermissionEventLogic(pending) { name, params ->
        events.add(Emitted(name, params))
    }

    // The call-site sequence used by MainActivity's requestPermission:
    // synchronous mark, durable requested record, then the system UI.
    private suspend fun request(logic: PermissionEventLogic, permission: AppPermission) {
        logic.markPending(permission)
        logic.emitRequested(permission)
    }

    @Test
    fun requestThenResumeGrantedEmitsRequestedThenGranted() = runBlocking {
        val events = mutableListOf<Emitted>()
        val logic = logic(events)

        request(logic, AppPermission.ACCESSIBILITY)
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

        request(logic, AppPermission.OVERLAY)
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

        request(logic, AppPermission.NOTIFICATIONS)
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

        request(logic, AppPermission.ACCESSIBILITY)
        request(logic, AppPermission.BATTERY)
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

        request(logic, AppPermission.OVERLAY)
        logic.settle { false }
        // A second resume right after must not re-emit the outcome.
        logic.settle { true }

        request(logic, AppPermission.OVERLAY)
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

    // — restoration (review blocker 1): the pending list arrives
    // pre-populated from its saved values —

    @Test
    fun restoredAccessibilityRequestSettlesExactlyOnceFromRealState() = runBlocking {
        val events = mutableListOf<Emitted>()
        // Simulates recreation: the saveable list was restored with the
        // outstanding accessibility request still pending.
        val logic = logic(events, pending = mutableListOf(AppPermission.ACCESSIBILITY))

        logic.settle { true }
        logic.settle { true }

        assertEquals(
            listOf(
                Emitted(EventRepository.EVENT_PERMISSION_GRANTED, mapOf("permission" to "accessibility")),
            ),
            events,
        )
    }

    @Test
    fun restoredRequestStillMissingSettlesDeniedOnce() = runBlocking {
        val events = mutableListOf<Emitted>()
        val logic = logic(events, pending = mutableListOf(AppPermission.BATTERY))

        logic.settle { false }
        logic.settle { false }

        assertEquals(
            listOf(
                Emitted(EventRepository.EVENT_PERMISSION_DENIED, mapOf("permission" to "battery")),
            ),
            events,
        )
    }

    @Test
    fun restoredRequestCannotEmitADuplicateResultAfterCallbackAndResume() = runBlocking {
        val events = mutableListOf<Emitted>()
        val logic = logic(events, pending = mutableListOf(AppPermission.NOTIFICATIONS))

        logic.resolve(AppPermission.NOTIFICATIONS, granted = true)
        logic.settle { true }

        assertEquals(
            listOf(
                Emitted(EventRepository.EVENT_PERMISSION_GRANTED, mapOf("permission" to "notifications")),
            ),
            events,
        )
    }

    @Test
    fun multipleRestoredRequestsEachSettleOnce() = runBlocking {
        val events = mutableListOf<Emitted>()
        val logic = logic(
            events,
            pending = mutableListOf(AppPermission.ACCESSIBILITY, AppPermission.OVERLAY, AppPermission.BATTERY),
        )

        logic.settle { it == AppPermission.OVERLAY }

        assertEquals(
            listOf(
                Emitted(EventRepository.EVENT_PERMISSION_DENIED, mapOf("permission" to "accessibility")),
                Emitted(EventRepository.EVENT_PERMISSION_GRANTED, mapOf("permission" to "overlay")),
                Emitted(EventRepository.EVENT_PERMISSION_DENIED, mapOf("permission" to "battery")),
            ),
            events,
        )
    }

    @Test
    fun unmarkingARolledBackRequestPreventsAnyOutcome() = runBlocking {
        val events = mutableListOf<Emitted>()
        val logic = logic(events)

        logic.markPending(AppPermission.OVERLAY)
        // The requested record or the launch failed; the mark is rolled
        // back, so no outcome may settle later.
        logic.unmarkPending(AppPermission.OVERLAY)
        logic.settle { true }

        assertEquals(emptyList<Emitted>(), events)
    }

    @Test
    fun markingTwiceStillYieldsASingleOutcome() = runBlocking {
        val events = mutableListOf<Emitted>()
        val logic = logic(events)

        logic.markPending(AppPermission.ACCESSIBILITY)
        logic.markPending(AppPermission.ACCESSIBILITY)
        logic.emitRequested(AppPermission.ACCESSIBILITY)
        logic.settle { true }

        assertEquals(
            listOf(
                Emitted(EventRepository.EVENT_PERMISSION_REQUESTED, mapOf("permission" to "accessibility")),
                Emitted(EventRepository.EVENT_PERMISSION_GRANTED, mapOf("permission" to "accessibility")),
            ),
            events,
        )
    }

    // — outcome durability (review round 2): a failed or cancelled event
    // write keeps the pending identity so the outcome can be retried, and
    // a written outcome is never repeated —

    @Test
    fun failedResolveRetainsPendingAndARetryEmitsExactlyOnce() = runBlocking {
        val events = mutableListOf<Emitted>()
        var failNextOutcome = true
        val logic = PermissionEventLogic(mutableListOf(AppPermission.OVERLAY)) { name, params ->
            if (failNextOutcome) {
                failNextOutcome = false
                throw IllegalStateException("event write failed")
            }
            events.add(Emitted(name, params))
        }

        try {
            logic.resolve(AppPermission.OVERLAY, granted = true)
            throw AssertionError("resolve must propagate the event-write failure")
        } catch (_: IllegalStateException) {
        }
        assertEquals(emptyList<Emitted>(), events)

        // The identity survived the failed write: the retry emits exactly
        // once, clears it, and a later resume cannot repeat the outcome.
        logic.resolve(AppPermission.OVERLAY, granted = true)
        logic.settle { true }

        assertEquals(
            listOf(Emitted(EventRepository.EVENT_PERMISSION_GRANTED, mapOf("permission" to "overlay"))),
            events,
        )
    }

    @Test
    fun cancelledResolveRetainsPendingForRetry() = runBlocking {
        val events = mutableListOf<Emitted>()
        var cancelNextOutcome = true
        val logic = PermissionEventLogic(mutableListOf(AppPermission.NOTIFICATIONS)) { name, params ->
            if (cancelNextOutcome) {
                cancelNextOutcome = false
                throw CancellationException("cancelled mid-write")
            }
            events.add(Emitted(name, params))
        }

        try {
            logic.resolve(AppPermission.NOTIFICATIONS, granted = false)
        } catch (_: CancellationException) {
        }
        assertEquals(emptyList<Emitted>(), events)

        logic.resolve(AppPermission.NOTIFICATIONS, granted = false)
        logic.settle { true }

        assertEquals(
            listOf(Emitted(EventRepository.EVENT_PERMISSION_DENIED, mapOf("permission" to "notifications"))),
            events,
        )
    }

    @Test
    fun settleWriteFailureRetainsThatRequestForRetry() = runBlocking {
        val events = mutableListOf<Emitted>()
        var failNextOutcome = true
        val logic = PermissionEventLogic(mutableListOf(AppPermission.BATTERY)) { name, params ->
            if (failNextOutcome) {
                failNextOutcome = false
                throw IllegalStateException("event write failed")
            }
            events.add(Emitted(name, params))
        }

        try {
            logic.settle { true }
            throw AssertionError("settle must propagate the event-write failure")
        } catch (_: IllegalStateException) {
        }
        assertEquals(emptyList<Emitted>(), events)

        logic.settle { false }
        logic.settle { true }

        assertEquals(
            listOf(Emitted(EventRepository.EVENT_PERMISSION_DENIED, mapOf("permission" to "battery"))),
            events,
        )
    }

    @Test
    fun settleStateCheckFailureRetainsEverythingForRetry() = runBlocking {
        val events = mutableListOf<Emitted>()
        val logic = logic(events, pending = mutableListOf(AppPermission.ACCESSIBILITY))

        try {
            logic.settle { throw IllegalStateException("settings read failed") }
            throw AssertionError("settle must propagate the state-check failure")
        } catch (_: IllegalStateException) {
        }
        assertEquals(emptyList<Emitted>(), events)

        logic.settle { true }

        assertEquals(
            listOf(Emitted(EventRepository.EVENT_PERMISSION_GRANTED, mapOf("permission" to "accessibility"))),
            events,
        )
    }

    @Test
    fun settleRemovesWrittenItemsAndKeepsFailedAndUnprocessedPending() = runBlocking {
        val events = mutableListOf<Emitted>()
        var outcomeWrites = 0
        val logic = PermissionEventLogic(
            mutableListOf(AppPermission.ACCESSIBILITY, AppPermission.BATTERY, AppPermission.OVERLAY),
        ) { name, params ->
            if (name != EventRepository.EVENT_PERMISSION_REQUESTED) {
                outcomeWrites += 1
                // First outcome writes and is removed; the second (battery)
                // fails and must stay pending along with the unprocessed
                // third (overlay).
                if (outcomeWrites == 2) throw IllegalStateException("event write failed")
            }
            events.add(Emitted(name, params))
        }

        try {
            logic.settle { it == AppPermission.ACCESSIBILITY }
        } catch (_: IllegalStateException) {
        }
        assertEquals(
            listOf(Emitted(EventRepository.EVENT_PERMISSION_GRANTED, mapOf("permission" to "accessibility"))),
            events,
        )

        // Retry settles battery and overlay exactly once; the already
        // written accessibility outcome cannot repeat.
        logic.settle { true }
        logic.settle { true }

        assertEquals(
            listOf(
                Emitted(EventRepository.EVENT_PERMISSION_GRANTED, mapOf("permission" to "accessibility")),
                Emitted(EventRepository.EVENT_PERMISSION_GRANTED, mapOf("permission" to "battery")),
                Emitted(EventRepository.EVENT_PERMISSION_GRANTED, mapOf("permission" to "overlay")),
            ),
            events,
        )
    }
}

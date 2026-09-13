package com.arjunrana.tokishrine.data.permissions

import com.arjunrana.tokishrine.data.repo.EventRepository

/*
 * Tracks which permissions have an outstanding system request and settles
 * them against actual system state, emitting the §10 permission events:
 * permission_requested when a request is launched, permission_granted /
 * permission_denied when the outcome becomes observable.
 *
 * Pure logic over injected callbacks so the event sequences are pinned in
 * JVM tests; the activity shell supplies the real system-state reader and
 * the event-store writer.
 */
class PermissionEventLogic(
    private val emitEvent: suspend (name: String, params: Map<String, Any?>) -> Unit,
) {
    private val pendingRequests = LinkedHashSet<AppPermission>()

    suspend fun requested(permission: AppPermission) {
        pendingRequests.add(permission)
        emitEvent(
            EventRepository.EVENT_PERMISSION_REQUESTED,
            mapOf("permission" to permission.eventValue),
        )
    }

    // Immediate outcome path for the notifications runtime dialog, whose
    // result arrives through the activity-result callback instead of an
    // app resume. Emits only when a request is actually outstanding, so a
    // stray callback cannot fabricate an outcome event.
    suspend fun resolve(permission: AppPermission, granted: Boolean) {
        if (!pendingRequests.remove(permission)) return
        emitOutcome(permission, granted)
    }

    // Outcome path for permissions granted via system settings screens:
    // the app resumes with the request either reflected in system state or
    // still missing, and each outstanding request settles exactly once.
    suspend fun settle(isGranted: (AppPermission) -> Boolean) {
        val outstanding = pendingRequests.toList()
        pendingRequests.clear()
        outstanding.forEach { permission ->
            emitOutcome(permission, isGranted(permission))
        }
    }

    private suspend fun emitOutcome(permission: AppPermission, granted: Boolean) {
        emitEvent(
            if (granted) EventRepository.EVENT_PERMISSION_GRANTED else EventRepository.EVENT_PERMISSION_DENIED,
            mapOf("permission" to permission.eventValue),
        )
    }
}

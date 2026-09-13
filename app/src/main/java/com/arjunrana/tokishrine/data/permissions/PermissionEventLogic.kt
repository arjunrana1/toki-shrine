package com.arjunrana.tokishrine.data.permissions

import com.arjunrana.tokishrine.data.repo.EventRepository

/*
 * Tracks which permissions have an outstanding system request and settles
 * them against actual system state, emitting the §10 permission events:
 * permission_requested when a system UI is launched, permission_granted /
 * permission_denied when the outcome becomes observable.
 *
 * The pending list is injected so the shell can back it with a saveable
 * snapshot list: outstanding request identities survive activity
 * recreation (review blocker 1), and after restoration each settles
 * exactly once from real system state. Call order is load-bearing —
 * markPending happens synchronously before the system UI is launched,
 * emitRequested durably records the request, and unmarkPending rolls the
 * mark back when the record or the launch fails so no phantom outcome is
 * ever settled. Pure logic over injected callbacks; the sequences are
 * pinned in JVM tests.
 */
class PermissionEventLogic(
    private val pendingRequests: MutableList<AppPermission>,
    private val emitEvent: suspend (name: String, params: Map<String, Any?>) -> Unit,
) {

    fun markPending(permission: AppPermission) {
        if (!pendingRequests.contains(permission)) pendingRequests.add(permission)
    }

    fun unmarkPending(permission: AppPermission) {
        pendingRequests.remove(permission)
    }

    suspend fun emitRequested(permission: AppPermission) {
        emitEvent(
            EventRepository.EVENT_PERMISSION_REQUESTED,
            mapOf("permission" to permission.eventValue),
        )
    }

    // Immediate outcome path for the notifications runtime dialog, whose
    // result arrives through the activity-result callback instead of an app
    // resume. Emits only when a request is actually outstanding, so a
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

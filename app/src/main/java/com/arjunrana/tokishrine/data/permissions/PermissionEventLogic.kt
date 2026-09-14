package com.arjunrana.tokishrine.data.permissions

import com.arjunrana.tokishrine.data.repo.EventRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/*
 * Tracks which permissions have an outstanding system request and settles
 * them against actual system state, emitting the §10 permission events:
 * permission_requested when a system UI is launched, permission_granted /
 * permission_denied when the outcome becomes observable.
 *
 * The pending list is injected so the shell can back it with a saveable
 * snapshot list: outstanding request identities survive activity
 * recreation, and after restoration each settles exactly once from real
 * system state. Outcomes are durable — a pending identity is removed only
 * after its own outcome write returns successfully, so a cancelled or
 * failed write keeps the request pending and the next resume retries it;
 * once written, it can never be emitted twice.
 *
 * Pure logic over injected callbacks; the sequences are pinned in JVM
 * tests. Call order for launching a system UI is load-bearing and owned by
 * the shell: markPending happens synchronously before the launch,
 * emitRequested durably records the request, and unmarkPending rolls the
 * mark back when the record or the launch fails so no phantom outcome is
 * ever settled.
 */
class PermissionEventLogic(
    private val pendingRequests: MutableList<AppPermission>,
    private val emitEvent: suspend (name: String, params: Map<String, Any?>) -> Unit,
) {

    // Serializes the two settlement paths across their suspended event
    // writes, so a resolve() arriving while a settle() is mid-write cannot
    // observe a not-yet-removed identity and double-emit its outcome.
    private val settlement = Mutex()

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
    // resume. Emits only when a request is actually outstanding, and the
    // identity is removed only after the outcome write succeeds — a failed
    // or cancelled write leaves it pending for the retry.
    suspend fun resolve(permission: AppPermission, granted: Boolean) {
        settlement.withLock {
            if (!pendingRequests.contains(permission)) return@withLock
            emitOutcome(permission, granted)
            pendingRequests.remove(permission)
        }
    }

    // Outcome path for permissions granted via system settings screens: on
    // resume each outstanding request settles in order, and each identity
    // is removed only after its own outcome write succeeds. A failed or
    // cancelled write — or state check — leaves that permission and every
    // unprocessed one pending for the next resume; successes before it stay
    // settled.
    suspend fun settle(isGranted: (AppPermission) -> Boolean) {
        settlement.withLock {
            val outstanding = pendingRequests.toList()
            for (permission in outstanding) {
                val granted = isGranted(permission)
                emitOutcome(permission, granted)
                pendingRequests.remove(permission)
            }
        }
    }

    private suspend fun emitOutcome(permission: AppPermission, granted: Boolean) {
        emitEvent(
            if (granted) EventRepository.EVENT_PERMISSION_GRANTED else EventRepository.EVENT_PERMISSION_DENIED,
            mapOf("permission" to permission.eventValue),
        )
    }
}

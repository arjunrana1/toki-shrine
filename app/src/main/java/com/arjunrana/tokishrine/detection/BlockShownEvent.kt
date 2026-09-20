package com.arjunrana.tokishrine.detection

import com.arjunrana.tokishrine.data.repo.EventRepository

// Canonical data for the §10 block_screen_shown event. Construction happens
// only when BlockActivity reaches its resumed lifecycle, so an accepted launch
// request cannot be mistaken for a screen that was actually presented.
data class BlockShownEvent(
    val blockId: Long,
    val triggerType: String,
    val target: String,
    val latencyMs: Long,
) {
    companion object {
        fun create(
            blockId: Long,
            triggerType: String?,
            target: String?,
            startedAtElapsedMs: Long,
            shownAtElapsedMs: Long,
        ): BlockShownEvent? {
            val canonicalType = triggerType?.takeIf {
                it == EventRepository.TARGET_TYPE_APP || it == EventRepository.TARGET_TYPE_SITE
            }
            val canonicalTarget = target?.takeIf { it.isNotBlank() }
            if (blockId <= 0 || canonicalType == null || canonicalTarget == null || startedAtElapsedMs < 0) {
                return null
            }
            return BlockShownEvent(
                blockId = blockId,
                triggerType = canonicalType,
                target = canonicalTarget,
                latencyMs = (shownAtElapsedMs - startedAtElapsedMs).coerceAtLeast(0),
            )
        }
    }
}

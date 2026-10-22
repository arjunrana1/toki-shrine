package com.arjunrana.timeshrine.challenge

import com.arjunrana.timeshrine.data.db.BlockWithContents
import com.arjunrana.timeshrine.data.repo.EventRepository

fun isValidDetectionLaunch(
    block: BlockWithContents,
    triggerType: String?,
    target: String?,
): Boolean {
    if (!block.block.enabled) return false
    return when (triggerType) {
        EventRepository.TARGET_TYPE_APP -> !target.isNullOrBlank() &&
            block.apps.any { it.packageName == target }
        EventRepository.TARGET_TYPE_SITE -> !target.isNullOrBlank() &&
            block.sites.any { target == it.domain || target.endsWith(".${it.domain}") }
        else -> false
    }
}

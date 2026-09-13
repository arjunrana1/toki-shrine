package com.arjunrana.tokishrine.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

// Small key-value store for app-level facts Stats needs but events don't
// carry: first_launch_at backs the "days active" figure (PRD §9).
@Entity(tableName = "app_meta")
data class AppMeta(
    @PrimaryKey val key: String,
    val value: String,
) {
    companion object {
        const val KEY_FIRST_LAUNCH_AT = "first_launch_at"
        const val KEY_ONBOARDING_COMPLETED_AT = "onboarding_completed_at"
    }
}

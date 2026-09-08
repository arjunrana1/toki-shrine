package com.arjunrana.tokishrine.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

// Analytics + Stats source of truth. Column names follow the PRD §10 schema
// (event(id, name, timestamp_utc, block_id, target, params_json)) plus one
// added metadata column: target_type distinguishes app targets from website
// targets, because both contain dots and cannot be told apart by shape. The
// per-app Stats leaderboard filters on it; global totals stay inclusive.
// block_id is deliberately NOT a foreign key: events must survive block
// deletion for all-time Stats to stay correct.
@Entity(tableName = "event")
data class Event(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    @ColumnInfo(name = "timestamp_utc") val timestampUtc: Long,
    @ColumnInfo(name = "block_id") val blockId: Long? = null,
    val target: String? = null,
    @ColumnInfo(name = "target_type") val targetType: String? = null,
    @ColumnInfo(name = "params_json") val paramsJson: String? = null,
)

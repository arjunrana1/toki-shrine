package com.arjunrana.tokishrine.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

enum class FrictionType { TYPING, DELAY }

// The per-method configuration columns are always present and always hold
// that method's approved values (PRD §7); frictionType decides which pair is
// the block's active pause/disable path. turnoff_chars is a typing ladder
// count, never a seconds value; the disable wait lives in turnoff_seconds.
@Entity(tableName = "blocks")
data class Block(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    @ColumnInfo(name = "friction_type") val frictionType: FrictionType,
    @ColumnInfo(name = "pause_minutes") val pauseMinutes: Int,
    @ColumnInfo(name = "pause_chars") val pauseChars: Int,
    @ColumnInfo(name = "turnoff_chars") val turnoffChars: Int,
    @ColumnInfo(name = "countdown_seconds") val countdownSeconds: Int,
    @ColumnInfo(name = "turnoff_seconds") val turnoffSeconds: Int,
    val enabled: Boolean,
)

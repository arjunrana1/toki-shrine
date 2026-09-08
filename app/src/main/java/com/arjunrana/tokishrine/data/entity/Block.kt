package com.arjunrana.tokishrine.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

enum class FrictionType { TYPING, DELAY }

@Entity(tableName = "blocks")
data class Block(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    @ColumnInfo(name = "friction_type") val frictionType: FrictionType,
    @ColumnInfo(name = "pause_minutes") val pauseMinutes: Int,
    @ColumnInfo(name = "pause_chars") val pauseChars: Int,
    @ColumnInfo(name = "turnoff_chars") val turnoffChars: Int,
    @ColumnInfo(name = "countdown_seconds") val countdownSeconds: Int,
    @ColumnInfo(name = "show_typos") val showTypos: Boolean,
    val enabled: Boolean,
)

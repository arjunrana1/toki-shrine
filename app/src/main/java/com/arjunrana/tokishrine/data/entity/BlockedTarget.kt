package com.arjunrana.tokishrine.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

// An app belongs to exactly one block (PRD §4). The global unique index on
// package_name makes a cross-block duplicate impossible at the storage layer;
// the repository surfaces it as a conflict instead of letting it insert.
@Entity(
    tableName = "blocked_apps",
    indices = [
        Index(value = ["package_name"], unique = true),
        Index(value = ["block_id"]),
    ],
    foreignKeys = [
        ForeignKey(
            entity = Block::class,
            parentColumns = ["id"],
            childColumns = ["block_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class BlockedApp(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "block_id") val blockId: Long,
    @ColumnInfo(name = "package_name") val packageName: String,
)

@Entity(
    tableName = "blocked_sites",
    indices = [
        Index(value = ["domain"], unique = true),
        Index(value = ["block_id"]),
    ],
    foreignKeys = [
        ForeignKey(
            entity = Block::class,
            parentColumns = ["id"],
            childColumns = ["block_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class BlockedSite(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "block_id") val blockId: Long,
    val domain: String,
)

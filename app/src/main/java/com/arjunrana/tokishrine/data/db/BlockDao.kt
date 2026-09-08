package com.arjunrana.tokishrine.data.db

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import androidx.room.Update
import com.arjunrana.tokishrine.data.entity.BlockedApp
import com.arjunrana.tokishrine.data.entity.BlockedSite
import com.arjunrana.tokishrine.data.entity.Block
import kotlinx.coroutines.flow.Flow

data class BlockWithContents(
    @Embedded val block: Block,
    @Relation(parentColumn = "id", entityColumn = "block_id")
    val apps: List<BlockedApp>,
    @Relation(parentColumn = "id", entityColumn = "block_id")
    val sites: List<BlockedSite>,
)

@Dao
interface BlockDao {

    @Insert
    suspend fun insertBlock(block: Block): Long

    @Update
    suspend fun updateBlock(block: Block)

    @Query("UPDATE blocks SET enabled = :enabled WHERE id = :id")
    suspend fun setEnabled(id: Long, enabled: Boolean)

    @Query("DELETE FROM blocks WHERE id = :id")
    suspend fun deleteBlock(id: Long)

    @Query("SELECT * FROM blocks WHERE id = :id")
    suspend fun getBlock(id: Long): Block?

    @Transaction
    @Query("SELECT * FROM blocks WHERE id = :id")
    suspend fun getBlockWithContents(id: Long): BlockWithContents?

    @Transaction
    @Query("SELECT * FROM blocks ORDER BY id")
    suspend fun getBlocksWithContents(): List<BlockWithContents>

    @Transaction
    @Query("SELECT * FROM blocks ORDER BY id")
    fun observeBlocksWithContents(): Flow<List<BlockWithContents>>

    // — apps —

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertApp(app: BlockedApp): Long

    @Query("SELECT * FROM blocked_apps WHERE package_name = :packageName")
    suspend fun findAppByPackageName(packageName: String): BlockedApp?

    // The conflict dialog's *Move it here*: reassign in one statement so the
    // global unique index is never in a violated intermediate state.
    @Query("UPDATE blocked_apps SET block_id = :toBlockId WHERE package_name = :packageName")
    suspend fun moveAppToBlock(packageName: String, toBlockId: Long)

    @Query("DELETE FROM blocked_apps WHERE package_name = :packageName")
    suspend fun deleteAppByPackageName(packageName: String)

    // — sites —

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSite(site: BlockedSite): Long

    @Query("SELECT * FROM blocked_sites WHERE domain = :domain")
    suspend fun findSiteByDomain(domain: String): BlockedSite?

    // The conflict dialog's *Move it here*: reassign in one statement so the
    // global unique index is never in a violated intermediate state.
    @Query("UPDATE blocked_sites SET block_id = :toBlockId WHERE domain = :domain")
    suspend fun moveSiteToBlock(domain: String, toBlockId: Long)

    @Query("DELETE FROM blocked_sites WHERE domain = :domain")
    suspend fun deleteSiteByDomain(domain: String)
}

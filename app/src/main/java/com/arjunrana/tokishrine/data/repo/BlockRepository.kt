package com.arjunrana.tokishrine.data.repo

import androidx.room.withTransaction
import com.arjunrana.tokishrine.data.db.BlockDao
import com.arjunrana.tokishrine.data.db.BlockWithContents
import com.arjunrana.tokishrine.data.db.TokiDatabase
import com.arjunrana.tokishrine.data.entity.BlockedApp
import com.arjunrana.tokishrine.data.entity.BlockedSite
import com.arjunrana.tokishrine.data.entity.Block
import com.arjunrana.tokishrine.data.entity.FrictionType
import kotlinx.coroutines.flow.Flow

// Result of adding an app or site to a block. An app/site belongs to exactly
// one block (PRD §4); a target already held elsewhere is surfaced as a
// Conflict naming the holding block, never inserted silently.
sealed interface AddTargetResult {
    data object Added : AddTargetResult
    data object AlreadyInThisBlock : AddTargetResult
    data class Conflict(val holdingBlockId: Long, val holdingBlockName: String) : AddTargetResult
}

data class BlockDraft(
    val name: String,
    val appPackageNames: List<String>,
    val siteDomains: List<String>,
    val frictionType: FrictionType,
    val pauseMinutes: Int,
    val pauseChars: Int,
    val turnoffChars: Int,
    val countdownSeconds: Int,
    val showTypos: Boolean,
)

class BlockRepository(private val db: TokiDatabase) {

    private val dao: BlockDao = db.blockDao()

    // PRD §4: a new block is saved OFF and does nothing until turned on —
    // enabled is forced false regardless of any draft state.
    //
    // One transaction for the parent row and every child, so a rejected save
    // (e.g. a duplicate reaching the unique index) rolls back completely: no
    // half-built block, no reserved targets. The create flow resolves
    // conflicts in the editor before saving; the index stays as the backstop.
    suspend fun createBlock(draft: BlockDraft): Long = db.withTransaction {
        val id = dao.insertBlock(
            Block(
                name = draft.name,
                frictionType = draft.frictionType,
                pauseMinutes = draft.pauseMinutes,
                pauseChars = draft.pauseChars,
                turnoffChars = draft.turnoffChars,
                countdownSeconds = draft.countdownSeconds,
                showTypos = draft.showTypos,
                enabled = false,
            ),
        )
        draft.appPackageNames.forEach { dao.insertApp(BlockedApp(blockId = id, packageName = it)) }
        draft.siteDomains.forEach { dao.insertSite(BlockedSite(blockId = id, domain = canonicalDomain(it))) }
        id
    }

    suspend fun getBlockWithContents(id: Long): BlockWithContents? = dao.getBlockWithContents(id)

    suspend fun getBlocksWithContents(): List<BlockWithContents> = dao.getBlocksWithContents()

    fun observeBlocksWithContents(): Flow<List<BlockWithContents>> = dao.observeBlocksWithContents()

    suspend fun updateBlock(block: Block) = dao.updateBlock(block)

    suspend fun setEnabled(id: Long, enabled: Boolean) = dao.setEnabled(id, enabled)

    suspend fun deleteBlock(id: Long) = dao.deleteBlock(id)

    suspend fun addApp(blockId: Long, packageName: String): AddTargetResult {
        val existing = dao.findAppByPackageName(packageName)
        if (existing == null) {
            dao.insertApp(BlockedApp(blockId = blockId, packageName = packageName))
            return AddTargetResult.Added
        }
        if (existing.blockId == blockId) return AddTargetResult.AlreadyInThisBlock
        return AddTargetResult.Conflict(existing.blockId, holdingName(existing.blockId))
    }

    suspend fun addSite(blockId: Long, domain: String): AddTargetResult {
        val canonical = canonicalDomain(domain)
        val existing = dao.findSiteByDomain(canonical)
        if (existing == null) {
            dao.insertSite(BlockedSite(blockId = blockId, domain = canonical))
            return AddTargetResult.Added
        }
        if (existing.blockId == blockId) return AddTargetResult.AlreadyInThisBlock
        return AddTargetResult.Conflict(existing.blockId, holdingName(existing.blockId))
    }

    // The conflict dialog's *Move it here*: reassign the single row atomically.
    // Lookups canonicalize too, so a case-variant spelling still finds the row.
    suspend fun moveApp(packageName: String, toBlockId: Long) =
        dao.moveAppToBlock(packageName, toBlockId)

    suspend fun moveSite(domain: String, toBlockId: Long) =
        dao.moveSiteToBlock(canonicalDomain(domain), toBlockId)

    suspend fun removeApp(packageName: String) = dao.deleteAppByPackageName(packageName)

    suspend fun removeSite(domain: String) = dao.deleteSiteByDomain(canonicalDomain(domain))

    // Domains are case-insensitive in DNS; identity is the lowercased,
    // trimmed name without a trailing root dot. Storage only ever holds
    // canonical spellings, so the unique index protects the same identity the
    // repository compares. Whole-domain only — no path handling (PRD §13).
    private fun canonicalDomain(domain: String): String =
        domain.trim().trimEnd('.').lowercase()

    private suspend fun holdingName(blockId: Long): String =
        dao.getBlock(blockId)?.name ?: ""
}

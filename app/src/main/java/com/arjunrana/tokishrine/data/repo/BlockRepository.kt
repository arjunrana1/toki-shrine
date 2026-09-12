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

// Raised at the persistence boundary when a draft claims a target another
// block already owns. The picker prevents this normally; the guard exists so
// no write path — including stale drafts — can strip a target from its block
// (PRD §4: removing an app from a block must never be a bypass).
class ConflictingOwnershipException(
    val target: String,
    val isApp: Boolean,
) : IllegalStateException("Target '$target' is already owned by another block")

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

// open only so instrumented UI tests can gate the async reads the create
// flow performs; production code always uses this class directly.
open class BlockRepository(private val db: TokiDatabase) {

    private val dao: BlockDao = db.blockDao()

    // PRD §4: a new block is saved OFF and does nothing until turned on —
    // enabled is forced false regardless of any draft state.
    //
    // One transaction for the parent row and every child. A draft claiming a
    // target owned by any other block — ON or OFF — aborts the whole save:
    // the parent row rolls back with the children, so a rejected save leaves
    // exactly the data that existed before it. Targets are never reassigned.
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
        // In-draft duplicates collapse; only cross-block ownership rejects.
        draft.appPackageNames.distinct().forEach { packageName ->
            if (dao.findAppByPackageName(packageName) != null) {
                throw ConflictingOwnershipException(packageName, isApp = true)
            }
            dao.insertApp(BlockedApp(blockId = id, packageName = packageName))
        }
        draft.siteDomains.map { canonicalDomain(it) }.distinct().forEach { domain ->
            if (dao.findSiteByDomain(domain) != null) {
                throw ConflictingOwnershipException(domain, isApp = false)
            }
            dao.insertSite(BlockedSite(blockId = id, domain = domain))
        }
        id
    }

    // Edit (block OFF only, enforced by the UI): rewrites the row and syncs
    // the target lists atomically. Removals drop rows, additions insert —
    // and a target held by another block aborts the whole update. The
    // edited block's own targets stay editable: re-adding one is a no-op,
    // removing one deletes it.
    suspend fun updateBlock(id: Long, draft: BlockDraft) = db.withTransaction {
        val current = dao.getBlockWithContents(id) ?: error("Block $id does not exist")
        dao.updateBlock(
            current.block.copy(
                name = draft.name,
                frictionType = draft.frictionType,
                pauseMinutes = draft.pauseMinutes,
                pauseChars = draft.pauseChars,
                turnoffChars = draft.turnoffChars,
                countdownSeconds = draft.countdownSeconds,
                showTypos = draft.showTypos,
            ),
        )
        val draftApps = draft.appPackageNames.toSet()
        current.apps.filter { it.packageName !in draftApps }.forEach {
            dao.deleteAppByPackageName(it.packageName)
        }
        draftApps.forEach { packageName ->
            val existing = dao.findAppByPackageName(packageName)
            when {
                existing == null -> dao.insertApp(BlockedApp(blockId = id, packageName = packageName))
                existing.blockId != id -> throw ConflictingOwnershipException(packageName, isApp = true)
            }
        }
        val draftSites = draft.siteDomains.map { canonicalDomain(it) }.toSet()
        current.sites.filter { it.domain !in draftSites }.forEach {
            dao.deleteSiteByDomain(it.domain)
        }
        draftSites.forEach { domain ->
            val existing = dao.findSiteByDomain(domain)
            when {
                existing == null -> dao.insertSite(BlockedSite(blockId = id, domain = domain))
                existing.blockId != id -> throw ConflictingOwnershipException(domain, isApp = false)
            }
        }
    }

    suspend fun getBlockWithContents(id: Long): BlockWithContents? = dao.getBlockWithContents(id)

    suspend fun getBlocksWithContents(): List<BlockWithContents> = dao.getBlocksWithContents()

    fun observeBlocksWithContents(): Flow<List<BlockWithContents>> = dao.observeBlocksWithContents()

    suspend fun setEnabled(id: Long, enabled: Boolean) = dao.setEnabled(id, enabled)

    suspend fun deleteBlock(id: Long) = dao.deleteBlock(id)

    // Ownership maps for the picker: package name / canonical domain →
    // owning block id. The UI marks targets held by another block as
    // unavailable; the persistence guards above remain the real boundary.
    open suspend fun appOwnerships(): Map<String, Long> =
        dao.getAllApps().associate { it.packageName to it.blockId }

    open suspend fun siteOwnerships(): Map<String, Long> =
        dao.getAllSites().associate { it.domain to it.blockId }

    // Domains are case-insensitive in DNS; identity is the lowercased,
    // trimmed name without a trailing root dot. Storage only ever holds
    // canonical spellings, so the unique index protects the same identity the
    // repository compares. Whole-domain only — no path handling (PRD §13).
    private fun canonicalDomain(domain: String): String =
        domain.trim().trimEnd('.').lowercase()
}

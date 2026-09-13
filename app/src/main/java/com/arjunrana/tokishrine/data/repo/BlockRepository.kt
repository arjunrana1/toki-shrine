package com.arjunrana.tokishrine.data.repo

import androidx.room.withTransaction
import com.arjunrana.tokishrine.data.db.BlockDao
import com.arjunrana.tokishrine.data.db.BlockWithContents
import com.arjunrana.tokishrine.data.db.TokiDatabase
import com.arjunrana.tokishrine.data.entity.AppMeta
import com.arjunrana.tokishrine.data.entity.BlockedApp
import com.arjunrana.tokishrine.data.entity.BlockedSite
import com.arjunrana.tokishrine.data.entity.Block
import com.arjunrana.tokishrine.data.entity.Event
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

// Owner addendum, 13 September 2026: the wait/countdown is capped. The
// configuration stepper offers at most 5 minutes (300 s); storage accepts at
// most 20 minutes (1200 s) so no write path — stale drafts included — can
// persist an endless wait. The UI never mentions this bound.
const val MAX_STORED_COUNTDOWN_SECONDS = 1200

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
open class BlockRepository(
    private val db: TokiDatabase,
    private val clock: () -> Long = System::currentTimeMillis,
) {

    private val dao: BlockDao = db.blockDao()
    private val metaDao = db.appMetaDao()

    // PRD §4: a new block is saved OFF and does nothing until turned on —
    // enabled is forced false regardless of any draft state.
    //
    // One transaction for the parent row and every child. A draft claiming a
    // target owned by any other block — ON or OFF — aborts the whole save:
    // the parent row rolls back with the children, so a rejected save leaves
    // exactly the data that existed before it. Targets are never reassigned.
    suspend fun createBlock(draft: BlockDraft): Long = db.withTransaction {
        // PRD §17 R8: a block always covers at least one target. The editor
        // gates this in the UI; the boundary guards stale drafts and any
        // other write path.
        require(draft.appPackageNames.isNotEmpty() || draft.siteDomains.isNotEmpty()) {
            "A block must contain at least one app or site"
        }
        // Owner addendum, 13 September: the stored wait is bounded at 20 min.
        require(draft.countdownSeconds in 1..MAX_STORED_COUNTDOWN_SECONDS) {
            "Countdown must be between 1 and $MAX_STORED_COUNTDOWN_SECONDS seconds"
        }
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
        // PRD §17 R8: an edit may not empty a block either — including a
        // direct friction edit that never visited the contents step.
        require(draft.appPackageNames.isNotEmpty() || draft.siteDomains.isNotEmpty()) {
            "A block must contain at least one app or site"
        }
        // Owner addendum, 13 September: the stored wait is bounded at 20 min.
        require(draft.countdownSeconds in 1..MAX_STORED_COUNTDOWN_SECONDS) {
            "Countdown must be between 1 and $MAX_STORED_COUNTDOWN_SECONDS seconds"
        }
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

    // The terminal enabled-state transition (turn-on/turn-off): the blocks
    // row and its block_turned_on/off event commit in one transaction, so a
    // cancelled or failed caller can never leave a block without its event
    // (review blocker 2). Returns whether the stored value actually changed
    // — an already-stored value emits no event (and no haptic upstream).
    suspend fun setEnabledRecordingTransition(id: Long, enabled: Boolean): Boolean = db.withTransaction {
        val changed = dao.setEnabled(id, enabled) > 0
        if (changed) {
            // Transition events carry no target, so they are written
            // directly beside the state change rather than through
            // EventRepository.log, which would run outside this
            // transaction. First-launch parity with log().
            metaDao.putIfAbsent(AppMeta(AppMeta.KEY_FIRST_LAUNCH_AT, clock().toString()))
            db.eventDao().insert(
                Event(
                    name = if (enabled) {
                        EventRepository.EVENT_BLOCK_TURNED_ON
                    } else {
                        EventRepository.EVENT_BLOCK_TURNED_OFF
                    },
                    timestampUtc = clock(),
                    blockId = id,
                ),
            )
        }
        changed
    }

    // Direct state write with no transition event — fixture seeding and
    // future internal re-arm only. Every user-visible transition goes
    // through setEnabledRecordingTransition; internal keeps it out of
    // production call sites while staying visible to the test source sets.
    internal suspend fun setEnabled(id: Long, enabled: Boolean): Boolean =
        dao.setEnabled(id, enabled) > 0

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

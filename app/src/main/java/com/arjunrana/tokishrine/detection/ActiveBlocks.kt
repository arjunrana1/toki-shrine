package com.arjunrana.tokishrine.detection

import com.arjunrana.tokishrine.data.db.BlockWithContents

// The block a detection trigger points at. The block name travels with
// the id so the Phase 4 placeholder can name the trigger without a
// database read on the event path.
data class BlockRef(val blockId: Long, val blockName: String)

// Enabled blocks only, as package/domain → owning block lookup maps.
// The service keeps a volatile snapshot built from the repository's
// blocks flow, so per-event matching never touches the database, and
// OFF blocks vanish from detection the moment they are turned off.
data class ActiveBlockIndex(
    val apps: Map<String, BlockRef>,
    val sites: Map<String, BlockRef>,
) {
    companion object {

        fun from(blocks: List<BlockWithContents>, excludePackage: String): ActiveBlockIndex {
            val enabled = blocks.filter { it.block.enabled }
            return ActiveBlockIndex(
                // Toki Shrine's own package is never a matchable app: the
                // placeholder activity itself would otherwise re-trigger
                // detection the moment it appears.
                apps = enabled.flatMap { block ->
                    block.apps.map { it.packageName to BlockRef(block.block.id, block.block.name) }
                }.toMap().filterKeys { it != excludePackage },
                sites = enabled.flatMap { block ->
                    block.sites.map { it.domain to BlockRef(block.block.id, block.block.name) }
                }.toMap(),
            )
        }
    }
}

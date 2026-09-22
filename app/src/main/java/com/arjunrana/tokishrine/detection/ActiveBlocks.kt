package com.arjunrana.tokishrine.detection

import com.arjunrana.tokishrine.data.db.BlockWithContents

// The block a detection trigger points at. The block name travels with
// the id so the interruption launch can carry useful display data without a
// database read on the event path.
data class BlockRef(val blockId: Long, val blockName: String)

// Enabled, non-paused blocks as package/domain → owning block lookup maps.
// The service keeps a volatile snapshot built from the repository's blocks
// flow combined with the live pause set, so per-event matching never touches
// the database: OFF blocks vanish from detection the moment they are turned
// off, and a paused block's whole target set opens for the pause (PRD §4)
// and returns at re-arm with the next emission.
data class ActiveBlockIndex(
    val apps: Map<String, BlockRef>,
    val sites: Map<String, BlockRef>,
) {
    companion object {

        fun from(
            blocks: List<BlockWithContents>,
            excludePackage: String,
            pausedBlockIds: Set<Long> = emptySet(),
        ): ActiveBlockIndex {
            val enabled = blocks.filter { it.block.enabled && it.block.id !in pausedBlockIds }
            return ActiveBlockIndex(
                // Toki Shrine's own package is never a matchable app: the
                // interruption Activity itself would otherwise re-trigger
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

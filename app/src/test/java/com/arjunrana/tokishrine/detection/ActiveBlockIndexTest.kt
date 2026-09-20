package com.arjunrana.tokishrine.detection

import com.arjunrana.tokishrine.data.db.BlockWithContents
import com.arjunrana.tokishrine.data.entity.BlockedApp
import com.arjunrana.tokishrine.data.entity.BlockedSite
import com.arjunrana.tokishrine.data.entity.Block
import com.arjunrana.tokishrine.data.entity.FrictionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

// The detection snapshot: enabled blocks only, with Toki Shrine's own
// package never matchable, so turning a block off removes it from
// detection without any event-path database read.
class ActiveBlockIndexTest {

    private fun block(id: Long, name: String, enabled: Boolean) = Block(
        id = id,
        name = name,
        frictionType = FrictionType.TYPING,
        pauseMinutes = 15,
        pauseChars = 150,
        turnoffChars = 350,
        countdownSeconds = 60,
        turnoffSeconds = 360,
        enabled = enabled,
    )

    private fun withContents(block: Block, apps: List<String>, sites: List<String>) =
        BlockWithContents(
            block = block,
            apps = apps.map { BlockedApp(blockId = block.id, packageName = it) },
            sites = sites.map { BlockedSite(blockId = block.id, domain = it) },
        )

    @Test
    fun onlyEnabledBlocksAreIndexed() {
        val index = ActiveBlockIndex.from(
            listOf(
                withContents(block(1, "Dooms", enabled = true), apps = listOf("com.instagram.android"), sites = listOf("reddit.com")),
                withContents(block(2, "Off block", enabled = false), apps = listOf("com.twitter.android"), sites = listOf("x.com")),
            ),
            excludePackage = "com.arjunrana.tokishrine",
        )
        assertEquals(setOf("com.instagram.android"), index.apps.keys)
        assertEquals(setOf("reddit.com"), index.sites.keys)
    }

    @Test
    fun ownPackageIsNeverMatchableEvenWhenBlocked() {
        val index = ActiveBlockIndex.from(
            listOf(
                withContents(block(1, "Self", enabled = true), apps = listOf("com.arjunrana.tokishrine", "com.instagram.android"), sites = emptyList()),
            ),
            excludePackage = "com.arjunrana.tokishrine",
        )
        assertFalse(index.apps.containsKey("com.arjunrana.tokishrine"))
        assertEquals(setOf("com.instagram.android"), index.apps.keys)
    }

    @Test
    fun refsCarryBlockIdAndNameForThePlaceholder() {
        val index = ActiveBlockIndex.from(
            listOf(withContents(block(7, "Dooms", enabled = true), apps = listOf("com.instagram.android"), sites = listOf("reddit.com"))),
            excludePackage = "com.arjunrana.tokishrine",
        )
        assertEquals(BlockRef(7, "Dooms"), index.apps["com.instagram.android"])
        assertEquals(BlockRef(7, "Dooms"), index.sites["reddit.com"])
    }

    @Test
    fun emptyBlockListYieldsEmptyIndex() {
        val index = ActiveBlockIndex.from(emptyList(), excludePackage = "com.arjunrana.tokishrine")
        assertEquals(emptyMap<String, BlockRef>(), index.apps)
        assertEquals(emptyMap<String, BlockRef>(), index.sites)
    }
}

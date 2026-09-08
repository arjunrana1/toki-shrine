package com.arjunrana.tokishrine.data

import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.arjunrana.tokishrine.data.db.BlockDao
import com.arjunrana.tokishrine.data.db.TokiDatabase
import com.arjunrana.tokishrine.data.entity.FrictionType
import com.arjunrana.tokishrine.data.repo.AddTargetResult
import com.arjunrana.tokishrine.data.repo.BlockDraft
import com.arjunrana.tokishrine.data.repo.BlockRepository
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BlockRepositoryTest {

    private lateinit var db: TokiDatabase
    private lateinit var dao: BlockDao
    private lateinit var repo: BlockRepository

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            TokiDatabase::class.java,
        ).build()
        dao = db.blockDao()
        repo = BlockRepository(db)
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun draft(
        name: String,
        apps: List<String> = emptyList(),
        sites: List<String> = emptyList(),
        frictionType: FrictionType = FrictionType.TYPING,
    ) = BlockDraft(
        name = name,
        appPackageNames = apps,
        siteDomains = sites,
        frictionType = frictionType,
        pauseMinutes = 25,
        pauseChars = 120,
        turnoffChars = 320,
        countdownSeconds = 45,
        showTypos = false,
    )

    // Acceptance: insert a block, read it back, all fields match.
    @Test
    fun insertBlockAndReadBackAllFields() = runBlocking {
        val id = repo.createBlock(
            draft(
                "Social",
                apps = listOf("com.instagram.android", "com.reddit.frontpage"),
                sites = listOf("reddit.com"),
                frictionType = FrictionType.DELAY,
            ),
        )

        val read = repo.getBlockWithContents(id)
        assertNotNull(read)
        read!!
        assertEquals("Social", read.block.name)
        assertEquals(FrictionType.DELAY, read.block.frictionType)
        assertEquals(25, read.block.pauseMinutes)
        assertEquals(120, read.block.pauseChars)
        assertEquals(320, read.block.turnoffChars)
        assertEquals(45, read.block.countdownSeconds)
        assertEquals(false, read.block.showTypos)
        // PRD §4: a new block is saved OFF, whatever the draft carried.
        assertEquals(false, read.block.enabled)
        assertEquals(
            setOf("com.instagram.android", "com.reddit.frontpage"),
            read.apps.map { it.packageName }.toSet(),
        )
        assertEquals(listOf("reddit.com"), read.sites.map { it.domain })
    }

    // Acceptance: inserting an app already present in another block is
    // flagged by the repository — it never silently duplicates.
    @Test
    fun appHeldByAnotherBlockConflictsAndNeverDuplicates() = runBlocking {
        val social = repo.createBlock(draft("Social"))
        val news = repo.createBlock(draft("News"))

        assertEquals(AddTargetResult.Added, repo.addApp(social, "com.instagram.android"))
        assertEquals(
            AddTargetResult.Conflict(social, "Social"),
            repo.addApp(news, "com.instagram.android"),
        )
        // The single row still belongs to Social; no duplicate was written.
        assertEquals(social, dao.findAppByPackageName("com.instagram.android")?.blockId)

        // Re-adding to the same block is a no-op, not a conflict.
        assertEquals(
            AddTargetResult.AlreadyInThisBlock,
            repo.addApp(social, "com.instagram.android"),
        )
        assertEquals(social, dao.findAppByPackageName("com.instagram.android")?.blockId)
    }

    @Test
    fun siteHeldByAnotherBlockConflictsAndNeverDuplicates() = runBlocking {
        val social = repo.createBlock(draft("Social"))
        val news = repo.createBlock(draft("News"))

        assertEquals(AddTargetResult.Added, repo.addSite(social, "reddit.com"))
        assertEquals(
            AddTargetResult.Conflict(social, "Social"),
            repo.addSite(news, "reddit.com"),
        )
        assertEquals(social, dao.findSiteByDomain("reddit.com")?.blockId)
    }

    // P2 fix: domain identity is the canonical (lowercased, trimmed) name, so
    // REDDIT.COM is reddit.com and cannot enter a second block. Storage holds
    // the canonical spelling only.
    @Test
    fun domainIdentityIsCaseInsensitive() = runBlocking {
        val social = repo.createBlock(draft("Social"))
        val news = repo.createBlock(draft("News"))

        assertEquals(AddTargetResult.Added, repo.addSite(social, "reddit.com"))
        assertEquals(
            AddTargetResult.Conflict(social, "Social"),
            repo.addSite(news, "REDDIT.COM"),
        )
        // Stored spelling is canonical; only one row exists.
        assertEquals("reddit.com", dao.findSiteByDomain("reddit.com")?.domain)
        assertNull(dao.findSiteByDomain("REDDIT.COM"))

        // The create path canonicalizes too.
        val other = repo.createBlock(draft("Other", sites = listOf("News.SiteExample.COM")))
        assertEquals(
            listOf("news.siteexample.com"),
            repo.getBlockWithContents(other)!!.sites.map { it.domain },
        )

        // Move and remove find the row through case-variant spellings.
        repo.moveSite("REDDIT.COM", news)
        assertEquals(news, dao.findSiteByDomain("reddit.com")?.blockId)
        repo.removeSite("Reddit.Com")
        assertNull(dao.findSiteByDomain("reddit.com"))
    }

    // P1 fix: a save rejected mid-way (duplicate reaching the unique index)
    // must roll back completely — no half-built block, no reserved targets.
    @Test
    fun failedCreateBlockLeavesNothingBehind() = runBlocking {
        val existing = repo.createBlock(draft("Existing", apps = listOf("com.instagram.android")))

        assertThrows(SQLiteConstraintException::class.java) {
            runBlocking {
                repo.createBlock(
                    draft("Failed", apps = listOf("com.reddit.frontpage", "com.instagram.android")),
                )
            }
        }

        // Only the original block survives, intact; nothing from the failed
        // save was persisted, including the app inserted before the failure.
        val blocks = repo.getBlocksWithContents()
        assertEquals(1, blocks.size)
        assertEquals(existing, blocks[0].block.id)
        assertEquals("Existing", blocks[0].block.name)
        assertEquals(
            listOf("com.instagram.android"),
            blocks[0].apps.map { it.packageName },
        )
        assertNull(dao.findAppByPackageName("com.reddit.frontpage"))
    }

    // The conflict dialog's *Move it here*: the row reassigns and the conflict
    // then names the new holder.
    @Test
    fun moveAppReassignsOwnership() = runBlocking {
        val social = repo.createBlock(draft("Social"))
        val news = repo.createBlock(draft("News"))
        repo.addApp(social, "com.instagram.android")

        repo.moveApp("com.instagram.android", news)

        assertEquals(news, dao.findAppByPackageName("com.instagram.android")?.blockId)
        assertEquals(
            AddTargetResult.Conflict(news, "News"),
            repo.addApp(social, "com.instagram.android"),
        )
    }

    @Test
    fun deletingBlockCascadesItsTargetsOnly() = runBlocking {
        val social = repo.createBlock(draft("Social", apps = listOf("com.instagram.android")))
        val news = repo.createBlock(draft("News", apps = listOf("com.reddit.frontpage")))

        repo.deleteBlock(social)

        assertNull(dao.findAppByPackageName("com.instagram.android"))
        assertNotNull(dao.findAppByPackageName("com.reddit.frontpage"))
        assertEquals(news, dao.findAppByPackageName("com.reddit.frontpage")?.blockId)
    }

    @Test
    fun setEnabledTogglesWithoutTouchingOtherFields() = runBlocking {
        val id = repo.createBlock(draft("Social"))
        repo.setEnabled(id, true)

        val on = repo.getBlockWithContents(id)!!
        assertEquals(true, on.block.enabled)
        assertEquals("Social", on.block.name)
        assertEquals(25, on.block.pauseMinutes)
    }
}

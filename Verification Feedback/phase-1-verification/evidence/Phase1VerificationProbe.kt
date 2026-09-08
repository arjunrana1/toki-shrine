package com.arjunrana.tokishrine.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.arjunrana.tokishrine.data.db.TokiDatabase
import com.arjunrana.tokishrine.data.entity.FrictionType
import com.arjunrana.tokishrine.data.repo.*
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith

// Verifier-only regression probes. No production code is changed.
@RunWith(AndroidJUnit4::class)
class Phase1VerificationProbe {
    private lateinit var db: TokiDatabase
    private lateinit var repo: BlockRepository
    @Before fun setup() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), TokiDatabase::class.java).build()
        repo = BlockRepository(db.blockDao())
    }
    @After fun close() { db.close() }
    private fun draft(name: String, apps: List<String> = emptyList()) = BlockDraft(
        name, apps, emptyList(), FrictionType.TYPING, 15, 100, 300, 30, true
    )
    @Test fun failedCreateMustRollBackAllRows() = runBlocking {
        repo.createBlock(draft("Existing", listOf("com.instagram.android")))
        var rejected = false
        try { repo.createBlock(draft("Failed", listOf("com.reddit.frontpage", "com.instagram.android"))) }
        catch (e: android.database.sqlite.SQLiteConstraintException) { rejected = true }
        assertTrue("Expected duplicate rejection", rejected)
        assertEquals("Failed create left a partial block and its first target", 1, repo.getBlocksWithContents().size)
        assertNull(db.blockDao().findAppByPackageName("com.reddit.frontpage"))
    }
    @Test fun sameDomainCaseMustConflict() = runBlocking {
        val a=repo.createBlock(draft("A")); val b=repo.createBlock(draft("B"))
        assertEquals(AddTargetResult.Added, repo.addSite(a,"reddit.com"))
        assertEquals("DNS names differing only in case are the same site", AddTargetResult.Conflict(a,"A"),repo.addSite(b,"REDDIT.COM"))
    }
    @Test fun perAppLeaderboardMustExcludeSites() = runBlocking {
        val events=EventRepository(db.eventDao(),db.appMetaDao())
        events.log("walk_away",target="com.instagram.android",params=mapOf("source" to "block_screen"))
        repeat(2) { events.log("walk_away",target="reddit.com",params=mapOf("source" to "block_screen")) }
        val stats=events.getStats()
        assertEquals(3,stats.totalWalkAways)
        assertEquals("PRD section 9 specifies a per-app leaderboard", listOf("com.instagram.android"),stats.mostWalkedAwayFrom.map { it.target })
    }
}

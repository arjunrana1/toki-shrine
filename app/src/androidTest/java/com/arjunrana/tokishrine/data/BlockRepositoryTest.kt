package com.arjunrana.tokishrine.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.arjunrana.tokishrine.data.db.BlockDao
import com.arjunrana.tokishrine.data.db.TokiDatabase
import com.arjunrana.tokishrine.data.entity.FrictionType
import com.arjunrana.tokishrine.data.repo.BlockDraft
import com.arjunrana.tokishrine.data.repo.BlockRepository
import com.arjunrana.tokishrine.data.repo.ConflictingOwnershipException
import com.arjunrana.tokishrine.data.repo.DISABLE_CHARS_CHOICES
import com.arjunrana.tokishrine.data.repo.DISABLE_WAIT_SECONDS_CHOICES
import com.arjunrana.tokishrine.data.repo.EventRepository
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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
        countdownSeconds: Int = 90,
        turnoffSeconds: Int = 360,
        turnoffChars: Int = 350,
        pauseChars: Int = 120,
        pauseMinutes: Int = 25,
    ) = BlockDraft(
        name = name,
        appPackageNames = apps,
        siteDomains = sites,
        frictionType = frictionType,
        pauseMinutes = pauseMinutes,
        pauseChars = pauseChars,
        turnoffChars = turnoffChars,
        countdownSeconds = countdownSeconds,
        turnoffSeconds = turnoffSeconds,
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
                turnoffSeconds = 720,
            ),
        )

        val read = repo.getBlockWithContents(id)
        assertNotNull(read)
        read!!
        assertEquals("Social", read.block.name)
        assertEquals(FrictionType.DELAY, read.block.frictionType)
        assertEquals(25, read.block.pauseMinutes)
        assertEquals(120, read.block.pauseChars)
        assertEquals(350, read.block.turnoffChars)
        assertEquals(90, read.block.countdownSeconds)
        assertEquals(720, read.block.turnoffSeconds)
        // PRD §4: a new block is saved OFF, whatever the draft carried.
        assertEquals(false, read.block.enabled)
        assertEquals(
            setOf("com.instagram.android", "com.reddit.frontpage"),
            read.apps.map { it.packageName }.toSet(),
        )
        assertEquals(listOf("reddit.com"), read.sites.map { it.domain })
    }

    // Owner decision: a target held by any other block rejects the whole
    // save at the transaction boundary — never a silent transfer. Here the
    // parent row and the first child were already written when the second
    // child tripped the guard: both must roll back.
    @Test
    fun creatingBlockWithAppHeldElsewhereIsRejectedAndRollsBack() = runBlocking {
        val existing = repo.createBlock(draft("Existing", apps = listOf("com.instagram.android")))

        assertThrows(ConflictingOwnershipException::class.java) {
            runBlocking {
                repo.createBlock(
                    draft("Failed", apps = listOf("com.reddit.frontpage", "com.instagram.android")),
                )
            }
        }

        val blocks = repo.getBlocksWithContents()
        assertEquals(1, blocks.size)
        assertEquals(existing, blocks[0].block.id)
        assertEquals(
            listOf("com.instagram.android"),
            blocks[0].apps.map { it.packageName },
        )
        assertNull(dao.findAppByPackageName("com.reddit.frontpage"))
    }

    // The owner rule is ownership-based, not state-based: an ON source
    // rejects exactly like an OFF one.
    @Test
    fun creatingBlockWithAppHeldByAnOnBlockIsAlsoRejected() = runBlocking {
        val existing = repo.createBlock(draft("Existing", apps = listOf("com.instagram.android")))
        repo.setEnabled(existing, true)

        assertThrows(ConflictingOwnershipException::class.java) {
            runBlocking { repo.createBlock(draft("Failed", apps = listOf("com.instagram.android"))) }
        }

        assertEquals(1, repo.getBlocksWithContents().size)
        assertEquals(
            existing,
            dao.findAppByPackageName("com.instagram.android")?.blockId,
        )
        assertTrue(repo.getBlockWithContents(existing)!!.block.enabled)
    }

    // Sites resolve through the canonical domain identity: a case-variant
    // spelling hits the same guard and rolls back the same way.
    @Test
    fun creatingBlockWithSiteHeldElsewhereIsRejectedAndRollsBack() = runBlocking {
        val existing = repo.createBlock(draft("Existing", sites = listOf("reddit.com")))

        assertThrows(ConflictingOwnershipException::class.java) {
            runBlocking {
                repo.createBlock(draft("Failed", apps = listOf("com.reddit.frontpage"), sites = listOf("REDDIT.COM")))
            }
        }

        val blocks = repo.getBlocksWithContents()
        assertEquals(1, blocks.size)
        assertEquals(existing, blocks[0].block.id)
        assertNull(dao.findAppByPackageName("com.reddit.frontpage"))
        assertEquals(
            listOf("reddit.com"),
            blocks[0].sites.map { it.domain },
        )
    }

    // Duplicates inside one draft collapse to a single row each; the unique
    // index stays the storage-level backstop.
    @Test
    fun duplicateTargetsCollapseInsideTheTransaction() = runBlocking {
        val created = repo.createBlock(
            draft(
                "New",
                apps = listOf("com.reddit.frontpage", "com.reddit.frontpage"),
                sites = listOf("news.siteexample.com", "news.siteexample.com"),
            ),
        )

        val read = repo.getBlockWithContents(created)!!
        assertEquals(listOf("com.reddit.frontpage"), read.apps.map { it.packageName })
        assertEquals(listOf("news.siteexample.com"), read.sites.map { it.domain })
    }

    // Edit path: fields and target lists sync atomically — removals drop
    // rows, new free targets insert, and the edited block's own targets
    // stay editable.
    @Test
    fun updateBlockSyncsFieldsAndOwnTargets() = runBlocking {
        val social = repo.createBlock(
            draft(
                "Social",
                apps = listOf("com.instagram.android", "com.reddit.frontpage"),
                sites = listOf("reddit.com"),
            ),
        )

        repo.updateBlock(
            social,
            BlockDraft(
                name = "Social renamed",
                appPackageNames = listOf("com.reddit.frontpage"),
                siteDomains = listOf("news.siteexample.com"),
                frictionType = FrictionType.DELAY,
                pauseMinutes = 30,
                pauseChars = 120,
                turnoffChars = 350,
                countdownSeconds = 90,
                turnoffSeconds = 720,
            ),
        )

        val updated = repo.getBlockWithContents(social)!!
        assertEquals("Social renamed", updated.block.name)
        assertEquals(FrictionType.DELAY, updated.block.frictionType)
        assertEquals(30, updated.block.pauseMinutes)
        assertEquals(720, updated.block.turnoffSeconds)
        assertEquals(false, updated.block.enabled)
        assertEquals(listOf("com.reddit.frontpage"), updated.apps.map { it.packageName })
        assertEquals(listOf("news.siteexample.com"), updated.sites.map { it.domain })
        assertNull(dao.findAppByPackageName("com.instagram.android"))
    }

    // A stale draft claiming a target that another block took meanwhile
    // aborts the update, leaving the edited block exactly as it was.
    @Test
    fun updateBlockRejectingHeldTargetChangesNothing() = runBlocking {
        val social = repo.createBlock(
            draft("Social", apps = listOf("com.instagram.android"), sites = listOf("reddit.com")),
        )
        val news = repo.createBlock(draft("News", apps = listOf("com.apple.news")))

        assertThrows(ConflictingOwnershipException::class.java) {
            runBlocking {
                repo.updateBlock(
                    social,
                    BlockDraft(
                        name = "Should never persist",
                        appPackageNames = listOf("com.apple.news"),
                        siteDomains = emptyList(),
                        frictionType = FrictionType.TYPING,
                        pauseMinutes = 60,
                        pauseChars = 200,
                        turnoffChars = 350,
                        countdownSeconds = 90,
                        turnoffSeconds = 360,
                    ),
                )
            }
        }

        val unchanged = repo.getBlockWithContents(social)!!
        assertEquals("Social", unchanged.block.name)
        assertEquals(
            listOf("com.instagram.android"),
            unchanged.apps.map { it.packageName },
        )
        assertEquals(listOf("reddit.com"), unchanged.sites.map { it.domain })
        assertNotNull(dao.findAppByPackageName("com.apple.news"))
        assertEquals(news, dao.findAppByPackageName("com.apple.news")?.blockId)
    }

    // Update ordering: the draft's app mutations (a removal and an insert)
    // run before the site list is checked, so a site held by another block
    // must abort AFTER writes have already happened — and the transaction
    // must roll all of them back. Every stored field of every block, every
    // target row, and the ownership maps are compared before vs after.
    @Test
    fun updateBlockSiteConflictAfterAppMutationsRollsBackEverything() = runBlocking {
        val social = repo.createBlock(
            draft(
                "Social",
                apps = listOf("com.instagram.android"),
                sites = listOf("reddit.com"),
                frictionType = FrictionType.TYPING,
            ),
        )
        val news = repo.createBlock(
            draft(
                "News",
                apps = listOf("com.apple.news"),
                sites = listOf("news.siteexample.com"),
                frictionType = FrictionType.DELAY,
            ),
        )
        repo.setEnabled(social, true)

        val beforeSocial = dao.getBlockWithContents(social)
        val beforeNews = dao.getBlockWithContents(news)
        val beforeAppOwners = repo.appOwnerships()
        val beforeSiteOwners = repo.siteOwnerships()

        val exception = assertThrows(ConflictingOwnershipException::class.java) {
            runBlocking {
                repo.updateBlock(
                    social,
                    BlockDraft(
                        name = "Should never persist",
                        // Free app: the delete of instagram and the insert of
                        // com.reddit.frontpage both commit inside the
                        // transaction BEFORE the site guard trips.
                        appPackageNames = listOf("com.reddit.frontpage"),
                        // The edited block's own site plus one held by News.
                        siteDomains = listOf("reddit.com", "news.siteexample.com"),
                        frictionType = FrictionType.DELAY,
                        pauseMinutes = 60,
                        pauseChars = 200,
                        turnoffChars = 350,
                        countdownSeconds = 90,
                        turnoffSeconds = 360,
                    ),
                )
            }
        }
        assertEquals("news.siteexample.com", exception.target)
        assertFalse(exception.isApp)

        // Data-class equality covers every stored column: the block row
        // (name, friction, pause/turn-off settings, enabled) and each
        // child row (block_id, package/domain).
        assertEquals(beforeSocial, dao.getBlockWithContents(social))
        assertEquals(beforeNews, dao.getBlockWithContents(news))
        // The failing app mutation left nothing behind and nothing moved.
        assertEquals(beforeAppOwners, repo.appOwnerships())
        assertEquals(beforeSiteOwners, repo.siteOwnerships())
        assertNull(dao.findAppByPackageName("com.reddit.frontpage"))
    }

    // Re-adding one of the edited block's own sites under a different
    // spelling is a canonical no-op, not a conflict and not a duplicate.
    @Test
    fun updateBlockOwnSiteReAddIsCanonicalNoOp() = runBlocking {
        val social = repo.createBlock(draft("Social", sites = listOf("reddit.com")))

        repo.updateBlock(
            social,
            draft("Social", sites = listOf("REDDIT.COM")),
        )

        val updated = repo.getBlockWithContents(social)!!
        assertEquals(listOf("reddit.com"), updated.sites.map { it.domain })
    }

    // The picker's unavailable rows read from these maps; ownership is
    // reported by block id for both target kinds.
    @Test
    fun ownershipMapsExposeHeldTargets() = runBlocking {
        val social = repo.createBlock(
            draft("Social", apps = listOf("com.instagram.android"), sites = listOf("reddit.com")),
        )
        repo.createBlock(draft("News", apps = listOf("com.apple.news")))

        val appOwners = repo.appOwnerships()
        val siteOwners = repo.siteOwnerships()
        assertEquals(social, appOwners["com.instagram.android"])
        assertEquals(social, siteOwners["reddit.com"])
        assertFalse(appOwners.containsKey("com.reddit.frontpage"))
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
        val id = repo.createBlock(draft("Social", apps = listOf("com.instagram.android")))
        repo.setEnabled(id, true)

        val on = repo.getBlockWithContents(id)!!
        assertEquals(true, on.block.enabled)
        assertEquals("Social", on.block.name)
        assertEquals(25, on.block.pauseMinutes)
    }

    @Test
    fun enabledTransitionCommitsStateAndEventAtMostOnce() = runBlocking {
        val id = repo.createBlock(draft("Social", apps = listOf("com.instagram.android")))

        assertTrue(repo.setEnabledRecordingTransition(id, true))
        assertFalse(repo.setEnabledRecordingTransition(id, true))

        assertTrue(repo.getBlockWithContents(id)!!.block.enabled)
        val transitions = db.eventDao().getAll()
            .filter { it.name == EventRepository.EVENT_BLOCK_TURNED_ON }
        assertEquals(1, transitions.size)
        assertEquals(id, transitions.single().blockId)
    }

    @Test
    fun enabledTransitionRollsBackStateWhenEventInsertFails() = runBlocking {
        val id = repo.createBlock(draft("Social", apps = listOf("com.instagram.android")))
        db.openHelper.writableDatabase.execSQL(
            """CREATE TRIGGER fail_enabled_event
                BEFORE INSERT ON event
                WHEN NEW.name = '${EventRepository.EVENT_BLOCK_TURNED_ON}'
                BEGIN SELECT RAISE(ABORT, 'forced enabled event failure'); END""",
        )

        assertThrows(android.database.sqlite.SQLiteException::class.java) {
            runBlocking { repo.setEnabledRecordingTransition(id, true) }
        }

        assertFalse(repo.getBlockWithContents(id)!!.block.enabled)
        assertEquals(0, db.eventDao().countByName(EventRepository.EVENT_BLOCK_TURNED_ON))
    }

    // PRD §17 R8: a block must cover at least one target — the boundary
    // guards every write path, not just the editor's own gating.
    @Test
    fun createBlockRejectsEmptyDraftAndWritesNothing() = runBlocking {
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { repo.createBlock(draft("Empty")) }
        }

        assertTrue(repo.getBlocksWithContents().isEmpty())
    }

    @Test
    fun updateBlockRejectsEmptyDraftAndChangesNothing() = runBlocking {
        val social = repo.createBlock(
            draft("Social", apps = listOf("com.instagram.android"), sites = listOf("reddit.com")),
        )

        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { repo.updateBlock(social, draft("Emptied")) }
        }

        val unchanged = repo.getBlockWithContents(social)!!
        assertEquals("Social", unchanged.block.name)
        assertEquals(listOf("com.instagram.android"), unchanged.apps.map { it.packageName })
        assertEquals(listOf("reddit.com"), unchanged.sites.map { it.domain })
    }

    // Wizard redesign (19 September): the old 1..1200-second allowance is
    // superseded. Debug owner testing temporarily lowers the pause-wait floor
    // to 20 seconds; out-of-range writes persist nothing.
    @Test
    fun createBlockRejectsPauseWaitOutsideApprovedRange() = runBlocking {
        listOf(15, 305).forEach { seconds ->
            assertThrows(IllegalArgumentException::class.java) {
                runBlocking {
                    repo.createBlock(draft("Bad wait", apps = listOf("com.instagram.android"), countdownSeconds = seconds))
                }
            }
        }

        assertTrue(repo.getBlocksWithContents().isEmpty())
    }

    @Test
    fun updateBlockRejectsPauseWaitOutsideApprovedRangeAndChangesNothing() = runBlocking {
        val social = repo.createBlock(draft("Social", apps = listOf("com.instagram.android")))

        assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                repo.updateBlock(social, draft("Social", apps = listOf("com.instagram.android"), countdownSeconds = 310))
            }
        }

        assertEquals(90, repo.getBlockWithContents(social)!!.block.countdownSeconds)
    }

    // Pause minutes: 5..100 in steps of 5.
    @Test
    fun createBlockRejectsPauseMinutesOutsideApprovedRange() = runBlocking {
        listOf(0, 105).forEach { minutes ->
            assertThrows(IllegalArgumentException::class.java) {
                runBlocking {
                    repo.createBlock(draft("Bad pause", apps = listOf("com.instagram.android"), pauseMinutes = minutes))
                }
            }
        }

        assertTrue(repo.getBlocksWithContents().isEmpty())
    }

    // Debug owner testing temporarily lowers typing to 20..200 chars step 10.
    @Test
    fun createBlockRejectsPassageLengthOutsideApprovedRange() = runBlocking {
        listOf(10, 205, 155).forEach { chars ->
            assertThrows(IllegalArgumentException::class.java) {
                runBlocking {
                    repo.createBlock(draft("Bad passage", apps = listOf("com.instagram.android"), pauseChars = chars))
                }
            }
        }

        assertTrue(repo.getBlocksWithContents().isEmpty())
    }

    // Fixed disable ladders: only the approved rungs may be written, on both
    // per-method columns — the inactive column included.
    @Test
    fun createBlockRejectsDisableValuesOffTheLadders() = runBlocking {
        listOf(300, 220 + 10).forEach { chars ->
            assertThrows(IllegalArgumentException::class.java) {
                runBlocking {
                    repo.createBlock(draft("Bad typing disable", apps = listOf("com.instagram.android"), turnoffChars = chars))
                }
            }
        }
        listOf(3600, 200).forEach { seconds ->
            assertThrows(IllegalArgumentException::class.java) {
                runBlocking {
                    repo.createBlock(
                        draft("Bad wait disable", apps = listOf("com.instagram.android"), turnoffSeconds = seconds),
                    )
                }
            }
        }

        assertTrue(repo.getBlocksWithContents().isEmpty())
    }

    @Test
    fun updateBlockRejectsDisableValueOffTheLadderAndChangesNothing() = runBlocking {
        val social = repo.createBlock(draft("Social", apps = listOf("com.instagram.android")))

        assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                repo.updateBlock(
                    social,
                    draft("Social", apps = listOf("com.instagram.android"), turnoffSeconds = 120),
                )
            }
        }

        assertEquals(360, repo.getBlockWithContents(social)!!.block.turnoffSeconds)
    }

    // Every approved rung of both ladders round-trips through the
    // persistence boundary.
    @Test
    fun approvedDisableLadderValuesRoundTrip() = runBlocking {
        DISABLE_CHARS_CHOICES.forEachIndexed { index, chars ->
            val id = repo.createBlock(
                draft(
                    "Typing $chars",
                    apps = listOf("com.instagram.android"),
                    turnoffChars = chars,
                    turnoffSeconds = DISABLE_WAIT_SECONDS_CHOICES[index],
                ),
            )
            val read = repo.getBlockWithContents(id)!!
            assertEquals(chars, read.block.turnoffChars)
            assertEquals(DISABLE_WAIT_SECONDS_CHOICES[index], read.block.turnoffSeconds)
            repo.deleteBlock(id)
        }
        assertTrue(repo.getBlocksWithContents().isEmpty())
    }
}

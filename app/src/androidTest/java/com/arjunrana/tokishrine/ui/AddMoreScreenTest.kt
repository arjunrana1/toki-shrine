package com.arjunrana.tokishrine.ui

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.isNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.arjunrana.tokishrine.data.apps.AppEntry
import com.arjunrana.tokishrine.data.apps.InstalledAppsRepository
import com.arjunrana.tokishrine.data.db.BlockDao
import com.arjunrana.tokishrine.data.db.TokiDatabase
import com.arjunrana.tokishrine.data.entity.FrictionType
import com.arjunrana.tokishrine.data.repo.BlockDraft
import com.arjunrana.tokishrine.data.repo.BlockRepository
import com.arjunrana.tokishrine.ui.screens.AddMoreScreen
import com.arjunrana.tokishrine.ui.theme.NocturneTheme
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/*
 * P7-F5 Add more regressions (compiled, not executed in this delta — no
 * device/emulator run is authorized): the add-only screen over the real
 * repository — existing targets render locked, a save through "Confirm and
 * Save Block" writes exactly the additions on an ON block, Never mind saves
 * nothing, and a domain another block owns surfaces the mapped "Already
 * added to a block" copy. Compiled-only until Arjun authorizes execution.
 */
@RunWith(AndroidJUnit4::class)
class AddMoreScreenTest {

    companion object {
        // Phosphor glyph text node: the selected-row remove X.
        private const val REMOVE_GLYPH = "\ue4f6"
    }

    @get:Rule
    val compose = createAndroidComposeRule<CreateFlowTestActivity>()

    private lateinit var db: TokiDatabase
    private lateinit var dao: BlockDao
    private lateinit var blockRepo: BlockRepository

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            TokiDatabase::class.java,
        ).build()
        dao = db.blockDao()
        blockRepo = BlockRepository(db)
    }

    @After
    fun tearDown() {
        CreateFlowTestActivity.content = null
        db.close()
    }

    private class StubAppsRepository(
        private val entries: List<AppEntry>,
    ) : InstalledAppsRepository(ApplicationProvider.getApplicationContext()) {
        override suspend fun loadApps(): List<AppEntry> = entries
        override fun labelFor(packageName: String): String =
            entries.firstOrNull { it.packageName == packageName }?.label ?: packageName
    }

    private fun stubApps(vararg entries: AppEntry) = StubAppsRepository(entries.toList())

    private fun draft(name: String, apps: List<String>, sites: List<String>) = BlockDraft(
        name = name,
        appPackageNames = apps,
        siteDomains = sites,
        frictionType = FrictionType.TYPING,
        pauseMinutes = 15,
        pauseChars = 120,
        turnoffChars = 350,
        countdownSeconds = 60,
        turnoffSeconds = 360,
    )

    /** Seeds the block under test ON with Instagram + reddit.com and shows the screen. */
    private fun showScreen(
        apps: InstalledAppsRepository = stubApps(AppEntry("com.instagram", "Instagram", null)),
        doneCount: AtomicInteger = AtomicInteger(0),
    ): Long {
        var id = -1L
        runBlocking {
            id = blockRepo.createBlock(draft("Social", listOf("com.instagram"), listOf("reddit.com")))
            blockRepo.setEnabled(id, true)
        }
        CreateFlowTestActivity.content = {
            NocturneTheme {
                AddMoreScreen(
                    blockId = id,
                    blockRepo = blockRepo,
                    appsRepo = apps,
                    onDone = { doneCount.incrementAndGet() },
                )
            }
        }
        // Same lifecycle participation as CreateFlowScreenTest: reinstall the
        // configured root from onCreate.
        compose.activityRule.scenario.recreate()
        awaitText("What should this cover?")
        return id
    }

    private fun awaitText(text: String, timeoutMs: Long = 5_000) {
        compose.waitUntil(timeoutMillis = timeoutMs) {
            compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText(text).assertExists()
    }

    private fun addSite(domain: String) {
        compose.onNodeWithText("Websites").performClick()
        compose.onNode(hasSetTextAction()).performTextReplacement(domain)
        // The Add button only enables once ownership is known and the input
        // is a free, complete domain.
        compose.waitUntil {
            compose.onAllNodes(hasText("Add").and(isEnabled())).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Add").performClick()
        awaitText(domain)
    }

    @Test
    fun confirmSavesOnlyAdditionsToTheOnBlockAndExistingTargetsStayLocked() {
        val done = AtomicInteger(0)
        val id = showScreen(doneCount = done)
        awaitText("Instagram")
        awaitText("reddit.com")
        awaitText("2 selected")
        // The block's own targets render included with no remove affordance.
        compose.onAllNodesWithText(REMOVE_GLYPH).assertCountEquals(0)

        addSite("example.com")
        awaitText("3 selected")
        compose.onNodeWithText("Confirm and Save Block").performClick()
        compose.waitUntil { done.get() == 1 }

        runBlocking {
            val stored = blockRepo.getBlockWithContents(id)!!
            assertEquals(listOf("com.instagram"), stored.apps.map { it.packageName })
            assertEquals(setOf("reddit.com", "example.com"), stored.sites.map { it.domain }.toSet())
            assertTrue(stored.block.enabled)
        }
    }

    @Test
    fun neverMindAndBackReturnWithoutSavingAnything() {
        val done = AtomicInteger(0)
        val id = showScreen(doneCount = done)
        addSite("example.com")
        compose.onNodeWithText("Never mind").performClick()
        compose.waitUntil { done.get() == 1 }
        runBlocking {
            val stored = blockRepo.getBlockWithContents(id)!!
            assertEquals(listOf("reddit.com"), stored.sites.map { it.domain })
            assertTrue(stored.block.enabled)
        }
    }

    @Test
    fun aDomainAnotherBlockOwnsShowsTheMappedCopyAndCannotBeAdded() {
        runBlocking { blockRepo.createBlock(draft("Holder", emptyList(), listOf("twitter.com"))) }
        showScreen()
        compose.onNodeWithText("Websites").performClick()
        compose.onNode(hasSetTextAction()).performTextReplacement("twitter.com")
        awaitText("Already added to a block")
        compose.waitUntil(5_000) {
            compose.onAllNodes(hasText("Add").and(isNotEnabled())).fetchSemanticsNodes().isNotEmpty()
        }
    }
}

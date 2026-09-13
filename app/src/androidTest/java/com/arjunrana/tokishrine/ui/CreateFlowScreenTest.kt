package com.arjunrana.tokishrine.ui

import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.isNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.activity.ComponentActivity
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.arjunrana.tokishrine.data.apps.AppEntry
import com.arjunrana.tokishrine.data.apps.InstalledAppsRepository
import com.arjunrana.tokishrine.data.db.BlockDao
import com.arjunrana.tokishrine.data.db.EventDao
import com.arjunrana.tokishrine.data.db.TokiDatabase
import com.arjunrana.tokishrine.data.entity.BlockedApp
import com.arjunrana.tokishrine.data.entity.Block
import com.arjunrana.tokishrine.data.entity.Event
import com.arjunrana.tokishrine.data.entity.FrictionType
import com.arjunrana.tokishrine.data.repo.BlockDraft
import com.arjunrana.tokishrine.data.repo.BlockRepository
import com.arjunrana.tokishrine.data.repo.EventRepository
import com.arjunrana.tokishrine.ui.screens.CreateFlowScreen
import com.arjunrana.tokishrine.ui.theme.NocturneTheme
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// Regressions for the Phase 2 code-review blockers: the terminal transition
// (save/abandon) must happen exactly once, and no target may be selected or
// saved before ownership is known — a rejected save keeps the draft. Drives
// the real CreateFlowScreen against an in-memory Room database.
@RunWith(AndroidJUnit4::class)
class CreateFlowScreenTest {

    companion object {
        // Phosphor glyph text nodes (arrow-left appbar back, X remove). The
        // glyph characters are the nodes' text content.
        private const val BACK_GLYPH = "\ue058"
        private const val REMOVE_GLYPH = "\ue4f6"

        // Conflict shown/resolved telemetry is retired with the removed
        // conflict flow; no flow may ever write these again.
        private val RETIRED_CONFLICT_EVENTS = setOf("block_conflict_shown", "block_conflict_resolved")
    }

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private lateinit var db: TokiDatabase
    private lateinit var dao: BlockDao
    private lateinit var eventDao: EventDao
    private lateinit var blockRepo: BlockRepository
    private lateinit var eventRepo: GatedEventRepository

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            TokiDatabase::class.java,
        ).build()
        dao = db.blockDao()
        eventDao = db.eventDao()
        blockRepo = BlockRepository(db)
        eventRepo = GatedEventRepository(db)
    }

    @After
    fun tearDown() {
        db.close()
    }

    // — gates (suspending: the flow's coroutines run on the main dispatcher) —

    private class SuspensionGate {
        private val latch = CountDownLatch(1)

        suspend fun await() {
            withContext(Dispatchers.IO) { latch.await(10, TimeUnit.SECONDS) }
        }

        fun release() {
            latch.countDown()
        }
    }

    private class GatedEventRepository(db: TokiDatabase) : EventRepository(db.eventDao(), db.appMetaDao()) {
        val gatedNames = mutableSetOf<String>()
        val gate = SuspensionGate()

        override suspend fun log(
            name: String,
            blockId: Long?,
            target: String?,
            targetType: String?,
            params: Map<String, Any?>,
        ) {
            if (name in gatedNames) gate.await()
            super.log(name, blockId, target, targetType, params)
        }
    }

    private class GatedBlockRepository(db: TokiDatabase) : BlockRepository(db) {
        var appGate: SuspensionGate? = null
        var siteGate: SuspensionGate? = null

        override suspend fun appOwnerships(): Map<String, Long> {
            appGate?.let { it.await() }
            return super.appOwnerships()
        }

        override suspend fun siteOwnerships(): Map<String, Long> {
            siteGate?.let { it.await() }
            return super.siteOwnerships()
        }
    }

    private class StubAppsRepository(
        private val entries: List<AppEntry>,
    ) : InstalledAppsRepository(ApplicationProvider.getApplicationContext()) {
        override suspend fun loadApps(): List<AppEntry> = entries
        override fun labelFor(packageName: String): String =
            entries.firstOrNull { it.packageName == packageName }?.label ?: packageName
    }

    // — helpers —

    private fun stubApps(vararg entries: AppEntry) = StubAppsRepository(entries.toList())

    private fun appEntry(pkg: String, label: String) = AppEntry(pkg, label, null)

    private fun draft(
        name: String,
        apps: List<String> = emptyList(),
        sites: List<String> = emptyList(),
    ) = BlockDraft(
        name = name,
        appPackageNames = apps,
        siteDomains = sites,
        frictionType = FrictionType.TYPING,
        pauseMinutes = 25,
        pauseChars = 120,
        turnoffChars = 320,
        countdownSeconds = 45,
        showTypos = false,
    )

    private fun showFlow(
        editBlockId: Long?,
        repo: BlockRepository = blockRepo,
        apps: InstalledAppsRepository = stubApps(),
        initialStep: Int = 1,
    ): AtomicInteger {
        val closeCount = AtomicInteger(0)
        compose.setContent {
            NocturneTheme {
                CreateFlowScreen(
                    editBlockId = editBlockId,
                    blockRepo = repo,
                    eventRepo = eventRepo,
                    appsRepo = apps,
                    onClose = { closeCount.incrementAndGet() },
                    initialStep = initialStep,
                )
            }
        }
        return closeCount
    }

    // Drafts a free app through the picker. A holder block owning
    // com.apple.news is created first so the picker's "Already added to a
    // block" pill doubles as the ownership-loaded signal before the free app
    // is tapped (rows stay inert until ownership is known).
    private fun seedHolderAndPickFreeApp(freeApp: AppEntry) {
        runBlocking { blockRepo.createBlock(draft("Holder", apps = listOf("com.apple.news"))) }
        compose.onNodeWithText("Search your installed apps…").performClick()
        awaitText("Already added to a block")
        compose.onNodeWithText(freeApp.label).performClick()
        compose.onNodeWithText("Added").assertExists()
        compose.onNodeWithText("Done").performClick()
        awaitText("1 selected")
    }

    private fun awaitText(text: String, timeoutMs: Long = 5_000) {
        compose.waitUntil(timeoutMillis = timeoutMs) {
            compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText(text).assertExists()
    }

    private fun typeBlockName(name: String) {
        compose.onNode(hasSetTextAction()).performTextReplacement(name)
    }

    private fun next() {
        compose.onNodeWithText("Next").performClick()
    }

    private fun events(): List<Event> = runBlocking { eventDao.getAll() }

    private fun assertNoRetiredConflictEvents() {
        assertTrue(
            "retired conflict telemetry fired: " + events().map { it.name },
            events().none { it.name in RETIRED_CONFLICT_EVENTS },
        )
    }

    // — blocker 1: the terminal transition happens exactly once —

    // Two appbar Backs while the abandonment write is suspended: the second
    // finds the guard taken and does nothing. Exactly one
    // block_create_abandoned(step=1), exactly one navigation.
    @Test
    fun repeatedBackWhileLoggingSuspendsLogsAbandonmentExactlyOnce() {
        eventRepo.gatedNames.add(EventRepository.EVENT_BLOCK_CREATE_ABANDONED)
        val closeCount = showFlow(editBlockId = null)

        awaitText("What should this cover?") // step 1
        compose.onNodeWithText(BACK_GLYPH).performClick()
        compose.onNodeWithText(BACK_GLYPH).performClick()

        // Still inside the suspended window: nothing recorded, nothing closed.
        compose.waitForIdle()
        assertEquals(0, closeCount.get())
        assertEquals(0, runBlocking { eventDao.countByName(EventRepository.EVENT_BLOCK_CREATE_ABANDONED) })

        eventRepo.gate.release()
        compose.waitUntil(timeoutMillis = 5_000) { closeCount.get() == 1 }
        compose.waitForIdle()
        assertEquals(1, closeCount.get())

        val recorded = events()
        assertEquals(
            listOf(EventRepository.EVENT_BLOCK_CREATE_STARTED, EventRepository.EVENT_BLOCK_CREATE_ABANDONED),
            recorded.map { it.name },
        )
        val abandoned = recorded.last()
        assertEquals(1, JSONObject(abandoned.paramsJson!!).getInt("step"))
        assertNoRetiredConflictEvents()
    }

    // Two Save taps while the block_created write is suspended: the second
    // finds the guard taken and does nothing. Exactly one block, exactly one
    // block_created, exactly one navigation — and no abandonment after it.
    // PRD §17 R8 made empty saves impossible, so the block carries a target.
    @Test
    fun repeatedSaveWhilePersistSuspendsCreatesExactlyOnce() {
        eventRepo.gatedNames.add(EventRepository.EVENT_BLOCK_CREATED)
        val freeApp = appEntry("com.sleeper.app", "Sleeper")
        val closeCount = showFlow(
            editBlockId = null,
            apps = stubApps(appEntry("com.apple.news", "Apple News"), freeApp),
        )

        awaitText("What should this cover?")
        seedHolderAndPickFreeApp(freeApp)
        next()
        typeBlockName("Evening lockout")
        next()
        next()
        awaitText("4 / 4")
        compose.onNodeWithText("Save block").performClick()
        compose.onNodeWithText("Save block").performClick()

        compose.waitForIdle()
        assertEquals(0, closeCount.get())
        assertEquals(0, runBlocking { eventDao.countByName(EventRepository.EVENT_BLOCK_CREATED) })

        eventRepo.gate.release()
        compose.waitUntil(timeoutMillis = 5_000) { closeCount.get() == 1 }
        compose.waitForIdle()
        assertEquals(1, closeCount.get())

        val recorded = events()
        assertEquals(
            listOf(
                EventRepository.EVENT_BLOCK_CREATE_STARTED,
                EventRepository.EVENT_BLOCK_CREATE_STEP_COMPLETED,
                EventRepository.EVENT_BLOCK_CREATE_STEP_COMPLETED,
                EventRepository.EVENT_BLOCK_CREATE_STEP_COMPLETED,
                EventRepository.EVENT_BLOCK_CREATE_STEP_COMPLETED,
                EventRepository.EVENT_BLOCK_CREATED,
            ),
            recorded.map { it.name },
        )
        assertEquals(listOf(1, 2, 3, 4), recorded.drop(1).take(4).map { JSONObject(it.paramsJson!!).getInt("step") })
        val created = recorded.last()
        val params = JSONObject(created.paramsJson!!)
        assertEquals(1, params.getInt("app_count"))
        assertEquals(0, params.getInt("site_count"))
        assertEquals("typing", params.getString("friction_type"))
        assertEquals(15, params.getInt("pause_minutes"))
        assertEquals(100, params.getInt("pause_chars"))
        assertEquals(300, params.getInt("turnoff_chars"))
        assertEquals(30, params.getInt("countdown_seconds"))

        // The seeded holder plus exactly one new block.
        val blocks = runBlocking { blockRepo.getBlocksWithContents() }
        assertEquals(2, blocks.size)
        val saved = blocks.first { it.block.name == "Evening lockout" }
        assertEquals(listOf("com.sleeper.app"), saved.apps.map { it.packageName })
        assertEquals(false, saved.block.enabled)
        assertNoRetiredConflictEvents()
    }

    // — blocker 2: selection waits for ownership; rejected saves keep the draft —

    // App rows render before the ownership read finishes; until it does they
    // are inert, so nothing can enter the draft unchecked. Afterwards held
    // rows stay unselectable and free rows work.
    @Test
    fun appSelectionIsDisabledUntilOwnershipLoads() {
        val gate = SuspensionGate()
        val gatedRepo = GatedBlockRepository(db).apply { appGate = gate }
        runBlocking { gatedRepo.createBlock(draft("Holder", apps = listOf("com.instagram.android"))) }
        val closeCount = showFlow(
            editBlockId = null,
            repo = gatedRepo,
            apps = stubApps(appEntry("com.instagram.android", "Instagram"), appEntry("com.sleeper.app", "Sleeper")),
        )

        compose.onNodeWithText("Search your installed apps…").performClick()
        awaitText("Sleeper")
        compose.onNodeWithText("Already added to a block").assertDoesNotExist()

        // Ownership unknown: rows exist but are disabled, so taps do nothing.
        compose.onNodeWithText("Instagram").assert(isNotEnabled())
        compose.onNodeWithText("Instagram").performTouchInput { click() }
        compose.onNodeWithText("Sleeper").assert(isNotEnabled())
        compose.onNodeWithText("Sleeper").performTouchInput { click() }
        compose.onAllNodesWithText("Added").assertCountEquals(0)

        gate.release()
        awaitText("Already added to a block") // HeldPill for Instagram

        compose.onNodeWithText("Instagram").performTouchInput { click() }
        compose.onAllNodesWithText("Added").assertCountEquals(0)
        compose.onNodeWithText("Sleeper").performClick()
        compose.onNodeWithText("Added").assertExists()

        compose.onNodeWithText("Done").performClick()
        awaitText("1 selected")
        compose.onNodeWithText("Sleeper").assertExists()
        assertNoRetiredConflictEvents()
    }

    // The website field behaves the same: Add stays inert until ownership
    // loads, a held domain shows the inline label with Add disabled, and a
    // free domain adds normally.
    @Test
    fun siteAddIsDisabledUntilOwnershipLoads() {
        val gate = SuspensionGate()
        val gatedRepo = GatedBlockRepository(db).apply { siteGate = gate }
        runBlocking { gatedRepo.createBlock(draft("Holder", sites = listOf("reddit.com"))) }
        val closeCount = showFlow(editBlockId = null, repo = gatedRepo)

        compose.onNodeWithText("Websites").performClick()
        compose.onNode(hasSetTextAction()).performTextReplacement("reddit.com")

        compose.onNodeWithText("Add").assert(isNotEnabled())
        compose.onNodeWithText("Already added to a block").assertDoesNotExist()
        compose.onNodeWithText("Add").performTouchInput { click() }
        compose.onNodeWithText("0 selected").assertExists()

        gate.release()
        awaitText("Already added to a block")
        compose.onNodeWithText("Add").assert(isNotEnabled())
        compose.onNodeWithText("Add").performTouchInput { click() }
        compose.onNodeWithText("0 selected").assertExists()

        compose.onNode(hasSetTextAction()).performTextReplacement("free.example.com")
        compose.onNodeWithText("Add").assertHasClickAction()
        compose.onNodeWithText("Add").performClick()
        awaitText("1 selected")
        compose.onNodeWithText("free.example.com").assertExists()
        assertNoRetiredConflictEvents()
    }    // A stale draft (the drafted app was claimed by another block while the
    // editor was open) is rejected at the repository: the flow stays open,
    // the draft survives, the inline label shows without naming the owner,
    // and nothing is written. Removing the held app then saves normally,
    // proving the terminal guard was released for retries.
    @Test
    fun rejectedSaveRetainsDraftAndShowsInlineMessage() {
        val socialId = runBlocking {
            blockRepo.createBlock(draft("Social", apps = listOf("com.instagram.android"), sites = listOf("reddit.com")))
        }
        val newsId = runBlocking {
            blockRepo.createBlock(draft("News holder", apps = listOf("com.apple.news")))
        }
        val closeCount = showFlow(
            editBlockId = socialId,
            apps = stubApps(
                appEntry("com.instagram.android", "Instagram"),
                appEntry("com.apple.news", "Apple News"),
                appEntry("com.sleeper.app", "Sleeper"),
            ),
        )

        awaitText("Instagram") // edit prefill landed
        compose.onNodeWithText("reddit.com").assertExists()

        // Draft a still-free app through the picker.
        compose.onNodeWithText("Search your installed apps…").performClick()
        awaitText("Already added to a block") // Apple News pill: ownership has loaded
        compose.onNodeWithText("Sleeper").performClick()
        // Two pills: the edited block's own Instagram shows as Added already.
        compose.onAllNodesWithText("Added").assertCountEquals(2)
        compose.onNodeWithText("Done").performClick()
        awaitText("3 selected")

        // Out-of-band write claims the drafted app: the draft is now stale.
        runBlocking { dao.insertApp(BlockedApp(blockId = newsId, packageName = "com.sleeper.app")) }

        next()
        next()
        next()
        awaitText("4 / 4")
        compose.onNodeWithText("Sleeper").assertExists() // review chip: draft intact
        compose.onNodeWithText("Save block").performClick()

        awaitText("Already added to a block") // inline explanation on the review step
        assertEquals(0, closeCount.get())
        compose.onNodeWithText("4 / 4").assertExists()
        compose.onNodeWithText("Sleeper").assertExists()

        // Nothing persisted; the holder still owns the app.
        val untouched = runBlocking { blockRepo.getBlockWithContents(socialId)!! }
        assertEquals("Social", untouched.block.name)
        assertEquals(listOf("com.instagram.android"), untouched.apps.map { it.packageName })
        assertEquals(listOf("reddit.com"), untouched.sites.map { it.domain })
        assertEquals(newsId, runBlocking { dao.findAppByPackageName("com.sleeper.app")?.blockId })
        assertTrue(events().isEmpty()) // edit flow: no create telemetry, no block_edited
        assertNoRetiredConflictEvents()

        // Recovery: unwind, drop the held app, save again — the guard is free.
        compose.onNodeWithText(BACK_GLYPH).performClick()
        compose.onNodeWithText(BACK_GLYPH).performClick()
        compose.onNodeWithText(BACK_GLYPH).performClick()
        awaitText("What should this cover?")
        // Selected rows: apps [Instagram, Sleeper] then sites [reddit.com].
        compose.onAllNodesWithText(REMOVE_GLYPH)[1].performClick()
        awaitText("2 selected")
        next()
        next()
        next()
        compose.onNodeWithText("Save block").performClick()
        compose.waitUntil(timeoutMillis = 5_000) { closeCount.get() == 1 }
        assertEquals(1, closeCount.get())

        assertEquals(listOf(EventRepository.EVENT_BLOCK_EDITED), events().map { it.name })
        assertEquals("none", JSONObject(events().last().paramsJson!!).getString("fields_changed"))
        val recovered = runBlocking { blockRepo.getBlockWithContents(socialId)!! }
        assertEquals(listOf("com.instagram.android"), recovered.apps.map { it.packageName })
        assertEquals(newsId, runBlocking { dao.findAppByPackageName("com.sleeper.app")?.blockId })
    }

    // Owner addendum, 13 September: the website field is single-line —
    // pasted line breaks are stripped before the value lands — and Add
    // stays unavailable until the text is a complete domain like
    // reddit.com; bare words ("reddit") never pass.
    @Test
    fun siteAddRequiresACompleteSingleLineDomain() {
        showFlow(editBlockId = null)

        compose.onNodeWithText("Websites").performClick()
        val field = compose.onNode(hasSetTextAction())

        // A dot-less word is rejected with an explanation.
        field.performTextReplacement("reddit")
        compose.onNodeWithText("Add").assert(isNotEnabled())
        awaitText("Enter a complete domain like reddit.com")

        // Newlines never survive into the value (the owner's two-line case).
        field.performTextReplacement("abc\ndef")
        compose.onNodeWithText("abcdef").assertExists()
        compose.onNodeWithText("Add").assert(isNotEnabled())

        // A complete domain unlocks Add.
        field.performTextReplacement("free.example.com")
        compose.waitUntil(timeoutMillis = 5_000) {
            try {
                compose.onNodeWithText("Add").assertHasClickAction()
                true
            } catch (e: AssertionError) {
                false
            }
        }
        compose.onNodeWithText("Add").performClick()
        awaitText("1 selected")
        compose.onNodeWithText("free.example.com").assertExists()
    }

    // — event-table evidence: create success/cancel, search/step Back, edit cancel —

    // Search Back and step Back never leave the flow, so they log nothing
    // terminal; every Next logs its step; the save is the single terminal
    // event. PRD §17 R8: the block carries a target — empty saves are gone.
    @Test
    fun searchBackAndStepBackLogExactSequence() {
        val freeApp = appEntry("com.sleeper.app", "Sleeper")
        val closeCount = showFlow(
            editBlockId = null,
            apps = stubApps(appEntry("com.apple.news", "Apple News"), freeApp),
        )

        compose.onNodeWithText("Search your installed apps…").performClick()
        awaitText("Done")
        compose.onNodeWithText(BACK_GLYPH).performClick() // search exits, nothing logged
        awaitText("Search your installed apps…")

        seedHolderAndPickFreeApp(freeApp)
        next() // step_completed(1)
        typeBlockName("Evening lockout")
        next() // step_completed(2)
        next() // step_completed(3)
        compose.onNodeWithText(BACK_GLYPH).performClick() // 4 → 3, nothing logged
        next() // step_completed(3) again
        awaitText("4 / 4")
        compose.onNodeWithText("Save block").performClick()
        compose.waitUntil(timeoutMillis = 5_000) { closeCount.get() == 1 }
        assertEquals(1, closeCount.get())

        assertEquals(
            listOf(
                EventRepository.EVENT_BLOCK_CREATE_STARTED,
                EventRepository.EVENT_BLOCK_CREATE_STEP_COMPLETED,
                EventRepository.EVENT_BLOCK_CREATE_STEP_COMPLETED,
                EventRepository.EVENT_BLOCK_CREATE_STEP_COMPLETED,
                EventRepository.EVENT_BLOCK_CREATE_STEP_COMPLETED,
                EventRepository.EVENT_BLOCK_CREATE_STEP_COMPLETED,
                EventRepository.EVENT_BLOCK_CREATED,
            ),
            events().map { it.name },
        )
        assertEquals(
            listOf(1, 2, 3, 3, 4),
            events().drop(1).take(5).map { JSONObject(it.paramsJson!!).getInt("step") },
        )
        assertNoRetiredConflictEvents()
    }

    // Cancelling an edit logs nothing at all: no create_started, no
    // block_create_abandoned, no block_edited, no retired conflict events.
    @Test
    fun editCancellationLogsNothing() {
        val socialId = runBlocking {
            blockRepo.createBlock(draft("Social", apps = listOf("com.instagram.android")))
        }
        val closeCount = showFlow(
            editBlockId = socialId,
            apps = stubApps(appEntry("com.instagram.android", "Instagram")),
        )

        awaitText("Instagram")
        compose.onNodeWithText(BACK_GLYPH).performClick()
        compose.waitUntil(timeoutMillis = 5_000) { closeCount.get() == 1 }
        compose.waitForIdle()
        assertEquals(1, closeCount.get())
        assertEquals(0, runBlocking { eventDao.countAll() })
        assertNoRetiredConflictEvents()
    }

    // — PRD §17 refinements —

    // R8: every target may be removed temporarily, but step 1 cannot be
    // left (and nothing saved) while the selection is empty.
    @Test
    fun stepOneNextStaysDisabledUntilATargetIsSelected() {
        val freeApp = appEntry("com.sleeper.app", "Sleeper")
        showFlow(
            editBlockId = null,
            apps = stubApps(appEntry("com.apple.news", "Apple News"), freeApp),
        )

        awaitText("What should this cover?")
        compose.onNodeWithText("Next").assert(isNotEnabled())
        compose.onNodeWithText("Next").performTouchInput { click() }
        compose.onNodeWithText("Give it a name").assertDoesNotExist() // still step 1

        seedHolderAndPickFreeApp(freeApp)
        compose.onNodeWithText("Next").performClick()
        awaitText("Give it a name")
        assertNoRetiredConflictEvents()
    }

    // R7 (cap raised 20 → 30 after device testing): the name field is
    // single-line with a 30-character cap. Normal and boundary-length input
    // stores as typed; an over-limit update truncates to the first 30
    // characters; pasted line breaks are stripped before the cap is applied,
    // so they can never reach the stored name.
    @Test
    fun nameInputStripsLineBreaksAndTruncatesAtThirtyCharacters() {
        val freeApp = appEntry("com.sleeper.app", "Sleeper")
        showFlow(
            editBlockId = null,
            apps = stubApps(appEntry("com.apple.news", "Apple News"), freeApp),
        )

        awaitText("What should this cover?")
        seedHolderAndPickFreeApp(freeApp)
        next()

        // Normal input stores as typed.
        typeBlockName("Evening")
        compose.onNodeWithText("Evening").assertExists()

        // Exactly 30 characters is accepted in full.
        val thirty = "abcdefghij".repeat(3)
        typeBlockName(thirty)
        compose.onNodeWithText(thirty).assertExists()

        // Over-limit input is truncated to the first 30 characters — the
        // cap holds and the field shows exactly what is stored.
        typeBlockName(thirty + "abcde")
        compose.onNodeWithText(thirty + "abcde").assertDoesNotExist()
        compose.onNodeWithText(thirty).assertExists()

        // Pasted line breaks are stripped on their own…
        typeBlockName("ab\ncd\r\nef")
        compose.onNodeWithText("abcdef").assertExists()

        // …and the cap applies to the single-line result: 33 raw characters
        // with one break still truncate to 30.
        typeBlockName(thirty + "\nabc")
        compose.onNodeWithText(thirty).assertExists()
    }

    // R9: detail's THE FRICTION → Edit opens the editor directly at 3/4
    // with the stored values loaded; Back at that entry step closes the
    // flow (returning to detail in real navigation) without any event.
    @Test
    fun frictionEditEntersAtStepThreeAndBackClosesSilently() {
        val socialId = runBlocking {
            blockRepo.createBlock(draft("Social", apps = listOf("com.instagram.android")))
        }
        val closeCount = showFlow(
            editBlockId = socialId,
            apps = stubApps(appEntry("com.instagram.android", "Instagram")),
            initialStep = 3,
        )

        awaitText("3 / 4")
        awaitText("Type to pause")
        // Stored pauseChars (120, not the 100 default) proves the prefill
        // landed; the nonempty draft may proceed from the friction step.
        awaitText("120 characters")
        compose.onNodeWithText("Next").assertHasClickAction()

        compose.onNodeWithText(BACK_GLYPH).performClick() // entry step: exits, not step 2
        compose.waitUntil(timeoutMillis = 5_000) { closeCount.get() == 1 }
        assertEquals(1, closeCount.get())
        assertTrue(events().isEmpty()) // edit cancellation stays silent
    }

    // R8: a legacy empty block (seeded directly — the repository now rejects
    // empty drafts) opened at the friction step can neither advance nor
    // save; Back still exits.
    @Test
    fun frictionEditOnAnEmptyBlockCannotAdvance() {
        val emptyId = runBlocking {
            dao.insertBlock(
                Block(
                    name = "Legacy empty",
                    frictionType = FrictionType.TYPING,
                    pauseMinutes = 25,
                    pauseChars = 120,
                    turnoffChars = 320,
                    countdownSeconds = 45,
                    showTypos = false,
                    enabled = false,
                ),
            )
        }
        val closeCount = showFlow(editBlockId = emptyId, initialStep = 3)

        awaitText("3 / 4")
        compose.onNodeWithText("Next").assert(isNotEnabled())
        compose.onNodeWithText("Next").performTouchInput { click() }
        compose.onNodeWithText("4 / 4").assertDoesNotExist()

        compose.onNodeWithText(BACK_GLYPH).performClick()
        compose.waitUntil(timeoutMillis = 5_000) { closeCount.get() == 1 }
        assertEquals(1, closeCount.get())
        assertTrue(events().isEmpty())
    }
}

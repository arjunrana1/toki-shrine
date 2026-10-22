package com.arjunrana.timeshrine.ui

import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.isNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.arjunrana.timeshrine.config.AppConfig.PAUSE_CHARS_DEFAULT
import com.arjunrana.timeshrine.config.AppConfig.PAUSE_WAIT_SECONDS_DEFAULT
import com.arjunrana.timeshrine.data.apps.AppEntry
import com.arjunrana.timeshrine.data.apps.InstalledAppsRepository
import com.arjunrana.timeshrine.data.db.BlockDao
import com.arjunrana.timeshrine.data.db.EventDao
import com.arjunrana.timeshrine.data.db.TimeShrineDatabase
import com.arjunrana.timeshrine.data.entity.BlockedApp
import com.arjunrana.timeshrine.data.entity.Block
import com.arjunrana.timeshrine.data.entity.Event
import com.arjunrana.timeshrine.data.entity.FrictionType
import com.arjunrana.timeshrine.data.repo.BlockDraft
import com.arjunrana.timeshrine.data.repo.BlockRepository
import com.arjunrana.timeshrine.data.repo.DISABLE_CHARS_DEFAULT
import com.arjunrana.timeshrine.data.repo.DISABLE_WAIT_SECONDS_DEFAULT
import com.arjunrana.timeshrine.data.repo.EventRepository
import com.arjunrana.timeshrine.ui.screens.CreateFlowScreen
import com.arjunrana.timeshrine.ui.theme.NocturneTheme
import com.arjunrana.timeshrine.ui.util.formatCountdown
import androidx.compose.ui.semantics.SemanticsActions
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
// saved before ownership is known — a rejected save keeps the draft. Also
// the wizard redesign (19 September): five steps, inline method cards on
// step 3 (09 v3, 11 October: no details sheet), per-method drafts and the
// fixed disable ladders. Drives the real
// CreateFlowScreen against an in-memory Room database.
@RunWith(AndroidJUnit4::class)
class CreateFlowScreenTest {

    companion object {
        // The appbar back control is named "Back"; its glyph is cleared from
        // the merged tree (Phase 7 accessibility pass, launch-gate LG-4).
        private const val BACK = "Back"

        // Phosphor glyph text nodes (X remove, plus). The glyph characters
        // are the nodes' text content.
        private const val REMOVE_GLYPH = "\ue4f6"
        private const val PLUS_GLYPH = "\ue3d4"

        // Conflict shown/resolved telemetry is retired with the removed
        // conflict flow; no flow may ever write these again.
        private val RETIRED_CONFLICT_EVENTS = setOf("block_conflict_shown", "block_conflict_resolved")
    }

    @get:Rule
    val compose = createAndroidComposeRule<CreateFlowTestActivity>()

    private lateinit var db: TimeShrineDatabase
    private lateinit var dao: BlockDao
    private lateinit var eventDao: EventDao
    private lateinit var blockRepo: BlockRepository
    private lateinit var eventRepo: GatedEventRepository

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            TimeShrineDatabase::class.java,
        ).build()
        dao = db.blockDao()
        eventDao = db.eventDao()
        blockRepo = BlockRepository(db)
        eventRepo = GatedEventRepository(db)
    }

    @After
    fun tearDown() {
        CreateFlowTestActivity.content = null
        db.close()
    }

    // — gates (suspending: the flow's coroutines run on the main dispatcher) —

    private class SuspensionGate {
        private val latch = CountDownLatch(1)
        private val entered = CountDownLatch(1)

        suspend fun await() {
            entered.countDown()
            withContext(Dispatchers.IO) { latch.await(10, TimeUnit.SECONDS) }
        }

        fun awaitEntered() {
            assertTrue("suspending operation did not reach its gate", entered.await(5, TimeUnit.SECONDS))
        }

        fun release() {
            latch.countDown()
        }
    }

    private class GatedEventRepository(db: TimeShrineDatabase) : EventRepository(db) {
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

    private class GatedBlockRepository(db: TimeShrineDatabase) : BlockRepository(db) {
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
        pauseChars = 120,
        turnoffChars = 200,
        countdownSeconds = 90,
        turnoffSeconds = 120,
    )

    private fun showFlow(
        editBlockId: Long?,
        repo: BlockRepository = blockRepo,
        apps: InstalledAppsRepository = stubApps(),
        initialStep: Int = 1,
        closeCount: AtomicInteger = AtomicInteger(0),
    ): AtomicInteger {
        CreateFlowTestActivity.content = {
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
        // The rule launches before @Before; recreate the initially blank host
        // so the configured root is installed from onCreate and participates
        // in the same saved-state lifecycle used by subsequent recreations.
        compose.activityRule.scenario.recreate()
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

    // Step 3 (09 v3): a new block starts with no card selected and Next
    // dimmed, so a create walk picks a method before continuing. An edit opens
    // with its stored method chosen and needs no pick.
    private fun pickMethod(title: String = "Type a passage") {
        compose.onNodeWithText(title).performClick()
    }

    // System-back route through the activity's dispatcher — the same path a
    // hardware/gesture Back takes, including any callbacks registered by
    // composed surfaces such as the modal sheet.
    private fun pressSystemBack() {
        compose.activityRule.scenario.onActivity { activity ->
            activity.onBackPressedDispatcher.onBackPressed()
        }
        compose.waitForIdle()
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
        compose.onNodeWithContentDescription(BACK).performClick()
        compose.onNodeWithContentDescription(BACK).performClick()

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
    // The five-step wizard logs steps 1..5; the created payload carries the
    // fresh-draft defaults (every build: 40 chars / 30 s wait; release middle
    // ladder rungs 200 chars and 120 s, debug owner testing the 20 first
    // rungs; 23 October config values). Since the unlock-minutes addendum (UM-6) it carries no pause
    // length: the exact key set is pinned.
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
        pickMethod()
        next()
        next()
        awaitText("5 / 5")
        // Invoke the semantics action twice on the UI thread without an
        // intervening test-idleness wait; the first callback launches the
        // gated transaction and the second must observe the retained guard.
        val saveClick = compose.onNodeWithText("Save block").fetchSemanticsNode()
            .config[SemanticsActions.OnClick].action
        compose.activity.runOnUiThread {
            saveClick?.invoke()
            saveClick?.invoke()
        }
        eventRepo.gate.awaitEntered()
        assertEquals(0, closeCount.get())

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
                EventRepository.EVENT_BLOCK_CREATE_STEP_COMPLETED,
                EventRepository.EVENT_BLOCK_CREATED,
            ),
            recorded.map { it.name },
        )
        assertEquals(listOf(1, 2, 3, 4, 5), recorded.drop(1).take(5).map { JSONObject(it.paramsJson!!).getInt("step") })
        val created = recorded.last()
        val params = JSONObject(created.paramsJson!!)
        assertEquals(1, params.getInt("app_count"))
        assertEquals(0, params.getInt("site_count"))
        assertEquals("typing", params.getString("friction_type"))
        assertEquals(
            setOf(
                "app_count", "site_count", "friction_type", "pause_chars",
                "turnoff_chars", "countdown_seconds", "turnoff_seconds",
            ),
            params.keys().asSequence().toSet(),
        )
        assertEquals(PAUSE_CHARS_DEFAULT, params.getInt("pause_chars"))
        assertEquals(DISABLE_CHARS_DEFAULT, params.getInt("turnoff_chars"))
        assertEquals(PAUSE_WAIT_SECONDS_DEFAULT, params.getInt("countdown_seconds"))
        assertEquals(DISABLE_WAIT_SECONDS_DEFAULT, params.getInt("turnoff_seconds"))

        // The seeded holder plus exactly one new block.
        val blocks = runBlocking { blockRepo.getBlocksWithContents() }
        assertEquals(2, blocks.size)
        val saved = blocks.first { it.block.name == "Evening lockout" }
        assertEquals(listOf("com.sleeper.app"), saved.apps.map { it.packageName })
        assertEquals(false, saved.block.enabled)
        assertNoRetiredConflictEvents()
    }

    // WZ-F01: block_created is suspended after the block and step 5 commit.
    // Recreation must retain the terminal owner, reject a second Save, then
    // deliver the completed close to the new composition exactly once.
    @Test
    fun recreationDuringTerminalCreateDoesNotStrandOrDuplicateTheBlock() {
        eventRepo.gatedNames.add(EventRepository.EVENT_BLOCK_CREATED)
        val freeApp = appEntry("com.sleeper.app", "Sleeper")
        val closeCount = AtomicInteger(0)
        val apps = stubApps(appEntry("com.apple.news", "Apple News"), freeApp)
        showFlow(editBlockId = null, apps = apps, closeCount = closeCount)

        awaitText("What should this cover?")
        seedHolderAndPickFreeApp(freeApp)
        next()
        typeBlockName("Evening lockout")
        next()
        pickMethod()
        next()
        next()
        awaitText("5 / 5")
        compose.onNodeWithText("Save block").performClick()
        eventRepo.gate.awaitEntered()

        compose.activityRule.scenario.recreate()
        awaitText("5 / 5")
        compose.onNodeWithText("Save block").performClick() // retained guard: no-op

        eventRepo.gate.release()
        compose.waitUntil(timeoutMillis = 5_000) { closeCount.get() == 1 }
        assertEquals(1, closeCount.get())
        assertEquals(2, runBlocking { blockRepo.getBlocksWithContents().size })
        assertEquals(1, runBlocking { eventDao.countByName(EventRepository.EVENT_BLOCK_CREATED) })
        assertEquals(
            listOf(1, 2, 3, 4, 5),
            events().filter { it.name == EventRepository.EVENT_BLOCK_CREATE_STEP_COMPLETED }
                .map { JSONObject(it.paramsJson!!).getInt("step") },
        )
    }

    // WZ-F01: the start write itself may be suspended during recreation. Its
    // retained worker completes once; the recreated screen does not enqueue a
    // second start and does not suppress the pending first one.
    @Test
    fun recreationDuringCreateStartLogsExactlyOnce() {
        eventRepo.gatedNames.add(EventRepository.EVENT_BLOCK_CREATE_STARTED)
        val closeCount = AtomicInteger(0)
        showFlow(editBlockId = null, closeCount = closeCount)
        awaitText("What should this cover?")
        eventRepo.gate.awaitEntered()

        compose.activityRule.scenario.recreate()
        awaitText("What should this cover?")
        eventRepo.gate.release()

        compose.waitUntil(timeoutMillis = 5_000) {
            runBlocking { eventDao.countByName(EventRepository.EVENT_BLOCK_CREATE_STARTED) == 1 }
        }
        assertEquals(1, runBlocking { eventDao.countByName(EventRepository.EVENT_BLOCK_CREATE_STARTED) })
        assertEquals(0, closeCount.get())
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
        compose.onNodeWithText("Add").assertIsEnabled()
        compose.onNodeWithText("Add").performClick()
        awaitText("1 selected")
        compose.onNodeWithText("free.example.com").assertExists()
        assertNoRetiredConflictEvents()
    }

    // A stale draft (the drafted app was claimed by another block while the
    // editor was open) is rejected at the repository: the flow stays open,
    // the draft survives, the inline label shows without naming the owner,
    // and nothing is written. Removing the held app then saves normally,
    // proving the terminal guard was released for retries.
    @Test
    fun rejectedSaveRetainsDraftAndShowsInlineMessage() {
        val socialId = runBlocking {
            blockRepo.createBlock(
                draft("Social", apps = listOf("com.instagram.android"), sites = listOf("reddit.com")),
            )
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
        next()
        awaitText("5 / 5")
        compose.onNodeWithText("Sleeper").assertExists() // review chip: draft intact
        compose.onNodeWithText("Save block").performClick()

        awaitText("Already added to a block") // inline explanation on the review step
        assertEquals(0, closeCount.get())
        compose.onNodeWithText("5 / 5").assertExists()
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
        repeat(4) { compose.onNodeWithContentDescription(BACK).performClick() }
        awaitText("What should this cover?")
        // Selected rows: apps [Instagram, Sleeper] then sites [reddit.com].
        compose.onAllNodesWithText(REMOVE_GLYPH)[1].performClick()
        awaitText("2 selected")
        next()
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

    // Owner addendum + clarification, 13 September: the website field is
    // single-line, dot-less words are rejected, and pasted line breaks are
    // NOT stripped — a multiline paste stays in the field and keeps Add
    // disabled, so "reddit.com\nabdes" can never merge into an addable
    // domain. Only removing the break re-enables Add.
    @Test
    fun siteAddRequiresACompleteSingleLineDomain() {
        showFlow(editBlockId = null)

        compose.onNodeWithText("Websites").performClick()
        val field = compose.onNode(hasSetTextAction())

        // A dot-less word is rejected with an explanation.
        field.performTextReplacement("reddit")
        compose.onNodeWithText("Add").assert(isNotEnabled())
        awaitText("Enter a complete domain like reddit.com")

        // Newlines are preserved, not stripped: the raw value stays and
        // remains invalid.
        field.performTextReplacement("abc\ndef")
        compose.onNodeWithText("abc\ndef").assertExists()
        compose.onNodeWithText("Add").assert(isNotEnabled())

        // The owner's exact case: a domain plus a second line is never
        // merged into a valid one.
        field.performTextReplacement("reddit.com\nabdes")
        compose.onNodeWithText("Add").assert(isNotEnabled())
        awaitText("Enter a complete domain like reddit.com")

        // A single complete domain — even copied with a trailing newline —
        // unlocks Add and adds its canonical form.
        field.performTextReplacement("free.example.com\n")
        compose.waitUntil(timeoutMillis = 5_000) {
            try {
                compose.onNodeWithText("Add").assertIsEnabled()
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
    // event. Five steps: Back from the disable step re-logs step 3, and the
    // save logs step 5 before block_created.
    @Test
    fun searchBackAndStepBackLogExactSequence() {
        val freeApp = appEntry("com.sleeper.app", "Sleeper")
        val closeCount = showFlow(
            editBlockId = null,
            apps = stubApps(appEntry("com.apple.news", "Apple News"), freeApp),
        )

        compose.onNodeWithText("Search your installed apps…").performClick()
        awaitText("Done")
        compose.onNodeWithContentDescription(BACK).performClick() // search exits, nothing logged
        awaitText("Search your installed apps…")

        seedHolderAndPickFreeApp(freeApp)
        next() // step_completed(1)
        typeBlockName("Evening lockout")
        next() // step_completed(2)
        pickMethod() // 09 v3: Next stays dimmed until a card is picked
        next() // step_completed(3)
        awaitText("Type a bit more") // disable step 4/5 with the typing ladder
        compose.onNodeWithContentDescription(BACK).performClick() // 4 → 3, nothing logged
        awaitText("Type a passage")
        next() // step_completed(3) again: the pick was kept
        awaitText("Type a bit more")
        next() // step_completed(4)
        awaitText("5 / 5")
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
                EventRepository.EVENT_BLOCK_CREATE_STEP_COMPLETED,
                EventRepository.EVENT_BLOCK_CREATED,
            ),
            events().map { it.name },
        )
        assertEquals(
            listOf(1, 2, 3, 3, 4, 5),
            events().drop(1).take(6).map { JSONObject(it.paramsJson!!).getInt("step") },
        )
        assertNoRetiredConflictEvents()
    }

    // WZ-F02: every step event uses one retained FIFO. Even when the first
    // step write is suspended, later navigation and Save cannot overtake it or
    // persist a block before the ordered history reaches the terminal save.
    @Test
    fun suspendedStepEventsRemainOrderedAheadOfSave() {
        eventRepo.gatedNames.add(EventRepository.EVENT_BLOCK_CREATE_STEP_COMPLETED)
        val freeApp = appEntry("com.sleeper.app", "Sleeper")
        val closeCount = showFlow(
            editBlockId = null,
            apps = stubApps(appEntry("com.apple.news", "Apple News"), freeApp),
        )

        awaitText("What should this cover?")
        seedHolderAndPickFreeApp(freeApp)
        next()
        eventRepo.gate.awaitEntered()
        typeBlockName("Evening lockout")
        next()
        pickMethod()
        next()
        next()
        awaitText("5 / 5")
        compose.onNodeWithText("Save block").performClick()

        // Holder only: terminal creation is queued behind the suspended step.
        assertEquals(1, runBlocking { blockRepo.getBlocksWithContents().size })
        assertEquals(0, closeCount.get())

        eventRepo.gate.release()
        compose.waitUntil(timeoutMillis = 5_000) { closeCount.get() == 1 }
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
            listOf(1, 2, 3, 4, 5),
            events().drop(1).take(5).map { JSONObject(it.paramsJson!!).getInt("step") },
        )
        assertEquals(2, runBlocking { blockRepo.getBlocksWithContents().size })
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
        compose.onNodeWithContentDescription(BACK).performClick()
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

    // Wizard redesign (19 September): detail's THE FRICTION → Edit opens the
    // editor directly at 3/5 with the stored values loaded into the method
    // cards; Back at that entry step closes the flow (returning to detail in
    // real navigation) without any event.
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

        awaitText("3 / 5")
        awaitText("Type a passage")
        // Stored pauseChars (120, not the 40 default) proves the prefill
        // landed in the open card's stepper (09 v3: the stored method opens
        // chosen, with no pick needed); the nonempty draft may proceed from
        // the method step. The card names no pause length (UM-A).
        awaitText("120 characters")
        awaitText("Random words.")
        compose.onNodeWithText("Wait before you're in").assertDoesNotExist() // other card folded
        compose.onNodeWithText("Next").assertIsEnabled()

        compose.onNodeWithContentDescription(BACK).performClick() // entry step: exits, not step 2
        compose.waitUntil(timeoutMillis = 5_000) { closeCount.get() == 1 }
        assertEquals(1, closeCount.get())
        assertTrue(events().isEmpty()) // edit cancellation stays silent
    }

    // R8: a legacy empty block (seeded directly — the repository now rejects
    // empty drafts) opened at the method step can neither advance nor save;
    // Back still exits.
    @Test
    fun frictionEditOnAnEmptyBlockCannotAdvance() {
        val emptyId = runBlocking {
            dao.insertBlock(
                Block(
                    name = "Legacy empty",
                    frictionType = FrictionType.TYPING,
                    pauseChars = 120,
                    turnoffChars = 200,
                    countdownSeconds = 90,
                    turnoffSeconds = 120,
                    enabled = false,
                ),
            )
        }
        val closeCount = showFlow(editBlockId = emptyId, initialStep = 3)

        awaitText("3 / 5")
        compose.onNodeWithText("Next").assert(isNotEnabled())
        compose.onNodeWithText("Next").performTouchInput { click() }
        compose.onNodeWithText("4 / 5").assertDoesNotExist()

        compose.onNodeWithContentDescription(BACK).performClick()
        compose.waitUntil(timeoutMillis = 5_000) { closeCount.get() == 1 }
        assertEquals(1, closeCount.get())
        assertTrue(events().isEmpty())
    }

    // — wizard redesign: inline method cards, disable step, per-method drafts —

    // 09 v3 (11 October unlock-minutes addendum): a new block's step 3 shows
    // the new title and the pause-length hint, no subtitle, no "Adjust the
    // details" button and no unfolded stepper; Next is dimmed and does nothing
    // until a card is picked.
    @Test
    fun stepThreeStartsWithFoldedCardsAndNextWaitsForAPick() {
        val freeApp = appEntry("com.sleeper.app", "Sleeper")
        showFlow(
            editBlockId = null,
            apps = stubApps(appEntry("com.apple.news", "Apple News"), freeApp),
        )

        awaitText("What should this cover?")
        seedHolderAndPickFreeApp(freeApp)
        next()
        typeBlockName("Evening lockout")
        next()
        awaitText("3 / 5")

        awaitText("How do you want to pause the block?")
        awaitText("Type a passage")
        awaitText("Wait it out")
        awaitText("You'll pick how long each pause lasts when you unlock.")
        compose.onNodeWithText("What should getting in cost?").assertDoesNotExist()
        compose.onNodeWithText("Pick whichever you'll actually respect.").assertDoesNotExist()
        compose.onNodeWithText("Adjust the details").assertDoesNotExist()
        compose.onNodeWithText("Passage length").assertDoesNotExist()
        compose.onNodeWithText("Wait before you're in").assertDoesNotExist()

        compose.onNodeWithText("Next").assert(isNotEnabled())
        compose.onNodeWithText("Next").performTouchInput { click() }
        compose.onNodeWithText("4 / 5").assertDoesNotExist()

        pickMethod("Type a passage")
        awaitText("Passage length")
        compose.onNodeWithText("Next").assertIsEnabled()
    }

    // Tapping a card unfolds its one stepper at the draft value; tapping the
    // other folds it and opens that one. Each method keeps its own value.
    @Test
    fun onlyOneMethodCardIsOpenAndEachKeepsItsOwnValue() {
        val freeApp = appEntry("com.sleeper.app", "Sleeper")
        showFlow(
            editBlockId = null,
            apps = stubApps(appEntry("com.apple.news", "Apple News"), freeApp),
        )

        awaitText("What should this cover?")
        seedHolderAndPickFreeApp(freeApp)
        next()
        typeBlockName("Evening lockout")
        next()
        awaitText("3 / 5")

        pickMethod("Type a passage")
        awaitText("Passage length")
        awaitText("$PAUSE_CHARS_DEFAULT characters")
        awaitText("Random words.")
        compose.onNodeWithText("Wait before you're in").assertDoesNotExist()
        // Nothing about a time estimate on the card (UM-8, UM-9).
        compose.onAllNodesWithText("about", substring = true).assertCountEquals(0)

        // One step up on the typing stepper: 20 + 10 characters.
        compose.onAllNodesWithText(PLUS_GLYPH)[0].performClick()
        awaitText("${PAUSE_CHARS_DEFAULT + 10} characters")

        pickMethod("Wait it out")
        awaitText("Wait before you're in")
        awaitText(formatCountdown(PAUSE_WAIT_SECONDS_DEFAULT))
        awaitText("A countdown you can't skip.")
        compose.onNodeWithText("Passage length").assertDoesNotExist()
        compose.onAllNodesWithText(PLUS_GLYPH)[0].performClick()
        awaitText(formatCountdown(PAUSE_WAIT_SECONDS_DEFAULT + 10))

        pickMethod("Type a passage")
        awaitText("${PAUSE_CHARS_DEFAULT + 10} characters") // typing kept its own value
        compose.onNodeWithText("Wait before you're in").assertDoesNotExist()
        pickMethod("Wait it out")
        awaitText(formatCountdown(PAUSE_WAIT_SECONDS_DEFAULT + 10)) // and so did waiting
    }

    // The waiting path end to end: picking the waiting card, its stepper being
    // the only field (no passage length and no pause length), the inherited
    // disable ladder with the middle rung preselected (Next alone is valid),
    // and a round-trip of every per-method column through the save.
    @Test
    fun waitingMethodRoundTripsThroughCardDisableAndSave() {
        val freeApp = appEntry("com.sleeper.app", "Sleeper")
        val closeCount = showFlow(
            editBlockId = null,
            apps = stubApps(appEntry("com.apple.news", "Apple News"), freeApp),
        )

        awaitText("What should this cover?")
        seedHolderAndPickFreeApp(freeApp)
        next()
        typeBlockName("Evening wind-down")
        next()
        awaitText("3 / 5")

        // Pick the waiting card: its stepper unfolds at the default.
        pickMethod("Wait it out")
        awaitText("Wait before you're in")
        awaitText(formatCountdown(PAUSE_WAIT_SECONDS_DEFAULT))
        compose.onNodeWithText("Passage length").assertDoesNotExist() // typing field folded
        // Supersedes the pause-length stepper checks (UM-A): absence check only.
        compose.onNodeWithText("Block stays off for").assertDoesNotExist()
        compose.onNodeWithText("Next").assertIsEnabled()

        next() // step_completed(3)
        awaitText("4 / 5")
        awaitText("Disabling the block")
        // Waiting ladder inherited, default rung preselected: Next alone is valid.
        awaitText("Wait a bit")
        awaitText("Wait a bit more")
        awaitText("Make it hurt")
        awaitText("Recommended")
        compose.onNodeWithText("Next").assertIsEnabled()
        compose.onNodeWithText("Wait a bit more").performClick() // 2 min rung, both variants
        next() // step_completed(4)

        awaitText("5 / 5")
        awaitText("Wait ${formatCountdown(PAUSE_WAIT_SECONDS_DEFAULT)}") // pause cost row: no phone-in-hand copy
        awaitText("Wait 2 min") // disable cost row follows the chosen rung
        compose.onNodeWithText("Save block").performClick()
        compose.waitUntil(timeoutMillis = 5_000) { closeCount.get() == 1 }

        val saved = runBlocking { blockRepo.getBlocksWithContents() }
            .first { it.block.name == "Evening wind-down" }
        assertEquals(FrictionType.DELAY, saved.block.frictionType)
        assertEquals(PAUSE_WAIT_SECONDS_DEFAULT, saved.block.countdownSeconds)
        assertEquals(120, saved.block.turnoffSeconds)
        // The typing method's columns keep their untouched drafts.
        assertEquals(PAUSE_CHARS_DEFAULT, saved.block.pauseChars)
        assertEquals(DISABLE_CHARS_DEFAULT, saved.block.turnoffChars)
        assertEquals(false, saved.block.enabled)

        val created = events().last()
        assertEquals(EventRepository.EVENT_BLOCK_CREATED, created.name)
        val params = JSONObject(created.paramsJson!!)
        assertEquals("delay", params.getString("friction_type"))
        assertEquals(PAUSE_WAIT_SECONDS_DEFAULT, params.getInt("countdown_seconds"))
        assertEquals(120, params.getInt("turnoff_seconds"))
    }

    // Supersedes sheetBackDismissesTheSheetBeforeWizardBack (UM-B): the
    // details sheet is gone, so system Back on step 3 goes straight to step 2
    // whether or not a card is open, and logs nothing.
    @Test
    fun systemBackOnStepThreeGoesStraightToStepTwo() {
        val freeApp = appEntry("com.sleeper.app", "Sleeper")
        showFlow(
            editBlockId = null,
            apps = stubApps(appEntry("com.apple.news", "Apple News"), freeApp),
        )

        awaitText("What should this cover?")
        seedHolderAndPickFreeApp(freeApp)
        next()
        typeBlockName("Evening lockout")
        next()
        awaitText("3 / 5")

        pickMethod("Type a passage")
        awaitText("Passage length")
        pressSystemBack() // wizard Back: 3 → 2, no sheet to close first
        awaitText("Give it a name")
        awaitText("2 / 5")
    }

    // BW-06: an in-progress create survives activity recreation — the
    // saveable draft restores the step, method switch and name.
    @Test
    fun createDraftSurvivesActivityRecreation() {
        val freeApp = appEntry("com.sleeper.app", "Sleeper")
        val closeCount = AtomicInteger(0)
        val apps = stubApps(appEntry("com.apple.news", "Apple News"), freeApp)
        showFlow(editBlockId = null, apps = apps, closeCount = closeCount)

        awaitText("What should this cover?")
        seedHolderAndPickFreeApp(freeApp)
        next()
        typeBlockName("Evening lockout")
        next()
        awaitText("3 / 5")
        compose.onNodeWithText("Next").assert(isNotEnabled()) // nothing picked yet
        pickMethod("Wait it out")

        compose.activityRule.scenario.recreate()

        awaitText("3 / 5")
        // The pick survived: the waiting card is still open, the typing card
        // folded, and Next is enabled without picking again.
        awaitText("Wait before you're in")
        compose.onNodeWithText("Passage length").assertDoesNotExist()
        compose.onNodeWithText("Next").assertIsEnabled()

        // Backtracking keeps the unwound draft: the typed name is intact.
        compose.onNodeWithContentDescription(BACK).performClick()
        awaitText("2 / 5")
        awaitText("Evening lockout")
        assertEquals(0, closeCount.get())
    }

    // BW-06, edit side: a restored edit draft is not clobbered by the
    // stored block on recreation — the in-session method switch survives.
    @Test
    fun editDraftSurvivesRecreationWithoutPrefillOverwrite() {
        val socialId = runBlocking {
            blockRepo.createBlock(draft("Social", apps = listOf("com.instagram.android")))
        }
        val closeCount = AtomicInteger(0)
        val apps = stubApps(appEntry("com.instagram.android", "Instagram"))
        showFlow(editBlockId = socialId, apps = apps, closeCount = closeCount)

        awaitText("Instagram")
        next()
        next()
        awaitText("3 / 5")
        awaitText("Passage length") // the stored typing method opened chosen
        pickMethod("Wait it out") // diverges from stored TYPING

        compose.activityRule.scenario.recreate()

        awaitText("3 / 5")
        // The restored draft still says waiting — prefill did not overwrite it.
        awaitText("Wait before you're in")
        compose.onNodeWithText("Passage length").assertDoesNotExist()

        compose.onNodeWithContentDescription(BACK).performClick() // 3 → 2, then exit
        awaitText("2 / 5")
        compose.onNodeWithContentDescription(BACK).performClick()
        compose.onNodeWithContentDescription(BACK).performClick() // entry step: exits
        compose.waitUntil(timeoutMillis = 5_000) { closeCount.get() == 1 }
        assertEquals(1, closeCount.get())
        assertTrue(events().isEmpty()) // canceled edit writes nothing
        assertEquals(FrictionType.TYPING, runBlocking { blockRepo.getBlockWithContents(socialId)!!.block.frictionType })
    }
}

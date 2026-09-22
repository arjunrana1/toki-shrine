package com.arjunrana.tokishrine.detection

import com.arjunrana.tokishrine.data.repo.EventRepository
import org.junit.Assert.assertEquals
import org.junit.Test

class DetectionCoordinatorTest {

    @Test
    fun attachedEngineReceivesSuppressionRelease() {
        var now = 0L
        val engine = DetectionEngine(clock = { now }, debounceMs = 10_000)
        val ref = BlockRef(blockId = 1, blockName = "Dooms")
        engine.onBlocksChanged(mapOf("com.example.a" to ref), emptyMap())
        val coordinator = DetectionCoordinator()
        coordinator.attach(engine)

        assertEquals(1, engine.onWindowStateChanged(10, "com.example.a").size)
        coordinator.releaseRepeatSuppression(EventRepository.TARGET_TYPE_APP, "com.example.a", 1)
        now = 1

        assertEquals(1, engine.onWindowStateChanged(10, "com.example.a").size)
    }

    @Test
    fun detachedOrReplacedEngineCannotBeMutatedThroughCoordinator() {
        var now = 0L
        val first = configuredEngine { now }
        val second = configuredEngine { now }
        val coordinator = DetectionCoordinator()
        coordinator.attach(first)
        coordinator.attach(second)
        coordinator.detach(first)

        assertEquals(1, second.onWindowStateChanged(10, "com.example.a").size)
        coordinator.releaseRepeatSuppression(EventRepository.TARGET_TYPE_APP, "com.example.a", 1)
        now = 1

        assertEquals(1, second.onWindowStateChanged(10, "com.example.a").size)
    }

    private fun configuredEngine(clock: () -> Long) = DetectionEngine(clock = clock, debounceMs = 10_000).also {
        it.onBlocksChanged(
            apps = mapOf("com.example.a" to BlockRef(blockId = 1, blockName = "Dooms")),
            sites = emptyMap(),
        )
    }
}

package com.arjunrana.tokishrine.detection

import com.arjunrana.tokishrine.data.repo.EventRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BlockShownEventTest {

    @Test
    fun shownEventMeasuresFromOriginalDetectionTime() {
        assertEquals(
            BlockShownEvent(
                blockId = 7,
                triggerType = EventRepository.TARGET_TYPE_SITE,
                target = "reddit.com",
                latencyMs = 2_125,
            ),
            BlockShownEvent.create(
                blockId = 7,
                triggerType = EventRepository.TARGET_TYPE_SITE,
                target = "reddit.com",
                startedAtElapsedMs = 10_000,
                shownAtElapsedMs = 12_125,
            ),
        )
    }

    @Test
    fun negativeClockDriftCannotProduceNegativeLatency() {
        assertEquals(
            0L,
            BlockShownEvent.create(
                blockId = 7,
                triggerType = EventRepository.TARGET_TYPE_APP,
                target = "com.instagram.android",
                startedAtElapsedMs = 10_000,
                shownAtElapsedMs = 9_999,
            )?.latencyMs,
        )
    }

    @Test
    fun invalidLaunchDataCannotBecomeShownTelemetry() {
        assertNull(BlockShownEvent.create(0, "app", "target", 1, 2))
        assertNull(BlockShownEvent.create(1, "unknown", "target", 1, 2))
        assertNull(BlockShownEvent.create(1, "app", " ", 1, 2))
        assertNull(BlockShownEvent.create(1, "app", "target", -1, 2))
    }
}

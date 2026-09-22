package com.arjunrana.tokishrine.detection

/**
 * Process-local bridge between the interruption Activity and the bound
 * accessibility service's detection engine. Both components run in the app
 * process. If the service is absent, its next engine starts with no debounce
 * memory, so releasing suppression is already satisfied.
 */
class DetectionCoordinator {
    private var engine: DetectionEngine? = null

    @Synchronized
    fun attach(engine: DetectionEngine) {
        this.engine = engine
    }

    @Synchronized
    fun detach(engine: DetectionEngine) {
        if (this.engine === engine) this.engine = null
    }

    @Synchronized
    fun releaseRepeatSuppression(triggerType: String, target: String, blockId: Long) {
        engine?.releaseRepeatSuppression(triggerType, target, blockId)
    }
}

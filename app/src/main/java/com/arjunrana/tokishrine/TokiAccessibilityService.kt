package com.arjunrana.tokishrine

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

/*
 * Permission prerequisite for Phase 3 only: the service must exist and be
 * declared for Android to list Toki Shrine in the system accessibility
 * settings, so the onboarding row can be granted at all. It deliberately
 * observes nothing yet — the detection engine (Phase 4) implements event
 * handling, URL reading and the service-health events.
 */
class TokiAccessibilityService : AccessibilityService() {
    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit
}

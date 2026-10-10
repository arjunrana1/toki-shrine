package com.arjunrana.tokishrine.ui.interruption

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import kotlin.math.PI
import kotlin.math.sin

/*
 * P7-F2: BlockActivity bumps an integer each time Back is pressed during an
 * active challenge; the screen's Never mind control reacts to each new
 * value. The wiggle is a damped sine — two and a half horizontal swings
 * whose amplitude melts to zero, so the control glides out, eases back and
 * settles exactly where it started (the F-A placeholder was a stepped
 * shake). Purely visual: the counter, haptic and Back wiring stay in
 * BlockActivity. The offset state is read inside the graphicsLayer block,
 * so each frame re-renders the layer without recomposing the screen.
 */
private const val NUDGE_DURATION_MS = 550
private const val NUDGE_AMPLITUDE_DP = 8f
private const val NUDGE_SWINGS = 2.5f

/** Horizontal offset in dp at normalized time [progress] ∈ [0, 1]. */
private fun nudgeOffset(progress: Float): Float {
    val decay = (1f - progress) * (1f - progress)
    return (NUDGE_AMPLITUDE_DP * sin(2.0 * PI * NUDGE_SWINGS * progress) * decay).toFloat()
}

@Composable
internal fun Modifier.escapeNudge(nudge: Int): Modifier {
    val offset = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(nudge) {
        if (nudge == 0) return@LaunchedEffect
        animate(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = tween(NUDGE_DURATION_MS),
        ) { progress, _ ->
            offset.floatValue = nudgeOffset(progress)
        }
    }
    return graphicsLayer { translationX = offset.floatValue * density }
}

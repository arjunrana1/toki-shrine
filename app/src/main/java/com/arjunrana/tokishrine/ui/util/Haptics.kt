package com.arjunrana.tokishrine.ui.util

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.os.postDelayed

/*
 * Haptic confirmation for block activation changes (owner addendum,
 * 13 September): a stronger pulse whenever a block turns on, a lighter
 * one when it turns off. Predefined effects keep each device's own
 * haptic tuning; a missing vibrator or failed effect is a silent no-op.
 * Requires the (install-time, normal) VIBRATE permission.
 *
 * Owner feel feedback (P3-F04, 19 September): the single heavy click was
 * too brief. ON is now a heavy click followed by a settling click 30 ms
 * later — one fused pulse noticeably longer, with every segment still
 * device-tuned. Predefined effects expose no duration, so acceptance is
 * owner feel-testing on the target device, not a measured 30%.
 */
object BlockHaptics {

    private const val ON_SETTLE_DELAY_MILLIS = 30L

    // Main-looper delay keeps the follow-up segment fire-and-forget from
    // any calling thread without owning a thread or lifecycle.
    private val settle = Handler(Looper.getMainLooper())

    fun turnedOn(context: Context) {
        val vibrator = vibrator(context) ?: return
        vibrate(vibrator, VibrationEffect.EFFECT_HEAVY_CLICK)
        settle.postDelayed(ON_SETTLE_DELAY_MILLIS) {
            vibrate(vibrator, VibrationEffect.EFFECT_CLICK)
        }
    }

    fun turnedOff(context: Context) {
        val vibrator = vibrator(context) ?: return
        vibrate(vibrator, VibrationEffect.EFFECT_CLICK)
    }

    private fun vibrator(context: Context): Vibrator? = runCatching {
        context.getSystemService(VibratorManager::class.java)?.defaultVibrator
    }.getOrNull()

    private fun vibrate(vibrator: Vibrator, effectId: Int) {
        runCatching {
            vibrator.vibrate(VibrationEffect.createPredefined(effectId))
        }
    }
}

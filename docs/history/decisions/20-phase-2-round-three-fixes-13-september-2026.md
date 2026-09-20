# Historical decision record

Historical evidence, not current workflow instructions. Later records may supersede this entry. Use current component contracts and PRD requirements for implementation.

Source: pre-migration DECISIONS.md, section 20.

## Phase 2 round-three fixes — 13 September 2026

- **Estimate-box sentence + turn-on/off haptics (owner request on 4eea876).** The friction screen's typing estimate now reads exactly "You'll take X seconds to type random words each time you want to pause this block." with X = typingEstimateSeconds in plain seconds (the 50–200 char passage range keeps it 20–80 s, so no minute formatting). The working tree already carried this exact edit from a parallel session when this pass started; it was adopted as-is rather than redone (unused formatEstimate import removed from CreateFlowScreen; TurnOnScreen and other surfaces keep formatEstimate).
- **Haptics:** new `ui/util/Haptics.kt` (`BlockHaptics.turnedOn/turnedOff`) using predefined `VibrationEffect`s — EFFECT_HEAVY_CLICK for on, EFFECT_CLICK for off — keeping each device's own haptic tuning; missing vibrator/failed effect is a silent no-op. Calls: TurnOnScreen's Turn it on (the single enabling path — both toggle entry points route there), and both OFF sites (detail switch, home row switch). Manifest gained the normal install-time VIBRATE permission — the only permission in the app so far besides the picker's visibility query. Haptic strength pairing (heavy/click) is easily retunable; no automated coverage (device-feel check belongs to the owner).
- **Checks:** assembleDebug, testDebugUnitTest (12 JVM green), assembleDebugAndroidTest compile, hex grep clean. Reinstall follows on the connected, authorized phone.

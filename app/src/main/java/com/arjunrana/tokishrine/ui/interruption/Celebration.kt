package com.arjunrana.tokishrine.ui.interruption

import com.arjunrana.tokishrine.config.AppConfig
import com.arjunrana.tokishrine.config.AppConfig.CELEBRATION_MINUTES_TOKEN
import kotlin.random.Random

/** What the Walk-Away moment shows (9 October §17 addendum / P7-F17). */
data class Celebration(val title: String, val image: Int, val subtitle: String, val winsLine: String)

/*
 * Pure selection for the celebration screen. BlockActivity keeps the seed,
 * the day's count and the visit minutes in saved state, so a rotation or
 * recreation shows the same pick instead of reshuffling.
 */
object CelebrationPicker {

    fun pick(
        dailyCount: Int,
        visitMinutes: Long?,
        random: Random,
        titles: List<AppConfig.CelebrationTitle> = AppConfig.CELEBRATION_TITLES,
        subtitles: List<String> = AppConfig.CELEBRATION_SUBTITLES,
    ): Celebration {
        val title = eligibleTitles(dailyCount, titles).random(random)
        val subtitle = eligibleSubtitles(visitMinutes, subtitles).random(random)
        return Celebration(title.text, title.image, subtitle, winsLine(dailyCount))
    }

    /** A count-limited title appears only on that nope of the day. */
    fun eligibleTitles(dailyCount: Int, titles: List<AppConfig.CelebrationTitle> = AppConfig.CELEBRATION_TITLES) =
        titles.filter { it.onlyOnDailyCount == null || it.onlyOnDailyCount == dailyCount }

    /** The minutes line is offered only when this nope saved time; its token becomes "N minutes". */
    fun eligibleSubtitles(visitMinutes: Long?, subtitles: List<String> = AppConfig.CELEBRATION_SUBTITLES) =
        subtitles.mapNotNull { line ->
            when {
                CELEBRATION_MINUTES_TOKEN !in line -> line
                visitMinutes == null || visitMinutes < 1 -> null
                else -> line.replace(CELEBRATION_MINUTES_TOKEN, minutesPhrase(visitMinutes))
            }
        }

    fun minutesPhrase(minutes: Long): String = if (minutes == 1L) "1 minute" else "$minutes minutes"

    fun winsLine(dailyCount: Int): String = if (dailyCount == 1) "1 win today" else "$dailyCount wins today"
}

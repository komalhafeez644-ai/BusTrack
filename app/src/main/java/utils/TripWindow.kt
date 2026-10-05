package utils

import java.util.Calendar

/** The single authoritative clock classification for trip and attendance periods. */
enum class TripPeriod { MORNING, EVENING, GAP }

object TripWindow {
    private const val MORNING_START_MINUTE = 12 * 60
    private const val MORNING_END_MINUTE = 2* 60
    private const val EVENING_START_MINUTE = 17 * 60
    private const val EVENING_END_MINUTE = 21 * 60

    fun currentPeriod(calendar: Calendar = Calendar.getInstance()): TripPeriod {
        val minuteOfDay = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
        return when
        {
            minuteOfDay in MORNING_START_MINUTE until MORNING_END_MINUTE -> TripPeriod.MORNING
            minuteOfDay in EVENING_START_MINUTE until EVENING_END_MINUTE -> TripPeriod.EVENING
            else -> TripPeriod.GAP
        }
    }
}

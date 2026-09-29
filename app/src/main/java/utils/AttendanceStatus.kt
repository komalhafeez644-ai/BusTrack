package utils

/** Canonical labels for attendance states shared by Driver and staff views. */
object AttendanceStatus {
    const val SHORT_LEAVE = "S Leave"

    /** Accept legacy records so previously saved Leave entries keep their meaning. */
    fun isShortLeave(value: String?): Boolean =
        value.equals("Leave", ignoreCase = true) ||
                value.equals(SHORT_LEAVE, ignoreCase = true) ||
                value.equals("S. Leave", ignoreCase = true)

    fun forStorage(value: String): String = if (isShortLeave(value)) SHORT_LEAVE else value

    fun forDisplay(value: String): String = if (isShortLeave(value)) SHORT_LEAVE else value
}

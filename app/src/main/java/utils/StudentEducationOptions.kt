package utils

/** Shared Grade and Semester choices used by both Add Student and Edit Student. */
object StudentEducationOptions {
    val grades = listOf(
        "11th",
        "12th",
        "BS English",
        "BS Urdu",
        "BS Islamic Studies",
        "BS Economics",
        "BS Mathematics",
        "BS Botany",
        "BS Information Technology (IT)",
        "BS Applied Psychology",
        "BS Political Science",
        "BBA (Bachelor of Business Administration)"
    )

    val semesters = listOf(
        "1st SEM", "2nd SEM", "3rd SEM", "4th SEM", "5th SEM", "6th SEM", "7th SEM", "8th SEM",
        "ICS", "Pre-ENG", "Pre-MED", "Stats", "Arts"
    )

    /** Splits the legacy single `grade` field, which Add Student stores as "grade semester". */
    fun split(value: String): Pair<String, String> {
        val saved = value.trim()
        if (saved.isBlank()) return "" to ""

        val exactSemester = semesters.firstOrNull { saved.endsWith(" $it", ignoreCase = true) }
        if (exactSemester != null) {
            val gradePart = saved.dropLast(exactSemester.length).trim()
            return canonicalGrade(gradePart) to exactSemester
        }

        // Older records used spellings such as "7th semester" and "BS IT".
        val legacySem = Regex("(?i)\\s+(1st|2nd|3rd|[4-8]th)\\s+semester$").find(saved)
        if (legacySem != null) {
            val semester = legacySem.groupValues[1].uppercase() + " SEM"
            val canonicalSemester = semesters.firstOrNull { it.equals(semester, true) } ?: semester
            return canonicalGrade(saved.substring(0, legacySem.range.first).trim()) to canonicalSemester
        }

        val legacyChoice = semesters.firstOrNull { saved.endsWith(" $it", true) }
        if (legacyChoice != null) {
            return canonicalGrade(saved.dropLast(legacyChoice.length).trim()) to legacyChoice
        }
        return canonicalGrade(saved) to ""
    }

    fun combine(original: String, selectedGrade: String, selectedSemester: String): String {
        val (originalGrade, originalSemester) = split(original)
        if (selectedGrade == originalGrade && selectedSemester == originalSemester) return original
        return listOf(selectedGrade.trim(), selectedSemester.trim())
            .filter(String::isNotBlank)
            .joinToString(" ")
    }

    private fun canonicalGrade(value: String): String {
        if (value.equals("BS IT", ignoreCase = true)) return "BS Information Technology (IT)"
        return grades.firstOrNull { it.equals(value, ignoreCase = true) } ?: value
    }
}

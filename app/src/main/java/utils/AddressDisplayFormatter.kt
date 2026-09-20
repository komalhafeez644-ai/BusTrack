package utils

/**
 * Produces one stable, human-readable address from Android's reverse-geocoder
 * response.  The same address is shown by both the driver and live-tracking
 * screens, so de-duplicate it before either screen renders it.
 */
object AddressDisplayFormatter {
    fun normalize(address: String): String = address
        .split(',')
        .map(String::trim)
        .filter(String::isNotEmpty)
        .fold(mutableListOf<String>()) { uniqueParts, part ->
            // Geocoders can repeat a component with harmless punctuation or
            // whitespace differences (for example, "Road" and "Road.").
            // Compare a display-safe key but retain the original first value.
            val key = part.lowercase()
                .replace(Regex("[\\p{Punct}\\s]+"), " ")
                .trim()
            if (key.isNotEmpty() && uniqueParts.none { existing ->
                    existing.lowercase()
                        .replace(Regex("[\\p{Punct}\\s]+"), " ")
                        .trim() == key
                }
            ) {
                uniqueParts += part
            }
            uniqueParts
        }
        .joinToString(", ")
}

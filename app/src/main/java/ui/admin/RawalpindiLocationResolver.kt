package ui.admin

import android.content.Context
import android.location.Geocoder
import android.util.Log
import com.example.bustrack_app.R
import com.mapbox.geojson.Point
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale

data class RawalpindiSearchResult(
    val name: String,
    val fullAddress: String,
    val point: Point
)

/**
 * Shared address lookup for admin route workflows. The bbox is deliberately a hard
 * Rawalpindi service-area boundary, rather than a proximity hint, so an ambiguous
 * name cannot silently select an overseas location.
 */
object RawalpindiLocationResolver {
    private const val BBOX = "72.70,33.35,73.35,33.90"
    private const val MIN_LNG = 72.70
    private const val MAX_LNG = 73.35
    private const val MIN_LAT = 33.35
    private const val MAX_LAT = 33.90

    suspend fun search(context: Context, rawQuery: String): List<RawalpindiSearchResult> =
        withContext(Dispatchers.IO) {
            val query = rawQuery.trim()
            if (query.isEmpty()) return@withContext emptyList()

            val variants = buildList {
                add(query)
                add(canonicalizeLocality(query))
            }.distinctBy { it.lowercase(Locale.ROOT) }

            variants.forEach { variant ->
                mapboxSearch(context, variant).takeIf { it.isNotEmpty() }?.let { return@withContext it }
            }
            variants.forEach { variant ->
                openStreetMapSearch(variant).takeIf { it.isNotEmpty() }?.let { return@withContext it }
            }
            androidGeocoderSearch(context, variants.first())
        }

    suspend fun resolve(context: Context, query: String): RawalpindiSearchResult? =
        search(context, query).firstOrNull()

    private fun mapboxSearch(context: Context, query: String): List<RawalpindiSearchResult> = try {
        val token = context.getString(R.string.mapbox_access_token)
        val url = URL(
            "https://api.mapbox.com/search/geocode/v6/forward" +
                "?q=${URLEncoder.encode(query, "UTF-8")}" +
                "&access_token=$token&bbox=$BBOX&proximity=73.0679,33.6007" +
                "&country=pk&types=address,street,place,locality,neighborhood,district" +
                "&autocomplete=true&language=en&limit=10"
        )
        readJson(url)?.let { json ->
            val features = JSONObject(json).optJSONArray("features") ?: return emptyList()
            buildList {
                for (index in 0 until features.length()) {
                    val feature = features.optJSONObject(index) ?: continue
                    val coordinates = feature.optJSONObject("geometry")?.optJSONArray("coordinates") ?: continue
                    val lng = coordinates.optDouble(0, Double.NaN)
                    val lat = coordinates.optDouble(1, Double.NaN)
                    if (!isInsideRawalpindi(lat, lng)) continue
                    val properties = feature.optJSONObject("properties")
                    val name = properties?.optString("name").orEmpty().ifBlank { feature.optString("name") }
                    val address = properties?.optString("full_address").orEmpty()
                        .ifBlank { properties?.optString("place_formatted").orEmpty() }
                        .ifBlank { name }
                    add(RawalpindiSearchResult(name.ifBlank { query }, address.ifBlank { query }, Point.fromLngLat(lng, lat)))
                }
            }.distinctBy { "${it.point.longitude()},${it.point.latitude()}" }
        } ?: emptyList()
    } catch (e: Exception) {
        Log.w("RawalpindiSearch", "Mapbox geocoding failed for '$query'", e)
        emptyList()
    }

    private fun openStreetMapSearch(query: String): List<RawalpindiSearchResult> = try {
        val localQuery = if (query.contains("rawalpindi", ignoreCase = true)) query else "$query, Rawalpindi, Pakistan"
        val url = URL(
            "https://nominatim.openstreetmap.org/search?q=${URLEncoder.encode(localQuery, "UTF-8")}" +
                "&format=jsonv2&limit=10&countrycodes=pk&accept-language=en"
        )
        val results = readJson(url, "BusTrackApp/1.0 (Android location search)") ?: return emptyList()
        val entries = JSONArray(results)
        buildList {
            for (index in 0 until entries.length()) {
                val item = entries.optJSONObject(index) ?: continue
                val lat = item.optString("lat").toDoubleOrNull() ?: continue
                val lng = item.optString("lon").toDoubleOrNull() ?: continue
                if (!isInsideRawalpindi(lat, lng)) continue
                val fullAddress = item.optString("display_name").ifBlank { query }
                add(RawalpindiSearchResult(fullAddress.substringBefore(",").ifBlank { query }, fullAddress, Point.fromLngLat(lng, lat)))
            }
        }.distinctBy { "${it.point.longitude()},${it.point.latitude()}" }
    } catch (e: Exception) {
        Log.w("RawalpindiSearch", "OpenStreetMap fallback failed for '$query'", e)
        emptyList()
    }

    private fun androidGeocoderSearch(context: Context, query: String): List<RawalpindiSearchResult> {
        if (!Geocoder.isPresent()) return emptyList()
        return try {
            @Suppress("DEPRECATION")
            val addresses = Geocoder(context, Locale("en", "PK"))
                .getFromLocationName(query, 10, MIN_LAT, MIN_LNG, MAX_LAT, MAX_LNG)
                ?: emptyList()
            addresses.mapNotNull { address ->
                if (!isInsideRawalpindi(address.latitude, address.longitude)) return@mapNotNull null
                val name = address.featureName ?: address.thoroughfare ?: address.subLocality ?: query
                val fullAddress = (0..address.maxAddressLineIndex)
                    .mapNotNull { address.getAddressLine(it) }
                    .joinToString(", ")
                    .ifBlank { name }
                RawalpindiSearchResult(name, fullAddress, Point.fromLngLat(address.longitude, address.latitude))
            }
        } catch (e: Exception) {
            Log.w("RawalpindiSearch", "Android geocoder fallback failed for '$query'", e)
            emptyList()
        }
    }

    private fun readJson(url: URL, userAgent: String? = null): String? {
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 8000
            readTimeout = 8000
            userAgent?.let { setRequestProperty("User-Agent", it) }
        }
        return try {
            if (connection.responseCode != HttpURLConnection.HTTP_OK) return null
            BufferedReader(InputStreamReader(connection.inputStream)).use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    private fun canonicalizeLocality(query: String): String = query.trim()
        .replace(Regex("\\s+"), " ")
        .replace(Regex("\\s+abad", RegexOption.IGNORE_CASE), "abad")
        .replace(Regex("a\\s+bad", RegexOption.IGNORE_CASE), "abad")
        .replace(Regex("aabad", RegexOption.IGNORE_CASE), "abad")

    private fun isInsideRawalpindi(latitude: Double, longitude: Double): Boolean =
        latitude.isFinite() && longitude.isFinite() &&
            latitude in MIN_LAT..MAX_LAT && longitude in MIN_LNG..MAX_LNG
}

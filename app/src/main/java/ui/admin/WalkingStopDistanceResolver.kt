package ui.admin

import android.content.Context
import android.util.Log
import com.example.bustrack_app.R
import com.mapbox.geojson.Point
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

/** Gets pedestrian network distances from one home location to a large stop list. */
object WalkingStopDistanceResolver {
    private const val MATRIX_COORDINATE_LIMIT = 25

    suspend fun resolveMeters(context: Context, origin: Point, stops: List<Point>): List<Double?>? =
        withContext(Dispatchers.IO) {
            if (stops.isEmpty()) return@withContext emptyList()
            try {
                val result = mutableListOf<Double?>()
                // Matrix API permits up to 25 coordinates: the home plus 24 stops.
                stops.chunked(MATRIX_COORDINATE_LIMIT - 1).forEach { stopChunk ->
                    val coordinates = listOf(origin) + stopChunk
                    val coordinateString = coordinates.joinToString(";") {
                        String.format(Locale.US, "%.6f,%.6f", it.longitude(), it.latitude())
                    }
                    val destinations = (1 until coordinates.size).joinToString(";")
                    val url = URL(
                        "https://api.mapbox.com/directions-matrix/v1/mapbox/walking/$coordinateString" +
                            "?sources=0&destinations=$destinations&annotations=distance" +
                            "&access_token=${context.getString(R.string.mapbox_access_token)}"
                    )
                    val connection = (url.openConnection() as HttpURLConnection).apply {
                        requestMethod = "GET"
                        connectTimeout = 10_000
                        readTimeout = 12_000
                    }
                    val payload = try {
                        if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                            throw IllegalStateException("Walking matrix request failed (${connection.responseCode})")
                        }
                        connection.inputStream.bufferedReader().use { it.readText() }
                    } finally {
                        connection.disconnect()
                    }
                    val distanceRow = JSONObject(payload).optJSONArray("distances")
                        ?.optJSONArray(0)
                        ?: throw IllegalStateException("Walking matrix response has no distance row")
                    for (index in 0 until stopChunk.size) {
                        val meters = if (distanceRow.isNull(index)) null else distanceRow.optDouble(index, Double.NaN)
                        result += meters?.takeIf { it.isFinite() && it >= 0.0 }
                    }
                }
                result
            } catch (error: Exception) {
                Log.w("WalkingStopDistance", "Pedestrian distance lookup failed; no walking-based match is available", error)
                null
            }
        }
}

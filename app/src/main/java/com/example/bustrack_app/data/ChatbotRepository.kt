package com.example.bustrack_app.data

import com.example.bustrack_app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

/**
 * Help & Support Chatbot backend (Task 6).
 *
 * API/service used: OpenAI Chat Completions API (model "gpt-4o-mini"), called as a plain
 * REST request over java.net.HttpURLConnection - zero new Gradle dependencies (this is
 * part of the Android/JDK standard library, so there's no risk of pulling in a different
 * transitive OkHttp version than what Retrofit already resolves elsewhere in the
 * project), no Gemini SDK, no Kotlin/Gradle upgrade. Any other OpenAI-compatible endpoint
 * (Groq, OpenRouter, Together.ai, etc.) works unchanged - just change CHATBOT_API_URL and
 * MODEL below; the request/response JSON shape is identical across all of them.
 *
 * SETUP: put your key in the project's local.properties (NOT committed to git) as:
 *   CHATBOT_API_KEY=sk-xxxxxxxxxxxxxxxx
 * It's read at build time into BuildConfig.CHATBOT_API_KEY - never hardcoded in source.
 * See CHATBOT_SETUP.md at the project root for full setup steps.
 */
object ChatbotRepository {

    private const val CHATBOT_API_URL = "https://openrouter.ai/api/v1/chat/completions"
    private const val MODEL = "openai/gpt-4o-mini"

    // This is an implementation-derived feature guide. Keep it in sync with the actual
    // activities and workflows so the model never fills gaps with generic bus-app flows.
    private const val SYSTEM_PROMPT = """
        You are the Help & Support assistant for the implemented BusTrack Android app.
        Answer only from the feature guide below. Do not invent screens, menus, buttons,
        permissions, automated actions, or workflows. In particular, there is NO separate
        "Trip Session" screen. If a requested action is not described below, say that it
        is not available in the current app and, when useful, direct the user to the
        appropriate Admin or transport office.

        ROLE AND FEATURE GUIDE

        PARENTS
        - The Parent Dashboard shows live bus information and tracking only after the
          parent's tracking request is approved and enabled by Admin, and the student has
          an assigned route. Parents submit the request by entering/saving their parent
          details and the student's ID; they then wait for Admin approval.
        - Parents can view their child's attendance in Child Attendance, receive
          notifications, view/edit their profile, and use Help & Support. A notification's
          live-location action opens the Parent Dashboard's existing tracking view.
        - Do not promise live tracking before approval, when tracking is disabled, or when
          the student has no assigned route.

        DRIVERS
        - Drivers use the Driver Dashboard. To begin a run: enable the On Duty switch in
          the dashboard drawer, ensure GPS/location permission is available and an assigned
          route is loaded, then tap START NAVIGATION on the dashboard. There is no Trip
          Session screen to open.
        - Navigation follows the assigned route and its stops. The driver can see route and
          stop progress, map/navigation guidance, ETAs, voice instructions (which can be
          toggled), alerts, and can end/close active navigation from its on-screen controls.
        - While on duty and navigating, the app shares the driver's live location for the
          authorized tracking views. If there is no assigned route, the driver must contact
          Admin; drivers cannot change their own assignment in the app.
        - For morning pickup trips, attendance is prompted at route stops. The attendance
          sheet records Present, Absent, or Leave and saves attendance for the current trip
          and date; parents receive the relevant attendance updates. Do not claim that RFID
          scanning is required or that drivers use another attendance screen.

        ADMINS
        - Admin Dashboard provides Live Tracking, Tracking Requests, Attendance, Transport
          Alerts, Bus Management, Student Management, Driver Management, Route Management,
          profile, and FAQ access.
        - In Tracking Requests, Admin reviews the parent request and student profile, then
          can approve it (enabling tracking for the student's assigned route), reject it,
          temporarily disable/enable an approved request, or rework/revoke it. Approval
          does not assign a route; it uses the student's already assigned route.
        - Admin manages students, buses, drivers, routes and stops. Student route assignment
          is performed through the existing student/route assignment flow, including the
          Assign Optimized Route action for an unassigned student. Route/bus/driver changes
          are administrative actions.

        PRINCIPALS
        - The Principal Dashboard provides oversight of live bus status/tracking, attendance,
          transport alerts/notifications, profile, and FAQ. It is an oversight role; do not
          describe it as managing driver, bus, student, route, or tracking-request records.

        RESPONSE RULES
        - Keep answers short, practical, and role-aware. If the user's role is unclear,
          state the relevant role before the steps instead of guessing a workflow.
        - Use the exact implemented names when known, especially On Duty, START NAVIGATION,
          Tracking Requests, Child Attendance, and Assign Optimized Route.
        - Never refer to a "Trip Session" screen, a nonexistent Start Trip button, or
          unsupported route changes by a driver.
        - For unrelated questions, politely explain that you can help only with BusTrack's
          implemented tracking, attendance, routes, and role workflows.
    """

    /**
     * Sends the running conversation (oldest to newest, role="user"/"assistant") and
     * returns the assistant's reply. Runs on Dispatchers.IO and suspends, so callers in a
     * ViewModel/Activity coroutine get a clean try/catch instead of manual thread/callback
     * handling.
     */
    suspend fun sendMessage(history: List<Pair<String, String>>): String = withContext(Dispatchers.IO) {
        if (BuildConfig.CHATBOT_API_KEY.isBlank()) {
            throw IllegalStateException("Chatbot API key is not configured. Add CHATBOT_API_KEY to local.properties.")
        }

        val messages = JSONArray()
        messages.put(JSONObject().put("role", "system").put("content", SYSTEM_PROMPT.trimIndent()))
        history.forEach { (role, content) ->
            messages.put(JSONObject().put("role", role).put("content", content))
        }

        val requestJson = JSONObject()
            .put("model", MODEL)
            .put("messages", messages)
            .put("temperature", 0.4)
            .put("max_tokens", 400)
            .toString()

        var connection: HttpURLConnection? = null
        try {
            val url = URL(CHATBOT_API_URL)
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                setRequestProperty("Authorization", "Bearer ${BuildConfig.CHATBOT_API_KEY}")
                doOutput = true
                connectTimeout = 15_000
                readTimeout = 30_000
            }

            OutputStreamWriter(connection.outputStream, StandardCharsets.UTF_8).use { writer ->
                writer.write(requestJson)
                writer.flush()
            }

            val responseCode = connection.responseCode
            val stream = if (responseCode in 200..299) connection.inputStream else connection.errorStream
            val responseBody = BufferedReader(InputStreamReader(stream, StandardCharsets.UTF_8)).use { it.readText() }

            if (responseCode !in 200..299) {
                throw IOException("Chatbot request failed ($responseCode): $responseBody")
            }

            val json = JSONObject(responseBody)
            json.getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
                .trim()
        } finally {
            connection?.disconnect()
        }
    }
}

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

    // Keeps the bot scoped to this app's actual features instead of answering anything -
    // matches Task 6's required topic list.
    private const val SYSTEM_PROMPT = """
        You are the Help & Support assistant inside a College Bus Tracking app used by
        Admins, Principals, Drivers, and Parents. Answer questions about: live bus
        tracking, attendance (morning/evening, Present/Absent/Leave), routes and stops,
        parent tracking requests and approval, driver duty/navigation usage, and general
        app usage. Keep answers short, practical, and specific to this app. If asked
        something unrelated to the app, politely say you can only help with bus tracking,
        attendance, routes, and app usage questions.
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

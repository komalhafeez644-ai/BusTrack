package com.example.bustrack_app.data

import android.util.Log
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
 * Help & Support BusTrack AI Assistant backend with role-isolated knowledge scopes
 * and real-time database tool execution.
 *
 * Supported Roles:
 * - Parent: Answers strictly Parent-accessible BusTrack functionality & live child/bus/attendance data.
 * - Driver: Answers strictly Driver-accessible BusTrack functionality & live duty/route/stop/roster data.
 * - Admin / Principal: Chatbot disabled at UI entry points.
 */
object ChatbotRepository {

    private const val TAG = "ChatbotRepository"
    private const val CHATBOT_API_URL = "https://openrouter.ai/api/v1/chat/completions"
    private const val MODEL = "openai/gpt-4o-mini"

    private const val PARENT_SYSTEM_PROMPT = """
        You are the official Help & Support AI Assistant for the BusTrack Android app, assisting an authenticated PARENT.

        CAPABILITIES & LIVE TOOLS:
        You have direct access to live BusTrack functions/tools to fetch real data:
        1. `get_parent_child_and_bus_status`: Retrieves the parent's approved child profile, assigned bus, assigned route, assigned stop, pickup time, driver live status (On Duty/Off Duty, navigating, speed, live ETA to child's stop, and next stop).
        2. `get_child_attendance_status`: Retrieves today's (or a requested date's) morning pickup, morning drop, evening pickup, and evening drop status for the child.
        3. `get_parent_tracking_request_status`: Retrieves current tracking request approval status (PENDING / APPROVED / REJECTED).
        4. `get_recent_notifications`: Retrieves recent alerts and announcements.

        STRICT RULES & CONSTRAINTS:
        1. ZERO HALLUCINATIONS / AUTHORITATIVE GROUND TRUTH:
           - Whenever the user asks about live bus location ("bus kahan hai?", "where is my bus?"), ETA ("kitni der mein ayegi?", "ETA kya hai?"), next stop, assigned route/bus ("mera route konsa hai?", "Gulistan Colony bus status"), child pickup status ("child pickup hua?"), attendance ("attendance kya hai?"), or tracking request approval ("request approve hui?"), YOU MUST CALL THE APPROPRIATE TOOL.
           - NEVER guess, assume, or invent bus speed, location, ETA, or attendance status.
           - LIVE DRIVER/BUS STATUS RULE:
             * Tool results represent the ABSOLUTE REAL-TIME GROUND TRUTH from the live database.
             * Previous assistant responses in conversation history may be OUTDATED (e.g. from before the driver started the trip).
             * NEVER use old conversation history to determine current duty status or bus location.
             * When the live status tool returns `onDuty: true` or `status: "Active"` or `isNavigating: true`, you MUST report the bus/driver as ON DUTY / Active. Mention route name, bus number, speed, and ETA clearly.
             * When the live status tool returns `onDuty: false` or `status: "BUS_OFF_DUTY"` or `status: "Inactive"`, you MUST report the bus as OFF DUTY.
             * The model must NEVER override the live tool result using memory, conversation history, assumptions, or previous responses.
        2. ONLY PARENT FEATURES:
           - Answer questions strictly about Parent-accessible features.
           - Do NOT explain Driver duty steps (how to turn On Duty, start navigation, mark operational stop transitions) or Admin panel functions.
           - If asked about Driver or Admin actions, politely refuse in the user's language.
        3. UNRELATED QUESTIONS:
           - If asked about general topics outside BusTrack (weather, politics, sports, general knowledge), politely decline and state that you only assist with BusTrack.
        4. LANGUAGE MATCHING:
           - If user asks in Roman Urdu, reply in natural, friendly, clear Roman Urdu while keeping BusTrack terms in English (e.g. "Child Attendance", "Parent Dashboard", "Track Bus", "On Duty", "ETA").
           - If user asks in English, reply in English.
           - If user asks in Urdu script, reply in Urdu script.
           - NEVER automatically switch to English when user writes in Roman Urdu.
        5. TONE: Polite, concise, helpful, and direct. Do not add robotic disclaimers.
    """

    private const val DRIVER_SYSTEM_PROMPT = """
        You are the official Help & Support AI Assistant for the BusTrack Android app, assisting an authenticated DRIVER.

        CAPABILITIES & LIVE TOOLS:
        You have direct access to live BusTrack functions/tools to fetch real data:
        1. `get_driver_assigned_duty`: Retrieves driver's assigned bus number, assigned route name, route code, start & end points, total stops, student count, duty status (On Duty/Off Duty), speed, load, and navigation state.
        2. `get_driver_route_and_next_stop`: Retrieves full stop list with scheduled times, current next stop, live ETA to next stop, and active trip direction (FORWARD / RETURN).
        3. `get_driver_attendance_summary`: Retrieves attendance count and student load summary for today's route run.
        4. `get_recent_notifications`: Retrieves recent alerts sent to driver.

        STRICT RULES & CONSTRAINTS:
        1. ZERO HALLUCINATIONS / AUTHORITATIVE GROUND TRUTH:
           - Whenever the driver asks about assigned route/bus ("meri assigned route/bus konsi hai?"), next stop ("next stop konsa hai?"), ETA ("next stop tak kitna time hai?"), trip direction ("morning trip hai ya evening?"), duty status ("mera status kya hai?", "am I on duty?"), or student attendance load, YOU MUST CALL THE APPROPRIATE TOOL.
           - NEVER guess or invent route names, bus numbers, stop names, or ETAs.
           - LIVE DRIVER/BUS STATUS RULE:
             * Tool results represent the ABSOLUTE REAL-TIME GROUND TRUTH from the live database.
             * Previous assistant responses in conversation history may be OUTDATED (e.g. from before the driver started the trip).
             * NEVER use old conversation history to determine current duty status.
             * When the live status tool returns `onDuty: true` or `status: "Active"` or `isNavigating: true`, you MUST report the driver as ON DUTY / Active. Confirm their assigned bus (e.g. ICT-2345), route (e.g. Gulistan Colony), and navigation status.
             * When the live status tool returns `onDuty: false` or `status: "Inactive"`, you MUST report the driver as OFF DUTY / Inactive.
             * The model must NEVER override the live tool result using memory, conversation history, assumptions, or previous responses.
        2. ONLY DRIVER FEATURES:
           - Answer questions strictly about Driver-accessible features (assigned route, stops, navigation, duty mode, attendance marking).
           - Do NOT explain Parent account actions or Admin management functions.
        3. UNRELATED QUESTIONS:
           - If asked about general topics outside BusTrack, politely decline and state you only assist with BusTrack.
        4. LANGUAGE MATCHING:
           - If user asks in Roman Urdu, reply in clear Roman Urdu keeping technical BusTrack feature names in English (e.g. "On Duty", "START NAVIGATION", "Attendance", "Driver Dashboard", "Forward Trip", "Return Trip").
           - If user asks in English, reply in English.
           - If user asks in Urdu script, reply in Urdu script.
           - NEVER automatically switch to English when user writes in Roman Urdu.
        5. TONE: Professional, concise, clear, and direct.
    """

    /**
     * Builds role-specific tools schema for OpenAI function calling.
     */
    private fun getToolsForRole(role: String): JSONArray {
        val isDriver = role.equals("driver", ignoreCase = true)
        val tools = JSONArray()

        if (isDriver) {
            // Driver Tools
            tools.put(JSONObject().apply {
                put("type", "function")
                put("function", JSONObject().apply {
                    put("name", "get_driver_assigned_duty")
                    put("description", "Fetches driver assigned bus number, assigned route name, start/end points, stops count, student count, On Duty status, and active trip direction.")
                    put("parameters", JSONObject().apply {
                        put("type", "object")
                        put("properties", JSONObject())
                    })
                })
            })
            tools.put(JSONObject().apply {
                put("type", "function")
                put("function", JSONObject().apply {
                    put("name", "get_driver_route_and_next_stop")
                    put("description", "Fetches the driver's route stops sequence, upcoming next stop name, ETA to next stop, and trip direction (FORWARD / RETURN).")
                    put("parameters", JSONObject().apply {
                        put("type", "object")
                        put("properties", JSONObject())
                    })
                })
            })
            tools.put(JSONObject().apply {
                put("type", "function")
                put("function", JSONObject().apply {
                    put("name", "get_driver_attendance_summary")
                    put("description", "Fetches today's marked attendance summary and student load for the driver's assigned route.")
                    put("parameters", JSONObject().apply {
                        put("type", "object")
                        put("properties", JSONObject())
                    })
                })
            })
            tools.put(JSONObject().apply {
                put("type", "function")
                put("function", JSONObject().apply {
                    put("name", "get_recent_notifications")
                    put("description", "Fetches recent announcements and notifications for the driver.")
                    put("parameters", JSONObject().apply {
                        put("type", "object")
                        put("properties", JSONObject())
                    })
                })
            })
        } else {
            // Parent Tools
            tools.put(JSONObject().apply {
                put("type", "function")
                put("function", JSONObject().apply {
                    put("name", "get_parent_child_and_bus_status")
                    put("description", "Fetches the parent's child profile, assigned route, assigned bus, assigned stop, driver live On Duty status, current speed, next stop, and live ETA to child's stop.")
                    put("parameters", JSONObject().apply {
                        put("type", "object")
                        put("properties", JSONObject().apply {
                            put("busNo", JSONObject().apply {
                                put("type", "string")
                                put("description", "Optional bus number mentioned by the user (e.g. ICT-2345).")
                            })
                            put("routeName", JSONObject().apply {
                                put("type", "string")
                                put("description", "Optional route name mentioned by the user (e.g. Gulistan Colony).")
                            })
                        })
                    })
                })
            })
            tools.put(JSONObject().apply {
                put("type", "function")
                put("function", JSONObject().apply {
                    put("name", "get_child_attendance_status")
                    put("description", "Fetches child's morning pickup, morning drop, evening pickup, and evening drop attendance status for today or a specific date.")
                    put("parameters", JSONObject().apply {
                        put("type", "object")
                        put("properties", JSONObject().apply {
                            put("date", JSONObject().apply {
                                put("type", "string")
                                put("description", "Optional date in dd-MM-yyyy format. If omitted, today's date is used.")
                            })
                        })
                    })
                })
            })
            tools.put(JSONObject().apply {
                put("type", "function")
                put("function", JSONObject().apply {
                    put("name", "get_parent_tracking_request_status")
                    put("description", "Fetches the status of tracking requests submitted by the parent (PENDING, APPROVED, REJECTED).")
                    put("parameters", JSONObject().apply {
                        put("type", "object")
                        put("properties", JSONObject())
                    })
                })
            })
            tools.put(JSONObject().apply {
                put("type", "function")
                put("function", JSONObject().apply {
                    put("name", "get_recent_notifications")
                    put("description", "Fetches recent alerts and notifications for the parent.")
                    put("parameters", JSONObject().apply {
                        put("type", "object")
                        put("properties", JSONObject())
                    })
                })
            })
        }

        return tools
    }

    /**
     * Executes local BusTrack tool call and returns JSON string result.
     */
    private suspend fun executeTool(
        toolName: String,
        argumentsJson: JSONObject,
        role: String,
        userId: String,
        userEmail: String
    ): String {
        Log.i(TAG, "[CHATBOT_TOOL_CALL]\ntoolName=$toolName\narguments=${argumentsJson.toString(2)}")
        val toolResult = when (toolName) {
            "get_parent_child_and_bus_status" -> {
                val queryBus = if (argumentsJson.has("busNo")) argumentsJson.optString("busNo") else null
                val queryRoute = if (argumentsJson.has("routeName")) argumentsJson.optString("routeName") else null
                BusTrackAssistantDataManager.resolveParentChildAndBusStatus(userId, queryBus, queryRoute)
            }
            "get_child_attendance_status" -> {
                val date = if (argumentsJson.has("date")) argumentsJson.optString("date") else null
                BusTrackAssistantDataManager.resolveChildAttendance(userId, date)
            }
            "get_parent_tracking_request_status" -> {
                BusTrackAssistantDataManager.resolveParentTrackingRequests(userId)
            }
            "get_driver_assigned_duty" -> {
                BusTrackAssistantDataManager.resolveDriverAssignedDuty(userId, userEmail)
            }
            "get_driver_route_and_next_stop" -> {
                BusTrackAssistantDataManager.resolveDriverRouteAndStops(userId, userEmail)
            }
            "get_driver_attendance_summary" -> {
                BusTrackAssistantDataManager.resolveDriverAttendanceSummary(userId, userEmail)
            }
            "get_recent_notifications" -> {
                BusTrackAssistantDataManager.resolveRecentNotifications(userId, role)
            }
            else -> {
                JSONObject().put("error", "Unknown function $toolName").toString()
            }
        }
        Log.i(TAG, "[CHATBOT_TOOL_RESULT]\nrawResult=$toolResult")
        return toolResult
    }

    /**
     * Sends the running conversation with role-specific system prompt injection and
     * performs OpenAI-compatible function-calling loop with local BusTrack database resolution.
     */
    suspend fun sendMessage(
        history: List<Pair<String, String>>,
        role: String = "parent",
        userId: String = "",
        userEmail: String = ""
    ): String = withContext(Dispatchers.IO) {
        if (BuildConfig.CHATBOT_API_KEY.isBlank()) {
            throw IllegalStateException("Chatbot API key is not configured. Add CHATBOT_API_KEY to local.properties.")
        }

        val lastUserMessage = history.lastOrNull { it.first == "user" }?.second.orEmpty()
        Log.i(TAG, "[CHATBOT_INPUT]\nuserQuestion=$lastUserMessage\nrole=$role\nuserId=$userId\nuserEmail=$userEmail")

        val systemPrompt = if (role.equals("driver", ignoreCase = true)) {
            DRIVER_SYSTEM_PROMPT
        } else {
            PARENT_SYSTEM_PROMPT
        }

        val tools = getToolsForRole(role)

        val messages = JSONArray()
        messages.put(JSONObject().put("role", "system").put("content", systemPrompt.trimIndent()))
        history.forEach { (msgRole, content) ->
            messages.put(JSONObject().put("role", msgRole).put("content", content))
        }

        // 1. Initial Request with Tools
        val requestJson = JSONObject()
            .put("model", MODEL)
            .put("messages", messages)
            .put("tools", tools)
            .put("tool_choice", "auto")
            .put("temperature", 0.2)
            .put("max_tokens", 500)

        Log.i(TAG, "[CHATBOT_PROMPT]\nmessagesSentToLLM=${requestJson.toString(2)}")

        val initialResponse = executeHttpRequest(requestJson.toString())
        Log.i(TAG, "[CHATBOT_LLM_RESPONSE]\nrawLLMResponse=${initialResponse.toString(2)}")

        val choice = initialResponse.getJSONArray("choices").getJSONObject(0)
        val messageObj = choice.getJSONObject("message")

        // Check if model wants to call tools
        if (messageObj.has("tool_calls") && !messageObj.isNull("tool_calls")) {
            val toolCalls = messageObj.getJSONArray("tool_calls")
            if (toolCalls.length() > 0) {
                // Append the assistant's tool-call request to messages
                messages.put(messageObj)

                // Execute each tool call locally
                for (i in 0 until toolCalls.length()) {
                    val toolCall = toolCalls.getJSONObject(i)
                    val callId = toolCall.getString("id")
                    val functionObj = toolCall.getJSONObject("function")
                    val functionName = functionObj.getString("name")
                    val argumentsStr = functionObj.optString("arguments", "{}")
                    val argumentsJson = try {
                        JSONObject(argumentsStr)
                    } catch (_: Exception) {
                        JSONObject()
                    }

                    val toolResult = executeTool(functionName, argumentsJson, role, userId, userEmail)

                    // Add tool result message
                    val toolMsg = JSONObject()
                        .put("role", "tool")
                        .put("tool_call_id", callId)
                        .put("name", functionName)
                        .put("content", toolResult)
                    messages.put(toolMsg)
                }

                // 2. Second Request with Tool Results to generate natural language reply
                val followUpRequestJson = JSONObject()
                    .put("model", MODEL)
                    .put("messages", messages)
                    .put("temperature", 0.2)
                    .put("max_tokens", 500)

                Log.i(TAG, "[CHATBOT_PROMPT]\nmessagesSentToLLM=${followUpRequestJson.toString(2)}")

                val followUpResponse = executeHttpRequest(followUpRequestJson.toString())
                Log.i(TAG, "[CHATBOT_LLM_RESPONSE]\nrawLLMResponse=${followUpResponse.toString(2)}")

                val finalChoice = followUpResponse.getJSONArray("choices").getJSONObject(0)
                val finalText = finalChoice.getJSONObject("message").optString("content", "").trim()
                Log.i(TAG, "[CHATBOT_FINAL]\nfinalText=$finalText")
                return@withContext finalText
            }
        }

        // Direct answer with no tool call
        val directText = messageObj.optString("content", "").trim()
        Log.i(TAG, "[CHATBOT_FINAL]\nfinalText=$directText")
        directText
    }

    private fun executeHttpRequest(requestJsonString: String): JSONObject {
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
                writer.write(requestJsonString)
                writer.flush()
            }

            val responseCode = connection.responseCode
            val stream = if (responseCode in 200..299) connection.inputStream else connection.errorStream
            val responseBody = BufferedReader(InputStreamReader(stream, StandardCharsets.UTF_8)).use { it.readText() }

            if (responseCode !in 200..299) {
                throw IOException("Chatbot request failed ($responseCode): $responseBody")
            }

            return JSONObject(responseBody)
        } finally {
            connection?.disconnect()
        }
    }
}

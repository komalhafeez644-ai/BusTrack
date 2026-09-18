package ui.chatbot

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.bustrack_app.R
import com.example.bustrack_app.adapter.ChatMessageAdapter
import com.example.bustrack_app.data.AuthRepository
import com.example.bustrack_app.data.ChatbotRepository
import com.example.bustrack_app.models.ChatMessageModel
import kotlinx.coroutines.launch

/**
 * Help & Support Chatbot. Accessible only for Parent and Driver roles.
 * Disallowed for Admin and Principal accounts.
 */
class ChatbotActivity : AppCompatActivity() {

    private lateinit var rvChat: RecyclerView
    private lateinit var etInput: EditText
    private lateinit var progressSending: ProgressBar
    private lateinit var btnSend: ImageView
    private lateinit var tvEmpty: TextView
    private lateinit var tvChatStatus: TextView

    private val messages = mutableListOf<ChatMessageModel>()
    private lateinit var adapter: ChatMessageAdapter
    private var userRole: String = "parent"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chatbot)

        supportActionBar?.hide()

        val passedRole = intent.getStringExtra("USER_ROLE")?.lowercase()
        if (!passedRole.isNullOrBlank()) {
            userRole = passedRole
        }

        rvChat = findViewById(R.id.rvChatMessages)
        etInput = findViewById(R.id.etChatInput)
        progressSending = findViewById(R.id.progressSending)
        btnSend = findViewById(R.id.btnSendChat)
        tvEmpty = findViewById(R.id.tvEmptyChat)
        tvChatStatus = findViewById(R.id.tvChatStatus)

        adapter = ChatMessageAdapter(messages)
        rvChat.layoutManager = LinearLayoutManager(this)
        rvChat.adapter = adapter

        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }

        btnSend.setOnClickListener {
            sendCurrentInput()
        }
        etInput.setOnEditorActionListener { _, _, _ ->
            sendCurrentInput()
            true
        }

        resolveRoleAndConfigureUI()
    }

    private fun resolveRoleAndConfigureUI() {
        // Immediate UI configuration based on passed intent role
        applyRoleUI(userRole)

        lifecycleScope.launch {
            try {
                val actualRole = AuthRepository().getCurrentUserRole().lowercase()
                if (actualRole.isNotBlank()) {
                    userRole = actualRole
                }
            } catch (e: Exception) {
                Log.w("ChatbotActivity", "Role lookup error, fallback to $userRole", e)
            }

            if (userRole == "admin" || userRole == "principal") {
                Toast.makeText(this@ChatbotActivity, "Chatbot is not available for this account.", Toast.LENGTH_SHORT).show()
                finish()
                return@launch
            }

            applyRoleUI(userRole)
        }
    }

    private fun applyRoleUI(role: String) {
        if (role == "driver") {
            tvChatStatus.text = "Ask about assigned route, stops, duty, navigation…"
            tvEmpty.text = "Ask me anything about your assigned route, stops, navigation, duty mode, or attendance marking."
            etInput.hint = "Type your driver question…"
        } else {
            tvChatStatus.text = "Ask about tracking, child attendance, routes…"
            tvEmpty.text = "Ask me anything about child attendance, bus tracking, pickup/drop status, route info, or notifications."
            etInput.hint = "Type your question…"
        }
    }

    private fun sendCurrentInput() {
        val text = etInput.text.toString().trim()
        if (text.isEmpty() || progressSending.visibility == View.VISIBLE) return

        tvEmpty.visibility = View.GONE
        etInput.setText("")

        adapter.addMessage(ChatMessageModel("user", text))
        rvChat.scrollToPosition(messages.size - 1)

        setLoading(true)

        lifecycleScope.launch {
            try {
                // Send full running history and user role for role-isolated prompt injection
                val history = messages.map { it.role to it.content }
                val reply = ChatbotRepository.sendMessage(history, userRole)
                adapter.addMessage(ChatMessageModel("assistant", reply))
            } catch (e: Exception) {
                Log.e("ChatbotDebug", "Chatbot request failed: ${e.javaClass.name}: ${e.message}", e)

                val friendlyMessage = when {
                    e.message?.contains("API key", true) == true ->
                        "Chatbot isn't configured yet. Please contact the app administrator."
                    else ->
                        "Sorry, I couldn't reach the support assistant right now. Please check your internet connection and try again."
                }
                adapter.addMessage(ChatMessageModel("assistant", friendlyMessage, isError = true))
            } finally {
                setLoading(false)
                rvChat.scrollToPosition(messages.size - 1)
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        progressSending.visibility = if (loading) View.VISIBLE else View.GONE
        btnSend.visibility = if (loading) View.GONE else View.VISIBLE
        etInput.isEnabled = !loading
    }
}
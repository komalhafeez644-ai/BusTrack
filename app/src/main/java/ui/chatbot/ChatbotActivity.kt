package ui.chatbot

import android.os.Bundle
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
import com.example.bustrack_app.data.ChatbotRepository
import com.example.bustrack_app.models.ChatMessageModel
import kotlinx.coroutines.launch

/**
 * Help & Support Chatbot (Task 6). Reachable from every module's existing FAQ screen via
 * a "Chat with us" button - the FAQ screens themselves are untouched otherwise.
 */
class ChatbotActivity : AppCompatActivity() {

    private lateinit var rvChat: RecyclerView
    private lateinit var etInput: EditText
    private lateinit var progressSending: ProgressBar
    private lateinit var btnSend: ImageView
    private lateinit var tvEmpty: TextView

    private val messages = mutableListOf<ChatMessageModel>()
    private lateinit var adapter: ChatMessageAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chatbot)

        supportActionBar?.hide()

        rvChat = findViewById(R.id.rvChatMessages)
        etInput = findViewById(R.id.etChatInput)
        progressSending = findViewById(R.id.progressSending)
        btnSend = findViewById(R.id.btnSendChat)
        tvEmpty = findViewById(R.id.tvEmptyChat)

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
                // Send full running history so the bot has conversational context, not
                // just the last message.
                val history = messages.map { it.role to it.content }
                val reply = ChatbotRepository.sendMessage(history)
                adapter.addMessage(ChatMessageModel("assistant", reply))
            } catch (e: Exception) {
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

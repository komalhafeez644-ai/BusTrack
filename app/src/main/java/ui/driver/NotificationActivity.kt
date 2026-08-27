package ui.driver

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.bustrack_app.R
import com.example.bustrack_app.data.FirebaseRepository
import com.example.bustrack_app.models.NotificationModel
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.ktx.Firebase
import utils.FormUtils
import utils.ViewUtils

class NotificationActivity : AppCompatActivity() {

    private lateinit var rvNotifications: RecyclerView
    private var listeners: List<ListenerRegistration> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.drivernotifications)

        window.statusBarColor = Color.parseColor("#051024")

        rvNotifications = findViewById(R.id.rvNotifications)
        rvNotifications.layoutManager = LinearLayoutManager(this)

        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            ViewUtils.applyClickEffect(it)
            finish()
        }

        listenForNotifications()
    }

    override fun onDestroy() {
        super.onDestroy()
        listeners.forEach { it.remove() }
    }

    private fun listenForNotifications() {
        val uid = Firebase.auth.currentUser?.uid ?: return
        listeners = FirebaseRepository.listenToNotifications(uid, "driver") { notifications ->
            val displayList = notifications.map { it.toNotificationData() }
            rvNotifications.adapter = NotificationAdapter(displayList) { data ->
                notifications.find { it.id == data.id }?.let { original ->
                    if (!original.isRead) FirebaseRepository.markNotificationRead(original.id)
                }
                val intent = Intent(this, DriverAlertDetailActivity::class.java)
                intent.putExtra("ALERT_TITLE", data.title)
                intent.putExtra("ALERT_MESSAGE", data.message)
                intent.putExtra("ALERT_TIME", data.time)
                intent.putExtra("ALERT_TYPE", data.type)
                intent.putExtra("ALERT_ICON", data.iconRes)
                startActivity(intent)
            }
        }
    }

    private fun NotificationModel.toNotificationData(): NotificationData {
        val (tag, icon) = when (this.type) {
            "ATTENDANCE" -> "CRITICAL" to R.drawable.warning
            "BROADCAST" -> "IMPORTANT" to R.drawable.notifications
            else -> "GENERAL" to R.drawable.notifications
        }
        return NotificationData(this.id, this.title, this.message, FormUtils.timeAgo(this.timestamp), tag, icon)
    }

    data class NotificationData(
        val id: String,
        val title: String,
        val message: String,
        val time: String,
        val type: String,
        val iconRes: Int
    )

    class NotificationAdapter(
        private val items: List<NotificationData>,
        private val onItemClick: (NotificationData) -> Unit
    ) : RecyclerView.Adapter<NotificationAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvTitle: TextView = view.findViewById(R.id.tvAlertTitle)
            val tvSubtitle: TextView = view.findViewById(R.id.tvAlertSubtitle)
            val tvTime: TextView = view.findViewById(R.id.tvTime)
            val ivIcon: ImageView = view.findViewById(R.id.ivAlertIcon)
            val tvTag: TextView = view.findViewById(R.id.tvTagText)
            val cvIconBg: CardView = view.findViewById(R.id.cvIconBg)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_alert_card, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.tvTitle.text = item.title
            holder.tvSubtitle.text = item.message
            holder.tvTime.text = item.time
            holder.ivIcon.setImageResource(item.iconRes)
            holder.tvTag.text = item.type

            val (bgColor, textColor) = when (item.type.uppercase()) {
                "CRITICAL" -> Pair("#FEE2E2", "#EF4444")
                "IMPORTANT" -> Pair("#FEF3C7", "#F59E0B")
                else -> Pair("#D1FAE5", "#10B981")
            }

            holder.cvIconBg.setCardBackgroundColor(Color.parseColor(bgColor))
            holder.ivIcon.setColorFilter(Color.parseColor(textColor))
            holder.tvTag.setTextColor(Color.parseColor(textColor))
            holder.tvTag.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor(bgColor))

            holder.itemView.setOnClickListener { onItemClick(item) }
        }

        override fun getItemCount() = items.size
    }
}

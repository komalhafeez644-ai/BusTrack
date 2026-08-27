package ui.parent

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.bustrack_app.R
import com.example.bustrack_app.data.FirebaseRepository
import com.example.bustrack_app.models.NotificationModel
import com.example.bustrack_app.models.NotificationType
import com.example.bustrack_app.models.ParentNotificationModel
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.ktx.Firebase
import utils.FormUtils

class ParentNotificationsActivity : AppCompatActivity() {

    private lateinit var rvNotifications: RecyclerView
    private lateinit var emptyState: View
    private var listeners: List<ListenerRegistration> = emptyList()
    private var currentNotifications: List<NotificationModel> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_parent_notifications)

        supportActionBar?.hide()

        rvNotifications = findViewById(R.id.rvNotifications)
        emptyState = findViewById(R.id.emptyState)
        rvNotifications.layoutManager = LinearLayoutManager(this)

        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            finish()
        }

        findViewById<TextView>(R.id.btnClearAll).setOnClickListener {
            // Marks everything currently loaded as read in Firestore (keeps history,
            // matches the "Mark as read functionality" requirement).
            val unreadIds = currentNotifications.filter { !it.isRead }.map { it.id }
            FirebaseRepository.markAllNotificationsRead(unreadIds)
        }

        listenForNotifications()
    }

    override fun onDestroy() {
        super.onDestroy()
        listeners.forEach { it.remove() }
    }

    private fun listenForNotifications() {
        val uid = Firebase.auth.currentUser?.uid ?: return
        listeners = FirebaseRepository.listenToNotifications(uid, "parent") { notifications ->
            currentNotifications = notifications
            emptyState.visibility = if (notifications.isEmpty()) View.VISIBLE else View.GONE

            val displayList = notifications.map { it.toParentNotificationModel() }
            rvNotifications.adapter = NotificationAdapter(displayList) { notif ->
                val original = notifications.find { it.id == notif.id }
                if (original != null && !original.isRead) {
                    FirebaseRepository.markNotificationRead(original.id)
                }
                val intent = Intent(this, ParentNotificationDetailActivity::class.java)
                intent.putExtra("notification", notif)
                startActivity(intent)
            }
        }
    }

    private fun NotificationModel.toParentNotificationModel(): ParentNotificationModel {
        val type = when (this.type) {
            "TRACKING_APPROVED" -> NotificationType.ARRIVAL
            "TRACKING_REJECTED", "TRACKING_REVOKED" -> NotificationType.CANCELLATION
            "ATTENDANCE" -> NotificationType.DELAY
            "BROADCAST" -> NotificationType.ROUTE_CHANGE
            else -> NotificationType.GENERAL
        }
        return ParentNotificationModel(
            id = this.id,
            title = this.title,
            message = this.message,
            time = FormUtils.timeAgo(this.timestamp),
            type = type,
            isRead = this.isRead
        )
    }

    class NotificationAdapter(
        private val items: List<ParentNotificationModel>,
        private val onItemClick: (ParentNotificationModel) -> Unit
    ) : RecyclerView.Adapter<NotificationAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvTitle: TextView = view.findViewById(R.id.tvNotifTitle)
            val tvMessage: TextView = view.findViewById(R.id.tvNotifMessage)
            val tvTime: TextView = view.findViewById(R.id.tvNotifTime)
            val ivIcon: ImageView = view.findViewById(R.id.ivNotifIcon)
            val vUnread: View = view.findViewById(R.id.vUnreadDot)
            val cvIconBg: androidx.cardview.widget.CardView = view.findViewById(R.id.cvIconBg)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_parent_notification, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.tvTitle.text = item.title
            holder.tvMessage.text = item.message
            holder.tvTime.text = item.time
            holder.vUnread.visibility = if (item.isRead) View.GONE else View.VISIBLE

            when (item.type) {
                NotificationType.DELAY -> {
                    holder.ivIcon.setImageResource(R.drawable.notifications)
                    holder.cvIconBg.setCardBackgroundColor(0xFFFEF2F2.toInt()) // Light Red
                    holder.ivIcon.setColorFilter(0xFFEF4444.toInt())
                }
                NotificationType.ARRIVAL -> {
                    holder.ivIcon.setImageResource(R.drawable.person_check)
                    holder.cvIconBg.setCardBackgroundColor(0xFFF0FDF4.toInt()) // Light Green
                    holder.ivIcon.setColorFilter(0xFF22C55E.toInt())
                }
                NotificationType.CANCELLATION -> {
                    holder.ivIcon.setImageResource(R.drawable.warning)
                    holder.cvIconBg.setCardBackgroundColor(0xFFFFF7ED.toInt()) // Light Orange
                    holder.ivIcon.setColorFilter(0xFFF97316.toInt())
                }
                else -> {
                    holder.ivIcon.setImageResource(R.drawable.notifications)
                    holder.cvIconBg.setCardBackgroundColor(0xFFF1F5F9.toInt())
                    holder.ivIcon.setColorFilter(0xFF3B82F6.toInt())
                }
            }

            holder.itemView.setOnClickListener { onItemClick(item) }
        }

        override fun getItemCount() = items.size
    }
}

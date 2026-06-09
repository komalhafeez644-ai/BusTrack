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
import com.example.bustrack_app.models.NotificationType
import com.example.bustrack_app.models.ParentNotificationModel

class ParentNotificationsActivity : AppCompatActivity() {

    private lateinit var rvNotifications: RecyclerView
    private lateinit var emptyState: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_parent_notifications)

        supportActionBar?.hide()

        rvNotifications = findViewById(R.id.rvNotifications)
        emptyState = findViewById(R.id.emptyState)

        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            finish()
        }

        findViewById<TextView>(R.id.btnClearAll).setOnClickListener {
            // Mock clear
            rvNotifications.adapter = NotificationAdapter(emptyList()) {}
            emptyState.visibility = View.VISIBLE
        }

        setupRecyclerView()
    }

    private fun setupRecyclerView() {
        val notifications = listOf(
            ParentNotificationModel(
                "1", "Bus Delayed", 
                "Route 42-B is running 15 mins late due to traffic.", 
                "2m ago", NotificationType.DELAY, false
            ),
            ParentNotificationModel(
                "2", "Bus Arrived", 
                "The bus has arrived at Sector 15 North stop.", 
                "10m ago", NotificationType.ARRIVAL, true
            ),
            ParentNotificationModel(
                "3", "Route Change", 
                "Route 12-A has been slightly modified for today.", 
                "1h ago", NotificationType.ROUTE_CHANGE, true
            ),
            ParentNotificationModel(
                "4", "No Bus Today", 
                "Bus #102 will not operate today due to maintenance.", 
                "3h ago", NotificationType.CANCELLATION, false
            )
        )

        rvNotifications.layoutManager = LinearLayoutManager(this)
        rvNotifications.adapter = NotificationAdapter(notifications) { notif ->
            val intent = Intent(this, ParentNotificationDetailActivity::class.java)
            intent.putExtra("notification", notif)
            startActivity(intent)
        }
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

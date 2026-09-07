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
    private var allNotifications: List<NotificationModel> = emptyList()
    private var currentFilter = "ALL"

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

        setupFilters()
        listenForNotifications()
    }

    private fun setupFilters() {
        val chipAll = findViewById<TextView>(R.id.chipAll)
        val chipImportant = findViewById<TextView>(R.id.chipImportant)
        val chipGeneral = findViewById<TextView>(R.id.chipGeneral)
        val chipAdminBroadcast = findViewById<TextView>(R.id.chipAdminBroadcast)

        val chips = listOf(chipAll, chipImportant, chipGeneral, chipAdminBroadcast)

        chipAll.setOnClickListener {
            updateFilter("ALL", chips)
        }
        chipImportant.setOnClickListener {
            updateFilter("IMPORTANT", chips)
        }
        chipGeneral.setOnClickListener {
            updateFilter("GENERAL", chips)
        }
        chipAdminBroadcast.setOnClickListener {
            updateFilter("ADMIN_BROADCAST", chips)
        }
    }

    private fun updateFilter(filter: String, chips: List<TextView>) {
        currentFilter = filter
        chips.forEach { chip ->
            val isSelected = (chip.id == R.id.chipAll && filter == "ALL") ||
                    (chip.id == R.id.chipImportant && filter == "IMPORTANT") ||
                    (chip.id == R.id.chipGeneral && filter == "GENERAL") ||
                    (chip.id == R.id.chipAdminBroadcast && filter == "ADMIN_BROADCAST")

            if (isSelected) {
                chip.setBackgroundResource(R.drawable.bg_chip_selected)
                chip.setTextColor(Color.parseColor("#0F172A"))
                chip.setTypeface(null, android.graphics.Typeface.BOLD)
            } else {
                chip.setBackgroundResource(R.drawable.bg_chip_unselected)
                chip.setTextColor(Color.parseColor("#64748B"))
                chip.setTypeface(null, android.graphics.Typeface.NORMAL)
            }
        }
        applyFilter()
    }

    private fun applyFilter() {
        val filteredList = when (currentFilter) {
            "IMPORTANT" -> allNotifications.filter { it.isImportant() }
            "GENERAL" -> allNotifications.filter { it.isGeneral() }
            "ADMIN_BROADCAST" -> allNotifications.filter { it.type == "ADMIN_BROADCAST" }
            else -> allNotifications
        }
        updateRecyclerView(filteredList)
    }

    private fun NotificationModel.isImportant(): Boolean {
        return this.type == NotificationModel.TYPE_IMPORTANT || 
               this.type == NotificationModel.TYPE_EMERGENCY || 
               this.type == NotificationModel.TYPE_TRIP_CANCELLED ||
               this.type == NotificationModel.TYPE_ATTENDANCE_REQUIRED
    }

    private fun NotificationModel.isGeneral(): Boolean {
        val generalTypes = listOf(
            NotificationModel.TYPE_GENERAL,
            NotificationModel.TYPE_NEW_TRIP,
            NotificationModel.TYPE_TRIP_UPDATE,
            NotificationModel.TYPE_ROUTE_UPDATE,
            NotificationModel.TYPE_STOP_UPDATE,
            NotificationModel.TYPE_NAV_READY,
            NotificationModel.TYPE_TRIP_REMINDER,
            NotificationModel.TYPE_TRIP_STARTED,
            NotificationModel.TYPE_ATTENDANCE
        )
        return this.type in generalTypes
    }

    private fun updateRecyclerView(notifications: List<NotificationModel>) {
        val displayList = notifications.map { it.toNotificationData() }
        rvNotifications.adapter = NotificationAdapter(displayList) { data ->
            allNotifications.find { it.id == data.id }?.let { original ->
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

    override fun onDestroy() {
        super.onDestroy()
        listeners.forEach { it.remove() }
    }

    private fun listenForNotifications() {
        val uid = Firebase.auth.currentUser?.uid ?: return
        listeners = FirebaseRepository.listenToNotifications(uid, "driver") { notifications ->
            allNotifications = notifications
            applyFilter()
        }
    }

    private fun NotificationModel.toNotificationData(): NotificationData {
        val (tag, icon) = when (this.type) {
            NotificationModel.TYPE_EMERGENCY -> "EMERGENCY" to R.drawable.warning
            NotificationModel.TYPE_IMPORTANT -> "IMPORTANT" to R.drawable.notifications
            NotificationModel.TYPE_ADMIN_BROADCAST -> "ADMIN BROADCAST" to R.drawable.notifications
            NotificationModel.TYPE_ATTENDANCE, NotificationModel.TYPE_ATTENDANCE_REQUIRED -> "ATTENDANCE" to R.drawable.warning
            NotificationModel.TYPE_TRIP_CANCELLED -> "CANCELLED" to R.drawable.warning
            NotificationModel.TYPE_NEW_TRIP, NotificationModel.TYPE_TRIP_STARTED -> "TRIP" to R.drawable.bus
            NotificationModel.TYPE_ROUTE_UPDATE, NotificationModel.TYPE_STOP_UPDATE -> "ROUTE" to R.drawable.notifications
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
            holder.tvTag.text = item.type

            val (bgColor, textColor) = when (item.type.uppercase()) {
                "CRITICAL", "IMPORTANT", "EMERGENCY", "CANCELLED", "ATTENDANCE" -> Pair("#FEE2E2", "#EF4444")
                "ADMIN BROADCAST" -> Pair("#E0E7FF", "#4338CA") // indigo for admin broadcast
                "TRIP", "ROUTE" -> Pair("#DBEAFE", "#2563EB") // blue for trip/route
                else -> Pair("#D1FAE5", "#10B981")
            }

            holder.cvIconBg.setCardBackgroundColor(Color.parseColor(bgColor))
            holder.ivIcon.setImageResource(item.iconRes)
            holder.ivIcon.setColorFilter(Color.parseColor(textColor))
            holder.tvTag.setTextColor(Color.parseColor(textColor))
            holder.tvTag.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor(bgColor))

            holder.itemView.setOnClickListener { onItemClick(item) }
        }

        override fun getItemCount() = items.size
    }
}

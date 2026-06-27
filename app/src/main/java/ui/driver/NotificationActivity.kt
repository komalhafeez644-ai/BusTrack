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
import utils.ViewUtils

class NotificationActivity : AppCompatActivity() {

    private lateinit var rvNotifications: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.drivernotifications)

        window.statusBarColor = Color.parseColor("#051024")

        rvNotifications = findViewById(R.id.rvNotifications)
        
        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            ViewUtils.applyClickEffect(it)
            finish()
        }

        setupRecyclerView()
    }

    private fun setupRecyclerView() {
        val notifications = listOf(
            NotificationData("Route Update", "Diversion on Route-04 due to roadworks. Follow the updated map path.", "2m ago", "IMPORTANT", R.drawable.alt_route),
            NotificationData("Traffic Delay", "Heavy congestion at Sector G-9. Estimated +10 min delay to your schedule.", "15m ago", "CRITICAL", R.drawable.warning),
            NotificationData("Admin Announcement", "New fuel logs are required by end of shift today. Check the office desk.", "1h ago", "GENERAL", R.drawable.notifications),
            NotificationData("Attendance Alert", "3 students unmarked at Stop-12. Please verify manually.", "2h ago", "CRITICAL", R.drawable.warning),
            NotificationData("Shift Change", "Your shift for next Monday has been updated by the admin.", "3h ago", "IMPORTANT", R.drawable.time)
        )

        rvNotifications.layoutManager = LinearLayoutManager(this)
        rvNotifications.adapter = NotificationAdapter(notifications) { data ->
            val intent = Intent(this, DriverAlertDetailActivity::class.java)
            intent.putExtra("ALERT_TITLE", data.title)
            intent.putExtra("ALERT_MESSAGE", data.message)
            intent.putExtra("ALERT_TIME", data.time)
            intent.putExtra("ALERT_TYPE", data.type)
            intent.putExtra("ALERT_ICON", data.iconRes)
            startActivity(intent)
        }
    }

    data class NotificationData(
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

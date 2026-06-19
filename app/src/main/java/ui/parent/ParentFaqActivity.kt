package ui.parent

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
import com.example.bustrack_app.models.FaqModel

class ParentFaqActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_parent_faq)

        supportActionBar?.hide()

        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            utils.ViewUtils.applyClickEffect(it)
            finish()
        }

        setupFaqList()
    }

    private fun setupFaqList() {
        val faqs = listOf(
            FaqModel(
                "How do I track my bus in real-time?",
                "You can track your bus by navigating to the 'Dashboard' and clicking on 'Track Bus' button. A real-time map will show the current GPS location, estimated arrival time, and any traffic delays affecting the schedule."
            ),
            FaqModel(
                "What if the driver is running late?",
                "If the bus is significantly delayed, you will receive a push notification on your app. You can also monitor the live tracking to see the driver's current progress towards your stop."
            ),
            FaqModel(
                "How is student attendance verified?",
                "Our system uses RFID/Manual scanning. When your child boards or exits the bus, the driver marks the attendance, which is instantly updated in your 'Child Attendance' section."
            ),
            FaqModel(
                "Is my child's location data secure?",
                "Yes, we take privacy seriously. All location data is encrypted and only accessible by verified parents for their own children and authorized school transport administrators."
            ),
            FaqModel(
                "How can I update my pickup/drop-off point?",
                "To request a change in your stop location, please go to 'Settings' or contact the administrator directly through the support section. Changes usually take 24-48 hours to process."
            ),
            FaqModel(
                "Can I receive alerts for specific stops?",
                "Yes! You can configure proximity alerts in 'Notification Settings' to get notified when the bus is within 1km or 5 minutes of your stop."
            )
        )

        val recyclerView = findViewById<RecyclerView>(R.id.rvFaq)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = FaqAdapter(faqs)
    }

    class FaqAdapter(private val items: List<FaqModel>) :
        RecyclerView.Adapter<FaqAdapter.ViewHolder>() {

        private var expandedPosition = -1

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvQuestion: TextView = view.findViewById(R.id.tvQuestion)
            val tvAnswer: TextView = view.findViewById(R.id.tvAnswer)
            val ivExpand: ImageView = view.findViewById(R.id.ivExpand)
            val rlQuestion: View = view.findViewById(R.id.rlQuestion)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_parent_faq, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.tvQuestion.text = item.question
            holder.tvAnswer.text = item.answer

            val isExpanded = position == expandedPosition
            holder.tvAnswer.visibility = if (isExpanded) View.VISIBLE else View.GONE
            holder.ivExpand.rotation = if (isExpanded) 180f else 0f
            
            // Highlight question if expanded
            holder.tvQuestion.setTextColor(if (isExpanded) 0xFF1E3A8A.toInt() else 0xFF1E293B.toInt())

            holder.rlQuestion.setOnClickListener {
                val previousExpanded = expandedPosition
                expandedPosition = if (isExpanded) -1 else position
                
                notifyItemChanged(previousExpanded)
                notifyItemChanged(expandedPosition)
            }
        }

        override fun getItemCount() = items.size
    }
}

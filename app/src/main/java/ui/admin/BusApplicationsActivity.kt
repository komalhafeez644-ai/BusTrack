package ui.admin

import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.bustrack_app.R
import com.example.bustrack_app.adapter.ApplicationAdapter
import com.example.bustrack_app.models.ApplicationModel

class BusApplicationsActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ApplicationAdapter
    private lateinit var list: ArrayList<ApplicationModel>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bus_application)

        // Back Button click listener (taaki app back ho sakay)
        val btnBack = findViewById<ImageView>(R.id.btnBack)
        btnBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        recyclerView = findViewById(R.id.recyclerApplications)
        list = ArrayList()

        // Parameters order as per your Model:
        // (id, studentName, studentClass, pickupPoint, routeMatch, time, status, image)

        list.add(
            ApplicationModel(
                1,
                "Aryan Sharma",
                "Grade 10 • Section B",
                "Green Park Sector 4",
                "92% Coverage",
                "2 HRS AGO",
                "Pending",
                R.drawable.ic_person
            )
        )

        list.add(
            ApplicationModel(
                2,
                "Vanya Patel",
                "Grade 8 • Section A",
                "Sunrise Heights",
                "88% Coverage",
                "5 HRS AGO",
                "Pending",
                R.drawable.ic_person
            )
        )

        list.add(
            ApplicationModel(
                3,
                "Rohan Gupta",
                "Grade 12 • Section C",
                "Oakwood Avenue",
                "75% Coverage",
                "Yesterday",
                "Pending",
                R.drawable.ic_person
            )
        )

        adapter = ApplicationAdapter(list)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }
}
package ui.admin

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.bustrack_app.databinding.ActivityAddRouteBinding
import com.example.bustrack_app.models.LatLngModel
import com.example.bustrack_app.models.RouteModel
import com.example.bustrack_app.data.RouteRepository

class AddRouteActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddRouteBinding
    private var drawnPath: MutableList<LatLngModel> = mutableListOf()

    private val drawRouteLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val points = data?.getSerializableExtra("PATH_POINTS") as? ArrayList<LatLngModel>
            val startAddr = data?.getStringExtra("START_ADDRESS")
            val endAddr = data?.getStringExtra("END_ADDRESS")

            points?.let {
                drawnPath = it.toMutableList()
                binding.btnDrawRoute.text = "Path Selected (${drawnPath.size} points)"
                Toast.makeText(this, "Route path and addresses captured!", Toast.LENGTH_SHORT).show()
            }
            
            if (!startAddr.isNullOrEmpty()) {
                binding.etStartPoint.setText(startAddr)
            }
            if (!endAddr.isNullOrEmpty()) {
                binding.etEndPoint.setText(endAddr)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddRouteBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }

        binding.btnDrawRoute.setOnClickListener {
            val startLoc = binding.etStartPoint.text.toString().trim()
            val endLoc = binding.etEndPoint.text.toString().trim()

            val intent = Intent(this, DrawRouteActivity::class.java)
            if (startLoc.isNotEmpty()) intent.putExtra("MANUAL_START", startLoc)
            if (endLoc.isNotEmpty()) intent.putExtra("MANUAL_END", endLoc)

            drawRouteLauncher.launch(intent)
        }

        binding.btnSaveRoute.setOnClickListener {
            saveRoute()
        }
    }

    private fun saveRoute() {
        val name = binding.etRouteName.text.toString().trim()
        val code = binding.etRouteCode.text.toString().trim()
        val desc = binding.etDescription.text.toString().trim()
        val start = binding.etStartPoint.text.toString().trim()
        val end = binding.etEndPoint.text.toString().trim()

        if (name.isEmpty()) {
            Toast.makeText(this, "Please enter a Route Name", Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnSaveRoute.isEnabled = false
        binding.btnSaveRoute.text = "Saving..."

        // Generate a new unique ID using UUID for database consistency
        val newId = java.util.UUID.randomUUID().toString()

        val newRoute = RouteModel(
            id = newId,
            routeCode = code,
            routeName = name,
            description = desc,
            startPoint = start,
            endPoint = end,
            status = "ACTIVE",
            pathPoints = drawnPath,
            stopsList = mutableListOf()
        )

        // Standardized save via Firestore Repository
        RouteRepository.updateRoute(newRoute) { success ->
            if (success) {
                Toast.makeText(this, "Route '$name' saved to Cloud!", Toast.LENGTH_LONG).show()
                finish()
            } else {
                binding.btnSaveRoute.isEnabled = true
                binding.btnSaveRoute.text = "Save Route"
                Toast.makeText(this, "Failed to save route. Check connection.", Toast.LENGTH_LONG).show()
            }
        }
    }
}

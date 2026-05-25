package ui.admin

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.bustrack_app.databinding.ActivityRouteMapBinding

class RouteMapActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRouteMapBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRouteMapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnClose.setOnClickListener {
            finish()
        }

        // Note: Map initialization would go here if API key was available.
        // For now, it shows the simulated map layout.
    }
}

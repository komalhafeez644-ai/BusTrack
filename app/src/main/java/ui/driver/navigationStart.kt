package ui.driver

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.bustrack_app.databinding.StartnavigationBinding

class navigationStart : AppCompatActivity() {

    private lateinit var binding: StartnavigationBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = StartnavigationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnStartNav.setOnClickListener {
            // Sequence: Start Preview -> Live Navigation
            startActivity(Intent(this, navigationEnd::class.java))
            finish()
        }

        binding.btnMenu.setOnClickListener {
            finish() // Back to Dashboard
        }
    }
}

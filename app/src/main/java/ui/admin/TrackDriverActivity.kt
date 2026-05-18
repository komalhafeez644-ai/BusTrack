package ui.admin

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.NestedScrollView
import com.example.bustrack_app.R
import com.google.android.material.bottomsheet.BottomSheetBehavior

class TrackDriverActivity : AppCompatActivity() {

    private lateinit var bottomSheetBehavior: BottomSheetBehavior<NestedScrollView>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_track_driver)

        supportActionBar?.hide()

        // Initialize Bottom Sheet
        val bottomSheet = findViewById<NestedScrollView>(R.id.bottomSheet)
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)

        // Optional: Bottom Sheet state change listener
        bottomSheetBehavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                // Yahan aap state manage kar sakte hain (Expanded, Collapsed, etc.)
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                // Animation logic if needed
            }
        })

        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            onBackPressed()
        }
    }
}
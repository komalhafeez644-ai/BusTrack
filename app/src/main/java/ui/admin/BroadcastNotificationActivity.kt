package ui.admin

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.bustrack_app.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class BroadcastNotificationActivity : AppCompatActivity() {

    private lateinit var tilSelect: TextInputLayout
    private lateinit var autoSelect: AutoCompleteTextView
    private lateinit var etTitle: TextInputEditText
    private lateinit var etMessage: TextInputEditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_broadcast_notification)

        supportActionBar?.hide()

        tilSelect = findViewById(R.id.tilSelectTarget)
        autoSelect = findViewById(R.id.autoCompleteSelect)
        etTitle = findViewById(R.id.etTitle)
        etMessage = findViewById(R.id.etMessage)

        val btnBack = findViewById<ImageView>(R.id.btnBack)
        val toggleAudience = findViewById<MaterialButtonToggleGroup>(R.id.toggleGroupAudience)
        val rgLevel = findViewById<RadioGroup>(R.id.rgSelectionLevel)
        val btnSend = findViewById<MaterialButton>(R.id.btnSendBroadcast)

        btnBack.setOnClickListener {
            utils.ViewUtils.applyClickEffect(it)
            finish()
        }

        rgLevel.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.rbAll -> {
                    tilSelect.visibility = View.GONE
                }
                R.id.rbIndividual -> {
                    tilSelect.visibility = View.VISIBLE
                    tilSelect.hint = "Select Individual"
                    setupSelectionData(toggleAudience.checkedButtonId == R.id.btnTargetParents)
                }
                R.id.rbGroup -> {
                    tilSelect.visibility = View.VISIBLE
                    tilSelect.hint = "Select Group/Route"
                    setupSelectionData(toggleAudience.checkedButtonId == R.id.btnTargetParents, true)
                }
            }
        }

        toggleAudience.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked && rgLevel.checkedRadioButtonId != R.id.rbAll) {
                setupSelectionData(checkedId == R.id.btnTargetParents, rgLevel.checkedRadioButtonId == R.id.rbGroup)
            }
        }

        btnSend.setOnClickListener {
            utils.ViewUtils.applyClickEffect(it)
            val title = etTitle.text.toString().trim()
            val message = etMessage.text.toString().trim()

            if (title.isEmpty() || message.isEmpty()) {
                Toast.makeText(this, "Please fill in title and message", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Mock sending
            Toast.makeText(this, "Broadcast Sent Successfully!", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun setupSelectionData(isParents: Boolean, isGroup: Boolean = false) {
        val data = if (isGroup) {
            listOf("Route 42-B", "Route 12-A", "Sector 15 North", "Downtown Fleet")
        } else {
            if (isParents) {
                listOf("Ali Hassan (Parent)", "Zoya Khan (Parent)", "Elena Rodriguez (Parent)")
            } else {
                listOf("John Driver", "Ahmed Khan (Bus-08)", "Michael (Bus-01)")
            }
        }

        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, data)
        autoSelect.setAdapter(adapter)
    }
}

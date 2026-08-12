package ui.parent

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.*
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.example.bustrack_app.R
import com.example.bustrack_app.data.ParentRepository
import com.example.bustrack_app.data.StudentRepository
import com.example.bustrack_app.models.ParentModel
import com.example.bustrack_app.models.StudentModel
import com.example.bustrack_app.viewmodels.ProfileViewModel
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import kotlinx.coroutines.launch
import ui.admin.*
import ui_authentication.LoginActivity

class ParentDashboardActivity : AppCompatActivity() {

    private var mapView: MapView? = null
    private lateinit var drawerLayout: DrawerLayout
    private val profileViewModel: ProfileViewModel by viewModels()
    private val parentRepository = ParentRepository()
    private var myChild: StudentModel? = null
    
    private val busLocations = listOf(
        Point.fromLngLat(67.0011, 24.8607) to "Bus-01",
        Point.fromLngLat(67.0599, 24.8716) to "Bus-08"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_parent_dashboard)

        supportActionBar?.hide()
        drawerLayout = findViewById(R.id.drawerLayout)

        // Initialize Mapbox Map
        mapView = findViewById(R.id.mapView)
        mapView?.mapboxMap?.loadStyle(Style.MAPBOX_STREETS) {
            setupMapAnnotations()
        }

        setupUI()
        setupDrawerListeners()
        loadParentAndChildData()

        // Automatically show Info Bottom Sheet for UI Flow
        mapView?.postDelayed({
            showParentInfoBottomSheet()
        }, 1000)
        
        // Handle Drawer opening if coming back from shared screens
        if (intent.getBooleanExtra("OPEN_DRAWER", false)) {
            drawerLayout.openDrawer(GravityCompat.END)
        }
    }

    private fun showParentInfoBottomSheet() {
        val bottomSheetDialog = BottomSheetDialog(this, R.style.TransparentBottomSheetDialog)
        val view = layoutInflater.inflate(R.layout.layout_parent_info_bottom_sheet, null)
        bottomSheetDialog.setContentView(view)

        // Find views
        val etParentName = view.findViewById<EditText>(R.id.etParentName)
        val etCnic = view.findViewById<EditText>(R.id.etCnic)
        val etPhone = view.findViewById<EditText>(R.id.etPhone)
        val etChildName = view.findViewById<EditText>(R.id.etChildName)
        val etStudentId = view.findViewById<EditText>(R.id.etStudentId)
        val actvRelationship = view.findViewById<AutoCompleteTextView>(R.id.actvRelationship)
        
        val tilParentName = view.findViewById<TextInputLayout>(R.id.tilParentName)
        val tilCnic = view.findViewById<TextInputLayout>(R.id.tilCnic)
        val tilPhone = view.findViewById<TextInputLayout>(R.id.tilPhone)
        val tilChildName = view.findViewById<TextInputLayout>(R.id.tilChildName)
        val tilStudentId = view.findViewById<TextInputLayout>(R.id.tilStudentId)
        val tilRelationship = view.findViewById<TextInputLayout>(R.id.tilRelationship)

        // Dropdown setup
        val options = resources.getStringArray(R.array.relationship_options)
        val adapter = ArrayAdapter(this, R.layout.spinner_dropdown_item, options)
        actvRelationship.setAdapter(adapter)
        
        // Use a modern, elevated background for the dropdown popup
        actvRelationship.setDropDownBackgroundResource(R.drawable.bg_dropdown_popup)

        // Parent Name Formatting (Rajesh Sharma)
        etParentName.addTextChangedListener(object : TextWatcher {
            private var isUpdating = false
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (isUpdating || s.isNullOrEmpty()) return
                isUpdating = true
                val input = s.toString()
                val capitalized = input.split(" ").joinToString(" ") { word ->
                    word.lowercase().replaceFirstChar { it.uppercase() }
                }
                if (capitalized != input) {
                    val selection = etParentName.selectionStart
                    etParentName.setText(capitalized)
                    etParentName.setSelection(selection.coerceAtMost(capitalized.length))
                }
                isUpdating = false
            }
        })

        // Child Name Formatting (Rajesh Sharma)
        etChildName.addTextChangedListener(object : TextWatcher {
            private var isUpdating = false
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (isUpdating || s.isNullOrEmpty()) return
                isUpdating = true
                val input = s.toString()
                val capitalized = input.split(" ").joinToString(" ") { word ->
                    word.lowercase().replaceFirstChar { it.uppercase() }
                }
                if (capitalized != input) {
                    val selection = etChildName.selectionStart
                    etChildName.setText(capitalized)
                    etChildName.setSelection(selection.coerceAtMost(capitalized.length))
                }
                isUpdating = false
            }
        })

        // Student ID Formatting Logic (ABC-123)
        etStudentId.addTextChangedListener(object : TextWatcher {
            private var isUpdating = false
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (isUpdating || s == null) return
                isUpdating = true

                val original = s.toString().replace("-", "").uppercase()
                val letters = original.filter { it.isLetter() }.take(3)
                val numbers = original.substring(letters.length.coerceAtMost(original.length))
                    .filter { it.isDigit() }.take(3)

                val formatted = StringBuilder()
                formatted.append(letters)
                if (letters.length == 3) {
                    formatted.append("-")
                    formatted.append(numbers)
                }

                if (formatted.toString() != s.toString()) {
                    etStudentId.setText(formatted.toString())
                    etStudentId.setSelection(formatted.length)
                }
                isUpdating = false
            }
        })

        // CNIC Formatting Logic (XXXXX-XXXXXXX-X)
        etCnic.addTextChangedListener(object : TextWatcher {
            private var isUpdating = false
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (isUpdating || s == null) return
                isUpdating = true

                val digits = s.toString().replace("-", "").filter { it.isDigit() }.take(13)
                val formatted = StringBuilder()
                for (i in digits.indices) {
                    formatted.append(digits[i])
                    if ((i == 4 || i == 11) && i != digits.length - 1) {
                        formatted.append("-")
                    }
                }

                if (formatted.toString() != s.toString()) {
                    etCnic.setText(formatted.toString())
                    etCnic.setSelection(formatted.length)
                }
                isUpdating = false
            }
        })

        view.findViewById<View>(R.id.btnContinue).setOnClickListener {
            // Simple Validation
            var isValid = true

            if (etParentName.text.isNullOrEmpty()) {
                tilParentName.error = "Required"; isValid = false
            } else tilParentName.error = null

            if (etCnic.text.isNullOrEmpty() || etCnic.text.toString().replace("-", "").length != 13) {
                tilCnic.error = "Enter 13 digit CNIC"; isValid = false
            } else tilCnic.error = null

            if (etPhone.text.isNullOrEmpty() || etPhone.text?.length != 11) {
                tilPhone.error = "Enter 11 digit number"; isValid = false
            } else tilPhone.error = null

            if (etChildName.text.isNullOrEmpty()) {
                tilChildName.error = "Required"; isValid = false
            } else tilChildName.error = null

            if (etStudentId.text.isNullOrEmpty() || etStudentId.text.toString().length != 7) {
                tilStudentId.error = "Format: GCW-XXX"; isValid = false
            } else tilStudentId.error = null

            if (actvRelationship.text.isNullOrEmpty()) {
                tilRelationship.error = "Required"; isValid = false
            } else tilRelationship.error = null

            if (isValid) {
                utils.ViewUtils.applyClickEffect(it)
                
                val parentName = etParentName.text.toString()
                val cnic = etCnic.text.toString()
                val phone = etPhone.text.toString()
                val relationship = actvRelationship.text.toString()
                val studentId = etStudentId.text.toString()

                lifecycleScope.launch {
                    it.isEnabled = false
                    
                    val parentModel = ParentModel(
                        name = parentName,
                        cnic = cnic,
                        phone = phone,
                        relationship = relationship
                    )

                    val (saveProfileSuccess, profileError) = parentRepository.saveParentData(parentModel)
                    if (saveProfileSuccess) {
                        val (requestSuccess, requestError) = parentRepository.submitTrackingRequest(
                            studentId = studentId,
                            parentName = parentName,
                            phone = phone,
                            relationship = relationship
                        )
                        
                        if (requestSuccess) {
                            bottomSheetDialog.dismiss()
                            showRequestSubmittedDialog()
                        } else {
                            Toast.makeText(this@ParentDashboardActivity, "Request Error: $requestError", Toast.LENGTH_LONG).show()
                            it.isEnabled = true
                        }
                    } else {
                        Toast.makeText(this@ParentDashboardActivity, "Profile Error: $profileError", Toast.LENGTH_LONG).show()
                        it.isEnabled = true
                    }
                }
            }
        }

        bottomSheetDialog.show()
    }

    private fun showRequestSubmittedDialog() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_request_submitted)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        
        // Fix Popup Size
        val width = (resources.displayMetrics.widthPixels * 0.90).toInt()
        dialog.window?.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)

        dialog.setCancelable(false)

        dialog.findViewById<View>(R.id.btnDone).setOnClickListener {
            utils.ViewUtils.applyClickEffect(it)
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun setupMapAnnotations() {
        val annotationApi = mapView?.annotations
        val pointAnnotationManager = annotationApi?.createPointAnnotationManager()

        busLocations.forEach { (point, busId) ->
            val pointAnnotationOptions = PointAnnotationOptions()
                .withPoint(point)
                .withTextField(busId)
            pointAnnotationManager?.create(pointAnnotationOptions)
        }

        // Set initial camera
        if (busLocations.isNotEmpty()) {
            mapView?.mapboxMap?.setCamera(
                CameraOptions.Builder()
                    .center(busLocations[0].first)
                    .zoom(12.0)
                    .build()
            )
        }

        pointAnnotationManager?.addClickListener { annotation ->
            showChildCardForBus(annotation.textField ?: "")
            true
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra("OPEN_DRAWER", false)) {
            drawerLayout.openDrawer(GravityCompat.END)
        }
    }

    private fun setupUI() {
        findViewById<View>(R.id.btnMenuDrawer)?.setOnClickListener {
            utils.ViewUtils.applyClickEffect(it)
            drawerLayout.openDrawer(GravityCompat.END)
        }

        findViewById<View>(R.id.btnCloseCard)?.setOnClickListener {
            utils.ViewUtils.applyClickEffect(it)
            findViewById<View>(R.id.studentCard)?.visibility = View.GONE
        }

        findViewById<View>(R.id.btnTrackBus)?.setOnClickListener {
            utils.ViewUtils.applyClickEffect(it)
            val intent = Intent(this, ParentTrackingActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setupDrawerListeners() {
        findViewById<View>(R.id.drawerImgProfile)?.setOnClickListener {
            utils.ViewUtils.applyClickEffect(it)
            startActivity(Intent(this, ParentProfileActivity::class.java))
            drawerLayout.closeDrawer(GravityCompat.END)
        }
        findViewById<View>(R.id.drawerSettings)?.setOnClickListener {
            utils.ViewUtils.applyClickEffect(it)
            val intent = Intent(this, NotificationSettingsActivity::class.java)
            intent.putExtra("FROM_USER", "parent")
            startActivity(intent)
            drawerLayout.closeDrawer(GravityCompat.END)
        }
        findViewById<View>(R.id.drawerPreferences)?.setOnClickListener {
            utils.ViewUtils.applyClickEffect(it)
            val intent = Intent(this, PreferencesActivity::class.java)
            intent.putExtra("FROM_USER", "parent")
            startActivity(intent)
            drawerLayout.closeDrawer(GravityCompat.END)
        }
        findViewById<View>(R.id.drawerPrivacy)?.setOnClickListener {
            utils.ViewUtils.applyClickEffect(it)
            val intent = Intent(this, PrivacyPolicyActivityActivity::class.java)
            intent.putExtra("FROM_USER", "parent")
            startActivity(intent)
            drawerLayout.closeDrawer(GravityCompat.END)
        }
        findViewById<View>(R.id.drawerTerms)?.setOnClickListener {
            utils.ViewUtils.applyClickEffect(it)
            val intent = Intent(this, TermsConditionsActivity::class.java)
            intent.putExtra("FROM_USER", "parent")
            startActivity(intent)
            drawerLayout.closeDrawer(GravityCompat.END)
        }

        findViewById<View>(R.id.drawerAttendance)?.setOnClickListener {
            utils.ViewUtils.applyClickEffect(it)
            startActivity(Intent(this, StudentAttendanceActivity::class.java))
            drawerLayout.closeDrawer(GravityCompat.END)
        }
        findViewById<View>(R.id.drawerNotifications)?.setOnClickListener {
            utils.ViewUtils.applyClickEffect(it)
            startActivity(Intent(this, ParentNotificationsActivity::class.java))
            drawerLayout.closeDrawer(GravityCompat.END)
        }
        findViewById<View>(R.id.drawerFaq)?.setOnClickListener {
            utils.ViewUtils.applyClickEffect(it)
            startActivity(Intent(this, ParentFaqActivity::class.java))
            drawerLayout.closeDrawer(GravityCompat.END)
        }

        findViewById<View>(R.id.drawerChangePassword)?.setOnClickListener {
            utils.ViewUtils.applyClickEffect(it)
            startActivity(Intent(this, ChangePasswordActivity::class.java))
            drawerLayout.closeDrawer(GravityCompat.END)
        }

        findViewById<View>(R.id.drawerLogout)?.setOnClickListener {
            utils.ViewUtils.applyClickEffect(it)
            Firebase.auth.signOut()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun observeProfileData() {
        profileViewModel.adminData.observe(this) { user ->
            val parentName = user.fullName.ifEmpty { "Parent User" }
            val displayResult = if (parentName.contains("Admin", true)) "Parent User" else parentName
            
            findViewById<TextView>(R.id.tvParentName)?.text = displayResult
            
            // Update Drawer
            findViewById<TextView>(R.id.drawerName)?.text = displayResult
            findViewById<TextView>(R.id.drawerEmail)?.text = user.email
            
            val drawerImageView = findViewById<ImageView>(R.id.drawerImgProfile)
            if (user.profileImageUrl.isNotEmpty()) {
                Glide.with(this).load(user.profileImageUrl).placeholder(R.drawable.ic_person).circleCrop().into(drawerImageView)
            }
        }
    }

    private fun loadParentAndChildData() {
        observeProfileData()
        StudentRepository.studentList.observe(this) { students ->
            myChild = students.find { it.name.contains("Ali", true) || it.name.contains("Rohan", true) }
            myChild?.let { updateChildUI(it, "Bus-01") }
        }
    }

    private fun showChildCardForBus(busId: String) {
        myChild?.let { updateChildUI(it, busId) } ?: run {
            Toast.makeText(this, "No child info found", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun updateChildUI(child: StudentModel, busId: String) {
        val card = findViewById<View>(R.id.studentCard)
        findViewById<TextView>(R.id.tvStudentName)?.text = child.name
        findViewById<TextView>(R.id.tvStudentId)?.text = "Student ID: ${child.id}"
        findViewById<TextView>(R.id.tvBusRoute)?.text = "${child.busNo ?: busId} • ${child.route ?: "Route-01"}"
        
        if (child.profileImage != 0) {
            findViewById<ImageView>(R.id.ivStudent)?.setImageResource(child.profileImage)
        }
        card?.visibility = View.VISIBLE
    }

    override fun onStart() {
        super.onStart()
        mapView?.onStart()
    }

    override fun onStop() {
        super.onStop()
        mapView?.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView?.onDestroy()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
            drawerLayout.closeDrawer(GravityCompat.END)
        } else {
            super.onBackPressed()
        }
    }
}
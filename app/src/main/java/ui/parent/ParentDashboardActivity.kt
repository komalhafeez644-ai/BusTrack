package ui.parent

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.Editable
import com.mapbox.maps.plugin.animation.flyTo
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import com.example.bustrack_app.R
import com.example.bustrack_app.data.ParentRepository
import com.example.bustrack_app.data.StudentRepository
import com.example.bustrack_app.models.ParentModel
import com.example.bustrack_app.models.StudentModel
import com.example.bustrack_app.models.TrackingRequestModel
import com.example.bustrack_app.models.DriverModel
import com.example.bustrack_app.viewmodels.ProfileViewModel
import com.example.bustrack_app.viewmodels.LiveTrackingViewModel
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.animation.MapAnimationOptions
import com.mapbox.maps.plugin.animation.easeTo
import com.mapbox.maps.plugin.animation.flyTo
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.content.Context
import androidx.core.content.ContextCompat
import android.view.animation.LinearInterpolator
import android.animation.ValueAnimator
import com.google.firebase.firestore.ListenerRegistration
import com.example.bustrack_app.data.FirebaseRepository
import kotlinx.coroutines.launch
import utils.ViewUtils
import utils.FormUtils
import ui.admin.ChangePasswordActivity
import ui.admin.LiveTrackingActivity
import ui.admin.NotificationSettingsActivity
import ui.admin.PreferencesActivity
import ui.admin.PrivacyPolicyActivityActivity
import ui.admin.TermsConditionsActivity
import ui.admin.TrackDriverActivity
import ui_authentication.LoginActivity
import ui.parent.StudentAttendanceActivity

class ParentDashboardActivity : AppCompatActivity() {

    private var mapView: MapView? = null
    private lateinit var drawerLayout: DrawerLayout
    private val profileViewModel: ProfileViewModel by viewModels()
    private val liveTrackingViewModel: LiveTrackingViewModel by viewModels()
    private val parentRepository = ParentRepository()
    private var statusDialog: Dialog? = null
    private var unavailableDialog: Dialog? = null
    private var isUnavailablePopupDismissed = false
    private val studentListeners = mutableMapOf<String, ListenerRegistration>()
    private val driverMarkers = mutableMapOf<String, com.mapbox.maps.plugin.annotation.generated.PointAnnotation>()
    // Reused across every live-location update instead of being recreated each time
    // (see updateMapMarkers fix below) - creating a brand new annotation manager /
    // deleting+recreating every marker on every 1-3s Firestore update was the source of
    // the bus icon visibly flickering/blinking on the Parent dashboard map.
    private var driverPointAnnotationManager: com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager? = null

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
        mapView?.mapboxMap?.loadStyle(Style.MAPBOX_STREETS) { style ->
            // Add bus icon to map style
            bitmapFromDrawableRes(this@ParentDashboardActivity, R.drawable.ic_marker_bus)?.let {
                style.addImage("bus-icon", it)
            }
        }

        // START DATA STREAM IMMEDIATELY
        observeTrackingStatus()

        setupUI()
        setupDrawerListeners()
        loadProfileData()

        // Handle Drawer opening if coming back from shared screens
        if (intent.getBooleanExtra("OPEN_DRAWER", false)) {
            drawerLayout.openDrawer(GravityCompat.END)
        }
    }

    private fun observeTrackingStatus() {
        parentRepository.listenToAllTrackingRequests { requests ->
            if (requests.isEmpty()) {
                // No request yet, show info sheet after delay for new users
                statusDialog?.dismiss()
                statusDialog = null
                studentListeners.values.forEach { it.remove() }
                studentListeners.clear()

                mapView?.postDelayed({
                    if (requests.isEmpty()) showParentInfoBottomSheet()
                }, 2000)

                setupPlaceholderAnnotations()
                return@listenToAllTrackingRequests
            }

            val approvedRequests = requests.filter { it.status.uppercase() == "APPROVED" && it.trackingEnabled }

            if (approvedRequests.isNotEmpty()) {
                statusDialog?.dismiss()
                statusDialog = null

                // Hide search bar for parents
                findViewById<View>(R.id.searchContainer)?.visibility = View.GONE

                // Start listening to all approved students
                approvedRequests.forEach { request ->
                    if (!studentListeners.containsKey(request.studentId)) {
                        val listener = FirebaseRepository.listenToStudent(request.studentId) { student ->
                            if (student != null) {
                                liveTrackingViewModel.setAllowedRoute(student.route)
                            }
                        }
                        studentListeners[request.studentId] = listener
                    }
                }

                observeLiveTracking()
            } else {
                // No approved requests yet. Check if any are pending/rejected/disabled.
                val pendingRequest = requests.find { it.status.uppercase() == "PENDING" }
                val rejectedRequest = requests.find { it.status.uppercase() == "REJECTED" }
                val disabledRequest = requests.find { it.status.uppercase() == "APPROVED" && !it.trackingEnabled }

                when {
                    pendingRequest != null -> showBlockingStatusDialog("PENDING")
                    rejectedRequest != null -> showBlockingStatusDialog("REJECTED")
                    disabledRequest != null -> showBlockingStatusDialog("DISABLED")
                    else -> {
                        statusDialog?.dismiss()
                        statusDialog = null
                    }
                }
                setupPlaceholderAnnotations()
            }
        }
    }

    private fun observeLiveTracking() {
        liveTrackingViewModel.activeDrivers.observe(this) { drivers ->
            updateMapMarkers(drivers)
        }

        liveTrackingViewModel.trackingStatus.observe(this) { status ->
            when (status) {
                "NO_ROUTE" -> showUnavailableDialog("NO_ROUTE")
                "OFF_DUTY" -> showUnavailableDialog("OFF_DUTY")
                "AVAILABLE" -> {
                    unavailableDialog?.dismiss()
                    unavailableDialog = null
                    isUnavailablePopupDismissed = false // Reset for next time it goes off-duty
                }
            }
        }
    }

    private fun showUnavailableDialog(type: String) {
        if (unavailableDialog?.isShowing == true || isUnavailablePopupDismissed) return

        unavailableDialog = Dialog(this)
        unavailableDialog?.requestWindowFeature(Window.FEATURE_NO_TITLE)
        unavailableDialog?.setContentView(R.layout.dialog_request_submitted)
        unavailableDialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        unavailableDialog?.setCancelable(false) // Must click OK

        val tvTitle = unavailableDialog?.findViewById<TextView>(R.id.tvStatusTitle)
        val tvMsg = unavailableDialog?.findViewById<TextView>(R.id.tvStatusMessage)
        val tvFooter = unavailableDialog?.findViewById<TextView>(R.id.tvFooterStatus)
        val ivIcon = unavailableDialog?.findViewById<ImageView>(R.id.ivStatusIcon)
        val btnOk = unavailableDialog?.findViewById<Button>(R.id.btnOk)

        btnOk?.visibility = View.VISIBLE
        btnOk?.setOnClickListener {
            isUnavailablePopupDismissed = true
            unavailableDialog?.dismiss()
        }

        if (type == "NO_ROUTE") {
            tvTitle?.text = "No Route Assigned"
            tvMsg?.text = "Live tracking is unavailable because no route is currently assigned to your child."
            tvFooter?.text = "Contact Admin"
            ivIcon?.setImageResource(R.drawable.warning)
        } else {
            tvTitle?.text = "Live Tracking Unavailable"
            tvMsg?.text = "The assigned bus is currently not on duty. Live location will be available when the bus goes on duty."
            tvFooter?.text = "Bus Offline"
            ivIcon?.setImageResource(R.drawable.warning)
        }

        // Set width and height correctly
        val width = (resources.displayMetrics.widthPixels * 0.90).toInt()
        unavailableDialog?.window?.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)

        unavailableDialog?.show()
    }

    private fun updateMapMarkers(drivers: List<DriverModel>) {
        // Reuse one PointAnnotationManager for the whole activity lifetime instead of
        // creating a new one on every update (was happening every 1-3s on every live
        // location push, a real source of visible flicker on its own).
        val pointAnnotationManager = driverPointAnnotationManager ?: run {
            val manager = mapView?.annotations?.createPointAnnotationManager() ?: return
            driverPointAnnotationManager = manager
            // Click listener only needs to be attached once, when the manager is first created.
            manager.addClickListener { annotation ->
                val currentDrivers = liveTrackingViewModel.activeDrivers.value
                val clickedDriver = currentDrivers?.find {
                    it.assignedBus == annotation.textField || it.name == annotation.textField
                }
                clickedDriver?.let { updateDriverCard(it) }
                true
            }
            manager
        }

        // Update-in-place instead of deleteAll()+recreate every update: the previous
        // code wiped every marker and rebuilt them from scratch on every single
        // location push, which made the bus icon itself blink/flicker on screen (a
        // second, more visible source of the same "blinking" symptom, separate from the
        // camera flyTo() issue fixed in TrackDriverActivity/LiveTrackingActivity/
        // PrincipalDashboardActivity). Now: move existing markers, add only new ones,
        // remove only ones for drivers that dropped off.
        val currentDriverIds = drivers.map { it.driverId }.toSet()

        // Remove markers for drivers no longer present
        val idsToRemove = driverMarkers.keys - currentDriverIds
        idsToRemove.forEach { id ->
            driverMarkers[id]?.let { pointAnnotationManager.delete(it) }
            driverMarkers.remove(id)
        }

        drivers.forEach { driver ->
            val point = Point.fromLngLat(driver.longitude, driver.latitude)
            val existing = driverMarkers[driver.driverId]
            if (existing != null) {
                // Already on the map - just move it, no delete/recreate flicker.
                existing.point = point
                existing.textField = driver.assignedBus ?: driver.name
                pointAnnotationManager.update(existing)
            } else {
                val options = PointAnnotationOptions()
                    .withPoint(point)
                    .withIconImage("bus-icon")
                    .withIconSize(1.5)
                    .withTextField(driver.assignedBus ?: driver.name)
                    .withTextOffset(listOf(0.0, 2.0))
                    .withTextColor(Color.BLUE)
                    .withTextHaloColor(Color.WHITE)
                    .withTextHaloWidth(1.0)

                val annotation = pointAnnotationManager.create(options)
                driverMarkers[driver.driverId] = annotation
            }
        }

        // Smooth Camera flyTo (Only if card is not already visible)
        if (drivers.isNotEmpty() && findViewById<View>(R.id.driverCard)?.visibility == View.GONE) {
            val point = Point.fromLngLat(drivers[0].longitude, drivers[0].latitude)
            // Same fix as the other tracking screens: easeTo() instead of flyTo() so a
            // newer update interrupting the animation doesn't look like a blink.
            mapView?.mapboxMap?.easeTo(
                CameraOptions.Builder()
                    .center(point)
                    .zoom(14.0)
                    .build(),
                MapAnimationOptions.mapAnimationOptions { duration(800) }

            )
        }
    }

    private fun setupPlaceholderAnnotations() {
        val annotationApi = mapView?.annotations
        val pointAnnotationManager = annotationApi?.createPointAnnotationManager() ?: return
        pointAnnotationManager.deleteAll()

        busLocations.forEach { (point, busId) ->
            val options = PointAnnotationOptions()
                .withPoint(point)
                .withIconImage("bus-icon")
                .withIconSize(1.2)
                .withTextField(busId)
            pointAnnotationManager.create(options)
        }

        // Center camera on the first placeholder bus (Rawalpindi area) with smooth animation
        if (busLocations.isNotEmpty()) {
            mapView?.mapboxMap?.flyTo(
                CameraOptions.Builder()
                    .center(busLocations[0].first)
                    .zoom(12.0)
                    .build(),
                MapAnimationOptions.mapAnimationOptions { duration(1500) }
            )
        }
    }

    private fun updateDriverCard(driver: DriverModel) {
        val card = findViewById<View>(R.id.driverCard)
        if (card?.visibility == View.GONE) {
            card.visibility = View.VISIBLE
            card.alpha = 0f
            card.translationY = 100f
            card.animate().alpha(1f).translationY(0f).setDuration(500).start()
        }

        findViewById<TextView>(R.id.tvDriverName)?.text = driver.name
        findViewById<TextView>(R.id.tvBusRouteInfo)?.text = "Bus #${driver.assignedBus ?: "N/A"} • ${driver.route ?: "Route"}"
        findViewById<TextView>(R.id.tvRouteDetail)?.text = "Active Status: ${driver.status}"

        findViewById<TextView>(R.id.tvEta)?.text = driver.eta
        findViewById<TextView>(R.id.tvSpeed)?.text = "${driver.speed.toInt()} km/h"
        findViewById<TextView>(R.id.tvLoad)?.text = driver.load
    }

    private fun setupUI() {
        findViewById<View>(R.id.btnMenuDrawer)?.setOnClickListener {
            if (statusDialog != null && statusDialog!!.isShowing) return@setOnClickListener
            ViewUtils.applyClickEffect(it)
            drawerLayout.openDrawer(GravityCompat.END)
        }

        findViewById<View>(R.id.btnCloseCard)?.setOnClickListener {
            ViewUtils.applyClickEffect(it)
            findViewById<View>(R.id.driverCard)?.visibility = View.GONE
        }

        findViewById<MaterialButton>(R.id.btnTrackDriver)?.setOnClickListener {
            val drivers = liveTrackingViewModel.activeDrivers.value
            if (!drivers.isNullOrEmpty()) {
                val intent = Intent(this, TrackDriverActivity::class.java)
                intent.putExtra("DRIVER_ID", drivers[0].driverId)
                intent.putExtra("IS_PARENT", true)
                startActivity(intent)
            } else {
                Toast.makeText(this, "Bus is currently offline", Toast.LENGTH_SHORT).show()
            }
        }

        // Coming from ParentNotificationDetailActivity's "Track Live Location" action
        // (the dead ParentTrackingActivity used to handle this) - open tracking the same
        // way the button above does, as soon as the driver list has loaded. Guarded with
        // a one-shot flag since this observer stays registered for the activity's life and
        // would otherwise re-launch TrackDriverActivity on every later driver-list update.
        if (intent.getBooleanExtra("OPEN_TRACKING", false)) {
            var alreadyOpened = false
            liveTrackingViewModel.activeDrivers.observe(this) { drivers ->
                if (!alreadyOpened && !drivers.isNullOrEmpty()) {
                    alreadyOpened = true
                    val trackIntent = Intent(this, TrackDriverActivity::class.java)
                    trackIntent.putExtra("DRIVER_ID", drivers[0].driverId)
                    trackIntent.putExtra("IS_PARENT", true)
                    startActivity(trackIntent)
                }
            }
        }
    }

    private fun showBlockingStatusDialog(type: String) {
        if (statusDialog == null) {
            statusDialog = Dialog(this)
            statusDialog?.requestWindowFeature(Window.FEATURE_NO_TITLE)
            statusDialog?.setContentView(R.layout.dialog_request_submitted)
            statusDialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            statusDialog?.setCancelable(false)
            statusDialog?.setCanceledOnTouchOutside(false)
            val width = (resources.displayMetrics.widthPixels * 0.90).toInt()
            statusDialog?.window?.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
        }

        val tvTitle = statusDialog?.findViewById<TextView>(R.id.tvStatusTitle)
        val tvMsg = statusDialog?.findViewById<TextView>(R.id.tvStatusMessage)
        val tvFooter = statusDialog?.findViewById<TextView>(R.id.tvFooterStatus)
        val ivIcon = statusDialog?.findViewById<ImageView>(R.id.ivStatusIcon)

        when (type) {
            "PENDING" -> {
                tvTitle?.text = "Request Submitted!"
                tvMsg?.text = "Your tracking request has been submitted successfully. Please wait for Admin approval to enable live bus tracking."
                tvFooter?.text = "Verification in Progress"
                ivIcon?.setImageResource(R.drawable.check_circle)
                ivIcon?.imageTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#4CAF50"))
            }
            "REJECTED" -> {
                tvTitle?.text = "Tracking Request Rejected"
                tvMsg?.text = "Your tracking request has been rejected by the Admin. Live bus tracking cannot be enabled for your account."
                tvFooter?.text = "Tracking Request Rejected"
                ivIcon?.setImageResource(R.drawable.ic_close)
                ivIcon?.imageTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#D32F2F"))
            }
            "DISABLED" -> {
                tvTitle?.text = "Tracking Disabled"
                tvMsg?.text = "Tracking has been temporarily disabled by the Admin. Please contact the management for more details."
                tvFooter?.text = "Temporarily Disabled"
                ivIcon?.setImageResource(R.drawable.warning)
                ivIcon?.imageTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#FF9800"))
            }
        }
        if (statusDialog?.isShowing == false) statusDialog?.show()
    }

    private fun showParentInfoBottomSheet() {
        val bottomSheetDialog = BottomSheetDialog(this, R.style.TransparentBottomSheetDialog)
        val view = layoutInflater.inflate(R.layout.layout_parent_info_bottom_sheet, null)
        bottomSheetDialog.setContentView(view)
        bottomSheetDialog.setCancelable(false)
        bottomSheetDialog.setCanceledOnTouchOutside(false)
        bottomSheetDialog.behavior.apply {
            state = com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
            isDraggable = false
            skipCollapsed = true
        }

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

        val options = resources.getStringArray(R.array.relationship_options)
        actvRelationship.setAdapter(ArrayAdapter(this, R.layout.spinner_dropdown_item, options))
        actvRelationship.setDropDownBackgroundResource(R.drawable.bg_dropdown_popup)

        // RESTORE FORMATTING RULES
        FormUtils.setupCnicFormatting(etCnic)
        FormUtils.setupStudentIdFormatting(etStudentId)
        etPhone.filters = arrayOf(android.text.InputFilter.LengthFilter(11))

        etParentName.addTextChangedListener(object : TextWatcher {
            private var isUpdating = false
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (isUpdating || s.isNullOrEmpty()) return
                isUpdating = true
                val capitalized = s.toString().split(" ").joinToString(" ") { it.lowercase().replaceFirstChar { c -> c.uppercase() } }
                if (capitalized != s.toString()) {
                    val sel = etParentName.selectionStart
                    etParentName.setText(capitalized)
                    etParentName.setSelection(sel.coerceAtMost(capitalized.length))
                }
                isUpdating = false
            }
        })

        view.findViewById<View>(R.id.btnContinue).setOnClickListener {
            ViewUtils.applyPressEffect(it)
            var isValid = true
            if (etParentName.text.isNullOrEmpty()) { tilParentName.error = "Required"; isValid = false } else tilParentName.error = null

            val cleanCnic = etCnic.text.toString().replace("-", "")
            if (cleanCnic.length != 13) { tilCnic.error = "Enter 13 digit CNIC"; isValid = false } else tilCnic.error = null

            if (etPhone.text.isNullOrEmpty() || etPhone.text?.length != 11) { tilPhone.error = "Enter 11 digit number"; isValid = false } else tilPhone.error = null
            if (etChildName.text.isNullOrEmpty()) { tilChildName.error = "Required"; isValid = false } else tilChildName.error = null

            val studentId = etStudentId.text.toString()
            if (studentId.isEmpty() || !studentId.contains("-") || studentId.length < 5) { tilStudentId.error = "Format: GCW-XXX"; isValid = false } else tilStudentId.error = null

            if (actvRelationship.text.isNullOrEmpty()) { tilRelationship.error = "Required"; isValid = false } else tilRelationship.error = null

            if (isValid) {
                lifecycleScope.launch {
                    it.isEnabled = false
                    val parentModel = ParentModel(name = etParentName.text.toString(), cnic = etCnic.text.toString(), phone = etPhone.text.toString(), relationship = actvRelationship.text.toString())
                    val (saveProfileSuccess, _) = parentRepository.saveParentData(parentModel)
                    if (saveProfileSuccess) {
                        val (requestSuccess, _) = parentRepository.submitTrackingRequest(studentId = etStudentId.text.toString(), parentName = etParentName.text.toString(), phone = etPhone.text.toString(), relationship = actvRelationship.text.toString())
                        if (requestSuccess) bottomSheetDialog.dismiss() else it.isEnabled = true
                    } else it.isEnabled = true
                }
            }
        }
        bottomSheetDialog.show()
    }

    private fun setupDrawerListeners() {
        findViewById<View>(R.id.drawerImgProfile)?.setOnClickListener { ViewUtils.applyClickEffect(it); startActivity(Intent(this, ParentProfileActivity::class.java)); drawerLayout.closeDrawer(GravityCompat.END) }
        findViewById<View>(R.id.drawerPrivacy)?.setOnClickListener { ViewUtils.applyClickEffect(it); val intent = Intent(this, PrivacyPolicyActivityActivity::class.java); intent.putExtra("FROM_USER", "parent"); startActivity(intent); drawerLayout.closeDrawer(GravityCompat.END) }
        findViewById<View>(R.id.drawerTerms)?.setOnClickListener { ViewUtils.applyClickEffect(it); val intent = Intent(this, TermsConditionsActivity::class.java); intent.putExtra("FROM_USER", "parent"); startActivity(intent); drawerLayout.closeDrawer(GravityCompat.END) }
        findViewById<View>(R.id.drawerAttendance)?.setOnClickListener { ViewUtils.applyClickEffect(it); startActivity(Intent(this, StudentAttendanceActivity::class.java)); drawerLayout.closeDrawer(GravityCompat.END) }
        findViewById<View>(R.id.drawerNotifications)?.setOnClickListener { ViewUtils.applyClickEffect(it); startActivity(Intent(this, ParentNotificationsActivity::class.java)); drawerLayout.closeDrawer(GravityCompat.END) }
        findViewById<View>(R.id.drawerFaq)?.setOnClickListener { ViewUtils.applyClickEffect(it); startActivity(Intent(this, ParentFaqActivity::class.java)); drawerLayout.closeDrawer(GravityCompat.END) }
        findViewById<View>(R.id.drawerChangePassword)?.setOnClickListener { ViewUtils.applyClickEffect(it); startActivity(Intent(this, ChangePasswordActivity::class.java)); drawerLayout.closeDrawer(GravityCompat.END) }
        findViewById<View>(R.id.drawerLogout)?.setOnClickListener { ViewUtils.applyClickEffect(it); Firebase.auth.signOut(); val intent = Intent(this, LoginActivity::class.java); intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK; startActivity(intent); finish() }
    }

    private fun loadProfileData() {
        profileViewModel.adminData.observe(this) { user ->
            val parentName = user.fullName.ifEmpty { "Parent User" }
            val displayResult = if (parentName.contains("Admin", true)) "Parent User" else parentName
            findViewById<TextView>(R.id.tvParentName)?.text = displayResult
            findViewById<TextView>(R.id.drawerName)?.text = displayResult
            findViewById<TextView>(R.id.drawerEmail)?.text = user.email
            val drawerImageView = findViewById<ImageView>(R.id.drawerImgProfile)
            if (user.profileImageUrl.isNotEmpty()) Glide.with(this).load(user.profileImageUrl).placeholder(R.drawable.ic_person).circleCrop().into(drawerImageView)
        }
    }

    private fun bitmapFromDrawableRes(context: Context, resourceId: Int): Bitmap? {
        val drawable = ContextCompat.getDrawable(context, resourceId)
        if (drawable is BitmapDrawable) return drawable.bitmap
        if (drawable != null) {
            val bitmap = Bitmap.createBitmap(drawable.intrinsicWidth.takeIf { it > 0 } ?: 64, drawable.intrinsicHeight.takeIf { it > 0 } ?: 64, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            return bitmap
        }
        return null
    }

    override fun onStart() { super.onStart(); mapView?.onStart() }
    override fun onStop() { super.onStop(); mapView?.onStop() }
    override fun onDestroy() {
        super.onDestroy()
        studentListeners.values.forEach { it.remove() }
        studentListeners.clear()
        mapView?.onDestroy()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (statusDialog != null && statusDialog!!.isShowing) return
        if (drawerLayout.isDrawerOpen(GravityCompat.END)) drawerLayout.closeDrawer(GravityCompat.END) else super.onBackPressed()
    }
}

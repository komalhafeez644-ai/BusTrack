package ui.parent

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.example.bustrack_app.R
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import ui_authentication.LoginActivity

class ParentTrackingRequestActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var drawerLayout: DrawerLayout
    private var mMap: GoogleMap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_parent_tracking_request)

        supportActionBar?.hide()
        drawerLayout = findViewById(R.id.drawerLayout)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as? SupportMapFragment
        mapFragment?.getMapAsync(this)

        setupUI()
        setupDrawerListeners()
        loadParentData()
    }

    private fun setupUI() {
        findViewById<Button>(R.id.btnRequestTracking).setOnClickListener {
            showEnableTrackingDialog()
        }

        findViewById<View>(R.id.btnMenuDrawer).setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.END)
        }
    }

    private fun setupDrawerListeners() {
        // Options that should work
        findViewById<View>(R.id.drawerImgProfile)?.setOnClickListener {
            startActivity(Intent(this, ParentProfileActivity::class.java))
            drawerLayout.closeDrawer(GravityCompat.END)
        }
        findViewById<View>(R.id.drawerPrivacy)?.setOnClickListener {
            val intent = Intent(this, ui.admin.PrivacyPolicyActivityActivity::class.java)
            intent.putExtra("FROM_USER", "parent")
            startActivity(intent)
            drawerLayout.closeDrawer(GravityCompat.END)
        }
        findViewById<View>(R.id.drawerTerms)?.setOnClickListener {
            val intent = Intent(this, ui.admin.TermsConditionsActivity::class.java)
            intent.putExtra("FROM_USER", "parent")
            startActivity(intent)
            drawerLayout.closeDrawer(GravityCompat.END)
        }
        findViewById<View>(R.id.drawerLogout)?.setOnClickListener {
            Firebase.auth.signOut()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        // Options that are LOCKED (Tracking required)
        val lockMessage = "Please enable tracking first to access this feature"
        
        findViewById<View>(R.id.drawerAttendance)?.apply {
            alpha = 0.5f
            setOnClickListener { Toast.makeText(this@ParentTrackingRequestActivity, lockMessage, Toast.LENGTH_SHORT).show() }
        }
        findViewById<View>(R.id.drawerNotifications)?.apply {
            alpha = 0.5f
            setOnClickListener { Toast.makeText(this@ParentTrackingRequestActivity, lockMessage, Toast.LENGTH_SHORT).show() }
        }
        findViewById<View>(R.id.drawerSettings)?.apply {
            alpha = 0.5f
            setOnClickListener { Toast.makeText(this@ParentTrackingRequestActivity, lockMessage, Toast.LENGTH_SHORT).show() }
        }
        findViewById<View>(R.id.drawerPreferences)?.apply {
            alpha = 0.5f
            setOnClickListener { Toast.makeText(this@ParentTrackingRequestActivity, lockMessage, Toast.LENGTH_SHORT).show() }
        }
    }

    private fun loadParentData() {
        val user = Firebase.auth.currentUser
        if (user != null) {
            Firebase.firestore.collection("users").document(user.uid).get()
                .addOnSuccessListener { doc ->
                    if (doc.exists()) {
                        findViewById<TextView>(R.id.tvParentName)?.text = doc.getString("fullName") ?: "Parent User"
                    }
                }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        val defaultLoc = LatLng(24.8607, 67.0011)
        mMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLoc, 12f))
    }

    private fun showEnableTrackingDialog() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_enable_tracking)
        
        dialog.window?.let {
            it.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            it.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }

        val spinnerRelationship = dialog.findViewById<Spinner>(R.id.spinnerRelationship)
        val relationships = arrayOf("Father", "Mother", "Guardian", "Other")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, relationships)
        spinnerRelationship.adapter = adapter

        dialog.findViewById<Button>(R.id.btnSubmitRequest).setOnClickListener {
            dialog.dismiss()
            showConfirmationDialog()
        }

        dialog.findViewById<Button>(R.id.btnCancel).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showConfirmationDialog() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_request_submitted)

        dialog.window?.let {
            it.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            it.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }

        dialog.findViewById<Button>(R.id.btnDone).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
            drawerLayout.closeDrawer(GravityCompat.END)
        } else {
            super.onBackPressed()
        }
    }
}

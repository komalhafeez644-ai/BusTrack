package com.example.bustrack_app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback

class MainActivity : AppCompatActivity() {

    private val IMAGE_PICK_CODE = 1001
    private var imageUri: Uri? = null

    private lateinit var imageView: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // ImageView init
        imageView = findViewById(R.id.imageView)

        // Cloudinary init happens once in MyApp.onCreate() (the app's registered
        // Application class - see AndroidManifest.xml android:name=".MyApp") before any
        // Activity is created. Calling MediaManager.init() again here would throw
        // IllegalStateException("MediaManager is already initialized"), so it's
        // intentionally NOT re-initialized in this Activity.

        // Buttons connect
        findViewById<Button>(R.id.btnPick).setOnClickListener {
            pickImage()
        }

        findViewById<Button>(R.id.btnUpload).setOnClickListener {
            uploadImage()
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    // 📷 Pick image
    private fun pickImage() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, IMAGE_PICK_CODE)
    }

    // 📥 Get image + show preview
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == IMAGE_PICK_CODE && resultCode == RESULT_OK) {
            imageUri = data?.data

            // ✅ Show image in app
            imageView.setImageURI(imageUri)
        }
    }

    // ☁️ Upload image
    private fun uploadImage() {

        if (imageUri == null) {
            Toast.makeText(this, "Please select image first", Toast.LENGTH_SHORT).show()
            return
        }

        MediaManager.get().upload(imageUri!!)
            .callback(object : UploadCallback {

                override fun onStart(requestId: String) {
                    Toast.makeText(this@MainActivity, "Uploading...", Toast.LENGTH_SHORT).show()
                }

                override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {}

                override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                    val url = resultData["secure_url"].toString()

                    Toast.makeText(this@MainActivity, "Upload Success!", Toast.LENGTH_SHORT).show()

                    android.util.Log.d("UPLOAD_URL", url)
                }

                override fun onError(requestId: String, error: ErrorInfo) {
                    Toast.makeText(this@MainActivity, "Upload Failed!", Toast.LENGTH_SHORT).show()

                    android.util.Log.e("UPLOAD_ERROR", error.description)
                }

                override fun onReschedule(requestId: String, error: ErrorInfo) {}
            })
            .dispatch()
    }
}
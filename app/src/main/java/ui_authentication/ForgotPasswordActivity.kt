package com.example.bustrack_app.login

import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import com.example.bustrack_app.R

class ForgotPasswordActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        val btnBack = findViewById<ImageView>(R.id.btnBack)
        val layoutBackToLogin = findViewById<LinearLayout>(R.id.layoutBackToLogin)

        btnBack.setOnClickListener {
            finish()
        }

        layoutBackToLogin.setOnClickListener {
            finish()
        }
    }
}

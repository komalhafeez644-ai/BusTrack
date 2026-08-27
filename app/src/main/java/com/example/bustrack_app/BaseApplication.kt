package com.example.bustrack_app

import android.app.Application
import com.google.firebase.FirebaseApp

class BaseApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Firebase
        FirebaseApp.initializeApp(this)
    }
}

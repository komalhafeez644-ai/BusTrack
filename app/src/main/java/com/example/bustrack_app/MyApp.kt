package com.example.bustrack_app

import android.app.Application
import com.cloudinary.android.MediaManager

class MyApp : Application() {

    override fun onCreate() {
        super.onCreate()

        val config = HashMap<String, String>()
        config["cloud_name"] = "zhi36daa"
        // Credentials moved to zhi36daa per requirements. 
        // We use unsigned upload with preset 'BusTrack'.

        MediaManager.init(this, config)
    }
}
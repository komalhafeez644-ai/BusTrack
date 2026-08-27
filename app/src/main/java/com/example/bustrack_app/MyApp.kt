package com.example.bustrack_app

import android.app.Application
import com.cloudinary.android.MediaManager

class MyApp : Application() {

    override fun onCreate() {
        super.onCreate()

        val config = HashMap<String, String>()
        config["cloud_name"] = "vomxnqzr"
        config["upload_preset"] = "bus_track_images"

        MediaManager.init(this, config)
    }
}
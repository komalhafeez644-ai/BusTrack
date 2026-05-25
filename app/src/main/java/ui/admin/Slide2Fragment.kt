package com.example.bustrack_app.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.bustrack_app.R

class Slide2Fragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Yeh line aapki Slide 2 ki XML file ko background se link karti hai
        return inflater.inflate(R.layout.fragment_intro_slide2, container, false)
    }
}
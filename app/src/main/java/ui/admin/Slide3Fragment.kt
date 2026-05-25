package com.example.bustrack_app.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.bustrack_app.R

class Slide3Fragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Yeh line Slide 3 ki layout design ko load karegi
        return inflater.inflate(R.layout.fragment_intro_slide3, container, false)
    }
}
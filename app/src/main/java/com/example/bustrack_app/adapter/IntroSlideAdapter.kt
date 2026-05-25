package com.example.bustrack_app.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.bustrack_app.ui.admin.Slide1Fragment
import com.example.bustrack_app.ui.admin.Slide2Fragment
import com.example.bustrack_app.ui.admin.Slide3Fragment

class IntroSlideAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {

    private val fragmentsList = listOf(Slide1Fragment(), Slide2Fragment(), Slide3Fragment())

    override fun getItemCount(): Int = fragmentsList.size

    override fun createFragment(position: Int): Fragment = fragmentsList[position]
}
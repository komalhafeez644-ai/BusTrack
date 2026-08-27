package com.example.bustrack_app.models

data class ThemeOption(
    val id: Int,
    val title: String,
    val description: String,
    val icon: Int,
    var isSelected: Boolean = false
)
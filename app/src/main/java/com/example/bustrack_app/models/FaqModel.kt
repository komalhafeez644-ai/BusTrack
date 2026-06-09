package com.example.bustrack_app.models

data class FaqModel(
    val question: String,
    val answer: String,
    var isExpanded: Boolean = false
)
package com.example.bustrack_app.models

import com.google.firebase.Timestamp

data class ParentModel(
    val parentId: String = "",
    val name: String = "",
    val cnic: String = "",
    val phone: String = "",
    val relationship: String = "",
    val updatedAt: Timestamp? = null
)

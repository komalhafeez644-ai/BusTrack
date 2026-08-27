package com.example.bustrack_app.models
data class EditAssignmentModel(
    val studentName: String,
    val studentId: String,
    val studentYear: String,
    val status: String,
    val currentBus: String,
    val currentStop: String,
    val currentArrivalTime: String
)
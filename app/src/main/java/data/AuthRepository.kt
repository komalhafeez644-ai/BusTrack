package com.example.bustrack_app.data

import kotlinx.coroutines.delay

class AuthRepository {

    suspend fun login(email: String, password: String): Boolean {

        delay(2000)

        return email == "admin@university.edu" && password == "123456"
    }
}
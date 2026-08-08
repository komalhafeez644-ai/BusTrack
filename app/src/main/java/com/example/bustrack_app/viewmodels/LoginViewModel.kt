package com.example.bustrack_app.viewmodels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bustrack_app.data.AuthRepository
import com.example.bustrack_app.utils.Resource
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

class LoginViewModel : ViewModel() {

    private val repository = AuthRepository()

    val loginState = MutableLiveData<Resource<String>>()

    fun login(email: String, password: String) {
        viewModelScope.launch {
            loginState.value = Resource.Loading()
            val result = repository.login(email, password)
            val successRoles = listOf("admin", "principal", "driver", "parent", "user")
            if (result in successRoles) {
                loginState.value = Resource.Success(result!!)
            } else {
                loginState.value = Resource.Error(result ?: "Invalid Email or Password")
            }
        }
    }

    fun loginWithGoogle() {
        viewModelScope.launch {
            loginState.value = Resource.Loading()
            
            // Handle Firestore entry for Google login
            val user = Firebase.auth.currentUser
            if (user != null) {
                val db = Firebase.firestore
                val userData = mapOf(
                    "email" to user.email,
                    "fullName" to (user.displayName ?: "Parent Name"),
                    "role" to "user"
                )
                db.collection("users").document(user.uid).set(userData, com.google.firebase.firestore.SetOptions.merge())
            }
            
            loginState.value = Resource.Success("user") 
        }
    }
}
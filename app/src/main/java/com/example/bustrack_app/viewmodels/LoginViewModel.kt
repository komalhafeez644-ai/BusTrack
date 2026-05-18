package com.example.bustrack_app.viewmodels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bustrack_app.data.AuthRepository
import com.example.bustrack_app.utils.Resource
import kotlinx.coroutines.launch

class LoginViewModel : ViewModel() {

    private val repository = AuthRepository()

    val loginState = MutableLiveData<Resource<Boolean>>()

    fun login(email: String, password: String) {
        viewModelScope.launch {
            loginState.value = Resource.Loading()
            val result = repository.login(email, password)
            if (result) {
                loginState.value = Resource.Success(true)
            } else {
                loginState.value = Resource.Error("Invalid Email or Password")
            }
        }
    }
}
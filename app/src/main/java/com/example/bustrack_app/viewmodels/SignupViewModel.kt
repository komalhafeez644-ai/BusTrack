package com.example.bustrack_app.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bustrack_app.data.AuthRepository
import com.example.bustrack_app.utils.Resource
import kotlinx.coroutines.launch

class SignupViewModel : ViewModel() {

    private val repository = AuthRepository()

    private val _signupState = MutableLiveData<Resource<Unit>>()
    val signupState: LiveData<Resource<Unit>> = _signupState

    fun register(email: String, pass: String) {
        viewModelScope.launch {
            _signupState.value = Resource.Loading()
            val result = repository.register(email, pass)
            result.onSuccess {
                _signupState.value = Resource.Success(Unit)
            }.onFailure { error ->
                _signupState.value = Resource.Error(error.localizedMessage ?: "Registration failed")
            }
        }
    }
}
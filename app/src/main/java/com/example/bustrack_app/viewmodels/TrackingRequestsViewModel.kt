package com.example.bustrack_app.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.bustrack_app.data.FirebaseRepository
import com.example.bustrack_app.models.TrackingRequestModel

class TrackingRequestsViewModel : ViewModel() {

    private val _requests = MutableLiveData<List<TrackingRequestModel>>()
    val requests: LiveData<List<TrackingRequestModel>> get() = _requests

    fun loadRequests() {
        FirebaseRepository.fetchTrackingRequests {
            _requests.postValue(it)
        }
    }
}

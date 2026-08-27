package com.example.bustrack_app.viewmodels
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.bustrack_app.models.PrivacyPolicyModel
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.bustrack_app.R

class PrivacyPolicyViewModel(application: Application) : AndroidViewModel(application) {

    private val _policyList = MutableLiveData<List<PrivacyPolicyModel>>()
    val policyList: LiveData<List<PrivacyPolicyModel>> get() = _policyList

    init {
        val context = application.applicationContext
        _policyList.value = listOf(
            PrivacyPolicyModel(
                context.getString(R.string.student_parents_title),
                context.getString(R.string.student_desc)
            ),
            PrivacyPolicyModel(
                context.getString(R.string.drivers_title),
                context.getString(R.string.drivers_desc)
            ),
            PrivacyPolicyModel(
                context.getString(R.string.system_admin_title),
                context.getString(R.string.admin_desc)
            )
        )
    }
}
package com.example.bustrack_app.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.bustrack_app.R
import com.example.bustrack_app.models.ThemeOption

class ThemeViewModel : ViewModel() {

    private val _themes = MutableLiveData<List<ThemeOption>>()
    val themes: LiveData<List<ThemeOption>> = _themes

    init {
        loadThemes()
    }

    private fun loadThemes() {
        _themes.value = listOf(
            ThemeOption(
                id = 1,
                title = "Light Mode",
                description = "Bright and clean UI",
                icon = R.drawable.baseline_light_mode_24,
                isSelected = true
            ),
            ThemeOption(
                id = 2,
                title = "Dark Mode",
                description = "Reduce eye strain",
                icon = R.drawable.outline_dark_mode_24,
                isSelected = false
            )
        )
    }

    fun selectTheme(id: Int) {
        _themes.value = _themes.value?.map {
            it.copy(isSelected = it.id == id)
        }
    }
}
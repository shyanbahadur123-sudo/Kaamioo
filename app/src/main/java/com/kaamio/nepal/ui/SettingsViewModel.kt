package com.kaamio.nepal.ui

import androidx.lifecycle.viewModelScope
import com.kaamio.nepal.data.KaamioDatabase
import com.kaamio.nepal.data.PreferenceManager
import com.kaamio.nepal.data.UserProfile
import com.kaamio.nepal.data.repository.IUserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userRepository: IUserRepository,
    private val preferenceManager: PreferenceManager,
    private val database: KaamioDatabase
) : BaseViewModel() {

    val isDarkMode: StateFlow<Boolean> = preferenceManager.isDarkMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val currentLanguage: StateFlow<String> = preferenceManager.language
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "en")

    private val _isClearingCache = MutableStateFlow(false)
    val isClearingCache = _isClearingCache.asStateFlow()

    fun setDarkMode(enabled: Boolean) {
        viewModelScope.launch { preferenceManager.setDarkMode(enabled) }
    }

    fun setLanguage(lang: String) {
        viewModelScope.launch { preferenceManager.setLanguage(lang) }
    }

    fun updateSettingsPreference(key: String, value: Boolean, currentProfile: UserProfile) {
        viewModelScope.launch {
            val updates = mapOf(key to value)
            userRepository.updateProfile(updates)
        }
    }

    fun clearLocalCache() {
        viewModelScope.launch {
            _isClearingCache.value = true
            try {
                withContext(Dispatchers.IO) { KaamioDatabase.clearCacheData(database) }
                showSnackbar("Local cache cleared. Pull to refresh to reload.")
            } catch (_: Exception) {
                showSnackbar("Could not clear local cache.")
            } finally {
                _isClearingCache.value = false
            }
        }
    }
}

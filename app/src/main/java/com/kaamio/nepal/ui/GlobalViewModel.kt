package com.kaamio.nepal.ui

import androidx.lifecycle.viewModelScope
import com.kaamio.nepal.data.ConnectivityObserver
import com.kaamio.nepal.data.repository.IUserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GlobalViewModel @Inject constructor(
    private val userRepository: IUserRepository,
    private val connectivityObserver: ConnectivityObserver
) : BaseViewModel() {

    val isOffline: StateFlow<Boolean> = connectivityObserver.isOffline
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun logout() {
        viewModelScope.launch {
            try {
                userRepository.logout()
            } catch (_: Exception) {}
        }
    }
}
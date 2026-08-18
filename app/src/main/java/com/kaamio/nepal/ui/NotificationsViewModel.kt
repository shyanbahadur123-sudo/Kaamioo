package com.kaamio.nepal.ui

import androidx.lifecycle.viewModelScope
import com.kaamio.nepal.data.NotificationItem
import com.kaamio.nepal.data.repository.INotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val notificationRepository: INotificationRepository
) : BaseViewModel() {

    val notifications: StateFlow<List<NotificationItem>> = notificationRepository.notifications
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                notificationRepository.refresh()
            } catch (_: Exception) {}
            _isLoading.value = false
        }
    }

    fun markRead(id: String) {
        viewModelScope.launch {
            notificationRepository.markRead(id)
        }
    }
}
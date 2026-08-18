package com.kaamio.nepal.ui

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

abstract class BaseViewModel : ViewModel() {
    protected val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage = _snackbarMessage.asStateFlow()

    fun clearSnackbar() { _snackbarMessage.value = null }
    fun showSnackbar(message: String) {
        _snackbarMessage.value = message
        SnackbarBroker.post(message)
    }
}

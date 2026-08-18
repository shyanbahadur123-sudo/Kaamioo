package com.kaamio.nepal.ui

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Process-wide channel used to surface transient toast messages from any
 * [BaseViewModel] to the single SnackbarHost hosted in MainActivity.
 */
object SnackbarBroker {
    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages = _messages.asSharedFlow()

    fun post(message: String) {
        _messages.tryEmit(message)
    }
}

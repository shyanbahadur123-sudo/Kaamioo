package com.kaamio.nepal.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConnectivityObserver @Inject constructor(
    @ApplicationContext private val appContext: Context
) {

    private val _isOffline = MutableStateFlow(false)
    val isOffline: StateFlow<Boolean> = _isOffline.asStateFlow()

    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    fun start() {
        val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return
        // Guard against duplicate registration (e.g. ViewModel re-creation on
        // configuration change leaking a second NetworkCallback).
        if (networkCallback != null) return
        _isOffline.value = !isOnline(cm)
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                setOffline(false)
            }

            override fun onLost(network: Network) {
                setOffline(true)
            }

            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                val online = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                setOffline(!online)
            }
        }
        networkCallback = callback
        try {
            cm.registerDefaultNetworkCallback(callback)
        } catch (_: Exception) {}
    }

    private fun isOnline(cm: ConnectivityManager): Boolean {
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    fun stop() {
        val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return
        val callback = networkCallback ?: return
        try {
            cm.unregisterNetworkCallback(callback)
        } catch (_: Exception) {}
        networkCallback = null
    }

    fun setOffline(offline: Boolean) {
        if (_isOffline.value != offline) {
            _isOffline.value = offline
        }
    }

    val onConnectivityChange: (Boolean) -> Unit = { offline ->
        setOffline(offline)
    }
}

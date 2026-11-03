package com.example

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.util.NetworkObserver
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        const val TARGET_WEBSITE_URL = "https://ai-website-xi-lac.vercel.app/"
    }

    private val networkObserver = NetworkObserver(application)

    private val _isOnline = MutableStateFlow(networkObserver.isCurrentlyConnected())
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val _isSplashScreenShowing = MutableStateFlow(true)
    val isSplashScreenShowing: StateFlow<Boolean> = _isSplashScreenShowing.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _progress = MutableStateFlow(0)
    val progress: StateFlow<Int> = _progress.asStateFlow()

    private val _currentUrl = MutableStateFlow(TARGET_WEBSITE_URL)
    val currentUrl: StateFlow<String> = _currentUrl.asStateFlow()

    private val _webError = MutableStateFlow<String?>(null)
    val webError: StateFlow<String?> = _webError.asStateFlow()

    private val _canWebViewGoBack = MutableStateFlow(false)
    val canWebViewGoBack: StateFlow<Boolean> = _canWebViewGoBack.asStateFlow()

    private val _reloadTrigger = MutableStateFlow(0L)
    val reloadTrigger: StateFlow<Long> = _reloadTrigger.asStateFlow()

    init {
        // Monitor network state
        viewModelScope.launch {
            networkObserver.observe().collect { connected ->
                _isOnline.value = connected
                if (connected) {
                    // Auto refresh when internet connection returns or updates
                    triggerReload()
                }
            }
        }

        // Fast splash screen dismiss for snappy launch
        viewModelScope.launch {
            delay(1000)
            _isSplashScreenShowing.value = false
        }
    }

    fun onPageStarted(url: String) {
        _currentUrl.value = url
        _isLoading.value = true
        _webError.value = null
    }

    fun onPageFinished(url: String) {
        _currentUrl.value = url
        _isLoading.value = false
        _progress.value = 100
        _isSplashScreenShowing.value = false
    }

    fun onProgressChanged(newProgress: Int) {
        _progress.value = newProgress
        if (newProgress >= 100) {
            _isLoading.value = false
        }
    }

    fun onWebError(description: String) {
        _webError.value = description
        _isLoading.value = false
    }

    fun clearError() {
        _webError.value = null
        triggerReload()
    }

    fun triggerReload() {
        _webError.value = null
        _reloadTrigger.value = System.currentTimeMillis()
    }

    fun updateCanGoBack(canGoBack: Boolean) {
        _canWebViewGoBack.value = canGoBack
    }
}

package com.example

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.util.AppUpdateInfo
import com.example.util.NetworkObserver
import com.example.util.UpdateManager
import kotlinx.coroutines.Job
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

    // Full-screen branded loading screen state (shimmering logo):
    // Active during app open and page loads until onPageFinished confirms core AI chat elements are rendered
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var hasInitialLaunchCompleted = false
    private var startupTimeoutJob: Job? = null

    // Theme synchronization states
    private val _isDarkTheme = MutableStateFlow(true)
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    private val _themeColorHex = MutableStateFlow("#0F172A")
    val themeColorHex: StateFlow<String> = _themeColorHex.asStateFlow()

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

    // APK Update Info State
    private val _updateInfo = MutableStateFlow<AppUpdateInfo?>(null)
    val updateInfo: StateFlow<AppUpdateInfo?> = _updateInfo.asStateFlow()

    init {
        // Monitor network state
        viewModelScope.launch {
            networkObserver.observe().collect { connected ->
                val wasOffline = !_isOnline.value
                _isOnline.value = connected
                if (connected && wasOffline) {
                    triggerReload()
                }
            }
        }

        // Fast, non-blocking check for GitHub release APK updates
        viewModelScope.launch {
            val update = UpdateManager.checkForUpdates(BuildConfig.VERSION_NAME)
            if (update != null) {
                _updateInfo.value = update
            }
        }

        // Safety timeout for initial launch loading screen:
        // Dismiss loading overlay within 4000ms maximum so the app never hangs indefinitely on slow network
        startupTimeoutJob = viewModelScope.launch {
            delay(4000)
            if (!hasInitialLaunchCompleted) {
                hasInitialLaunchCompleted = true
                _isLoading.value = false
            }
        }
    }

    fun dismissUpdate() {
        _updateInfo.value = null
    }

    fun navigateToUrl(url: String) {
        if (url.isNotBlank() && url != _currentUrl.value) {
            _currentUrl.value = url
            _isLoading.value = true
            hasInitialLaunchCompleted = false
            _webError.value = null
            triggerReload()
        }
    }

    fun onThemeDetected(isDark: Boolean, hexColor: String) {
        if (_isDarkTheme.value != isDark || _themeColorHex.value != hexColor) {
            _isDarkTheme.value = isDark
            _themeColorHex.value = hexColor
        }
    }

    fun onWebsiteReady() {
        if (!hasInitialLaunchCompleted) {
            hasInitialLaunchCompleted = true
            startupTimeoutJob?.cancel()
            _isLoading.value = false
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
        onWebsiteReady()
    }

    fun onProgressChanged(newProgress: Int) {
        _progress.value = newProgress
        if (newProgress >= 80) {
            // Confirm core AI elements rendered and ready
            onWebsiteReady()
        }
        if (newProgress >= 100) {
            _isLoading.value = false
        }
    }

    fun onWebError(description: String) {
        _webError.value = description
        _isLoading.value = false
        onWebsiteReady()
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

package com.example

import android.Manifest
import android.content.ComponentCallbacks2
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.ui.components.StupidAiErrorScreen
import com.example.ui.components.StupidAiOfflineScreen
import com.example.ui.components.StupidAiSplashScreen
import com.example.ui.theme.StupidAiTheme
import com.example.ui.webview.StupidAiWebViewContainer

class MainActivity : ComponentActivity(), ComponentCallbacks2 {

    private val viewModel: MainViewModel by viewModels()

    // File Chooser state
    private var filePathCallback: ValueCallback<Array<Uri>>? = null
    private val fileChooserLauncher = registerForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (filePathCallback != null) {
            filePathCallback?.onReceiveValue(uris.toTypedArray())
            filePathCallback = null
        }
    }

    // Camera & Audio Permission request state
    private var pendingPermissionRequest: PermissionRequest? = null
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val cameraGranted = permissions[Manifest.permission.CAMERA] ?: false
        val audioGranted = permissions[Manifest.permission.RECORD_AUDIO] ?: false

        pendingPermissionRequest?.let { req ->
            val grantedResources = mutableListOf<String>()
            if (cameraGranted) grantedResources.add(PermissionRequest.RESOURCE_VIDEO_CAPTURE)
            if (audioGranted) grantedResources.add(PermissionRequest.RESOURCE_AUDIO_CAPTURE)

            if (grantedResources.isNotEmpty()) {
                req.grant(grantedResources.toTypedArray())
            } else {
                req.deny()
            }
        }
        pendingPermissionRequest = null
    }

    // Reference to active WebView instance
    private var activeWebView: WebView? = null
    private var backPressedTime = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        // Install Android Splash Screen API
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Keep branded splash screen active until WebView initializes initial load
        splashScreen.setKeepOnScreenCondition {
            viewModel.isLoading.value && viewModel.isSplashScreenShowing.value
        }

        setContent {
            StupidAiTheme(darkTheme = true) {
                StupidAiAppScreen(
                    viewModel = viewModel,
                    onShowFileChooser = { callback, _ ->
                        filePathCallback?.onReceiveValue(null)
                        filePathCallback = callback
                        fileChooserLauncher.launch("*/*")
                        true
                    },
                    onRequestPermissions = { request ->
                        pendingPermissionRequest = request
                        val resources = request.resources
                        val permissionsToRequest = mutableListOf<String>()

                        if (resources.contains(PermissionRequest.RESOURCE_VIDEO_CAPTURE)) {
                            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                                permissionsToRequest.add(Manifest.permission.CAMERA)
                            }
                        }
                        if (resources.contains(PermissionRequest.RESOURCE_AUDIO_CAPTURE)) {
                            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                                permissionsToRequest.add(Manifest.permission.RECORD_AUDIO)
                            }
                        }

                        if (permissionsToRequest.isNotEmpty()) {
                            permissionLauncher.launch(permissionsToRequest.toTypedArray())
                        } else {
                            request.grant(resources)
                            pendingPermissionRequest = null
                        }
                    },
                    onWebViewCreated = { webView ->
                        activeWebView = webView
                    },
                    onBackPressed = {
                        handleAppBackPress()
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Only trigger reload if the app was previously in an error or offline state
        if (viewModel.webError.value != null || !viewModel.isOnline.value) {
            viewModel.triggerReload()
        }
    }

    // Custom back-button handling navigating through WebView browser history
    private fun handleAppBackPress() {
        val webView = activeWebView
        if (webView != null && webView.canGoBack()) {
            webView.goBack() // Navigate backwards through browser history
        } else {
            // Confirmation exit toast at root home page
            val now = System.currentTimeMillis()
            if (now - backPressedTime < 2000) {
                finish()
            } else {
                backPressedTime = now
                Toast.makeText(this, "Press back again to exit STUPID AI", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= ComponentCallbacks2.TRIM_MEMORY_MODERATE) {
            // Trim non-critical memory caches without clearing user cookies or sessions
            activeWebView?.freeMemory()
        }
    }
}

@Composable
fun StupidAiAppScreen(
    viewModel: MainViewModel,
    onShowFileChooser: (ValueCallback<Array<Uri>>?, WebChromeClient.FileChooserParams?) -> Boolean,
    onRequestPermissions: (PermissionRequest) -> Unit,
    onWebViewCreated: (WebView) -> Unit,
    onBackPressed: () -> Unit
) {
    val isOnline by viewModel.isOnline.collectAsState()
    val isSplashShowing by viewModel.isSplashScreenShowing.collectAsState()
    val webError by viewModel.webError.collectAsState()
    val reloadTrigger by viewModel.reloadTrigger.collectAsState()

    // Register BackHandler for Jetpack Compose back navigation
    BackHandler {
        onBackPressed()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            contentWindowInsets = WindowInsets.systemBars
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .imePadding()
            ) {
                // Pure Full-Screen WebView Container with WindowInsets & IME Padding
                StupidAiWebViewContainer(
                    targetUrl = MainViewModel.TARGET_WEBSITE_URL,
                    reloadTrigger = reloadTrigger,
                    clearCacheTrigger = 0L,
                    onPageStarted = { url -> viewModel.onPageStarted(url) },
                    onPageFinished = { url -> viewModel.onPageFinished(url) },
                    onProgressChanged = { newProgress -> viewModel.onProgressChanged(newProgress) },
                    onError = { description -> viewModel.onWebError(description) },
                    onCanGoBackChanged = { canGoBack -> viewModel.updateCanGoBack(canGoBack) },
                    onShowFileChooser = onShowFileChooser,
                    onRequestPermissions = onRequestPermissions,
                    onWebViewCreated = onWebViewCreated
                )

                // Native Offline Overlay (Only shown when internet is completely lost)
                if (!isOnline) {
                    StupidAiOfflineScreen(
                        onRetryClick = { viewModel.triggerReload() }
                    )
                }

                // Native Error Overlay
                if (isOnline && webError != null) {
                    StupidAiErrorScreen(
                        errorMessage = webError ?: "Failed to connect",
                        onRetryClick = { viewModel.clearError() }
                    )
                }
            }
        }

        // Native Splash Overlay (smooth fade-out)
        AnimatedVisibility(
            visible = isSplashShowing,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            StupidAiSplashScreen()
        }
    }
}

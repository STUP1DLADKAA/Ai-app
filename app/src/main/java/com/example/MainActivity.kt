package com.example

import android.Manifest
import android.app.Activity
import android.content.ComponentCallbacks2
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.os.Bundle
import android.view.View
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
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import com.example.ui.components.IosUpdateDialog
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

    // Reference to active WebView instance & background tracking flag
    private var activeWebView: WebView? = null
    private var backPressedTime = 0L
    private var wasInBackground = false

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Handle incoming intent (e.g. from Home Screen chat shortcut)
        handleShortcutIntent(intent)

        setContent {
            val isDarkTheme by viewModel.isDarkTheme.collectAsState()
            val themeColorHex by viewModel.themeColorHex.collectAsState()

            StupidAiTheme(darkTheme = isDarkTheme) {
                StupidAiAppScreen(
                    viewModel = viewModel,
                    isDarkTheme = isDarkTheme,
                    themeColorHex = themeColorHex,
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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleShortcutIntent(intent)
    }

    private fun handleShortcutIntent(intent: Intent?) {
        val chatUrl = intent?.getStringExtra("TARGET_CHAT_URL") ?: intent?.dataString
        if (!chatUrl.isNullOrBlank() && chatUrl.startsWith("http")) {
            viewModel.navigateToUrl(chatUrl)
        }
    }

    override fun onPause() {
        super.onPause()
        wasInBackground = true
        activeWebView?.onPause()
    }

    override fun onResume() {
        super.onResume()
        activeWebView?.onResume()

        // Fix background white page bug when returning to the app
        if (wasInBackground) {
            wasInBackground = false
            activeWebView?.post {
                val url = activeWebView?.url
                if (url.isNullOrBlank() || url == "about:blank") {
                    viewModel.triggerReload()
                } else {
                    // Check if DOM body is missing/blank after backgrounding; refresh if blank
                    activeWebView?.evaluateJavascript(
                        "(function() { if (!document.body || document.body.children.length === 0) { window.location.reload(); } else if (window.checkTheme) { window.checkTheme(); } })()",
                        null
                    )
                }
            }
        }

        // Trigger reload if app was in error or offline state
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
                Toast.makeText(this, "Press back again to exit Stupid AI", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= ComponentCallbacks2.TRIM_MEMORY_MODERATE) {
            activeWebView?.freeMemory()
        }
    }
}

@Composable
fun StupidAiAppScreen(
    viewModel: MainViewModel,
    isDarkTheme: Boolean,
    themeColorHex: String,
    onShowFileChooser: (ValueCallback<Array<Uri>>?, WebChromeClient.FileChooserParams?) -> Boolean,
    onRequestPermissions: (PermissionRequest) -> Unit,
    onWebViewCreated: (WebView) -> Unit,
    onBackPressed: () -> Unit
) {
    val context = LocalContext.current
    val isOnline by viewModel.isOnline.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val webError by viewModel.webError.collectAsState()
    val reloadTrigger by viewModel.reloadTrigger.collectAsState()
    val currentUrl by viewModel.currentUrl.collectAsState()
    val updateInfo by viewModel.updateInfo.collectAsState()

    val surfaceColor = remember(themeColorHex) {
        try {
            Color(AndroidColor.parseColor(themeColorHex))
        } catch (e: Exception) {
            if (isDarkTheme) Color(0xFF0F172A) else Color(0xFFFFFFFF)
        }
    }

    // Dynamic Android Theme Synchronization for Status Bar and System Navigation Area
    LaunchedEffect(isDarkTheme, themeColorHex) {
        val window = (context as? Activity)?.window
        if (window != null) {
            val insetsController = WindowCompat.getInsetsController(window, window.decorView)
            insetsController.isAppearanceLightStatusBars = !isDarkTheme
            insetsController.isAppearanceLightNavigationBars = !isDarkTheme

            try {
                val parsedColor = AndroidColor.parseColor(themeColorHex)
                @Suppress("DEPRECATION")
                window.statusBarColor = parsedColor
                @Suppress("DEPRECATION")
                window.navigationBarColor = parsedColor
            } catch (e: Exception) {
                val fallbackColor = if (isDarkTheme) AndroidColor.parseColor("#0F172A") else AndroidColor.WHITE
                @Suppress("DEPRECATION")
                window.statusBarColor = fallbackColor
                @Suppress("DEPRECATION")
                window.navigationBarColor = fallbackColor
            }
        }
    }

    // Full-screen custom view state for HTML5 video / media
    var customView by remember { mutableStateOf<View?>(null) }
    var customViewCallback by remember { mutableStateOf<WebChromeClient.CustomViewCallback?>(null) }

    // Register BackHandler for Jetpack Compose back navigation
    BackHandler {
        if (customView != null) {
            customViewCallback?.onCustomViewHidden()
            customView = null
            customViewCallback = null
        } else {
            onBackPressed()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(surfaceColor)
    ) {
        Scaffold(
            contentWindowInsets = WindowInsets.systemBars,
            containerColor = surfaceColor
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .imePadding()
            ) {
                // Pure Full-Screen WebView Container with WindowInsets & IME Padding
                StupidAiWebViewContainer(
                    targetUrl = currentUrl,
                    reloadTrigger = reloadTrigger,
                    clearCacheTrigger = 0L,
                    onPageStarted = { url -> viewModel.onPageStarted(url) },
                    onPageFinished = { url -> viewModel.onPageFinished(url) },
                    onProgressChanged = { newProgress -> viewModel.onProgressChanged(newProgress) },
                    onError = { description -> viewModel.onWebError(description) },
                    onCanGoBackChanged = { canGoBack -> viewModel.updateCanGoBack(canGoBack) },
                    onThemeDetected = { isDark, hex -> viewModel.onThemeDetected(isDark, hex) },
                    onWebsiteReady = { viewModel.onWebsiteReady() },
                    onRetry = { viewModel.clearError() },
                    onShowFileChooser = onShowFileChooser,
                    onRequestPermissions = onRequestPermissions,
                    onWebViewCreated = onWebViewCreated,
                    onShowCustomView = { view, callback ->
                        customView = view
                        customViewCallback = callback
                    },
                    onHideCustomView = {
                        customViewCallback?.onCustomViewHidden()
                        customView = null
                        customViewCallback = null
                    }
                )

                // Native Offline Overlay (Only shown when internet is completely lost)
                if (!isOnline) {
                    StupidAiOfflineScreen(
                        onRetryClick = { viewModel.triggerReload() }
                    )
                }

                // Custom Error Page View displaying network connectivity failures with Retry button
                if (isOnline && webError != null) {
                    StupidAiErrorScreen(
                        errorMessage = webError ?: "Network request failed",
                        onRetryClick = { viewModel.clearError() },
                        isDarkTheme = isDarkTheme
                    )
                }
            }
        }

        // Full-screen custom view overlay (HTML5 video, presentations, media)
        if (customView != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .testTag("fullscreen_custom_view_container")
            ) {
                AndroidView(
                    factory = { customView!! },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Full-Screen Branded Loading Layout (shimmering logo) hiding ONLY when pageFinished/AI elements are ready
        AnimatedVisibility(
            visible = isLoading,
            enter = fadeIn(),
            exit = fadeOut(animationSpec = tween(durationMillis = 400))
        ) {
            StupidAiSplashScreen()
        }

        // iOS-Style Update Dialog when a new GitHub APK release is detected
        updateInfo?.let { update ->
            IosUpdateDialog(
                updateInfo = update,
                onUpdateClick = { url ->
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(context, "Could not open update link", Toast.LENGTH_SHORT).show()
                    }
                    viewModel.dismissUpdate()
                },
                onDismissClick = {
                    viewModel.dismissUpdate()
                }
            )
        }
    }
}

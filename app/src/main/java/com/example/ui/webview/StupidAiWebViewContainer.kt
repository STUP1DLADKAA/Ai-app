package com.example.ui.webview

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.DownloadListener
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun StupidAiWebViewContainer(
    targetUrl: String,
    reloadTrigger: Long,
    clearCacheTrigger: Long,
    onPageStarted: (String) -> Unit,
    onPageFinished: (String) -> Unit,
    onProgressChanged: (Int) -> Unit,
    onError: (String) -> Unit,
    onCanGoBackChanged: (Boolean) -> Unit,
    onShowFileChooser: (ValueCallback<Array<Uri>>?, WebChromeClient.FileChooserParams?) -> Boolean,
    onRequestPermissions: (PermissionRequest) -> Unit,
    onWebViewCreated: (WebView) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val webView = remember {
        WebView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )

            // Hardware acceleration & input focus setup
            setLayerType(View.LAYER_TYPE_HARDWARE, null)
            isFocusable = true
            isFocusableInTouchMode = true
            isScrollContainer = true

            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true
                setSupportZoom(false)
                builtInZoomControls = false
                displayZoomControls = false
                mediaPlaybackRequiresUserGesture = false
                mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                allowFileAccess = false
                allowContentAccess = true
                cacheMode = WebSettings.LOAD_DEFAULT
                useWideViewPort = true
                loadWithOverviewMode = true
                setNeedInitialFocus(true)

                // Custom User-Agent tag to signal native STUPID AI Android app wrapper
                val defaultUa = userAgentString
                userAgentString = "$defaultUa StupidAiAndroidApp/1.0"
            }

            // Cookie Persistence Setup
            val cookieManager = CookieManager.getInstance()
            cookieManager.setAcceptCookie(true)
            cookieManager.setAcceptThirdPartyCookies(this, true)

            // Native Download Handling
            setDownloadListener { url, userAgent, contentDisposition, mimetype, contentLength ->
                try {
                    val request = DownloadManager.Request(Uri.parse(url)).apply {
                        setMimeType(mimetype)
                        addRequestHeader("User-Agent", userAgent)
                        setDescription("Downloading file from STUPID AI...")
                        setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                        setDestinationInExternalPublicDir(
                            Environment.DIRECTORY_DOWNLOADS,
                            URLUtil.guessFileName(url, contentDisposition, mimetype)
                        )
                    }
                    val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                    dm.enqueue(request)
                    Toast.makeText(context, "Downloading file...", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Download failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Set clients
    DisposableEffect(webView) {
        val client = StupidAiWebViewClient(
            context = context,
            onPageStarted = onPageStarted,
            onPageFinished = onPageFinished,
            onError = onError,
            onCanGoBackChanged = onCanGoBackChanged
        )

        val chromeClient = StupidAiWebChromeClient(
            onProgressChanged = onProgressChanged,
            onShowFileChooser = onShowFileChooser,
            onRequestPermissions = onRequestPermissions
        )

        webView.webViewClient = client
        webView.webChromeClient = chromeClient

        onWebViewCreated(webView)

        // Load initial target URL
        webView.loadUrl(targetUrl)

        onDispose {
            // Clean up to prevent memory leak while keeping cookies intact
            webView.stopLoading()
        }
    }

    // Handle Reload trigger
    LaunchedEffect(reloadTrigger) {
        if (reloadTrigger > 0) {
            webView.reload()
        }
    }

    // Handle Clear Cache trigger (false = preserves user session & cookies)
    LaunchedEffect(clearCacheTrigger) {
        if (clearCacheTrigger > 0) {
            webView.clearCache(false)
            Toast.makeText(context, "Memory cache optimized", Toast.LENGTH_SHORT).show()
        }
    }

    AndroidView(
        factory = { webView },
        modifier = modifier
            .fillMaxSize()
            .testTag("stupid_ai_webview")
    )
}

// Helper object for filename guessing
private object URLUtil {
    fun guessFileName(url: String, contentDisposition: String?, mimeType: String?): String {
        return android.webkit.URLUtil.guessFileName(url, contentDisposition, mimeType)
    }
}

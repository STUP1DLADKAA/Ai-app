package com.example.ui.webview

import android.app.DownloadManager
import android.content.Context
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
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
import com.example.BuildConfig

@Suppress("DEPRECATION")
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
    onThemeDetected: (Boolean, String) -> Unit,
    onWebsiteReady: () -> Unit,
    onRetry: () -> Unit,
    onShowFileChooser: (ValueCallback<Array<Uri>>?, WebChromeClient.FileChooserParams?) -> Boolean,
    onRequestPermissions: (PermissionRequest) -> Unit,
    onWebViewCreated: (WebView) -> Unit,
    onShowCustomView: ((View, WebChromeClient.CustomViewCallback) -> Unit)? = null,
    onHideCustomView: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Injected JavaScript helper for dynamic theme detection, specific chat shortcut creation & AI readiness
    val helperJs = """
        (function() {
            if (window.__stupidAiBridgeInitialized) {
                if (typeof checkTheme === 'function') checkTheme();
                if (typeof checkAiElements === 'function') checkAiElements();
                return;
            }
            window.__stupidAiBridgeInitialized = true;

            // Signal native environment to the website
            window.isNativeApp = true;
            window.__STUPID_AI_NATIVE_APP__ = true;
            try {
                Object.defineProperty(navigator, 'standalone', { get: function() { return true; } });
            } catch(e) {}

            // Prevent default browser install banner and handle via native shortcut
            window.addEventListener('beforeinstallprompt', function(e) {
                e.preventDefault();
                return false;
            });

            // Intercept clicks on Add to Home Screen / Add Shortcut / Install App buttons
            document.addEventListener('click', function(e) {
                var target = e.target;
                while (target && target !== document.body) {
                    var text = (target.innerText || target.textContent || '').trim().toLowerCase();
                    var aria = (target.getAttribute('aria-label') || '').toLowerCase();
                    var testId = (target.getAttribute('data-testid') || '').toLowerCase();
                    if (text.indexOf('add to home') !== -1 || text.indexOf('install app') !== -1 || text.indexOf('add shortcut') !== -1 ||
                        aria.indexOf('install') !== -1 || testId.indexOf('install') !== -1) {
                        if (window.StupidAiNativeBridge && window.StupidAiNativeBridge.requestInstallShortcut) {
                            e.preventDefault();
                            e.stopPropagation();

                            // Extract specific chat title if present
                            var titleElem = document.querySelector('h1, h2, [data-testid*="title"], [class*="title"], header');
                            var chatTitle = (titleElem ? (titleElem.innerText || titleElem.textContent || '').trim() : '') || document.title || 'Stupid AI Chat';
                            var chatUrl = window.location.href;

                            window.StupidAiNativeBridge.requestInstallShortcut(chatTitle, chatUrl);
                            return false;
                        }
                    }
                    target = target.parentElement;
                }
            }, true);

            // Dynamic Theme Detection Logic
            function checkTheme() {
                try {
                    var html = document.documentElement;
                    var body = document.body;
                    var classNames = (html.className + ' ' + (body ? body.className : '')).toLowerCase();
                    var dataTheme = (html.getAttribute('data-theme') || (body ? body.getAttribute('data-theme') : '') || '').toLowerCase();
                    var isDark = false;
                    var bgColorHex = '#0F172A';

                    var elem = body || html;
                    var comp = window.getComputedStyle(elem);
                    var bg = comp.backgroundColor;
                    if ((!bg || bg === 'transparent' || bg.indexOf('rgba(0, 0, 0, 0)') !== -1) && body) {
                        comp = window.getComputedStyle(html);
                        bg = comp.backgroundColor;
                    }

                    var rgb = bg ? bg.match(/\d+/g) : null;
                    if (rgb && rgb.length >= 3) {
                        var r = parseInt(rgb[0], 10);
                        var g = parseInt(rgb[1], 10);
                        var b = parseInt(rgb[2], 10);
                        var lum = 0.299 * r + 0.587 * g + 0.114 * b;
                        isDark = lum < 128;
                        bgColorHex = '#' + ((1 << 24) + (r << 16) + (g << 8) + b).toString(16).slice(1);
                    } else if (classNames.indexOf('dark') !== -1 || dataTheme.indexOf('dark') !== -1) {
                        isDark = true;
                        bgColorHex = '#0F172A';
                    } else if (classNames.indexOf('light') !== -1 || dataTheme.indexOf('light') !== -1) {
                        isDark = false;
                        bgColorHex = '#FFFFFF';
                    } else {
                        isDark = window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches;
                        bgColorHex = isDark ? '#0F172A' : '#FFFFFF';
                    }

                    if (window.StupidAiNativeBridge && window.StupidAiNativeBridge.onThemeChanged) {
                        window.StupidAiNativeBridge.onThemeChanged(isDark, bgColorHex);
                    }
                } catch (err) {}
            }
            window.checkTheme = checkTheme;

            // Confirm core AI chat elements rendered before dismissing full-screen loading overlay
            function checkAiElements() {
                try {
                    var aiChatInputs = document.querySelectorAll('textarea, input[type="text"], button[type="submit"], form, [contenteditable="true"]');
                    if (aiChatInputs.length > 0 || document.readyState === 'complete') {
                        if (window.StupidAiNativeBridge && window.StupidAiNativeBridge.onAppReady) {
                            window.StupidAiNativeBridge.onAppReady();
                        }
                    }
                } catch(e) {}
            }
            window.checkAiElements = checkAiElements;

            // Run initial checks
            checkTheme();
            checkAiElements();

            // Observe dynamic theme and DOM mutations
            var observer = new MutationObserver(function() {
                checkTheme();
                checkAiElements();
            });
            observer.observe(document.documentElement, { attributes: true, childList: true, subtree: true });

            // Signal website readiness when DOM is interactive
            if (document.readyState === 'complete' || document.readyState === 'interactive') {
                checkAiElements();
            } else {
                window.addEventListener('DOMContentLoaded', function() {
                    checkAiElements();
                });
            }
        })();
    """.trimIndent()

    val webView = remember {
        WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG)
        WebView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )

            // Explicit dark background color to prevent white flashes during backgrounding/resuming or page loads
            setBackgroundColor(AndroidColor.parseColor("#0F172A"))

            // Hardware acceleration & input focus setup
            setLayerType(View.LAYER_TYPE_HARDWARE, null)
            isFocusable = true
            isFocusableInTouchMode = true
            isScrollContainer = true

            // Attach safe native bridge
            val nativeBridge = StupidAiNativeBridge(
                context = context,
                onThemeDetected = onThemeDetected,
                onWebsiteReady = onWebsiteReady,
                onRetry = onRetry
            )
            addJavascriptInterface(nativeBridge, "StupidAiNativeBridge")

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
                setSupportMultipleWindows(false)
                loadsImagesAutomatically = true

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    safeBrowsingEnabled = true
                }

                val defaultUa = userAgentString
                userAgentString = "$defaultUa StupidAiAndroidApp/1.0"
            }

            // Cookie Persistence Setup
            val cookieManager = CookieManager.getInstance()
            cookieManager.setAcceptCookie(true)
            cookieManager.setAcceptThirdPartyCookies(this, true)

            // Native Download Handling
            setDownloadListener { url, userAgent, contentDisposition, mimetype, _ ->
                try {
                    val request = DownloadManager.Request(Uri.parse(url)).apply {
                        setMimeType(mimetype)
                        addRequestHeader("User-Agent", userAgent)
                        setDescription("Downloading file from Stupid AI...")
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
            onPageStarted = { url ->
                onPageStarted(url)
                webView.evaluateJavascript(helperJs, null)
            },
            onPageFinished = { url ->
                onPageFinished(url)
                webView.evaluateJavascript(helperJs, null)
            },
            onError = onError,
            onCanGoBackChanged = onCanGoBackChanged
        )

        val chromeClient = StupidAiWebChromeClient(
            onProgressChanged = { progress ->
                onProgressChanged(progress)
                if (progress in 30..90) {
                    webView.evaluateJavascript(helperJs, null)
                }
            },
            onShowFileChooser = onShowFileChooser,
            onRequestPermissions = onRequestPermissions,
            onShowCustomView = onShowCustomView,
            onHideCustomView = onHideCustomView
        )

        webView.webViewClient = client
        webView.webChromeClient = chromeClient

        onWebViewCreated(webView)

        // Load initial target URL
        webView.loadUrl(targetUrl)

        onDispose {
            webView.stopLoading()
        }
    }

    // Handle targetUrl navigation changes
    LaunchedEffect(targetUrl) {
        if (targetUrl.isNotBlank() && webView.url != targetUrl) {
            webView.loadUrl(targetUrl)
        }
    }

    // Handle Reload trigger
    LaunchedEffect(reloadTrigger) {
        if (reloadTrigger > 0) {
            webView.reload()
        }
    }

    // Handle Clear Cache trigger
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

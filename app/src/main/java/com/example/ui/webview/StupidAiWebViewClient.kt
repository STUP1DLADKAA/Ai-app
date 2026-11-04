package com.example.ui.webview

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient

@Suppress("DEPRECATION")
class StupidAiWebViewClient(
    private val context: Context,
    private val targetHost: String = "ai-website-xi-lac.vercel.app",
    private val onPageStarted: (String) -> Unit,
    private val onPageFinished: (String) -> Unit,
    private val onError: (String) -> Unit,
    private val onCanGoBackChanged: (Boolean) -> Unit
) : WebViewClient() {

    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        val uri = request?.url ?: return false
        val scheme = uri.scheme ?: ""

        // Render ALL http/https web content directly inside the app's WebView
        if (scheme.equals("http", ignoreCase = true) || scheme.equals("https", ignoreCase = true)) {
            return false // Render inside the app, never forward to external browser
        }

        // Handle special system intents (tel, mailto) if present
        if (scheme.equals("mailto", ignoreCase = true) || scheme.equals("tel", ignoreCase = true)) {
            try {
                val intent = Intent(Intent.ACTION_VIEW, uri)
                context.startActivity(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return true
        }

        return false
    }

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        url?.let { onPageStarted(it) }
        view?.let { onCanGoBackChanged(it.canGoBack()) }
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        url?.let { onPageFinished(it) }
        view?.let { onCanGoBackChanged(it.canGoBack()) }
    }

    override fun onReceivedError(
        view: WebView?,
        request: WebResourceRequest?,
        error: WebResourceError?
    ) {
        super.onReceivedError(view, request, error)
        if (request?.isForMainFrame == true) {
            val description = error?.description?.toString() ?: "Network request failed"
            // Intercept default browser error page and display local offline HTML
            view?.loadUrl("file:///android_asset/offline.html")
            onError(description)
        }
    }

    override fun onReceivedError(
        view: WebView?,
        errorCode: Int,
        description: String?,
        failingUrl: String?
    ) {
        super.onReceivedError(view, errorCode, description, failingUrl)
        view?.loadUrl("file:///android_asset/offline.html")
        onError(description ?: "Network error occurred")
    }

    override fun onReceivedHttpError(
        view: WebView?,
        request: WebResourceRequest?,
        errorResponse: WebResourceResponse?
    ) {
        super.onReceivedHttpError(view, request, errorResponse)
        if (request?.isForMainFrame == true && (errorResponse?.statusCode ?: 0) >= 500) {
            view?.loadUrl("file:///android_asset/offline.html")
            onError("Server Error (${errorResponse?.statusCode}): ${errorResponse?.reasonPhrase}")
        }
    }

    override fun onRenderProcessGone(view: WebView?, detail: RenderProcessGoneDetail?): Boolean {
        view?.destroy()
        onError("Web renderer reset. Reloading app...")
        return true
    }
}

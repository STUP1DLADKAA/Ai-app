package com.example.ui.webview

import android.net.Uri
import android.util.Log
import android.view.View
import android.webkit.ConsoleMessage
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView

class StupidAiWebChromeClient(
    private val onProgressChanged: (Int) -> Unit,
    private val onShowFileChooser: (ValueCallback<Array<Uri>>?, WebChromeClient.FileChooserParams?) -> Boolean,
    private val onRequestPermissions: (PermissionRequest) -> Unit,
    private val onShowCustomView: ((View, WebChromeClient.CustomViewCallback) -> Unit)? = null,
    private val onHideCustomView: (() -> Unit)? = null
) : WebChromeClient() {

    override fun onProgressChanged(view: WebView?, newProgress: Int) {
        super.onProgressChanged(view, newProgress)
        onProgressChanged(newProgress)
    }

    override fun onShowFileChooser(
        webView: WebView?,
        filePathCallback: ValueCallback<Array<Uri>>?,
        fileChooserParams: FileChooserParams?
    ): Boolean {
        return onShowFileChooser(filePathCallback, fileChooserParams)
    }

    override fun onPermissionRequest(request: PermissionRequest?) {
        if (request == null) return
        onRequestPermissions(request)
    }

    override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
        if (view != null && callback != null && onShowCustomView != null) {
            onShowCustomView.invoke(view, callback)
        } else {
            super.onShowCustomView(view, callback)
        }
    }

    override fun onHideCustomView() {
        if (onHideCustomView != null) {
            onHideCustomView.invoke()
        } else {
            super.onHideCustomView()
        }
    }

    override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
        consoleMessage?.let {
            Log.d("STUPID_AI_WEBVIEW", "[JS Console] ${it.message()} -- From line ${it.lineNumber()} of ${it.sourceId()}")
        }
        return true
    }
}

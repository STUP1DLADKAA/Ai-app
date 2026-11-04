package com.example.ui.webview

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.widget.Toast
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.example.MainActivity
import com.example.R

/**
 * Lightweight, secure JavaScript-to-Native bridge for Stupid AI.
 * Handles dynamic theme detection, startup readiness notification,
 * native Home Screen pinned shortcut creation for specific chats, and error retry.
 */
class StupidAiNativeBridge(
    private val context: Context,
    private val onThemeDetected: (isDark: Boolean, hexColor: String) -> Unit,
    private val onWebsiteReady: () -> Unit,
    private val onRetry: () -> Unit
) {
    private val mainHandler = Handler(Looper.getMainLooper())

    @JavascriptInterface
    fun onThemeChanged(isDark: Boolean, hexColor: String?) {
        mainHandler.post {
            val validColor = if (!hexColor.isNullOrBlank() && hexColor.startsWith("#")) {
                hexColor
            } else {
                if (isDark) "#0F172A" else "#FFFFFF"
            }
            onThemeDetected(isDark, validColor)
        }
    }

    @JavascriptInterface
    fun onAppReady() {
        mainHandler.post {
            onWebsiteReady()
        }
    }

    @JavascriptInterface
    fun isNativeApp(): Boolean = true

    @JavascriptInterface
    fun retry() {
        mainHandler.post {
            onRetry()
        }
    }

    @JavascriptInterface
    fun requestInstallShortcut(title: String?, url: String?) {
        mainHandler.post {
            val chatTitle = if (!title.isNullOrBlank() && title != "null" && title != "undefined") title else "Stupid AI Chat"
            val chatUrl = if (!url.isNullOrBlank() && url != "null" && url != "undefined") url else "https://ai-website-xi-lac.vercel.app/"
            createChatHomeScreenShortcut(chatTitle, chatUrl)
        }
    }

    private fun createChatHomeScreenShortcut(title: String, url: String) {
        try {
            if (ShortcutManagerCompat.isRequestPinShortcutSupported(context)) {
                // Intent targeting MainActivity with specific chat URL
                val shortcutIntent = Intent(context, MainActivity::class.java).apply {
                    action = Intent.ACTION_VIEW
                    data = Uri.parse(url)
                    putExtra("TARGET_CHAT_URL", url)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }

                val shortcutId = "chat_shortcut_" + url.hashCode().toString()
                val pinShortcutInfo = ShortcutInfoCompat.Builder(context, shortcutId)
                    .setShortLabel(title.take(25))
                    .setLongLabel(title)
                    .setIcon(IconCompat.createWithResource(context, R.mipmap.ic_launcher))
                    .setIntent(shortcutIntent)
                    .build()

                ShortcutManagerCompat.requestPinShortcut(context, pinShortcutInfo, null)
                Toast.makeText(context, "Added shortcut for \"$title\" to Home Screen", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Pinned shortcuts are not supported on this device launcher", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to create shortcut: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}

package com.example.util

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class AppUpdateInfo(
    val tagName: String,
    val title: String,
    val releaseNotes: String,
    val downloadUrl: String
)

object UpdateManager {

    private const val GITHUB_LATEST_RELEASE_URL = "https://api.github.com/repos/STUP1DLADKAA/Ai-app/releases/latest"
    private const val GITHUB_RELEASES_LIST_URL = "https://api.github.com/repos/STUP1DLADKAA/Ai-app/releases"
    private const val FALLBACK_RELEASE_PAGE = "https://github.com/STUP1DLADKAA/Ai-app/releases"

    suspend fun checkForUpdates(currentVersionName: String): AppUpdateInfo? = withContext(Dispatchers.IO) {
        // Try latest release endpoint first, then fallback to releases list
        val releaseObj = fetchReleaseJson(GITHUB_LATEST_RELEASE_URL)
            ?: fetchFirstFromReleasesList(GITHUB_RELEASES_LIST_URL)

        if (releaseObj != null) {
            val tagName = releaseObj.optString("tag_name", "").trim()
            val title = releaseObj.optString("name", "").trim()
            val body = releaseObj.optString("body", "").trim()

            var apkDownloadUrl = releaseObj.optString("html_url", FALLBACK_RELEASE_PAGE)
            val assets = releaseObj.optJSONArray("assets")
            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val asset = assets.optJSONObject(i)
                    val name = asset?.optString("name", "") ?: ""
                    if (name.endsWith(".apk", ignoreCase = true)) {
                        apkDownloadUrl = asset?.optString("browser_download_url", apkDownloadUrl) ?: apkDownloadUrl
                        break
                    }
                }
            }

            if (tagName.isNotBlank() && isNewerVersion(tagName, currentVersionName)) {
                return@withContext AppUpdateInfo(
                    tagName = tagName,
                    title = if (title.isNotBlank()) title else "Stupid AI $tagName",
                    releaseNotes = body,
                    downloadUrl = apkDownloadUrl
                )
            }
        }

        return@withContext null
    }

    private fun fetchReleaseJson(urlString: String): JSONObject? {
        return try {
            val url = URL(urlString)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 3000
                readTimeout = 3000
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                setRequestProperty("User-Agent", "StupidAiAndroidApp")
            }

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val jsonStr = connection.inputStream.bufferedReader().use { it.readText() }
                JSONObject(jsonStr)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun fetchFirstFromReleasesList(urlString: String): JSONObject? {
        return try {
            val url = URL(urlString)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 3000
                readTimeout = 3000
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                setRequestProperty("User-Agent", "StupidAiAndroidApp")
            }

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val jsonStr = connection.inputStream.bufferedReader().use { it.readText() }
                val jsonArray = JSONArray(jsonStr)
                if (jsonArray.length() > 0) jsonArray.getJSONObject(0) else null
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun isNewerVersion(latestTag: String, currentVersion: String): Boolean {
        try {
            val cleanTag = latestTag.lowercase().replace("v", "").trim()
            val cleanCurrent = currentVersion.lowercase().replace("v", "").trim()

            if (cleanTag == cleanCurrent) return false

            val tagParts = cleanTag.split(".").mapNotNull { it.toIntOrNull() }
            val currentParts = cleanCurrent.split(".").mapNotNull { it.toIntOrNull() }

            val maxLength = maxOf(tagParts.size, currentParts.size)
            for (i in 0 until maxLength) {
                val tagPart = tagParts.getOrElse(i) { 0 }
                val currentPart = currentParts.getOrElse(i) { 0 }
                if (tagPart > currentPart) return true
                if (tagPart < currentPart) return false
            }
        } catch (e: Exception) {
            Log.e("UpdateManager", "Error comparing versions: ${e.message}")
        }
        return false
    }
}

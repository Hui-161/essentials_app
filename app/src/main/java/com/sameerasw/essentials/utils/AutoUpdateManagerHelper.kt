/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Utilities - General
 * File: AutoUpdateManagerHelper.kt
 * Description: Downloads app updates from this build's own GitHub releases and installs them
 * only if they are signed with the same certificate as the installed app.
 */

package com.sameerasw.essentials.utils

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import com.sameerasw.essentials.BuildConfig
import com.sameerasw.essentials.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

class AutoUpdateManagerHelper(
    private val context: Context,
) {
    companion object {
        private const val TAG = "AppUpdater"
        private const val UPDATES_DIR = "updates"

        /** Release assets of the repository this build is published from. */
        val RELEASE_DOWNLOAD_PREFIX = "https://github.com/${BuildConfig.RELEASE_REPO}/releases/download/"

        fun isTrustedDownloadUrl(url: String): Boolean = url.startsWith(RELEASE_DOWNLOAD_PREFIX) && !url.contains("..")
    }

    suspend fun downloadAndInstallApk(
        apkUrl: String,
        apkName: String,
        onProgressUpdate: (Int) -> Unit,
    ) = withContext(Dispatchers.IO) {
        try {
            if (!isTrustedDownloadUrl(apkUrl)) {
                Log.w(TAG, "Refusing update from a URL outside ${BuildConfig.RELEASE_REPO} releases")
                showToast(R.string.update_download_failed)
                return@withContext
            }

            val dir = File(context.cacheDir, UPDATES_DIR).apply { mkdirs() }
            dir.listFiles()?.forEach { it.delete() }
            val apk = File(dir, "${apkName.replace(Regex("[^A-Za-z0-9_]"), "_")}.apk")

            if (!download(apkUrl, apk, onProgressUpdate)) {
                apk.delete()
                showToast(R.string.update_download_failed)
                return@withContext
            }

            if (!isSignedLikeInstalledApp(apk)) {
                Log.w(TAG, "Downloaded APK is not signed like the installed app, not installing it")
                apk.delete()
                showToast(R.string.update_signature_mismatch)
                return@withContext
            }

            install(apk)
        } catch (e: Exception) {
            Log.e(TAG, "Update failed", e)
            showToast(R.string.update_download_failed)
        } finally {
            onProgressUpdate(100)
        }
    }

    fun getCurrentAppVersionName(): String = BuildConfig.VERSION_NAME

    private fun download(
        url: String,
        target: File,
        onProgressUpdate: (Int) -> Unit,
    ): Boolean {
        // GitHub redirects release assets to its download host; HttpURLConnection follows https -> https only
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 15_000
        connection.readTimeout = 30_000
        try {
            if (connection.responseCode != HttpURLConnection.HTTP_OK) return false
            if (connection.url.protocol != "https") return false
            val total = connection.contentLengthLong
            var done = 0L
            var lastProgress = -1
            connection.inputStream.use { input ->
                target.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                        done += read
                        if (total > 0) {
                            val progress = (done * 99 / total).toInt()
                            if (progress != lastProgress) {
                                lastProgress = progress
                                onProgressUpdate(progress)
                            }
                        }
                    }
                }
            }
            return total <= 0 || done == total
        } finally {
            connection.disconnect()
        }
    }

    /**
     * True if [apk] is this app (same package name) and is signed by exactly the certificates
     * of the installed app. Android would reject a differently signed update anyway; checking
     * here also keeps any other app from being offered for installation.
     */
    private fun isSignedLikeInstalledApp(apk: File): Boolean {
        val pm = context.packageManager
        val flags =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                PackageManager.GET_SIGNING_CERTIFICATES
            } else {
                @Suppress("DEPRECATION")
                PackageManager.GET_SIGNATURES
            }
        val archive = pm.getPackageArchiveInfo(apk.path, flags) ?: return false
        if (archive.packageName != context.packageName) return false
        val installed = pm.getPackageInfo(context.packageName, flags)
        val archiveSigners = signerDigests(archive)
        return archiveSigners.isNotEmpty() && archiveSigners == signerDigests(installed)
    }

    private fun signerDigests(info: PackageInfo): Set<String> {
        val signatures =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                info.signingInfo?.apkContentsSigners
            } else {
                @Suppress("DEPRECATION")
                info.signatures
            } ?: return emptySet()
        val sha256 = MessageDigest.getInstance("SHA-256")
        return signatures.map { signature -> sha256.digest(signature.toByteArray()).joinToString("") { "%02x".format(it) } }.toSet()
    }

    private fun install(apk: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apk)
        val intent =
            Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        context.startActivity(intent)
    }

    private fun showToast(messageRes: Int) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, messageRes, Toast.LENGTH_LONG).show()
        }
    }
}

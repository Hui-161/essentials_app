/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Utilities - General
 * File: ShortcutUtil.kt
 * Description: Utility helper for ShortcutUtil.kt.
 */

package com.sameerasw.essentials.utils

import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.os.Build
import com.sameerasw.essentials.ShortcutHandlerActivity
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.domain.model.NotificationApp

object ShortcutUtil {
    const val EXTRA_PACKAGE_NAME = "package_name"
    const val EXTRA_SHORTCUT_TOKEN = "shortcut_token"

    /** Intent of a pinned "open frozen app" shortcut. */
    private fun appShortcutIntent(
        context: Context,
        packageName: String,
    ): Intent =
        Intent(context, ShortcutHandlerActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            putExtra(EXTRA_PACKAGE_NAME, packageName)
            putExtra(EXTRA_SHORTCUT_TOKEN, SettingsRepository(context).getShortcutToken())
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }

    /** True if [token] is the one this installation puts into its own pinned shortcuts. */
    fun isValidShortcutToken(
        context: Context,
        token: String?,
    ): Boolean {
        if (token.isNullOrEmpty()) return false
        val expected = SettingsRepository(context).getShortcutToken()
        return java.security.MessageDigest.isEqual(token.toByteArray(), expected.toByteArray())
    }

    /**
     * Gives pinned "open frozen app" shortcuts created before the token existed (or with another
     * token) the current one. Only the intent changes; label and icon stay.
     */
    fun refreshPinnedAppShortcuts(context: Context) {
        try {
            val shortcutManager = context.getSystemService(ShortcutManager::class.java) ?: return
            val token = SettingsRepository(context).getShortcutToken()
            val outdated =
                shortcutManager.pinnedShortcuts.filter { info ->
                    val intent = info.intent ?: return@filter false
                    intent.component?.className == ShortcutHandlerActivity::class.java.name &&
                        intent.getStringExtra(EXTRA_SHORTCUT_TOKEN) != token
                }
            if (outdated.isEmpty()) return
            val updated =
                outdated.mapNotNull { info ->
                    val packageName = info.intent?.getStringExtra(EXTRA_PACKAGE_NAME) ?: return@mapNotNull null
                    ShortcutInfo
                        .Builder(context, info.id)
                        .setIntent(appShortcutIntent(context, packageName))
                        .build()
                }
            shortcutManager.updateShortcuts(updated)
        } catch (e: Exception) {
            android.util.Log.w("ShortcutUtil", "Could not refresh pinned shortcuts", e)
        }
    }

    /**
     * Executes the pin app shortcut operation.
     *
     * @param context [Context] Target context.
     * @param app [NotificationApp] Target app.
     */
    fun pinAppShortcut(
        context: Context,
        app: NotificationApp,
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val shortcutManager = context.getSystemService(ShortcutManager::class.java)

            if (shortcutManager != null && shortcutManager.isRequestPinShortcutSupported) {
                val intent = appShortcutIntent(context, app.packageName)

                val shortcut =
                    ShortcutInfo
                        .Builder(context, app.packageName)
                        .setShortLabel(app.appName)
                        .setLongLabel(app.appName)
                        .setIcon(
                            Icon.createWithBitmap(
                                AppUtil.getShortcutIcon(
                                    context,
                                    app.packageName,
                                ),
                            ),
                        ).setIntent(intent)
                        .build()

                shortcutManager.requestPinShortcut(shortcut, null)
            }
        }
    }

    /**
     * Executes the update launcher dynamic shortcuts operation.
     *
     * @param context [Context] Target context.
     */
    fun updateLauncherDynamicShortcuts(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            val repository =
                com.sameerasw.essentials.data.repository
                    .SettingsRepository(context)
            val shortcutManager = context.getSystemService(ShortcutManager::class.java) ?: return

            val shortcuts = mutableListOf<ShortcutInfo>()

            // Wallpaper
            val wallpaperIntent =
                Intent(
                    context,
                    com.sameerasw.essentials.ui.activities.WallpaperActivity::class.java,
                ).apply {
                    action = Intent.ACTION_VIEW
                }
            val wallpaperShortcut =
                ShortcutInfo
                    .Builder(context, "shortcut_wallpaper")
                    .setShortLabel(context.getString(com.sameerasw.essentials.R.string.feat_daily_wallpaper_title))
                    .setLongLabel(context.getString(com.sameerasw.essentials.R.string.feat_daily_wallpaper_title))
                    .setIcon(
                        Icon.createWithResource(
                            context,
                            com.sameerasw.essentials.R.drawable.rounded_wallpaper_24,
                        ),
                    ).setIntent(wallpaperIntent)
                    .build()
            shortcuts.add(wallpaperShortcut)

            // Dynamic shortcuts
            val pinnedKeys = repository.getPinnedFeatures()
            val featuresMap =
                com.sameerasw.essentials.domain.registry.FeatureRegistry.ALL_FEATURES
                    .associateBy { it.id }

            var count = 0
            for (key in pinnedKeys) {
                if (count >= 2) break
                val feature = featuresMap[key] ?: continue
                if (feature.id == "DailyWallpaper" || feature.id == "LiveWallpaper") continue

                val intent =
                    Intent(
                        context,
                        com.sameerasw.essentials.FeatureSettingsActivity::class.java,
                    ).apply {
                        action = Intent.ACTION_VIEW
                        putExtra("feature", feature.id)
                    }
                val shortcut =
                    ShortcutInfo
                        .Builder(context, "shortcut_feat_${feature.id}")
                        .setShortLabel(context.getString(feature.title))
                        .setLongLabel(context.getString(feature.title))
                        .setIcon(Icon.createWithResource(context, feature.iconRes))
                        .setIntent(intent)
                        .build()
                shortcuts.add(shortcut)
                count++
            }

            shortcutManager.dynamicShortcuts = shortcuts
        }
    }
}

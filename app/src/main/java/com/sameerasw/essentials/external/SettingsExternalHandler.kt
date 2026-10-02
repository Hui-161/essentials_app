/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: External
 * File: SettingsExternalHandler.kt
 * Description: Component file for SettingsExternalHandler.kt.
 */

package com.sameerasw.essentials.external

import android.content.Context
import android.database.Cursor
import android.database.MatrixCursor
import android.os.Bundle
import com.sameerasw.essentials.data.repository.SettingsRepository

class SettingsExternalHandler : ExternalHandler {
    override val path: String = "settings"

    companion object {
        // Privilege and protection switches must only change inside Essentials, behind its own checks
        private val PROTECTED_KEYS =
            setOf(
                SettingsRepository.KEY_USE_ROOT,
                SettingsRepository.KEY_BUTTON_REMAP_USE_SHIZUKU,
                SettingsRepository.KEY_APP_LOCK_ENABLED,
                SettingsRepository.KEY_CONSCIOUS_GATE_ENABLED,
                SettingsRepository.KEY_SCREEN_LOCKED_SECURITY_ENABLED,
                SettingsRepository.KEY_SCREEN_LOCKED_DISABLE_NOTIFICATION_INTERACTIONS,
                SettingsRepository.KEY_ISLAND_NOTIF_CONCEAL_LOCKED,
                SettingsRepository.KEY_ISLAND_CALENDAR_HIDE_LOCKED,
                SettingsRepository.KEY_DUO_HIDE_WHEN_LOCKED,
            )

        /**
         * Other apps only get plain feature switches. Strings (tokens, app lists, automation JSON)
         * and numbers stay private, so they can neither be read nor turned into shell input.
         */
        private fun isExposed(
            key: String,
            value: Any?,
        ): Boolean = value is Boolean && key !in PROTECTED_KEYS
    }

    override fun onQuery(
        context: Context,
        remainingPath: String,
        extras: Bundle?,
    ): Cursor? {
        val key = remainingPath
        val prefs =
            context.getSharedPreferences(SettingsRepository.PREFS_NAME, Context.MODE_PRIVATE)
        if (!prefs.contains(key)) return null

        val value = prefs.all[key] ?: return null
        if (!isExposed(key, value)) return null
        val cursor = MatrixCursor(arrayOf("key", "value", "type"))
        cursor.addRow(arrayOf(key, value, value.javaClass.simpleName))
        return cursor
    }

    override fun onUpdate(
        context: Context,
        remainingPath: String,
        value: String?,
        extras: Bundle?,
    ): Boolean {
        val key = remainingPath
        val prefs =
            context.getSharedPreferences(SettingsRepository.PREFS_NAME, Context.MODE_PRIVATE)
        if (!prefs.contains(key)) return false

        val currentValue = prefs.all[key] ?: return false
        if (!isExposed(key, currentValue)) return false
        val repository = SettingsRepository(context)

        return try {
            when (currentValue) {
                is Boolean -> repository.putBoolean(key, value?.toBoolean() ?: false)
                is String -> repository.putString(key, value)
                is Int -> repository.putInt(key, value?.toInt() ?: 0)
                is Float -> repository.putFloat(key, value?.toFloat() ?: 0f)
                is Long -> repository.putLong(key, value?.toLong() ?: 0L)
                else -> false
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    override fun onAction(
        context: Context,
        remainingPath: String,
        action: String?,
        extras: Bundle?,
    ): Bundle? {
        if (action == "toggle") {
            val key = remainingPath
            val prefs =
                context.getSharedPreferences(SettingsRepository.PREFS_NAME, Context.MODE_PRIVATE)
            val currentValue = prefs.all[key]
            if (currentValue is Boolean && isExposed(key, currentValue)) {
                val repository = SettingsRepository(context)
                val newValue = !currentValue
                repository.putBoolean(key, newValue)
                return Bundle().apply { putBoolean("value", newValue) }
            }
        }
        return null
    }
}

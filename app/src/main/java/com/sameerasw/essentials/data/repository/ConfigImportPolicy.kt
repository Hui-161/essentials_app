/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Data & Repository Layer
 * File: ConfigImportPolicy.kt
 * Description: Rules for exporting and importing the app configuration (Settings → Export/Import).
 */

package com.sameerasw.essentials.data.repository

import com.google.gson.JsonElement
import com.google.gson.JsonParser
import com.sameerasw.essentials.R

/**
 * A configuration file can come from anywhere, so an import may only write the app's own
 * settings files, never credentials or state the app records about this device, and has to
 * keep each key's value type. Settings that protect the user and automations that change the
 * system are listed to the user before anything is applied.
 */
object ConfigImportPolicy {
    /** Preference files that are exported, and the only ones an import may write. */
    val PREF_FILES =
        listOf(
            SettingsRepository.PREFS_NAME,
            "caffeinate_prefs",
            "link_prefs",
            AUTOMATIONS_FILE,
            SettingsRepository.LIVE_WALLPAPER_PREFS_NAME,
        )

    const val AUTOMATIONS_FILE = "diy_automations_prefs"
    const val AUTOMATIONS_KEY = "saved_automations"

    /** Upper bound for an import file; real configurations are a few hundred kilobytes. */
    const val MAX_FILE_BYTES = 5 * 1024 * 1024

    // Recorded by the app on this device. An imported "shut_up_original_settings" would be
    // written to Settings.Global/Secure/System on the next restore (e.g. accessibility services).
    private val LOCAL_STATE_KEYS =
        setOf(
            SettingsRepository.KEY_SHUT_UP_ORIGINAL_SETTINGS,
            SettingsRepository.KEY_SNOOZE_DISCOVERED_CHANNELS,
            SettingsRepository.KEY_MAPS_DISCOVERED_CHANNELS,
            "airsync_mac_connected",
            "battery_history_points",
        )

    /** Keys that are neither exported nor imported. */
    fun isLocalOnly(key: String): Boolean =
        SettingsRepository.isSecretKey(key) || key in LOCAL_STATE_KEYS || key.startsWith("mac_battery_")

    /** Settings that protect the user, with the label shown before an import changes them. */
    val PROTECTION_KEYS: Map<String, Int> =
        mapOf(
            SettingsRepository.KEY_APP_LOCK_ENABLED to R.string.feat_app_lock_title,
            SettingsRepository.KEY_APP_LOCK_SELECTED_APPS to R.string.feat_app_lock_title,
            SettingsRepository.KEY_CONSCIOUS_GATE_ENABLED to R.string.feat_conscious_gate_title,
            SettingsRepository.KEY_CONSCIOUS_GATE_SELECTED_APPS to R.string.feat_conscious_gate_title,
            SettingsRepository.KEY_SCREEN_LOCKED_SECURITY_ENABLED to R.string.feat_screen_locked_security_title,
            SettingsRepository.KEY_SCREEN_LOCKED_DISABLE_NOTIFICATION_INTERACTIONS to
                R.string.feat_screen_locked_security_title,
            SettingsRepository.KEY_SECURE_SENSITIVE_TILES to R.string.qs_secure_sensitive_tiles_title,
            SettingsRepository.KEY_ISLAND_NOTIF_CONCEAL_LOCKED to R.string.island_notif_conceal_locked_title,
            SettingsRepository.KEY_ISLAND_CALENDAR_HIDE_LOCKED to R.string.island_calendar_hide_locked_title,
            SettingsRepository.KEY_DUO_HIDE_WHEN_LOCKED to R.string.duo_hide_when_locked_title,
            SettingsRepository.KEY_USE_ROOT to R.string.setting_use_root_title,
            SettingsRepository.KEY_BUTTON_REMAP_USE_SHIZUKU to R.string.button_remap_use_shizuku_title,
        )

    /** DIY actions that change the system or other apps, with their titles. */
    val PRIVILEGED_ACTION_TYPES: Map<String, Int> =
        mapOf(
            "CustomSettings" to R.string.diy_action_custom_settings,
            "Keyboard" to R.string.diy_set_keyboard_title,
            "FreezeApps" to R.string.diy_action_freeze_apps,
            "UnfreezeApps" to R.string.diy_action_unfreeze_apps,
            "FreezeTag" to R.string.diy_action_freeze_tag,
        )

    data class AutomationSummary(
        val total: Int = 0,
        val privileged: Int = 0,
        val privilegedActionTypes: Set<String> = emptySet(),
    )

    /** What an import would change, shown before it is applied. */
    data class Preview(
        val settingsCount: Int,
        val changedProtectionKeys: List<String>,
        val automations: AutomationSummary,
    ) {
        val needsAttention: Boolean get() = changedProtectionKeys.isNotEmpty() || automations.privileged > 0
    }

    /**
     * Converts an exported value (Gson reads numbers as Double and sets as lists) to the type
     * SharedPreferences stores, or null if it does not match [type].
     */
    fun toPrefValue(
        type: String?,
        value: Any?,
    ): Any? =
        try {
            when (type) {
                "Boolean" -> value as Boolean
                "Int" -> (value as Number).toInt()
                "Long" -> (value as Number).toLong()
                "Float" -> (value as Number).toFloat()
                "String" -> value as String
                "StringSet" -> (value as List<*>).map { it as String }.toSet()
                else -> null
            }
        } catch (e: ClassCastException) {
            null
        }

    /**
     * An imported value must have the type already stored under its key; a different type would
     * make the app's getters throw (ClassCastException) every time the setting is read.
     */
    fun isCompatible(
        current: Any?,
        imported: Any,
    ): Boolean =
        when (current) {
            null -> true
            is Boolean -> imported is Boolean
            is Int -> imported is Int
            is Long -> imported is Long
            is Float -> imported is Float
            is String -> imported is String
            is Set<*> -> imported is Set<*>
            else -> false
        }

    /**
     * True if importing [imported] over [current] changes a protection setting. A setting that was
     * never stored is off/empty, so importing false or an empty list for it changes nothing.
     */
    fun isProtectionChange(
        current: Any?,
        imported: Any,
    ): Boolean {
        if (current == null) {
            return when (imported) {
                is Boolean -> imported
                is String -> imported.isNotEmpty()
                is Set<*> -> imported.isNotEmpty()
                else -> true
            }
        }
        return current != imported
    }

    /** Counts automations in [json] (the "saved_automations" list) that contain privileged actions. */
    fun summarizeAutomations(json: String?): AutomationSummary {
        if (json.isNullOrBlank()) return AutomationSummary()
        val automations =
            try {
                JsonParser.parseString(json).takeIf { it.isJsonArray }?.asJsonArray
            } catch (e: RuntimeException) {
                null
            } ?: return AutomationSummary()

        var privileged = 0
        val privilegedTypes = mutableSetOf<String>()
        automations.forEach { automation ->
            val types = mutableSetOf<String>()
            collectTypes(automation, types)
            val hits = types.intersect(PRIVILEGED_ACTION_TYPES.keys)
            if (hits.isNotEmpty()) {
                privileged++
                privilegedTypes += hits
            }
        }
        return AutomationSummary(automations.size(), privileged, privilegedTypes)
    }

    // Actions are stored with a "type" discriminator, also inside nested actions
    private fun collectTypes(
        element: JsonElement,
        out: MutableSet<String>,
    ) {
        when {
            element.isJsonObject ->
                element.asJsonObject.entrySet().forEach { (key, value) ->
                    if (key == "type" && value.isJsonPrimitive && value.asJsonPrimitive.isString) {
                        out += value.asString
                    } else {
                        collectTypes(value, out)
                    }
                }

            element.isJsonArray -> element.asJsonArray.forEach { collectTypes(it, out) }
        }
    }
}

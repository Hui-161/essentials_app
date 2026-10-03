package com.sameerasw.essentials.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ConfigImportPolicyTest {
    @Test
    fun credentialsAndRecordedStateAreLocalOnly() {
        assertTrue(ConfigImportPolicy.isLocalOnly(SettingsRepository.KEY_GITHUB_ACCESS_TOKEN))
        assertTrue(ConfigImportPolicy.isLocalOnly(SettingsRepository.KEY_SHIZUKU_AUTH_TOKEN))
        assertTrue(ConfigImportPolicy.isLocalOnly(SettingsRepository.KEY_UNSPLASH_ACCESS_KEY))
        assertTrue(ConfigImportPolicy.isLocalOnly("weather_api_key_legacy"))
        assertTrue(ConfigImportPolicy.isLocalOnly(SettingsRepository.KEY_SHUT_UP_ORIGINAL_SETTINGS))
        assertTrue(ConfigImportPolicy.isLocalOnly("mac_battery_level"))
        assertFalse(ConfigImportPolicy.isLocalOnly(SettingsRepository.KEY_APP_LOCK_ENABLED))
        assertFalse(ConfigImportPolicy.isLocalOnly("haptic_feedback_type"))
    }

    @Test
    fun exportedValuesAreConvertedToPreferenceTypes() {
        assertEquals(3, ConfigImportPolicy.toPrefValue("Int", 3.0))
        assertEquals(5L, ConfigImportPolicy.toPrefValue("Long", 5.0))
        assertEquals(0.5f, ConfigImportPolicy.toPrefValue("Float", 0.5))
        assertEquals(true, ConfigImportPolicy.toPrefValue("Boolean", true))
        assertEquals(setOf("a", "b"), ConfigImportPolicy.toPrefValue("StringSet", listOf("a", "b")))
        assertNull(ConfigImportPolicy.toPrefValue("Boolean", "true"))
        assertNull(ConfigImportPolicy.toPrefValue("Int", "1"))
        assertNull(ConfigImportPolicy.toPrefValue("StringSet", listOf(1.0)))
        assertNull(ConfigImportPolicy.toPrefValue("Unknown", 1.0))
        assertNull(ConfigImportPolicy.toPrefValue(null, 1.0))
    }

    @Test
    fun importedTypeMustMatchStoredType() {
        assertTrue(ConfigImportPolicy.isCompatible(null, "anything"))
        assertTrue(ConfigImportPolicy.isCompatible(true, false))
        assertTrue(ConfigImportPolicy.isCompatible(1, 2))
        assertTrue(ConfigImportPolicy.isCompatible(setOf("a"), setOf("b")))
        assertFalse(ConfigImportPolicy.isCompatible(true, "false"))
        assertFalse(ConfigImportPolicy.isCompatible(1, 1L))
        assertFalse(ConfigImportPolicy.isCompatible("json", setOf("a")))
    }

    @Test
    fun protectionChangeIgnoresDefaultsOfUnsetSettings() {
        assertFalse(ConfigImportPolicy.isProtectionChange(null, false))
        assertFalse(ConfigImportPolicy.isProtectionChange(null, emptySet<String>()))
        assertFalse(ConfigImportPolicy.isProtectionChange(true, true))
        assertTrue(ConfigImportPolicy.isProtectionChange(null, true))
        assertTrue(ConfigImportPolicy.isProtectionChange(true, false))
        assertTrue(ConfigImportPolicy.isProtectionChange(setOf("com.bank"), emptySet<String>()))
    }

    @Test
    fun automationsWithSystemActionsAreCounted() {
        val json =
            """
            [
              {"id":"1","type":"TRIGGER","trigger":{"type":"ScreenOff"},
               "actions":[{"type":"TurnOnFlashlight"}]},
              {"id":"2","type":"STATE","state":{"type":"Charging"},
               "entryAction":{"type":"CustomSettings","entries":[{"table":"SECURE","key":"x","value":"1"}]},
               "exitAction":{"type":"Keyboard","inputMethodId":"other/.Ime"}},
              {"id":"3","type":"TRIGGER","actions":[{"type":"SometimesEssentials",
               "actions":[{"type":"FreezeApps","packageNames":["com.example"]}]}]}
            ]
            """.trimIndent()

        val summary = ConfigImportPolicy.summarizeAutomations(json)

        assertEquals(3, summary.total)
        assertEquals(2, summary.privileged)
        assertEquals(setOf("CustomSettings", "Keyboard", "FreezeApps"), summary.privilegedActionTypes)
    }

    @Test
    fun unreadableAutomationsGiveEmptySummary() {
        assertEquals(ConfigImportPolicy.AutomationSummary(), ConfigImportPolicy.summarizeAutomations(null))
        assertEquals(ConfigImportPolicy.AutomationSummary(), ConfigImportPolicy.summarizeAutomations("not json ["))
        assertEquals(ConfigImportPolicy.AutomationSummary(), ConfigImportPolicy.summarizeAutomations("""{"id":"1"}"""))
    }
}

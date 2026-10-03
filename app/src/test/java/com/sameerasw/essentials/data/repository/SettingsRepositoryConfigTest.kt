package com.sameerasw.essentials.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.gson.Gson
import com.sameerasw.essentials.external.SettingsExternalHandler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], application = android.app.Application::class)
class SettingsRepositoryConfigTest {
    private lateinit var context: Context

    private val prefs get() = context.getSharedPreferences(SettingsRepository.PREFS_NAME, Context.MODE_PRIVATE)
    private val secrets get() = context.getSharedPreferences(SettingsRepository.SECRETS_PREFS_NAME, Context.MODE_PRIVATE)

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        (ConfigImportPolicy.PREF_FILES + SettingsRepository.SECRETS_PREFS_NAME + "foreign_prefs").forEach {
            context.getSharedPreferences(it, Context.MODE_PRIVATE).edit().clear().commit()
        }
        SettingsRepository.secretsMigrated = false
    }

    private fun config(vararg files: Pair<String, Map<String, Pair<String, Any>>>): String =
        Gson().toJson(
            files.associate { (file, entries) ->
                file to entries.mapValues { (_, item) -> mapOf("type" to item.first, "value" to item.second) }
            },
        )

    @Test
    fun tokensMoveOutOfTheSettingsFile() {
        prefs
            .edit()
            .putString(SettingsRepository.KEY_GITHUB_ACCESS_TOKEN, "token-a")
            .putString(SettingsRepository.KEY_SHIZUKU_AUTH_TOKEN, "token-b")
            .putString(SettingsRepository.KEY_UNSPLASH_ACCESS_KEY, "token-c")
            .putBoolean("some_feature", true)
            .commit()

        val repository = SettingsRepository(context)

        assertEquals("token-a", repository.getGitHubToken())
        assertEquals("token-b", repository.getShizukuAuthToken())
        assertEquals("token-c", repository.getUnsplashAccessKey())
        assertFalse(prefs.contains(SettingsRepository.KEY_GITHUB_ACCESS_TOKEN))
        assertFalse(prefs.contains(SettingsRepository.KEY_SHIZUKU_AUTH_TOKEN))
        assertFalse(prefs.contains(SettingsRepository.KEY_UNSPLASH_ACCESS_KEY))
        assertEquals("token-a", secrets.getString(SettingsRepository.KEY_GITHUB_ACCESS_TOKEN, null))
        assertTrue(prefs.getBoolean("some_feature", false))
    }

    @Test
    fun newTokensAreStoredOnlyInTheSecretsFile() {
        val repository = SettingsRepository(context)
        repository.saveGitHubToken("token-a")
        repository.setShizukuAuthToken("token-b")

        assertFalse(prefs.contains(SettingsRepository.KEY_GITHUB_ACCESS_TOKEN))
        assertFalse(prefs.contains(SettingsRepository.KEY_SHIZUKU_AUTH_TOKEN))
        assertNull(SettingsExternalHandler().onQuery(context, SettingsRepository.KEY_GITHUB_ACCESS_TOKEN, null))
    }

    @Test
    fun exportLeavesOutCredentialsAndRecordedState() {
        val repository = SettingsRepository(context)
        repository.saveGitHubToken("token-a")
        prefs
            .edit()
            .putString(SettingsRepository.KEY_SHUT_UP_ORIGINAL_SETTINGS, """{"secure:x":"1"}""")
            .putBoolean("some_feature", true)
            .commit()

        val json = repository.getAllConfigsAsJsonString()

        assertFalse(json.contains("token-a"))
        assertFalse(json.contains(SettingsRepository.KEY_SHUT_UP_ORIGINAL_SETTINGS))
        assertTrue(json.contains("some_feature"))
    }

    @Test
    fun importWritesOnlyKnownFilesAndSafeKeys() {
        prefs.edit().putBoolean("some_feature", false).commit()
        val repository = SettingsRepository(context)
        val json =
            config(
                SettingsRepository.PREFS_NAME to
                    mapOf(
                        "some_feature" to ("Boolean" to true),
                        "new_number" to ("Int" to 7.0),
                        SettingsRepository.KEY_GITHUB_ACCESS_TOKEN to ("String" to "injected"),
                        SettingsRepository.KEY_SHUT_UP_ORIGINAL_SETTINGS to
                            ("String" to """{"secure:enabled_accessibility_services":"x/.Y"}"""),
                    ),
                "foreign_prefs" to mapOf("anything" to ("Boolean" to true)),
            )

        assertTrue(repository.importConfigJson(json, keepPrefs = true))

        assertTrue(prefs.getBoolean("some_feature", false))
        assertEquals(7, prefs.getInt("new_number", 0))
        assertNull(repository.getGitHubToken())
        assertFalse(prefs.contains(SettingsRepository.KEY_GITHUB_ACCESS_TOKEN))
        assertFalse(prefs.contains(SettingsRepository.KEY_SHUT_UP_ORIGINAL_SETTINGS))
        assertTrue(context.getSharedPreferences("foreign_prefs", Context.MODE_PRIVATE).all.isEmpty())
    }

    @Test
    fun importSkipsValuesOfAnotherType() {
        prefs.edit().putBoolean("some_feature", true).commit()
        val repository = SettingsRepository(context)
        val json = config(SettingsRepository.PREFS_NAME to mapOf("some_feature" to ("String" to "oops")))

        repository.importConfigJson(json, keepPrefs = true)

        // getBoolean would throw ClassCastException if a String had been stored
        assertTrue(prefs.getBoolean("some_feature", false))
    }

    @Test
    fun replaceKeepsProtectionSettingsTheFileLeavesOut() {
        prefs
            .edit()
            .putBoolean(SettingsRepository.KEY_APP_LOCK_ENABLED, true)
            .putBoolean("other_feature", true)
            .commit()
        val repository = SettingsRepository(context)
        val json = config(SettingsRepository.PREFS_NAME to mapOf("some_feature" to ("Boolean" to true)))

        repository.importConfigJson(json, keepPrefs = false)

        assertTrue(prefs.getBoolean(SettingsRepository.KEY_APP_LOCK_ENABLED, false))
        assertFalse(prefs.contains("other_feature"))
        assertTrue(prefs.getBoolean("some_feature", false))
    }

    @Test
    fun previewListsProtectionChangesAndSystemAutomations() {
        prefs.edit().putBoolean(SettingsRepository.KEY_APP_LOCK_ENABLED, true).commit()
        val repository = SettingsRepository(context)
        val automations = """[{"id":"1","type":"TRIGGER","actions":[{"type":"CustomSettings","entries":[]}]}]"""
        val json =
            config(
                SettingsRepository.PREFS_NAME to
                    mapOf(
                        SettingsRepository.KEY_APP_LOCK_ENABLED to ("Boolean" to false),
                        "some_feature" to ("Boolean" to true),
                    ),
                ConfigImportPolicy.AUTOMATIONS_FILE to
                    mapOf(ConfigImportPolicy.AUTOMATIONS_KEY to ("String" to automations)),
            )

        val preview = repository.previewConfigImport(json)

        assertNotNull(preview)
        assertEquals(3, preview!!.settingsCount)
        assertEquals(listOf(SettingsRepository.KEY_APP_LOCK_ENABLED), preview.changedProtectionKeys)
        assertEquals(1, preview.automations.privileged)
        assertTrue(preview.needsAttention)
    }

    @Test
    fun previewRejectsFilesThatAreNoConfiguration() {
        val repository = SettingsRepository(context)

        assertNull(repository.previewConfigImport("[1, 2, 3]"))
        assertNull(repository.previewConfigImport(config("foreign_prefs" to mapOf("a" to ("Boolean" to true)))))
    }

    @Test
    fun oversizedFilesAreNotRead() {
        val repository = SettingsRepository(context)
        val big = ByteArray(ConfigImportPolicy.MAX_FILE_BYTES + 1) { 'a'.code.toByte() }

        assertNull(repository.readConfigFile(big.inputStream()))
        assertEquals("{}", repository.readConfigFile("{}".byteInputStream()))
    }
}

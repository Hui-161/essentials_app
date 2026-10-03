package com.sameerasw.essentials.utils

import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import androidx.test.core.app.ApplicationProvider
import com.sameerasw.essentials.ShortcutHandlerActivity
import com.sameerasw.essentials.data.repository.ConfigImportPolicy
import com.sameerasw.essentials.data.repository.SettingsRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], application = android.app.Application::class)
class ShortcutTokenTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        listOf(SettingsRepository.PREFS_NAME, SettingsRepository.SECRETS_PREFS_NAME).forEach {
            context.getSharedPreferences(it, Context.MODE_PRIVATE).edit().clear().commit()
        }
    }

    @Test
    fun tokenIsStableSecretAndNeverExported() {
        val token = SettingsRepository(context).getShortcutToken()

        assertEquals(32, token.length)
        assertEquals(token, SettingsRepository(context).getShortcutToken())
        assertTrue(ConfigImportPolicy.isLocalOnly(SettingsRepository.KEY_SHORTCUT_TOKEN))
        assertFalse(
            context
                .getSharedPreferences(SettingsRepository.PREFS_NAME, Context.MODE_PRIVATE)
                .contains(SettingsRepository.KEY_SHORTCUT_TOKEN),
        )
    }

    @Test
    fun onlyTheInstallationsOwnTokenIsAccepted() {
        val token = SettingsRepository(context).getShortcutToken()

        assertTrue(ShortcutUtil.isValidShortcutToken(context, token))
        assertFalse(ShortcutUtil.isValidShortcutToken(context, null))
        assertFalse(ShortcutUtil.isValidShortcutToken(context, ""))
        assertFalse(ShortcutUtil.isValidShortcutToken(context, token.reversed()))
    }

    @Test
    fun pinnedShortcutsWithoutTokenGetTheCurrentOne() {
        val shortcutManager = context.getSystemService(ShortcutManager::class.java)
        val oldIntent =
            Intent(context, ShortcutHandlerActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                putExtra(ShortcutUtil.EXTRA_PACKAGE_NAME, "com.example.app")
            }
        shortcutManager.requestPinShortcut(
            ShortcutInfo
                .Builder(context, "com.example.app")
                .setShortLabel("Example")
                .setIntent(oldIntent)
                .build(),
            null,
        )

        ShortcutUtil.refreshPinnedAppShortcuts(context)

        val intent = shortcutManager.pinnedShortcuts.single { it.id == "com.example.app" }.intent!!
        assertEquals("com.example.app", intent.getStringExtra(ShortcutUtil.EXTRA_PACKAGE_NAME))
        assertTrue(ShortcutUtil.isValidShortcutToken(context, intent.getStringExtra(ShortcutUtil.EXTRA_SHORTCUT_TOKEN)))
    }
}

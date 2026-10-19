package com.sameerasw.essentials.services.handlers

import android.accessibilityservice.AccessibilityService
import android.app.KeyguardManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.domain.diy.Action
import com.sameerasw.essentials.services.automation.executors.CombinedActionExecutor
import com.sameerasw.essentials.services.receivers.FlashlightActionReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowCameraCharacteristics

/**
 * Simulates turning the flashlight on while the phone is locked, with pocket detection, global
 * controls and start brightness 100 %. Covers the app's logic only: the real camera, lock screen
 * and island window are not part of Robolectric.
 */
@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [36],
    application = android.app.Application::class,
    shadows = [ShadowTorchCameraManager::class],
)
class FlashlightLockScreenTest {
    class TestService : AccessibilityService() {
        override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

        override fun onInterrupt() {}
    }

    private lateinit var service: TestService
    private lateinit var scope: CoroutineScope
    private lateinit var handler: FlashlightHandler
    private val defaultDelivery = FlashlightActionReceiver.deliverToRunningService

    @Before
    fun setUp() {
        ShadowTorchCameraManager.clear()
        service = Robolectric.setupService(TestService::class.java)
        service.getSharedPreferences(SettingsRepository.PREFS_NAME, Context.MODE_PRIVATE).edit().clear().commit()

        val characteristics = ShadowCameraCharacteristics.newCameraCharacteristics()
        shadowOf(characteristics).apply {
            set(CameraCharacteristics.FLASH_INFO_AVAILABLE, true)
            set(CameraCharacteristics.LENS_FACING, CameraCharacteristics.LENS_FACING_BACK)
            set(CameraCharacteristics.FLASH_INFO_STRENGTH_MAXIMUM_LEVEL, ShadowTorchCameraManager.MAX_LEVEL)
            set(CameraCharacteristics.FLASH_INFO_STRENGTH_DEFAULT_LEVEL, ShadowTorchCameraManager.DEFAULT_LEVEL)
        }
        val cameraManager = service.getSystemService(CameraManager::class.java)
        (shadowOf(cameraManager) as ShadowTorchCameraManager).addCamera(CAMERA, characteristics)
        ShadowTorchCameraManager.torch[CAMERA] = false

        val keyguard = service.getSystemService(KeyguardManager::class.java)
        shadowOf(keyguard).setKeyguardLocked(true)
        shadowOf(keyguard).setIsDeviceLocked(true)

        scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        handler = FlashlightHandler(service, scope)
        handler.register()
    }

    @After
    fun tearDown() {
        FlashlightActionReceiver.deliverToRunningService = defaultDelivery
        handler.unregister()
        scope.cancel()
    }

    private fun userSettings(startBrightness: String = FlashlightPolicy.START_MAX) {
        service
            .getSharedPreferences(SettingsRepository.PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(SettingsRepository.KEY_FLASHLIGHT_POCKET_TURN_OFF_ENABLED, true)
            .putBoolean(SettingsRepository.KEY_FLASHLIGHT_GLOBAL_ENABLED, true)
            .putString(SettingsRepository.KEY_FLASHLIGHT_START_BRIGHTNESS, startBrightness)
            .commit()
    }

    private fun torchOn() = ShadowTorchCameraManager.torch[CAMERA] == true

    private fun level() = ShadowTorchCameraManager.strength[CAMERA] ?: ShadowTorchCameraManager.DEFAULT_LEVEL

    /** Runs main-thread work and waits for fades on background threads. */
    private fun settle(until: () -> Boolean = { false }) {
        repeat(150) {
            shadowOf(Looper.getMainLooper()).idle()
            if (until()) return
            Thread.sleep(20)
        }
    }

    private fun flashlightNotificationShown() =
        shadowOf(service.getSystemService(NotificationManager::class.java))
            .allNotifications
            .isNotEmpty()

    @Test
    fun tileOrButtonOnTheLockScreenTurnsTheTorchOnAtFullBrightness() {
        userSettings()
        FlashlightActionReceiver.deliverToRunningService = { handler.handleIntent(it); true }

        // Essentials tile, volume button and widgets all end in this action
        FlashlightActionReceiver().onReceive(service, Intent(FlashlightActionReceiver.ACTION_TOGGLE))
        settle()

        assertTrue(torchOn())
        assertTrue(handler.isTorchOn)
        assertEquals(ShadowTorchCameraManager.MAX_LEVEL, level())
        assertTrue(flashlightNotificationShown())
    }

    @Test
    fun notchGestureOnTheLockScreenTurnsTheTorchOnAndKeepsItOn() {
        userSettings()

        // Island gestures run this action (CompactGestureController -> CombinedActionExecutor)
        scope.launch { CombinedActionExecutor.execute(service, Action.ToggleFlashlight) }
        settle { level() == ShadowTorchCameraManager.MAX_LEVEL }
        settle() // nothing turns it off again afterwards

        assertTrue(torchOn())
        assertTrue(handler.isTorchOn)
        assertEquals(ShadowTorchCameraManager.MAX_LEVEL, level())
        assertTrue(flashlightNotificationShown())
    }

    @Test
    fun secondToggleTurnsTheTorchOffAgain() {
        userSettings()
        FlashlightActionReceiver.deliverToRunningService = { handler.handleIntent(it); true }

        FlashlightActionReceiver().onReceive(service, Intent(FlashlightActionReceiver.ACTION_TOGGLE))
        settle()
        FlashlightActionReceiver().onReceive(service, Intent(FlashlightActionReceiver.ACTION_TOGGLE))
        settle()

        assertTrue(!torchOn())
        assertTrue(!handler.isTorchOn)
    }

    @Test
    fun islandIsHiddenOnTheLockScreenUnlessShowWhenIsChanged() {
        // Fresh install (e.g. the separate debug app): "Show when" falls back to "Unlocked", so the
        // island has no content on the lock screen and its compact gestures are not active
        assertEquals(
            SettingsRepository.ISLAND_SHOW_WHEN_UNLOCKED,
            SettingsRepository(service).getIslandShowWhen(),
        )
        SettingsRepository(service).setIslandShowWhen(SettingsRepository.ISLAND_SHOW_WHEN_SCREEN_ON)
        assertEquals(
            SettingsRepository.ISLAND_SHOW_WHEN_SCREEN_ON,
            SettingsRepository(service).getIslandShowWhen(),
        )
    }

    private companion object {
        const val CAMERA = "0"
    }
}

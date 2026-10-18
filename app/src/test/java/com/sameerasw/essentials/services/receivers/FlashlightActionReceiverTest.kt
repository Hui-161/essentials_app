package com.sameerasw.essentials.services.receivers

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.sameerasw.essentials.services.tiles.ScreenOffAccessibilityService
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], application = android.app.Application::class)
class FlashlightActionReceiverTest {
    private lateinit var context: Context
    private val defaultDelivery = FlashlightActionReceiver.deliverToRunningService
    private val delivered = mutableListOf<Intent>()

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        shadowOf(context as android.app.Application).clearStartedServices()
    }

    @After
    fun tearDown() {
        FlashlightActionReceiver.deliverToRunningService = defaultDelivery
    }

    private fun receive(intent: Intent) = FlashlightActionReceiver().onReceive(context, intent)

    private fun startedService(): Intent? = shadowOf(context as android.app.Application).nextStartedService

    @Test
    fun notificationButtonsReachTheRunningServiceWithoutStartService() {
        FlashlightActionReceiver.deliverToRunningService = { delivered += it; true }

        receive(Intent(FlashlightActionReceiver.ACTION_INCREASE))
        receive(Intent(FlashlightActionReceiver.ACTION_DECREASE))
        receive(Intent(FlashlightActionReceiver.ACTION_OFF))

        assertEquals(
            listOf(
                FlashlightActionReceiver.ACTION_INCREASE,
                FlashlightActionReceiver.ACTION_DECREASE,
                FlashlightActionReceiver.ACTION_OFF,
            ),
            delivered.map { it.action },
        )
        assertNull(startedService())
    }

    @Test
    fun extrasArePassedOn() {
        FlashlightActionReceiver.deliverToRunningService = { delivered += it; true }

        receive(
            Intent(FlashlightActionReceiver.ACTION_SET_INTENSITY)
                .putExtra(FlashlightActionReceiver.EXTRA_INTENSITY, 3),
        )
        receive(
            Intent(FlashlightActionReceiver.ACTION_START_STROBE)
                .putExtra(FlashlightActionReceiver.EXTRA_STROBE_SPEED, 7f)
                .putExtra(FlashlightActionReceiver.EXTRA_STROBE_FADE, true),
        )

        assertEquals(3, delivered[0].getIntExtra(FlashlightActionReceiver.EXTRA_INTENSITY, -1))
        assertEquals(7f, delivered[1].getFloatExtra(FlashlightActionReceiver.EXTRA_STROBE_SPEED, -1f), 0f)
        assertTrue(delivered[1].getBooleanExtra(FlashlightActionReceiver.EXTRA_STROBE_FADE, false))
        assertFalse(delivered[1].hasExtra(FlashlightActionReceiver.EXTRA_INTENSITY))
    }

    @Test
    fun withoutRunningServiceItFallsBackToStartService() {
        FlashlightActionReceiver.deliverToRunningService = { false }

        receive(Intent(FlashlightActionReceiver.ACTION_TOGGLE))

        val started = startedService()
        assertEquals(FlashlightActionReceiver.ACTION_TOGGLE, started?.action)
        assertEquals(ScreenOffAccessibilityService::class.java.name, started?.component?.className)
    }

    @Test
    fun defaultDeliveryReportsAMissingService() {
        ScreenOffAccessibilityService.instance = null

        assertFalse(defaultDelivery(Intent(FlashlightActionReceiver.ACTION_INCREASE)))
    }

    @Test
    fun intentsWithoutActionAreIgnored() {
        FlashlightActionReceiver.deliverToRunningService = { delivered += it; true }

        receive(Intent())

        assertTrue(delivered.isEmpty())
        assertNull(startedService())
    }
}

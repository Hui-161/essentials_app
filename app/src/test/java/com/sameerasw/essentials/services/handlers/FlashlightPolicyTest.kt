package com.sameerasw.essentials.services.handlers

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FlashlightPolicyTest {
    private fun pocketTurnOff(
        gravityZ: Float?,
        ignoreWhenFlat: Boolean = true,
        proximityBlocked: Boolean = true,
        torchOn: Boolean = true,
        enabled: Boolean = true,
    ) = FlashlightPolicy.shouldPocketTurnOff(enabled, torchOn, proximityBlocked, ignoreWhenFlat, gravityZ)

    @Test
    fun phoneOnTheTableKeepsTheTorchOn() {
        // Display down (torch pointing up) and display up
        assertFalse(pocketTurnOff(gravityZ = -9.81f))
        assertFalse(pocketTurnOff(gravityZ = 9.81f))
        // Slightly tilted on an uneven surface
        assertFalse(pocketTurnOff(gravityZ = -8.5f))
    }

    @Test
    fun phoneInThePocketTurnsTheTorchOff() {
        // Upright in a trouser or jacket pocket
        assertTrue(pocketTurnOff(gravityZ = 0.3f))
        assertTrue(pocketTurnOff(gravityZ = -4f))
        // Exactly at the threshold does not count as flat
        assertTrue(pocketTurnOff(gravityZ = FlashlightPolicy.FLAT_GRAVITY_Z))
        assertTrue(pocketTurnOff(gravityZ = -FlashlightPolicy.FLAT_GRAVITY_Z))
    }

    @Test
    fun flatCheckCanBeSwitchedOff() {
        assertTrue(pocketTurnOff(gravityZ = -9.81f, ignoreWhenFlat = false))
    }

    @Test
    fun withoutOrientationReadingTheProximitySensorDecides() {
        assertTrue(pocketTurnOff(gravityZ = null))
    }

    @Test
    fun nothingIsTurnedOffWithoutAllPreconditions() {
        assertFalse(pocketTurnOff(gravityZ = 0f, proximityBlocked = false))
        assertFalse(pocketTurnOff(gravityZ = 0f, torchOn = false))
        assertFalse(pocketTurnOff(gravityZ = 0f, enabled = false))
    }

    @Test
    fun accelerometerValuesAreSmoothed() {
        assertEquals(9.81f, FlashlightPolicy.lowPass(null, 9.81f), 0.001f)
        // A single jolt does not make a flat phone "upright", a lasting change does
        assertTrue(FlashlightPolicy.isFlat(FlashlightPolicy.lowPass(-9.81f, -5f)))
        var z: Float? = -9.81f
        repeat(10) { z = FlashlightPolicy.lowPass(z, 0f) }
        assertFalse(FlashlightPolicy.isFlat(z))
    }

    @Test
    fun startModeKeepsTheOldMeaningOfGlobalControls() {
        assertEquals(FlashlightPolicy.START_DEFAULT, FlashlightPolicy.startMode(null, globalEnabled = false))
        assertEquals(FlashlightPolicy.START_LAST, FlashlightPolicy.startMode(null, globalEnabled = true))
        assertEquals(FlashlightPolicy.START_MAX, FlashlightPolicy.startMode("max", globalEnabled = true))
        assertEquals(FlashlightPolicy.START_DEFAULT, FlashlightPolicy.startMode("default", globalEnabled = true))
        assertEquals(FlashlightPolicy.START_DEFAULT, FlashlightPolicy.startMode("unknown", globalEnabled = false))
    }

    @Test
    fun startLevelFollowsTheSetting() {
        // Device with 5 levels and system default 4 (80 %)
        assertEquals(4, FlashlightPolicy.startLevel(FlashlightPolicy.START_DEFAULT, 4, 5, 2))
        assertEquals(5, FlashlightPolicy.startLevel(FlashlightPolicy.START_MAX, 4, 5, 2))
        assertEquals(2, FlashlightPolicy.startLevel(FlashlightPolicy.START_LAST, 4, 5, 2))
        // Nothing remembered yet or out of range: system default
        assertEquals(4, FlashlightPolicy.startLevel(FlashlightPolicy.START_LAST, 4, 5, 0))
        assertEquals(4, FlashlightPolicy.startLevel(FlashlightPolicy.START_LAST, 4, 5, 9))
        // Without intensity support everything is level 1
        assertEquals(1, FlashlightPolicy.startLevel(FlashlightPolicy.START_MAX, 1, 1, 0))
    }

    @Test
    fun plusAndMinusMoveInFifthsUpToTheLimits() {
        assertEquals(5, FlashlightPolicy.adjustedLevel(4, 5, increase = true))
        assertEquals(3, FlashlightPolicy.adjustedLevel(4, 5, increase = false))
        assertEquals(45, FlashlightPolicy.adjustedLevel(36, 45, increase = true))
        assertEquals(27, FlashlightPolicy.adjustedLevel(36, 45, increase = false))
        assertEquals(1, FlashlightPolicy.adjustedLevel(5, 45, increase = false))
        assertEquals(64, FlashlightPolicy.adjustedLevel(60, 64, increase = true))
    }

    @Test
    fun noStepBeyondTheLimits() {
        assertNull(FlashlightPolicy.adjustedLevel(5, 5, increase = true))
        assertNull(FlashlightPolicy.adjustedLevel(1, 5, increase = false))
        assertNull(FlashlightPolicy.adjustedLevel(1, 1, increase = true))
    }

    @Test
    fun percentageIsRoundedNotTruncated() {
        assertEquals(80, FlashlightPolicy.percent(4, 5))
        assertEquals(100, FlashlightPolicy.percent(5, 5))
        assertEquals(33, FlashlightPolicy.percent(1, 3))
        assertEquals(67, FlashlightPolicy.percent(2, 3))
        assertEquals(100, FlashlightPolicy.percent(1, 1))
        assertEquals(100, FlashlightPolicy.percent(1, 0))
    }

    @Test
    fun overheatingPreventionLimit() {
        assertEquals(3, FlashlightPolicy.overheatLimit(5))
        assertEquals(33, FlashlightPolicy.overheatLimit(45))
        assertEquals(1, FlashlightPolicy.overheatLimit(1))
    }
}

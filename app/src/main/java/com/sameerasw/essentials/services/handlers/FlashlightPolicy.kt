/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Background Services & Receivers
 * File: FlashlightPolicy.kt
 * Description: Pure flashlight decisions (pocket turn-off, start level, intensity steps).
 */

package com.sameerasw.essentials.services.handlers

import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Decisions of [FlashlightHandler] and the pocket detection without Android calls, so they can be
 * unit tested.
 */
object FlashlightPolicy {
    /**
     * Gravity along the display normal (m/s², 9.81 when lying flat, negative with the display down)
     * above which the phone counts as lying flat, i.e. tilted less than about 35° from horizontal.
     */
    const val FLAT_GRAVITY_Z = 8f

    const val START_DEFAULT = "default"
    const val START_MAX = "max"
    const val START_LAST = "last"

    /** Overheating prevention lowers the torch to this share of the maximum level. */
    const val OVERHEAT_LIMIT_FACTOR = 0.75f

    fun isFlat(gravityZ: Float?): Boolean = gravityZ != null && abs(gravityZ) > FLAT_GRAVITY_Z

    /**
     * The torch is turned off for a covered proximity sensor, unless the phone lies on a table
     * (display up or down) and [ignoreWhenFlat] is set. Without an orientation reading (no sensor)
     * the proximity sensor alone decides, as before.
     */
    fun shouldPocketTurnOff(
        enabled: Boolean,
        torchOn: Boolean,
        proximityBlocked: Boolean,
        ignoreWhenFlat: Boolean,
        gravityZ: Float?,
    ): Boolean = enabled && torchOn && proximityBlocked && !(ignoreWhenFlat && isFlat(gravityZ))

    /** Smooths raw accelerometer values (gravity plus movement) when there is no gravity sensor. */
    fun lowPass(
        previous: Float?,
        value: Float,
        alpha: Float = 0.8f,
    ): Float = if (previous == null) value else alpha * previous + (1 - alpha) * value

    /**
     * Start brightness setting. Without a stored choice, "Global controls" keeps its old meaning
     * (start with the last used level).
     */
    fun startMode(
        stored: String?,
        globalEnabled: Boolean,
    ): String =
        when (stored) {
            START_DEFAULT, START_MAX, START_LAST -> stored
            else -> if (globalEnabled) START_LAST else START_DEFAULT
        }

    fun startLevel(
        mode: String,
        defaultLevel: Int,
        maxLevel: Int,
        lastLevel: Int,
    ): Int {
        val max = maxLevel.coerceAtLeast(1)
        val default = defaultLevel.coerceIn(1, max)
        return when (mode) {
            START_MAX -> max
            START_LAST -> if (lastLevel in 1..max) lastLevel else default
            else -> default
        }
    }

    /** Five steps from minimum to maximum. */
    fun intensityStep(maxLevel: Int): Int = maxOf(1, maxLevel / 5)

    /** Level after one +/- step, or null if [current] is already at the limit. */
    fun adjustedLevel(
        current: Int,
        maxLevel: Int,
        increase: Boolean,
    ): Int? {
        val max = maxLevel.coerceAtLeast(1)
        val level = current.coerceIn(1, max)
        if (increase && level >= max || !increase && level <= 1) return null
        val step = intensityStep(max)
        return if (increase) (level + step).coerceAtMost(max) else (level - step).coerceAtLeast(1)
    }

    fun percent(
        level: Int,
        maxLevel: Int,
    ): Int = (level * 100f / maxLevel.coerceAtLeast(1)).roundToInt().coerceIn(0, 100)

    fun overheatLimit(maxLevel: Int): Int = (maxLevel * OVERHEAT_LIMIT_FACTOR).toInt().coerceAtLeast(1)
}

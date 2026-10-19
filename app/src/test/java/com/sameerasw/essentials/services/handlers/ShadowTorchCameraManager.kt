package com.sameerasw.essentials.services.handlers

import android.hardware.camera2.CameraManager
import android.os.Handler
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.shadows.ShadowCameraManager
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Torch with strength levels for Robolectric: Robolectric's own shadow has no strength API and
 * does not report the current state on registration, which the real CameraManager does.
 * Callbacks run synchronously instead of on the given handler.
 */
@Implements(CameraManager::class)
class ShadowTorchCameraManager : ShadowCameraManager() {
    companion object {
        const val MAX_LEVEL = 5
        const val DEFAULT_LEVEL = 4

        val callbacks = CopyOnWriteArrayList<CameraManager.TorchCallback>()
        val torch = ConcurrentHashMap<String, Boolean>()
        val strength = ConcurrentHashMap<String, Int>()

        fun clear() {
            callbacks.clear()
            torch.clear()
            strength.clear()
        }
    }

    @Implementation
    override fun registerTorchCallback(
        callback: CameraManager.TorchCallback,
        handler: Handler?,
    ) {
        callbacks += callback
        torch.forEach { (id, on) -> callback.onTorchModeChanged(id, on) }
    }

    @Implementation
    override fun unregisterTorchCallback(callback: CameraManager.TorchCallback) {
        callbacks -= callback
    }

    @Implementation
    override fun setTorchMode(
        cameraId: String,
        enabled: Boolean,
    ) {
        val changed = torch[cameraId] != enabled
        torch[cameraId] = enabled
        // Turning off resets the level to the default, as on real devices
        if (!enabled) strength.remove(cameraId)
        if (changed) callbacks.forEach { it.onTorchModeChanged(cameraId, enabled) }
    }

    @Implementation(minSdk = 33)
    protected fun turnOnTorchWithStrengthLevel(
        cameraId: String,
        torchStrength: Int,
    ) {
        require(torchStrength in 1..MAX_LEVEL) { "level $torchStrength" }
        val wasOn = torch[cameraId] == true
        torch[cameraId] = true
        strength[cameraId] = torchStrength
        if (!wasOn) callbacks.forEach { it.onTorchModeChanged(cameraId, true) }
        callbacks.forEach { it.onTorchStrengthLevelChanged(cameraId, torchStrength) }
    }

    @Implementation(minSdk = 33)
    protected fun getTorchStrengthLevel(cameraId: String): Int = strength[cameraId] ?: DEFAULT_LEVEL
}

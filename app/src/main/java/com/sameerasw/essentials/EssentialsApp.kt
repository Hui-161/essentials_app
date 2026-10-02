/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Core Application Entrypoint
 * File: EssentialsApp.kt
 * Description: Main application entrypoint for EssentialsApp.kt.
 */

package com.sameerasw.essentials

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import com.sameerasw.essentials.utils.ShizukuUtils
import org.lsposed.hiddenapibypass.HiddenApiBypass

class EssentialsApp : Application() {
    companion object {
        lateinit var context: Context
            private set
    }

    private val securityReceiver =
        com.sameerasw.essentials.services.receivers
            .SecurityReceiver()

    override fun onCreate() {
        super.onCreate()
        context = applicationContext
        com.sameerasw.essentials.utils.HapticUtil.initialize(this)

        try {
            resources?.configuration
        } catch (e: Exception) {
        }

        try {
            HiddenApiBypass.setHiddenApiExemptions("")
        } catch (_: Throwable) {
        }

        ShizukuUtils.initialize()
        com.sameerasw.essentials.utils.CarConnectionMonitor
            .initialize(this)
        com.sameerasw.essentials.utils.LogManager
            .init(this)

        // Init Automation
        com.sameerasw.essentials.domain.diy.DIYRepository
            .init(this)
        com.sameerasw.essentials.services.automation.AutomationManager
            .init(this)
        com.sameerasw.essentials.services.CalendarSyncManager
            .init(this)
        com.sameerasw.essentials.services.DeviceInfoSyncManager
            .init(this)
        com.sameerasw.essentials.utils.ServiceUtils
            .startRequiredServices(this)

        com.sameerasw.essentials.appfunctions.AppFunctionAvailabilityManager
            .updateAvailability(this)


        val intentFilter =
            IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_USER_PRESENT)
            }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(securityReceiver, intentFilter, RECEIVER_EXPORTED)
        } else {
            registerReceiver(securityReceiver, intentFilter)
        }
    }

    override fun onTerminate() {
        super.onTerminate()
        unregisterReceiver(securityReceiver)
    }
}

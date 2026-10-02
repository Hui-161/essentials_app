/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Security Utilities
 * File: RootUtils.kt
 * Description: Helper methods for executing root shell actions and verifying superuser access.
 */

package com.sameerasw.essentials.utils

import java.io.DataOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit

object RootUtils {
    // A root manager prompt (or a hanging su) must never block the caller forever
    private const val CHECK_TIMEOUT_SECONDS = 5L
    private const val COMMAND_TIMEOUT_SECONDS = 30L

    private fun Process.exitedWithin(seconds: Long): Boolean {
        val finished = waitFor(seconds, TimeUnit.SECONDS)
        if (!finished) destroy()
        return finished && exitValue() == 0
    }

    fun isRootAvailable(): Boolean =
        try {
            Runtime.getRuntime().exec(arrayOf("sh", "-c", "which su")).exitedWithin(CHECK_TIMEOUT_SECONDS)
        } catch (e: Exception) {
            false
        }

    fun isRootPermissionGranted(): Boolean {
        // In many root managers, 'su -c id' will return 0 if granted
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "id"))
            process.outputStream.close()
            process.exitedWithin(CHECK_TIMEOUT_SECONDS)
        } catch (e: Exception) {
            false
        }
    }

    fun runCommand(command: String): Boolean {
        var process: Process? = null
        var os: DataOutputStream? = null
        return try {
            process = Runtime.getRuntime().exec("su")
            os = DataOutputStream(process.outputStream)
            os.writeBytes("$command\n")
            os.writeBytes("exit\n")
            os.flush()
            process.exitedWithin(COMMAND_TIMEOUT_SECONDS)
        } catch (
            @Suppress("UNUSED_PARAMETER") e: IOException,
        ) {
            false
        } catch (
            @Suppress("UNUSED_PARAMETER") e: InterruptedException,
        ) {
            false
        } finally {
            try {
                os?.close()
            } catch (e: Exception) {
            }
            try {
                process?.destroy()
            } catch (e: Exception) {
            }
        }
    }

    fun newProcess(cmd: Array<String>): Process? =
        try {
            // su -c takes a single shell string: quote every argument so none is read as shell syntax
            Runtime.getRuntime().exec(arrayOf("su", "-c", cmd.joinToString(" ") { ShellUtils.quote(it) }))
        } catch (e: Exception) {
            null
        }
}

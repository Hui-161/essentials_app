/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Application Activities
 * File: ActionShortcutActivity.kt
 * Description: Activity component for ActionShortcutActivity.kt.
 */

package com.sameerasw.essentials.ui.activities

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.sameerasw.essentials.domain.diy.Automation
import com.sameerasw.essentials.domain.diy.DIYRepository
import com.sameerasw.essentials.services.automation.executors.CombinedActionExecutor
import kotlinx.coroutines.launch

class ActionShortcutActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        DIYRepository.init(applicationContext)
        // Reached only through the launcher alias, which is enabled only while such an automation is on
        val automation =
            DIYRepository.automations.value.find { it.type == Automation.Type.ACTION_SHORTCUT && it.isEnabled }

        if (automation != null && automation.actions.isNotEmpty()) {
            lifecycleScope.launch {
                automation.actions.forEach { action ->
                    CombinedActionExecutor.execute(applicationContext, action)
                }
                finish()
            }
        } else {
            Toast
                .makeText(this, "No customized action shortcut configured", Toast.LENGTH_SHORT)
                .show()
            finish()
        }
    }
}

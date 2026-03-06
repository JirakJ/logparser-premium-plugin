/*
 * Copyright (c) 2026 Jakub Jirák. All rights reserved.
 * Licensed under the terms of the JetBrains Marketplace EULA.
 */
package com.jakubjirak.ui.actions

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.Messages

/** Opens the JetBrains Marketplace upgrade page in the default browser. */
class UpgradePremiumAction : AnAction() {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun actionPerformed(e: AnActionEvent) {
        val result = Messages.showYesNoDialog(
            e.project,
            "Upgrade to LogParser Pro Premium!\n\n" +
                "30-Day FREE Trial (no credit card required)\n" +
                "All Premium features unlocked\n\n" +
                "Visit the Marketplace to start your free trial?",
            "Upgrade to Premium",
            "Visit Marketplace",
            "Maybe Later",
            Messages.getInformationIcon()
        )

        if (result == Messages.YES) {
            BrowserUtil.browse(MARKETPLACE_URL)
        }
    }

    companion object {
        internal const val MARKETPLACE_URL = "https://plugins.jetbrains.com/plugin/29050-logparser-pro"
    }
}

/*
 * Copyright (c) 2026 Jakub Jirák. All rights reserved.
 * Licensed under the terms of the JetBrains Marketplace EULA.
 */
package com.jakubjirak.ui.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.Messages
import com.jakubjirak.core.LicenseManager
import com.jakubjirak.core.PremiumFeature

/** Prompts for multi-source connection (Premium) or offers a trial. */
class ConnectSourceAction : AnAction() {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun actionPerformed(e: AnActionEvent) {
        val licenseManager = LicenseManager.getInstance()

        if (!licenseManager.canUseFeature(PremiumFeature.MULTI_SOURCE)) {
            val result = Messages.showYesNoDialog(
                e.project,
                "Multi-source log aggregation is a Premium feature.\n\n" +
                    "Start your FREE 30-day trial?",
                "Premium Feature",
                "Start Trial",
                "Cancel",
                Messages.getInformationIcon()
            )

            if (result == Messages.YES) {
                if (licenseManager.startTrial()) {
                    Messages.showInfoMessage(e.project, "Trial activated!", "Trial Started")
                } else {
                    Messages.showErrorDialog(
                        e.project,
                        "Trial already used. Please purchase a license.",
                        "Trial Not Available"
                    )
                }
            }
        } else {
            Messages.showInfoMessage(
                e.project,
                "Remote source connections will be available in a future release.",
                "Connect Log Source"
            )
        }
    }
}

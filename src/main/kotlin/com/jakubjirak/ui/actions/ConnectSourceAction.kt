package com.jakubjirak.ui.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.Messages
import com.jakubjirak.core.LicenseManager
import com.jakubjirak.core.PremiumFeature

class ConnectSourceAction : AnAction() {
    
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
    
    override fun actionPerformed(e: AnActionEvent) {
        val licenseManager = LicenseManager.getInstance()
        
        if (!licenseManager.canUseFeature(PremiumFeature.MULTI_SOURCE)) {
            val result = Messages.showYesNoDialog(
                e.project,
                """Multi-source log aggregation is a Premium feature!
                
                With Premium, you can:
                • Connect to Kubernetes pods
                • Access Docker containers
                • SSH into remote servers
                • Tail multiple sources simultaneously
                • Correlate logs across your entire stack
                
                Start your FREE 30-day trial?""".trimMargin(),
                "Premium Feature",
                "Start Trial",
                "Cancel",
                Messages.getInformationIcon()
            )
            
            if (result == Messages.YES) {
                val started = licenseManager.startTrial()
                if (started) {
                    Messages.showInfoMessage(
                        e.project,
                        "Trial activated! You can now connect to multiple log sources.",
                        "Trial Started"
                    )
                } else {
                    Messages.showErrorDialog(
                        e.project,
                        "Trial already used. Please purchase a license to continue.",
                        "Trial Not Available"
                    )
                }
            }
        } else {
            Messages.showInfoMessage(
                e.project,
                "Connection dialog coming soon!\n\nSupported sources:\n• Kubernetes\n• Docker\n• SSH\n• Cloud Storage",
                "Connect Log Source"
            )
        }
    }
}

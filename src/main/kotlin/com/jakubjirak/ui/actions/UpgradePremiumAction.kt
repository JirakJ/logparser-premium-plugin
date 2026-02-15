package com.jakubjirak.ui.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.Messages
import java.awt.Desktop
import java.net.URI

class UpgradePremiumAction : AnAction() {
    
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
    
    override fun actionPerformed(e: AnActionEvent) {
        val result = Messages.showYesNoDialog(
            e.project,
            """
            Upgrade to LogParser Pro Premium!
            
            ✅ 30-Day FREE Trial (No credit card required)
            💎 All Premium Features Unlocked
            💰 Only $79/year ($6.58/month)
            
            Features:
            • Transaction/Correlation ID Tracking
            • Kubernetes & Docker Integration  
            • Real-time Multi-source Monitoring
            • AI-powered Pattern Detection
            • Unlimited Performance
            • Priority Support
            
            Would you like to visit our website to start your free trial?
            """.trimIndent(),
            "Upgrade to Premium",
            "Visit Website",
            "Maybe Later",
            Messages.getInformationIcon()
        )
        
        if (result == Messages.YES) {
            try {
                Desktop.getDesktop().browse(URI("https://logparser.dev/upgrade"))
            } catch (ex: Exception) {
                Messages.showErrorDialog(
                    e.project,
                    "Could not open browser. Please visit: https://logparser.dev/upgrade",
                    "Error"
                )
            }
        }
    }
}

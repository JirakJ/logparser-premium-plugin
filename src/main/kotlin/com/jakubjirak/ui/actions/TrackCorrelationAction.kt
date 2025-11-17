package com.jakubjirak.ui.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.Messages
import com.jakubjirak.core.LicenseManager
import com.jakubjirak.core.PremiumFeature

class TrackCorrelationAction : AnAction() {
    
    override fun actionPerformed(e: AnActionEvent) {
        val licenseManager = LicenseManager.getInstance()
        
        if (!licenseManager.canUseFeature(PremiumFeature.CORRELATION_TRACKING)) {
            Messages.showInfoMessage(
                e.project,
                """Transaction Correlation Tracking is a Premium feature!
                
                Track request flows across:
                • Multiple microservices
                • Different Kubernetes pods
                • Docker containers
                • Cloud functions
                
                See the complete journey of every request with timeline visualization.
                
                Start your FREE 30-day trial to unlock this feature!""".trimMargin(),
                "Premium Feature"
            )
        } else {
            val correlationId = Messages.showInputDialog(
                e.project,
                "Enter correlation ID to track:",
                "Track Transaction",
                Messages.getQuestionIcon()
            )
            
            if (!correlationId.isNullOrEmpty()) {
                Messages.showInfoMessage(
                    e.project,
                    "Searching for correlation ID: $correlationId\n\nThis feature is under development.",
                    "Tracking..."
                )
            }
        }
    }
}

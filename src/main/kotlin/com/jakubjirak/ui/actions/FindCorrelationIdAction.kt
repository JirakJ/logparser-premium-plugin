package com.jakubjirak.ui.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.ui.Messages
import com.jakubjirak.core.LicenseManager
import com.jakubjirak.core.PremiumFeature
import com.jakubjirak.parsers.PatternLibrary

class FindCorrelationIdAction : AnAction() {
    
    private val patternLibrary = PatternLibrary()
    
    override fun actionPerformed(e: AnActionEvent) {
        val licenseManager = LicenseManager.getInstance()
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val selectedText = editor.selectionModel.selectedText ?: return
        
        val correlationId = patternLibrary.extractCorrelationId(selectedText)
        
        if (correlationId == null) {
            Messages.showWarningDialog(
                e.project,
                """No correlation ID found in selected text.
                
                Supported formats:
                • X-Request-ID
                • X-Correlation-ID
                • traceId
                • UUID""".trimMargin(),
                "No Correlation ID"
            )
            return
        }
        
        if (!licenseManager.canUseFeature(PremiumFeature.CORRELATION_TRACKING)) {
            Messages.showInfoMessage(
                e.project,
                """Found correlation ID: $correlationId
                
                Cross-source correlation tracking is a Premium feature!
                Start your FREE 30-day trial to:
                • Track this ID across all your services
                • See the complete transaction timeline
                • Identify bottlenecks and errors""".trimMargin(),
                "Premium Feature"
            )
        } else {
            Messages.showInfoMessage(
                e.project,
                "Searching for correlation ID: $correlationId\n\nThis feature is under development.",
                "Finding Correlation ID"
            )
        }
    }
    
    override fun update(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR)
        e.presentation.isEnabled = editor != null && !editor.selectionModel.selectedText.isNullOrEmpty()
    }
}

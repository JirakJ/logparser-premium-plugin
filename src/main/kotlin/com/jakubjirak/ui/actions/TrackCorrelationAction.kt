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
import com.jakubjirak.core.LogParserService
import com.jakubjirak.core.PremiumFeature
import com.jakubjirak.correlation.CorrelationTracker
import com.jakubjirak.correlation.SourceType

/**
 * Prompts the user for a correlation ID and searches the currently open log file.
 * Requires [PremiumFeature.CORRELATION_TRACKING].
 */
class TrackCorrelationAction : AnAction() {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val licenseManager = LicenseManager.getInstance()

        if (!licenseManager.canUseFeature(PremiumFeature.CORRELATION_TRACKING)) {
            Messages.showInfoMessage(
                project,
                "Correlation tracking is a Premium feature.\n" +
                    "Start your FREE 30-day trial to unlock it!",
                "Premium Feature"
            )
            return
        }

        val correlationId = Messages.showInputDialog(
            project,
            "Enter correlation ID to track:",
            "Track Transaction",
            Messages.getQuestionIcon()
        )

        if (correlationId.isNullOrBlank()) return

        val service = LogParserService.getInstance(project)
        val tracker = CorrelationTracker()

        val editor = com.intellij.openapi.fileEditor.FileEditorManager.getInstance(project)
        val currentFile = editor.selectedFiles.firstOrNull() ?: return
        val ioFile = java.io.File(currentFile.path)
        if (!ioFile.exists()) return

        val parser = service.parseLogFile(ioFile)
        val entries = parser.searchCorrelationId(correlationId)

        if (entries.isEmpty()) {
            Messages.showInfoMessage(
                project,
                "No entries found for correlation ID: $correlationId",
                "Tracking Results"
            )
        } else {
            entries.forEach { tracker.addEntry(correlationId, it, ioFile.name, SourceType.LOCAL_FILE) }
            val flow = tracker.getTransaction(correlationId)
            val durationStr = flow?.duration?.let { "${it}ms" } ?: "unknown"
            Messages.showInfoMessage(
                project,
                "Found ${entries.size} entries for ID: $correlationId\n" +
                    "Duration: $durationStr\n" +
                    "Has errors: ${flow?.hasErrors ?: false}",
                "Tracking Results"
            )
        }
    }
}

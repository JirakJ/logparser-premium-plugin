/*
 * Copyright (c) 2026 Jakub Jirák. All rights reserved.
 * Licensed under the terms of the JetBrains Marketplace EULA.
 */
package com.jakubjirak.ui.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.ui.Messages
import com.jakubjirak.core.LicenseManager
import com.jakubjirak.core.PremiumFeature
import com.jakubjirak.parsers.PatternLibrary

/**
 * Editor popup action that extracts a correlation ID from the current selection
 * and, with a premium license, offers to search for it across the file.
 */
class FindCorrelationIdAction : AnAction() {

    private val patternLibrary = PatternLibrary()

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val selectedText = editor.selectionModel.selectedText ?: return

        val correlationId = patternLibrary.extractCorrelationId(selectedText)

        if (correlationId == null) {
            Messages.showWarningDialog(
                project,
                "No correlation ID found in selected text.\n\n" +
                    "Supported formats: X-Request-ID, X-Correlation-ID, traceId, UUID",
                "No Correlation ID"
            )
            return
        }

        val licenseManager = LicenseManager.getInstance()
        if (!licenseManager.canUseFeature(PremiumFeature.CORRELATION_TRACKING)) {
            Messages.showInfoMessage(
                project,
                "Found correlation ID: $correlationId\n\n" +
                    "Cross-source correlation tracking is a Premium feature.\n" +
                    "Start your FREE 30-day trial to unlock it!",
                "Premium Feature"
            )
        } else {
            Messages.showInfoMessage(project, "Correlation ID: $correlationId", "Found Correlation ID")
        }
    }

    override fun update(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR)
        e.presentation.isEnabled = editor != null && !editor.selectionModel.selectedText.isNullOrEmpty()
    }
}

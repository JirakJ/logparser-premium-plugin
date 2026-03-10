/*
 * Copyright (c) 2026 Jakub Jirák. All rights reserved.
 * Licensed under the terms of the JetBrains Marketplace EULA.
 */
package com.jakubjirak.logparser.pro.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.ui.Messages
import com.jakubjirak.logparser.pro.core.PatternLibrary

/**
 * Editor popup action that extracts a correlation ID from the current selection
 * and displays it to the user.
 *
 * Supports X-Request-ID, X-Correlation-ID, traceId, UUID, and other common
 * correlation header formats.
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

        Messages.showInfoMessage(project, "Correlation ID: $correlationId", "Found Correlation ID")
    }

    override fun update(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR)
        e.presentation.isEnabled = editor != null && !editor.selectionModel.selectedText.isNullOrEmpty()
    }
}

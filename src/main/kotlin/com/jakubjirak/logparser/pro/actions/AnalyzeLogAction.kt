/*
 * Copyright (c) 2026 Jakub Jirák. All rights reserved.
 * Licensed under the terms of the JetBrains Marketplace EULA.
 */
package com.jakubjirak.logparser.pro.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.Messages
import com.jakubjirak.logparser.pro.analysis.LogClassifier
import com.jakubjirak.logparser.pro.analysis.LogStatistics
import com.jakubjirak.logparser.pro.core.LogParserService
import java.io.File

/**
 * Runs full analysis (classification + statistics + anomaly detection) on the
 * currently open log file and presents a summary dialog.
 */
class AnalyzeLogAction : AnAction() {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        val editor = com.intellij.openapi.fileEditor.FileEditorManager.getInstance(project)
        val currentFile = editor.selectedFiles.firstOrNull() ?: run {
            Messages.showWarningDialog(project, "No file is currently open.", "No File")
            return
        }

        val ioFile = File(currentFile.path)
        if (!ioFile.exists()) return

        val service = LogParserService.getInstance(project)
        val parser = service.parseLogFile(ioFile)
        val entries = parser.parseLines()

        if (entries.isEmpty()) {
            Messages.showInfoMessage(project, "No log entries found.", "Analysis")
            return
        }

        val stats = LogStatistics().compute(entries)
        val classifier = LogClassifier()
        val anomalies = classifier.detectAnomalies(entries)

        val report = buildString {
            appendLine("=== Log Analysis Report ===")
            appendLine("Total entries: ${stats.totalEntries}")
            appendLine("Error rate: ${String.format(java.util.Locale.US, "%.1f%%", stats.errorRate * 100)}")
            appendLine("Warning rate: ${String.format(java.util.Locale.US, "%.1f%%", stats.warningRate * 100)}")
            appendLine("Stack traces: ${stats.stackTraceCount}")
            appendLine("Avg message length: ${stats.avgMessageLength.toInt()} chars")
            appendLine()
            appendLine("Level distribution:")
            stats.countByLevel.entries.sortedByDescending { it.value }.forEach {
                appendLine("  ${it.key}: ${it.value}")
            }
            if (stats.topLoggers.isNotEmpty()) {
                appendLine()
                appendLine("Top loggers:")
                stats.topLoggers.forEach { appendLine("  ${it.first}: ${it.second}") }
            }
            if (anomalies.isNotEmpty()) {
                appendLine()
                appendLine("Anomalies detected: ${anomalies.size}")
                anomalies.take(5).forEach { a ->
                    appendLine("  Line ${a.entry.lineNumber}: ${a.reasons.joinToString(", ")}")
                }
            }
        }

        Messages.showInfoMessage(project, report, "Log Analysis — ${ioFile.name}")
    }
}

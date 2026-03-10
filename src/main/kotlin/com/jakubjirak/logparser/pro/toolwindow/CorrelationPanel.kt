/*
 * Copyright (c) 2026 Jakub Jirák. All rights reserved.
 * Licensed under the terms of the JetBrains Marketplace EULA.
 */
package com.jakubjirak.logparser.pro.toolwindow

import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.JBUI
import com.jakubjirak.logparser.pro.core.LogParserService
import com.jakubjirak.logparser.pro.correlation.CorrelationTracker
import com.jakubjirak.logparser.pro.correlation.SourceType
import java.awt.BorderLayout
import java.io.File
import javax.swing.*

/**
 * Correlation tracking tab in the tool window.
 *
 * Allows the user to enter a correlation ID and view all matching entries
 * with timeline, duration, and error status across the loaded log file.
 */
class CorrelationPanel(private val project: Project) {

    private val panel = JPanel(BorderLayout())
    private val correlationField = JBTextField(30)
    private val resultsArea = JBTextArea(20, 60)

    init {
        setupUI()
    }

    private fun setupUI() {
        val topPanel = JPanel()
        topPanel.layout = BoxLayout(topPanel, BoxLayout.Y_AXIS)
        topPanel.border = JBUI.Borders.empty(12)

        topPanel.add(JBLabel("<html><h3>Correlation Tracking</h3></html>"))
        topPanel.add(Box.createVerticalStrut(8))

        val inputBar = JPanel()
        inputBar.layout = BoxLayout(inputBar, BoxLayout.X_AXIS)
        inputBar.add(JBLabel("Correlation ID: "))
        inputBar.add(correlationField)
        inputBar.add(Box.createHorizontalStrut(8))
        val trackButton = JButton("Track")
        trackButton.addActionListener { performTracking() }
        inputBar.add(trackButton)
        topPanel.add(inputBar)

        panel.add(topPanel, BorderLayout.NORTH)

        resultsArea.isEditable = false
        resultsArea.font = resultsArea.font.deriveFont(12f)
        panel.add(JBScrollPane(resultsArea), BorderLayout.CENTER)

        correlationField.addActionListener { performTracking() }
    }

    private fun performTracking() {
        val corrId = correlationField.text.trim()
        if (corrId.isEmpty()) return

        val editorManager = FileEditorManager.getInstance(project)
        val currentFile = editorManager.selectedFiles.firstOrNull() ?: return
        val ioFile = File(currentFile.path)
        if (!ioFile.exists()) return

        val service = LogParserService.getInstance(project)
        val parser = service.parseLogFile(ioFile)
        val entries = parser.searchCorrelationId(corrId)

        if (entries.isEmpty()) {
            resultsArea.text = "No entries found for correlation ID: $corrId"
            return
        }

        val tracker = CorrelationTracker()
        entries.forEach { tracker.addEntry(corrId, it, ioFile.name, SourceType.LOCAL_FILE) }
        val flow = tracker.getTransaction(corrId)

        resultsArea.text = buildString {
            appendLine("=== Correlation: $corrId ===")
            appendLine("Entries found: ${entries.size}")
            appendLine("Duration: ${flow?.duration?.let { "${it}ms" } ?: "unknown"}")
            appendLine("Has errors: ${flow?.hasErrors ?: false}")
            appendLine("Sources: ${flow?.sources?.joinToString(", ") ?: "N/A"}")
            appendLine()
            appendLine("Timeline:")
            entries.forEachIndexed { index, entry ->
                appendLine("  ${index + 1}. [${entry.timestamp ?: "?"}] ${entry.level} — ${entry.message}")
            }
        }
        resultsArea.caretPosition = 0
    }

    /** Returns the root UI component. */
    fun getContent(): JComponent = panel
}
